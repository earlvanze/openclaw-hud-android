#!/usr/bin/env bun

import { $ } from "bun";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const buildGradlePath = join(androidDir, "app", "build.gradle.kts");
const releaseOutputDir = join(androidDir, "build", "release-bundles");

const releaseVariants = [
  {
    flavorName: "hud",
    gradleTask: ":app:bundleHudRelease",
    bundlePath: join(androidDir, "app", "build", "outputs", "bundle", "hudRelease", "app-hud-release.aab"),
  },
  {
    flavorName: "play",
    gradleTask: ":app:bundlePlayRelease",
    bundlePath: join(androidDir, "app", "build", "outputs", "bundle", "playRelease", "app-play-release.aab"),
  },
  {
    flavorName: "third-party",
    gradleTask: ":app:bundleThirdPartyRelease",
    bundlePath: join(
      androidDir,
      "app",
      "build",
      "outputs",
      "bundle",
      "thirdPartyRelease",
      "app-thirdParty-release.aab",
    ),
  },
] as const;

type VersionState = {
  versionName: string;
  versionCode: number;
};

type ParsedVersionMatches = {
  versionNameMatch: RegExpMatchArray;
  versionCodeMatch: RegExpMatchArray;
};

type ParsedArgs = {
  variants: Array<(typeof releaseVariants)[number]>;
  skipVersionBump: boolean;
};

function parseArgs(argv: string[]): ParsedArgs {
  const selected: string[] = [];
  let skipVersionBump = false;

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--flavor") {
      const flavor = argv[++index];
      if (!flavor) throw new Error("--flavor requires hud, play, or third-party");
      selected.push(flavor);
    } else if (arg === "--skip-version-bump") {
      skipVersionBump = true;
    } else if (arg === "--help" || arg === "-h") {
      console.log("Usage: scripts/build-release-aab.ts [--flavor hud|play|third-party] [--skip-version-bump]");
      process.exit(0);
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }

  const variants =
    selected.length === 0
      ? [...releaseVariants]
      : selected.map((name) => {
          const variant = releaseVariants.find((candidate) => candidate.flavorName === name);
          if (!variant) throw new Error(`Unknown flavor: ${name}`);
          return variant;
        });

  return { variants, skipVersionBump };
}

function formatVersionName(date: Date): string {
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();
  return `${year}.${month}.${day}`;
}

function formatVersionCodePrefix(date: Date): string {
  const year = date.getFullYear().toString();
  const month = (date.getMonth() + 1).toString().padStart(2, "0");
  const day = date.getDate().toString().padStart(2, "0");
  return `${year}${month}${day}`;
}

function parseVersionMatches(buildGradleText: string): ParsedVersionMatches {
  const versionCodeMatch = buildGradleText.match(/versionCode = (\d+)/);
  const versionNameMatch = buildGradleText.match(/versionName = "([^"]+)"/);
  if (!versionCodeMatch || !versionNameMatch) {
    throw new Error(`Couldn't parse versionName/versionCode from ${buildGradlePath}`);
  }
  return { versionCodeMatch, versionNameMatch };
}

function resolveNextVersionCode(currentVersionCode: number, todayPrefix: string): number {
  const currentRaw = currentVersionCode.toString();
  let nextSuffix = 0;

  if (currentRaw.startsWith(todayPrefix)) {
    const suffixRaw = currentRaw.slice(todayPrefix.length);
    nextSuffix = (suffixRaw ? Number.parseInt(suffixRaw, 10) : 0) + 1;
  }

  if (!Number.isInteger(nextSuffix) || nextSuffix < 0 || nextSuffix > 99) {
    throw new Error(
      `Can't auto-bump Android versionCode for ${todayPrefix}: next suffix ${nextSuffix} is invalid`,
    );
  }

  return Number.parseInt(`${todayPrefix}${nextSuffix.toString().padStart(2, "0")}`, 10);
}

function resolveNextVersion(buildGradleText: string, date: Date): VersionState {
  const { versionCodeMatch } = parseVersionMatches(buildGradleText);
  const currentVersionCode = Number.parseInt(versionCodeMatch[1] ?? "", 10);
  if (!Number.isInteger(currentVersionCode)) {
    throw new Error(`Invalid Android versionCode in ${buildGradlePath}`);
  }

  const todayPrefix = formatVersionCodePrefix(date);
  const versionCode = resolveNextVersionCode(currentVersionCode, todayPrefix);
  const versionSuffix = Number.parseInt(versionCode.toString().slice(todayPrefix.length), 10);
  if (!Number.isInteger(versionSuffix)) {
    throw new Error(`Invalid Android version suffix for ${versionCode}`);
  }
  const versionName = `${formatVersionName(date)}.${versionSuffix}`;
  return { versionName, versionCode };
}

function updateBuildGradleVersions(buildGradleText: string, nextVersion: VersionState): string {
  return buildGradleText
    .replace(/versionCode = \d+/, `versionCode = ${nextVersion.versionCode}`)
    .replace(/versionName = "[^"]+"/, `versionName = "${nextVersion.versionName}"`);
}

async function sha256Hex(path: string): Promise<string> {
  const buffer = await Bun.file(path).arrayBuffer();
  const digest = await crypto.subtle.digest("SHA-256", buffer);
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

async function verifyBundleSignature(path: string): Promise<void> {
  await $`jarsigner -verify ${path}`.quiet();
}

async function copyBundle(sourcePath: string, destinationPath: string): Promise<void> {
  const sourceFile = Bun.file(sourcePath);
  if (!(await sourceFile.exists())) {
    throw new Error(`Signed bundle missing at ${sourcePath}`);
  }

  await Bun.write(destinationPath, sourceFile);
}

async function main() {
  const { variants, skipVersionBump } = parseArgs(process.argv.slice(2));
  const buildGradleFile = Bun.file(buildGradlePath);
  const originalText = await buildGradleFile.text();
  const currentVersionMatches = parseVersionMatches(originalText);
  let nextVersion: VersionState = {
    versionName: currentVersionMatches.versionNameMatch[1] ?? "",
    versionCode: Number.parseInt(currentVersionMatches.versionCodeMatch[1] ?? "", 10),
  };

  if (!Number.isInteger(nextVersion.versionCode)) {
    throw new Error(`Invalid Android versionCode in ${buildGradlePath}`);
  }

  if (!skipVersionBump) {
    nextVersion = resolveNextVersion(originalText, new Date());
    const updatedText = updateBuildGradleVersions(originalText, nextVersion);

    if (updatedText === originalText) {
      throw new Error("Android version bump produced no change");
    }

    await Bun.write(buildGradlePath, updatedText);
  }

  console.log(`Android versionName: ${nextVersion.versionName}`);
  console.log(`Android versionCode: ${nextVersion.versionCode}`);
  console.log(`Release flavors: ${variants.map((variant) => variant.flavorName).join(", ")}`);

  await $`mkdir -p ${releaseOutputDir}`;

  try {
    await $`./gradlew ${variants.map((variant) => variant.gradleTask)}`.cwd(androidDir);
  } catch (error) {
    if (!skipVersionBump) await Bun.write(buildGradlePath, originalText);
    throw error;
  }

  for (const variant of variants) {
    const outputPath = join(
      releaseOutputDir,
      `openclaw-${nextVersion.versionName}-${variant.flavorName}-release.aab`,
    );

    await copyBundle(variant.bundlePath, outputPath);
    await verifyBundleSignature(outputPath);
    const hash = await sha256Hex(outputPath);

    console.log(`Signed AAB (${variant.flavorName}): ${outputPath}`);
    console.log(`SHA-256 (${variant.flavorName}): ${hash}`);
  }
}

await main();
