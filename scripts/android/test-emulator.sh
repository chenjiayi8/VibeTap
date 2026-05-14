#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

require_android_tools
bash "${SCRIPT_DIR}/setup-emulator.sh" --check

serial_file=$(mktemp)
trap 'rm -f "${serial_file}"' EXIT

VIBETAP_EMULATOR_SERIAL_FILE="${serial_file}" bash "${SCRIPT_DIR}/start-emulator.sh"
TARGET_SERIAL=$(<"${serial_file}")
[[ -n "${TARGET_SERIAL}" ]] || {
  echo "start-emulator.sh did not expose an emulator serial." >&2
  exit 1
}

ADB=$(adb_bin)

ANDROID_SERIAL="${TARGET_SERIAL}" gradlew_cmd :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest
"${ADB}" -s "${TARGET_SERIAL}" install -r "${PROJECT_ROOT}/app/build/outputs/apk/debug/app-debug.apk"
launch_main_activity "${TARGET_SERIAL}"

echo "Emulator verification complete."
echo "Next: use scripts/android/capture-evidence.sh screenshot <name> for artifacts."
