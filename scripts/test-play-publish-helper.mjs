#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { chmod, mkdir, mkdtemp, rm, utimes, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { delimiter } from "node:path";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const publishScript = join("scripts", "publish-play-internal.mjs");
const readinessScript = join("scripts", "report-play-readiness.mjs");
const allowedAccount = "earlvanze@gmail.com";
const secondaryAllowedAccount = "earl@earlbnb.com";
const serviceAccount = "rclone@sacred-result-442018-v2.iam.gserviceaccount.com";

function runPublish(args, env, expectedStatus = 0) {
  const result = spawnSync(process.execPath, [publishScript, ...args], {
    cwd: androidDir,
    encoding: "utf8",
    env,
    stdio: ["ignore", "pipe", "pipe"],
  });
  if (result.status !== expectedStatus) {
    throw new Error(
      [
        `publish helper exited ${result.status}, expected ${expectedStatus}.`,
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
  return `${result.stdout}\n${result.stderr}`;
}

async function main() {
  const tempDir = await mkdtemp(join(tmpdir(), "openclaw-play-publish-helper-"));
  try {
    const fakeBin = join(tempDir, "bin");
    const fakeGcloud = join(fakeBin, "gcloud");
    await mkdir(fakeBin);
    await writeFile(
      fakeGcloud,
      `#!/usr/bin/env bash
set -euo pipefail
scenario="\${OPENCLAW_FAKE_GCLOUD_SCENARIO:-missing-allowed}"
allowed="${allowedAccount}"
secondary="${secondaryAllowedAccount}"
service="${serviceAccount}"

if [[ "$1 $2" == "auth list" ]]; then
  if [[ "$scenario" == "allowed-authenticated" ]]; then
    printf '[{"account":"%s","status":"ACTIVE"}]\\n' "$allowed"
  elif [[ "$scenario" == "secondary-allowed-authenticated" ]]; then
    printf '[{"account":"%s","status":"ACTIVE"}]\\n' "$secondary"
  else
    printf '[{"account":"%s","status":"ACTIVE"}]\\n' "$service"
  fi
  exit 0
fi

if [[ "$1 $2 $3" == "config get-value account" ]]; then
  printf '%s\\n' "$service"
  exit 0
fi

if [[ "$1 $2" == "auth print-access-token" ]]; then
  printf 'fake-token-for-%s\\n' "\${3:-active}"
  exit 0
fi

printf 'unexpected gcloud args: %s\\n' "$*" >&2
exit 64
`,
    );
    await chmod(fakeGcloud, 0o755);

    const env = {
      ...process.env,
      PATH: `${fakeBin}${delimiter}${process.env.PATH}`,
      GOOGLE_PLAY_SERVICE_ACCOUNT_JSON: "",
      GOOGLE_APPLICATION_CREDENTIALS: "",
      GOOGLE_PLAY_AUTH: "gcloud",
    };

    const staleBundle = join(tempDir, "stale-hud-release.aab");
    await writeFile(staleBundle, "synthetic stale bundle");
    await utimes(staleBundle, new Date(0), new Date(0));
    const staleDryRun = runPublish(["--dry-run", "--bundle", staleBundle], env, 1);
    const staleDryRunText = outputText(staleDryRun);
    if (!staleDryRunText.includes("HUD AAB is stale")) {
      throw new Error(`Dry-run did not reject a stale HUD bundle:\n${staleDryRunText}`);
    }

    const freshBundle = join(tempDir, "fresh-hud-release.aab");
    await writeFile(freshBundle, "synthetic fresh bundle");
    const future = new Date(Date.now() + 60_000);
    await utimes(freshBundle, future, future);
    const dryRun = runPublish(["--dry-run", "--bundle", freshBundle], env);
    const dryRunText = outputText(dryRun);
    if (!dryRunText.includes("HUD bundle freshness verified")) {
      throw new Error(`Dry-run did not verify HUD bundle freshness:\n${dryRunText}`);
    }
    if (!dryRunText.includes("Local Play submission package verified.")) {
      throw new Error(`Dry-run did not verify the local Play submission package:\n${dryRunText}`);
    }
    if (!dryRunText.includes("Dry-run complete.")) {
      throw new Error(`Dry-run did not complete before Play API access:\n${dryRunText}`);
    }

    const missingAllowed = runPublish(
      ["--auth", "gcloud", "--gcloud-account", allowedAccount, "--auth-check"],
      { ...env, OPENCLAW_FAKE_GCLOUD_SCENARIO: "missing-allowed" },
      1,
    );
    const missingAllowedText = outputText(missingAllowed);
    if (!missingAllowedText.includes(`Selected gcloud account is ${allowedAccount}, but it is not authenticated locally.`)) {
      throw new Error(`Missing-account failure did not explain the selected account:\n${missingAllowedText}`);
    }
    if (!missingAllowedText.includes(serviceAccount)) {
      throw new Error(`Missing-account failure did not list authenticated accounts:\n${missingAllowedText}`);
    }

    const allowed = runPublish(
      ["--auth", "gcloud", "--gcloud-account", allowedAccount, "--auth-check"],
      { ...env, OPENCLAW_FAKE_GCLOUD_SCENARIO: "allowed-authenticated" },
    );
    const allowedText = outputText(allowed);
    if (!allowedText.includes(`Auth: gcloud:${allowedAccount}`)) {
      throw new Error(`Auth-check did not use the requested allowed account:\n${allowedText}`);
    }
    if (!allowedText.includes("no Play API request was made")) {
      throw new Error(`Auth-check did not stop before Play API access:\n${allowedText}`);
    }

    const secondaryAllowed = runPublish(
      ["--auth", "gcloud", "--gcloud-account", secondaryAllowedAccount, "--auth-check"],
      { ...env, OPENCLAW_FAKE_GCLOUD_SCENARIO: "secondary-allowed-authenticated" },
    );
    const secondaryAllowedText = outputText(secondaryAllowed);
    if (!secondaryAllowedText.includes(`Auth: gcloud:${secondaryAllowedAccount}`)) {
      throw new Error(`Auth-check did not use the requested secondary allowed account:\n${secondaryAllowedText}`);
    }
    if (!secondaryAllowedText.includes("no Play API request was made")) {
      throw new Error(`Secondary auth-check did not stop before Play API access:\n${secondaryAllowedText}`);
    }

    const disallowedActive = runPublish(["--auth", "gcloud", "--auth-check"], env, 1);
    const disallowedActiveText = outputText(disallowedActive);
    if (!disallowedActiveText.includes("Play publishing is restricted to")) {
      throw new Error(`Disallowed active-account failure did not explain the account restriction:\n${disallowedActiveText}`);
    }

    const readiness = spawnSync(process.execPath, [readinessScript, "--json", "--skip-signature"], {
      cwd: androidDir,
      encoding: "utf8",
      env: { ...env, OPENCLAW_FAKE_GCLOUD_SCENARIO: "missing-allowed" },
      stdio: ["ignore", "pipe", "pipe"],
    });
    if (readiness.status !== 0) {
      throw new Error(`Readiness report exited ${readiness.status}:\n${outputText(readiness)}`);
    }
    const readinessReport = JSON.parse(readiness.stdout);
    if (readinessReport.publishReady !== false) {
      throw new Error(`Readiness report should not claim publish readiness without OAuth/final Play evidence:\n${readiness.stdout}`);
    }
    if (readinessReport.localReleaseReady !== true) {
      throw new Error(`Readiness report should accept the local release gates in fake-gcloud mode:\n${readiness.stdout}`);
    }
    if (readinessReport.localArtifactReady !== true || readinessReport.localDryRunReady !== true) {
      throw new Error(`Readiness report should split ready local artifact and dry-run gates:\n${readiness.stdout}`);
    }
    if (readinessReport.oauthReady !== false) {
      throw new Error(`Readiness report should reject missing allowed OAuth accounts:\n${readiness.stdout}`);
    }
    if (readinessReport.externalBlockers.length < 2 || readinessReport.localBlockers.length !== 0) {
      throw new Error(`Readiness report should keep external blockers separate from local blockers:\n${readiness.stdout}`);
    }
    if (!readinessReport.blockers.some((blocker) => blocker.includes("Authenticate one allowed publisher account"))) {
      throw new Error(`Readiness report did not include the OAuth blocker:\n${readiness.stdout}`);
    }

    console.log("Play publish helper regression tests passed.");
  } finally {
    await rm(tempDir, { recursive: true, force: true });
  }
}

await main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
