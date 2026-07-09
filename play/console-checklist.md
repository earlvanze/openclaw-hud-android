# Google Play Console Checklist

Package: `ai.openclaw.app.hud`

Use this checklist before running `node scripts/publish-play-internal.mjs --commit`.

- Create the app in Google Play Console with package name `ai.openclaw.app.hud`.
- Enable Google Play Android Developer API for the linked Google Cloud project.
- Grant the publishing service account access in Play Console.
- Complete App content forms, including Data safety, Ads, App access, Content rating, Target audience, and Data deletion.
- Use the copy in `play/listings/en-US/` for the initial English listing.
- Use `play/privacy-policy.md` as the hosted privacy-policy source.
- Run `node scripts/verify-play-hud-release.mjs` before every Play upload.
- Upload `build/release-bundles/openclaw-2026.7.9-hud-release.aab` to the internal track first.
- Keep the initial release status as `draft` until screenshots, policy forms, and tester access are verified.

Current local status:

- Latest signed HUD AAB builds successfully from `main`:
  `build/release-bundles/openclaw-2026.7.9-hud-release.aab`
- Latest HUD AAB SHA-256:
  `5e8a395313cb50614ed8f6da06733ad1cfb1d040af613b488b5644cd7674066e`
- `node scripts/verify-play-hud-release.mjs` passes against the latest signed
  HUD AAB, packaged HUD manifest, and English Play listing copy.
- `lintHudRelease` passes.
- `testHudDebugUnitTest` passes.
- `node scripts/publish-play-internal.mjs --dry-run` validates the local AAB,
  English listing copy, and release notes character limits.
- The publish helper now patches the English Play listing and localized release
  notes during `--commit` unless `--skip-listing` or `--skip-release-notes` is
  supplied.
- HUD release manifest package is `ai.openclaw.app.hud` and does not request
  SMS, Call Log, camera, location, contacts, calendar, or media-library
  permissions.
- Google Play Android Developer API is enabled for the active gcloud project.
- API preflight reaches Google Play with gcloud auth, but returns
  `Package not found: ai.openclaw.app.hud`.
- API publishing cannot proceed until the Play Console app exists for
  `ai.openclaw.app.hud`. The Android Publisher API can create edits and upload
  bundles for an existing package; first app creation still has to happen in
  Play Console.
