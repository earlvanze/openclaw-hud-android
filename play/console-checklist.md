# Google Play Console Checklist

Package: `ai.openclaw.app.hud`

Use this checklist before running `node scripts/publish-play-internal.mjs --commit`.

- Create the app in Google Play Console with package name `ai.openclaw.app.hud`.
- Enable Google Play Android Developer API for the linked Google Cloud project.
- Grant the publishing service account access in Play Console.
- Complete App content forms, including Data safety, Ads, App access, Content rating, Target audience, and Data deletion.
- Use the copy in `play/listings/en-US/` for the initial English listing.
- Use `play/privacy-policy.md` as the hosted privacy-policy source.
- Upload `build/release-bundles/openclaw-2026.7.9-hud-release.aab` to the internal track first.
- Keep the initial release status as `draft` until screenshots, policy forms, and tester access are verified.

Current local status:

- Latest signed HUD AAB builds successfully from `main`:
  `build/release-bundles/openclaw-2026.7.9-hud-release.aab`
- Latest HUD AAB SHA-256:
  `8ff8a4c3925915856efdf69b90a231b0535c551294c05b3860c0eced2eeb2f31`
- `lintHudRelease` passes.
- `testHudDebugUnitTest` passes.
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
