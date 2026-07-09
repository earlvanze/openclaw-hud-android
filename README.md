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
  safe area, Splendid mode, brightness, blue-light filter, distance, IPD,
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
./gradlew :app:installPlayDebug
./gradlew :app:testPlayDebugUnitTest
```

Third-party debug flavor:

```bash
./gradlew :app:assembleThirdPartyDebug
./gradlew :app:installThirdPartyDebug
./gradlew :app:testThirdPartyDebugUnitTest
```

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
into Samsung DeX/freeform mode by default.

Display modes:

- `--display presentation` or `--display auto`: current default; phone Activity
  on display 0, external display via Android Presentation.
- `--display default`: phone display only.
- `--display external`: find the first external display and launch the Activity
  there, using the older DeX/freeform path.
- `--display <id>`: force a specific Android display id.

The launch script also attempts non-destructive DeX/taskbar suppression and sets
Samsung external-display audio output when an external display is present.

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
- Tap `cc` in the HUD status lights to toggle Samsung/native captions.
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
| Working / Gaming / Infinity / Custom modes | Implemented as saved HUD profile slots with mode-specific defaults, user-named Custom 1 / Custom 2 labels, copy-current-to-custom actions, JSON profile backup/import, active-profile reset, HUD placement, physical main screen behavior, safe area, scale/layout, brightness, distance, IPD, Splendid, Eye Care, Motion Sync, and Light Load values. |
| Brightness | Implemented as software HUD dimming. Hardware brightness remains available from the M1 touch bar. |
| Screen distance | Implemented as virtual HUD distance scaling. |
| IPD | Stored as a calibration value, defaulting to 67 mm. Adjustment is locked while Light Load Mode is enabled, matching the ASUS app. Firmware-level apply still needs the ASUS HID protocol. |
| Splendid Standard / Theater / Office / Game / Eye Care | Stored as profile settings. Eye Care adds a warm HUD overlay now; true panel presets need HID support. |
| Blue Light Filter | Implemented as Android HUD warm filtering and available only in Eye Care mode, matching the ASUS app. |
| Motion Sync | Stored in the AirVision profile; hardware apply needs HID support. |
| 3D Mode | Stored in the AirVision profile and disabled while Light Load Mode is enabled. Hardware apply needs HID support. |
| Light Load Mode | Stored in the AirVision profile for low-overhead HUD operation and locks IPD adjustment while enabled. |
| Gesture & Hotkey Settings | Implemented for HUD touch actions, swipe-to-scroll, brightness-key scroll, Android HUD brightness, or virtual-distance handling, and M1 media/tap key double-tap mic behavior. |
| App Preferences | Implemented with startup view, AirVision companion language preference, demo mode, JSON profile backup/import, software version/build display, EULA note, in-app privacy policy, official FAQ/tutorial, product registration, and ASUS support links. |
| Device Information | Implemented Android USB identity details for manufacturer, product, USB ID, device path, serial availability, and firmware protocol status. Actual firmware version still needs ASUS HID support. |
| Firmware link | Implemented USB detection for the known AirVision M1 device (`0x0b05:0x1b3c`), Android USB permission, HID/audio/input interface status, derived HID report-path capability summary, USB interface/endpoint diagnostics for protocol capture work, and a JSON diagnostics export that omits raw USB serial values. |
| Identify | Implemented as a temporary `HUD 1` marker on the Android Presentation display. |
| Multi-screen desktop layouts | Android cannot own DeX topology like the Windows app, but the HUD now supports configurable external-display targeting, per-mode placement, per-mode physical main screen visibility, and safe-area layout profiles for the M1 presentation. |

Firmware controls are intentionally read-only/status-only for now. The app can
detect the M1 USB HID control interface, request Android USB permission, and
display USB device identity plus interface/endpoint descriptors in Settings for
future ASUS protocol capture work. Firmware Link can also export an AirVision
diagnostics JSON snapshot containing current USB readiness, descriptor summaries,
derived HID report-path capabilities, the active HUD profile, gesture settings,
and AirVision app preferences without gateway endpoints, auth tokens, chat
history, or raw USB serial values. The app does not send ASUS vendor reports
until the Windows app protocol is captured and validated.

Captions default to Samsung/Android native captioning so the system floating
caption window can sit over the minimal HUD. The Voice tab exposes the provider
selector: Samsung, OpenClaw, or Off. The OpenClaw fallback is intended for
low-latency walking use when native captions are not available or when captions
need OpenClaw session context. It forces thinking `off`, prefers the
`sage-router/fast` model profile, suppresses spoken assistant replies while
captions are active, and labels alternating turns as `S1` / `S2`. The default
OpenClaw target language is Spanish. Source and target language selection live
in the Voice tab and currently support Auto, English, Spanish, French, German,
Italian, Portuguese, Japanese, Korean, and Chinese.

Notification text is whitespace-cleaned and secret-shaped assignments such as
`token=...`, `password=...`, and `accountSignatureKey=...` are redacted before
being rendered in the HUD.

AirVision startup view and demo mode live in Settings -> AirVision M1 -> App
Preferences. Startup view selects the default launch tab for HUD builds. Demo
mode renders a deterministic navigation, chat, mic, speaker, and caption sample
in the HUD so tutorials, screenshots, and fit checks can be performed without a
live gateway or notification stream.

HUD Display Target lives in Settings -> AirVision M1. AirVision Preferred keeps
the default behavior of selecting displays named like ASUS AirVision M1. Largest
External, First External, and Last External provide manual fallbacks for Samsung
DeX or adapter setups where Android exposes multiple presentation displays.

Custom profile names and copy-current-to-custom actions live under Viewing Mode
in Settings -> AirVision M1. The labels rename the saved Custom 1 and Custom 2
profile slots without changing their stored display settings; the copy actions
duplicate the active profile settings into a custom slot.

Profile Backup lives under Settings -> AirVision M1 -> App Preferences. Export
writes a JSON file containing AirVision M1 tuning profiles, custom labels,
gesture/hotkey settings, display targeting, startup view, language preference,
and demo mode. Import validates the same AirVision-only schema and never
includes gateway endpoints, auth tokens, accounts, chat history, or other
OpenClaw runtime state.

To pair on first launch, pass a setup code directly or from a local file:

```bash
./scripts/install-launch-hud.sh --serial <adb-serial> --setup-json /tmp/openclaw-android-pair/qr-tailnet-wss.json
```

The setup launch stores the gateway endpoint/auth, marks onboarding complete for
the HUD package, and starts connecting immediately.

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

Google Play internal-track publishing helper:

```bash
node scripts/verify-play-hud-release.mjs
node scripts/verify-play-submission-package.mjs
node scripts/verify-play-submission-package.mjs --final
node scripts/publish-play-internal.mjs --dry-run
node scripts/publish-play-internal.mjs --auth gcloud --preflight
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON=/path/to/service-account.json \
  node scripts/publish-play-internal.mjs --commit
node scripts/publish-play-internal.mjs --auth gcloud --commit
```

The publish helper defaults to package `ai.openclaw.app.hud`, track `internal`,
release status `draft`, and the newest
`build/release-bundles/*-hud-release.aab`. It validates the English Play
listing files, creates a Play edit, uploads the AAB, patches the localized
store listing, attaches localized release notes to the internal-track release,
and commits only when `--commit` is supplied. Commit mode refuses to create a
Play edit or upload a bundle until `node scripts/verify-play-submission-package.mjs --final`
passes, so screenshots, hosted privacy URL, tester setup, reviewer access, and
app creation status must be recorded locally first. Auth can come from a Play
Console service-account JSON file or the active `gcloud auth print-access-token`
account. For `--auth gcloud`, the helper restricts publishing to
`earlvanze@gmail.com` or `earl@earlbnb.com` by default so an accidental active
Cloud account cannot publish the app; run `gcloud config set account <email>`
before `--preflight` or `--commit`. Set `GOOGLE_PLAY_ALLOWED_ACCOUNTS` or pass
`--allowed-account` only when a different authorized Play publisher should be
accepted. Store listing and policy prep files live under `play/`. Use
`--skip-listing` or `--skip-release-notes` for bundle-only edits. The Play
Console app must already exist for `ai.openclaw.app.hud`; follow
`play/console-checklist.md` before the first commit.

Play phone screenshots can be captured from the HUD build without a live M1 by
using the app's review/demo launch action:

```bash
ANDROID_HOME=/home/digit/android-sdk ANDROID_SDK_ROOT=/home/digit/android-sdk \
  ./gradlew :app:assembleHudDebug
./scripts/capture-play-screenshots.sh --serial <adb-serial>
node scripts/test-play-screenshot-tools.mjs
```

The script installs the HUD debug APK, marks the app ready for review/demo mode,
launches deterministic HUD and Settings states on the phone display, normalizes
Android screencaps to 24-bit PNG without alpha, and writes
`play/screenshots/phone/hud-demo.png`,
`play/screenshots/phone/settings-demo.png`, plus a generated
`play/screenshots/phone/manifest.json`. After uploading screenshots in Play
Console, record their local paths, names, or URLs in
`play/app-content-answers.json` `finalSubmission.phoneScreenshots`.
`test-play-screenshot-tools.mjs` is the offline regression gate for this path:
it converts a synthetic alpha PNG, verifies the converted asset passes the final
submission checker, and verifies the checker rejects unconverted alpha PNGs.

`verify-play-hud-release.mjs` is the offline-safe local gate for the HUD/M1
Play target. It verifies the newest signed HUD AAB, confirms the generated
manifest package is `ai.openclaw.app.hud`, fails if restricted Play-risk
permissions drift back into the HUD flavor, confirms the notification-listener
service declaration, and checks the English listing/release-notes length limits.
GitHub Actions also builds the HUD release bundle with
`-POPENCLAW_ANDROID_ALLOW_UNSIGNED_RELEASE=true` and runs the verifier with
`--skip-signature` so CI can validate the release manifest and listing without
repo-stored signing keys. Publishing still requires the locally signed bundle
from `scripts/build-release-aab.mjs`.

`verify-play-submission-package.mjs` checks the local Play App content packet in
`play/app-content-answers.json` against the generated HUD manifest, privacy
policy, in-app privacy policy source, data-safety notes, console checklist, and
English listing files. It is a drift guard for Play Console form prep; the
final hosted privacy-policy URL, screenshots, tester access, and any reviewer
setup codes still belong in Play Console.

Use `node scripts/verify-play-submission-package.mjs --final` only when the
external Play Console artifacts have been filled into
`play/app-content-answers.json` under `finalSubmission`. The normal verifier is
the local draft gate; `--final` is expected to fail until the Play app exists,
the hosted privacy policy URL is public, at least two phone screenshots are
recorded, internal testers are configured, and reviewer access instructions have
been entered in Play Console. When screenshot entries are local paths, `--final`
also validates that the files are JPEG or 24-bit PNG without alpha, no larger
than 8 MiB, at least 320 px on the shortest side, no larger than 3840 px on the
longest side, and no more than 2:1 in either orientation. The publish helper
runs this final verifier automatically in `--commit` mode before contacting the
Google Play edit/upload API.

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
HUD package is marked onboarded and starts connecting immediately.

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
