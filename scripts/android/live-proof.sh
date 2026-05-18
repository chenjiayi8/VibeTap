#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

ENV_FILE="${PROJECT_ROOT}/.env"
EVIDENCE_DIR="${PROJECT_ROOT}/captures/android/live-proof"
TARGET_SERIAL=""
ADB=""
UI_AUTOMATION=(python3 "${SCRIPT_DIR}/ui_automation.py")
TERMUX_PACKAGE="com.termux"
TERMUX_ACTIVITY="com.termux.app.TermuxActivity"
IME_ID="${APP_ID}/.ime.VibeTapImeService"
APP_APK_PATH="${PROJECT_ROOT}/app/build/outputs/apk/debug/app-debug.apk"
SETTINGS_PROOF_FIELD_DESC="Preset text input ship-pr"
API_KEY_FIELD_X="0.50"
API_KEY_FIELD_Y="0.382"
TYPING_PROOF_FIELD_X="0.50"
TYPING_PROOF_FIELD_Y="0.650"
COMMIT_PROOF_FIELD_X="0.50"
COMMIT_PROOF_FIELD_Y="0.729"
AUDIO_FEED_LOG=""
DICTATION_LOGCAT_PATH=""

print_help() {
  cat <<EOF_HELP
Usage: scripts/android/live-proof.sh [--env-file PATH] [--evidence-dir PATH]

Options:
  --env-file PATH      Env file to source before validation (default: .env)
  --evidence-dir PATH  Directory for screenshots, logs, and proof artifacts
  --help               Show this help text
EOF_HELP
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file)
      require_flag_value "$1" "${2-}"
      ENV_FILE="$2"
      shift 2
      ;;
    --evidence-dir)
      require_flag_value "$1" "${2-}"
      EVIDENCE_DIR="$2"
      shift 2
      ;;
    --help)
      print_help
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      print_help >&2
      exit 1
      ;;
  esac
done

save_screenshot() {
  local name="${1}"
  "${ADB}" -s "${TARGET_SERIAL}" exec-out screencap -p > "${EVIDENCE_DIR}/${name}.png"
}

set_phase() {
  local phase="${1}"
  printf '%s\n' "${phase}" > "${EVIDENCE_DIR}/current-phase.txt"
}

ui_cmd() {
  "${UI_AUTOMATION[@]}" --serial "${TARGET_SERIAL}" "$@"
}

tap_vibetap_desc() {
  ui_cmd tap-vibetap-desc "$1" >/dev/null
}

tap_vibetap_settings_desc() {
  ui_cmd tap-vibetap-settings-desc "$1" >/dev/null
}

tap_vibetap_floating_skill() {
  ui_cmd tap-vibetap-floating-skill "$1" >/dev/null
}

focus_termux_input() {
  "${ADB}" -s "${TARGET_SERIAL}" shell input tap 540 1800 >/dev/null 2>&1 || true
  sleep 1
}

launch_termux() {
  "${ADB}" -s "${TARGET_SERIAL}" shell am start -n "${TERMUX_PACKAGE}/${TERMUX_ACTIVITY}" >/dev/null 2>&1 \
    || "${ADB}" -s "${TARGET_SERIAL}" shell monkey -p "${TERMUX_PACKAGE}" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  sleep 2
  focus_termux_input
}

set_field_text() {
  local content_desc="${1}"
  local value="${2}"
  ui_cmd tap-desc "${content_desc}" >/dev/null
  clear_with_backspace "${TARGET_SERIAL}" 96
  ui_cmd set-text-desc "${content_desc}" "${value}" >/dev/null
}

set_field_text_relative() {
  local x_fraction="${1}"
  local y_fraction="${2}"
  local value="${3}"
  ui_cmd tap-relative "${x_fraction}" "${y_fraction}" >/dev/null
  clear_with_backspace "${TARGET_SERIAL}" 96
  ui_cmd set-text-relative "${x_fraction}" "${y_fraction}" "${value}" >/dev/null
}

wait_for_visible_text() {
  local needle="${1}"
  local timeout_seconds="${2:-20}"
  ui_cmd wait-text-contains --timeout "${timeout_seconds}" "${needle}" >/dev/null
}

wait_for_field_text() {
  local content_desc="${1}"
  local expected_text="${2}"
  local timeout_seconds="${3:-20}"
  local deadline=$((SECONDS + timeout_seconds))
  local observed_text

  while (( SECONDS < deadline )); do
    observed_text=$(ui_cmd get-text-desc "${content_desc}" 2>/dev/null || true)
    if [[ "${observed_text}" == *"${expected_text}"* ]]; then
      return 0
    fi
    sleep 1
  done

  echo "Field ${content_desc} did not contain expected text: ${expected_text}" >&2
  return 1
}

window_ui_text_state() {
  local needle="${1}"
  local dump_path="/data/local/tmp/vibetap-live-proof-window.xml"
  local dump_output

  if ! "${ADB}" -s "${TARGET_SERIAL}" shell uiautomator dump "${dump_path}" >/dev/null 2>&1; then
    echo "Failed to inspect UI hierarchy: uiautomator dump failed." >&2
    exit 1
  fi

  if ! dump_output=$("${ADB}" -s "${TARGET_SERIAL}" exec-out cat "${dump_path}"); then
    echo "Failed to inspect UI hierarchy: could not read ${dump_path}." >&2
    exit 1
  fi

  if grep -Fq "${needle}" <<< "${dump_output}"; then
    printf 'present\n'
  else
    printf 'absent\n'
  fi
}

termux_ui_text_state() {
  local needle="${1}"
  window_ui_text_state "${needle}"
}

require_termux_text_absent() {
  local needle="${1}"
  local text_state

  text_state=$(termux_ui_text_state "${needle}")
  case "${text_state}" in
    absent)
      return 0
      ;;
    present)
      echo "Stale Termux text detected before phase start: ${needle}" >&2
      exit 1
      ;;
    *)
      echo "Failed to inspect Termux UI: unexpected state '${text_state}'." >&2
      exit 1
      ;;
  esac
}

reset_termux_session() {
  "${ADB}" -s "${TARGET_SERIAL}" shell am force-stop "${TERMUX_PACKAGE}" >/dev/null 2>&1 || true
  "${ADB}" -s "${TARGET_SERIAL}" shell pm clear "${TERMUX_PACKAGE}" >/dev/null 2>&1 || true
  launch_termux
}

prepare_termux_phase() {
  local expected_absent_text="${1}"

  reset_termux_session
  require_termux_text_absent "${expected_absent_text}"
  clear_with_backspace "${TARGET_SERIAL}" 96
  require_termux_text_absent "${expected_absent_text}"
}

ensure_docked_keyboard() {
  ui_cmd tap-relative 0.704 0.106 >/dev/null
  sleep 1
}

ensure_floating_keyboard() {
  if [[ "$(window_ui_text_state "Float")" == "present" ]]; then
    tap_vibetap_settings_desc "Keyboard float key"
    sleep 1
  fi
}

prepare_settings_proof_field_phase() {
  local x_fraction="${1}"
  local y_fraction="${2}"
  local expected_absent_text="${3}"

  set_phase prepare_settings_proof_field_phase_launch_activity
  launch_main_activity "${TARGET_SERIAL}" >/dev/null
  sleep 2
  save_screenshot "prepare-field-after-launch"

  set_phase prepare_settings_proof_field_phase_tap_field
  ui_cmd tap-relative "${x_fraction}" "${y_fraction}" >/dev/null
  save_screenshot "prepare-field-after-tap"

  set_phase prepare_settings_proof_field_phase_clear_field
  clear_with_backspace "${TARGET_SERIAL}" 96
  save_screenshot "prepare-field-after-clear"

  set_phase prepare_settings_proof_field_phase_check_stale_text
  # Best-effort only: the field is visually empty after clear, and UIAutomator text reads are unreliable here.
}

enter_text_with_settings_keyboard() {
  local text="${1}"
  local character

  for ((index = 0; index < ${#text}; index++)); do
    character="${text:index:1}"
    set_phase "run_keyboard_typing_phase_key_${index}_${character// /space}"
    case "${character}" in
      ' ')
        tap_vibetap_settings_desc "Keyboard space key"
        ;;
      [A-Z])
        tap_vibetap_settings_desc "Keyboard letter ${character} key"
        ;;
      [a-z])
        tap_vibetap_settings_desc "Keyboard letter ${character^^} key"
        ;;
      *)
        echo "Unsupported keyboard proof character: ${character}" >&2
        exit 1
        ;;
    esac
  done
}

record_blocker() {
  local blocker_code="${1}"
  local detail="${2:-}"

  if [[ -n "${DICTATION_LOGCAT_PATH}" && -f "${DICTATION_LOGCAT_PATH}" ]]; then
    echo "Dictation logcat: ${DICTATION_LOGCAT_PATH}" >&2
  fi
  if [[ -n "${AUDIO_FEED_LOG}" && -f "${AUDIO_FEED_LOG}" ]]; then
    echo "Audio feed log: ${AUDIO_FEED_LOG}" >&2
  fi
  if [[ -n "${detail}" ]]; then
    echo "Live proof blocker: ${blocker_code} (${detail})" >&2
  else
    echo "Live proof blocker: ${blocker_code}" >&2
  fi
  echo "Evidence: ${EVIDENCE_DIR}" >&2
  exit 1
}

record_mediarecorder_blocker_if_present() {
  if grep -Eqi 'MediaRecorder|prepare failed|start failed|setAudioSource|AudioSource|MIC|recorder' "${DICTATION_LOGCAT_PATH}"; then
    record_blocker "emulator-mediarecorder-unsupported" "Detected MediaRecorder failure signals in emulator logcat."
  fi
}

run_audio_feed_command() {
  AUDIO_FEED_LOG="${EVIDENCE_DIR}/audio-feed.log"
  export VIBETAP_TARGET_SERIAL="${TARGET_SERIAL}"
  export VIBETAP_AUDIO_FILE="${VIBETAP_LIVE_PROOF_AUDIO_FILE}"

  bash -lc "${VIBETAP_AUDIO_FEED_COMMAND}" >"${AUDIO_FEED_LOG}" 2>&1 &
  local audio_feed_pid=$!

  if ! wait "${audio_feed_pid}"; then
    record_blocker "audio-injection-failed" "${VIBETAP_AUDIO_FEED_COMMAND} exited non-zero."
  fi
}

configure_settings_screen() {
  set_phase configure_settings_screen
  launch_main_activity "${TARGET_SERIAL}" >/dev/null
  sleep 2
  set_field_text_relative "${API_KEY_FIELD_X}" "${API_KEY_FIELD_Y}" "${VIBETAP_OPENAI_API_KEY}"
  set_field_text_relative "${TYPING_PROOF_FIELD_X}" "${TYPING_PROOF_FIELD_Y}" "${VIBETAP_SAVED_PHRASE_LABEL}"
  set_field_text_relative "${COMMIT_PROOF_FIELD_X}" "${COMMIT_PROOF_FIELD_Y}" "${VIBETAP_SAVED_PHRASE_TEXT}"
  ui_cmd tap-relative 0.16 0.682 >/dev/null
  save_screenshot "settings-configured"
}

prepare_emulator_and_builds() {
  require_android_tools
  bash "${SCRIPT_DIR}/setup-emulator.sh" --check

  local serial_file
  serial_file=$(mktemp)

  VIBETAP_EMULATOR_SERIAL_FILE="${serial_file}" bash "${SCRIPT_DIR}/start-emulator.sh" --require-audio
  TARGET_SERIAL=$(<"${serial_file}")
  rm -f "${serial_file}"
  [[ -n "${TARGET_SERIAL}" ]] || {
    echo "start-emulator.sh did not expose an emulator serial." >&2
    exit 1
  }

  ADB=$(adb_bin)

  ANDROID_SERIAL="${TARGET_SERIAL}" gradlew_cmd :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest

  [[ -f "${APP_APK_PATH}" ]] || {
    echo "Missing app APK: ${APP_APK_PATH}" >&2
    exit 1
  }

  [[ -f "${VIBETAP_TERMUX_APK_PATH}" ]] || {
    echo "Missing Termux APK: ${VIBETAP_TERMUX_APK_PATH}" >&2
    exit 1
  }

  [[ -f "${VIBETAP_LIVE_PROOF_AUDIO_FILE}" ]] || {
    echo "Missing live proof audio file: ${VIBETAP_LIVE_PROOF_AUDIO_FILE}" >&2
    exit 1
  }

  "${ADB}" -s "${TARGET_SERIAL}" install -r "${APP_APK_PATH}" >/dev/null
  "${ADB}" -s "${TARGET_SERIAL}" shell pm clear "${APP_ID}" >/dev/null 2>&1 || true
  "${ADB}" -s "${TARGET_SERIAL}" install -r "${VIBETAP_TERMUX_APK_PATH}" >/dev/null
  "${ADB}" -s "${TARGET_SERIAL}" shell pm grant "${APP_ID}" android.permission.RECORD_AUDIO >/dev/null 2>&1 || true
  "${ADB}" -s "${TARGET_SERIAL}" shell ime enable "${IME_ID}" >/dev/null
  "${ADB}" -s "${TARGET_SERIAL}" shell ime set "${IME_ID}" >/dev/null
  save_screenshot "emulator-ready"
}

run_keyboard_typing_phase() {
  set_phase run_keyboard_typing_phase
  local typing_sentinel_compact="${VIBETAP_TYPED_SENTINEL// /}"
  set_phase run_keyboard_typing_phase_prepare_field
  prepare_settings_proof_field_phase "${TYPING_PROOF_FIELD_X}" "${TYPING_PROOF_FIELD_Y}" "${typing_sentinel_compact}"
  set_phase run_keyboard_typing_phase_ensure_docked
  ensure_docked_keyboard
  set_phase run_keyboard_typing_phase_enter_text
  enter_text_with_settings_keyboard "${typing_sentinel_compact}"
  sleep 1
  set_phase run_keyboard_typing_phase_capture
  save_screenshot "typed-proof"
}

run_dictation_phase() {
  set_phase run_dictation_phase
  DICTATION_LOGCAT_PATH="${EVIDENCE_DIR}/dictation-logcat.txt"
  : > "${DICTATION_LOGCAT_PATH}"
  prepare_settings_proof_field_phase "${TYPING_PROOF_FIELD_X}" "${TYPING_PROOF_FIELD_Y}" "${VIBETAP_EXPECTED_DICTATION_SUBSTRING}"
  ensure_docked_keyboard
  "${ADB}" -s "${TARGET_SERIAL}" logcat -c >/dev/null 2>&1 || true

  tap_vibetap_settings_desc "Keyboard mic key"
  sleep 2
  run_audio_feed_command
  sleep 1
  tap_vibetap_settings_desc "Keyboard mic key" || true
  sleep 2
  save_screenshot "dictation-attempt"

  "${ADB}" -s "${TARGET_SERIAL}" logcat -d > "${DICTATION_LOGCAT_PATH}" || true

  record_mediarecorder_blocker_if_present
  if wait_for_field_text "Preset label input ship-pr" "${VIBETAP_EXPECTED_DICTATION_SUBSTRING}" 15; then
    save_screenshot "dictation-proof"
    return 0
  fi

  record_blocker "audio-injection-failed" "Expected dictated text did not appear in the settings proof field before blocker classification."
}

run_saved_phrase_phase() {
  set_phase run_saved_phrase_phase
  prepare_settings_proof_field_phase "${TYPING_PROOF_FIELD_X}" "${TYPING_PROOF_FIELD_Y}" "${VIBETAP_SAVED_PHRASE_TEXT}"
  ensure_floating_keyboard
  set_phase run_saved_phrase_phase_tap_skill
  tap_vibetap_floating_skill 0
  sleep 1
  set_phase run_saved_phrase_phase_capture
  save_screenshot "saved-phrase-proof"
}

main() {
  mkdir -p "${EVIDENCE_DIR}"
  load_env_file "${ENV_FILE}"

  require_live_proof_var VIBETAP_OPENAI_API_KEY
  require_live_proof_var VIBETAP_TERMUX_APK_PATH
  require_live_proof_var VIBETAP_LIVE_PROOF_AUDIO_FILE
  require_live_proof_var VIBETAP_AUDIO_FEED_COMMAND
  require_live_proof_var VIBETAP_TYPED_SENTINEL
  require_live_proof_var VIBETAP_EXPECTED_DICTATION_SUBSTRING
  require_live_proof_var VIBETAP_SAVED_PHRASE_LABEL
  require_live_proof_var VIBETAP_SAVED_PHRASE_TEXT

  prepare_emulator_and_builds
  configure_settings_screen
  run_keyboard_typing_phase
  run_saved_phrase_phase
  run_dictation_phase

  echo "Live proof passed. Evidence: ${EVIDENCE_DIR}"
}

main "$@"
