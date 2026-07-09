#!/usr/bin/env node

import { readFile, writeFile } from "node:fs/promises";
import { deflateSync, inflateSync } from "node:zlib";

const pngSignature = Buffer.from("89504e470d0a1a0a", "hex");
const crcTable = new Uint32Array(256);

for (let index = 0; index < crcTable.length; index += 1) {
  let value = index;
  for (let bit = 0; bit < 8; bit += 1) {
    value = value & 1 ? 0xedb88320 ^ (value >>> 1) : value >>> 1;
  }
  crcTable[index] = value >>> 0;
}

function usage() {
  console.error("Usage: node scripts/convert-play-screenshot.mjs input.png output.png");
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

function unfilterScanlines(raw, width, height, bytesPerPixel) {
  const stride = width * bytesPerPixel;
  const expectedSize = height * (stride + 1);
  if (raw.length !== expectedSize) {
    throw new Error(`Unexpected PNG scanline size: ${raw.length}, expected ${expectedSize}`);
  }

  const decoded = Buffer.alloc(height * stride);
  for (let row = 0; row < height; row += 1) {
    const filter = raw[row * (stride + 1)];
    const scanlineOffset = row * (stride + 1) + 1;
    const decodedOffset = row * stride;
    const previousOffset = decodedOffset - stride;

    for (let column = 0; column < stride; column += 1) {
      const current = raw[scanlineOffset + column];
      const left = column >= bytesPerPixel ? decoded[decodedOffset + column - bytesPerPixel] : 0;
      const above = row > 0 ? decoded[previousOffset + column] : 0;
      const upperLeft = row > 0 && column >= bytesPerPixel ? decoded[previousOffset + column - bytesPerPixel] : 0;
      let value;

      if (filter === 0) value = current;
      else if (filter === 1) value = current + left;
      else if (filter === 2) value = current + above;
      else if (filter === 3) value = current + Math.floor((left + above) / 2);
      else if (filter === 4) value = current + paethPredictor(left, above, upperLeft);
      else throw new Error(`Unsupported PNG filter type: ${filter}`);

      decoded[decodedOffset + column] = value & 0xff;
    }
  }
  return decoded;
}

function readChunks(buffer) {
  if (!buffer.subarray(0, pngSignature.length).equals(pngSignature)) {
    throw new Error("Input is not a PNG file.");
  }

  const chunks = [];
  let offset = pngSignature.length;
  while (offset < buffer.length) {
    const length = buffer.readUInt32BE(offset);
    const type = buffer.toString("ascii", offset + 4, offset + 8);
    const data = buffer.subarray(offset + 8, offset + 8 + length);
    chunks.push({ type, data });
    offset += 12 + length;
    if (type === "IEND") break;
  }
  return chunks;
}

function makeRgbPng({ width, height, rgb }) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;
  ihdr[9] = 2;
  ihdr[10] = 0;
  ihdr[11] = 0;
  ihdr[12] = 0;

  const stride = width * 3;
  const filtered = Buffer.alloc(height * (stride + 1));
  for (let row = 0; row < height; row += 1) {
    const outOffset = row * (stride + 1);
    filtered[outOffset] = 0;
    rgb.copy(filtered, outOffset + 1, row * stride, (row + 1) * stride);
  }

  return Buffer.concat([
    pngSignature,
    chunk("IHDR", ihdr),
    chunk("IDAT", deflateSync(filtered, { level: 9 })),
    chunk("IEND"),
  ]);
}

function convertToRgbPng(input) {
  const chunks = readChunks(input);
  const ihdr = chunks.find((entry) => entry.type === "IHDR")?.data;
  if (!ihdr) throw new Error("PNG is missing IHDR.");

  const width = ihdr.readUInt32BE(0);
  const height = ihdr.readUInt32BE(4);
  const bitDepth = ihdr[8];
  const colorType = ihdr[9];
  const compression = ihdr[10];
  const filter = ihdr[11];
  const interlace = ihdr[12];

  if (bitDepth !== 8) throw new Error(`Unsupported PNG bit depth: ${bitDepth}`);
  if (compression !== 0 || filter !== 0 || interlace !== 0) throw new Error("Unsupported PNG compression/filter/interlace settings.");
  if (colorType !== 2 && colorType !== 6) throw new Error(`Unsupported PNG color type: ${colorType}`);

  const bytesPerPixel = colorType === 6 ? 4 : 3;
  const idat = Buffer.concat(chunks.filter((entry) => entry.type === "IDAT").map((entry) => entry.data));
  const decoded = unfilterScanlines(inflateSync(idat), width, height, bytesPerPixel);

  if (colorType === 2) return makeRgbPng({ width, height, rgb: decoded });

  const rgb = Buffer.alloc(width * height * 3);
  for (let pixel = 0; pixel < width * height; pixel += 1) {
    rgb[pixel * 3] = decoded[pixel * 4];
    rgb[pixel * 3 + 1] = decoded[pixel * 4 + 1];
    rgb[pixel * 3 + 2] = decoded[pixel * 4 + 2];
  }
  return makeRgbPng({ width, height, rgb });
}

const [inputPath, outputPath] = process.argv.slice(2);
if (!inputPath || !outputPath) {
  usage();
  process.exit(2);
}

const input = await readFile(inputPath);
await writeFile(outputPath, convertToRgbPng(input));
