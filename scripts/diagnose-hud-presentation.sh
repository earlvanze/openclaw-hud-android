#!/usr/bin/env bash
set -euo pipefail

PACKAGE_NAME="${OPENCLAW_HUD_PACKAGE:-ai.openclaw.app.hud}"
SERIAL="${ANDROID_SERIAL:-}"

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
Usage: scripts/diagnose-hud-presentation.sh [--serial SERIAL] [--package PACKAGE]

Prints a compact ADB report for HUD presentation launches: display topology,
task/window placement, DeX/taskbar settings, Samsung external-display audio
settings, audio device hints, and recent HUD logs.
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --serial)
            SERIAL="${2:?missing serial}"
            shift 2
            ;;
        --package)
            PACKAGE_NAME="${2:?missing package}"
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

section() {
    printf '\n== %s ==\n' "$1"
}

adb_shell() {
    "$ADB_BIN" -s "$SERIAL" shell "$@" 2>/dev/null | tr -d '\r' || true
}

section "Device"
echo "ADB: $ADB_BIN"
echo "Serial: $SERIAL"
adb_shell getprop ro.product.manufacturer
adb_shell getprop ro.product.model
adb_shell getprop ro.build.version.release

section "Package"
adb_shell dumpsys package "$PACKAGE_NAME" |
    grep -E 'versionName|versionCode|firstInstallTime|lastUpdateTime' || true

section "Samsung/DeX Settings"
for key in task_bar taskbar_recent_apps_enabled policy_control external_display_audio_output; do
    value="$(adb_shell settings get global "$key" | tail -n 1)"
    printf '%s=%s\n' "$key" "${value:-unknown}"
done

section "Displays"
adb_shell dumpsys display |
    grep -E 'DisplayDeviceInfo|mBaseDisplayInfo|mDisplayId|displayId|type EXTERNAL|type INTERNAL|uniqueId|name=' |
    sed -n '1,120p' || true

section "HUD Activity/Task Placement"
adb_shell dumpsys activity activities |
    grep -E "$PACKAGE_NAME|mResumedActivity|topResumedActivity|displayId|windowingMode|RootTask|Task\\{" |
    sed -n '1,160p' || true

section "HUD Windows"
adb_shell dumpsys window windows |
    grep -E "$PACKAGE_NAME|mCurrentFocus|mFocusedApp|DisplayId|displayId|Window\\{" |
    sed -n '1,160p' || true

section "Audio Routing Hints"
adb_shell dumpsys audio |
    grep -Ei 'AirVision|usb|speaker|external|communication|mCommunicationDevice|DEVICE_OUT|preferred device' |
    sed -n '1,160p' || true

section "Recent HUD Logs"
"$ADB_BIN" -s "$SERIAL" logcat -d -t 300 \
    -s MainActivity HudPresentation AirVisionAudioRouter AirVisionUsbController 2>/dev/null |
    tr -d '\r' |
    sed -n '1,160p' || true
