#!/usr/bin/env node

import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { deflateSync, inflateSync } from "node:zlib";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const outDir = join(androidDir, "play", "screenshots", "phone");
const pngSignature = Buffer.from("89504e470d0a1a0a", "hex");

const width = 1080;
const height = 1920;
const black = [0, 0, 0];
const green = [87, 255, 122];
const greenDim = [30, 126, 54];
const red = [255, 68, 68];
const white = [226, 255, 231];
const muted = [118, 176, 130];
const line = [18, 72, 30];

const font = {
  " ": ["00000", "00000", "00000", "00000", "00000", "00000", "00000"],
  A: ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
  B: ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
  C: ["01111", "10000", "10000", "10000", "10000", "10000", "01111"],
  D: ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
  E: ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
  F: ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
  G: ["01111", "10000", "10000", "10111", "10001", "10001", "01111"],
  H: ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
  I: ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
  J: ["00111", "00010", "00010", "00010", "10010", "10010", "01100"],
  K: ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
  L: ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
  M: ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
  N: ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
  O: ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
  P: ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
  Q: ["01110", "10001", "10001", "10001", "10101", "10010", "01101"],
  R: ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
  S: ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
  T: ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
  U: ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
  V: ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
  W: ["10001", "10001", "10001", "10101", "10101", "10101", "01010"],
  X: ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
  Y: ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
  Z: ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
  0: ["01110", "10001", "10011", "10101", "11001", "10001", "01110"],
  1: ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
  2: ["01110", "10001", "00001", "00010", "00100", "01000", "11111"],
  3: ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
  4: ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
  5: ["11111", "10000", "10000", "11110", "00001", "00001", "11110"],
  6: ["01110", "10000", "10000", "11110", "10001", "10001", "01110"],
  7: ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
  8: ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
  9: ["01110", "10001", "10001", "01111", "00001", "00001", "01110"],
  ".": ["00000", "00000", "00000", "00000", "00000", "01100", "01100"],
  ":": ["00000", "01100", "01100", "00000", "01100", "01100", "00000"],
  "-": ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
  "/": ["00001", "00010", "00010", "00100", "01000", "01000", "10000"],
  "+": ["00000", "00100", "00100", "11111", "00100", "00100", "00000"],
  ">": ["10000", "01000", "00100", "00010", "00100", "01000", "10000"],
  "<": ["00001", "00010", "00100", "01000", "00100", "00010", "00001"],
};

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

function makeCanvas() {
  const pixels = Buffer.alloc(width * height * 3);
  for (let offset = 0; offset < pixels.length; offset += 3) {
    pixels[offset] = black[0];
    pixels[offset + 1] = black[1];
    pixels[offset + 2] = black[2];
  }
  return pixels;
}

function setPixel(pixels, x, y, color) {
  if (x < 0 || y < 0 || x >= width || y >= height) return;
  const offset = (Math.floor(y) * width + Math.floor(x)) * 3;
  pixels[offset] = color[0];
  pixels[offset + 1] = color[1];
  pixels[offset + 2] = color[2];
}

function fillRect(pixels, x, y, w, h, color) {
  for (let yy = Math.max(0, y); yy < Math.min(height, y + h); yy += 1) {
    for (let xx = Math.max(0, x); xx < Math.min(width, x + w); xx += 1) {
      setPixel(pixels, xx, yy, color);
    }
  }
}

function strokeRect(pixels, x, y, w, h, color, thickness = 4) {
  fillRect(pixels, x, y, w, thickness, color);
  fillRect(pixels, x, y + h - thickness, w, thickness, color);
  fillRect(pixels, x, y, thickness, h, color);
  fillRect(pixels, x + w - thickness, y, thickness, h, color);
}

function circle(pixels, cx, cy, radius, color) {
  const r2 = radius * radius;
  for (let y = cy - radius; y <= cy + radius; y += 1) {
    for (let x = cx - radius; x <= cx + radius; x += 1) {
      const dx = x - cx;
      const dy = y - cy;
      if (dx * dx + dy * dy <= r2) setPixel(pixels, x, y, color);
    }
  }
}

function drawGlyph(pixels, ch, x, y, scale, color) {
  const glyph = font[ch] ?? font[" "];
  for (let row = 0; row < glyph.length; row += 1) {
    for (let col = 0; col < glyph[row].length; col += 1) {
      if (glyph[row][col] === "1") {
        fillRect(pixels, x + col * scale, y + row * scale, scale, scale, color);
      }
    }
  }
}

function measureText(text, scale) {
  return text.length * 6 * scale;
}

function drawText(pixels, text, x, y, scale, color) {
  const upper = text.toUpperCase();
  let cursor = x;
  for (const ch of upper) {
    drawGlyph(pixels, ch, cursor, y, scale, color);
    cursor += 6 * scale;
  }
}

function centerText(pixels, text, y, scale, color) {
  drawText(pixels, text, Math.floor((width - measureText(text, scale)) / 2), y, scale, color);
}

function drawRow(pixels, y, label, value) {
  drawText(pixels, label, 96, y, 6, muted);
  const valueScale = measureText(value, 6) <= 888 ? 6 : 5;
  drawText(pixels, value, 96, y + 54, valueScale, green);
  fillRect(pixels, 96, y + 126, 888, 3, line);
}

function makePng(pixels) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;
  ihdr[9] = 2;
  ihdr[10] = 0;
  ihdr[11] = 0;
  ihdr[12] = 0;

  const stride = width * 3;
  const scanlines = Buffer.alloc(height * (stride + 1));
  for (let row = 0; row < height; row += 1) {
    scanlines[row * (stride + 1)] = 0;
    pixels.copy(scanlines, row * (stride + 1) + 1, row * stride, (row + 1) * stride);
  }

  return Buffer.concat([
    pngSignature,
    chunk("IHDR", ihdr),
    chunk("IDAT", deflateSync(scanlines, { level: 9 })),
    chunk("IEND"),
  ]);
}

function pngPixels(png, path) {
  if (!png.subarray(0, pngSignature.length).equals(pngSignature)) {
    throw new Error(`Generated Play screenshot artifact is not a PNG: ${path}`);
  }

  let offset = pngSignature.length;
  let seenIhdr = false;
  const idatChunks = [];

  while (offset < png.length) {
    if (offset + 12 > png.length) throw new Error(`PNG chunk is truncated: ${path}`);
    const length = png.readUInt32BE(offset);
    const type = png.toString("ascii", offset + 4, offset + 8);
    const dataStart = offset + 8;
    const dataEnd = dataStart + length;
    const crcEnd = dataEnd + 4;
    if (crcEnd > png.length) throw new Error(`PNG chunk data is truncated: ${path}`);

    const data = png.subarray(dataStart, dataEnd);
    if (type === "IHDR") {
      seenIhdr = true;
      const actualWidth = data.readUInt32BE(0);
      const actualHeight = data.readUInt32BE(4);
      const bitDepth = data[8];
      const colorType = data[9];
      const interlace = data[12];
      if (
        actualWidth !== width ||
        actualHeight !== height ||
        bitDepth !== 8 ||
        colorType !== 2 ||
        interlace !== 0
      ) {
        throw new Error(`PNG must be ${width}x${height} 8-bit RGB without interlace: ${path}`);
      }
    } else if (type === "IDAT") {
      idatChunks.push(data);
    } else if (type === "IEND") {
      break;
    }
    offset = crcEnd;
  }

  if (!seenIhdr || idatChunks.length === 0) {
    throw new Error(`PNG is missing required chunks: ${path}`);
  }

  const pixels = inflateSync(Buffer.concat(idatChunks));
  const expectedLength = height * (width * 3 + 1);
  if (pixels.length !== expectedLength) {
    throw new Error(`PNG has unexpected pixel data length: ${path}`);
  }
  return pixels;
}

function renderHudDemo() {
  const pixels = makeCanvas();
  circle(pixels, 968, 72, 14, green);
  circle(pixels, 914, 72, 14, red);
  strokeRect(pixels, 846, 56, 32, 28, greenDim, 3);
  fillRect(pixels, 878, 66, 8, 8, greenDim);

  centerText(pixels, "OPENCLAW HUD", 144, 10, green);
  centerText(pixels, "SAMSUNG DEX / AIRVISION M1", 230, 5, muted);

  strokeRect(pixels, 72, 330, 936, 265, line, 4);
  drawText(pixels, "MAPS", 112, 370, 5, muted);
  drawText(pixels, "LEFT IN 0.2 MI", 112, 430, 9, green);
  drawText(pixels, "COLFAX AVE", 112, 535, 6, white);

  strokeRect(pixels, 72, 690, 936, 330, line, 4);
  drawText(pixels, "LIVE CAPTIONS  ES", 112, 730, 5, muted);
  drawText(pixels, "S1  WE TURN HERE", 112, 800, 7, green);
  drawText(pixels, "S2  SI A LA IZQUIERDA", 112, 890, 7, white);

  strokeRect(pixels, 72, 1110, 936, 430, line, 4);
  drawText(pixels, "ASSISTANT", 112, 1150, 5, muted);
  drawText(pixels, "ROUTE CLEAR", 112, 1220, 7, green);
  drawText(pixels, "NEXT TURN IN 0.2 MI", 112, 1310, 7, green);
  drawText(pixels, "THINK OFF", 112, 1430, 5, muted);

  strokeRect(pixels, 72, 1645, 936, 125, greenDim, 4);
  drawText(pixels, "ASK OPENCLAW", 112, 1688, 7, green);
  return makePng(pixels);
}

function renderSettingsDemo() {
  const pixels = makeCanvas();
  circle(pixels, 968, 72, 14, green);
  centerText(pixels, "AIRVISION M1", 128, 10, green);
  centerText(pixels, "APP PREFERENCES / HUD PROFILE", 214, 5, muted);

  drawRow(pixels, 340, "VIEWING MODE", "WORKING");
  drawRow(pixels, 500, "IPD CALIBRATION", "67 MM");
  drawRow(pixels, 660, "HUD DISPLAY TARGET", "AIRVISION PREFERRED");
  drawRow(pixels, 820, "GESTURES", "TAP CLEAR / MIC");
  drawRow(pixels, 980, "SWIPE ACTION", "SCROLL CHAT");
  drawRow(pixels, 1140, "CAPTIONS", "NATIVE + OPENCLAW");
  drawRow(pixels, 1300, "TRANSLATION TARGET", "SPANISH");
  drawRow(pixels, 1460, "DEMO MODE", "PLAY REVIEW READY");

  strokeRect(pixels, 72, 1668, 936, 164, greenDim, 4);
  drawText(pixels, "PRIVACY SAFE HUD BUILD", 112, 1710, 6, green);
  drawText(pixels, "NO SMS / CALL LOG / OVERLAY", 112, 1772, 5, muted);
  return makePng(pixels);
}

function manifest() {
  return `${JSON.stringify(
    {
      schema: "openclaw.play.screenshots",
      version: 1,
      deviceType: "phone",
      generatedBy: "scripts/render-play-screenshots.mjs",
      screenshots: ["play/screenshots/phone/hud-demo.png", "play/screenshots/phone/settings-demo.png"],
    },
    null,
    2,
  )}\n`;
}

async function writeIfChanged(path, data, check) {
  if (check) {
    const existing = await readFile(path).catch(() => null);
    const expected = Buffer.isBuffer(data) ? data : Buffer.from(data);
    const matches = Buffer.isBuffer(data)
      ? existing && pngPixels(existing, path).equals(pngPixels(expected, path))
      : existing && existing.equals(expected);
    if (!matches) {
      throw new Error(`Generated Play screenshot artifact is stale: ${path}`);
    }
    return;
  }
  await writeFile(path, data);
}

const check = process.argv.includes("--check");
await mkdir(outDir, { recursive: true });
await writeIfChanged(join(outDir, "hud-demo.png"), renderHudDemo(), check);
await writeIfChanged(join(outDir, "settings-demo.png"), renderSettingsDemo(), check);
await writeIfChanged(join(outDir, "manifest.json"), manifest(), check);
console.log(`Play screenshot artifacts ${check ? "verified" : "rendered"} in ${outDir}`);
