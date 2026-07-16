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
  "short-description.txt": "Minimal assistant HUD for Android external and wearable displays.",
  "full-description.txt": [
    "OpenClaw HUD uses Android Presentation with USB-C and wireless displays, including Samsung DeX on foldable Android phones and AirVision M1.",
    "Remembered-display pinning survives Android display-ID changes.",
    "It includes HID report-path summaries, per-feature firmware-apply readiness, desired firmware-sync state, and protocol-capture targets.",
  ].join("\n"),
  "release-notes.txt": [
    "Remember USB-C, HDMI, wireless, or wearable displays across Android display-ID changes.",
    "- Wait when the pinned display disconnects and keep remembered display identity.",
    "- Use display-scoped density and resources.",
    "- Automatic Presentation routing with generic accessory-key controls.",
    "- Chat, notifications, voice, captions, reporting, and In-app privacy policy.",
  ].join("\n"),
};

const validReadme = [
  "OpenClaw HUD uses Android Presentation APIs and has been live-tested with a foldable Android phone.",
  "",
  "The single-tap, double-tap, swipe, M1 brightness-key, and M1 media/tap-key actions are configurable.",
  "Defaults are tuned for walking HUD use: single-tap clears the current notification, double-tap toggles mic, vertical swipe scrolls chat, and M1 brightness-key events can scroll chat.",
  "",
  "| Windows AirVision feature | Android HUD status |",
  "| --- | --- |",
  "| Working / Gaming / Infinity / Custom modes | Profiles, custom labels, backup/import, brightness, distance, IPD, Splendid, Eye Care, Motion Sync, and Light Load values. |",
  "| Brightness | Software HUD dimming. |",
  "| Screen distance | Virtual HUD distance scaling. |",
  "| IPD | Calibration value clamped to the ASUS documented 53.5-74.5 mm range as 54-74 mm slider stops. Adjustment is locked while Light Load Mode is enabled. |",
  "| Fit, clarity, and text size | 53.5-74.5 mm IPD range, 3D-mode blur checks, and fit/clarity/text-size guidance. |",
  "| Splendid Standard / Theater / Office / Game / Eye Care | Stored profile setting. |",
  "| Blue Light Filter | HUD warm filtering. |",
  "| Motion Sync | Stored in the AirVision profile. |",
  "| 3D Mode | Stored in the AirVision profile. |",
  "| Light Load Mode | Low-overhead HUD operation. |",
  "| Gesture & Hotkey Settings | HUD touch, swipe, brightness-key, media/tap key, and shortcut-menu parity handling. |",
  "| Cursor Follow / Center Cursor / 3DoF | Structured Windows-only capability status plus Android distance hotkey substitute. |",
  "| Unity mirror window / projected glasses view | Structured Windows-only mirror window status plus Android Cast, Display, and Samsung DeX fallback actions. |",
  "| Demo Mode / Tutorials | Android deterministic review mode without a live gateway or live M1 plus Windows tutorial shortcut status. |",
  "| Companion parity states | Settings, diagnostics, and Windows App Handoff render offline-reviewable, M1-optional, firmware-gated, and Windows-only feature state counts. |",
  "| App Preferences | Startup, language, speaker, captions, backup, policy, and support links. |",
  "| Windows app profile handoff | ASUS Windows app apply steps and privacy reminders. |",
  "| Device Information | USB identity details. |",
  "| Firmware link | USB diagnostics and protocol capture state. |",
  "| Firmware update | Windows-only update workflow status. |",
  "| Identify | Temporary HUD marker. |",
  "| Multi-screen desktop layouts | Configurable external-display targeting. |",
  "",
  "Captions default to Samsung/Android native captioning. The OpenClaw fallback forces thinking off, prefers sage-router/fast, and labels alternating turns as `S1` / `S2`.",
  "Diagnostics exports include Android demo/offline reviewer experience state, profile backup/restore readiness, all saved AirVision profile values, runtime profile metadata, structured M1 brightness-key/media-key mapping evidence with Android consumption state and step sizes, shortcut-menu parity for ASUS brightness/volume/distance behavior, structured Android Cast/Display/Samsung DeX mirror fallback actions and limitations, row-level observed Windows surfaces/keys/defaults/capture implications, structured caption/translation mode status, structured support/legal/registration metadata, Windows gesture catalog with instant transparent and shortcut-menu hold/slide behavior, Cyber-observed ASUS AirVision 1.0.7.1 build/settings-key evidence for VirtualSpaceDistance, SoftwareIPD, DisplaySplendidMode, EyeCareLevel, PreventMotionBlur, IsEcoMode, CenterCursorHotkey, and DistanceHotkey, and companion parity-state counts for offline-reviewable, M1-optional, firmware-gated, and Windows-only features.",
  "Firmware Link shows per-control WAIT/CAPTURE/READY rows for target value, Android effect, firmware status, missing evidence, and blocker text while writes remain blocked.",
  "Windows App Handoff exports ASUS Windows app apply steps, ASUS AirVision 1.0.7.1 build/settings-key evidence, VirtualSpaceDistance, SoftwareIPD, DisplaySplendidMode, DistanceHotkey, row-level observed Windows surfaces/keys/defaults/capture implications, structured M1 hardware-key mapping, brightness/media key consumption state, Windows gesture catalog entries, structured caption/translation mode status, structured support/legal/registration metadata, and privacy reminders while omitting raw USB serial values.",
  "Profile Backup exports gesture/hotkey settings, speaker state, visible structured native captions plus OpenClaw translation fallback status, Samsung/native captions preference, and OpenClaw translation caption settings, and never includes gateway endpoints, auth tokens, or chat history.",
  "`play/airvision-firmware-capture-results.json` is the machine-checked sanitized capture-results file. Android firmware writes remain blocked until evidence is validated.",
].join("\n");

const validSettingsSheet = [
  "package ai.openclaw.app.ui",
  "import ai.openclaw.app.AirVisionCompanionParity",
  "import ai.openclaw.app.AirVisionCaptionModeStatus",
  "val airVisionCompanionParity = AirVisionCompanionParity.fromState(...)",
  "val airVisionCaptionModeStatus = AirVisionCaptionModeStatus.from(...)",
  'headlineContent = { Text("Companion Parity", style = mobileHeadline) }',
  'headlineContent = { Text("Caption & Translation Mode", style = mobileHeadline) }',
  "airVisionCompanionParitySettingsText(airVisionCompanionParity)",
  "airVisionCaptionModeSettingsText(airVisionCaptionModeStatus)",
  '\"reviewable_offline\" -> \"offline-reviewable\"',
  '\"m1_optional\" -> \"M1-optional\"',
  '\"firmware_gated\" -> \"firmware-gated\"',
  '\"windows_only\" -> \"Windows-only\"',
  "Fallback model:",
  "thinking",
  "Languages:",
  "Speaker labels:",
].join("\n");

function runVerifier(
  gradlePath,
  localePath,
  listingDir,
  readmePath,
  settingsSheetPath,
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
        ...(settingsSheetPath ? { OPENCLAW_ANDROID_SETTINGS_SHEET_PATH: settingsSheetPath } : {}),
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
  settingsSheetPath,
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
        ...(settingsSheetPath ? { OPENCLAW_ANDROID_SETTINGS_SHEET_PATH: settingsSheetPath } : {}),
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
    const settingsSheetPath = join(tempDir, "SettingsSheet.kt");
    await writeFile(localePath, localeSource);
    await writeFile(readmePath, validReadme);
    await writeFile(settingsSheetPath, validSettingsSheet);

    await writeFile(gradlePath, gradleWithLanguageSplitsEnabled);
    const failing = runVerifier(gradlePath, localePath, null, readmePath, settingsSheetPath);
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
    const passing = runVerifier(gradlePath, localePath, null, readmePath, settingsSheetPath);
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
    const listingPassing = runListingVerifier(gradlePath, localePath, listingPath, readmePath, settingsSheetPath);
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
    const listingFailing = runListingVerifier(gradlePath, localePath, listingPath, readmePath, settingsSheetPath);
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
    await writeFile(
      readmePath,
      validReadme.replace(
        "| IPD | Calibration value clamped to the ASUS documented 53.5-74.5 mm range as 54-74 mm slider stops. Adjustment is locked while Light Load Mode is enabled. |",
        "",
      ),
    );
    const readmeFailing = runListingVerifier(gradlePath, localePath, listingPath, readmePath, settingsSheetPath);
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

    await writeFile(readmePath, validReadme.replace("54-74 mm slider stops", "generic slider stops"));
    const readmeIpdFailing = runListingVerifier(gradlePath, localePath, listingPath, readmePath, settingsSheetPath);
    if (
      readmeIpdFailing.status === 0 ||
      !readmeIpdFailing.stderr.includes("README AirVision fit and clarity guidance")
    ) {
      throw new Error(
        [
          "Expected verifier to reject README text that omits the ASUS IPD clamp.",
          `status=${readmeIpdFailing.status}`,
          `stderr=${readmeIpdFailing.stderr.trim()}`,
          `stdout=${readmeIpdFailing.stdout.trim()}`,
        ].join("\n"),
      );
    }

    await writeFile(readmePath, validReadme);
    await writeFile(settingsSheetPath, "package ai.openclaw.app.ui\nfun SettingsSheet() = Unit\n");
    const settingsFailing = runListingVerifier(gradlePath, localePath, listingPath, readmePath, settingsSheetPath);
    if (
      settingsFailing.status === 0 ||
      !settingsFailing.stderr.includes("Settings AirVision companion parity")
    ) {
      throw new Error(
        [
          "Expected verifier to reject Settings text that omits visible companion parity.",
          `status=${settingsFailing.status}`,
          `stderr=${settingsFailing.stderr.trim()}`,
          `stdout=${settingsFailing.stdout.trim()}`,
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
