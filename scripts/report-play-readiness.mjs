#!/usr/bin/env node

import { spawnSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const allowedAccounts = ["earlvanze@gmail.com", "earl@earlbnb.com"];

function parseArgs(argv) {
  const args = {
    format: "markdown",
    strict: false,
    skipSignature: false,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--json") args.format = "json";
    else if (arg === "--markdown") args.format = "markdown";
    else if (arg === "--strict") args.strict = true;
    else if (arg === "--skip-signature") args.skipSignature = true;
    else if (arg === "--help" || arg === "-h") {
      console.log(
        [
          "Usage: node scripts/report-play-readiness.mjs [--json] [--strict] [--skip-signature]",
          "",
          "Runs the local HUD release, Play submission, publish dry-run, final-readiness,",
          "and allowed OAuth account checks, then prints a publish-readiness report.",
          "",
          "Default output is Markdown. Use --json for automation.",
          "Use --strict to exit nonzero unless all publish requirements are ready.",
          "Use --skip-signature for CI checks against unsigned Gradle release bundles.",
        ].join("\n"),
      );
      process.exit(0);
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }

  return args;
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: androidDir,
    encoding: "utf8",
    env: process.env,
    stdio: ["ignore", "pipe", "pipe"],
    ...options,
  });
  const stdout = result.stdout.trim();
  const stderr = result.stderr.trim();
  return {
    ok: result.status === 0,
    status: result.status,
    stdout,
    stderr,
    detail: [stdout, stderr].filter(Boolean).join("\n"),
  };
}

function runNodeScript(script, args = []) {
  return run(process.execPath, [join("scripts", script), ...args]);
}

function summarizeDetail(detail, maxLines = 8) {
  const lines = detail.split(/\r?\n/u).map((line) => line.trim()).filter(Boolean);
  return lines.slice(0, maxLines);
}

function resultSummary(result) {
  const detail = result.ok
    ? result.detail
    : [result.stderr, result.stdout].filter(Boolean).join("\n");
  return summarizeDetail(detail);
}

function gcloudAccounts() {
  const result = run("gcloud", ["auth", "list", "--format=json"]);
  if (!result.ok) {
    return {
      ok: false,
      accounts: [],
      detail: result.detail || "gcloud auth list failed",
    };
  }

  try {
    const entries = JSON.parse(result.stdout);
    const accounts =
      Array.isArray(entries)
        ? entries
            .map((entry) => String(entry.account ?? "").trim().toLowerCase())
            .filter(Boolean)
        : [];
    return {
      ok: true,
      accounts: [...new Set(accounts)],
      detail: null,
    };
  } catch (error) {
    return {
      ok: false,
      accounts: [],
      detail: `Unable to parse gcloud auth list output: ${error.message}`,
    };
  }
}

function authCheck(account) {
  return runNodeScript("publish-play-internal.mjs", [
    "--auth",
    "gcloud",
    "--gcloud-account",
    account,
    "--auth-check",
  ]);
}

function statusLabel(ok) {
  return ok ? "ready" : "blocked";
}

function markdownCheckLine(label, check) {
  const marker = check.ok ? "[x]" : "[ ]";
  return `- ${marker} ${label}: ${statusLabel(check.ok)}`;
}

function renderMarkdown(report) {
  const lines = [
    "# OpenClaw HUD Play Readiness",
    "",
    `Generated: ${report.generatedAt}`,
    "",
    "## Summary",
    "",
    `- Publish ready: ${report.publishReady ? "yes" : "no"}`,
    `- Local artifact gates ready: ${report.localArtifactReady ? "yes" : "no"}`,
    `- Local dry-run ready: ${report.localDryRunReady ? "yes" : "no"}`,
    `- Final Play Console fields ready: ${report.finalSubmissionReady ? "yes" : "no"}`,
    `- Allowed OAuth ready: ${report.oauthReady ? "yes" : "no"}`,
    "",
    "## Local Artifact Gates",
    "",
    markdownCheckLine("HUD release verifier", report.checks.hudRelease),
    markdownCheckLine("Play submission draft verifier", report.checks.submissionDraft),
    "",
    "## Local Publish Dry Run",
    "",
    markdownCheckLine("Publish dry run", report.checks.publishDryRun),
    "",
    "## External Publish Gates",
    "",
    markdownCheckLine("Play submission final verifier", report.checks.submissionFinal),
    "",
    "## OAuth",
    "",
    `Authenticated gcloud accounts: ${report.gcloud.accounts.length ? report.gcloud.accounts.join(", ") : "(none)"}`,
    "",
    ...report.oauth.map((entry) => markdownCheckLine(entry.account, entry)),
  ];

  if (report.blockers.length > 0) {
    lines.push("", "## Blockers", "");
    for (const blocker of report.blockers) lines.push(`- ${blocker}`);
  }

  return `${lines.join("\n")}\n`;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const hudReleaseArgs = args.skipSignature ? ["--skip-signature"] : [];
  const checks = {
    hudRelease: runNodeScript("verify-play-hud-release.mjs", hudReleaseArgs),
    submissionDraft: runNodeScript("verify-play-submission-package.mjs"),
    publishDryRun: runNodeScript("publish-play-internal.mjs", ["--dry-run"]),
    submissionFinal: runNodeScript("verify-play-submission-package.mjs", ["--final"]),
  };
  const gcloud = gcloudAccounts();
  const oauth = allowedAccounts.map((account) => ({
    account,
    ...authCheck(account),
  }));

  const localArtifactReady = checks.hudRelease.ok && checks.submissionDraft.ok;
  const localDryRunReady = checks.publishDryRun.ok;
  const localReleaseReady = localArtifactReady && localDryRunReady;
  const finalSubmissionReady = checks.submissionFinal.ok;
  const oauthReady = oauth.some((entry) => entry.ok);
  const publishReady = localReleaseReady && finalSubmissionReady && oauthReady;

  const blockers = [];
  const localBlockers = [];
  const externalBlockers = [];
  if (!checks.hudRelease.ok) {
    blockers.push("HUD release verifier is failing.");
    localBlockers.push("HUD release verifier is failing.");
  }
  if (!checks.submissionDraft.ok) {
    blockers.push("Draft Play submission verifier is failing.");
    localBlockers.push("Draft Play submission verifier is failing.");
  }
  if (!checks.publishDryRun.ok) {
    blockers.push("Publish dry run is failing before Play API upload.");
    localBlockers.push("Publish dry run is failing before Play API upload.");
  }
  if (!checks.submissionFinal.ok) {
    const blocker = "Final Play submission verifier is failing; complete Play Console external fields and evidence.";
    blockers.push(blocker);
    externalBlockers.push(blocker);
  }
  if (!oauthReady) {
    const blocker = `Authenticate one allowed publisher account with gcloud: ${allowedAccounts.join(" or ")}.`;
    blockers.push(blocker);
    externalBlockers.push(blocker);
  }

  const report = {
    generatedAt: new Date().toISOString(),
    packageName: "ai.openclaw.app.hud",
    allowedAccounts,
    publishReady,
    localArtifactReady,
    localDryRunReady,
    localReleaseReady,
    finalSubmissionReady,
    oauthReady,
    gcloud,
    checks: Object.fromEntries(
      Object.entries(checks).map(([key, value]) => [
        key,
        {
          ok: value.ok,
          status: value.status,
          summary: resultSummary(value),
        },
      ]),
    ),
    oauth: oauth.map((entry) => ({
      account: entry.account,
      ok: entry.ok,
      status: entry.status,
      summary: resultSummary(entry),
    })),
    blockers,
    localBlockers,
    externalBlockers,
  };

  if (args.format === "json") {
    console.log(JSON.stringify(report, null, 2));
  } else {
    process.stdout.write(renderMarkdown(report));
  }

  if (args.strict && !publishReady) process.exit(1);
}

await main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
