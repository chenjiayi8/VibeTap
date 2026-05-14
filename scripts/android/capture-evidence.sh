#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

TARGET_SERIAL="${ANDROID_SERIAL:-}"
if [[ "${1:-}" == "--serial" ]]; then
  require_flag_value "--serial" "${2-}"
  TARGET_SERIAL="${2}"
  shift 2
fi

MODE="${1:-}"
NAME="${2:-}"
RECORD_SECONDS="${3:-15}"
OUTPUT_DIR="${PROJECT_ROOT}/captures/android"

if [[ -z "${MODE}" || -z "${NAME}" ]]; then
  cat <<EOF_HELP
Usage:
  scripts/android/capture-evidence.sh [--serial <serial>] screenshot <name>
  scripts/android/capture-evidence.sh [--serial <serial>] screenrecord <name> [seconds]

Environment:
  ANDROID_SERIAL=<serial>  Target a specific device when more than one is attached.
EOF_HELP
  exit 1
fi

case "${MODE}" in
  screenshot|screenrecord)
    ;;
  *)
    echo "Unknown mode: ${MODE}" >&2
    exit 1
    ;;
esac

mkdir -p "${OUTPUT_DIR}"
ADB=$(adb_bin)
declare -a ADB_TARGET=()
if [[ -n "${TARGET_SERIAL}" ]]; then
  ADB_TARGET=(-s "${TARGET_SERIAL}")
fi

case "${MODE}" in
  screenshot)
    OUTPUT_PATH="${OUTPUT_DIR}/${NAME}.png"
    "${ADB}" "${ADB_TARGET[@]}" exec-out screencap -p > "${OUTPUT_PATH}"
    echo "Saved screenshot to ${OUTPUT_PATH}"
    ;;
  screenrecord)
    REMOTE_PATH="/sdcard/Download/${NAME}.mp4"
    OUTPUT_PATH="${OUTPUT_DIR}/${NAME}.mp4"
    "${ADB}" "${ADB_TARGET[@]}" shell rm -f "${REMOTE_PATH}" >/dev/null 2>&1 || true
    "${ADB}" "${ADB_TARGET[@]}" shell screenrecord --time-limit "${RECORD_SECONDS}" "${REMOTE_PATH}"
    "${ADB}" "${ADB_TARGET[@]}" pull "${REMOTE_PATH}" "${OUTPUT_PATH}" >/dev/null
    echo "Saved recording to ${OUTPUT_PATH}"
    ;;
  *)
    echo "Unknown mode: ${MODE}" >&2
    exit 1
    ;;
esac
