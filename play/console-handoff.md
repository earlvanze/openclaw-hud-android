# Google Play Console Handoff

Generated from repository sources. Run `node scripts/render-play-console-handoff.mjs --check` before publishing.

## Package

- Package name: `ai.openclaw.app.hud`
- Initial track: `internal`
- Initial release status: `draft`
- OAuth publisher accounts allowed by helper: `earlvanze@gmail.com`, `earl@earlbnb.com`

## Signed Bundle

- AAB: `build/release-bundles/openclaw-2026.7.17.3-hud-release.aab`
- SHA-256: `fbb127340e3c77cffe2c849898f9d2e767ff5ab4956170ab428fe620bff6e2e7`
- Version: 2026.7.17.3 (2026071703)

## Remaining Console Blockers

- [x] Create app in Play Console (verifiedAt=2026-07-15; source=Google Play Console > All apps > OpenClaw HUD (ai.openclaw.app.hud); notes=Verified that the separate OpenClaw HUD app exists as Play app 4973554177385209084.)
- [x] Configure internal testers (verifiedAt=2026-07-15; source=Google Play Console > Test and release > Testing > Internal testing > Testers; notes=Verified that the dedicated OpenClaw HUD Internal Testers list is attached with one intended tester.)
- [x] Configure reviewer app access (verifiedAt=2026-07-15; source=Google Play Console > Policy and programs > App content > Sign in details; notes=Verified that Play contains account-free Demo Mode instructions covering the HUD without a gateway or external hardware, plus the assistant-response flag and report flow.)
- [x] Implement in-app AI-content reporting to the developer

Console flags and evidence entries live in `play/app-content-answers.json` under `finalSubmission`. The AI-content gate lives under `aiGeneratedContent`. Flip a flag only after the matching setup or implementation is complete.

## Store Listing

Title: OpenClaw HUD

Short description: Minimal assistant HUD for Android external and wearable displays.

Full description:

OpenClaw HUD is a low-distraction assistant companion for Android devices connected to external or wearable displays.

The app uses Android Presentation mode and automatically targets a compatible USB-C, HDMI, wireless, or wearable display. It has been tested with Samsung DeX, foldable Android phones, and AirVision M1. Glanceable green text on black keeps status, chat, notifications, captions, microphone state, and speaker state readable without filling the display with a full desktop interface.

Key features:
- Automatic Android Presentation routing with first, last, largest, and optional AirVision-preferred display targets
- Remembered-display pinning that survives Android display-ID changes, waits safely when selected hardware disconnects, and recovers from transient Presentation failures
- Runtime display names in identification and host restore screens
- Minimal always-on HUD for external displays
- OpenClaw gateway pairing and secure local setup storage
- Compact chat with hardware keyboard send support
- Safe execution approvals with gateway-sanitized command previews, deny and allow-once actions, expiry handling, external gamepad controls, and optional global visibility that requires explicit gateway administrator scope
- In-app offensive-response reporting with explicit payload review, direct developer delivery, and receipt confirmation
- Actionable notification summaries with tap-to-open and swipe browsing
- Voice controls with wake-word and push-to-talk workflows
- Samsung/native caption launcher plus OpenClaw realtime translation fallback
- External-display profiles for view mode, animated Full/Wide/Compact/Panoramic frame shapes, custom labels, JSON backup/import, placement, main-screen visibility, safe area, display identification, software brightness, virtual distance, HUD scale, gesture/hotkey behavior, profile reset, and low-overhead preferences
- Capability-driven input for external touchscreens, touchpads, mice, styli, trackballs, wheels, rotary controls, DPAD/Page keys, gamepads, joystick/hat axes, and media-button mic control while the HUD is focused
- Optional speaker routing, IPD calibration, Eye Care, 3D Mode, USB diagnostics, and Windows handoff tools for AirVision M1
- AirVision Windows app handoff export for applying saved Android profile values and reviewing active HUD runtime scale/caption capacity, all-profile runtime summaries, runtime metadata freshness, HUD controls, 18-feature companion parity states, Cyber-observed ASUS AirVision 1.0.7.1 settings-key evidence, Windows-only spatial/mirror capability status, and companion preferences during ASUS AirVision sessions on Cyber or another Windows host
- AirVision companion app preferences for startup view, language intent, demo mode, software version, EULA note, in-app privacy policy, FAQ/tutorials, product registration, and ASUS support links
- Optional USB firmware-link detection, device identity, descriptor diagnostics, readable/writable HID report-path summaries, per-feature firmware-apply readiness, per-profile runtime summaries, desired firmware-sync state, protocol-capture targets, sanitized capture-results import preview with captured-review, pending, and source-evidence labels, and firmware-update handoff provenance for AirVision M1 hardware status
- Agent and provider/model controls for OpenClaw sessions

OpenClaw HUD is intended for users who already run an OpenClaw gateway. It does not use AccessibilityService, overlay windows, SMS, or Call Log permissions in the Play/HUD build. Notification access is optional and is used only after the user grants Android notification-listener access.

Release notes:

In-app privacy policy
- Review execution requests: deny or allow once. Global approvals require explicit administrator scope; Y allows once, B/Esc denies.
- external touchscreens, touchpads, mice, styli, and trackballs; wheels, rotary controls, DPAD/Page keys, gamepads; normalized joystick or hat axes.
- M1 tap and swipe support stays on the capability-driven input path in External HUD Input.
- Keep Presentation recovery, display pinning, frame-shape morphing, replies, and active-run stopping.

## Privacy Policy

- Hosted URL: https://earlvanze.github.io/openclaw-hud-android/privacy-policy.html
- Source file: `play/privacy-policy.md`
- Hosted page source: `docs/privacy-policy.html`
- In-app location: Settings > External Display HUD > App Preferences > Privacy Policy

## Screenshots

Use the tracked fallback screenshots when live Fold/M1 capture is unavailable. They are generated by `node scripts/render-play-screenshots.mjs` and validated by CI.

- `play/screenshots/phone/hud-demo.png`
- `play/screenshots/phone/settings-demo.png`

## App Access

Open the app, go to Settings > External Display HUD > App Preferences, and enable Demo Mode. It exposes the HUD without an account, credentials, pairing code, purchase, or external hardware; gateway pairing is optional. Reviewers can inspect chat, notifications, voice/caption controls, display placement, and external-display settings. Each assistant response has a flag button that opens the offensive-content report form and displays the exact submitted-data disclosure before confirmation.

## AirVision Companion Review Evidence

These steps exercise the Windows-like AirVision companion controls that can be reviewed from the Android HUD build without a live gateway or live M1.

- Settings > AirVision M1 > App Preferences > Demo Mode
- Settings > AirVision M1 > Windows Spatial & Mirror Controls > Cast
- Settings > AirVision M1 > Windows Spatial & Mirror Controls > Display
- Settings > AirVision M1 > Firmware Link per-control WAIT/CAPTURE/READY rows
- Settings > AirVision M1 > Firmware Updates > Export
- Settings > AirVision M1 > Windows App Handoff > Export
- Settings > AirVision M1 > Diagnostics Export > Export

Capability states:

- HUD presentation and DeX display targeting: Reviewable offline
  Review path: Enable Demo Mode, then open the HUD tab or launch the HUD presentation.
  Evidence: Live validation on 2026-07-16 rendered Demo Mode on a 1920 x 1080, 60 Hz M1 Android Presentation display while the Fold used another app; fallback screenshots and CI also exercise the phone review path.
- Windows-like profile controls: Reviewable offline
  Review path: Settings > AirVision M1 profile controls, profile backup/import, and Windows App Handoff export.
  Evidence: Saved Android values, per-profile runtime summaries, and export text are generated without raw USB serials.
- Brightness: M1 optional for review
  Review path: Settings > AirVision M1 profile brightness, HUD dimming preview, and M1 touch-bar hardware brightness.
  Evidence: Software HUD dimming is reviewable offline; live M1 testing proves panel brightness behavior and firmware touch-bar passthrough.
- Screen distance and HUD scale: Reviewable offline
  Review path: Settings > AirVision M1 profile distance and HUD scale controls.
  Evidence: Virtual HUD distance scaling, per-profile HUD scale, and runtime summaries are generated without a live M1.
- IPD, fit, clarity, and text size: M1 optional for review
  Review path: Settings > AirVision M1 IPD, Fit & Clarity guidance, and diagnostics export.
  Evidence: IPD calibration defaults to 67 mm and fit guidance is reviewable offline; physical focus proof needs the live M1.
- Splendid, Eye Care, and blue-light filter: Firmware-gated
  Review path: Settings > AirVision M1 Splendid and Eye Care profile controls.
  Evidence: Android previews HUD color/warmth states offline; true panel preset writes need ASUS HID protocol evidence.
- Motion Sync, 3D Mode, and Light Load Mode: Firmware-gated
  Review path: Settings > AirVision M1 Motion Sync, 3D Mode, Light Load Mode, and runtime diagnostics.
  Evidence: Light Load trims Android HUD work offline; Motion Sync and panel 3D writes remain firmware-protocol gated.
- Gesture and hotkey settings: M1 optional for review
  Review path: Settings > AirVision M1 > Gesture & Hotkey Settings and Diagnostics Export.
  Evidence: Single-tap, double-tap, swipe, brightness-key, and media-key mappings are stored offline. Settings and diagnostics export shortcut-menu parity for ASUS brightness/volume/distance behavior, plus the active M1 brightness-key/media-key mode, Android consumption state, step sizes, firmware passthrough expectation, and Windows gesture catalog entries for ASUS brightness swipe, play/pause tap, instant transparent, center virtual screens, 3D hold, and shortcut-menu hold/slide behavior; live M1 proof is needed for firmware-delivered events.
- USB firmware-link diagnostics: M1 optional for review
  Review path: Settings > AirVision M1 > Firmware Link and Diagnostics Export.
  Evidence: Firmware Link shows per-control WAIT/CAPTURE/READY rows with target value, Android effect, firmware status, missing evidence, and blocker text. Offline demo shows blocked/write-readiness states; connected M1 adds live descriptor and HID report-path context.
- Firmware apply and update: Firmware-gated
  Review path: Settings > AirVision M1 > Firmware Updates > Export.
  Evidence: Android write actions remain blocked until sanitized ASUS HID capture evidence and live M1 write tests are both present.
- Samsung/native captions and OpenClaw translation fallback: Reviewable offline
  Review path: Voice tab caption controls and Samsung/native caption launcher.
  Evidence: Native captioning is delegated to Android/Samsung UI; OpenClaw fallback exports structured caption-mode status with sage-router fast profile, thinking off, selected source/target languages, and S1/S2 speaker labels.
- App preferences and profile backup: Reviewable offline
  Review path: Settings > AirVision M1 > App Preferences and Profile Backup.
  Evidence: Startup view, display target, language, speaker, visible structured native captions plus OpenClaw translation fallback status, translation languages, demo mode, structured support/legal/registration metadata, privacy/support links, and JSON backup/import are reviewable offline.
- Windows app profile handoff: Reviewable offline
  Review path: Settings > AirVision M1 > Windows App Handoff > Export.
  Evidence: The Android app exports Cyber/Windows ASUS AirVision apply steps, runtime summaries, gesture mappings, row-level observed Windows surfaces/keys/defaults/capture implications, structured caption/translation mode status, structured support/legal/registration metadata, and privacy reminders without raw USB serials.
- Device information: M1 optional for review
  Review path: Settings > AirVision M1 > Firmware Link and diagnostics export.
  Evidence: USB manufacturer/product, vendor/product ID, descriptor version, and interface details populate when the M1 is connected.
- Identify marker: Reviewable offline
  Review path: Settings > AirVision M1 > Identify or Demo Mode HUD presentation.
  Evidence: Android can show a temporary HUD 1 marker on the Presentation display for reviewer and external-display identification.
- Multi-screen desktop layouts: M1 optional for review
  Review path: Settings > AirVision M1 > Display Target, presentation routing diagnostics, and DeX launch script.
  Evidence: External-display targeting and physical main-screen visibility are configurable offline; final DeX/M1 topology proof needs a live external display.
- Windows-only spatial controls: Windows-only handoff
  Review path: Settings > AirVision M1 > Windows-only spatial controls and Windows App Handoff export.
  Evidence: Cursor Follow, Center Cursor, and 3DoF are reported as Windows-only with structured fallback state. Android provides distance-hotkey mapping status, hardware touchpad passthrough status, and limitation text explaining the required ASUS Windows app surfaces.
- Unity mirror window / projected glasses view: Windows-only handoff
  Review path: Settings > AirVision M1 > Cast, Display, and Windows App Handoff mirror fallback guidance.
  Evidence: The ASUS Unity mirror window and Ctrl+Alt+E shortcut stay Windows-only; Settings, diagnostics, and Windows App Handoff provide structured Cast, Display, and Samsung DeX sharing fallback actions plus limitations.

Windows app apply matrix:

- View Mode
  Windows target: Working, Gaming, Infinity, Custom 1, or Custom 2
  Android effect: Per-mode Android profile slot and custom label state
  Proof: Reviewable offline in Settings, profile backup, Windows App Handoff, and diagnostics export.
  Firmware gate: none
- Brightness
  Windows target: Panel brightness percentage
  Android effect: Software HUD dimming plus M1 hardware touch-bar passthrough
  Proof: Software dimming is reviewable offline; panel brightness proof needs live M1 hardware.
  Firmware gate: HID capture pending
- Screen distance
  Windows target: Virtual screen distance in centimeters
  Android effect: HUD scale derived from virtual distance and per-profile HUD scale
  Proof: HUD scaling is reviewable offline; firmware-level distance write proof needs live M1 hardware.
  Firmware gate: HID capture pending
- IPD
  Windows target: Inter-pupillary distance in millimeters
  Android effect: Stored calibration, ASUS range check, and fit/clarity guidance
  Proof: Calibration and guidance are reviewable offline; physical focus proof needs live M1 hardware.
  Firmware gate: HID capture pending
- Splendid / Eye Care
  Windows target: Standard, Theater, Office, Game, Eye Care, and blue-light percentage
  Android effect: HUD color and warmth preview overlays
  Proof: Preview overlays are reviewable offline; true panel preset proof needs live M1 hardware.
  Firmware gate: HID capture pending
- Motion Sync
  Windows target: Motion Sync on or off
  Android effect: Stored desired state only
  Proof: Stored setting is reviewable offline; hardware behavior proof needs live M1 hardware.
  Firmware gate: HID capture pending
- Light Load Mode
  Windows target: Light Load on or off
  Android effect: Low-overhead HUD mode with trimmed transcript/caption history and locked IPD/3D controls
  Proof: Android runtime behavior is reviewable offline; panel state proof needs live M1 hardware.
  Firmware gate: HID capture pending
- 3D Mode
  Windows target: 3D Mode on or off
  Android effect: Stored desired state, locked off when Light Load Mode is enabled
  Proof: Android guard state is reviewable offline; panel 3D proof needs live M1 hardware.
  Firmware gate: HID capture pending
- Android HUD layout
  Windows target: none
  Android effect: HUD scale, placement, safe area, and physical main-screen visibility
  Proof: Reviewable offline with Demo Mode and screenshots; live display proof is optional for M1 framing.
  Firmware gate: none
- Display routing
  Windows target: none
  Android effect: Android Presentation display targeting for AirVision-preferred or other external displays
  Proof: Routing settings are reviewable offline; DeX/M1 topology proof needs live external display hardware.
  Firmware gate: none
- Gesture and hotkey settings
  Windows target: none
  Android effect: Single tap, double tap, swipe, brightness-key, and media-key mappings
  Proof: Stored mappings are reviewable offline; firmware-delivered event proof needs live M1 hardware.
  Firmware gate: none
- Windows spatial/mirror features
  Windows target: Cursor Follow, Center Cursor, 3DoF, or Unity mirror when needed
  Android effect: Windows-only status plus Android Cast, Display, and DeX fallback guidance
  Proof: Android fallback and Windows-only status are reviewable offline; ASUS spatial/mirror features remain Windows workflows.
  Firmware gate: Windows-only

Windows app settings-key evidence:

- App: ASUS AirVision 1.0.7.1
- Build time: 20250414_112726
- SDK: 1.0.0.1
- HID library: hidapi 0.14.0
- Settings keys: VirtualSpaceDistance, SoftwareIPD, DisplaySplendidMode, EyeCareLevel, PreventMotionBlur, IsEcoMode, CenterCursorHotkey, DistanceHotkey
- Proof: Windows App Handoff and diagnostics export include Cyber-observed ASUS AirVision version/build/settings-key evidence for reviewer inspection without a live M1.
- Boundary: The exported evidence omits raw HID bytes, raw USB serial values, user-specific paths, gateway endpoints, auth tokens, and chat history.

Demo Mode lets reviewers verify the 18-feature AirVision companion HUD catalog, 12-row Windows app apply matrix, structured M1 hardware-key diagnostics, and Cyber-observed ASUS AirVision 1.0.7.1 settings-key evidence without a live gateway or live M1. Cast and Display open Android or DeX mirror fallback settings outside the HUD. Firmware-update handoff, Windows app handoff, and diagnostics exports are user-initiated files that omit raw USB serial values; Android firmware writes remain blocked until validated ASUS HID protocol evidence exists.

Reviewer evidence sources:

- Release verifier: `node scripts/verify-play-hud-release.mjs`
- Submission verifier: `node scripts/verify-play-submission-package.mjs`
- Screenshot capture: `scripts/capture-play-screenshots.sh`
- CI workflow: `.github/workflows/android-hud.yml`

## App Content Answers

- Ads: does not contain ads
- Target audience: 18+
- Designed for children: no
- Content rating category: Productivity
- Account creation in app: no
- Data deletion path: Android Settings > Apps > OpenClaw HUD > Storage > Clear data, or uninstall the app.
- Data sale: no
- Data used for advertising: no
- Data encrypted in transit answer: no_not_all_paths

Collected data:

- Audio files: optional; purpose App functionality; Wake word, push-to-talk, and realtime caption/translation workflows when enabled by the user.
- Messages: optional; purpose App functionality, Developer communications; HUD chat prompts, assistant responses, and notification summaries when the user connects to a gateway. A user-confirmed offensive-content report sends only the selected assistant excerpt, category, optional note, app version, and one-way message hash.
- App activity: optional; purpose App functionality; Assistant run controls, deny or allow-once execution-approval decisions, plus active session, agent, provider, and model selections when enabled.
- App info and performance: optional; purpose App functionality; Gateway connection state, external-display capability status, optional AirVision profile state, and diagnostics exports created explicitly by the user.

Not collected:

- Advertising ID
- Precise location
- Contacts
- Calendar
- SMS
- Call logs
- Photos and videos

Sensitive permission declarations:

```json
{
  "sms": false,
  "callLog": false,
  "camera": false,
  "location": false,
  "contacts": false,
  "calendar": false,
  "mediaLibrary": false,
  "accessibilityService": false,
  "overlayWindows": false,
  "microphone": true,
  "notifications": true,
  "nearbyDevices": true
}
```

## Local Gates

- `node scripts/render-airvision-firmware-capture-plan.mjs --check`
- `node scripts/verify-airvision-firmware-capture-results.mjs`
- `node scripts/test-airvision-firmware-capture-results.mjs`
- `node scripts/test-airvision-firmware-capture-plan-renderer.mjs`
- `node scripts/test-install-launch-hud.mjs`
- `node scripts/render-play-screenshots.mjs --check`
- `node scripts/test-play-screenshot-tools.mjs`
- `node scripts/test-play-publish-helper.mjs`
- `node scripts/render-privacy-policy-site.mjs --check`
- `node scripts/verify-play-submission-package.mjs`
- `node scripts/test-play-submission-verifier.mjs`
- `node scripts/render-play-console-evidence-template.mjs --verified-at YYYY-MM-DD --json-only` after external Console blockers are complete
- `node scripts/report-play-readiness.mjs`
- `node scripts/publish-play-internal.mjs --dry-run`
- `node scripts/verify-play-submission-package.mjs --final` after Console blockers are complete
- `node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account earlvanze@gmail.com --auth-check` after OAuth login
- `node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account earl@earlbnb.com --auth-check` after OAuth login
- `node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account <allowed-account> --preflight` after the Play Console app exists
- `node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account <allowed-account> --commit` for the first internal draft upload
