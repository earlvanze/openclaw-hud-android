#!/usr/bin/env node

import { createHash } from "node:crypto";
import { access, copyFile, mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { spawn } from "node:child_process";

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
    bundlePath: join(androidDir, "app", "build", "outputs", "bundle", "thirdPartyRelease", "app-thirdParty-release.aab"),
  },
];

function parseArgs(argv) {
  const selected = [];
  let skipVersionBump = false;
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--flavor") {
      selected.push(argv[++index]);
    } else if (arg === "--skip-version-bump") {
      skipVersionBump = true;
    } else if (arg === "--help" || arg === "-h") {
      console.log("Usage: node scripts/build-release-aab.mjs [--flavor hud|play|third-party] [--skip-version-bump]");
      process.exit(0);
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }
  const variants =
    selected.length === 0
      ? releaseVariants
      : selected.map((name) => {
          const variant = releaseVariants.find((candidate) => candidate.flavorName === name);
          if (!variant) throw new Error(`Unknown flavor: ${name}`);
          return variant;
        });
  return { variants, skipVersionBump };
}

function formatVersionName(date) {
  return `${date.getFullYear()}.${date.getMonth() + 1}.${date.getDate()}`;
}

function formatVersionCodePrefix(date) {
  const year = String(date.getFullYear()).slice(-2);
  return `${year}${String(date.getMonth() + 1).padStart(2, "0")}${String(date.getDate()).padStart(2, "0")}`;
}

function parseVersionMatches(buildGradleText) {
  const versionCodeMatch = buildGradleText.match(/versionCode = (\d+)/);
  const versionNameMatch = buildGradleText.match(/versionName = "([^"]+)"/);
  if (!versionCodeMatch || !versionNameMatch) {
    throw new Error(`Couldn't parse versionName/versionCode from ${buildGradlePath}`);
  }
  return { versionCodeMatch, versionNameMatch };
}

function resolveNextVersionCode(currentVersionCode, todayPrefix) {
  const currentRaw = currentVersionCode.toString();
  let nextSuffix = 0;

  if (currentRaw.startsWith(todayPrefix)) {
    const suffixRaw = currentRaw.slice(todayPrefix.length);
    nextSuffix = (suffixRaw ? Number.parseInt(suffixRaw, 10) : 0) + 1;
  }

  if (!Number.isInteger(nextSuffix) || nextSuffix < 0 || nextSuffix > 99) {
    throw new Error(`Can't auto-bump Android versionCode for ${todayPrefix}: next suffix ${nextSuffix} is invalid`);
  }

  return Number.parseInt(`${todayPrefix}${nextSuffix.toString().padStart(2, "0")}`, 10);
}

function resolveNextVersion(buildGradleText, date) {
  const { versionCodeMatch } = parseVersionMatches(buildGradleText);
  const currentVersionCode = Number.parseInt(versionCodeMatch[1] ?? "", 10);
  if (!Number.isInteger(currentVersionCode)) {
    throw new Error(`Invalid Android versionCode in ${buildGradlePath}`);
  }

  return {
    versionName: formatVersionName(date),
    versionCode: resolveNextVersionCode(currentVersionCode, formatVersionCodePrefix(date)),
  };
}

function updateBuildGradleVersions(buildGradleText, nextVersion) {
  return buildGradleText
    .replace(/versionCode = \d+/, `versionCode = ${nextVersion.versionCode}`)
    .replace(/versionName = "[^"]+"/, `versionName = "${nextVersion.versionName}"`);
}

function run(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio: "inherit", ...options });
    child.on("error", reject);
    child.on("exit", (code) => {
      if (code === 0) resolve();
      else reject(new Error(`${command} ${args.join(" ")} exited with ${code}`));
    });
  });
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
    if (await executableExists(candidate)) return candidate;
  }
  return command;
}

async function sha256Hex(path) {
  return createHash("sha256").update(await readFile(path)).digest("hex");
}

async function copyBundle(sourcePath, destinationPath) {
  await copyFile(sourcePath, destinationPath);
}

async function main() {
  const { variants, skipVersionBump } = parseArgs(process.argv.slice(2));
  const jarsigner = await resolveExecutable("jarsigner", [
    join(process.env.JAVA_HOME ?? "", "bin", "jarsigner"),
    join(process.env.HOME ?? "", ".gradle", "jdks", "eclipse_adoptium-21-amd64-linux.2", "bin", "jarsigner"),
  ]);
  const originalText = await readFile(buildGradlePath, "utf8");
  const currentVersion = parseVersionMatches(originalText).versionNameMatch[1];
  let nextVersion = { versionName: currentVersion, versionCode: Number.parseInt(parseVersionMatches(originalText).versionCodeMatch[1], 10) };
  let updatedText = originalText;

  if (!skipVersionBump) {
    nextVersion = resolveNextVersion(originalText, new Date());
    updatedText = updateBuildGradleVersions(originalText, nextVersion);
    if (updatedText === originalText) throw new Error("Android version bump produced no change");
    await writeFile(buildGradlePath, updatedText);
  }

  console.log(`Android versionName: ${nextVersion.versionName}`);
  console.log(`Android versionCode: ${nextVersion.versionCode}`);
  console.log(`Release flavors: ${variants.map((variant) => variant.flavorName).join(", ")}`);

  await mkdir(releaseOutputDir, { recursive: true });

  try {
    await run("./gradlew", variants.map((variant) => variant.gradleTask), { cwd: androidDir });
  } catch (error) {
    if (!skipVersionBump) await writeFile(buildGradlePath, originalText);
    throw error;
  }

  for (const variant of variants) {
    const outputPath = join(releaseOutputDir, `openclaw-${nextVersion.versionName}-${variant.flavorName}-release.aab`);
    await copyBundle(variant.bundlePath, outputPath);
    await run(jarsigner, ["-verify", outputPath]);
    console.log(`Signed AAB (${variant.flavorName}): ${outputPath}`);
    console.log(`SHA-256 (${variant.flavorName}): ${await sha256Hex(outputPath)}`);
  }
}

await main();
