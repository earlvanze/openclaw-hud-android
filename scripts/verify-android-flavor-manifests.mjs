#!/usr/bin/env node

import { readFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");

const variants = [
  {
    name: "hud",
    manifest: join(
      androidDir,
      "app",
      "build",
      "intermediates",
      "packaged_manifests",
      "hudRelease",
      "processHudReleaseManifestForPackage",
      "AndroidManifest.xml",
    ),
    buildConfig: join(
      androidDir,
      "app",
      "build",
      "generated",
      "source",
      "buildConfig",
      "hud",
      "release",
      "ai",
      "openclaw",
      "app",
      "BuildConfig.java",
    ),
    packageName: "ai.openclaw.app.hud",
    expectedPermissions: [
      "android.permission.INTERNET",
      "android.permission.ACCESS_NETWORK_STATE",
      "android.permission.FOREGROUND_SERVICE",
      "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
      "android.permission.POST_NOTIFICATIONS",
      "android.permission.NEARBY_WIFI_DEVICES",
      "android.permission.RECORD_AUDIO",
      "android.permission.MODIFY_AUDIO_SETTINGS",
    ],
    forbiddenPermissions: [
      "android.permission.SEND_SMS",
      "android.permission.READ_SMS",
      "android.permission.READ_CALL_LOG",
      "android.permission.ACCESS_FINE_LOCATION",
      "android.permission.ACCESS_COARSE_LOCATION",
      "android.permission.CAMERA",
      "android.permission.READ_MEDIA_IMAGES",
      "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
      "android.permission.READ_EXTERNAL_STORAGE",
      "android.permission.READ_CONTACTS",
      "android.permission.WRITE_CONTACTS",
      "android.permission.READ_CALENDAR",
      "android.permission.WRITE_CALENDAR",
      "android.permission.ACTIVITY_RECOGNITION",
    ],
    expectedFeatures: ["android.hardware.usb.host"],
    forbiddenFeatures: ["android.hardware.camera", "android.hardware.telephony"],
    expectedBuildConfig: {
      APPLICATION_ID: "ai.openclaw.app.hud",
      FLAVOR: "hud",
      OPENCLAW_DEFAULT_HUD: true,
      OPENCLAW_ENABLE_CALL_LOG: false,
      OPENCLAW_ENABLE_SMS: false,
    },
    expectedFileProviderAuthority: "ai.openclaw.app.hud.fileprovider",
  },
  {
    name: "play",
    manifest: join(
      androidDir,
      "app",
      "build",
      "intermediates",
      "packaged_manifests",
      "playRelease",
      "processPlayReleaseManifestForPackage",
      "AndroidManifest.xml",
    ),
    buildConfig: join(
      androidDir,
      "app",
      "build",
      "generated",
      "source",
      "buildConfig",
      "play",
      "release",
      "ai",
      "openclaw",
      "app",
      "BuildConfig.java",
    ),
    packageName: "ai.openclaw.app",
    expectedPermissions: [
      "android.permission.INTERNET",
      "android.permission.ACCESS_NETWORK_STATE",
      "android.permission.FOREGROUND_SERVICE",
      "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
      "android.permission.POST_NOTIFICATIONS",
      "android.permission.NEARBY_WIFI_DEVICES",
      "android.permission.ACCESS_FINE_LOCATION",
      "android.permission.ACCESS_COARSE_LOCATION",
      "android.permission.CAMERA",
      "android.permission.RECORD_AUDIO",
      "android.permission.MODIFY_AUDIO_SETTINGS",
      "android.permission.READ_MEDIA_IMAGES",
      "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
      "android.permission.READ_CONTACTS",
      "android.permission.WRITE_CONTACTS",
      "android.permission.READ_CALENDAR",
      "android.permission.WRITE_CALENDAR",
      "android.permission.ACTIVITY_RECOGNITION",
    ],
    forbiddenPermissions: [
      "android.permission.SEND_SMS",
      "android.permission.READ_SMS",
      "android.permission.READ_CALL_LOG",
    ],
    expectedFeatures: [
      "android.hardware.camera",
      "android.hardware.telephony",
      "android.hardware.usb.host",
    ],
    forbiddenFeatures: [],
    expectedBuildConfig: {
      APPLICATION_ID: "ai.openclaw.app",
      FLAVOR: "play",
      OPENCLAW_DEFAULT_HUD: false,
      OPENCLAW_ENABLE_CALL_LOG: false,
      OPENCLAW_ENABLE_SMS: false,
    },
    expectedFileProviderAuthority: "ai.openclaw.app.fileprovider",
  },
  {
    name: "thirdParty",
    manifest: join(
      androidDir,
      "app",
      "build",
      "intermediates",
      "packaged_manifests",
      "thirdPartyRelease",
      "processThirdPartyReleaseManifestForPackage",
      "AndroidManifest.xml",
    ),
    buildConfig: join(
      androidDir,
      "app",
      "build",
      "generated",
      "source",
      "buildConfig",
      "thirdParty",
      "release",
      "ai",
      "openclaw",
      "app",
      "BuildConfig.java",
    ),
    packageName: "ai.openclaw.app",
    expectedPermissions: [
      "android.permission.INTERNET",
      "android.permission.ACCESS_NETWORK_STATE",
      "android.permission.FOREGROUND_SERVICE",
      "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
      "android.permission.POST_NOTIFICATIONS",
      "android.permission.NEARBY_WIFI_DEVICES",
      "android.permission.ACCESS_FINE_LOCATION",
      "android.permission.ACCESS_COARSE_LOCATION",
      "android.permission.CAMERA",
      "android.permission.RECORD_AUDIO",
      "android.permission.MODIFY_AUDIO_SETTINGS",
      "android.permission.SEND_SMS",
      "android.permission.READ_SMS",
      "android.permission.READ_MEDIA_IMAGES",
      "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
      "android.permission.READ_CONTACTS",
      "android.permission.WRITE_CONTACTS",
      "android.permission.READ_CALL_LOG",
      "android.permission.READ_CALENDAR",
      "android.permission.WRITE_CALENDAR",
      "android.permission.ACTIVITY_RECOGNITION",
    ],
    forbiddenPermissions: [],
    expectedFeatures: [
      "android.hardware.camera",
      "android.hardware.telephony",
      "android.hardware.usb.host",
    ],
    forbiddenFeatures: [],
    expectedBuildConfig: {
      APPLICATION_ID: "ai.openclaw.app",
      FLAVOR: "thirdParty",
      OPENCLAW_DEFAULT_HUD: false,
      OPENCLAW_ENABLE_CALL_LOG: true,
      OPENCLAW_ENABLE_SMS: true,
    },
    expectedFileProviderAuthority: "ai.openclaw.app.fileprovider",
  },
];

const globallyForbiddenPermissions = [
  "android.permission.ACCESS_BACKGROUND_LOCATION",
  "android.permission.BIND_ACCESSIBILITY_SERVICE",
  "android.permission.MANAGE_EXTERNAL_STORAGE",
  "android.permission.QUERY_ALL_PACKAGES",
  "android.permission.REQUEST_INSTALL_PACKAGES",
  "android.permission.SYSTEM_ALERT_WINDOW",
];

function unique(values) {
  return [...new Set(values)].sort();
}

function extractManifestPackage(manifest) {
  return manifest.match(/<manifest\b[^>]*\bpackage="([^"]+)"/u)?.[1] ?? null;
}

function extractAttributeValues(source, tagName, attributeName) {
  const values = [];
  const tagPattern = new RegExp(`<${tagName}\\b[^>]*>`, "gu");
  for (const match of source.matchAll(tagPattern)) {
    const value = match[0].match(new RegExp(`\\b${attributeName}="([^"]+)"`, "u"))?.[1];
    if (value) values.push(value);
  }
  return unique(values);
}

function parseBuildConfig(source) {
  const values = {};
  for (const match of source.matchAll(/public static final (?:String|boolean) (\w+) = (true|false|"[^"]*");/gu)) {
    const [, key, rawValue] = match;
    if (!key || rawValue == null) continue;
    values[key] =
      rawValue === "true" ? true
        : rawValue === "false" ? false
          : rawValue.slice(1, -1);
  }
  return values;
}

function requireIncludesAll(actual, expected, label) {
  const missing = expected.filter((value) => !actual.includes(value));
  if (missing.length > 0) {
    throw new Error(`${label} missing: ${missing.join(", ")}`);
  }
}

function requireExcludesAll(actual, forbidden, label) {
  const present = forbidden.filter((value) => actual.includes(value));
  if (present.length > 0) {
    throw new Error(`${label} must not include: ${present.join(", ")}`);
  }
}

async function verifyVariant(variant) {
  const manifest = await readFile(variant.manifest, "utf8");
  const buildConfig = parseBuildConfig(await readFile(variant.buildConfig, "utf8"));
  const packageName = extractManifestPackage(manifest);
  const permissions = extractAttributeValues(manifest, "uses-permission", "android:name");
  const features = extractAttributeValues(manifest, "uses-feature", "android:name");
  const serviceNames = extractAttributeValues(manifest, "service", "android:name");
  const activityNames = extractAttributeValues(manifest, "activity", "android:name");
  const providerAuthorities = extractAttributeValues(manifest, "provider", "android:authorities");

  if (packageName !== variant.packageName) {
    throw new Error(`${variant.name} package expected ${variant.packageName}, got ${packageName}`);
  }

  requireIncludesAll(permissions, variant.expectedPermissions, `${variant.name} permissions`);
  requireExcludesAll(permissions, variant.forbiddenPermissions, `${variant.name} permissions`);
  requireExcludesAll(permissions, globallyForbiddenPermissions, `${variant.name} permissions`);
  requireIncludesAll(features, variant.expectedFeatures, `${variant.name} features`);
  requireExcludesAll(features, variant.forbiddenFeatures, `${variant.name} features`);
  requireIncludesAll(
    serviceNames,
    [
      "ai.openclaw.app.NodeForegroundService",
      "ai.openclaw.app.node.DeviceNotificationListenerService",
    ],
    `${variant.name} services`,
  );
  requireIncludesAll(activityNames, ["ai.openclaw.app.MainActivity"], `${variant.name} activities`);
  requireIncludesAll(providerAuthorities, [variant.expectedFileProviderAuthority], `${variant.name} providers`);

  for (const [key, expected] of Object.entries(variant.expectedBuildConfig)) {
    if (buildConfig[key] !== expected) {
      throw new Error(`${variant.name} BuildConfig.${key} expected ${expected}, got ${buildConfig[key]}`);
    }
  }

  return {
    name: variant.name,
    packageName,
    permissionCount: permissions.length,
    featureCount: features.length,
  };
}

const results = [];
for (const variant of variants) {
  results.push(await verifyVariant(variant));
}

for (const result of results) {
  console.log(
    `${result.name}: ${result.packageName}, ${result.permissionCount} permissions, ${result.featureCount} features`,
  );
}
console.log("Android flavor manifest verifier passed.");
