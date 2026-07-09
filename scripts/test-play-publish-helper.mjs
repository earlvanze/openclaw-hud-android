#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { chmod, mkdir, mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { delimiter } from "node:path";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const publishScript = join("scripts", "publish-play-internal.mjs");
const allowedAccount = "earlvanze@gmail.com";
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
service="${serviceAccount}"

if [[ "$1 $2" == "auth list" ]]; then
  if [[ "$scenario" == "allowed-authenticated" ]]; then
    printf '[{"account":"%s","status":"ACTIVE"}]\\n' "$allowed"
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

    const disallowedActive = runPublish(["--auth", "gcloud", "--auth-check"], env, 1);
    const disallowedActiveText = outputText(disallowedActive);
    if (!disallowedActiveText.includes("Play publishing is restricted to")) {
      throw new Error(`Disallowed active-account failure did not explain the account restriction:\n${disallowedActiveText}`);
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
