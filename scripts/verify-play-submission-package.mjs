#!/usr/bin/env node

import { readFile, stat } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
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
const defaultAppContentPath = join(androidDir, "play", "app-content-answers.json");
const defaultPrivacyPolicyPath = join(androidDir, "play", "privacy-policy.md");
const defaultHostedPrivacyPolicyPagePath = join(androidDir, "docs", "privacy-policy.html");
const defaultInAppPrivacyPolicyPath = join(androidDir, "app", "src", "main", "java", "ai", "openclaw", "app", "PrivacyPolicyText.kt");
const defaultSettingsSheetPath = join(androidDir, "app", "src", "main", "java", "ai", "openclaw", "app", "ui", "SettingsSheet.kt");
const defaultDataSafetyNotesPath = join(androidDir, "play", "data-safety-notes.md");
const defaultConsoleChecklistPath = join(androidDir, "play", "console-checklist.md");
const defaultListingDir = join(androidDir, "play", "listings", "en-US");

const expectedPackage = "ai.openclaw.app.hud";
const expectedSchema = "openclaw.play.app-content";
const maxScreenshotBytes = 8 * 1024 * 1024;
const screenshotMinDimension = 320;
const screenshotMaxDimension = 3840;
const hostedPolicyFetchTimeoutMs = 15_000;
const pngSignature = Buffer.from("89504e470d0a1a0a", "hex");
const corePrivacyDisclosures = [
  "OpenClaw HUD connects your Android device to an OpenClaw gateway that you configure",
  "The Google Play HUD build may request microphone, notification, nearby-device, network, foreground-service, and audio-routing permissions",
  "Notification access is optional",
  "Voice and caption features may send microphone transcripts, caption text, chat text, assistant status, and selected AirVision HUD settings",
  "OpenClaw HUD does not sell personal data and does not include advertising",
];

const forbiddenPermissionGroups = {
  sms: ["android.permission.SEND_SMS", "android.permission.READ_SMS"],
  callLog: ["android.permission.READ_CALL_LOG"],
  camera: ["android.permission.CAMERA"],
  location: ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_BACKGROUND_LOCATION"],
  contacts: ["android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS"],
  calendar: ["android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR"],
  mediaLibrary: [
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.READ_MEDIA_IMAGES",
    "android.permission.READ_MEDIA_VIDEO",
    "android.permission.READ_MEDIA_AUDIO",
    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
  ],
  overlayWindows: ["android.permission.SYSTEM_ALERT_WINDOW"],
};

const requiredTruePermissionGroups = {
  microphone: ["android.permission.RECORD_AUDIO"],
  notifications: ["android.permission.POST_NOTIFICATIONS"],
  nearbyDevices: ["android.permission.NEARBY_WIFI_DEVICES"],
};

function parseArgs(argv) {
  const args = {
    manifest: defaultManifestPath,
    appContent: defaultAppContentPath,
    privacyPolicy: defaultPrivacyPolicyPath,
    hostedPrivacyPolicyPage: defaultHostedPrivacyPolicyPagePath,
    inAppPrivacyPolicy: defaultInAppPrivacyPolicyPath,
    settingsSheet: defaultSettingsSheetPath,
    dataSafetyNotes: defaultDataSafetyNotesPath,
    consoleChecklist: defaultConsoleChecklistPath,
    listingDir: defaultListingDir,
    final: false,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--manifest") args.manifest = resolve(argv[++index]);
    else if (arg === "--app-content") args.appContent = resolve(argv[++index]);
    else if (arg === "--privacy-policy") args.privacyPolicy = resolve(argv[++index]);
    else if (arg === "--hosted-privacy-policy-page") args.hostedPrivacyPolicyPage = resolve(argv[++index]);
    else if (arg === "--in-app-privacy-policy") args.inAppPrivacyPolicy = resolve(argv[++index]);
    else if (arg === "--settings-sheet") args.settingsSheet = resolve(argv[++index]);
    else if (arg === "--data-safety-notes") args.dataSafetyNotes = resolve(argv[++index]);
    else if (arg === "--console-checklist") args.consoleChecklist = resolve(argv[++index]);
    else if (arg === "--listing-dir") args.listingDir = resolve(argv[++index]);
    else if (arg === "--final") args.final = true;
    else if (arg === "--help" || arg === "-h") {
      console.log(
        [
          "Usage: node scripts/verify-play-submission-package.mjs [--manifest path]",
          "",
          "Checks the local Google Play submission packet against the generated HUD manifest,",
          "privacy policy, hosted privacy page, in-app privacy policy, data-safety notes, console checklist, and English listing files.",
          "",
          "Add --final to require external Play Console readiness fields such as hosted privacy URL,",
          "phone screenshots, reviewer access, tester access, and app creation status.",
        ].join("\n"),
      );
      process.exit(0);
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }

  return args;
}

async function readText(path) {
  return readFile(path, "utf8");
}

async function readJson(path) {
  return JSON.parse(await readText(path));
}

async function requireFile(path, label) {
  const info = await stat(path);
  if (!info.isFile() || info.size <= 0) throw new Error(`${label} is missing or empty: ${path}`);
  return info.size;
}

function openingTags(xml, tagName) {
  return [...xml.matchAll(new RegExp(`<${tagName}\\b[^>]*>`, "gu"))].map((match) => match[0]);
}

function attr(element, attrName) {
  const escaped = attrName.replace(/[.*+?^${}()|[\]\\]/gu, "\\$&");
  return element.match(new RegExp(`\\b${escaped}\\s*=\\s*"([^"]*)"`, "u"))?.[1] ?? null;
}

function manifestPermissions(xml) {
  return new Set(
    openingTags(xml, "uses-permission")
      .map((tag) => attr(tag, "android:name"))
      .filter((value) => value !== null),
  );
}

function manifestServicePermissions(xml) {
  return new Set(
    openingTags(xml, "service")
      .map((tag) => attr(tag, "android:permission"))
      .filter((value) => value !== null),
  );
}

function requireIncludes(label, text, needles) {
  const lower = text.toLowerCase();
  const missing = needles.filter((needle) => !lower.includes(needle.toLowerCase()));
  if (missing.length > 0) throw new Error(`${label} is missing required text: ${missing.join(", ")}`);
}

function normalizePolicyText(text) {
  return text
    .replace(/^# .+$/gmu, "")
    .replace(/`/gu, "")
    .replace(/\s+/gu, " ")
    .trim();
}

function normalizeHtmlPolicyText(text) {
  return text
    .replace(/<style\b[^>]*>[\s\S]*?<\/style>/giu, " ")
    .replace(/<script\b[^>]*>[\s\S]*?<\/script>/giu, " ")
    .replace(/<[^>]+>/gu, " ")
    .replace(/&gt;/gu, ">")
    .replace(/&lt;/gu, "<")
    .replace(/&quot;/gu, '"')
    .replace(/&amp;/gu, "&")
    .replace(/\s+/gu, " ")
    .trim();
}

function verifyInAppPolicyMirrorsHostedPolicy(hostedPolicy, inAppPolicy) {
  const hosted = normalizePolicyText(hostedPolicy);
  const inApp = normalizePolicyText(inAppPolicy);

  requireIncludes("Hosted privacy policy parity", hosted, corePrivacyDisclosures);
  requireIncludes("In-app privacy policy parity", inApp, corePrivacyDisclosures);
}

function verifyHostedPrivacyPolicyPage(hostedPolicy, hostedPolicyPage) {
  const page = normalizeHtmlPolicyText(hostedPolicyPage);
  requireIncludes("Hosted privacy policy page", hostedPolicyPage, [
    "<!doctype html>",
    "<title>OpenClaw HUD Privacy Policy</title>",
    "<meta name=\"viewport\"",
  ]);
  requireIncludes("Hosted privacy policy page parity", page, corePrivacyDisclosures);
  requireIncludes("Hosted privacy policy source parity", normalizePolicyText(hostedPolicy), corePrivacyDisclosures);
}

function requireArrayIncludes(label, values, expected) {
  const missing = expected.filter((value) => !values.includes(value));
  if (missing.length > 0) throw new Error(`${label} is missing: ${missing.join(", ")}`);
}

function requireBoolean(value, expected, label) {
  if (value !== expected) throw new Error(`${label} expected ${expected}, got ${value}`);
}

function isHttpsUrl(value) {
  if (typeof value !== "string" || value.trim() === "") return false;
  try {
    return new URL(value).protocol === "https:";
  } catch {
    return false;
  }
}

async function fetchText(url, timeoutMs) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, {
      signal: controller.signal,
      headers: { "user-agent": "openclaw-play-submission-verifier" },
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return await response.text();
  } finally {
    clearTimeout(timeout);
  }
}

async function verifyHostedPrivacyPolicyUrl(hostedUrl, localHostedPage, problems) {
  try {
    const remotePage = await fetchText(hostedUrl, hostedPolicyFetchTimeoutMs);
    requireIncludes("Hosted privacy policy URL response", remotePage, [
      "<!doctype html>",
      "<title>OpenClaw HUD Privacy Policy</title>",
      "<meta name=\"viewport\"",
    ]);
    requireIncludes("Hosted privacy policy URL parity", normalizeHtmlPolicyText(remotePage), corePrivacyDisclosures);

    if (normalizeHtmlPolicyText(remotePage) !== normalizeHtmlPolicyText(localHostedPage)) {
      problems.push("finalSubmission.hostedPrivacyPolicyUrl content must match docs/privacy-policy.html.");
    }
  } catch (error) {
    problems.push(`finalSubmission.hostedPrivacyPolicyUrl must be reachable and policy-complete: ${error.message}`);
  }
}

function resolveScreenshotPath(value) {
  const trimmed = value.trim();
  if (isHttpsUrl(trimmed)) return null;
  return resolve(androidDir, trimmed);
}

function readPngInfo(buffer) {
  if (!buffer.subarray(0, pngSignature.length).equals(pngSignature)) return null;
  const ihdrLength = buffer.readUInt32BE(8);
  const ihdrType = buffer.toString("ascii", 12, 16);
  if (ihdrLength !== 13 || ihdrType !== "IHDR") throw new Error("PNG is missing a valid IHDR chunk.");
  return {
    type: "PNG",
    width: buffer.readUInt32BE(16),
    height: buffer.readUInt32BE(20),
    bitDepth: buffer[24],
    colorType: buffer[25],
  };
}

function isJpegStartOfFrame(marker) {
  return (
    (marker >= 0xc0 && marker <= 0xc3) ||
    (marker >= 0xc5 && marker <= 0xc7) ||
    (marker >= 0xc9 && marker <= 0xcb) ||
    (marker >= 0xcd && marker <= 0xcf)
  );
}

function readJpegInfo(buffer) {
  if (buffer.length < 4 || buffer[0] !== 0xff || buffer[1] !== 0xd8) return null;
  let offset = 2;
  while (offset < buffer.length) {
    while (offset < buffer.length && buffer[offset] !== 0xff) offset += 1;
    while (offset < buffer.length && buffer[offset] === 0xff) offset += 1;
    if (offset >= buffer.length) break;

    const marker = buffer[offset];
    offset += 1;
    if (marker === 0xd9 || marker === 0xda) break;
    if (marker >= 0xd0 && marker <= 0xd7) continue;
    if (offset + 2 > buffer.length) break;
    const length = buffer.readUInt16BE(offset);
    if (length < 2 || offset + length > buffer.length) break;
    if (isJpegStartOfFrame(marker)) {
      return {
        type: "JPEG",
        width: buffer.readUInt16BE(offset + 5),
        height: buffer.readUInt16BE(offset + 3),
      };
    }
    offset += length;
  }
  throw new Error("JPEG dimensions could not be read.");
}

function verifyScreenshotDimensions(info, label, problems) {
  const minDimension = Math.min(info.width, info.height);
  const maxDimension = Math.max(info.width, info.height);
  if (minDimension < screenshotMinDimension) {
    problems.push(`${label} is too small: ${info.width}x${info.height}; minimum dimension is ${screenshotMinDimension}px.`);
  }
  if (maxDimension > screenshotMaxDimension) {
    problems.push(`${label} is too large: ${info.width}x${info.height}; maximum dimension is ${screenshotMaxDimension}px.`);
  }
  if (maxDimension > minDimension * 2) {
    problems.push(`${label} aspect ratio is too extreme: ${info.width}x${info.height}; max dimension must be no more than 2x the min dimension.`);
  }
}

async function verifyScreenshotFile(screenshot, index, problems) {
  const label = `finalSubmission.phoneScreenshots[${index}]`;
  if (isHttpsUrl(screenshot)) return;

  const path = resolveScreenshotPath(screenshot);
  let fileInfo;
  try {
    fileInfo = await stat(path);
  } catch {
    problems.push(`${label} local file does not exist: ${screenshot}`);
    return;
  }

  if (!fileInfo.isFile()) {
    problems.push(`${label} is not a file: ${screenshot}`);
    return;
  }
  if (fileInfo.size <= 0) {
    problems.push(`${label} is empty: ${screenshot}`);
    return;
  }
  if (fileInfo.size > maxScreenshotBytes) {
    problems.push(`${label} is ${(fileInfo.size / (1024 * 1024)).toFixed(1)} MiB; maximum is 8 MiB.`);
  }

  const bytes = await readFile(path);
  try {
    const info = readPngInfo(bytes) ?? readJpegInfo(bytes);
    if (!info) {
      problems.push(`${label} must be a JPEG or PNG file: ${screenshot}`);
      return;
    }
    if (info.type === "PNG" && (info.bitDepth !== 8 || info.colorType !== 2)) {
      problems.push(`${label} PNG must be 24-bit RGB without alpha; got bit depth ${info.bitDepth}, color type ${info.colorType}.`);
    }
    verifyScreenshotDimensions(info, label, problems);
  } catch (error) {
    problems.push(`${label} could not be validated: ${error.message}`);
  }
}

async function verifyFinalSubmissionReadiness(appContent, hostedPrivacyPolicyPage) {
  const finalSubmission = appContent.finalSubmission ?? {};
  const problems = [];
  if (!isHttpsUrl(finalSubmission.hostedPrivacyPolicyUrl)) {
    problems.push("finalSubmission.hostedPrivacyPolicyUrl must be a public https:// URL.");
  } else {
    await verifyHostedPrivacyPolicyUrl(finalSubmission.hostedPrivacyPolicyUrl, hostedPrivacyPolicyPage, problems);
  }
  if (finalSubmission.appCreatedInPlayConsole !== true) {
    problems.push("finalSubmission.appCreatedInPlayConsole must be true after ai.openclaw.app.hud exists in Play Console.");
  }
  if (finalSubmission.internalTestersConfiguredInPlayConsole !== true) {
    problems.push("finalSubmission.internalTestersConfiguredInPlayConsole must be true before internal-track release.");
  }
  if (finalSubmission.reviewerAccessConfiguredInPlayConsole !== true) {
    problems.push("finalSubmission.reviewerAccessConfiguredInPlayConsole must be true after App access review instructions/codes are entered in Play Console.");
  }
  const phoneScreenshots = Array.isArray(finalSubmission.phoneScreenshots) ? finalSubmission.phoneScreenshots : [];
  if (phoneScreenshots.length < 2) {
    problems.push("finalSubmission.phoneScreenshots must list at least two Play Console phone screenshots.");
  }
  for (const screenshot of phoneScreenshots) {
    if (typeof screenshot !== "string" || screenshot.trim() === "") {
      problems.push("finalSubmission.phoneScreenshots entries must be non-empty strings.");
      break;
    }
  }
  await Promise.all(
    phoneScreenshots
      .map((screenshot, index) => ({ screenshot, index }))
      .filter((entry) => typeof entry.screenshot === "string" && entry.screenshot.trim() !== "")
      .map((entry) => verifyScreenshotFile(entry.screenshot, entry.index, problems)),
  );
  if (problems.length > 0) {
    throw new Error(["Final Play submission readiness failed:", ...problems.map((problem) => `- ${problem}`)].join("\n"));
  }
}

function verifyAppContentShape(appContent) {
  if (appContent.schema !== expectedSchema) throw new Error(`Unsupported app-content schema: ${appContent.schema}`);
  if (appContent.version !== 1) throw new Error(`Unsupported app-content version: ${appContent.version}`);
  if (appContent.packageName !== expectedPackage) throw new Error(`App-content package is ${appContent.packageName}, expected ${expectedPackage}`);
  requireBoolean(appContent.ads?.containsAds, false, "Ads declaration");
  requireBoolean(appContent.targetAudience?.designedForChildren, false, "Target-audience children declaration");
  requireBoolean(appContent.dataDeletion?.accountCreationInApp, false, "Account creation declaration");
  requireBoolean(appContent.dataDeletion?.webDeletionRequestRequired, false, "Web deletion request declaration");
  requireBoolean(appContent.dataSafety?.sellsData, false, "Data sale declaration");
  requireBoolean(appContent.dataSafety?.dataUsedForAdvertising, false, "Advertising data declaration");
  requireBoolean(appContent.dataSafety?.dataDeletionSupported, true, "Data deletion support declaration");
  if (appContent.dataSafety?.dataEncryptedInTransitAnswer !== "no_not_all_paths") {
    throw new Error("Data safety transport-security answer must reflect that not all self-hosted gateway paths are encrypted.");
  }
  if (!appContent.privacyPolicy?.requiresHostedUrlBeforeSubmission) {
    throw new Error("Privacy policy must require a hosted URL before Play submission.");
  }
  if (appContent.privacyPolicy?.sourceFile !== "play/privacy-policy.md") {
    throw new Error("Privacy policy sourceFile must point to play/privacy-policy.md.");
  }
  if (appContent.privacyPolicy?.hostedPageSource !== "docs/privacy-policy.html") {
    throw new Error("Privacy policy hostedPageSource must point to docs/privacy-policy.html.");
  }
  if (!isHttpsUrl(appContent.privacyPolicy?.hostedUrlCandidate)) {
    throw new Error("Privacy policy hostedUrlCandidate must be a public https:// URL candidate.");
  }
  if (!appContent.privacyPolicy?.inAppLocation?.includes("Privacy Policy")) {
    throw new Error("Privacy policy in-app location must point to the Privacy Policy row.");
  }
  if (!appContent.appAccess?.restrictedFeatures || !appContent.appAccess.reviewAccessInstructions?.includes("Demo Mode")) {
    throw new Error("App access instructions must document Demo Mode review access.");
  }
  requireArrayIncludes(
    "Data safety not-collected list",
    appContent.dataSafety?.notCollected ?? [],
    ["Advertising ID", "Precise location", "Contacts", "Calendar", "SMS", "Call logs", "Photos and videos"],
  );
  const dataTypes = (appContent.dataSafety?.collectedData ?? []).map((entry) => entry.playType);
  requireArrayIncludes("Data safety collected data types", dataTypes, ["Audio", "App activity", "App info and performance"]);
}

function verifyManifestAgainstAppContent(xml, appContent) {
  const manifestTag = xml.match(/<manifest\b[^>]*>/u)?.[0];
  if (!manifestTag) throw new Error("No <manifest> tag found in HUD manifest.");
  const packageName = attr(manifestTag, "package");
  if (packageName !== expectedPackage) throw new Error(`HUD manifest package is ${packageName}, expected ${expectedPackage}`);

  const permissions = manifestPermissions(xml);
  const servicePermissions = manifestServicePermissions(xml);
  if (!servicePermissions.has("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE")) {
    throw new Error("HUD manifest must declare the notification-listener service permission.");
  }
  if (servicePermissions.has("android.permission.BIND_ACCESSIBILITY_SERVICE")) {
    throw new Error("HUD manifest declares AccessibilityService but app-content marks it unsupported.");
  }

  for (const [group, groupPermissions] of Object.entries(forbiddenPermissionGroups)) {
    const hasForbidden = groupPermissions.some((permission) => permissions.has(permission));
    if (hasForbidden) throw new Error(`HUD manifest contains forbidden ${group} permission.`);
    requireBoolean(appContent.sensitivePermissions?.[group], false, `Sensitive permission ${group}`);
  }

  for (const [group, groupPermissions] of Object.entries(requiredTruePermissionGroups)) {
    const present = groupPermissions.some((permission) => permissions.has(permission));
    if (!present) throw new Error(`HUD manifest is missing expected ${group} permission.`);
    requireBoolean(appContent.sensitivePermissions?.[group], true, `Sensitive permission ${group}`);
  }

  return permissions.size;
}

async function verifyListing(listingDir) {
  const files = ["title.txt", "short-description.txt", "full-description.txt", "release-notes.txt"];
  for (const file of files) await requireFile(join(listingDir, file), `Listing ${file}`);
  const fullDescription = await readText(join(listingDir, "full-description.txt"));
  const releaseNotes = await readText(join(listingDir, "release-notes.txt"));
  requireIncludes("Listing full description", fullDescription, ["in-app privacy policy"]);
  requireIncludes("Listing release notes", releaseNotes, ["In-app privacy policy"]);
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const appContent = await readJson(args.appContent);
  const manifestXml = await readText(args.manifest);
  const privacyPolicy = await readText(args.privacyPolicy);
  const hostedPrivacyPolicyPage = await readText(args.hostedPrivacyPolicyPage);
  const inAppPrivacyPolicy = await readText(args.inAppPrivacyPolicy);
  const settingsSheet = await readText(args.settingsSheet);
  const dataSafetyNotes = await readText(args.dataSafetyNotes);
  const consoleChecklist = await readText(args.consoleChecklist);

  await verifyListing(args.listingDir);
  verifyAppContentShape(appContent);
  if (args.final) await verifyFinalSubmissionReadiness(appContent, hostedPrivacyPolicyPage);
  const permissionCount = verifyManifestAgainstAppContent(manifestXml, appContent);

  requireIncludes("Privacy policy", privacyPolicy, [
    "OpenClaw HUD Privacy Policy",
    "microphone",
    "notification",
    "gateway",
    "encrypted storage",
    "does not sell personal data",
    "clear data",
  ]);
  requireIncludes("In-app privacy policy", inAppPrivacyPolicy, [
    "OpenClaw HUD Privacy Policy",
    "microphone",
    "notification",
    "gateway",
    "encrypted storage",
    "does not sell personal data",
    "clear data",
  ]);
  verifyInAppPolicyMirrorsHostedPolicy(privacyPolicy, inAppPrivacyPolicy);
  verifyHostedPrivacyPolicyPage(privacyPolicy, hostedPrivacyPolicyPage);
  requireIncludes("Settings privacy policy surface", settingsSheet, [
    "PrivacyPolicyText",
    "Privacy Policy",
    "showPrivacyPolicy",
    "handles microphone, notification, gateway, and local app data",
  ]);
  requireIncludes("Data safety notes", dataSafetyNotes, [
    "No advertising",
    "Optional microphone",
    "Optional notification-listener",
    "Data is sent only to the OpenClaw gateway endpoint configured by the user",
  ]);
  requireIncludes("Console checklist", consoleChecklist, [
    "Data safety",
    "App access",
    "Target audience",
    "Data deletion",
    "in-app privacy",
    "capture-play-screenshots",
    "render-privacy-policy-site",
    "verify-play-submission-package",
  ]);

  console.log(`App-content package: ${appContent.packageName}`);
  console.log(`Manifest permissions checked: ${permissionCount}`);
  console.log(`Privacy policy: ${args.privacyPolicy}`);
  console.log(`Hosted privacy policy page: ${args.hostedPrivacyPolicyPage}`);
  console.log(`In-app privacy policy: ${args.inAppPrivacyPolicy}`);
  console.log(`App-content answers: ${args.appContent}`);
  console.log(`Play submission package verifier passed (${args.final ? "final" : "draft"} mode).`);
}

await main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
