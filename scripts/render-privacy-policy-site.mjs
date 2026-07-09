#!/usr/bin/env node

import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const scriptDir = dirname(fileURLToPath(import.meta.url));
const androidDir = join(scriptDir, "..");
const defaultSourcePath = join(androidDir, "play", "privacy-policy.md");
const defaultOutDir = join(androidDir, "docs");

function parseArgs(argv) {
  const args = {
    source: defaultSourcePath,
    outDir: defaultOutDir,
    check: false,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === "--source") args.source = resolve(argv[++index]);
    else if (arg === "--out-dir") args.outDir = resolve(argv[++index]);
    else if (arg === "--check") args.check = true;
    else if (arg === "--help" || arg === "-h") {
      console.log(
        [
          "Usage: node scripts/render-privacy-policy-site.mjs [--check]",
          "",
          "Renders play/privacy-policy.md into docs/privacy-policy.html for the",
          "hosted Google Play privacy-policy URL. Use --check in CI to verify the",
          "checked-in HTML is in sync with the Markdown source.",
        ].join("\n"),
      );
      process.exit(0);
    } else {
      throw new Error(`Unknown argument: ${arg}`);
    }
  }

  return args;
}

function escapeHtml(value) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function inlineMarkdown(value) {
  const escaped = escapeHtml(value);
  return escaped.replace(/`([^`]+)`/gu, "<code>$1</code>");
}

function markdownToHtml(markdown) {
  const blocks = markdown.trim().split(/\n{2,}/u);
  return blocks
    .map((block) => {
      const trimmed = block.trim();
      if (trimmed.startsWith("# ")) return `<h1>${inlineMarkdown(trimmed.slice(2).trim())}</h1>`;
      return `<p>${inlineMarkdown(trimmed.replace(/\n+/gu, " "))}</p>`;
    })
    .join("\n");
}

function renderPage(markdown) {
  const body = markdownToHtml(markdown);
  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="robots" content="index,follow">
  <title>OpenClaw HUD Privacy Policy</title>
  <style>
    :root { color-scheme: light dark; }
    body {
      margin: 0;
      font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      line-height: 1.6;
      background: Canvas;
      color: CanvasText;
    }
    main {
      width: min(72ch, calc(100% - 32px));
      margin: 0 auto;
      padding: 48px 0;
    }
    h1 {
      line-height: 1.15;
      margin: 0 0 24px;
    }
    p {
      margin: 0 0 18px;
    }
    code {
      font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
      font-size: 0.95em;
    }
  </style>
</head>
<body>
  <main>
${body
  .split("\n")
  .map((line) => `    ${line}`)
  .join("\n")}
  </main>
</body>
</html>
`;
}

async function writeIfChanged(path, content) {
  const existing = await readFile(path, "utf8").catch(() => null);
  if (existing === content) return false;
  await writeFile(path, content);
  return true;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const markdown = await readFile(args.source, "utf8");
  const html = renderPage(markdown);
  const outputPath = join(args.outDir, "privacy-policy.html");

  if (args.check) {
    const existing = await readFile(outputPath, "utf8").catch(() => null);
    if (existing !== html) {
      throw new Error(`Generated privacy policy is stale. Run: node scripts/render-privacy-policy-site.mjs`);
    }
    console.log(`Privacy policy site is current: ${outputPath}`);
    return;
  }

  await mkdir(args.outDir, { recursive: true });
  const changed = await writeIfChanged(outputPath, html);
  console.log(`${changed ? "Rendered" : "Current"}: ${outputPath}`);
}

await main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
