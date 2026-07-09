# Play Screenshots

Use `scripts/capture-play-screenshots.sh` to generate deterministic phone
screenshots for Google Play review/demo mode. Generated PNG files are normalized
to 24-bit RGB without alpha and ignored by git. The script also writes a local
`manifest.json` with the generated phone screenshot paths.

After uploading the screenshots in Play Console, record their local paths, names,
or URLs in `play/app-content-answers.json` under
`finalSubmission.phoneScreenshots`. Local paths are validated by
`node scripts/verify-play-submission-package.mjs --final`.
