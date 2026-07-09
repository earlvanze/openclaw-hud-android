#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGE_NAME="ai.openclaw.app.hud"
COMPONENT="$PACKAGE_NAME/ai.openclaw.app.MainActivity"
APK_PATH="${OPENCLAW_HUD_APK:-}"
OUT_DIR="$ROOT_DIR/play/screenshots/phone"
SERIAL="${ANDROID_SERIAL:-}"
INSTALL_APK="true"
WAIT_SECONDS="3"
SCREENSHOT_DISPLAY_ID="${OPENCLAW_PLAY_SCREENSHOT_DISPLAY_ID:-}"
ACTIVITY_DISPLAY_ID="${OPENCLAW_PLAY_ACTIVITY_DISPLAY_ID:-0}"
RESTORE_SCREEN_OFF_POCKET=""
ACTION_PLAY_REVIEW_DEMO="ai.openclaw.app.action.PLAY_REVIEW_DEMO"
DESTINATION_EXTRA="destination"

WINDOWS_ADB="/mnt/c/Users/digit/AppData/Local/Microsoft/WinGet/Packages/Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe/platform-tools/adb.exe"
if [[ -n "${ADB:-}" ]]; then
    ADB_BIN="$ADB"
elif [[ -x "$WINDOWS_ADB" ]]; then
    ADB_BIN="$WINDOWS_ADB"
else
    ADB_BIN="adb"
fi

usage() {
    cat <<'USAGE'
Usage: scripts/capture-play-screenshots.sh [--serial SERIAL] [--apk APK] [--out DIR] [--skip-install] [--wait SECONDS] [--display-id DISPLAY_ID] [--activity-display DISPLAY_ID]

Captures deterministic Google Play phone screenshots from the HUD build by
launching OpenClaw HUD in review/demo mode. The script captures:

  1. hud-demo.png      Green-on-black minimal HUD demo.
  2. settings-demo.png Settings/App Preferences demo state.

The M1 display is not required; screenshots are captured from the phone display.
Captured screenshots are normalized to 24-bit PNG without alpha for Google Play.
On foldables, the script passes an explicit screencap display id to avoid
Samsung's multiple-display warning from corrupting PNG stdout.
On Samsung devices, the script temporarily disables screen_off_pocket while it
captures screenshots and restores the original value before exit.
USAGE
}

cleanup() {
    if [[ -n "$RESTORE_SCREEN_OFF_POCKET" ]]; then
        "$ADB_BIN" -s "$SERIAL" shell settings put system screen_off_pocket "$RESTORE_SCREEN_OFF_POCKET" >/dev/null 2>&1 || true
    fi
}

trap cleanup EXIT

while [[ $# -gt 0 ]]; do
    case "$1" in
        --serial)
            SERIAL="${2:?missing serial}"
            shift 2
            ;;
        --apk)
            APK_PATH="${2:?missing apk path}"
            shift 2
            ;;
        --out)
            OUT_DIR="${2:?missing output directory}"
            shift 2
            ;;
        --skip-install)
            INSTALL_APK="false"
            shift
            ;;
        --wait)
            WAIT_SECONDS="${2:?missing seconds}"
            shift 2
            ;;
        --display-id)
            SCREENSHOT_DISPLAY_ID="${2:?missing display id}"
            shift 2
            ;;
        --activity-display)
            ACTIVITY_DISPLAY_ID="${2:?missing display id}"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

if [[ -z "$SERIAL" ]]; then
    SERIAL="$("$ADB_BIN" devices | awk 'NR > 1 && $2 == "device" { print $1; exit }')"
fi

if [[ -z "$SERIAL" ]]; then
    echo "No authorized ADB device found. Set ANDROID_SERIAL or pass --serial." >&2
    "$ADB_BIN" devices -l >&2 || true
    exit 1
fi

restore_value="$("$ADB_BIN" -s "$SERIAL" shell settings get system screen_off_pocket 2>/dev/null | tr -d '\r' || true)"
if [[ "$restore_value" != "null" && -n "$restore_value" ]]; then
    RESTORE_SCREEN_OFF_POCKET="$restore_value"
    "$ADB_BIN" -s "$SERIAL" shell settings put system screen_off_pocket 0 >/dev/null 2>&1 || true
fi

if [[ -z "$SCREENSHOT_DISPLAY_ID" ]]; then
    screencap_help="$("$ADB_BIN" -s "$SERIAL" shell screencap -h 2>&1 || true)"
    SCREENSHOT_DISPLAY_ID="$(
        printf '%s\n' "$screencap_help" |
            sed -nE 's/.*defaults to ([0-9]+).*/\1/p' |
            head -n 1 |
            tr -d '\r'
    )"
fi

if [[ "$INSTALL_APK" == "true" && -z "$APK_PATH" ]]; then
    APK_PATH="$(
        find "$ROOT_DIR/app/build/outputs/apk/hud/debug" -maxdepth 1 -name '*-hud-debug.apk' -type f -printf '%T@ %p\n' 2>/dev/null |
            sort -nr |
            awk 'NR == 1 { print substr($0, index($0, $2)) }'
    )"
fi

if [[ "$INSTALL_APK" == "true" && ! -f "$APK_PATH" ]]; then
    echo "HUD APK not found." >&2
    echo "Run: ANDROID_HOME=/home/digit/android-sdk ANDROID_SDK_ROOT=/home/digit/android-sdk ./gradlew :app:assembleHudDebug" >&2
    echo "Or pass --apk /path/to/*-hud-debug.apk." >&2
    exit 1
fi

ADB_APK_PATH="$APK_PATH"
if [[ -n "$ADB_APK_PATH" && "$ADB_BIN" == *.exe && -x "$(command -v wslpath)" ]]; then
    ADB_APK_PATH="$(wslpath -w "$APK_PATH")"
fi

mkdir -p "$OUT_DIR"
MANIFEST_PATH="$OUT_DIR/manifest.json"

if [[ "$INSTALL_APK" == "true" ]]; then
    echo "Installing HUD APK on $SERIAL"
    "$ADB_BIN" -s "$SERIAL" install -r "$ADB_APK_PATH"
fi

capture_destination() {
    local destination="$1"
    local output_name="$2"
    local output_path="$OUT_DIR/$output_name"
    local raw_path
    raw_path="$(mktemp --suffix=.png)"
    trap 'rm -f "$raw_path"' RETURN

    echo "Launching demo destination: $destination"
    "$ADB_BIN" -s "$SERIAL" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
    "$ADB_BIN" -s "$SERIAL" shell wm dismiss-keyguard >/dev/null 2>&1 || true
    "$ADB_BIN" -s "$SERIAL" shell cmd statusbar collapse >/dev/null 2>&1 || true
    "$ADB_BIN" -s "$SERIAL" shell input swipe 540 2200 540 600 500 >/dev/null 2>&1 || true
    "$ADB_BIN" -s "$SERIAL" shell am force-stop "$PACKAGE_NAME" || true
    if ! "$ADB_BIN" -s "$SERIAL" shell am start \
        --display "$ACTIVITY_DISPLAY_ID" \
        -a "$ACTION_PLAY_REVIEW_DEMO" \
        -n "$COMPONENT" \
        --es "$DESTINATION_EXTRA" "$destination" >/dev/null; then
        if [[ "$ACTIVITY_DISPLAY_ID" != "0" ]]; then
            echo "Launch on display $ACTIVITY_DISPLAY_ID failed; retrying display 0." >&2
            "$ADB_BIN" -s "$SERIAL" shell am start \
                --display 0 \
                -a "$ACTION_PLAY_REVIEW_DEMO" \
                -n "$COMPONENT" \
                --es "$DESTINATION_EXTRA" "$destination" >/dev/null
        else
            exit 1
        fi
    fi
    sleep "$WAIT_SECONDS"
    if [[ -n "$SCREENSHOT_DISPLAY_ID" ]]; then
        "$ADB_BIN" -s "$SERIAL" exec-out screencap -p -d "$SCREENSHOT_DISPLAY_ID" > "$raw_path"
    else
        "$ADB_BIN" -s "$SERIAL" exec-out screencap -p > "$raw_path"
    fi
    node "$ROOT_DIR/scripts/convert-play-screenshot.mjs" "$raw_path" "$output_path"
    if [[ ! -s "$output_path" ]]; then
        echo "Screenshot is empty: $output_path" >&2
        exit 1
    fi
    echo "Captured: $output_path"
}

capture_destination "hud" "hud-demo.png"
capture_destination "settings" "settings-demo.png"

cat > "$MANIFEST_PATH" <<EOF
{
  "schema": "openclaw.play.screenshots",
  "version": 1,
  "deviceType": "phone",
  "screenshots": [
    "play/screenshots/phone/hud-demo.png",
    "play/screenshots/phone/settings-demo.png"
  ]
}
EOF

cat <<EOF

Play screenshots captured:
- $OUT_DIR/hud-demo.png
- $OUT_DIR/settings-demo.png
- $MANIFEST_PATH

After uploading these in Play Console, record their paths/names in:
  play/app-content-answers.json -> finalSubmission.phoneScreenshots

Local final verifier snippet:
  "phoneScreenshots": [
    "play/screenshots/phone/hud-demo.png",
    "play/screenshots/phone/settings-demo.png"
  ]
EOF
