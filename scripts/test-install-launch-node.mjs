#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { chmod, mkdir, mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const launchScript = join("scripts", "install-launch-node.sh");
const packageName = "ai.openclaw.app";
const component = `${packageName}/ai.openclaw.app.MainActivity`;

function runLaunch(args, env, expectedStatus = 0) {
  const result = spawnSync("bash", [launchScript, ...args], {
    cwd: androidDir,
    encoding: "utf8",
    env,
    stdio: ["ignore", "pipe", "pipe"],
  });
  if (result.status !== expectedStatus) {
    throw new Error(
      [
        `install-launch-node exited ${result.status}, expected ${expectedStatus}.`,
        result.stdout.trim(),
        result.stderr.trim(),
      ]
        .filter(Boolean)
        .join("\n"),
    );
  }
  return result;
}

function commandLines(logText) {
  return logText
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean);
}

function findCommand(lines, fragment) {
  return lines.find((line) => line.includes(fragment)) ?? "";
}

function assertIncludes(text, fragment, label) {
  if (!text.includes(fragment)) {
    throw new Error(`${label} did not include ${fragment}:\n${text}`);
  }
}

function assertNoCommand(lines, fragment, label) {
  const match = findCommand(lines, fragment);
  if (match) {
    throw new Error(`${label} unexpectedly included ${fragment}:\n${match}`);
  }
}

async function writeFakeAdb(fakeAdbPath) {
  await writeFile(
    fakeAdbPath,
    `#!/usr/bin/env bash
set -euo pipefail
printf '%s\\n' "$*" >> "$OPENCLAW_FAKE_ADB_LOG"

if [[ "$1" == "devices" ]]; then
  printf 'List of devices attached\\nfold-test\\tdevice\\n'
  exit 0
fi

if [[ "$1" == "-s" ]]; then
  shift 2
fi

if [[ "$1" == "install" ]]; then
  printf 'Success\\n'
  exit 0
fi

if [[ "$1" != "shell" ]]; then
  exit 0
fi

shift
case "\${1:-}" in
  am)
    exit 0
    ;;
  dumpsys)
    if [[ "\${2:-}" == "package" ]]; then
      printf 'versionName=2026.7.11\\n'
    fi
    exit 0
    ;;
esac
exit 0
`,
  );
  await chmod(fakeAdbPath, 0o755);
}

async function runScenario(tempDir, name, args = [], extraEnv = {}) {
  const logPath = join(tempDir, `${name}.log`);
  const apkPath = join(tempDir, `${name}.apk`);
  await writeFile(apkPath, "synthetic apk");
  const result = runLaunch(["--apk", apkPath, ...args], {
    ...process.env,
    ADB: join(tempDir, "adb"),
    OPENCLAW_FAKE_ADB_LOG: logPath,
    ANDROID_SERIAL: "",
    ...extraEnv,
  });
  const lines = commandLines(await readFile(logPath, "utf8"));
  return { result, lines, apkPath };
}

async function runEnvApkScenario(tempDir) {
  const logPath = join(tempDir, "env-apk.log");
  const apkPath = join(tempDir, "env-selected-thirdParty-debug.apk");
  await writeFile(apkPath, "synthetic apk");
  const result = runLaunch([], {
    ...process.env,
    ADB: join(tempDir, "adb"),
    OPENCLAW_FAKE_ADB_LOG: logPath,
    OPENCLAW_NODE_APK: apkPath,
    ANDROID_SERIAL: "",
  });
  const lines = commandLines(await readFile(logPath, "utf8"));
  return { result, lines, apkPath };
}

async function main() {
  const tempDir = await mkdtemp(join(tmpdir(), "openclaw-install-launch-node-"));
  try {
    await mkdir(tempDir, { recursive: true });
    await writeFakeAdb(join(tempDir, "adb"));

    const envApk = await runEnvApkScenario(tempDir);
    assertIncludes(envApk.result.stdout, "Using flavor: thirdParty", "env APK output");
    assertIncludes(envApk.result.stdout, `Installing: ${envApk.apkPath}`, "env APK output");
    assertIncludes(findCommand(envApk.lines, "install"), envApk.apkPath, "env APK install command");

    const defaultLaunch = await runScenario(tempDir, "default");
    const defaultStart = findCommand(defaultLaunch.lines, "shell am start");
    assertIncludes(defaultStart, `-n ${component}`, "default Node am start");
    assertNoCommand(defaultLaunch.lines, "--display", "default Node command log");
    assertNoCommand(defaultLaunch.lines, "windowingMode", "default Node command log");
    assertNoCommand(defaultLaunch.lines, "external_display_audio_output", "default Node command log");
    assertNoCommand(defaultLaunch.lines, "task_bar 0", "default Node command log");

    const playLaunch = await runScenario(tempDir, "play", ["--flavor", "play"]);
    assertIncludes(playLaunch.result.stdout, "Using flavor: play", "play Node output");
    assertIncludes(
      findCommand(playLaunch.lines, "shell am start"),
      `-n ${component}`,
      "play Node am start",
    );

    const setupNoAuto = await runScenario(tempDir, "setup-no-auto", [
      "--flavor",
      "third-party",
      "--setup-code",
      "tailnet-setup-code",
      "--no-auto-connect",
    ]);
    assertIncludes(setupNoAuto.result.stdout, "Using flavor: thirdParty", "third-party alias output");
    const setupStart = findCommand(setupNoAuto.lines, "shell am start");
    assertIncludes(
      setupStart,
      "-a ai.openclaw.app.action.SETUP_GATEWAY",
      "setup no-auto-connect am start",
    );
    assertIncludes(
      setupStart,
      "--es setup_code tailnet-setup-code",
      "setup no-auto-connect am start",
    );
    assertIncludes(setupStart, "--ez auto_connect false", "setup no-auto-connect am start");

    console.log("Node install/launch regression tests passed.");
  } finally {
    await rm(tempDir, { recursive: true, force: true });
  }
}

await main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
