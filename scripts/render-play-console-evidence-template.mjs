#!/usr/bin/env node

import { readFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const defaultAppContentPath = join(androidDir, "play", "app-content-answers.json");

const evidenceTemplates = [
  {
    key: "appCreatedInPlayConsole",
    label: "Create app in Play Console",
    source: (packageName) => `Google Play Console > All apps > OpenClaw HUD (${packageName})`,
    notes: (packageName) => `Verified the Play Console app exists for package ${packageName}.`,
  },
  {
    key: "internalTestersConfiguredInPlayConsole",
    label: "Configure internal testers",
    source: () => "Google Play Console > Test and release > Testing > Internal testing > Testers",
    notes: () => "Verified the internal testing track has the intended tester list or group configured.",
  },
  {
    key: "reviewerAccessConfiguredInPlayConsole",
    label: "Configure reviewer app access",
    source: () => "Google Play Console > Policy and programs > App content > App access",
    notes: () => "Verified App access contains Demo Mode review instructions and any temporary live-gateway access details needed by reviewers.",
  },
];

function todayIsoDate() {
  return new Date().toISOString().slice(0, 10);
}

function parseArgs(argv) {
  const args = {
    appContent: defaultAppContentPath,
    verifiedAt: todayIsoDate(),
    onlyCompleted: false,
    jsonOnly: false,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--app-content") args.appContent = resolve(argv[++index]);
    else if (arg === "--verified-at") args.verifiedAt = argv[++index];
    else if (arg === "--only-completed") args.onlyCompleted = true;
    else if (arg === "--json-only") args.jsonOnly = true;
    else if (arg === "--help" || arg === "-h") {
      console.log(
        [
          "Usage: node scripts/render-play-console-evidence-template.mjs [--verified-at YYYY-MM-DD] [--json-only]",
          "",
          "Renders copyable JSON for play/app-content-answers.json",
          "finalSubmission.consoleEvidence. Use --only-completed to render only",
          "the evidence keys whose finalSubmission flags are already true.",
        ].join("\n"),
      );
      process.exit(0);
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }

  if (!/^\d{4}-\d{2}-\d{2}$/u.test(args.verifiedAt)) {
    throw new Error("--verified-at must be a YYYY-MM-DD date.");
  }
  return args;
}

async function readJson(path) {
  return JSON.parse(await readFile(path, "utf8"));
}

function renderEvidence(appContent, verifiedAt, onlyCompleted) {
  const finalSubmission = appContent.finalSubmission ?? {};
  const packageName = appContent.packageName ?? "ai.openclaw.app.hud";
  const selectedTemplates = onlyCompleted
    ? evidenceTemplates.filter((template) => finalSubmission[template.key] === true)
    : evidenceTemplates;

  return Object.fromEntries(
    selectedTemplates.map((template) => [
      template.key,
      {
        source: template.source(packageName),
        verifiedAt,
        notes: template.notes(packageName),
      },
    ]),
  );
}

const args = parseArgs(process.argv.slice(2));
const appContent = await readJson(args.appContent);
const evidence = renderEvidence(appContent, args.verifiedAt, args.onlyCompleted);
const rendered = `${JSON.stringify(evidence, null, 2)}\n`;

if (args.jsonOnly) {
  process.stdout.write(rendered);
} else {
  console.log(
    [
      "Copy this JSON into play/app-content-answers.json under finalSubmission.consoleEvidence after the matching Play Console blockers are complete.",
      "Do not flip the finalSubmission booleans until the matching Console page has actually been verified.",
      "",
      rendered.trimEnd(),
    ].join("\n"),
  );
}
