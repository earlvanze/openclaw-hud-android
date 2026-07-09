#!/usr/bin/env node

import { createSign } from "node:crypto";
import { spawnSync } from "node:child_process";
import { createReadStream } from "node:fs";
import { readdir, readFile, stat } from "node:fs/promises";
import { basename, dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const releaseOutputDir = join(androidDir, "build", "release-bundles");
const defaultListingDir = join(androidDir, "play", "listings");
const androidPublisherScope = "https://www.googleapis.com/auth/androidpublisher";
const defaultAllowedGcloudAccounts = ["earlvanze@gmail.com", "earl@earlbnb.com"];

function parseAccountList(value) {
  return value
    .split(",")
    .map((account) => account.trim().toLowerCase())
    .filter(Boolean);
}

function parseArgs(argv) {
  const args = {
    packageName: "ai.openclaw.app.hud",
    track: "internal",
    status: "draft",
    language: "en-US",
    listingDir: defaultListingDir,
    uploadListing: true,
    uploadReleaseNotes: true,
    commit: false,
    preflight: false,
    bundle: null,
    serviceAccount: process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON || process.env.GOOGLE_APPLICATION_CREDENTIALS || null,
    auth: process.env.GOOGLE_PLAY_AUTH || "auto",
    allowedAccounts:
      process.env.GOOGLE_PLAY_ALLOWED_ACCOUNTS !== undefined
        ? parseAccountList(process.env.GOOGLE_PLAY_ALLOWED_ACCOUNTS)
        : [...defaultAllowedGcloudAccounts],
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--package") args.packageName = argv[++index];
    else if (arg === "--track") args.track = argv[++index];
    else if (arg === "--status") args.status = argv[++index];
    else if (arg === "--language") args.language = argv[++index];
    else if (arg === "--listing-dir") args.listingDir = argv[++index];
    else if (arg === "--bundle") args.bundle = argv[++index];
    else if (arg === "--service-account") args.serviceAccount = argv[++index];
    else if (arg === "--auth") args.auth = argv[++index];
    else if (arg === "--allowed-account") args.allowedAccounts.push(...parseAccountList(argv[++index]));
    else if (arg === "--skip-listing") args.uploadListing = false;
    else if (arg === "--skip-release-notes") args.uploadReleaseNotes = false;
    else if (arg === "--commit") args.commit = true;
    else if (arg === "--preflight") args.preflight = true;
    else if (arg === "--dry-run") args.commit = false;
    else if (arg === "--help" || arg === "-h") {
      console.log(
        [
          "Usage: node scripts/publish-play-internal.mjs [--commit] [--bundle path] [--package ai.openclaw.app.hud]",
          "",
          "Defaults to a dry-run for the latest HUD AAB in build/release-bundles.",
          "Auth modes:",
          "  --auth auto             Use service-account JSON when configured, otherwise gcloud.",
          "  --auth service-account  Require GOOGLE_PLAY_SERVICE_ACCOUNT_JSON, GOOGLE_APPLICATION_CREDENTIALS, or --service-account.",
          "  --auth gcloud           Use `gcloud auth print-access-token` from the active account.",
          "  --allowed-account email Restrict gcloud OAuth publishing to an expected account. Repeat or comma-separate.",
          "",
          `Default allowed gcloud OAuth accounts: ${defaultAllowedGcloudAccounts.join(", ")}`,
          "",
          "Listing files default to play/listings/en-US/{title,short-description,full-description,release-notes}.txt.",
          "Use --skip-listing or --skip-release-notes to upload only the bundle/track changes.",
          "",
          "Use --preflight to verify Play API package/access state without uploading a bundle.",
        ].join("\n"),
      );
      process.exit(0);
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }

  return args;
}

function base64Url(input) {
  return Buffer.from(input).toString("base64").replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/u, "");
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
  candidates.sort((a, b) => b.mtimeMs - a.mtimeMs);
  return candidates[0]?.path ?? null;
}

function charCount(value) {
  return Array.from(value).length;
}

function trimTrailingNewline(value) {
  return value.replace(/\r\n/gu, "\n").replace(/\n+$/u, "");
}

async function readTextIfEnabled(path, enabled) {
  if (!enabled) return null;
  return trimTrailingNewline(await readFile(path, "utf8"));
}

function requireLength(label, value, maxLength) {
  const length = charCount(value);
  if (length > maxLength) {
    throw new Error(`${label} is ${length} characters; Google Play limit is ${maxLength}.`);
  }
  return length;
}

async function loadPlayListing(args) {
  const languageDir = join(args.listingDir, args.language);
  const listing =
    args.uploadListing
      ? {
          title: await readTextIfEnabled(join(languageDir, "title.txt"), true),
          shortDescription: await readTextIfEnabled(join(languageDir, "short-description.txt"), true),
          fullDescription: await readTextIfEnabled(join(languageDir, "full-description.txt"), true),
        }
      : null;
  const releaseNotes = await readTextIfEnabled(join(languageDir, "release-notes.txt"), args.uploadReleaseNotes);

  const lengths = {};
  if (listing) {
    lengths.title = requireLength("Listing title", listing.title, 30);
    lengths.shortDescription = requireLength("Short description", listing.shortDescription, 80);
    lengths.fullDescription = requireLength("Full description", listing.fullDescription, 4000);
  }
  if (releaseNotes !== null) {
    lengths.releaseNotes = requireLength("Release notes", releaseNotes, 500);
  }
  return { listing, releaseNotes, lengths };
}

async function loadServiceAccount(path) {
  if (!path) throw new Error("Missing service account. Set GOOGLE_PLAY_SERVICE_ACCOUNT_JSON or GOOGLE_APPLICATION_CREDENTIALS.");
  const account = JSON.parse(await readFile(path, "utf8"));
  for (const key of ["client_email", "private_key"]) {
    if (!account[key]) throw new Error(`Service account JSON missing ${key}`);
  }
  return account;
}

async function accessToken(serviceAccount) {
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const payload = base64Url(
    JSON.stringify({
      iss: serviceAccount.client_email,
      scope: androidPublisherScope,
      aud: "https://oauth2.googleapis.com/token",
      iat: now,
      exp: now + 3600,
    }),
  );
  const signingInput = `${header}.${payload}`;
  const signature = createSign("RSA-SHA256").update(signingInput).sign(serviceAccount.private_key);
  const assertion = `${signingInput}.${base64Url(signature)}`;

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) throw new Error(`OAuth token exchange failed: ${response.status} ${await response.text()}`);
  const body = await response.json();
  return body.access_token;
}

function gcloudAccessToken() {
  const result = spawnSync("gcloud", ["auth", "print-access-token", `--scopes=${androidPublisherScope}`], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
  if (result.status !== 0) {
    const detail = result.stderr.trim() || result.stdout.trim() || "gcloud auth print-access-token failed";
    throw new Error(`Unable to get gcloud access token: ${detail}`);
  }
  const token = result.stdout.trim();
  if (!token) throw new Error("gcloud auth print-access-token returned an empty token");
  return token;
}

function gcloudActiveAccount() {
  const result = spawnSync("gcloud", ["config", "get-value", "account"], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  });
  if (result.status !== 0) {
    const detail = result.stderr.trim() || result.stdout.trim() || "gcloud config get-value account failed";
    throw new Error(`Unable to get active gcloud account: ${detail}`);
  }
  const account = result.stdout.trim().toLowerCase();
  if (!account || account === "(unset)") throw new Error("No active gcloud account is configured.");
  return account;
}

function verifyAllowedGcloudAccount(activeAccount, allowedAccounts) {
  const allowed = new Set(allowedAccounts.map((account) => account.trim().toLowerCase()).filter(Boolean));
  if (allowed.size === 0) return;
  if (allowed.has(activeAccount)) return;
  throw new Error(
    [
      `Active gcloud account is ${activeAccount}, but Play publishing is restricted to: ${[...allowed].join(", ")}.`,
      "Run `gcloud auth login earlvanze@gmail.com` or `gcloud auth login earl@earlbnb.com`, then `gcloud config set account <email>`.",
      "For a different authorized publisher, set GOOGLE_PLAY_ALLOWED_ACCOUNTS or pass --allowed-account.",
    ].join("\n"),
  );
}

async function resolveAccessToken(args) {
  if (args.auth === "service-account" || (args.auth === "auto" && args.serviceAccount)) {
    const serviceAccount = await loadServiceAccount(args.serviceAccount);
    return { token: await accessToken(serviceAccount), source: "service-account" };
  }
  if (args.auth === "gcloud" || args.auth === "auto") {
    const account = gcloudActiveAccount();
    verifyAllowedGcloudAccount(account, args.allowedAccounts);
    return { token: gcloudAccessToken(), source: `gcloud:${account}` };
  }
  throw new Error(`Unknown auth mode: ${args.auth}`);
}

async function googleJson(token, url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      authorization: `Bearer ${token}`,
      "content-type": "application/json",
      ...(options.headers ?? {}),
    },
  });
  if (!response.ok) {
    const text = await response.text();
    let message = `${options.method ?? "GET"} ${url} failed: ${response.status} ${text}`;
    if (response.status === 404 && text.includes("Package not found")) {
      message += [
        "",
        "Play Console setup required:",
        "  1. Create the app in Play Console with package ai.openclaw.app.hud.",
        "  2. Link/grant this Google Cloud service account access in Play Console.",
        "  3. Complete required App content, Data safety, and store listing forms.",
        "  4. Re-run this command.",
      ].join("\n");
    }
    throw new Error(message);
  }
  return response.json();
}

async function uploadBundle(token, packageName, editId, bundlePath) {
  const response = await fetch(
    `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/edits/${encodeURIComponent(editId)}/bundles?uploadType=media`,
    {
      method: "POST",
      headers: {
        authorization: `Bearer ${token}`,
        "content-type": "application/octet-stream",
      },
      body: createReadStream(bundlePath),
      duplex: "half",
    },
  );
  if (!response.ok) throw new Error(`Bundle upload failed: ${response.status} ${await response.text()}`);
  return response.json();
}

async function patchListing(token, baseUrl, editId, language, listing) {
  return googleJson(
    token,
    `${baseUrl}/edits/${encodeURIComponent(editId)}/listings/${encodeURIComponent(language)}`,
    {
      method: "PATCH",
      body: JSON.stringify(listing),
    },
  );
}

async function deleteEdit(token, baseUrl, editId) {
  await googleJson(token, `${baseUrl}/edits/${encodeURIComponent(editId)}:delete`, { method: "DELETE" }).catch(() => {});
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const bundlePath = args.bundle ?? (await latestHudBundle());
  if (!bundlePath) throw new Error(`No HUD release bundle found in ${releaseOutputDir}`);
  const bundleInfo = await stat(bundlePath);
  const playListing = await loadPlayListing(args);

  console.log(`Package: ${args.packageName}`);
  console.log(`Track: ${args.track}`);
  console.log(`Release status: ${args.status}`);
  console.log(`Language: ${args.language}`);
  console.log(`Bundle: ${bundlePath} (${bundleInfo.size} bytes)`);
  if (playListing.listing) {
    console.log(
      `Listing: title ${playListing.lengths.title}/30, short ${playListing.lengths.shortDescription}/80, full ${playListing.lengths.fullDescription}/4000`,
    );
  } else {
    console.log("Listing: skipped");
  }
  if (playListing.releaseNotes !== null) {
    console.log(`Release notes: ${playListing.lengths.releaseNotes}/500`);
  } else {
    console.log("Release notes: skipped");
  }
  console.log(`Mode: ${args.preflight ? "preflight" : args.commit ? "commit" : "dry-run"}`);

  if (!args.commit && !args.preflight) {
    console.log("Dry-run complete. Re-run with --preflight to verify Play access, or --commit to upload and commit a Google Play edit.");
    return;
  }

  const { token, source } = await resolveAccessToken(args);
  console.log(`Auth: ${source}`);
  const baseUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(args.packageName)}`;
  const edit = await googleJson(token, `${baseUrl}/edits`, { method: "POST", body: "{}" });
  console.log(`Created edit: ${edit.id}`);

  if (args.preflight) {
    await deleteEdit(token, baseUrl, edit.id);
    console.log("Preflight complete. Play package and auth are valid; no bundle was uploaded.");
    return;
  }

  try {
    const bundle = await uploadBundle(token, args.packageName, edit.id, bundlePath);
    const versionCode = bundle.versionCode;
    if (!versionCode) throw new Error(`Bundle upload did not return versionCode for ${basename(bundlePath)}`);
    console.log(`Uploaded bundle versionCode: ${versionCode}`);

    if (playListing.listing) {
      await patchListing(token, baseUrl, edit.id, args.language, playListing.listing);
      console.log(`Patched ${args.language} store listing`);
    }

    const release = {
      name: basename(bundlePath).replace(/\.aab$/u, ""),
      versionCodes: [String(versionCode)],
      status: args.status,
    };
    if (playListing.releaseNotes !== null) {
      release.releaseNotes = [
        {
          language: args.language,
          text: playListing.releaseNotes,
        },
      ];
    }

    await googleJson(token, `${baseUrl}/edits/${encodeURIComponent(edit.id)}/tracks/${encodeURIComponent(args.track)}`, {
      method: "PUT",
      body: JSON.stringify({
        track: args.track,
        releases: [release],
      }),
    });
    await googleJson(token, `${baseUrl}/edits/${encodeURIComponent(edit.id)}:commit`, { method: "POST", body: "{}" });
    console.log(`Committed Google Play edit ${edit.id}`);
  } catch (error) {
    await deleteEdit(token, baseUrl, edit.id);
    throw error;
  }
}

await main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
