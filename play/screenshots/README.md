# Play Screenshots

Use `scripts/capture-play-screenshots.sh` to generate deterministic phone
screenshots for Google Play review/demo mode. Generated PNG files are normalized
to 24-bit RGB without alpha. `scripts/render-play-screenshots.mjs` renders the
tracked fallback artifacts when the phone/M1 capture path is unavailable. Both
scripts write `manifest.json` with the generated phone screenshot paths.

For Samsung foldables, pass `--display-id <physical-display-id>` when Android
reports multiple displays. The final Play verifier rejects local PNG screenshots
that are blank/off-display, too tall for Play's phone screenshot aspect-ratio
limit, or missing the OpenClaw green HUD accent.

After uploading the screenshots in Play Console, record their local paths, names,
or URLs in `play/app-content-answers.json` under
`finalSubmission.phoneScreenshots`. Local paths are validated by
`node scripts/verify-play-submission-package.mjs --final`.
