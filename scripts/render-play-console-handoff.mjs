#!/usr/bin/env node

import { createHash } from "node:crypto";
import { readdir, readFile, stat, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const outputPath = join(androidDir, "play", "console-handoff.md");
const releaseOutputDir = process.env.OPENCLAW_ANDROID_RELEASE_OUTPUT_DIR?.trim()
  ? process.env.OPENCLAW_ANDROID_RELEASE_OUTPUT_DIR.trim()
  : join(androidDir, "build", "release-bundles");
const consoleChecklistPath = join(androidDir, "play", "console-checklist.md");
const buildGradlePath = join(androidDir, "app", "build.gradle.kts");
const manifestPath = join(
  androidDir,
  "app",
  "build",
  "intermediates",
  "packaged_manifests",
  "hudRelease",
  "processHudReleaseManifestForPackage",
  "AndroidManifest.xml",
);

async function readText(path) {
  return readFile(join(androidDir, path), "utf8");
}

async function readTrimmed(path) {
  return (await readText(path)).trim();
}

function checkbox(value) {
  return value ? "[x]" : "[ ]";
}

function jsonBlock(value) {
  return ["```json", JSON.stringify(value, null, 2), "```"].join("\n");
}

function evidenceLine(evidence) {
  if (evidence === null || typeof evidence !== "object" || Array.isArray(evidence)) {
    return "evidence: not recorded";
  }
  const parts = [
    evidence.verifiedAt?.trim() ? `verifiedAt=${evidence.verifiedAt.trim()}` : null,
    evidence.source?.trim() ? `source=${evidence.source.trim()}` : null,
    evidence.notes?.trim() ? `notes=${evidence.notes.trim()}` : null,
  ].filter(Boolean);
  return parts.length > 0 ? parts.join("; ") : "evidence: not recorded";
}

function linesFromArray(values, prefix = "- ") {
  if (!Array.isArray(values)) return [];
  return values.map((value) => `${prefix}${value}`);
}

function capabilityStateLines(values) {
  if (!Array.isArray(values)) return [];
  return values.flatMap((value) => [
    `- ${value.capability}: ${value.state}`,
    `  Review path: ${value.reviewPath}`,
    `  Evidence: ${value.evidence}`,
  ]);
}

async function latestSignedHudReleaseBundle() {
  const files = await readdir(releaseOutputDir).catch(() => []);
  const candidates = [];
  for (const file of files) {
    if (!file.endsWith("-hud-release.aab")) continue;
    const path = join(releaseOutputDir, file);
    const info = await stat(path);
    if (info.isFile()) {
      candidates.push({
        file,
        path,
        relativePath: `build/release-bundles/${file}`,
        size: info.size,
        mtimeMs: info.mtimeMs,
      });
    }
  }
  candidates.sort((a, b) => b.mtimeMs - a.mtimeMs);
  const bundle = candidates[0] ?? null;
  if (!bundle) return null;
  return {
    ...bundle,
    source: "local-bundle",
    sha256: createHash("sha256").update(await readFile(bundle.path)).digest("hex"),
  };
}

async function signedHudReleaseBundleFromChecklist() {
  const checklist = await readFile(consoleChecklistPath, "utf8").catch(() => "");
  const relativePath = checklist.match(/`(build\/release-bundles\/[^`]+-hud-release\.aab)`/u)?.[1] ?? null;
  const sha256 = checklist.match(/`([a-f0-9]{64})`/u)?.[1] ?? null;
  if (!relativePath || !sha256) return null;
  return {
    relativePath,
    sha256,
    source: "console-checklist",
  };
}

async function signedHudReleaseBundle() {
  return (await latestSignedHudReleaseBundle()) ?? (await signedHudReleaseBundleFromChecklist());
}

async function hudManifestVersion() {
  const manifest = await readFile(manifestPath, "utf8").catch(() => "");
  const gradle = await readFile(buildGradlePath, "utf8").catch(() => "");
  return {
    versionName:
      manifest.match(/\bandroid:versionName\s*=\s*"([^"]+)"/u)?.[1] ??
      gradle.match(/\bversionName\s*=\s*"([^"]+)"/u)?.[1] ??
      "unknown",
    versionCode:
      manifest.match(/\bandroid:versionCode\s*=\s*"([^"]+)"/u)?.[1] ??
      gradle.match(/\bversionCode\s*=\s*(\d+)/u)?.[1] ??
      "unknown",
  };
}

async function render() {
  const appContent = JSON.parse(await readText("play/app-content-answers.json"));
  const screenshotManifest = JSON.parse(await readText("play/screenshots/phone/manifest.json"));
  const bundle = await signedHudReleaseBundle();
  const version = await hudManifestVersion();
  const title = await readTrimmed("play/listings/en-US/title.txt");
  const shortDescription = await readTrimmed("play/listings/en-US/short-description.txt");
  const fullDescription = await readTrimmed("play/listings/en-US/full-description.txt");
  const releaseNotes = await readTrimmed("play/listings/en-US/release-notes.txt");
  const finalSubmission = appContent.finalSubmission ?? {};
  const consoleEvidence = finalSubmission.consoleEvidence ?? {};
  const reviewEvidence = appContent.reviewEvidence ?? {};

  const blockers = [
    ["Create app in Play Console", "appCreatedInPlayConsole", finalSubmission.appCreatedInPlayConsole === true],
    [
      "Configure internal testers",
      "internalTestersConfiguredInPlayConsole",
      finalSubmission.internalTestersConfiguredInPlayConsole === true,
    ],
    [
      "Configure reviewer app access",
      "reviewerAccessConfiguredInPlayConsole",
      finalSubmission.reviewerAccessConfiguredInPlayConsole === true,
    ],
  ];

  return `${[
    "# Google Play Console Handoff",
    "",
    "Generated from repository sources. Run `node scripts/render-play-console-handoff.mjs --check` before publishing.",
    "",
    "## Package",
    "",
    `- Package name: \`${appContent.packageName}\``,
    "- Initial track: `internal`",
    "- Initial release status: `draft`",
    "- OAuth publisher accounts allowed by helper: `earlvanze@gmail.com`, `earl@earlbnb.com`",
    "",
    "## Signed Bundle",
    "",
    bundle
      ? `- AAB: \`${bundle.relativePath}\``
      : "- AAB: missing; run `node scripts/build-release-aab.mjs --flavor hud --skip-version-bump`",
    bundle ? `- SHA-256: \`${bundle.sha256}\`` : "- SHA-256: missing",
    `- Version: ${version.versionName} (${version.versionCode})`,
    "",
    "## Remaining Console Blockers",
    "",
    ...blockers.map(([label, key, done]) => `- ${checkbox(done)} ${label} (${evidenceLine(consoleEvidence[key])})`),
    "",
    "These flags and evidence entries live in `play/app-content-answers.json` under `finalSubmission`. Flip a flag only after the matching Google Play Console setup is complete, and record `source`, `verifiedAt`, and `notes` evidence before final publish.",
    "",
    "## Store Listing",
    "",
    `Title: ${title}`,
    "",
    `Short description: ${shortDescription}`,
    "",
    "Full description:",
    "",
    fullDescription,
    "",
    "Release notes:",
    "",
    releaseNotes,
    "",
    "## Privacy Policy",
    "",
    `- Hosted URL: ${finalSubmission.hostedPrivacyPolicyUrl || appContent.privacyPolicy.hostedUrlCandidate}`,
    `- Source file: \`${appContent.privacyPolicy.sourceFile}\``,
    `- Hosted page source: \`${appContent.privacyPolicy.hostedPageSource}\``,
    `- In-app location: ${appContent.privacyPolicy.inAppLocation}`,
    "",
    "## Screenshots",
    "",
    "Use the tracked fallback screenshots when live Fold/M1 capture is unavailable. They are generated by `node scripts/render-play-screenshots.mjs` and validated by CI.",
    "",
    ...screenshotManifest.screenshots.map((screenshot) => `- \`${screenshot}\``),
    "",
    "## App Access",
    "",
    appContent.appAccess.reviewAccessInstructions,
    "",
    "## AirVision Companion Review Evidence",
    "",
    "These steps exercise the Windows-like AirVision companion controls that can be reviewed from the Android HUD build without a live gateway or live M1.",
    "",
    ...linesFromArray(reviewEvidence.airVisionCompanionReviewSteps),
    "",
    "Capability states:",
    "",
    ...capabilityStateLines(reviewEvidence.airVisionCompanionCapabilityStates),
    "",
    reviewEvidence.airVisionCompanionReviewNotes ?? "",
    "",
    "Reviewer evidence sources:",
    "",
    `- Release verifier: \`${reviewEvidence.releaseVerifier}\``,
    `- Submission verifier: \`${reviewEvidence.submissionVerifier}\``,
    `- Screenshot capture: \`${reviewEvidence.screenshotCapture}\``,
    `- CI workflow: \`${reviewEvidence.ciWorkflow}\``,
    "",
    "## App Content Answers",
    "",
    `- Ads: ${appContent.ads.containsAds ? "contains ads" : "does not contain ads"}`,
    `- Target audience: ${appContent.targetAudience.recommendedAgeGroups.join(", ")}`,
    `- Designed for children: ${appContent.targetAudience.designedForChildren ? "yes" : "no"}`,
    `- Content rating category: ${appContent.contentRating.category}`,
    `- Account creation in app: ${appContent.dataDeletion.accountCreationInApp ? "yes" : "no"}`,
    `- Data deletion path: ${appContent.dataDeletion.userDeletionPath}`,
    `- Data sale: ${appContent.dataSafety.sellsData ? "yes" : "no"}`,
    `- Data used for advertising: ${appContent.dataSafety.dataUsedForAdvertising ? "yes" : "no"}`,
    `- Data encrypted in transit answer: ${appContent.dataSafety.dataEncryptedInTransitAnswer}`,
    "",
    "Collected data:",
    "",
    ...appContent.dataSafety.collectedData.map(
      (entry) =>
        `- ${entry.playType}: ${entry.optional ? "optional" : "required"}; purpose ${entry.purpose.join(", ")}; ${entry.whereUsed}`,
    ),
    "",
    "Not collected:",
    "",
    ...appContent.dataSafety.notCollected.map((entry) => `- ${entry}`),
    "",
    "Sensitive permission declarations:",
    "",
    jsonBlock(appContent.sensitivePermissions),
    "",
    "## Local Gates",
    "",
    "- `node scripts/render-airvision-firmware-capture-plan.mjs --check`",
    "- `node scripts/verify-airvision-firmware-capture-results.mjs`",
    "- `node scripts/test-airvision-firmware-capture-results.mjs`",
    "- `node scripts/test-airvision-firmware-capture-plan-renderer.mjs`",
    "- `node scripts/test-install-launch-hud.mjs`",
    "- `node scripts/render-play-screenshots.mjs --check`",
    "- `node scripts/test-play-screenshot-tools.mjs`",
    "- `node scripts/test-play-publish-helper.mjs`",
    "- `node scripts/render-privacy-policy-site.mjs --check`",
    "- `node scripts/verify-play-submission-package.mjs`",
    "- `node scripts/test-play-submission-verifier.mjs`",
    "- `node scripts/render-play-console-evidence-template.mjs --verified-at YYYY-MM-DD --json-only` after external Console blockers are complete",
    "- `node scripts/publish-play-internal.mjs --dry-run`",
    "- `node scripts/verify-play-submission-package.mjs --final` after Console blockers are complete",
    "- `node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account earlvanze@gmail.com --auth-check` after OAuth login",
    "- `node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account earl@earlbnb.com --auth-check` after OAuth login",
    "- `node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account <allowed-account> --preflight` after the Play Console app exists",
    "- `node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account <allowed-account> --commit` for the first internal draft upload",
    "",
  ].join("\n")}\n`;
}

const check = process.argv.includes("--check");
const rendered = await render();

if (check) {
  const existing = await readFile(outputPath, "utf8").catch(() => null);
  if (existing !== rendered) {
    throw new Error(`Generated Play Console handoff is stale: ${outputPath}`);
  }
  console.log(`Play Console handoff verified at ${outputPath}`);
} else {
  await writeFile(outputPath, rendered);
  console.log(`Play Console handoff rendered at ${outputPath}`);
}
