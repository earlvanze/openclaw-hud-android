#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const handoffPath = join(androidDir, "play", "console-handoff.md");
const appContentPath = join(androidDir, "play", "app-content-answers.json");
const verifierArgs = ["scripts/verify-play-submission-package.mjs"];

function runVerifier(extraArgs = []) {
  return spawnSync(process.execPath, [...verifierArgs, ...extraArgs], {
    cwd: androidDir,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
}

function outputText(result) {
  return `${result.stdout}\n${result.stderr}`.trim();
}

const originalHandoff = await readFile(handoffPath, "utf8");
const originalAppContent = await readFile(appContentPath, "utf8");
const tempDir = await mkdtemp(join(tmpdir(), "openclaw-play-submission-verifier-"));

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

  await writeFile(handoffPath, originalHandoff);

  const appContent = JSON.parse(originalAppContent);
  appContent.sensitivePermissions = {
    ...appContent.sensitivePermissions,
    unsupportedFuturePermission: true,
  };
  const unsupportedAppContentPath = join(tempDir, "unsupported-app-content.json");
  await writeFile(unsupportedAppContentPath, `${JSON.stringify(appContent, null, 2)}\n`);
  const unknownSensitivePermission = runVerifier(["--app-content", unsupportedAppContentPath]);
  if (
    unknownSensitivePermission.status === 0 ||
    !outputText(unknownSensitivePermission).includes("unknown sensitive permission groups")
  ) {
    throw new Error(
      [
        "Expected submission verifier to reject unknown sensitive permission groups.",
        `status=${unknownSensitivePermission.status}`,
        outputText(unknownSensitivePermission),
      ].join("\n"),
    );
  }
} finally {
  await writeFile(handoffPath, originalHandoff);
  await writeFile(appContentPath, originalAppContent);
  await rm(tempDir, { recursive: true, force: true });
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
