#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptPath = fileURLToPath(new URL("./verify-play-hud-release.mjs", import.meta.url));
const bundlePath = fileURLToPath(
  new URL("../app/build/outputs/bundle/hudRelease/app-hud-release.aab", import.meta.url),
);

const localeSource = `
package ai.openclaw.app

import android.app.LocaleManager

object AirVisionAppLocale {
    fun wrap() = "createConfigurationContext"
    fun apply() = LocaleManager::class.java.name
}
`;

const gradleWithLanguageSplitsDisabled = `
plugins {
    id("com.android.application")
}

android {
    bundle {
        language {
            enableSplit = false
        }
    }
}
`;

const gradleWithLanguageSplitsEnabled = `
plugins {
    id("com.android.application")
}

android {
    // This commented block must not satisfy the verifier:
    // bundle { language { enableSplit = false } }
}
`;

function runVerifier(gradlePath, localePath) {
  return spawnSync(
    process.execPath,
    [scriptPath, "--bundle", bundlePath, "--skip-signature"],
    {
      encoding: "utf8",
      env: {
        ...process.env,
        OPENCLAW_ANDROID_GRADLE_BUILD_PATH: gradlePath,
        OPENCLAW_ANDROID_AIRVISION_APP_LOCALE_PATH: localePath,
      },
    },
  );
}

async function main() {
  const tempDir = await mkdtemp(join(tmpdir(), "openclaw-play-verifier-"));
  try {
    const localePath = join(tempDir, "AirVisionAppLocale.kt");
    const gradlePath = join(tempDir, "build.gradle.kts");
    await writeFile(localePath, localeSource);

    await writeFile(gradlePath, gradleWithLanguageSplitsEnabled);
    const failing = runVerifier(gradlePath, localePath);
    if (failing.status === 0 || !failing.stderr.includes("language splits")) {
      throw new Error(
        [
          "Expected verifier to reject runtime locale switching when App Bundle language splits are enabled.",
          `status=${failing.status}`,
          `stderr=${failing.stderr.trim()}`,
        ].join("\n"),
      );
    }

    await writeFile(gradlePath, gradleWithLanguageSplitsDisabled);
    const passing = runVerifier(gradlePath, localePath);
    if (passing.status !== 0) {
      throw new Error(
        [
          "Expected verifier to accept runtime locale switching when App Bundle language splits are disabled.",
          `status=${passing.status}`,
          `stderr=${passing.stderr.trim()}`,
          `stdout=${passing.stdout.trim()}`,
        ].join("\n"),
      );
    }

    console.log("Play HUD release verifier regression tests passed.");
  } finally {
    await rm(tempDir, { recursive: true, force: true });
  }
}

await main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
