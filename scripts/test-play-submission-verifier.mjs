#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { readFile, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const handoffPath = join(androidDir, "play", "console-handoff.md");
const verifierArgs = ["scripts/verify-play-submission-package.mjs"];

function runVerifier() {
  return spawnSync(process.execPath, verifierArgs, {
    cwd: androidDir,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
}

function outputText(result) {
  return `${result.stdout}\n${result.stderr}`.trim();
}

const originalHandoff = await readFile(handoffPath, "utf8");

try {
  const passing = runVerifier();
  if (passing.status !== 0 || !outputText(passing).includes("Play Console handoff: verified")) {
    throw new Error(
      [
        "Expected submission verifier to accept the current generated Play Console handoff.",
        `status=${passing.status}`,
        outputText(passing),
      ].join("\n"),
    );
  }

  await writeFile(
    handoffPath,
    `${originalHandoff}\n<!-- stale handoff regression marker -->\n`,
  );
  const failing = runVerifier();
  if (failing.status === 0 || !outputText(failing).includes("Generated Play Console handoff is stale or invalid")) {
    throw new Error(
      [
        "Expected submission verifier to reject a stale generated Play Console handoff.",
        `status=${failing.status}`,
        outputText(failing),
      ].join("\n"),
    );
  }
} finally {
  await writeFile(handoffPath, originalHandoff);
}

const restored = runVerifier();
if (restored.status !== 0 || !outputText(restored).includes("Play Console handoff: verified")) {
  throw new Error(
    [
      "Expected submission verifier to pass again after restoring the Play Console handoff.",
      `status=${restored.status}`,
      outputText(restored),
    ].join("\n"),
  );
}

console.log("Play submission verifier regression tests passed.");
