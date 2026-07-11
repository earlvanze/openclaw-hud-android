#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGE_NAME="ai.openclaw.app"
COMPONENT="$PACKAGE_NAME/ai.openclaw.app.MainActivity"
FLAVOR="${OPENCLAW_NODE_FLAVOR:-thirdParty}"
APK_PATH="${OPENCLAW_NODE_APK:-}"
SERIAL="${ANDROID_SERIAL:-}"
SETUP_CODE="${OPENCLAW_NODE_SETUP_CODE:-}"
AUTO_CONNECT="true"
SETUP_ACTION="ai.openclaw.app.action.SETUP_GATEWAY"
SETUP_EXTRA="setup_code"
AUTO_CONNECT_EXTRA="auto_connect"

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
Usage: scripts/install-launch-node.sh [--serial SERIAL] [--apk APK] [--flavor play|thirdParty|third-party] [--setup-code CODE|--setup-code-file FILE|--setup-json FILE] [--no-auto-connect]

Installs and launches OpenClaw Node on the primary Android display. The default
flavor is thirdParty, which keeps the full sideload permission set. Use
--flavor play for the Play-safe Node build without SMS or Call Log permissions.
USAGE
}

normalize_flavor() {
    case "$1" in
        play)
            printf 'play\n'
            ;;
        thirdParty|third-party|thirdparty)
            printf 'thirdParty\n'
            ;;
        *)
            echo "Unsupported Node flavor: $1" >&2
            echo "Expected play or thirdParty." >&2
            exit 2
            ;;
    esac
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
        --flavor)
            FLAVOR="${2:?missing flavor}"
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

FLAVOR="$(normalize_flavor "$FLAVOR")"

if [[ -z "$APK_PATH" ]]; then
    APK_PATH="$(
        find "$ROOT_DIR/app/build/outputs/apk/$FLAVOR/debug" -maxdepth 1 -name "*-$FLAVOR-debug.apk" -type f -printf '%T@ %p\n' 2>/dev/null |
            sort -nr |
            awk 'NR == 1 { print substr($0, index($0, $2)) }'
    )"
fi

if [[ ! -f "$APK_PATH" ]]; then
    echo "Node APK not found: $APK_PATH" >&2
    echo "Run: ANDROID_HOME=/home/digit/android-sdk ANDROID_SDK_ROOT=/home/digit/android-sdk ./gradlew :app:assemble${FLAVOR^}Debug" >&2
    echo "Or pass --apk /path/to/*-$FLAVOR-debug.apk." >&2
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

start_node_activity() {
    if [[ -n "$SETUP_CODE" ]]; then
        "$ADB_BIN" -s "$SERIAL" shell am start \
            -a "$SETUP_ACTION" \
            -n "$COMPONENT" \
            --es "$SETUP_EXTRA" "$SETUP_CODE" \
            --ez "$AUTO_CONNECT_EXTRA" "$AUTO_CONNECT"
    else
        "$ADB_BIN" -s "$SERIAL" shell am start -n "$COMPONENT"
    fi
}

echo "Using ADB: $ADB_BIN"
echo "Using device: $SERIAL"
echo "Using flavor: $FLAVOR"
echo "Installing: $APK_PATH"
"$ADB_BIN" -s "$SERIAL" install -r "$ADB_APK_PATH"
"$ADB_BIN" -s "$SERIAL" shell am force-stop "$PACKAGE_NAME" || true
start_node_activity
echo "Started Node activity on the primary display"

"$ADB_BIN" -s "$SERIAL" shell dumpsys package "$PACKAGE_NAME" |
    tr -d '\r' |
    grep -E 'versionName|firstInstallTime|lastUpdateTime' || true
