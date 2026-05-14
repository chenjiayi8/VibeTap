#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd -- "${SCRIPT_DIR}/../.." && pwd)
APP_ID="com.frank.voiceoverlay"
MAIN_ACTIVITY="${APP_ID}/.MainActivity"
DEFAULT_AVD_NAME="${VIBETAP_AVD_NAME:-vibetap-api-35}"
DEFAULT_SYSTEM_IMAGE="${VIBETAP_SYSTEM_IMAGE:-system-images;android-35;google_apis;x86_64}"
DEFAULT_PLATFORM="${VIBETAP_ANDROID_PLATFORM:-android-35}"
DEFAULT_BUILD_TOOLS="${VIBETAP_BUILD_TOOLS:-35.0.0}"

require_python3() {
  command -v python3 >/dev/null || {
    echo "python3 is required for local.properties parsing." >&2
    return 127
  }
}

read_local_properties_sdk_dir() {
  local properties_file="${PROJECT_ROOT}/local.properties"
  [[ -f "${properties_file}" ]] || return 1

  require_python3 || return $?

  python3 - <<'PY' "${properties_file}"
import sys
from pathlib import Path

for line in Path(sys.argv[1]).read_text().splitlines():
    if line.startswith("sdk.dir="):
        print(line.split("=", 1)[1].replace("\\\\", "\\").replace("\\:", ":"))
        break
PY
}

should_use_local_properties_sdk_dir() {
  [[ -z "${ANDROID_SDK_ROOT:-}" && -z "${ANDROID_HOME:-}" && -f "${PROJECT_ROOT}/local.properties" ]]
}

resolve_android_sdk_root() {
  if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
    printf '%s\n' "${ANDROID_SDK_ROOT}"
    return 0
  fi

  if [[ -n "${ANDROID_HOME:-}" ]]; then
    printf '%s\n' "${ANDROID_HOME}"
    return 0
  fi

  should_use_local_properties_sdk_dir || return 1
  read_local_properties_sdk_dir
}

require_android_sdk_root() {
  local sdk_root resolve_status

  set +e
  sdk_root=$(resolve_android_sdk_root)
  resolve_status=$?
  set -e

  if [[ "${resolve_status}" -eq 0 ]]; then
    if [[ -z "${sdk_root}" ]]; then
      echo "Missing Android SDK. Set ANDROID_SDK_ROOT/ANDROID_HOME or add sdk.dir to local.properties." >&2
      exit 1
    fi

    printf '%s\n' "${sdk_root}"
    return 0
  fi

  if [[ "${resolve_status}" -eq 1 ]]; then
    echo "Missing Android SDK. Set ANDROID_SDK_ROOT/ANDROID_HOME or add sdk.dir to local.properties." >&2
  fi
  exit "${resolve_status}"
}

find_cmdline_tools_root() {
  local sdk_root="${1}"
  local cmdline_tools_dir="${sdk_root}/cmdline-tools"

  if [[ -d "${cmdline_tools_dir}/latest/bin" ]]; then
    printf '%s\n' "${cmdline_tools_dir}/latest"
    return 0
  fi

  [[ -d "${cmdline_tools_dir}" ]] || return 1

  local first_match
  first_match=$(find "${cmdline_tools_dir}" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort | head -n 1 || true)
  [[ -n "${first_match}" ]] || return 1
  printf '%s\n' "${first_match}"
}

adb_bin() {
  printf '%s/platform-tools/adb\n' "$(require_android_sdk_root)"
}

emulator_bin() {
  printf '%s/emulator/emulator\n' "$(require_android_sdk_root)"
}

sdkmanager_bin() {
  local sdk_root tools_root
  sdk_root=$(require_android_sdk_root)
  tools_root=$(find_cmdline_tools_root "${sdk_root}" || true)
  [[ -n "${tools_root}" ]] || {
    echo "Android command-line tools not found under ${sdk_root}/cmdline-tools." >&2
    exit 1
  }
  printf '%s/bin/sdkmanager\n' "${tools_root}"
}

avdmanager_bin() {
  local sdk_root tools_root
  sdk_root=$(require_android_sdk_root)
  tools_root=$(find_cmdline_tools_root "${sdk_root}" || true)
  [[ -n "${tools_root}" ]] || {
    echo "Android command-line tools not found under ${sdk_root}/cmdline-tools." >&2
    exit 1
  }
  printf '%s/bin/avdmanager\n' "${tools_root}"
}

require_executable() {
  local path="${1}"
  [[ -x "${path}" ]] || {
    echo "Missing executable: ${path}" >&2
    exit 1
  }
}

require_android_tools() {
  if should_use_local_properties_sdk_dir; then
    require_python3
  fi

  require_executable "$(adb_bin)"
  require_executable "$(emulator_bin)"
  require_executable "$(sdkmanager_bin)"
  require_executable "$(avdmanager_bin)"
}

gradlew_cmd() {
  bash "${PROJECT_ROOT}/gradlew" "$@"
}

wait_for_boot_completed() {
  local adb timeout_seconds deadline boot_completed device_state
  adb=$(adb_bin)
  timeout_seconds="${VIBETAP_BOOT_TIMEOUT_SECONDS:-300}"
  deadline=$((SECONDS + timeout_seconds))
  boot_completed="unavailable"
  device_state="unknown"

  while (( SECONDS < deadline )); do
    device_state=$("${adb}" get-state 2>/dev/null | tr -d '\r' || true)

    if [[ "${device_state}" == "device" ]]; then
      boot_completed=$("${adb}" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
      if [[ "${boot_completed}" == "1" ]]; then
        return 0
      fi
    else
      boot_completed="unavailable"
    fi

    sleep 2
  done

  echo "Timed out waiting for Android device boot completion after ${timeout_seconds}s (device state: ${device_state:-unknown}, sys.boot_completed: ${boot_completed:-unavailable})." >&2
  exit 1
}

unlock_device() {
  local adb
  adb=$(adb_bin)
  "${adb}" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
  "${adb}" shell input keyevent 82 >/dev/null 2>&1 || true
}

launch_main_activity() {
  local adb
  adb=$(adb_bin)
  "${adb}" shell am start -n "${MAIN_ACTIVITY}"
}

print_android_summary() {
  local sdk_root
  sdk_root=$(require_android_sdk_root)

  cat <<EOF_SUMMARY
Android SDK: ${sdk_root}
ADB: $(adb_bin)
Emulator: $(emulator_bin)
AVD name: ${DEFAULT_AVD_NAME}
System image: ${DEFAULT_SYSTEM_IMAGE}
Platform: ${DEFAULT_PLATFORM}
Build tools: ${DEFAULT_BUILD_TOOLS}
EOF_SUMMARY
}
