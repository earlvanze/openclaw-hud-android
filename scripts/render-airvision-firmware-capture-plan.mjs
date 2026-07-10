#!/usr/bin/env node

import { readFile, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const sourcePath = join(androidDir, "app/src/main/java/ai/openclaw/app/AirVisionUsbController.kt");
const outputPath = join(androidDir, "play/airvision-firmware-capture-plan.md");

function parseCaptureFeatures(source) {
  const enumStart = source.indexOf("enum class AirVisionFirmwareFeature");
  const enumEnd = source.indexOf("data class AirVisionFirmwareFeatureReadiness", enumStart);
  if (enumStart < 0 || enumEnd < 0) {
    throw new Error("Could not locate AirVisionFirmwareFeature enum.");
  }

  const enumSource = source.slice(enumStart, enumEnd);
  const entryPattern =
    /(\w+)\(\s*rawValue\s*=\s*"([^"]+)",\s*label\s*=\s*"([^"]+)",[\s\S]*?androidStatus\s*=\s*"([^"]+)",\s*captureProbeValues\s*=\s*listOf\(([^)]*)\)/g;
  const features = [];
  let match;
  while ((match = entryPattern.exec(enumSource)) !== null) {
    const [, enumName, rawValue, label, androidStatus, probeSource] = match;
    const probeValues = [...probeSource.matchAll(/"([^"]+)"/g)].map((probeMatch) => probeMatch[1]);
    if (probeValues.length === 0) {
      throw new Error(`No capture probe values parsed for ${enumName}.`);
    }
    features.push({ enumName, rawValue, label, androidStatus, probeValues });
  }

  if (features.length === 0) {
    throw new Error("No AirVision firmware capture features parsed.");
  }
  return features;
}

function tableEscape(value) {
  return value.replace(/\|/g, "\\|");
}

function render(features) {
  const lines = [
    "# AirVision M1 Firmware Protocol Capture Plan",
    "",
    "Generated from `AirVisionFirmwareFeature` in `AirVisionUsbController.kt`. Run `node scripts/render-airvision-firmware-capture-plan.mjs --check` before capture work or release prep.",
    "",
    "## Capture Setup",
    "",
    "- Keep the AirVision M1 connected to the Windows/Cyber machine running the ASUS AirVision app when capturing vendor traffic.",
    "- Capture USB traffic while changing one AirVision setting at a time through the probe values below.",
    "- Export the Android HUD AirVision diagnostics JSON before and after the capture session, then compare its readable/writable HID report-path summaries against the USBPcap endpoints.",
    "- Keep Android firmware writes disabled until the vendor report payload, report ID, length, and checksum behavior are validated from captures.",
    "- Do not commit raw USB serial numbers, raw private capture payloads, or temporary review/demo credentials.",
    "",
    "## Capture Acceptance Criteria",
    "",
    "- Record the exact Windows write report ID, payload bytes, payload length, and endpoint for each feature before Android sends any vendor report.",
    "- Record any matching readback report ID, payload bytes, endpoint, and timing after each Windows setting change.",
    "- Identify checksum, framing, sequence, or padding behavior from at least two distinct probe values for the same feature.",
    "- Confirm the ASUS app UI and the M1 visible state changed as expected for every captured probe value.",
    "- Keep Android enablement as `blocked` until write, readback, checksum/framing, and visible-state evidence agree.",
    "",
    "## Probe Matrix",
    "",
    "| Feature | Raw key | Android status | Probe values |",
    "| --- | --- | --- | --- |",
    ...features.map(
      (feature) =>
        `| ${tableEscape(feature.label)} | \`${feature.rawValue}\` | ${tableEscape(feature.androidStatus)} | ${tableEscape(feature.probeValues.join(" -> "))} |`,
    ),
    "",
    "## Per-Feature Capture Checklist",
    "",
    ...features.flatMap((feature) => [
      `### ${feature.label}`,
      "",
      `- Raw key: \`${feature.rawValue}\``,
      `- Probe values: ${feature.probeValues.join(" -> ")}`,
      "- Capture notes:",
      "  - Start a fresh USB capture.",
      `  - Change ${feature.label} through the probe values in order, pausing briefly after each value.`,
      "  - Save the capture with the feature key and value sequence in the filename.",
      "  - Record any matching writable HID report path from the Android diagnostics export.",
      "",
    ]),
    "## Capture Result Template",
    "",
    "| Feature | Write report ID | Write payload bytes | Readback report ID | Readback payload bytes | Checksum/framing notes | ASUS UI + M1 visible confirmation | Android enablement decision |",
    "| --- | --- | --- | --- | --- | --- | --- | --- |",
    ...features.map(
      (feature) =>
        `| ${tableEscape(feature.label)} | pending | pending | pending | pending | pending | pending | blocked |`,
    ),
    "",
    "## Generated Metadata",
    "",
    `- Feature count: ${features.length}`,
    "",
  ];
  return `${lines.join("\n")}`;
}

const check = process.argv.includes("--check");
const source = await readFile(sourcePath, "utf8");
const rendered = render(parseCaptureFeatures(source));

if (check) {
  const existing = await readFile(outputPath, "utf8").catch(() => null);
  if (existing !== rendered) {
    throw new Error(`Generated AirVision firmware capture plan is stale: ${outputPath}`);
  }
  console.log(`AirVision firmware capture plan verified at ${outputPath}`);
} else {
  await writeFile(outputPath, rendered);
  console.log(`AirVision firmware capture plan rendered at ${outputPath}`);
}
