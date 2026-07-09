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
const defaultInAppPrivacyPolicyPath = join(androidDir, "app", "src", "main", "java", "ai", "openclaw", "app", "PrivacyPolicyText.kt");
const defaultSettingsSheetPath = join(androidDir, "app", "src", "main", "java", "ai", "openclaw", "app", "ui", "SettingsSheet.kt");
const defaultDataSafetyNotesPath = join(androidDir, "play", "data-safety-notes.md");
const defaultConsoleChecklistPath = join(androidDir, "play", "console-checklist.md");
const defaultListingDir = join(androidDir, "play", "listings", "en-US");

const expectedPackage = "ai.openclaw.app.hud";
const expectedSchema = "openclaw.play.app-content";

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
          "privacy policy, in-app privacy policy, data-safety notes, console checklist, and English listing files.",
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

function verifyInAppPolicyMirrorsHostedPolicy(hostedPolicy, inAppPolicy) {
  const hosted = normalizePolicyText(hostedPolicy);
  const inApp = normalizePolicyText(inAppPolicy);
  const coreDisclosures = [
    "OpenClaw HUD connects your Android device to an OpenClaw gateway that you configure",
    "The Google Play HUD build may request microphone, notification, nearby-device, network, foreground-service, and audio-routing permissions",
    "Notification access is optional",
    "Voice and caption features may send microphone transcripts, caption text, chat text, assistant status, and selected AirVision HUD settings",
    "OpenClaw HUD does not sell personal data and does not include advertising",
  ];

  requireIncludes("Hosted privacy policy parity", hosted, coreDisclosures);
  requireIncludes("In-app privacy policy parity", inApp, coreDisclosures);
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

function verifyFinalSubmissionReadiness(appContent) {
  const finalSubmission = appContent.finalSubmission ?? {};
  const problems = [];
  if (!isHttpsUrl(finalSubmission.hostedPrivacyPolicyUrl)) {
    problems.push("finalSubmission.hostedPrivacyPolicyUrl must be a public https:// URL.");
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
  const inAppPrivacyPolicy = await readText(args.inAppPrivacyPolicy);
  const settingsSheet = await readText(args.settingsSheet);
  const dataSafetyNotes = await readText(args.dataSafetyNotes);
  const consoleChecklist = await readText(args.consoleChecklist);

  await verifyListing(args.listingDir);
  verifyAppContentShape(appContent);
  if (args.final) verifyFinalSubmissionReadiness(appContent);
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
    "verify-play-submission-package",
  ]);

  console.log(`App-content package: ${appContent.packageName}`);
  console.log(`Manifest permissions checked: ${permissionCount}`);
  console.log(`Privacy policy: ${args.privacyPolicy}`);
  console.log(`In-app privacy policy: ${args.inAppPrivacyPolicy}`);
  console.log(`App-content answers: ${args.appContent}`);
  console.log(`Play submission package verifier passed (${args.final ? "final" : "draft"} mode).`);
}

await main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
