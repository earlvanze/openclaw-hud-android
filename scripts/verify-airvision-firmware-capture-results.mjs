#!/usr/bin/env node

import { readFile, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const sourcePath = join(androidDir, "app/src/main/java/ai/openclaw/app/AirVisionUsbController.kt");
const defaultResultsPath = join(androidDir, "play/airvision-firmware-capture-results.json");

function usage() {
  console.log(`Usage: node scripts/verify-airvision-firmware-capture-results.mjs [--results PATH] [--write-template] [--check]

Validates the sanitized AirVision M1 firmware capture results used to decide
whether Android firmware writes may be enabled. The committed results file must
not contain raw USBPcap dumps, raw USB serial numbers, auth tokens, or temporary
review credentials.`);
}

function parseArgs(argv) {
  const out = {
    resultsPath: defaultResultsPath,
    writeTemplate: false,
    check: false,
  };
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--results") {
      out.resultsPath = argv[++i];
      if (!out.resultsPath) throw new Error("--results requires a path.");
    } else if (arg === "--write-template") {
      out.writeTemplate = true;
    } else if (arg === "--check") {
      out.check = true;
    } else if (arg === "-h" || arg === "--help") {
      usage();
      process.exit(0);
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }
  return out;
}

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
    const [, enumName, rawKey, label, androidStatus, probeSource] = match;
    const probeValues = [...probeSource.matchAll(/"([^"]+)"/g)].map((probeMatch) => probeMatch[1]);
    if (probeValues.length === 0) {
      throw new Error(`No capture probe values parsed for ${enumName}.`);
    }
    features.push({ enumName, rawKey, label, androidStatus, probeValues });
  }
  if (features.length === 0) throw new Error("No AirVision firmware capture features parsed.");
  return features;
}

function templateFor(features) {
  return {
    schema: "openclaw.airvision.firmwareCaptureResults",
    version: 1,
    payloadPolicy:
      "Sanitized summaries only. Do not commit raw USBPcap dumps, raw USB serial numbers, auth tokens, or temporary review credentials.",
    source: {
      windowsHost: "Cyber",
      captureTool: "USBPcap/Wireshark",
      asusAirVisionAppVersion: null,
      androidDiagnosticsExportSha256: null,
      notes: "Fill after a Windows ASUS AirVision capture session.",
    },
    features: features.map((feature) => ({
      rawKey: feature.rawKey,
      label: feature.label,
      status: "pending",
      probeValues: feature.probeValues,
      writeReportId: null,
      writeEndpoint: null,
      writePayloadSummary: null,
      readbackReportId: null,
      readbackEndpoint: null,
      readbackPayloadSummary: null,
      checksumFramingNotes: null,
      visibleStateConfirmed: false,
      captureReferences: [],
      androidEnablementDecision: "blocked",
      blockerReason: "Windows ASUS HID protocol capture has not been validated.",
    })),
  };
}

function stableStringify(value) {
  return `${JSON.stringify(value, null, 2)}\n`;
}

function assertObject(value, label) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error(`${label} must be an object.`);
  }
}

function assertStringOrNull(value, label) {
  if (value !== null && typeof value !== "string") throw new Error(`${label} must be a string or null.`);
}

function assertArray(value, label) {
  if (!Array.isArray(value)) throw new Error(`${label} must be an array.`);
}

function isFilled(value) {
  return typeof value === "string" && value.trim().length > 0 && value.trim().toLowerCase() !== "pending";
}

function containsForbiddenSecretShape(text) {
  return /(token|password|secret|signaturekey|authorization|serial(?:number)?)\s*[:=]/i.test(text);
}

function containsRawHexDumpShape(text) {
  return /(?:\b[0-9a-fA-F]{2}\b[\s,;:-]*){16,}/.test(text);
}

function hasRawCaptureDumpExtension(value) {
  return /\.(pcap|pcapng|usbpcap|etl|cap)$/i.test(value.trim());
}

function assertSanitizedText(value, label) {
  if (typeof value !== "string") return;
  if (containsForbiddenSecretShape(value)) {
    throw new Error(`${label} contains a secret or raw-serial-shaped assignment; keep private values out of capture results.`);
  }
  if (containsRawHexDumpShape(value)) {
    throw new Error(`${label} looks like a raw byte dump; keep only sanitized summaries in capture results.`);
  }
}

function validateCaptureReferences(references, label) {
  assertArray(references, `${label}.captureReferences`);
  for (const [index, reference] of references.entries()) {
    assertObject(reference, `${label}.captureReferences[${index}]`);
    assertStringOrNull(reference.file, `${label}.captureReferences[${index}].file`);
    assertStringOrNull(reference.sha256, `${label}.captureReferences[${index}].sha256`);
    assertStringOrNull(reference.notes, `${label}.captureReferences[${index}].notes`);
    if (reference.sha256 !== null && !/^[a-f0-9]{64}$/i.test(reference.sha256)) {
      throw new Error(`${label}.captureReferences[${index}].sha256 must be a SHA-256 hex digest or null.`);
    }
    if (typeof reference.file === "string" && hasRawCaptureDumpExtension(reference.file)) {
      throw new Error(`${label}.captureReferences[${index}].file must reference a sanitized summary, not a raw capture dump.`);
    }
    assertSanitizedText(reference.file, `${label}.captureReferences[${index}].file`);
    assertSanitizedText(reference.notes, `${label}.captureReferences[${index}].notes`);
  }
}

function validateResults(results, features) {
  assertObject(results, "Capture results");
  if (results.schema !== "openclaw.airvision.firmwareCaptureResults") {
    throw new Error("Capture results schema must be openclaw.airvision.firmwareCaptureResults.");
  }
  if (results.version !== 1) throw new Error("Capture results version must be 1.");
  assertStringOrNull(results.payloadPolicy, "payloadPolicy");
  assertObject(results.source, "source");
  for (const field of ["windowsHost", "captureTool", "asusAirVisionAppVersion", "androidDiagnosticsExportSha256", "notes"]) {
    assertStringOrNull(results.source[field], `source.${field}`);
    assertSanitizedText(results.source[field], `source.${field}`);
  }
  if (
    results.source.androidDiagnosticsExportSha256 !== null &&
    !/^[a-f0-9]{64}$/i.test(results.source.androidDiagnosticsExportSha256)
  ) {
    throw new Error("source.androidDiagnosticsExportSha256 must be a SHA-256 hex digest or null.");
  }

  assertArray(results.features, "features");
  const expectedByKey = new Map(features.map((feature) => [feature.rawKey, feature]));
  const seen = new Set();
  const errors = [];

  for (const [index, result] of results.features.entries()) {
    const label = `features[${index}]`;
    try {
      assertObject(result, label);
      if (!expectedByKey.has(result.rawKey)) throw new Error(`${label}.rawKey is unknown: ${result.rawKey}`);
      if (seen.has(result.rawKey)) throw new Error(`${label}.rawKey is duplicated: ${result.rawKey}`);
      seen.add(result.rawKey);

      const expected = expectedByKey.get(result.rawKey);
      if (result.label !== expected.label) throw new Error(`${label}.label must be ${expected.label}.`);
      assertArray(result.probeValues, `${label}.probeValues`);
      if (JSON.stringify(result.probeValues) !== JSON.stringify(expected.probeValues)) {
        throw new Error(`${label}.probeValues must match AirVisionFirmwareFeature.`);
      }

      if (!["pending", "captured", "validated"].includes(result.status)) {
        throw new Error(`${label}.status must be pending, captured, or validated.`);
      }
      if (!["blocked", "enable_android_write"].includes(result.androidEnablementDecision)) {
        throw new Error(`${label}.androidEnablementDecision must be blocked or enable_android_write.`);
      }
      for (const field of [
        "writeReportId",
        "writeEndpoint",
        "writePayloadSummary",
        "readbackReportId",
        "readbackEndpoint",
        "readbackPayloadSummary",
        "checksumFramingNotes",
        "blockerReason",
      ]) {
        assertStringOrNull(result[field], `${label}.${field}`);
        assertSanitizedText(result[field], `${label}.${field}`);
      }
      if (typeof result.visibleStateConfirmed !== "boolean") {
        throw new Error(`${label}.visibleStateConfirmed must be boolean.`);
      }
      validateCaptureReferences(result.captureReferences, label);

      const rawJson = JSON.stringify(result);
      if (containsForbiddenSecretShape(rawJson)) {
        throw new Error(`${label} contains a secret or raw-serial-shaped assignment; keep private values out of capture results.`);
      }

      const hasCompleteEvidence =
        result.status === "validated" &&
        isFilled(result.writeReportId) &&
        isFilled(result.writeEndpoint) &&
        isFilled(result.writePayloadSummary) &&
        isFilled(result.readbackReportId) &&
        isFilled(result.readbackEndpoint) &&
        isFilled(result.readbackPayloadSummary) &&
        isFilled(result.checksumFramingNotes) &&
        result.visibleStateConfirmed === true &&
        result.captureReferences.length > 0;

      if (result.androidEnablementDecision === "enable_android_write" && !hasCompleteEvidence) {
        throw new Error(`${label} cannot enable Android writes without validated write/readback/checksum/visible-state evidence.`);
      }
      if (result.androidEnablementDecision === "blocked" && !isFilled(result.blockerReason)) {
        throw new Error(`${label}.blockerReason is required while Android enablement is blocked.`);
      }
    } catch (error) {
      errors.push(error.message);
    }
  }

  for (const feature of features) {
    if (!seen.has(feature.rawKey)) errors.push(`features missing required rawKey: ${feature.rawKey}`);
  }

  if (errors.length > 0) {
    throw new Error(`AirVision firmware capture results failed validation:\n- ${errors.join("\n- ")}`);
  }
}

const args = parseArgs(process.argv.slice(2));
const source = await readFile(sourcePath, "utf8");
const features = parseCaptureFeatures(source);
const renderedTemplate = stableStringify(templateFor(features));

if (args.writeTemplate) {
  await writeFile(args.resultsPath, renderedTemplate);
  console.log(`AirVision firmware capture results template written to ${args.resultsPath}`);
  process.exit(0);
}

const text = await readFile(args.resultsPath, "utf8");
const results = JSON.parse(text);
validateResults(results, features);

if (args.check && args.resultsPath === defaultResultsPath && text !== renderedTemplate) {
  console.log(`AirVision firmware capture results verified with validated capture data at ${args.resultsPath}`);
} else {
  console.log(`AirVision firmware capture results verified at ${args.resultsPath}`);
}
