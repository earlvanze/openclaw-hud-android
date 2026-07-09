#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { deflateSync } from "node:zlib";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const pngSignature = Buffer.from("89504e470d0a1a0a", "hex");
const crcTable = new Uint32Array(256);

for (let index = 0; index < crcTable.length; index += 1) {
  let value = index;
  for (let bit = 0; bit < 8; bit += 1) {
    value = value & 1 ? 0xedb88320 ^ (value >>> 1) : value >>> 1;
  }
  crcTable[index] = value >>> 0;
}

function crc32(buffer) {
  let crc = 0xffffffff;
  for (const byte of buffer) crc = crcTable[(crc ^ byte) & 0xff] ^ (crc >>> 8);
  return (crc ^ 0xffffffff) >>> 0;
}

function chunk(type, data = Buffer.alloc(0)) {
  const typeBuffer = Buffer.from(type, "ascii");
  const payload = Buffer.concat([typeBuffer, data]);
  const out = Buffer.alloc(12 + data.length);
  out.writeUInt32BE(data.length, 0);
  typeBuffer.copy(out, 4);
  data.copy(out, 8);
  out.writeUInt32BE(crc32(payload), 8 + data.length);
  return out;
}

function paethPredictor(left, above, upperLeft) {
  const estimate = left + above - upperLeft;
  const leftDistance = Math.abs(estimate - left);
  const aboveDistance = Math.abs(estimate - above);
  const upperLeftDistance = Math.abs(estimate - upperLeft);
  if (leftDistance <= aboveDistance && leftDistance <= upperLeftDistance) return left;
  if (aboveDistance <= upperLeftDistance) return above;
  return upperLeft;
}

function pixelBytes(width, height) {
  const bytes = Buffer.alloc(width * height * 4);
  for (let y = 0; y < height; y += 1) {
    for (let x = 0; x < width; x += 1) {
      const offset = (y * width + x) * 4;
      bytes[offset] = (x * 7 + y * 3) & 0xff;
      bytes[offset + 1] = (x * 5 + y * 11) & 0xff;
      bytes[offset + 2] = 80;
      bytes[offset + 3] = 160 + ((x + y) % 96);
    }
  }
  return bytes;
}

function encodeFilteredScanlines(decoded, width, height, bytesPerPixel) {
  const stride = width * bytesPerPixel;
  const filtered = Buffer.alloc(height * (stride + 1));
  const filters = [0, 1, 2, 3, 4];

  for (let row = 0; row < height; row += 1) {
    const filter = filters[row % filters.length];
    const inOffset = row * stride;
    const outOffset = row * (stride + 1);
    const previousOffset = inOffset - stride;
    filtered[outOffset] = filter;

    for (let column = 0; column < stride; column += 1) {
      const current = decoded[inOffset + column];
      const left = column >= bytesPerPixel ? decoded[inOffset + column - bytesPerPixel] : 0;
      const above = row > 0 ? decoded[previousOffset + column] : 0;
      const upperLeft = row > 0 && column >= bytesPerPixel ? decoded[previousOffset + column - bytesPerPixel] : 0;
      let predictor;

      if (filter === 0) predictor = 0;
      else if (filter === 1) predictor = left;
      else if (filter === 2) predictor = above;
      else if (filter === 3) predictor = Math.floor((left + above) / 2);
      else predictor = paethPredictor(left, above, upperLeft);

      filtered[outOffset + 1 + column] = (current - predictor) & 0xff;
    }
  }

  return filtered;
}

function rgbaPng(width, height) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;
  ihdr[9] = 6;
  ihdr[10] = 0;
  ihdr[11] = 0;
  ihdr[12] = 0;

  return Buffer.concat([
    pngSignature,
    chunk("IHDR", ihdr),
    chunk("IDAT", deflateSync(encodeFilteredScanlines(pixelBytes(width, height), width, height, 4), { level: 9 })),
    chunk("IEND"),
  ]);
}

function pngInfo(buffer) {
  if (!buffer.subarray(0, pngSignature.length).equals(pngSignature)) throw new Error("Not a PNG.");
  if (buffer.toString("ascii", 12, 16) !== "IHDR") throw new Error("Missing IHDR.");
  return {
    width: buffer.readUInt32BE(16),
    height: buffer.readUInt32BE(20),
    bitDepth: buffer[24],
    colorType: buffer[25],
  };
}

function runNode(args, label, expectedStatus = 0) {
  const result = spawnSync(process.execPath, args, {
    cwd: androidDir,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
  if (result.status !== expectedStatus) {
    throw new Error(
      [
        `${label} exited ${result.status}, expected ${expectedStatus}.`,
        result.stdout.trim(),
        result.stderr.trim(),
      ]
        .filter(Boolean)
        .join("\n"),
    );
  }
  return result;
}

async function writeFinalAppContent(path, screenshotPath) {
  const appContent = JSON.parse(await readFile(join(androidDir, "play", "app-content-answers.json"), "utf8"));
  appContent.finalSubmission = {
    hostedPrivacyPolicyUrl: "https://example.com/openclaw-hud-privacy",
    appCreatedInPlayConsole: true,
    internalTestersConfiguredInPlayConsole: true,
    reviewerAccessConfiguredInPlayConsole: true,
    phoneScreenshots: [screenshotPath, screenshotPath],
    notes: "Synthetic screenshot verifier regression test.",
  };
  await writeFile(path, `${JSON.stringify(appContent, null, 2)}\n`);
}

const workDir = await mkdtemp(join(tmpdir(), "openclaw-play-screenshots-"));

try {
  const rgbaPath = join(workDir, "rgba.png");
  const rgbPath = join(workDir, "rgb.png");
  const validAppContentPath = join(workDir, "app-content-valid.json");
  const invalidAppContentPath = join(workDir, "app-content-invalid.json");

  await writeFile(rgbaPath, rgbaPng(320, 640));
  runNode(["scripts/convert-play-screenshot.mjs", rgbaPath, rgbPath], "convert-play-screenshot");

  const convertedInfo = pngInfo(await readFile(rgbPath));
  if (convertedInfo.width !== 320 || convertedInfo.height !== 640 || convertedInfo.bitDepth !== 8 || convertedInfo.colorType !== 2) {
    throw new Error(`Converted screenshot is not Play-ready 24-bit PNG: ${JSON.stringify(convertedInfo)}`);
  }

  await writeFinalAppContent(validAppContentPath, rgbPath);
  runNode(
    [
      "scripts/verify-play-submission-package.mjs",
      "--app-content",
      validAppContentPath,
      "--final",
      "--skip-hosted-privacy-url-fetch",
    ],
    "final verifier with converted screenshot",
  );

  await writeFinalAppContent(invalidAppContentPath, rgbaPath);
  const rejected = runNode(
    [
      "scripts/verify-play-submission-package.mjs",
      "--app-content",
      invalidAppContentPath,
      "--final",
      "--skip-hosted-privacy-url-fetch",
    ],
    "final verifier with alpha screenshot",
    1,
  );
  const rejectionText = `${rejected.stdout}\n${rejected.stderr}`;
  if (!rejectionText.includes("PNG must be 24-bit RGB without alpha")) {
    throw new Error(`Final verifier rejected alpha PNG for the wrong reason:\n${rejectionText}`);
  }

  console.log("Play screenshot tool regression tests passed.");
} finally {
  await rm(workDir, { force: true, recursive: true });
}
