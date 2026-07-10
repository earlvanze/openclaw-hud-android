#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { mkdir, mkdtemp, rm, writeFile } from "node:fs/promises";
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

const validListingFiles = {
  "title.txt": "OpenClaw HUD",
  "short-description.txt": "Minimal assistant HUD for Samsung DeX and Asus AirVision M1 displays.",
  "full-description.txt": [
    "OpenClaw HUD is optimized for Samsung DeX on Galaxy Fold 7 with AirVision M1.",
    "It includes HID report-path summaries, per-feature firmware-apply readiness, desired firmware-sync state, and protocol-capture targets.",
  ].join("\n"),
  "release-notes.txt": [
    "Initial AirVision M1 HUD release candidate:",
    "- USB firmware-link diagnostics with HID report paths, feature readiness, desired firmware-sync state, and protocol-capture targets.",
  ].join("\n"),
};

const validReadme = [
  "OpenClaw HUD is optimized for Samsung DeX and has been live-tested with a Samsung Galaxy Fold 7.",
  "",
  "The single-tap, double-tap, swipe, M1 brightness-key, and M1 media/tap-key actions are configurable.",
  "Defaults are tuned for walking HUD use: single-tap clears the current notification, double-tap toggles mic, vertical swipe scrolls chat, and M1 brightness-key events can scroll chat.",
  "",
  "| Windows AirVision feature | Android HUD status |",
  "| --- | --- |",
  "| Working / Gaming / Infinity / Custom modes | Profiles, custom labels, backup/import, brightness, distance, IPD, Splendid, Eye Care, Motion Sync, and Light Load values. |",
  "| Brightness | Software HUD dimming. |",
  "| Screen distance | Virtual HUD distance scaling. |",
  "| IPD | Calibration value. |",
  "| Splendid Standard / Theater / Office / Game / Eye Care | Stored profile setting. |",
  "| Blue Light Filter | HUD warm filtering. |",
  "| Motion Sync | Stored in the AirVision profile. |",
  "| 3D Mode | Stored in the AirVision profile. |",
  "| Light Load Mode | Low-overhead HUD operation. |",
  "| Gesture & Hotkey Settings | HUD touch, swipe, brightness-key, and media/tap key handling. |",
  "| App Preferences | Startup, language, speaker, captions, backup, policy, and support links. |",
  "| Device Information | USB identity details. |",
  "| Firmware link | USB diagnostics and protocol capture state. |",
  "| Identify | Temporary HUD marker. |",
  "| Multi-screen desktop layouts | Configurable external-display targeting. |",
  "",
  "Captions default to Samsung/Android native captioning. The OpenClaw fallback forces thinking off, prefers sage-router/fast, and labels alternating turns as `S1` / `S2`.",
  "Profile Backup exports gesture/hotkey settings, speaker state, Samsung/native captions preference, and OpenClaw translation caption settings, and never includes gateway endpoints, auth tokens, or chat history.",
].join("\n");

function runVerifier(
  gradlePath,
  localePath,
  listingDir,
  readmePath,
) {
  const args = [scriptPath, "--bundle", bundlePath, "--skip-signature"];
  if (listingDir) args.push("--listing-dir", listingDir);
  return spawnSync(
    process.execPath,
    args,
    {
      encoding: "utf8",
      env: {
        ...process.env,
        OPENCLAW_ANDROID_GRADLE_BUILD_PATH: gradlePath,
        OPENCLAW_ANDROID_AIRVISION_APP_LOCALE_PATH: localePath,
        ...(readmePath ? { OPENCLAW_ANDROID_README_PATH: readmePath } : {}),
      },
    },
  );
}

async function writeListing(
  root,
  overrides = {},
) {
  const languageDir = join(root, "en-US");
  await mkdir(languageDir, { recursive: true });
  const files = { ...validListingFiles, ...overrides };
  await Promise.all(
    Object.entries(files).map(([file, content]) => writeFile(join(languageDir, file), content)),
  );
}

function runListingVerifier(
  gradlePath,
  localePath,
  listingDir,
  readmePath,
) {
  return spawnSync(
    process.execPath,
    [scriptPath, "--bundle", bundlePath, "--skip-signature", "--listing-dir", listingDir],
    {
      encoding: "utf8",
      env: {
        ...process.env,
        OPENCLAW_ANDROID_GRADLE_BUILD_PATH: gradlePath,
        OPENCLAW_ANDROID_AIRVISION_APP_LOCALE_PATH: localePath,
        ...(readmePath ? { OPENCLAW_ANDROID_README_PATH: readmePath } : {}),
      },
    },
  );
}

async function main() {
  const tempDir = await mkdtemp(join(tmpdir(), "openclaw-play-verifier-"));
  try {
    const localePath = join(tempDir, "AirVisionAppLocale.kt");
    const gradlePath = join(tempDir, "build.gradle.kts");
    const listingPath = join(tempDir, "listings");
    const readmePath = join(tempDir, "README.md");
    await writeFile(localePath, localeSource);
    await writeFile(readmePath, validReadme);

    await writeFile(gradlePath, gradleWithLanguageSplitsEnabled);
    const failing = runVerifier(gradlePath, localePath, null, readmePath);
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
    const passing = runVerifier(gradlePath, localePath, null, readmePath);
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

    await writeListing(listingPath);
    const listingPassing = runListingVerifier(gradlePath, localePath, listingPath, readmePath);
    if (listingPassing.status !== 0) {
      throw new Error(
        [
          "Expected verifier to accept Play listing text that covers AirVision diagnostics.",
          `status=${listingPassing.status}`,
          `stderr=${listingPassing.stderr.trim()}`,
          `stdout=${listingPassing.stdout.trim()}`,
        ].join("\n"),
      );
    }

    await writeListing(listingPath, {
      "release-notes.txt": "Initial AirVision M1 HUD release candidate.",
    });
    const listingFailing = runListingVerifier(gradlePath, localePath, listingPath, readmePath);
    if (listingFailing.status === 0 || !listingFailing.stderr.includes("required Play listing text")) {
      throw new Error(
        [
          "Expected verifier to reject Play listing text that omits AirVision diagnostics coverage.",
          `status=${listingFailing.status}`,
          `stderr=${listingFailing.stderr.trim()}`,
          `stdout=${listingFailing.stdout.trim()}`,
        ].join("\n"),
      );
    }

    await writeListing(listingPath);
    await writeFile(readmePath, validReadme.replace("| IPD | Calibration value. |", ""));
    const readmeFailing = runListingVerifier(gradlePath, localePath, listingPath, readmePath);
    if (readmeFailing.status === 0 || !readmeFailing.stderr.includes("README Windows AirVision feature matrix")) {
      throw new Error(
        [
          "Expected verifier to reject README text that omits Windows AirVision parity rows.",
          `status=${readmeFailing.status}`,
          `stderr=${readmeFailing.stderr.trim()}`,
          `stdout=${readmeFailing.stdout.trim()}`,
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
