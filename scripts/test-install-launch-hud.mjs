#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { chmod, mkdir, mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const launchScript = join("scripts", "install-launch-hud.sh");
const packageName = "ai.openclaw.app.hud";
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
        `install-launch-hud exited ${result.status}, expected ${expectedStatus}.`,
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
  cmd)
    exit 0
    ;;
  settings)
    if [[ "\${2:-}" == "get" && "\${4:-}" == "policy_control" ]]; then
      printf 'immersive.full=${packageName},immersive.navigation=com.example.keep\\n'
    fi
    exit 0
    ;;
  dumpsys)
    if [[ "\${2:-}" == "display" ]]; then
      if [[ "\${OPENCLAW_FAKE_ADB_DISPLAY_SCENARIO:-external}" == "external" ]]; then
        printf 'mBaseDisplayInfo=DisplayInfo{displayId 0, name Built-in Screen, type INTERNAL}\\n'
        printf 'mBaseDisplayInfo=DisplayInfo{displayId 2, name ASUS AirVision M1, type EXTERNAL}\\n'
      else
        printf 'mBaseDisplayInfo=DisplayInfo{displayId 0, name Built-in Screen, type INTERNAL}\\n'
      fi
    elif [[ "\${2:-}" == "package" ]]; then
      printf 'versionName=2026.7.10\\n'
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
  return { result, lines };
}

async function main() {
  const tempDir = await mkdtemp(join(tmpdir(), "openclaw-install-launch-hud-"));
  try {
    await mkdir(tempDir, { recursive: true });
    await writeFakeAdb(join(tempDir, "adb"));

    const presentation = await runScenario(tempDir, "presentation");
    assertIncludes(
      presentation.result.stdout,
      "Launching presentation HUD for external display 2",
      "default presentation launch output",
    );
    const presentationStart = findCommand(presentation.lines, "shell am start");
    assertIncludes(presentationStart, "--display 0", "default presentation am start");
    assertIncludes(presentationStart, "--windowingMode 1", "default presentation am start");
    assertIncludes(presentationStart, "-f 0x10008000", "default presentation am start");
    assertIncludes(
      presentationStart,
      "--activity-reset-task-if-needed",
      "default presentation am start",
    );
    assertIncludes(presentationStart, `-n ${component}`, "default presentation am start");
    assertNoCommand(
      presentation.lines,
      "shell am start --display 2",
      "default presentation command log",
    );
    assertIncludes(
      findCommand(presentation.lines, "shell settings put global task_bar 0"),
      "task_bar 0",
      "taskbar suppression command",
    );
    assertIncludes(
      findCommand(presentation.lines, "shell settings put global external_display_audio_output 1"),
      "external_display_audio_output 1",
      "external display audio command",
    );
    assertIncludes(
      findCommand(presentation.lines, "shell settings put global policy_control"),
      "immersive.navigation=com.example.keep",
      "policy cleanup command",
    );
    assertNoCommand(
      presentation.lines,
      `immersive.full=${packageName}`,
      "policy cleanup command log",
    );

    const forcedExternal = await runScenario(tempDir, "forced-external", ["--display", "external"]);
    assertIncludes(
      forcedExternal.result.stdout,
      "Launching HUD on display 2",
      "forced external launch output",
    );
    assertIncludes(
      findCommand(forcedExternal.lines, "shell am start --display 2"),
      `-n ${component}`,
      "forced external am start",
    );

    const noExternal = await runScenario(tempDir, "no-external", [], {
      OPENCLAW_FAKE_ADB_DISPLAY_SCENARIO: "internal-only",
    });
    assertIncludes(
      noExternal.result.stdout,
      "No external display found; launching on default display",
      "no-external launch output",
    );
    assertIncludes(
      findCommand(noExternal.lines, "shell am start"),
      "--display 0",
      "no-external presentation am start",
    );
    assertNoCommand(
      noExternal.lines,
      "external_display_audio_output 1",
      "no-external command log",
    );

    console.log("HUD install/launch regression tests passed.");
  } finally {
    await rm(tempDir, { recursive: true, force: true });
  }
}

await main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
