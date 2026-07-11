#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGE_NAME="ai.openclaw.app.hud"
COMPONENT="$PACKAGE_NAME/ai.openclaw.app.MainActivity"
APK_PATH="${OPENCLAW_HUD_APK:-}"
SERIAL="${ANDROID_SERIAL:-}"
DISPLAY_ID="${OPENCLAW_HUD_DISPLAY_ID:-presentation}"
SETUP_CODE="${OPENCLAW_HUD_SETUP_CODE:-}"
AUTO_CONNECT="true"
SETUP_ACTION="ai.openclaw.app.action.SETUP_GATEWAY"
SETUP_EXTRA="setup_code"
AUTO_CONNECT_EXTRA="auto_connect"
HIDE_DEX_TASKBAR="${OPENCLAW_HUD_HIDE_DEX_TASKBAR:-true}"
# New task + clear task prevents stale DeX/freeform launches from reusing the previous HUD task.
PRESENTATION_ACTIVITY_FLAGS=(--display 0 --windowingMode 1 -f 0x10008000 --activity-reset-task-if-needed)

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
Usage: scripts/install-launch-hud.sh [--serial SERIAL] [--apk APK] [--display presentation|DISPLAY_ID|auto|default] [--setup-code CODE|--setup-code-file FILE|--setup-json FILE] [--no-auto-connect]

Installs the HUD flavor and launches OpenClaw HUD. The default presentation
mode starts the activity on the phone display and lets Android Presentation own
the external HUD display. Pass a numeric display id only to force a DeX/freeform
activity on that display.
USAGE
}

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
        --display)
            DISPLAY_ID="${2:?missing display id}"
            shift 2
            ;;
        --setup-code)
            SETUP_CODE="${2:?missing setup code}"
            shift 2
            ;;
        --setup-code-file)
            SETUP_CODE="$(tr -d '\r\n' < "${2:?missing setup code file}")"
            shift 2
            ;;
        --setup-json)
            SETUP_CODE="$(jq -r '.setupCode // empty' "${2:?missing setup json file}" | tr -d '\r\n')"
            if [[ -z "$SETUP_CODE" ]]; then
                echo "No setupCode found in setup JSON: $2" >&2
                exit 1
            fi
            shift 2
            ;;
        --no-auto-connect)
            AUTO_CONNECT="false"
            shift
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

if [[ -z "$APK_PATH" ]]; then
    APK_PATH="$(
        find "$ROOT_DIR/app/build/outputs/apk/hud/debug" -maxdepth 1 -name '*-hud-debug.apk' -type f -printf '%T@ %p\n' 2>/dev/null |
            sort -nr |
            awk 'NR == 1 { print substr($0, index($0, $2)) }'
    )"
fi

if [[ ! -f "$APK_PATH" ]]; then
    echo "HUD APK not found: $APK_PATH" >&2
    echo "Run: ANDROID_HOME=/home/digit/android-sdk ANDROID_SDK_ROOT=/home/digit/android-sdk ./gradlew :app:assembleHudDebug" >&2
    echo "Or pass --apk /path/to/*-hud-debug.apk." >&2
    exit 1
fi

if [[ -z "$SERIAL" ]]; then
    SERIAL="$("$ADB_BIN" devices | awk 'NR > 1 && $2 == "device" { print $1; exit }')"
fi

if [[ -z "$SERIAL" ]]; then
    echo "No authorized ADB device found. Set ANDROID_SERIAL or pass --serial." >&2
    "$ADB_BIN" devices -l >&2 || true
    exit 1
fi

ADB_APK_PATH="$APK_PATH"
if [[ "$ADB_BIN" == *.exe && -x "$(command -v wslpath)" ]]; then
    ADB_APK_PATH="$(wslpath -w "$APK_PATH")"
fi

hide_dex_taskbar() {
    if [[ "$HIDE_DEX_TASKBAR" != "true" ]]; then
        return
    fi
    echo "Hiding DeX/taskbar chrome for HUD launch"
    "$ADB_BIN" -s "$SERIAL" shell settings put global task_bar 0 || true
    "$ADB_BIN" -s "$SERIAL" shell settings put global taskbar_recent_apps_enabled 0 || true
    current_policy="$("$ADB_BIN" -s "$SERIAL" shell settings get global policy_control 2>/dev/null | tr -d '\r' || true)"
    if [[ -n "$current_policy" && "$current_policy" != "null" ]]; then
        updated_policy=""
        IFS=',' read -ra policy_entries <<< "$current_policy"
        for policy_entry in "${policy_entries[@]}"; do
            if [[ "$policy_entry" == "immersive.full=$PACKAGE_NAME" || -z "$policy_entry" ]]; then
                continue
            fi
            updated_policy="${updated_policy:+$updated_policy,}$policy_entry"
        done
        if [[ -n "$updated_policy" ]]; then
            "$ADB_BIN" -s "$SERIAL" shell settings put global policy_control "$updated_policy" || true
        else
            "$ADB_BIN" -s "$SERIAL" shell settings delete global policy_control >/dev/null 2>&1 || true
        fi
    fi
    "$ADB_BIN" -s "$SERIAL" shell cmd statusbar collapse || true
}

start_hud_activity() {
    local display_mode="$1"
    shift
    local start_args=("$@")
    if [[ -n "$SETUP_CODE" ]]; then
        "$ADB_BIN" -s "$SERIAL" shell am start "${start_args[@]}" \
            -a "$SETUP_ACTION" \
            -n "$COMPONENT" \
            --es "$SETUP_EXTRA" "$SETUP_CODE" \
            --ez "$AUTO_CONNECT_EXTRA" "$AUTO_CONNECT"
    else
        "$ADB_BIN" -s "$SERIAL" shell am start "${start_args[@]}" -n "$COMPONENT"
    fi
    echo "Started HUD activity using $display_mode launch args: ${start_args[*]}"
}

echo "Using ADB: $ADB_BIN"
echo "Using device: $SERIAL"
echo "Installing: $APK_PATH"
"$ADB_BIN" -s "$SERIAL" install -r "$ADB_APK_PATH"
"$ADB_BIN" -s "$SERIAL" shell am force-stop "$PACKAGE_NAME" || true
hide_dex_taskbar

resolved_display="$DISPLAY_ID"
if [[ "$DISPLAY_ID" == "auto" ]]; then
    resolved_display="presentation"
fi

if [[ "$resolved_display" == "presentation" ]]; then
    presentation_display="$(
        "$ADB_BIN" -s "$SERIAL" shell dumpsys display |
            tr -d '\r' |
            sed -n '/mBaseDisplayInfo=DisplayInfo.*type EXTERNAL/s/.*displayId \([0-9][0-9]*\).*/\1/p' |
            awk '$1 != 0 { print; exit }'
    )"
    if [[ -n "$presentation_display" ]]; then
        "$ADB_BIN" -s "$SERIAL" shell settings put global external_display_audio_output 1 || true
        echo "Launching presentation HUD for external display $presentation_display"
    else
        echo "No external display found; launching on default display"
    fi
    resolved_display="default"
elif [[ "$resolved_display" == "external" ]]; then
    resolved_display="$(
        "$ADB_BIN" -s "$SERIAL" shell dumpsys display |
            tr -d '\r' |
            sed -n '/mBaseDisplayInfo=DisplayInfo.*type EXTERNAL/s/.*displayId \([0-9][0-9]*\).*/\1/p' |
            awk '$1 != 0 { print; exit }'
    )"
    if [[ -z "$resolved_display" ]]; then
        resolved_display="default"
    fi
fi

if [[ "$resolved_display" == "default" ]]; then
    echo "Launching presentation HUD on default phone display"
    start_hud_activity "presentation/default" "${PRESENTATION_ACTIVITY_FLAGS[@]}"
else
    echo "Launching HUD on display $resolved_display"
    "$ADB_BIN" -s "$SERIAL" shell settings put global external_display_audio_output 1 || true
    start_hud_activity "forced-display" --display "$resolved_display"
fi

"$ADB_BIN" -s "$SERIAL" shell dumpsys package "$PACKAGE_NAME" |
    tr -d '\r' |
    grep -E 'versionName|firstInstallTime|lastUpdateTime' || true
