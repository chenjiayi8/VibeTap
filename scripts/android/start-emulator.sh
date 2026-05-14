#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

AVD_NAME="${DEFAULT_AVD_NAME}"
WIPE_DATA="false"

running_emulator_avd_name() {
  local adb="${1}"
  local serial="${2}"

  "${adb}" -s "${serial}" emu avd name 2>/dev/null \
    | tr -d '\r' \
    | sed '/^OK$/d;/^$/d' \
    | tail -n 1
}

find_matching_emulator() {
  local adb="${1}"
  local target_avd="${2}"
  local serial status running_avd_name matched_serial matched_status

  matched_serial=""
  matched_status=""

  while IFS=$'\t' read -r serial status; do
    [[ -n "${serial}" ]] || continue
    [[ "${serial}" == emulator-* ]] || continue

    running_avd_name=$(running_emulator_avd_name "${adb}" "${serial}")
    if [[ -z "${running_avd_name}" ]]; then
      echo "An emulator entry already exists (${serial}, ${status:-unknown}), but its AVD name could not be determined. Stop it or wait for it to finish booting before retrying ${target_avd}." >&2
      return 2
    fi

    if [[ "${running_avd_name}" == "${target_avd}" ]]; then
      if [[ -n "${matched_serial}" ]]; then
        echo "Multiple emulator entries already exist for AVD '${target_avd}' (${matched_serial} and ${serial}). Stop the extras before retrying." >&2
        return 2
      fi
      matched_serial="${serial}"
      matched_status="${status}"
      continue
    fi

    echo "An emulator is already running for AVD '${running_avd_name}' (${serial}, ${status:-unknown}); requested '${target_avd}'. Stop the running emulator before starting a different AVD." >&2
    return 2
  done < <("${adb}" devices | tail -n +2)

  if [[ -n "${matched_serial}" ]]; then
    printf '%s\t%s\n' "${matched_serial}" "${matched_status}"
    return 0
  fi

  return 1
}

wait_for_launched_emulator_serial() {
  local adb="${1}"
  local target_avd="${2}"
  local timeout_seconds deadline serial status running_avd_name

  timeout_seconds="${VIBETAP_BOOT_TIMEOUT_SECONDS:-300}"
  deadline=$((SECONDS + timeout_seconds))

  while (( SECONDS < deadline )); do
    while IFS=$'\t' read -r serial status; do
      [[ -n "${serial}" ]] || continue
      [[ "${serial}" == emulator-* ]] || continue

      running_avd_name=$(running_emulator_avd_name "${adb}" "${serial}")
      if [[ -z "${running_avd_name}" ]]; then
        continue
      fi

      if [[ "${running_avd_name}" == "${target_avd}" ]]; then
        printf '%s\n' "${serial}"
        return 0
      fi

      echo "An unexpected emulator for AVD '${running_avd_name}' appeared on ${serial} while waiting for '${target_avd}'." >&2
      return 1
    done < <("${adb}" devices | tail -n +2)

    sleep 2
  done

  echo "Timed out waiting for emulator '${target_avd}' to appear in adb." >&2
  return 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --avd-name)
      require_flag_value "$1" "${2-}"
      AVD_NAME="$2"
      shift 2
      ;;
    --wipe-data)
      WIPE_DATA="true"
      shift
      ;;
    --help)
      cat <<EOF_HELP
Usage: scripts/android/start-emulator.sh [--avd-name NAME] [--wipe-data]
EOF_HELP
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

require_android_tools
ADB=$(adb_bin)
EMULATOR=$(emulator_bin)
set +e
MATCHING_EMULATOR=$(find_matching_emulator "${ADB}" "${AVD_NAME}")
MATCHING_STATUS=$?
set -e

if [[ "${MATCHING_STATUS}" -eq 0 ]]; then
  IFS=$'\t' read -r TARGET_SERIAL TARGET_STATE <<< "${MATCHING_EMULATOR}"
  if [[ "${TARGET_STATE}" == "device" ]]; then
    echo "An emulator for ${AVD_NAME} is already running (${TARGET_SERIAL}). Reusing the active emulator."
  else
    echo "An emulator for ${AVD_NAME} already exists (${TARGET_SERIAL}, ${TARGET_STATE}). Waiting for it instead of launching a duplicate."
  fi
elif [[ "${MATCHING_STATUS}" -eq 1 ]]; then
  EXTRA_ARGS=()
  if [[ "${WIPE_DATA}" == "true" ]]; then
    EXTRA_ARGS+=("-wipe-data")
  fi

  nohup "${EMULATOR}" \
    -avd "${AVD_NAME}" \
    -no-snapshot \
    -netdelay none \
    -netspeed full \
    "${EXTRA_ARGS[@]}" \
    >/tmp/vibetap-emulator.log 2>&1 &

  TARGET_SERIAL=$(wait_for_launched_emulator_serial "${ADB}" "${AVD_NAME}")
else
  exit "${MATCHING_STATUS}"
fi

wait_for_boot_completed "${TARGET_SERIAL}"
unlock_device "${TARGET_SERIAL}"
"${ADB}" -s "${TARGET_SERIAL}" shell settings put global window_animation_scale 0 >/dev/null 2>&1 || true
"${ADB}" -s "${TARGET_SERIAL}" shell settings put global transition_animation_scale 0 >/dev/null 2>&1 || true
"${ADB}" -s "${TARGET_SERIAL}" shell settings put global animator_duration_scale 0 >/dev/null 2>&1 || true

if [[ -n "${VIBETAP_EMULATOR_SERIAL_FILE:-}" ]]; then
  printf '%s\n' "${TARGET_SERIAL}" > "${VIBETAP_EMULATOR_SERIAL_FILE}"
fi

echo "Emulator ready: ${TARGET_SERIAL} (${AVD_NAME})"
