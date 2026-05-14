#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

CHECKLIST_PATH="${PROJECT_ROOT}/.codex/docs/plans/2026-05-14-vibetap-emulator-parity-checklist.md"
SERIAL="${1:-${ANDROID_SERIAL:-}}"

ADB=$(adb_bin)
require_executable "${ADB}"
mapfile -t PHYSICAL_SERIALS < <("${ADB}" devices | awk '/\tdevice$/ && $1 !~ /^emulator-/ { print $1 }')

if [[ -z "${SERIAL}" ]]; then
  if (( ${#PHYSICAL_SERIALS[@]} == 0 )); then
    echo "No physical Android device detected. Connect the USB tablet and enable debugging." >&2
    exit 1
  fi

  if (( ${#PHYSICAL_SERIALS[@]} > 1 )); then
    echo "Multiple physical Android devices detected (${PHYSICAL_SERIALS[*]}). Re-run with an explicit serial: bash scripts/android/tablet-parity.sh <serial>" >&2
    exit 1
  fi

  SERIAL="${PHYSICAL_SERIALS[0]}"
fi

[[ -n "${SERIAL}" ]] || {
  echo "No physical Android device detected. Connect the USB tablet and enable debugging." >&2
  exit 1
}

if [[ "${SERIAL}" == emulator-* ]]; then
  echo "Tablet parity requires a physical Android device serial, not emulator serial ${SERIAL}." >&2
  exit 1
fi

FOUND_PHYSICAL_SERIAL=0
for physical_serial in "${PHYSICAL_SERIALS[@]}"; do
  if [[ "${physical_serial}" == "${SERIAL}" ]]; then
    FOUND_PHYSICAL_SERIAL=1
    break
  fi
done

if (( FOUND_PHYSICAL_SERIAL == 0 )); then
  echo "Selected serial ${SERIAL} is not a currently connected physical Android device. Connect the USB tablet and enable debugging, then retry." >&2
  exit 1
fi

command -v scrcpy >/dev/null || {
  echo "scrcpy is required for tablet parity. Install it and retry." >&2
  exit 1
}

echo "Building current debug APK..."
gradlew_cmd :app:assembleDebug
echo "Installing current debug APK on ${SERIAL}..."
"${ADB}" -s "${SERIAL}" install -r "${PROJECT_ROOT}/app/build/outputs/apk/debug/app-debug.apk"
unlock_device "${SERIAL}"
launch_main_activity "${SERIAL}"

echo "Using tablet: ${SERIAL}"
echo "Checklist: ${CHECKLIST_PATH}"
echo "Opened the current debug build on ${SERIAL}; continuing with scrcpy for parity checks."
exec scrcpy --serial "${SERIAL}" --always-on-top --window-title "VibeTap Tablet Parity"
