#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

MODE="create"
AVD_NAME="${DEFAULT_AVD_NAME}"
SYSTEM_IMAGE="${DEFAULT_SYSTEM_IMAGE}"
PLATFORM="${DEFAULT_PLATFORM}"
BUILD_TOOLS="${DEFAULT_BUILD_TOOLS}"

find_avd_block() {
  local avdmanager="${1}"
  local avd_name="${2}"

  "${avdmanager}" list avd | awk -v target="${avd_name}" '
    BEGIN {
      in_block = 0
      matched = 0
      emitted = 0
      block = ""
    }
    /^Name: / {
      if (matched && !emitted) {
        print block
        emitted = 1
        exit 0
      }
      block = $0 ORS
      in_block = 1
      matched = ($0 == "Name: " target)
      emitted = 0
      next
    }
    /^$/ {
      if (in_block) {
        block = block ORS
        if (matched && !emitted) {
          print block
          emitted = 1
          exit 0
        }
        block = ""
        in_block = 0
        matched = 0
      }
      next
    }
    in_block {
      block = block $0 ORS
    }
    END {
      if (matched && !emitted) {
        print block
      }
    }
  '
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --check)
      MODE="check"
      shift
      ;;
    --create)
      MODE="create"
      shift
      ;;
    --avd-name)
      require_flag_value "$1" "${2-}"
      AVD_NAME="$2"
      shift 2
      ;;
    --system-image)
      require_flag_value "$1" "${2-}"
      SYSTEM_IMAGE="$2"
      shift 2
      ;;
    --platform)
      require_flag_value "$1" "${2-}"
      PLATFORM="$2"
      shift 2
      ;;
    --build-tools)
      require_flag_value "$1" "${2-}"
      BUILD_TOOLS="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

require_android_tools
SDKMANAGER=$(sdkmanager_bin)
AVDMANAGER=$(avdmanager_bin)

if [[ "${MODE}" == "create" ]]; then
  yes | "${SDKMANAGER}" --licenses >/dev/null || true
  "${SDKMANAGER}" \
    "platform-tools" \
    "emulator" \
    "platforms;${PLATFORM}" \
    "build-tools;${BUILD_TOOLS}" \
    "${SYSTEM_IMAGE}"
fi

AVD_BLOCK=$(find_avd_block "${AVDMANAGER}" "${AVD_NAME}")
if [[ -z "${AVD_BLOCK}" ]]; then
  if [[ "${MODE}" == "check" ]]; then
    echo "Missing AVD: ${AVD_NAME}" >&2
    exit 1
  fi

  echo "no" | "${AVDMANAGER}" create avd \
    --name "${AVD_NAME}" \
    --package "${SYSTEM_IMAGE}" \
    --device "pixel_7"

  AVD_BLOCK=$(find_avd_block "${AVDMANAGER}" "${AVD_NAME}")
fi

print_android_summary
printf '%s' "${AVD_BLOCK}"
