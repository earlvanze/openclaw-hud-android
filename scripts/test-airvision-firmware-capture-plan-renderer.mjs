#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { readFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const rendererPath = join(scriptDir, "render-airvision-firmware-capture-plan.mjs");
const resultsVerifierPath = join(scriptDir, "verify-airvision-firmware-capture-results.mjs");
const sourcePath = join(androidDir, "app/src/main/java/ai/openclaw/app/AirVisionUsbController.kt");
const capturePlanPath = join(androidDir, "play/airvision-firmware-capture-plan.md");
const captureResultsPath = join(androidDir, "play/airvision-firmware-capture-results.json");

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
const captureResults = await readFile(captureResultsPath, "utf8");
const labels = parseCaptureFeatureLabels(source);

requireIncludes("Capture plan safety criteria", capturePlan, [
  "## Firmware Write Gate",
  "- Status: `read_only_capture_pending`",
  "firmware writes: read-only",
  "- Firmware writes enabled: no",
  "- Protocol-ready feature labels: none",
  "- Blocked feature labels:",
  "- Live M1 required before writes: yes",
  "- Explicit user confirmation required: yes",
  "Capture and validate ASUS HID report payloads on Windows/Cyber",
  "### Live M1 Write-Test Checklist",
  "- Reconnect the AirVision M1 to the Android device.",
  "- Read back the matching report and verify checksum/framing.",
  "## Firmware Apply Preview",
  "- Status: `no_protocol_ready_commands`",
  "firmware apply preview: 0 protocol-ready",
  "- Protocol-ready command count: 0/",
  "- Commands: none",
  "## Capture Acceptance Criteria",
  "exact Windows write report ID",
  "sanitized payload summary",
  "Keep raw bytes only in private capture files.",
  "checksum/framing",
  "visible-state evidence",
  "## Capture Result Template",
  "Write payload summary",
  "Readback payload summary",
  "Android enablement decision",
  "- Do not commit raw USB serial numbers, raw private capture payloads, or temporary review/demo credentials.",
]);

if (capturePlan.includes("payload bytes")) {
  throw new Error("Capture plan must not ask operators to record raw payload bytes in checked-in results.");
}

for (const label of labels) {
  requireIncludes(`Capture result row for ${label}`, capturePlan, [
    `| ${label} | pending | pending | pending | pending | pending | pending | blocked |`,
  ]);
}

requireIncludes("Capture plan metadata", capturePlan, [`- Feature count: ${labels.length}`]);

const resultsCheck = spawnSync(process.execPath, [resultsVerifierPath], {
  cwd: androidDir,
  encoding: "utf8",
  stdio: ["ignore", "pipe", "pipe"],
});
if (resultsCheck.status !== 0) {
  throw new Error(resultsCheck.stderr.trim() || resultsCheck.stdout.trim() || "Capture results verifier failed.");
}

const captureResultsJson = JSON.parse(captureResults);
for (const label of labels) {
  if (!captureResultsJson.features.some((feature) => feature.label === label && feature.androidEnablementDecision === "blocked")) {
    throw new Error(`Capture results missing blocked feature entry for ${label}.`);
  }
}

console.log("AirVision firmware capture plan renderer regression tests passed.");
