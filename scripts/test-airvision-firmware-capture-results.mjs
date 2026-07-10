#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptPath = fileURLToPath(new URL("./verify-airvision-firmware-capture-results.mjs", import.meta.url));
const templatePath = fileURLToPath(new URL("../play/airvision-firmware-capture-results.json", import.meta.url));

function runVerifier(resultsPath) {
  return spawnSync(process.execPath, [scriptPath, "--results", resultsPath], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
}

function expectFailure(label, result, needle) {
  if (result.status === 0) {
    throw new Error(`${label} unexpectedly passed.`);
  }
  const output = `${result.stdout}\n${result.stderr}`;
  if (!output.includes(needle)) {
    throw new Error(`${label} failed without expected text ${JSON.stringify(needle)}.\n${output}`);
  }
}

function expectSuccess(label, result) {
  if (result.status !== 0) {
    throw new Error(`${label} failed unexpectedly.\n${result.stdout}\n${result.stderr}`);
  }
}

const tempDir = await mkdtemp(join(tmpdir(), "openclaw-airvision-results-"));
try {
  const template = JSON.parse(await readFile(templatePath, "utf8"));

  const validPath = join(tempDir, "valid.json");
  await writeFile(validPath, `${JSON.stringify(template, null, 2)}\n`);
  expectSuccess("pending template", runVerifier(validPath));

  const missingFeature = structuredClone(template);
  missingFeature.features = missingFeature.features.filter((feature) => feature.rawKey !== "ipd");
  const missingPath = join(tempDir, "missing-feature.json");
  await writeFile(missingPath, `${JSON.stringify(missingFeature, null, 2)}\n`);
  expectFailure("missing feature", runVerifier(missingPath), "features missing required rawKey: ipd");

  const unsafeEnablement = structuredClone(template);
  unsafeEnablement.features[0].androidEnablementDecision = "enable_android_write";
  const unsafePath = join(tempDir, "unsafe-enablement.json");
  await writeFile(unsafePath, `${JSON.stringify(unsafeEnablement, null, 2)}\n`);
  expectFailure("unsafe enablement", runVerifier(unsafePath), "cannot enable Android writes without validated");

  const secretShaped = structuredClone(template);
  secretShaped.features[0].blockerReason = "token=abc123 should not be committed";
  const secretPath = join(tempDir, "secret-shaped.json");
  await writeFile(secretPath, `${JSON.stringify(secretShaped, null, 2)}\n`);
  expectFailure("secret-shaped value", runVerifier(secretPath), "secret-shaped assignment");
} finally {
  await rm(tempDir, { recursive: true, force: true });
}

console.log("AirVision firmware capture results verifier tests passed.");
