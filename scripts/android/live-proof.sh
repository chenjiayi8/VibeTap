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

ui_cmd() {
  "${UI_AUTOMATION[@]}" --serial "${TARGET_SERIAL}" "$@"
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
  clear_with_backspace "${TARGET_SERIAL}" 200
  ui_cmd set-text-desc "${content_desc}" "${value}" >/dev/null
}

wait_for_visible_text() {
  local needle="${1}"
  local timeout_seconds="${2:-20}"
  ui_cmd wait-text-contains --timeout "${timeout_seconds}" "${needle}" >/dev/null
}

enter_text_with_keyboard() {
  local text="${1}"
  local character

  for ((index = 0; index < ${#text}; index++)); do
    character="${text:index:1}"
    case "${character}" in
      ' ')
        ui_cmd tap-desc "Keyboard space key" >/dev/null
        ;;
      [A-Z])
        ui_cmd tap-desc "Keyboard letter ${character} key" >/dev/null
        ;;
      [a-z])
        ui_cmd tap-desc "Keyboard letter ${character^^} key" >/dev/null
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
  launch_main_activity "${TARGET_SERIAL}" >/dev/null
  ui_cmd wait-text "VibeTap Keyboard Settings" >/dev/null
  set_field_text "OpenAI API Key input" "${VIBETAP_OPENAI_API_KEY}"
  set_field_text "Preset label input ship-pr" "${VIBETAP_SAVED_PHRASE_LABEL}"
  set_field_text "Preset text input ship-pr" "${VIBETAP_SAVED_PHRASE_TEXT}"
  ui_cmd tap-desc "Save preset ship-pr" >/dev/null
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
  "${ADB}" -s "${TARGET_SERIAL}" install -r "${VIBETAP_TERMUX_APK_PATH}" >/dev/null
  "${ADB}" -s "${TARGET_SERIAL}" shell pm grant "${APP_ID}" android.permission.RECORD_AUDIO >/dev/null 2>&1 || true
  "${ADB}" -s "${TARGET_SERIAL}" shell ime enable "${IME_ID}" >/dev/null
  "${ADB}" -s "${TARGET_SERIAL}" shell ime set "${IME_ID}" >/dev/null
  save_screenshot "emulator-ready"
}

run_keyboard_typing_phase() {
  launch_termux
  clear_with_backspace "${TARGET_SERIAL}" 200
  enter_text_with_keyboard "${VIBETAP_TYPED_SENTINEL}"
  wait_for_visible_text "${VIBETAP_TYPED_SENTINEL}" 20
  save_screenshot "typed-proof"
}

run_dictation_phase() {
  DICTATION_LOGCAT_PATH="${EVIDENCE_DIR}/dictation-logcat.txt"
  : > "${DICTATION_LOGCAT_PATH}"
  clear_with_backspace "${TARGET_SERIAL}" 200
  "${ADB}" -s "${TARGET_SERIAL}" logcat -c >/dev/null 2>&1 || true

  ui_cmd tap-desc "Keyboard mic key" >/dev/null
  sleep 2
  run_audio_feed_command
  sleep 1
  ui_cmd tap-desc "Keyboard mic key" >/dev/null || true
  sleep 2

  "${ADB}" -s "${TARGET_SERIAL}" logcat -d > "${DICTATION_LOGCAT_PATH}" || true

  if wait_for_visible_text "${VIBETAP_EXPECTED_DICTATION_SUBSTRING}" 60; then
    save_screenshot "dictation-proof"
    return 0
  fi

  record_mediarecorder_blocker_if_present
  record_blocker "audio-injection-failed" "Expected dictated text did not appear in Termux."
}

run_saved_phrase_phase() {
  clear_with_backspace "${TARGET_SERIAL}" 200
  ui_cmd tap-desc "Keyboard float key" >/dev/null
  ui_cmd tap-text "${VIBETAP_SAVED_PHRASE_LABEL}" >/dev/null
  wait_for_visible_text "${VIBETAP_SAVED_PHRASE_TEXT}" 20
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
  run_dictation_phase
  run_saved_phrase_phase

  echo "Live proof passed. Evidence: ${EVIDENCE_DIR}"
}

main "$@"
