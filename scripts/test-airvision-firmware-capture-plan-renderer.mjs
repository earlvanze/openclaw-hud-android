#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { readFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const rendererPath = join(scriptDir, "render-airvision-firmware-capture-plan.mjs");
const sourcePath = join(androidDir, "app/src/main/java/ai/openclaw/app/AirVisionUsbController.kt");
const capturePlanPath = join(androidDir, "play/airvision-firmware-capture-plan.md");

function parseCaptureFeatureLabels(source) {
  const enumStart = source.indexOf("enum class AirVisionFirmwareFeature");
  const enumEnd = source.indexOf("data class AirVisionFirmwareFeatureReadiness", enumStart);
  if (enumStart < 0 || enumEnd < 0) {
    throw new Error("Could not locate AirVisionFirmwareFeature enum.");
  }

  const enumSource = source.slice(enumStart, enumEnd);
  const labels = [...enumSource.matchAll(/label\s*=\s*"([^"]+)"/g)].map((match) => match[1]);
  if (labels.length === 0) throw new Error("No AirVision firmware feature labels parsed.");
  return labels;
}

function requireIncludes(label, text, needles) {
  const missing = needles.filter((needle) => !text.includes(needle));
  if (missing.length > 0) throw new Error(`${label} missing: ${missing.join(", ")}`);
}

const check = spawnSync(process.execPath, [rendererPath, "--check"], {
  cwd: androidDir,
  encoding: "utf8",
  stdio: ["ignore", "pipe", "pipe"],
});
if (check.status !== 0) {
  throw new Error(check.stderr.trim() || check.stdout.trim() || "Capture plan renderer check failed.");
}

const source = await readFile(sourcePath, "utf8");
const capturePlan = await readFile(capturePlanPath, "utf8");
const labels = parseCaptureFeatureLabels(source);

requireIncludes("Capture plan safety criteria", capturePlan, [
  "## Capture Acceptance Criteria",
  "exact Windows write report ID",
  "payload bytes",
  "checksum/framing",
  "visible-state evidence",
  "## Capture Result Template",
  "Android enablement decision",
  "- Do not commit raw USB serial numbers, raw private capture payloads, or temporary review/demo credentials.",
]);

for (const label of labels) {
  requireIncludes(`Capture result row for ${label}`, capturePlan, [
    `| ${label} | pending | pending | pending | pending | pending | pending | blocked |`,
  ]);
}

requireIncludes("Capture plan metadata", capturePlan, [`- Feature count: ${labels.length}`]);

console.log("AirVision firmware capture plan renderer regression tests passed.");
