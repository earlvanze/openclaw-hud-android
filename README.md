## OpenClaw HUD Android

Status: **active HUD prototype**. This repository contains the Android
companion/HUD app for OpenClaw, optimized for Samsung DeX and the Asus
AirVision M1 display. It has been live-tested with a Samsung Galaxy Fold 7. The
app is still pre-release, but the HUD path is usable for live testing.

Current live target:

- Host context: Cyber gateway / Umbrel OpenClaw.
- Device path: Fold over wireless ADB, with the M1 connected to the Fold.
- HUD package: `ai.openclaw.app.hud`, labeled **OpenClaw HUD**.
- Default launch mode: Android `Presentation` mode. The phone Activity stays on
  display 0 and the HUD presentation owns the external M1 display.
- Visual mode: green text on black, minimal status lights, chat input retained.
- Known caveat: Samsung/DeX may still composite its shelf over the external
  display when focus changes. The HUD layout avoids the shelf area, but Android
  does not always let the app fully suppress that system UI.

### Rebuild Checklist

- [x] New 4-step onboarding flow
- [x] Gateway setup folded into Settings with `Setup Code` + `Manual` modes
- [x] Encrypted persistence for gateway setup/auth state
- [x] Chat UI restyled
- [x] Settings UI restyled and de-duplicated
- [x] QR code scanning in onboarding
- [x] Performance improvements
- [x] Streaming support in chat UI
- [x] Request camera/location and other permissions in onboarding/settings flow
- [x] Push notifications for gateway/chat status updates
- [x] Security hardening (biometric lock, token handling, safer defaults)
- [x] Voice tab full functionality
- [x] Agents tab for agent/session selection and provider/model controls
- [x] HUD presentation mode for wearable display testing
- [x] Minimal HUD gestures: single-tap dismisses clearable notifications,
  double-tap toggles mic, and swipe scrolls chat
- [x] HUD input: Enter sends, Shift+Enter inserts a newline
- [x] HUD notification redaction for token/password/signature-key-shaped fields
- [x] Samsung/native captions provider plus OpenClaw realtime translation fallback
- [x] AirVision M1 per-mode settings profiles with viewing mode, HUD placement,
  safe area, Splendid mode, brightness, blue-light filter, distance, HUD scale, IPD,
  Motion Sync, 3D Mode, Light Load, and active-profile reset controls
- [x] Optional AirVision M1 USB firmware-link detection, permission flow, and
  interface/endpoint diagnostics
- [ ] Full end-to-end QA and release hardening

## Open in Android Studio

- Open this repository root.

## Build / Run

From this repo root:

```bash
./gradlew :app:assemblePlayDebug
./scripts/install-launch-node.sh --serial <adb-serial> --flavor play
./gradlew :app:testPlayDebugUnitTest
```

Third-party debug flavor:

```bash
./gradlew :app:assembleThirdPartyDebug
./scripts/install-launch-node.sh --serial <adb-serial>
./gradlew :app:testThirdPartyDebugUnitTest
```

The Node installer defaults to the full sideload `thirdParty` flavor
(`ai.openclaw.app`, labeled **OpenClaw Node**) and launches the phone app on the
primary display. Pass `--flavor play` to install the Play-safe Node build, set
`OPENCLAW_NODE_APK`, or pass `--apk` to pin a specific APK. It accepts the same
`--setup-code`, `--setup-code-file`, `--setup-json`, and `--no-auto-connect`
pairing flags as the HUD installer, but it does not apply HUD presentation,
DeX taskbar, or external-display audio mutations.

AirVision / HUD debug flavor:

```bash
./gradlew :app:assembleHudDebug
./scripts/install-launch-hud.sh --serial <adb-serial>
```

The HUD flavor installs as `ai.openclaw.app.hud`, is labeled **OpenClaw HUD**,
does not request SMS or Call Log permissions, and opens directly to the HUD tab.

`scripts/install-launch-hud.sh` now defaults to `--display presentation`.
Presentation mode starts the Android Activity on display 0 and lets
`HudPresentation` render the external HUD display. This avoids forcing the app
into Samsung DeX/freeform mode by default. Presentation launches also clear any
stale HUD task and request fullscreen windowing on display 0 so a previous
forced DeX/freeform launch is less likely to bleed into the next HUD start.
When `--apk` is omitted, the script installs the newest built
`*-hud-debug.apk` from `app/build/outputs/apk/hud/debug`; set
`OPENCLAW_HUD_APK` or pass `--apk` to pin a specific APK.

Display modes:

- `--display presentation` or `--display auto`: current default; phone Activity
  on display 0, external display via Android Presentation.
- `--display default`: phone display only.
- `--display external`: find the first external display and launch the Activity
  there, using the older DeX/freeform path.
- `--display <id>`: force a specific Android display id.

The launch script also attempts non-destructive DeX/taskbar suppression and sets
Samsung external-display audio output when an external display is present.
`node scripts/test-install-launch-hud.mjs` runs an offline fake-ADB regression
test that keeps the default path pinned to presentation launch args, verifies
the explicit forced-display fallback still works, and covers the no-external
display case.
`node scripts/test-install-launch-node.mjs` covers the parallel Node install
flow, including Play/full-flavor selection, setup-code forwarding, and the
absence of HUD-only display mutations.

When the M1/Fold is available, capture the current presentation placement,
DeX/taskbar state, display topology, audio route hints, and recent HUD logs with:

```bash
./scripts/diagnose-hud-presentation.sh --serial <adb-serial>
```

The HUD flavor is designed to remain Google Play publishable: it uses the
standard notification-listener grant for notification summaries, foreground
microphone permission for wake/push-to-talk input, and normal foreground app UI
for the glasses display. It does not use AccessibilityService, overlay windows,
SMS, or Call Log permissions.

HUD display behavior is intentionally low-distraction for wearable displays:
Google Maps / Waze navigation notifications are promoted to the main glance
line, OpenClaw run status and active tool calls are shown as compact text, and
voice wake/mic/speaker state stays visible without opening the full chat
interface.

HUD controls:

- Single-tap a clearable notification to dismiss it.
- Double-tap the HUD/touch surface to toggle mic capture when available.
- Swipe vertically to scroll the compact chat transcript.
- Tap the thinking-level text in the status lights to cycle thinking level.
- Tap `cc` in the HUD status lights to cycle captions: Off -> Samsung/native
  -> OpenClaw translation -> Off.
- Press Enter on a hardware keyboard to send the chat input.
- Press Shift+Enter for a newline.

The single-tap, double-tap, swipe, M1 brightness-key, and M1 media/tap-key
actions are configurable in Settings -> AirVision M1 -> Gesture & Hotkey
Settings. Defaults are tuned for walking HUD use: single-tap clears the current
notification, double-tap toggles mic, vertical swipe scrolls chat, and M1
brightness-key events can scroll chat, step Android HUD brightness, or step
virtual distance while the HUD is focused. Brightness and distance key changes
show a temporary green HUD text confirmation.

AirVision M1 companion settings live in Settings -> AirVision M1:

| Windows AirVision feature | Android HUD status |
| --- | --- |
| Working / Gaming / Infinity / Custom modes | Implemented as saved HUD profile slots with mode-specific defaults, user-named Custom 1 / Custom 2 labels, copy-current-to-custom actions, JSON profile backup/import, active-profile reset, HUD placement, physical main screen behavior, safe area, explicit HUD scale/zoom, brightness, distance, IPD, Splendid, Eye Care, Motion Sync, and Light Load values. |
| Brightness | Implemented as software HUD dimming. Hardware brightness remains available from the M1 touch bar. |
| Screen distance | Implemented as virtual HUD distance scaling. |
| HUD scale / zoom | Implemented as a per-profile Android Presentation scale percentage layered with view-mode and virtual-distance scaling. |
| IPD | Stored as a calibration value, defaulting to 67 mm. Adjustment is locked while Light Load Mode is enabled, matching the ASUS app. Firmware-level apply still needs the ASUS HID protocol. |
| Fit, clarity, and text size | Implemented as derived Settings guidance and diagnostics covering the ASUS documented 53.5-74.5 mm IPD range, 3D-mode blur checks, prescription/fit reminders, Android HUD scale, virtual-distance zoom, DeX display scaling, and browser zoom fallbacks. |
| Splendid Standard / Theater / Office / Game / Eye Care | Stored as profile settings with subtle Android HUD color previews for Theater, Office, Game, and Eye Care. True panel presets need HID support. |
| Blue Light Filter | Implemented as Android HUD warm filtering and available only in Eye Care mode, matching the ASUS app. |
| Motion Sync | Stored in the AirVision profile; hardware apply needs HID support. |
| 3D Mode | Stored in the AirVision profile and disabled while Light Load Mode is enabled. Hardware apply needs HID support. |
| Light Load Mode | Stored in the AirVision profile for low-overhead HUD operation, trims transcript/caption history, skips nonessential color-preview overlays, and locks IPD/3D adjustment while enabled. |
| Gesture & Hotkey Settings | Implemented for HUD touch actions, swipe-to-scroll, brightness-key scroll, Android HUD brightness, or virtual-distance handling, and M1 media/tap key double-tap mic behavior. |
| Cursor Follow / Center Cursor / 3DoF | Surfaced as Windows-only capability status in Android settings, diagnostics, and the Windows app handoff. Android can map the AirVision distance hotkey concept to M1 brightness-key events, but it does not claim Windows virtual-cursor or 3DoF control. |
| Unity mirror window / projected glasses view | Surfaced as Windows-only capability status in Android settings, diagnostics, and the Windows app handoff. Android cannot open the ASUS Unity mirror window or `Ctrl+Alt+E` shortcut, but Settings and handoff guidance provide Cast and Display shortcuts for Android/DeX screen-sharing fallback outside the HUD. |
| Demo Mode / Tutorials | Implemented as Android Demo Mode for deterministic HUD review, tutorials, screenshots, and fit checks without a live gateway or live M1. The hidden ASUS Windows tutorial shortcut remains Windows-only and is reported separately in diagnostics. |
| Companion parity states | Implemented as an 18-feature canonical app/export catalog that marks AirVision features as offline-reviewable, M1-optional, firmware-gated, or Windows-only. Settings, diagnostics, Windows App Handoff, and Play review evidence render from this catalog so Android UI claims and Windows handoff guidance stay aligned. |
| App Preferences | Implemented with startup view, AirVision companion language preference with Android locale application, speaker state, Samsung/native captions preference, OpenClaw translation caption source/target languages, demo mode, JSON profile backup/import, software version/build display, EULA note, in-app privacy policy, official FAQ/tutorial, product registration, and ASUS support links. |
| Windows app profile handoff | Implemented as an exportable Markdown handoff containing the active profile, derived active-runtime HUD scale/transcript/caption and overlay/dimming state, runtime-metadata freshness, a per-feature Windows app apply matrix with Android effect/live-M1/firmware-gate state, Cyber-observed ASUS AirVision 1.0.7.1 build/settings-key evidence, all saved profile values with compact runtime summaries, Android HUD gesture/hotkey mappings, Windows-only spatial/mirror capability status, companion app preferences, ASUS Windows app apply steps for Cyber sessions, Android USB context, and privacy reminders that omit raw USB serial values. |
| Device Information | Implemented Android USB identity details for manufacturer, product, USB ID, device path, device class/subclass/protocol, interface count, serial availability, USB descriptor version when Android exposes it, and firmware protocol status. Actual ASUS HID firmware version still needs vendor protocol support. |
| Firmware link | Implemented USB detection for the known AirVision M1 device (`0x0b05:0x1b3c`), Android USB permission, HID/audio/input interface status, derived readable/writable HID report-path capability summaries with endpoint packet sizes, per-feature firmware-apply readiness for Windows-style controls, per-feature Windows protocol-capture targets for View Mode/brightness/distance/IPD/Splendid/Eye Care/Motion Sync/Light Load/3D Mode, USB interface/endpoint diagnostics for protocol capture work, and a JSON diagnostics export that omits raw USB serial values. |
| Firmware update | Surfaced as a Windows-only workflow in Android settings and diagnostics. Android can export a firmware-update handoff with USB descriptor/version context, interface readiness, sanitized imported protocol-capture evidence, and ASUS support links, but ASUS firmware update checks and installs still require the Windows AirVision app. |
| Identify | Implemented as a temporary `HUD 1` marker on the Android Presentation display. |
| Multi-screen desktop layouts | Android cannot own DeX topology like the Windows app, but the HUD now supports configurable external-display targeting, per-mode placement, per-mode physical main screen visibility, and safe-area layout profiles for the M1 presentation. |

Firmware controls are intentionally read-only/status-only for now. The app can
detect the M1 USB HID control interface, request Android USB permission, and
display USB device identity plus interface/endpoint descriptors in Settings for
future ASUS protocol capture work. Firmware Updates can export an AirVision M1
firmware-update handoff for Windows/Cyber ASUS AirVision app sessions with
Android-visible USB descriptor/version context, interface readiness, sanitized
imported protocol-capture evidence, ASUS support links, and privacy reminders
that omit raw USB serial values. Firmware Link can
also export an AirVision diagnostics JSON snapshot containing current USB
readiness, descriptor summaries, derived readable/writable HID report paths with
endpoint addresses and packet sizes, USB class/subclass/protocol, interface
count, USB descriptor version when Android exposes it, per-feature firmware-apply readiness for Windows-style AirVision
controls, per-feature desired firmware-sync state derived from the active
AirVision profile, the Android firmware-write enablement decision, the validated
capture evidence required before a write can be enabled, per-feature capture
targets and probe values for future ASUS HID protocol validation, the active HUD
profile, derived HUD runtime state, Android Presentation routing state, display
candidate counts, selected display identity, profile backup/restore readiness,
imported firmware-capture-results provenance, safety counts, protocol-ready
feature labels, captured-review and pending evidence labels, blocked feature
labels/reasons, source-evidence completeness warnings, payload-safety preview
text, the live M1 write-test checklist,
all saved AirVision profile values, runtime profile metadata, compact per-profile
runtime summaries,
fit/clarity/text-size guidance,
Android demo/offline reviewer experience state, Windows-only cursor/3DoF and
Unity mirror-window capability status, Android Cast/Display mirror fallback
actions, a structured Windows app apply matrix that lists each ASUS Windows
target alongside the Android effect, live-M1 proof requirement, firmware gate,
and Windows-only state, Cyber-observed ASUS AirVision 1.0.7.1 build/settings-key
evidence for `VirtualSpaceDistance`, `SoftwareIPD`, `DisplaySplendidMode`,
`EyeCareLevel`, `PreventMotionBlur`, `IsEcoMode`, `CenterCursorHotkey`, and
`DistanceHotkey`, 18-feature companion parity-state counts for offline-reviewable, M1-optional,
firmware-gated, and Windows-only features, Windows-only firmware-update workflow
status, gesture settings, and AirVision app preferences without gateway
endpoints, auth tokens, chat history, or raw USB serial values. Settings ->
AirVision M1 -> Firmware Link also shows a compact
write-gate summary with protocol-ready and blocked feature labels so firmware
writes remain visibly blocked until the machine-checked capture results
validate a feature.
Settings -> AirVision M1 -> Windows App Handoff exports a Markdown bridge for
Cyber/Windows ASUS AirVision app sessions. It lists the active Android AirVision
profile, derived active-runtime HUD scale/transcript/caption capacity,
overlay/dimming state, runtime-metadata freshness, a per-feature Windows app
apply matrix that separates ASUS Windows targets from Android effects and
live-M1/firmware-gated proof requirements, all saved profile slots with compact
runtime summaries, Cyber-observed ASUS AirVision 1.0.7.1 build/settings-key
evidence from ReleaseInfo, SDK strings, and Data/Settings.json, Windows apply steps for View Mode, brightness, screen distance,
IPD, Splendid, Eye Care, Motion Sync, Light Load Mode, and 3D Mode, Android HUD gesture/hotkey mappings, companion app startup/display/language,
Windows-only Cursor Follow, Center Cursor, 3DoF, and Unity mirror-window status with Android Cast/Display
fallback actions, Android companion parity states, speaker, caption,
translation, and demo preferences, and Android USB context while omitting raw USB serial values,
gateway endpoints, auth tokens, and chat history.
The app does not send ASUS vendor reports until the Windows app protocol is
captured and validated.

`play/airvision-firmware-capture-plan.md` is a generated offline worksheet for
the next Windows/Cyber capture session. It is derived from the app's canonical
AirVision firmware feature list and records probe sequences for View Mode,
brightness, screen distance, IPD, Splendid, blue-light filter, Motion Sync,
Light Load Mode, and 3D Mode, plus the read-only firmware write gate,
acceptance criteria, and a per-feature capture-result template for report IDs,
payload lengths, endpoints, sanitized payload summaries, checksum/framing
notes, visible-state confirmation, and the Android firmware-write enablement
decision. The in-app export also reflects imported Windows/Cyber capture
results so protocol-ready evidence stays distinct from actual Android
hardware-write enablement. Raw payload bytes stay in private capture files and
are intentionally rejected by the checked-in results verifier.
`play/airvision-firmware-capture-results.json` is the machine-checked sanitized
capture-results file. It starts with every feature blocked and must stay free of
raw USBPcap dumps, raw USB serial numbers, tokens, and temporary review
credentials. Android firmware writes remain blocked unless the verifier sees
validated write/readback/checksum/visible-state evidence for a feature.
The Android app can import the same sanitized capture-results JSON from
Settings -> AirVision M1 -> Firmware Capture Results. Import validates the
schema and safety evidence locally, previews validated/captured-review/pending/
write-enabled/blocked feature counts, write-enabled, needs-validation, pending,
and blocked feature labels, sanitized source context, source-evidence
completeness warnings, and the payload-safety policy before applying, records a
compact summary in Settings, and persists the parsed result for Settings and
diagnostics. The imported evidence can mark a feature's capture result and
Android enablement decision as validated, but firmware writes remain disabled
until an Android HID write path is implemented and live-tested with the M1. The
same row includes a Clear
action so stale or wrong Windows/Cyber evidence can be removed without
reinstalling the app.
The Android app can also export a live copy from Settings -> AirVision M1 ->
Firmware Capture Plan, including the phone's current readable/writable HID
report-path status when the M1 is connected plus the active AirVision profile's
desired firmware-sync values and hardware-sync blocker reasons. Regenerate or
verify the tracked offline worksheet with:

```bash
node scripts/render-airvision-firmware-capture-plan.mjs
node scripts/render-airvision-firmware-capture-plan.mjs --check
node scripts/verify-airvision-firmware-capture-results.mjs
```

Captions default to Samsung/Android native captioning so the system floating
caption window can sit over the minimal HUD. The HUD `cc` status light cycles
between Samsung, OpenClaw, and Off; the Voice tab exposes the same providers as
larger controls. The OpenClaw fallback is intended for low-latency walking use
when native captions are not available or when captions need OpenClaw session
context. It forces thinking `off`, prefers the
`sage-router/fast` model profile, suppresses spoken assistant replies while
captions are active, and labels alternating turns as `S1` / `S2`. The default
OpenClaw target language is Spanish. Source and target language selection live
in the Voice tab and support Auto plus common caption targets including
English, Spanish, French, German, Italian, Portuguese, Dutch, Polish, Russian,
Ukrainian, Turkish, Arabic, Hebrew, Hindi, Indonesian, Thai, Vietnamese,
Japanese, Korean, and Chinese. Stored locale tags such as `pt-BR` and
`zh-Hans` normalize to the matching caption language.

Notification text is whitespace-cleaned and secret-shaped assignments such as
`token=...`, `password=...`, and `accountSignatureKey=...` are redacted before
being rendered in the HUD.

AirVision startup view and demo mode live in Settings -> AirVision M1 -> App
Preferences. Startup view selects the default launch tab for HUD builds. Demo
mode renders a deterministic navigation, chat, mic, speaker, and caption sample
in the HUD so tutorials, screenshots, and fit checks can be performed without a
live gateway or notification stream.

HUD Display Target lives in Settings -> AirVision M1. The app now prefers
Android displays exposed through `DISPLAY_CATEGORY_PRESENTATION`, which keeps
the phone controls on display 0 while the HUD owns the M1 presentation. AirVision
Preferred keeps the default behavior of selecting presentation displays named
like ASUS AirVision M1. Largest External, First External, and Last External
provide manual fallbacks for Samsung DeX or adapter setups where Android exposes
multiple presentation displays. If Android does not expose any presentation
targets, the router falls back to the older non-default display list. Settings
also shows the current HUD Display Route with candidate counts, selected display
identity, and whether fallback routing is active.

Custom profile names and copy-current-to-custom actions live under Viewing Mode
in Settings -> AirVision M1. The labels rename the saved Custom 1 and Custom 2
profile slots without changing their stored display settings; the copy actions
duplicate the active profile settings into a custom slot.

Profile Backup lives under Settings -> AirVision M1 -> App Preferences. Export
writes a JSON file containing AirVision M1 tuning profiles, custom labels,
gesture/hotkey settings, display targeting, startup view, language preference,
speaker state, Samsung/native captions preference, OpenClaw translation caption
source/target languages, demo mode, and derived HUD runtime metadata such as Light Load limits and
locked control availability. Import validates the same AirVision-only schema,
previews the active profile, profile slots, all-profile runtime summaries,
gesture mappings, startup target, display target, language, speaker state,
native caption state, translation caption languages, derived active-runtime HUD
scale/transcript/caption capacity, overlay/dimming state, and
profile-specific Light Load/demo/speaker/runtime-metadata warnings before applying,
accepts legacy v1/v2 backups, requires every viewing-mode profile slot exactly
once, and never includes gateway endpoints, auth tokens, accounts, chat history,
or other OpenClaw runtime state.

To pair on first launch, pass a setup code directly or from a local file:

```bash
./scripts/install-launch-hud.sh --serial <adb-serial> --setup-json /tmp/openclaw-android-pair/qr-tailnet-wss.json
```

The setup launch stores the gateway endpoint/auth, marks onboarding complete for
the HUD package, and starts connecting immediately by default. Add
`--no-auto-connect` when you want to import the setup code without opening a
gateway session on launch.

Recent live validation command:

```bash
ANDROID_HOME=/home/digit/android-sdk ANDROID_SDK_ROOT=/home/digit/android-sdk \
  ./gradlew :app:assembleHudDebug :app:testHudDebugUnitTest \
  --tests 'ai.openclaw.app.ui.HudNotificationFormatterTest'

ADB=/home/linuxbrew/.linuxbrew/bin/adb \
  ./scripts/install-launch-hud.sh --serial 100.88.253.107:46793
```

`node scripts/build-release-aab.mjs` auto-bumps Android
`versionName`/`versionCode` in `app/build.gradle.kts`, then builds signed
release bundles. The HUD/M1 bundle is the Google Play target package:

- HUD/M1 build: `build/release-bundles/openclaw-<version>-hud-release.aab`
  (`ai.openclaw.app.hud`)
- Play build: `build/release-bundles/openclaw-<version>-play-release.aab`
  (`ai.openclaw.app`)
- Third-party build: `build/release-bundles/openclaw-<version>-third-party-release.aab`
  (`ai.openclaw.app`)

Release helper:

```bash
node scripts/build-release-aab.mjs
node scripts/build-release-aab.mjs --flavor hud
```

Flavor-specific direct Gradle tasks:

```bash
./gradlew :app:bundleHudRelease
./gradlew :app:bundlePlayRelease
./gradlew :app:bundleThirdPartyRelease
```

GitHub Actions gates all three release bundle variants with the same unsigned
release flag used for local CI verification, so HUD, Play-safe Node, and full
third-party Node release minification/package regressions are caught before
pushes are treated as good. The CI verifier also checks packaged release
manifests and generated BuildConfig values so the HUD, Play-safe Node, and
third-party Node flavors keep the intended package IDs, permissions, services,
file-provider authorities, and SMS/call-log runtime gates.

Google Play internal-track publishing helper:

```bash
node scripts/verify-play-hud-release.mjs
node scripts/report-play-readiness.mjs
node scripts/report-play-readiness.mjs --json
node scripts/test-play-publish-helper.mjs
node scripts/verify-play-submission-package.mjs
node scripts/render-play-console-evidence-template.mjs --verified-at 2026-07-10 --json-only
node scripts/verify-play-submission-package.mjs --final
node scripts/publish-play-internal.mjs --dry-run
node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account earlvanze@gmail.com --auth-check
node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account earl@earlbnb.com --auth-check
node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account earlvanze@gmail.com --preflight
node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account earl@earlbnb.com --preflight
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON=/path/to/service-account.json \
  node scripts/publish-play-internal.mjs --commit
node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account earlvanze@gmail.com --commit
node scripts/publish-play-internal.mjs --auth gcloud --gcloud-account earl@earlbnb.com --commit
```

`node scripts/report-play-readiness.mjs` summarizes the HUD release verifier,
draft/final Play submission verifiers, publish dry-run, and both allowed OAuth
`--auth-check` paths in one report. Use `--strict` when a release step should
fail unless the package is actually publish-ready, and `--skip-signature` for CI
checks against unsigned Gradle release bundles.

The publish helper defaults to package `ai.openclaw.app.hud`, track `internal`,
release status `draft`, and the newest HUD AAB from either
`build/release-bundles/*-hud-release.aab` or Gradle's direct
`app/build/outputs/bundle/hudRelease/app-hud-release.aab` output. It validates
the English Play listing files and runs
`node scripts/verify-play-submission-package.mjs` during dry-run, preflight, and
commit flows. When a HUD AAB is selected, it also refuses to continue if HUD
source/build inputs are dirty or newer than the bundle, preventing stale local
artifacts from being uploaded after app changes. Commit mode creates a Play
edit, uploads the AAB, patches the localized store listing, attaches localized
release notes to the internal-track release, and refuses to upload until
`node scripts/verify-play-submission-package.mjs --final` passes, so
screenshots, hosted privacy URL, tester setup, reviewer access, and app creation
status must be recorded locally first. Auth can come from a Play
Console service-account JSON file or the active `gcloud auth print-access-token`
account. For `--auth gcloud`, the helper restricts publishing to
`earlvanze@gmail.com` or `earl@earlbnb.com` by default so an accidental active
Cloud account cannot publish the app. It also checks `gcloud auth list` before
requesting an Android Publisher token, so a selected but not locally
authenticated publisher account fails with the currently authenticated accounts
listed. Pass `--gcloud-account <email>` or set `GOOGLE_PLAY_GCLOUD_ACCOUNT` to
use an authenticated personal account without changing Cyber's global gcloud
config; `gcloud config set account <email>` is also supported. Set
`GOOGLE_PLAY_ALLOWED_ACCOUNTS` or pass `--allowed-account` only when a different
authorized Play publisher should be accepted. Store listing and policy prep
files live under `play/`. Use `--auth-check` to verify local OAuth/service
account readiness without contacting Play. Use `--preflight` to verify Play API
package/auth access before building an AAB. Use `--skip-listing` or
`--skip-release-notes` for bundle-only edits. The Play Console app must already
exist for `ai.openclaw.app.hud`; follow `play/console-checklist.md` before the
first commit.

The hosted privacy-policy page is generated from `play/privacy-policy.md` into
`docs/privacy-policy.html`:

```bash
node scripts/render-privacy-policy-site.mjs
node scripts/render-privacy-policy-site.mjs --check
```

The Pages workflow publishes `docs/` when the GitHub repository's Pages source
is configured to GitHub Actions. The prepared candidate URL is
`https://earlvanze.github.io/openclaw-hud-android/privacy-policy.html`. Keep
`play/app-content-answers.json` `finalSubmission.hostedPrivacyPolicyUrl` empty
until that exact URL is reachable, then fill it and run
`node scripts/verify-play-submission-package.mjs --final`. Final verification
fetches the public URL and confirms it still matches the generated local policy
page before any Play upload can start.

Play phone screenshots can be captured from the HUD build without a live M1 by
using the app's review/demo launch action, or rendered deterministically from
the same demo state when the phone/M1 capture path is unavailable:

```bash
ANDROID_HOME=/home/digit/android-sdk ANDROID_SDK_ROOT=/home/digit/android-sdk \
  ./gradlew :app:assembleHudDebug
./scripts/capture-play-screenshots.sh --serial <adb-serial>
node scripts/render-play-screenshots.mjs
node scripts/render-play-screenshots.mjs --check
node scripts/test-play-screenshot-tools.mjs
```

On Samsung foldables, pass `--display-id <physical-display-id>` if plain
`screencap` prints a multiple-display warning. The capture script also accepts
`--activity-display <logical-display-id>` and temporarily disables Samsung
`screen_off_pocket` while capturing, restoring the original setting on exit.

The script installs the HUD debug APK, marks the app ready for review/demo mode,
launches deterministic HUD and Settings states on the phone display, normalizes
Android screencaps to 24-bit PNG without alpha, and writes
`play/screenshots/phone/hud-demo.png`,
`play/screenshots/phone/settings-demo.png`, plus a generated
`play/screenshots/phone/manifest.json`. `render-play-screenshots.mjs` writes the
same tracked artifact paths using a host-side renderer for offline release prep.
After uploading screenshots in Play Console, record their local paths, names, or URLs in
`play/app-content-answers.json` `finalSubmission.phoneScreenshots`.
`test-play-screenshot-tools.mjs` is the offline regression gate for this path:
it converts a synthetic alpha PNG, verifies the converted asset passes the final
submission checker, verifies the checker rejects unconverted alpha PNGs, and
confirms blank/off-display captures are rejected. In final mode, local PNG
screenshots must be Play-sized, 24-bit RGB, visibly nonblank, and include the
green OpenClaw HUD accent so Samsung lock/protection overlays do not pass as
release screenshots.

`verify-play-hud-release.mjs` is the offline-safe local gate for the HUD/M1
Play target. It verifies the newest signed HUD AAB, confirms the generated
manifest package is `ai.openclaw.app.hud`, fails if restricted Play-risk
permissions drift back into the HUD flavor, confirms the notification-listener
service declaration, checks the English listing/release-notes length limits, and
confirms App Bundle language splits stay disabled while the app uses runtime
AirVision language switching. It also verifies that the generated AirVision
firmware capture worksheet is current and still contains the safety criteria
and per-feature blocked result rows needed before Android firmware writes can
be enabled. In-app capture-plan exports also include protocol-ready feature
labels, blocked feature labels, and the live M1 write-test checklist so
sanitized Windows evidence stays distinct from Android write enablement. The language split setting is
intentional: Play
can otherwise deliver only the install-time language resources, which breaks the
in-app AirVision companion language menu. Run
`node scripts/test-play-hud-release-verifier.mjs` after changing the release
verifier; it covers the language-split regression path with temporary sources.
Run `node scripts/test-airvision-firmware-capture-plan-renderer.mjs` after
changing the AirVision firmware feature list or worksheet renderer.
GitHub Actions also builds the HUD release bundle with
`-POPENCLAW_ANDROID_ALLOW_UNSIGNED_RELEASE=true` and runs the verifier with
`--skip-signature` so CI can validate the release manifest and listing without
repo-stored signing keys. Publishing still requires the locally signed bundle
from `scripts/build-release-aab.mjs`.

`verify-play-submission-package.mjs` checks the local Play App content packet in
`play/app-content-answers.json` against the generated HUD manifest, privacy
policy, in-app privacy policy source, data-safety notes, console checklist, and
English listing files. With the default repository paths it also verifies that
the generated Play Console handoff packet is current. When a locally signed HUD
AAB exists in `build/release-bundles/`, it also verifies that the console
checklist names the latest signed bundle and SHA-256. It also checks the
structured AirVision Companion Review Evidence used in `play/console-handoff.md`
so offline reviewer steps for Demo Mode, Cast/Display mirror fallback, firmware
update handoff export, diagnostics export, 18-feature companion parity states,
the 12-row Windows app apply matrix, and the Cyber-observed ASUS Windows app
settings-key evidence stay present. It is a drift guard for Play Console form prep; the final hosted privacy-policy URL, screenshots,
tester access, and any reviewer setup codes still belong in Play Console.

Use `node scripts/verify-play-submission-package.mjs --final` only when the
external Play Console artifacts have been filled into
`play/app-content-answers.json` under `finalSubmission`. The normal verifier is
the local draft gate; `--final` is expected to fail until the Play app exists,
the hosted privacy policy URL is public, at least two phone screenshots are
recorded, internal testers are configured, reviewer access instructions have
been entered in Play Console, and each completed Play Console blocker has
`finalSubmission.consoleEvidence` source/date/notes metadata. The final verifier
also fetches the public privacy URL and checks that it matches the generated
policy page. When screenshot entries are local paths, `--final` also validates
that the files are JPEG or 24-bit PNG without alpha, no larger than 8 MiB, at
least 320 px on the shortest side, no larger than 3840 px on the longest side,
and no more than 2:1 in either orientation. The publish helper runs this final
verifier automatically in `--commit` mode before contacting the Google Play
edit/upload API.

Use `node scripts/render-play-console-evidence-template.mjs --verified-at
YYYY-MM-DD --json-only` after each external Play Console blocker is actually
complete to generate copyable `finalSubmission.consoleEvidence` JSON. The helper
does not flip readiness booleans; it only renders the source/date/notes evidence
shape that the final verifier requires. The reviewer-access evidence notes refer
to the generated handoff's AirVision Companion Review Evidence section so Play
review can verify the AirVision controls without a live M1.

## Kotlin Lint + Format

```bash
./gradlew :app:ktlintCheck :benchmark:ktlintCheck
./gradlew :app:ktlintFormat :benchmark:ktlintFormat
```

Android framework/resource lint (separate pass):

```bash
./gradlew :app:lintDebug
```

`gradlew` auto-detects the Android SDK at `~/Library/Android/sdk` (macOS default) if `ANDROID_SDK_ROOT` / `ANDROID_HOME` are unset.

## Macrobenchmark (Startup + Frame Timing)

```bash
./gradlew :benchmark:connectedDebugAndroidTest
```

Reports are written under:

- `benchmark/build/reports/androidTests/connected/`

## Perf CLI (low-noise)

Deterministic startup measurement + hotspot extraction with compact CLI output:

```bash
./scripts/perf-startup-benchmark.sh
./scripts/perf-startup-hotspots.sh
```

Benchmark script behavior:

- Runs only `StartupMacrobenchmark#coldStartup` (10 iterations).
- Prints median/min/max/COV in one line.
- Writes timestamped snapshot JSON to `benchmark/results/`.
- Auto-compares with previous local snapshot (or pass explicit baseline: `--baseline <old-benchmarkData.json>`).

Hotspot script behavior:

- Ensures debug app installed, captures startup `simpleperf` data for `.MainActivity`.
- Prints top DSOs, top symbols, and key app-path clues (Compose/MainActivity/WebView).
- Writes raw `perf.data` path for deeper follow-up if needed.

## Run on a Real Android Phone (USB)

1) On phone, enable **Developer options** + **USB debugging**.
2) Connect by USB and accept the debugging trust prompt on phone.
3) Verify ADB can see the device:

```bash
adb devices -l
```

4) Install + launch debug build:

```bash
./gradlew :app:installPlayDebug
adb shell am start -n ai.openclaw.app/ai.openclaw.app.MainActivity
```

If `adb devices -l` shows `unauthorized`, re-plug and accept the trust prompt again.

### USB-only gateway testing (no LAN dependency)

Use `adb reverse` so Android `localhost:18789` tunnels to your laptop `localhost:18789`.

Terminal A, from the parent OpenClaw workspace:

```bash
pnpm openclaw gateway --port 18789 --verbose
```

Terminal B (USB tunnel):

```bash
adb reverse tcp:18789 tcp:18789
```

Then in app **Settings → Manual**:

- Host: `127.0.0.1`
- Port: `18789`
- TLS: off

## Hot Reload / Fast Iteration

This app is native Kotlin + Jetpack Compose.

- For Compose UI edits: use Android Studio **Live Edit** on a debug build (works on physical devices; project `minSdk=31` already meets API requirement).
- For many non-structural code/resource changes: use Android Studio **Apply Changes**.
- For structural/native/manifest/Gradle changes: do a full reinstall.
- Canvas web content already supports live reload when loaded from Gateway `__openclaw__/canvas/`.

## Connect / Pair

1) Start the gateway from the parent OpenClaw workspace:

```bash
pnpm openclaw gateway --port 18789 --verbose
```

2) In the Android app:

- Open **Settings**.
- Use **Setup Code** or **Manual** mode to connect.

3) Approve pairing (on the gateway machine):

```bash
openclaw devices list
openclaw devices approve <requestId>
```

For HUD pairing, prefer `scripts/install-launch-hud.sh --setup-json ...` so the
HUD package is marked onboarded and starts connecting immediately. Add
`--no-auto-connect` to store the setup code without opening a gateway session on
launch.

## Permissions

- Discovery:
  - Android 13+ (`API 33+`): `NEARBY_WIFI_DEVICES`
  - Android 12 and below: `ACCESS_FINE_LOCATION` (required for NSD scanning)
- Foreground service notification (Android 13+): `POST_NOTIFICATIONS`
- Camera:
  - `CAMERA` for `camera.snap` and `camera.clip`
  - `RECORD_AUDIO` for `camera.clip` when `includeAudio=true`

## Google Play Restricted Permissions

As of March 19, 2026, these manifest permissions are the main Google Play policy risk for this app:

- `READ_SMS`
- `SEND_SMS`
- `READ_CALL_LOG`

Why these matter:

- Google Play treats SMS and Call Log access as highly restricted. In most cases, Play only allows them for the default SMS app, default Phone app, default Assistant, or a narrow policy exception.
- Review usually involves a `Permissions Declaration Form`, policy justification, and demo video evidence in Play Console.
- If we want a Play-safe build, these should be the first permissions removed behind a dedicated product flavor / variant.

Current OpenClaw Android implication:

- APK / sideload build can keep SMS and Call Log features.
- Google Play build should exclude SMS send/search and Call Log search unless the product is intentionally positioned and approved as a default-handler exception case.
- The repo now ships this split as Android product flavors:
  - `play`: removes `READ_SMS`, `SEND_SMS`, and `READ_CALL_LOG`, and hides SMS / Call Log surfaces in onboarding, settings, and advertised node capabilities.
  - `thirdParty`: keeps the full permission set and the existing SMS / Call Log functionality.

Policy links:

- [Google Play SMS and Call Log policy](https://support.google.com/googleplay/android-developer/answer/10208820?hl=en)
- [Google Play sensitive permissions policy hub](https://support.google.com/googleplay/android-developer/answer/16558241)
- [Android default handlers guide](https://developer.android.com/guide/topics/permissions/default-handlers)

Other Play-restricted surfaces to watch if added later:

- `ACCESS_BACKGROUND_LOCATION`
- `MANAGE_EXTERNAL_STORAGE`
- `QUERY_ALL_PACKAGES`
- `REQUEST_INSTALL_PACKAGES`
- `AccessibilityService`

Reference links:

- [Background location policy](https://support.google.com/googleplay/android-developer/answer/9799150)
- [AccessibilityService policy](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en-GB)
- [Photo and Video Permissions policy](https://support.google.com/googleplay/android-developer/answer/14594990)

## Integration Capability Test (Preconditioned)

This suite assumes setup is already done manually. It does **not** install/run/pair automatically.

Pre-req checklist:

1) Gateway is running and reachable from the Android app.
2) Android app is connected to that gateway and `openclaw nodes status` shows it as paired + connected.
3) App stays unlocked and in foreground for the whole run.
4) Open the app **Agents** tab and keep it active during the run if canvas/A2UI commands require the canvas WebView attached there.
5) Grant runtime permissions for capabilities you expect to pass (camera/mic/location/notification listener/location, etc.).
6) No interactive system dialogs should be pending before test start.
7) Canvas host is enabled and reachable from the device (do not run gateway with `OPENCLAW_SKIP_CANVAS_HOST=1`; startup logs should include `canvas host mounted at .../__openclaw__/`).
8) Local operator test client pairing is approved. If first run fails with `pairing required`, approve latest pending device pairing request, then rerun:
9) For A2UI checks, keep the app on **Agents** tab; the node now auto-refreshes canvas capability once on first A2UI reachability failure (TTL-safe retry).

```bash
openclaw devices list
openclaw devices approve --latest
```

Run:

```bash
cd /home/digit/.openclaw/workspace/tmp/openclaw-src
pnpm android:test:integration
```

Optional overrides:

- `OPENCLAW_ANDROID_GATEWAY_URL=ws://...` (default: from your local OpenClaw config)
- `OPENCLAW_ANDROID_GATEWAY_TOKEN=...`
- `OPENCLAW_ANDROID_GATEWAY_PASSWORD=...`
- `OPENCLAW_ANDROID_NODE_ID=...` or `OPENCLAW_ANDROID_NODE_NAME=...`

What it does:

- Reads `node.describe` command list from the selected Android node.
- Invokes advertised non-interactive commands.
- Skips `screen.record` in this suite (Android requires interactive per-invocation screen-capture consent).
- Asserts command contracts (success or expected deterministic error for safe-invalid calls like `sms.send` and `notifications.actions`).

Common failure quick-fixes:

- `pairing required` before tests start:
  - approve pending device pairing (`openclaw devices approve --latest`) and rerun.
- `A2UI host not reachable` / `A2UI_HOST_NOT_CONFIGURED`:
  - ensure gateway canvas host is running and reachable, keep the app on the **Agents** tab. The app will auto-refresh canvas capability once; if it still fails, reconnect app and rerun.
- `NODE_BACKGROUND_UNAVAILABLE: canvas unavailable`:
  - app is not effectively ready for canvas commands; keep app foregrounded and **Agents** tab active.

## Contributions

This Android app is currently being rebuilt.
Maintainer: @obviyus. For issues/questions/contributions, please open an issue or reach out on Discord.
