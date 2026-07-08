#!/usr/bin/env node

import { createSign } from "node:crypto";
import { createReadStream } from "node:fs";
import { readdir, readFile, stat } from "node:fs/promises";
import { basename, dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const releaseOutputDir = join(androidDir, "build", "release-bundles");
const androidPublisherScope = "https://www.googleapis.com/auth/androidpublisher";

function parseArgs(argv) {
  const args = {
    packageName: "ai.openclaw.app.hud",
    track: "internal",
    status: "draft",
    commit: false,
    bundle: null,
    serviceAccount: process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON || process.env.GOOGLE_APPLICATION_CREDENTIALS || null,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--package") args.packageName = argv[++index];
    else if (arg === "--track") args.track = argv[++index];
    else if (arg === "--status") args.status = argv[++index];
    else if (arg === "--bundle") args.bundle = argv[++index];
    else if (arg === "--service-account") args.serviceAccount = argv[++index];
    else if (arg === "--commit") args.commit = true;
    else if (arg === "--dry-run") args.commit = false;
    else if (arg === "--help" || arg === "-h") {
      console.log(
        [
          "Usage: node scripts/publish-play-internal.mjs [--commit] [--bundle path] [--package ai.openclaw.app.hud]",
          "",
          "Defaults to a dry-run for the latest HUD AAB in build/release-bundles.",
          "Set GOOGLE_PLAY_SERVICE_ACCOUNT_JSON or GOOGLE_APPLICATION_CREDENTIALS to a Play Console service-account JSON file.",
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

async function googleJson(token, url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: {
      authorization: `Bearer ${token}`,
      "content-type": "application/json",
      ...(options.headers ?? {}),
    },
  });
  if (!response.ok) throw new Error(`${options.method ?? "GET"} ${url} failed: ${response.status} ${await response.text()}`);
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

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const bundlePath = args.bundle ?? (await latestHudBundle());
  if (!bundlePath) throw new Error(`No HUD release bundle found in ${releaseOutputDir}`);
  const bundleInfo = await stat(bundlePath);

  console.log(`Package: ${args.packageName}`);
  console.log(`Track: ${args.track}`);
  console.log(`Release status: ${args.status}`);
  console.log(`Bundle: ${bundlePath} (${bundleInfo.size} bytes)`);
  console.log(`Mode: ${args.commit ? "commit" : "dry-run"}`);

  if (!args.commit) {
    console.log("Dry-run complete. Re-run with --commit to create, upload, assign track, and commit a Google Play edit.");
    return;
  }

  const serviceAccount = await loadServiceAccount(args.serviceAccount);
  const token = await accessToken(serviceAccount);
  const baseUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(args.packageName)}`;
  const edit = await googleJson(token, `${baseUrl}/edits`, { method: "POST", body: "{}" });
  console.log(`Created edit: ${edit.id}`);

  try {
    const bundle = await uploadBundle(token, args.packageName, edit.id, bundlePath);
    const versionCode = bundle.versionCode;
    if (!versionCode) throw new Error(`Bundle upload did not return versionCode for ${basename(bundlePath)}`);
    console.log(`Uploaded bundle versionCode: ${versionCode}`);

    await googleJson(token, `${baseUrl}/edits/${encodeURIComponent(edit.id)}/tracks/${encodeURIComponent(args.track)}`, {
      method: "PUT",
      body: JSON.stringify({
        track: args.track,
        releases: [
          {
            name: basename(bundlePath).replace(/\.aab$/u, ""),
            versionCodes: [String(versionCode)],
            status: args.status,
          },
        ],
      }),
    });
    await googleJson(token, `${baseUrl}/edits/${encodeURIComponent(edit.id)}:commit`, { method: "POST", body: "{}" });
    console.log(`Committed Google Play edit ${edit.id}`);
  } catch (error) {
    await googleJson(token, `${baseUrl}/edits/${encodeURIComponent(edit.id)}:delete`, { method: "DELETE" }).catch(() => {});
    throw error;
  }
}

await main();
