#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { mkdtemp, readdir, readFile, rm, stat, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const handoffPath = join(androidDir, "play", "console-handoff.md");
const appContentPath = join(androidDir, "play", "app-content-answers.json");
const consoleChecklistPath = join(androidDir, "play", "console-checklist.md");
const releaseOutputDir = join(androidDir, "build", "release-bundles");
const verifierArgs = ["scripts/verify-play-submission-package.mjs"];
const evidenceTemplateArgs = ["scripts/render-play-console-evidence-template.mjs"];

function runVerifier(extraArgs = []) {
  return spawnSync(process.execPath, [...verifierArgs, ...extraArgs], {
    cwd: androidDir,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
}

function runHandoffRendererWithEmptyReleaseDir(emptyReleaseDir) {
  return spawnSync(process.execPath, ["scripts/render-play-console-handoff.mjs", "--check"], {
    cwd: androidDir,
    encoding: "utf8",
    env: {
      ...process.env,
      OPENCLAW_ANDROID_RELEASE_OUTPUT_DIR: emptyReleaseDir,
    },
    stdio: ["ignore", "pipe", "pipe"],
  });
}

function runEvidenceTemplate(extraArgs = [], expectedStatus = 0) {
  const result = spawnSync(process.execPath, [...evidenceTemplateArgs, ...extraArgs], {
    cwd: androidDir,
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
  if (result.status !== expectedStatus) {
    throw new Error(
      [
        `Evidence template renderer exited ${result.status}, expected ${expectedStatus}.`,
        result.stdout.trim(),
        result.stderr.trim(),
      ]
        .filter(Boolean)
        .join("\n"),
    );
  }
  return result;
}

function outputText(result) {
  return `${result.stdout}\n${result.stderr}`.trim();
}

async function hasSignedHudReleaseBundle() {
  const files = await readdir(releaseOutputDir).catch(() => []);
  for (const file of files) {
    if (!file.endsWith("-hud-release.aab")) continue;
    const info = await stat(join(releaseOutputDir, file)).catch(() => null);
    if (info?.isFile()) return true;
  }
  return false;
}

const originalHandoff = await readFile(handoffPath, "utf8");
const originalAppContent = await readFile(appContentPath, "utf8");
const originalConsoleChecklist = await readFile(consoleChecklistPath, "utf8");
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

  const cleanCheckoutHandoff = runHandoffRendererWithEmptyReleaseDir(join(tempDir, "missing-release-bundles"));
  if (
    cleanCheckoutHandoff.status !== 0 ||
    !outputText(cleanCheckoutHandoff).includes("Play Console handoff verified")
  ) {
    throw new Error(
      [
        "Expected Play Console handoff renderer to verify from checklist metadata without local signed AAB files.",
        `status=${cleanCheckoutHandoff.status}`,
        outputText(cleanCheckoutHandoff),
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

  if (await hasSignedHudReleaseBundle()) {
    await writeFile(
      handoffPath,
      originalHandoff.replace(/[a-f0-9]{64}/u, "0".repeat(64)),
    );
    const staleHandoffBundle = runVerifier();
    if (
      staleHandoffBundle.status === 0 ||
      !outputText(staleHandoffBundle).includes("Play Console handoff latest signed HUD AAB is missing required text")
    ) {
      throw new Error(
        [
          "Expected submission verifier to reject a stale signed HUD AAB checksum in the Play Console handoff.",
          `status=${staleHandoffBundle.status}`,
          outputText(staleHandoffBundle),
        ].join("\n"),
      );
    }
    await writeFile(handoffPath, originalHandoff);

    await writeFile(
      consoleChecklistPath,
      originalConsoleChecklist.replace(/[a-f0-9]{64}/u, "0".repeat(64)),
    );
    const staleChecklist = runVerifier();
    if (
      staleChecklist.status === 0 ||
      !outputText(staleChecklist).includes("Console checklist latest signed HUD AAB is missing required text")
    ) {
      throw new Error(
        [
          "Expected submission verifier to reject a stale signed HUD AAB checksum in the console checklist.",
          `status=${staleChecklist.status}`,
          outputText(staleChecklist),
        ].join("\n"),
      );
    }
    await writeFile(consoleChecklistPath, originalConsoleChecklist);
  }

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

  const missingCapabilityStatesAppContent = JSON.parse(originalAppContent);
  missingCapabilityStatesAppContent.reviewEvidence = {
    ...missingCapabilityStatesAppContent.reviewEvidence,
    airVisionCompanionCapabilityStates: [],
  };
  const missingCapabilityStatesPath = join(tempDir, "missing-airvision-capability-states-app-content.json");
  await writeFile(missingCapabilityStatesPath, `${JSON.stringify(missingCapabilityStatesAppContent, null, 2)}\n`);
  const missingCapabilityStates = runVerifier(["--app-content", missingCapabilityStatesPath]);
  if (
    missingCapabilityStates.status === 0 ||
    !outputText(missingCapabilityStates).includes("AirVision companion capability states must list")
  ) {
    throw new Error(
      [
        "Expected submission verifier to reject missing AirVision companion capability states.",
        `status=${missingCapabilityStates.status}`,
        outputText(missingCapabilityStates),
      ].join("\n"),
    );
  }

  const missingApplyMatrixAppContent = JSON.parse(originalAppContent);
  missingApplyMatrixAppContent.reviewEvidence = {
    ...missingApplyMatrixAppContent.reviewEvidence,
    airVisionWindowsApplyMatrixReview: [],
  };
  const missingApplyMatrixPath = join(tempDir, "missing-airvision-apply-matrix-app-content.json");
  await writeFile(missingApplyMatrixPath, `${JSON.stringify(missingApplyMatrixAppContent, null, 2)}\n`);
  const missingApplyMatrix = runVerifier(["--app-content", missingApplyMatrixPath]);
  if (
    missingApplyMatrix.status === 0 ||
    !outputText(missingApplyMatrix).includes("AirVision Windows apply matrix review must list exactly 12 feature rows")
  ) {
    throw new Error(
      [
        "Expected submission verifier to reject missing AirVision Windows apply matrix.",
        `status=${missingApplyMatrix.status}`,
        outputText(missingApplyMatrix),
      ].join("\n"),
    );
  }

  const missingEvidenceAppContent = JSON.parse(originalAppContent);
  missingEvidenceAppContent.finalSubmission = {
    ...missingEvidenceAppContent.finalSubmission,
    hostedPrivacyPolicyUrl: "https://example.com/openclaw-hud-privacy",
    appCreatedInPlayConsole: true,
    internalTestersConfiguredInPlayConsole: true,
    reviewerAccessConfiguredInPlayConsole: true,
    consoleEvidence: {},
  };
  const missingEvidencePath = join(tempDir, "missing-console-evidence-app-content.json");
  await writeFile(missingEvidencePath, `${JSON.stringify(missingEvidenceAppContent, null, 2)}\n`);
  const missingEvidence = runVerifier(["--app-content", missingEvidencePath, "--final", "--skip-hosted-privacy-url-fetch"]);
  if (
    missingEvidence.status === 0 ||
    !outputText(missingEvidence).includes("finalSubmission.consoleEvidence.appCreatedInPlayConsole")
  ) {
    throw new Error(
      [
        "Expected final verifier to reject completed Play Console blockers without evidence metadata.",
        `status=${missingEvidence.status}`,
        outputText(missingEvidence),
      ].join("\n"),
    );
  }

  const invalidEvidenceDate = runEvidenceTemplate(["--verified-at", "07/10/2026", "--json-only"], 1);
  if (!outputText(invalidEvidenceDate).includes("--verified-at must be a YYYY-MM-DD date")) {
    throw new Error(`Expected evidence template renderer to reject non-ISO dates:\n${outputText(invalidEvidenceDate)}`);
  }

  const evidenceTemplate = runEvidenceTemplate(["--verified-at", "2026-07-10", "--json-only"]);
  const renderedEvidence = JSON.parse(evidenceTemplate.stdout);
  const reviewerEvidenceNotes = renderedEvidence.reviewerAccessConfiguredInPlayConsole?.notes ?? "";
  if (!reviewerEvidenceNotes.includes("account-free Demo Mode")) {
    throw new Error(`Expected reviewer evidence notes to mention account-free Demo Mode:\n${reviewerEvidenceNotes}`);
  }
  const completeEvidenceAppContent = JSON.parse(originalAppContent);
  completeEvidenceAppContent.aiGeneratedContent = {
    ...completeEvidenceAppContent.aiGeneratedContent,
    inAppReportingImplemented: true,
  };
  completeEvidenceAppContent.finalSubmission = {
    ...completeEvidenceAppContent.finalSubmission,
    hostedPrivacyPolicyUrl: "https://example.com/openclaw-hud-privacy",
    appCreatedInPlayConsole: true,
    internalTestersConfiguredInPlayConsole: true,
    reviewerAccessConfiguredInPlayConsole: true,
    consoleEvidence: renderedEvidence,
  };
  const completeEvidencePath = join(tempDir, "complete-console-evidence-app-content.json");
  await writeFile(completeEvidencePath, `${JSON.stringify(completeEvidenceAppContent, null, 2)}\n`);
  const completeEvidence = runVerifier([
    "--app-content",
    completeEvidencePath,
    "--final",
    "--skip-hosted-privacy-url-fetch",
  ]);
  if (
    completeEvidence.status !== 0 ||
    !outputText(completeEvidence).includes("Play submission package verifier passed (final mode)")
  ) {
    throw new Error(
      [
        "Expected final verifier to accept evidence generated by render-play-console-evidence-template.mjs.",
        `status=${completeEvidence.status}`,
        outputText(completeEvidence),
      ].join("\n"),
    );
  }

  const missingAiReport = runVerifier(["--final", "--skip-hosted-privacy-url-fetch"]);
  if (
    missingAiReport.status === 0 ||
    !outputText(missingAiReport).includes("aiGeneratedContent.inAppReportingImplemented")
  ) {
    throw new Error(
      [
        "Expected final verifier to reject a release without in-app AI-content reporting.",
        `status=${missingAiReport.status}`,
        outputText(missingAiReport),
      ].join("\n"),
    );
  }
} finally {
  await writeFile(handoffPath, originalHandoff);
  await writeFile(appContentPath, originalAppContent);
  await writeFile(consoleChecklistPath, originalConsoleChecklist);
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
