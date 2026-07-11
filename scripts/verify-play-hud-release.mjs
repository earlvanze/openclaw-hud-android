#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { access, readdir, readFile, stat } from "node:fs/promises";
import { basename, dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const releaseOutputDir = join(androidDir, "build", "release-bundles");
const gradleHudBundlePath = join(
  androidDir,
  "app",
  "build",
  "outputs",
  "bundle",
  "hudRelease",
  "app-hud-release.aab",
);
const defaultManifestPath = join(
  androidDir,
  "app",
  "build",
  "intermediates",
  "packaged_manifests",
  "hudRelease",
  "processHudReleaseManifestForPackage",
  "AndroidManifest.xml",
);
const defaultListingDir = join(androidDir, "play", "listings");
const gradleBuildPath =
  process.env.OPENCLAW_ANDROID_GRADLE_BUILD_PATH?.trim() ||
  join(androidDir, "app", "build.gradle.kts");
const airVisionAppLocalePath =
  process.env.OPENCLAW_ANDROID_AIRVISION_APP_LOCALE_PATH?.trim() ||
  join(
    androidDir,
    "app",
    "src",
    "main",
    "java",
    "ai",
    "openclaw",
    "app",
    "AirVisionAppLocale.kt",
  );
const airVisionFirmwareCapturePlanScript = join(scriptDir, "render-airvision-firmware-capture-plan.mjs");
const airVisionFirmwareCapturePlanPath = join(androidDir, "play", "airvision-firmware-capture-plan.md");
const airVisionFirmwareCaptureResultsScript = join(scriptDir, "verify-airvision-firmware-capture-results.mjs");
const airVisionFirmwareCaptureResultsPath = join(androidDir, "play", "airvision-firmware-capture-results.json");
const readmePath =
  process.env.OPENCLAW_ANDROID_README_PATH?.trim() ||
  join(androidDir, "README.md");
const settingsSheetPath =
  process.env.OPENCLAW_ANDROID_SETTINGS_SHEET_PATH?.trim() ||
  join(androidDir, "app", "src", "main", "java", "ai", "openclaw", "app", "ui", "SettingsSheet.kt");

const expectedPackage = "ai.openclaw.app.hud";
const expectedPermissions = [
  "android.permission.INTERNET",
  "android.permission.ACCESS_NETWORK_STATE",
  "android.permission.FOREGROUND_SERVICE",
  "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
  "android.permission.POST_NOTIFICATIONS",
  "android.permission.NEARBY_WIFI_DEVICES",
  "android.permission.RECORD_AUDIO",
  "android.permission.MODIFY_AUDIO_SETTINGS",
];
const forbiddenPermissions = [
  "android.permission.SEND_SMS",
  "android.permission.READ_SMS",
  "android.permission.READ_CALL_LOG",
  "android.permission.ACCESS_FINE_LOCATION",
  "android.permission.ACCESS_COARSE_LOCATION",
  "android.permission.ACCESS_BACKGROUND_LOCATION",
  "android.permission.CAMERA",
  "android.permission.READ_MEDIA_IMAGES",
  "android.permission.READ_MEDIA_VIDEO",
  "android.permission.READ_MEDIA_AUDIO",
  "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
  "android.permission.READ_EXTERNAL_STORAGE",
  "android.permission.MANAGE_EXTERNAL_STORAGE",
  "android.permission.READ_CONTACTS",
  "android.permission.WRITE_CONTACTS",
  "android.permission.READ_CALENDAR",
  "android.permission.WRITE_CALENDAR",
  "android.permission.ACTIVITY_RECOGNITION",
  "android.permission.QUERY_ALL_PACKAGES",
  "android.permission.REQUEST_INSTALL_PACKAGES",
  "android.permission.SYSTEM_ALERT_WINDOW",
  "android.permission.BIND_ACCESSIBILITY_SERVICE",
];
const forbiddenFeatures = [
  "android.hardware.camera",
  "android.hardware.camera.any",
  "android.hardware.telephony",
];

function parseArgs(argv) {
  const args = {
    bundle: null,
    manifest: defaultManifestPath,
    listingDir: defaultListingDir,
    language: "en-US",
    skipSignature: false,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--bundle") args.bundle = resolve(argv[++index]);
    else if (arg === "--manifest") args.manifest = resolve(argv[++index]);
    else if (arg === "--listing-dir") args.listingDir = resolve(argv[++index]);
    else if (arg === "--language") args.language = argv[++index];
    else if (arg === "--skip-signature") args.skipSignature = true;
    else if (arg === "--help" || arg === "-h") {
      console.log(
        [
          "Usage: node scripts/verify-play-hud-release.mjs [--bundle path] [--manifest path]",
          "",
          "Verifies the latest signed HUD AAB, packaged HUD manifest, and Play listing copy.",
          "Defaults:",
          "  --bundle       newest build/release-bundles/*-hud-release.aab or Gradle's direct hudRelease output",
          "  --manifest     packaged hudRelease AndroidManifest.xml",
          "  --listing-dir  play/listings",
          "  --language     en-US",
          "  --skip-signature  For CI-only unsigned release manifest/listing checks.",
        ].join("\n"),
      );
      process.exit(0);
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }

  return args;
}

async function executableExists(path) {
  try {
    await access(path);
    return true;
  } catch {
    return false;
  }
}

async function resolveExecutable(command, fallbackCandidates = []) {
  const pathEntries = (process.env.PATH ?? "").split(":").filter(Boolean);
  for (const entry of pathEntries) {
    const candidate = join(entry, command);
    if (await executableExists(candidate)) return candidate;
  }
  for (const candidate of fallbackCandidates) {
    if (candidate && (await executableExists(candidate))) return candidate;
  }
  return command;
}

async function latestHudBundle() {
  const files = await readdir(releaseOutputDir).catch(() => []);
  const candidates = [];
  for (const file of files) {
    if (!file.endsWith("-hud-release.aab")) continue;
    const path = join(releaseOutputDir, file);
    const info = await stat(path);
    candidates.push({ path, mtimeMs: info.mtimeMs });
  }
  await stat(gradleHudBundlePath)
    .then((info) => {
      if (info.isFile()) candidates.push({ path: gradleHudBundlePath, mtimeMs: info.mtimeMs });
    })
    .catch(() => {});
  candidates.sort((a, b) => b.mtimeMs - a.mtimeMs);
  return candidates[0]?.path ?? null;
}

async function sha256Hex(path) {
  return createHash("sha256").update(await readFile(path)).digest("hex");
}

function runChecked(command, args, label) {
  const result = spawnSync(command, args, {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
  if (result.status !== 0) {
    const detail = result.stderr.trim() || result.stdout.trim() || `${command} ${args.join(" ")} failed`;
    throw new Error(`${label}: ${detail}`);
  }
  return result.stdout;
}

async function verifyBundle(bundlePath, skipSignature) {
  if (!bundlePath) {
    throw new Error(
      [
        `No HUD release bundle found in ${releaseOutputDir} or ${gradleHudBundlePath}.`,
        "Run ./gradlew :app:bundleHudRelease or node scripts/build-release-aab.mjs --flavor hud.",
      ].join(" "),
    );
  }
  const info = await stat(bundlePath);
  if (info.size <= 0) throw new Error(`HUD bundle is empty: ${bundlePath}`);
  runChecked("unzip", ["-l", bundlePath, "base/manifest/AndroidManifest.xml"], "AAB manifest check");

  if (!skipSignature) {
    const jarsigner = await resolveExecutable("jarsigner", [
      join(process.env.JAVA_HOME ?? "", "bin", "jarsigner"),
      join(process.env.HOME ?? "", ".gradle", "jdks", "eclipse_adoptium-21-amd64-linux.2", "bin", "jarsigner"),
    ]);
    runChecked(jarsigner, ["-verify", bundlePath], "AAB signature verification");
  }

  return {
    path: bundlePath,
    size: info.size,
    sha256: await sha256Hex(bundlePath),
  };
}

function charCount(value) {
  return Array.from(value).length;
}

function trimTrailingNewline(value) {
  return value.replace(/\r\n/gu, "\n").replace(/\n+$/u, "");
}

function stripKotlinComments(source) {
  let stripped = "";
  let index = 0;
  let inLineComment = false;
  let inBlockComment = false;
  let inString = false;
  let inTripleString = false;
  let escaped = false;

  while (index < source.length) {
    const current = source[index];
    const next = source[index + 1];
    const nextTwo = source.slice(index, index + 3);

    if (inLineComment) {
      if (current === "\n") {
        inLineComment = false;
        stripped += "\n";
      }
      index += 1;
      continue;
    }

    if (inBlockComment) {
      if (current === "*" && next === "/") {
        inBlockComment = false;
        index += 2;
      } else {
        if (current === "\n") stripped += "\n";
        index += 1;
      }
      continue;
    }

    if (inTripleString) {
      stripped += current;
      if (nextTwo === "\"\"\"") {
        stripped += source[index + 1] + source[index + 2];
        inTripleString = false;
        index += 3;
      } else {
        index += 1;
      }
      continue;
    }

    if (inString) {
      stripped += current;
      if (escaped) escaped = false;
      else if (current === "\\") escaped = true;
      else if (current === "\"") inString = false;
      index += 1;
      continue;
    }

    if (current === "/" && next === "/") {
      inLineComment = true;
      index += 2;
      continue;
    }
    if (current === "/" && next === "*") {
      inBlockComment = true;
      index += 2;
      continue;
    }
    if (nextTwo === "\"\"\"") {
      inTripleString = true;
      stripped += nextTwo;
      index += 3;
      continue;
    }
    if (current === "\"") {
      inString = true;
      stripped += current;
      index += 1;
      continue;
    }

    stripped += current;
    index += 1;
  }

  return stripped;
}

async function readListingFile(path) {
  return trimTrailingNewline(await readFile(path, "utf8"));
}

function requireLength(label, value, maxLength) {
  const length = charCount(value);
  if (length > maxLength) {
    throw new Error(`${label} is ${length} characters; Google Play limit is ${maxLength}.`);
  }
  return length;
}

function requireIncludes(label, value, requiredTerms) {
  const missing = requiredTerms.filter((term) => !value.includes(term));
  if (missing.length > 0) {
    throw new Error(`${label} is missing required Play listing text: ${missing.join(", ")}`);
  }
}

async function verifyListing(listingDir, language) {
  const languageDir = join(listingDir, language);
  const title = await readListingFile(join(languageDir, "title.txt"));
  const shortDescription = await readListingFile(join(languageDir, "short-description.txt"));
  const fullDescription = await readListingFile(join(languageDir, "full-description.txt"));
  const releaseNotes = await readListingFile(join(languageDir, "release-notes.txt"));
  requireIncludes("Short description", shortDescription, ["Samsung DeX", "Asus AirVision M1"]);
  requireIncludes("Full description", fullDescription, [
    "Samsung DeX",
    "Galaxy Fold 7",
    "AirVision M1",
    "HID report-path summaries",
    "per-feature firmware-apply readiness",
    "desired firmware-sync state",
    "protocol-capture targets",
  ]);
  requireIncludes("Release notes", releaseNotes, [
    "USB firmware-link diagnostics",
    "HID report paths",
    "feature readiness",
    "desired firmware-sync state",
    "protocol-capture targets",
  ]);

  return {
    title: requireLength("Listing title", title, 30),
    shortDescription: requireLength("Short description", shortDescription, 80),
    fullDescription: requireLength("Full description", fullDescription, 4000),
    releaseNotes: requireLength("Release notes", releaseNotes, 500),
  };
}

function attr(element, attrName) {
  const escaped = attrName.replace(/[.*+?^${}()|[\]\\]/gu, "\\$&");
  return element.match(new RegExp(`\\b${escaped}\\s*=\\s*"([^"]*)"`, "u"))?.[1] ?? null;
}

function openingTags(xml, tagName) {
  return [...xml.matchAll(new RegExp(`<${tagName}\\b[^>]*>`, "gu"))].map((match) => match[0]);
}

function attributeValues(xml, tagName, attrName) {
  return openingTags(xml, tagName)
    .map((tag) => attr(tag, attrName))
    .filter((value) => value !== null);
}

function verifyManifestXml(xml, manifestPath) {
  const manifestTag = xml.match(/<manifest\b[^>]*>/u)?.[0];
  if (!manifestTag) throw new Error(`No <manifest> tag found in ${manifestPath}`);

  const packageName = attr(manifestTag, "package");
  const versionCode = attr(manifestTag, "android:versionCode");
  const versionName = attr(manifestTag, "android:versionName");
  if (packageName !== expectedPackage) {
    throw new Error(`HUD manifest package is ${packageName || "(missing)"}, expected ${expectedPackage}`);
  }
  if (!versionCode || !versionName) {
    throw new Error(`HUD manifest is missing versionCode/versionName in ${manifestPath}`);
  }

  const permissions = new Set(attributeValues(xml, "uses-permission", "android:name"));
  const missingPermissions = expectedPermissions.filter((permission) => !permissions.has(permission));
  const presentForbiddenPermissions = forbiddenPermissions.filter((permission) => permissions.has(permission));
  if (missingPermissions.length > 0) {
    throw new Error(`HUD manifest missing expected permissions: ${missingPermissions.join(", ")}`);
  }
  if (presentForbiddenPermissions.length > 0) {
    throw new Error(`HUD manifest requests forbidden permissions: ${presentForbiddenPermissions.join(", ")}`);
  }

  const featureTags = openingTags(xml, "uses-feature");
  const presentForbiddenFeatures = featureTags
    .map((tag) => ({
      name: attr(tag, "android:name"),
      required: attr(tag, "android:required"),
    }))
    .filter((feature) => feature.name && forbiddenFeatures.includes(feature.name) && feature.required !== "false");
  if (presentForbiddenFeatures.length > 0) {
    throw new Error(`HUD manifest declares forbidden required features: ${presentForbiddenFeatures.map((feature) => feature.name).join(", ")}`);
  }

  const servicePermissions = new Set(attributeValues(xml, "service", "android:permission"));
  if (!servicePermissions.has("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE")) {
    throw new Error("HUD manifest is missing the notification-listener service permission");
  }
  if (servicePermissions.has("android.permission.BIND_ACCESSIBILITY_SERVICE")) {
    throw new Error("HUD manifest declares an AccessibilityService, which is not Play-safe for this HUD build");
  }

  return {
    packageName,
    versionCode,
    versionName,
    permissions: permissions.size,
    services: attributeValues(xml, "service", "android:name").length,
  };
}

async function verifyManifest(manifestPath) {
  const xml = await readFile(manifestPath, "utf8");
  return verifyManifestXml(xml, manifestPath);
}

async function verifyRuntimeLocaleBundleConfig() {
  const localeSource = await readFile(airVisionAppLocalePath, "utf8").catch(() => "");
  const usesRuntimeLocale =
    localeSource.includes("createConfigurationContext") ||
    localeSource.includes("LocaleManager") ||
    localeSource.includes("applicationLocales");

  if (!usesRuntimeLocale) {
    return { usesRuntimeLocale: false, languageSplitsDisabled: null };
  }

  const gradleSource = stripKotlinComments(await readFile(gradleBuildPath, "utf8"));
  const languageSplitsDisabled =
    /bundle\s*\{[\s\S]*language\s*\{[\s\S]*enableSplit\s*=\s*false/u.test(gradleSource);

  if (!languageSplitsDisabled) {
    throw new Error(
      [
        "Runtime AirVision app-language switching requires Play App Bundle language splits to stay disabled.",
        "Add android { bundle { language { enableSplit = false } } } to app/build.gradle.kts.",
      ].join(" "),
    );
  }

  return { usesRuntimeLocale: true, languageSplitsDisabled };
}

async function verifyReadmeAirVisionParity() {
  const readme = await readFile(readmePath, "utf8");
  requireIncludes("README AirVision positioning", readme, [
    "optimized for Samsung DeX",
    "Samsung Galaxy Fold 7",
    "Windows AirVision feature",
  ]);
  requireIncludes("README Windows AirVision feature matrix", readme, [
    "| Working / Gaming / Infinity / Custom modes |",
    "| Brightness |",
    "| Screen distance |",
    "| IPD |",
    "| Fit, clarity, and text size |",
    "| Splendid Standard / Theater / Office / Game / Eye Care |",
    "| Blue Light Filter |",
    "| Motion Sync |",
    "| 3D Mode |",
    "| Light Load Mode |",
    "| Gesture & Hotkey Settings |",
    "| Cursor Follow / Center Cursor / 3DoF |",
    "| Unity mirror window / projected glasses view |",
    "| Demo Mode / Tutorials |",
    "| Companion parity states |",
    "| App Preferences |",
    "| Windows app profile handoff |",
    "| Device Information |",
    "| Firmware link |",
    "| Firmware update |",
    "| Identify |",
    "| Multi-screen desktop layouts |",
  ]);
  requireIncludes("README AirVision offline reviewer mode", readme, [
    "without a live gateway or live M1",
    "Android demo/offline reviewer experience",
  ]);
  requireIncludes("README AirVision profile backup diagnostics", readme, [
    "profile backup/restore readiness",
    "all saved AirVision profile values",
    "runtime profile metadata",
  ]);
  requireIncludes("README AirVision Windows app handoff", readme, [
    "Windows App Handoff",
    "ASUS Windows app apply steps",
    "omitting raw USB serial values",
  ]);
  requireIncludes("README AirVision companion parity states", readme, [
    "companion parity-state counts",
    "offline-reviewable, M1-optional",
    "firmware-gated, and Windows-only",
  ]);
  requireIncludes("README AirVision fit and clarity guidance", readme, [
    "53.5-74.5 mm IPD range",
    "3D-mode blur checks",
    "fit/clarity/text-size guidance",
  ]);
  requireIncludes("README HUD walking defaults", readme, [
    "single-tap clears the current",
    "double-tap toggles mic",
    "vertical swipe scrolls chat",
    "brightness-key events can scroll chat",
  ]);
  requireIncludes("README captions parity", readme, [
    "Samsung/Android native captioning",
    "OpenClaw fallback",
    "sage-router/fast",
    "S1` / `S2",
  ]);
  requireIncludes("README AirVision profile backup boundary", readme, [
    "gesture/hotkey settings",
    "speaker state",
    "Samsung/native captions preference",
    "OpenClaw translation caption",
    "never includes gateway endpoints",
    "auth tokens",
    "chat history",
  ]);
  requireIncludes("README AirVision firmware capture results", readme, [
    "play/airvision-firmware-capture-results.json",
    "machine-checked sanitized",
    "Android firmware writes remain blocked",
  ]);

  return { path: readmePath };
}

async function verifyGeneratedAirVisionFirmwareCapturePlan() {
  runChecked(
    process.execPath,
    [airVisionFirmwareCapturePlanScript, "--check"],
    "AirVision firmware capture plan check",
  );
  const capturePlan = await readFile(airVisionFirmwareCapturePlanPath, "utf8");
  requireIncludes("AirVision firmware capture worksheet", capturePlan, [
    "## Capture Acceptance Criteria",
    "exact Windows write report ID",
    "sanitized payload summary",
    "Keep raw bytes only in private capture files.",
    "checksum/framing",
    "visible-state evidence",
    "## Capture Result Template",
    "Write payload summary",
    "Readback payload summary",
    "Android enablement decision",
    "| View Mode | `view_mode` | per-mode HUD profile active | working -> gaming -> infinity |",
    "| Light Load Mode | `light_load_mode` | low-overhead HUD profile active | off -> on |",
    "| View Mode | pending | pending | pending | pending | pending | pending | blocked |",
    "| Brightness | pending | pending | pending | pending | pending | pending | blocked |",
    "| IPD | pending | pending | pending | pending | pending | pending | blocked |",
    "| Light Load Mode | pending | pending | pending | pending | pending | pending | blocked |",
  ]);
  runChecked(
    process.execPath,
    [airVisionFirmwareCaptureResultsScript],
    "AirVision firmware capture results check",
  );
  const captureResults = await readFile(airVisionFirmwareCaptureResultsPath, "utf8");
  requireIncludes("AirVision firmware capture results", captureResults, [
    '"schema": "openclaw.airvision.firmwareCaptureResults"',
    '"rawKey": "view_mode"',
    '"rawKey": "brightness"',
    '"rawKey": "ipd"',
    '"rawKey": "light_load_mode"',
    '"androidEnablementDecision": "blocked"',
    "Windows ASUS HID protocol capture has not been validated.",
  ]);
}

async function verifySettingsAirVisionParity() {
  const settingsSheet = await readFile(settingsSheetPath, "utf8");
  requireIncludes("Settings AirVision companion parity", settingsSheet, [
    "AirVisionCompanionParity.fromState",
    'headlineContent = { Text("Companion Parity"',
    "airVisionCompanionParitySettingsText",
    "offline-reviewable",
    "M1-optional",
    "firmware-gated",
    "Windows-only",
  ]);
  return { path: settingsSheetPath };
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const bundlePath = args.bundle ?? (await latestHudBundle());
  const bundle = await verifyBundle(bundlePath, args.skipSignature);
  const manifest = await verifyManifest(args.manifest);
  const listing = await verifyListing(args.listingDir, args.language);
  const localeBundleConfig = await verifyRuntimeLocaleBundleConfig();
  const readmeParity = await verifyReadmeAirVisionParity();
  const settingsParity = await verifySettingsAirVisionParity();
  await verifyGeneratedAirVisionFirmwareCapturePlan();

  console.log(`Bundle: ${bundle.path} (${bundle.size} bytes)`);
  console.log(`SHA-256: ${bundle.sha256}`);
  console.log(`Manifest: ${args.manifest}`);
  console.log(`Package: ${manifest.packageName}`);
  console.log(`Version: ${manifest.versionName} (${manifest.versionCode})`);
  console.log(`Permissions: ${manifest.permissions} requested; restricted HUD permissions absent`);
  console.log(`Services: ${manifest.services}; notification listener declared`);
  console.log(
    `Listing ${args.language}: title ${listing.title}/30, short ${listing.shortDescription}/80, full ${listing.fullDescription}/4000, release notes ${listing.releaseNotes}/500`,
  );
  if (localeBundleConfig.usesRuntimeLocale) {
    console.log("Runtime locale delivery: App Bundle language splits disabled");
  }
  console.log(`README AirVision parity: ${readmeParity.path}`);
  console.log(`Settings AirVision parity: ${settingsParity.path}`);
  console.log("AirVision firmware capture plan/results: current");
  console.log("Play HUD release verifier passed.");
}

await main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
