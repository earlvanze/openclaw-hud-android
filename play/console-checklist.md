# Google Play Console Checklist

Package: `ai.openclaw.app.hud`

Use this checklist before running `node scripts/publish-play-internal.mjs --commit`.

- Create the app in Google Play Console with package name `ai.openclaw.app.hud`.
- Enable Google Play Android Developer API for the linked Google Cloud project.
- Grant the publishing service account access in Play Console.
- For OAuth publishing, pass
  `--auth gcloud --gcloud-account earlvanze@gmail.com` or
  `--auth gcloud --gcloud-account earl@earlbnb.com` before preflight/commit.
  `GOOGLE_PLAY_GCLOUD_ACCOUNT` and `gcloud config set account <email>` are also
  supported. The publish helper rejects other gcloud accounts by default.
- Complete App content forms, including Data safety, Ads, App access, Content rating, Target audience, and Data deletion.
- Use the copy in `play/listings/en-US/` for the initial English listing.
- Use `play/privacy-policy.md` as the hosted privacy-policy source; the same
  disclosure is available in-app at Settings -> AirVision M1 -> App Preferences
  -> Privacy Policy.
- Run `node scripts/render-privacy-policy-site.mjs --check` to verify
  `docs/privacy-policy.html` matches `play/privacy-policy.md`.
- Configure the GitHub repository's Pages source to GitHub Actions and verify
  the Pages workflow has deployed `docs/privacy-policy.html`.
- After the page is reachable, enter
  `https://earlvanze.github.io/openclaw-hud-android/privacy-policy.html` in
  Play Console and set `play/app-content-answers.json`
  `finalSubmission.hostedPrivacyPolicyUrl` to that URL.
- Run `node scripts/verify-play-hud-release.mjs` before every Play upload.
  This also confirms App Bundle language splits remain disabled for the
  AirVision companion language menu.
- Run `node scripts/verify-play-submission-package.mjs` before filling or
  updating Play Console App content answers.
- Upload the fresh HUD AAB selected by the publish helper to the internal track
  first. Rebuild with `./gradlew :app:bundleHudRelease` for local dry-runs, or
  `node scripts/build-release-aab.mjs --flavor hud` for a locally signed release
  bundle.
- Capture phone screenshots with `scripts/capture-play-screenshots.sh`, or run
  `node scripts/render-play-screenshots.mjs` when the Fold/M1 capture path is
  offline. Both paths write Play-ready 24-bit PNG screenshots and
  `play/screenshots/phone/manifest.json`.
- Use `play/console-handoff.md` as the generated copy/paste packet for Play
  Console listing, App content, screenshots, app access, and remaining external
  readiness flags. Keep it current with
  `node scripts/render-play-console-handoff.mjs --check`.
- Run `node scripts/test-play-screenshot-tools.mjs` after changes to screenshot
  capture, conversion, or final submission validation.
- Keep the initial release status as `draft` until screenshots, policy forms, and tester access are verified.
- Fill `play/app-content-answers.json` `finalSubmission` fields, including
  `finalSubmission.consoleEvidence` source/date/notes for each completed Play
  Console blocker, then run
  `node scripts/verify-play-submission-package.mjs --final` before the first
  `--commit`. The publish helper runs this final verifier again in `--commit`
  mode before creating a Play edit or uploading a bundle.

Current local status:

- Latest signed HUD AAB builds successfully from `main`:
  `build/release-bundles/openclaw-2026.7.10-hud-release.aab`
- Latest HUD AAB SHA-256:
  `eb08bb535ce8951065dbc9ae764228f855b40535330dfa310e58d3a930ac76a3`
- `node scripts/verify-play-hud-release.mjs` passes against the latest signed
  HUD AAB, packaged HUD manifest, and English Play listing copy.
- `lintHudRelease` passes.
- `testHudDebugUnitTest` passes.
- `node scripts/publish-play-internal.mjs --dry-run` validates the local AAB,
  English listing copy, release notes character limits, and local Play
  submission packet. It refuses stale local artifacts when HUD source/build
  inputs are dirty or newer than the selected AAB.
- OAuth publishing is restricted to `earlvanze@gmail.com` or
  `earl@earlbnb.com`; `--auth-check` verifies the selected account before any
  Play API request once that account is authenticated locally.
- Current Cyber gcloud auth state still needs OAuth login for both allowed
  publisher accounts. `--auth-check` currently fails early for
  `earlvanze@gmail.com` and `earl@earlbnb.com` because only the existing service
  account is authenticated locally.
- `node scripts/publish-play-internal.mjs --commit` is guarded by
  `node scripts/verify-play-submission-package.mjs --final` before any Play API
  upload.
- `node scripts/test-play-screenshot-tools.mjs` passes against the current
  screenshot converter and final submission verifier.
- `node scripts/test-play-publish-helper.mjs` passes against fake gcloud account
  scenarios for OAuth account selection and auth-check mode.
- `node scripts/test-play-submission-verifier.mjs` passes against stale generated
  Play Console handoff detection and generated Console evidence acceptance in
  the local submission verifier.
- `node scripts/render-play-console-evidence-template.mjs --verified-at
  YYYY-MM-DD --json-only` renders copyable
  `finalSubmission.consoleEvidence` JSON after external Console blockers are
  actually complete.
- `node scripts/render-play-console-handoff.mjs --check` verifies the generated
  Play Console handoff packet against the current listing copy, app-content
  answers, privacy URL, and screenshot manifest.
- `node scripts/verify-airvision-firmware-capture-results.mjs` and
  `node scripts/test-airvision-firmware-capture-results.mjs` pass against the
  structured AirVision firmware capture-results gate. Android firmware writes
  remain blocked until sanitized Windows ASUS HID evidence is validated.
- `play/app-content-answers.json` contains the draft App content answers for
  Privacy policy, Ads, App access, Target audience, Content rating, Data
  deletion, and Data safety. Keep it aligned with the final Play Console forms.
- `play/app-content-answers.json` also tracks final external readiness fields
  for hosted privacy URL, app creation, internal testers, reviewer access, and
  phone screenshots. Each completed Play Console blocker also needs
  source/date/notes evidence under `finalSubmission.consoleEvidence`. These are
  intentionally incomplete until Play Console is configured. Generate the
  evidence shape with `node scripts/render-play-console-evidence-template.mjs
  --verified-at YYYY-MM-DD --json-only` after verifying the matching Console
  page. Local screenshot paths are validated by the final submission verifier
  before commit publishing.
- `docs/privacy-policy.html` is the generated GitHub Pages privacy-policy page.
  It is published at
  `https://earlvanze.github.io/openclaw-hud-android/privacy-policy.html` and
  recorded in `finalSubmission.hostedPrivacyPolicyUrl`. Keep it current with
  `node scripts/render-privacy-policy-site.mjs --check`.
- Final submission verification fetches `finalSubmission.hostedPrivacyPolicyUrl`
  and fails if the public page is missing, stale, or policy-incomplete.
- `node scripts/verify-play-submission-package.mjs` checks the App content
  packet against the HUD manifest, hosted privacy policy source, in-app privacy
  policy source, data-safety notes, console checklist, English listing files,
  and the generated Play Console handoff packet when using default repo paths.
- The publish helper now patches the English Play listing and localized release
  notes during `--commit` unless `--skip-listing` or `--skip-release-notes` is
  supplied.
- HUD release manifest package is `ai.openclaw.app.hud` and does not request
  SMS, Call Log, camera, location, contacts, calendar, or media-library
  permissions.
- Google Play Android Developer API is enabled for the active gcloud project.
- Non-authorized gcloud accounts are rejected before the helper requests an
  upload token or creates a Play edit.
- With an authorized OAuth account selected through `--gcloud-account`,
  `GOOGLE_PLAY_GCLOUD_ACCOUNT`, or active gcloud config, API preflight should
  reach Google Play but still return
  `Package not found: ai.openclaw.app.hud`.
- API publishing cannot proceed until the Play Console app exists for
  `ai.openclaw.app.hud`. The Android Publisher API can create edits and upload
  bundles for an existing package; first app creation still has to happen in
  Play Console.
