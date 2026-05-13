# VibeTap

VibeTap is an Android overlay dictation app for personal use. It is designed to stay above other apps as a floating bubble, capture short microphone recordings, send them to OpenAI Whisper for transcription, run a lightweight cleanup pass with `gpt-5-nano`, and insert the cleaned text into the currently focused editable field through an Android Accessibility service.

## What this app does

- Shows a settings screen for API-key entry, overlay permission setup, accessibility setup, and shortcut editing.
- Stores a user-supplied OpenAI API key locally on the device.
- Defines an overlay bubble service that is intended to support:
  - single-tap start / single-tap stop dictation
  - processing feedback while transcription/cleanup is running
  - double-tap radial shortcuts for predefined phrases
- Uses the Accessibility service to insert either cleaned dictation text or a shortcut phrase into the currently focused editable field.

## Required Android permissions and capabilities

The current build depends on these Android capabilities:

- **Display over other apps** (`SYSTEM_ALERT_WINDOW`) — required for the floating overlay bubble.
- **Accessibility service enablement** — required so VibeTap can find the focused editable field and replace its text.
- **Microphone access** (`RECORD_AUDIO`) — required for dictation recording.
- **Network access** (`INTERNET`) — required for OpenAI transcription and cleanup requests.

Notes:
- Overlay permission is opened from the settings screen.
- Accessibility settings are opened from the settings screen.
- Microphone access can be requested from the settings screen before starting dictation.

## How to add the OpenAI API key

1. Launch the app.
2. In **OpenAI API Key**, paste your key in the `sk-...` field.
3. The key is saved locally as you edit the field.
4. Re-open the app to confirm the saved value is still present.

Security note: this is a personal-use prototype. The key is stored on-device and used directly by the client app. That is acceptable only for private testing and is not suitable for store distribution.

## Recording and shortcuts behavior

### Recording

The overlay controller is implemented with these interaction rules:

- **Single tap when idle**: start recording, but only if an OpenAI API key is present.
- **Single tap while listening**: stop recording and begin processing.
- **While processing**: another tap does not start a new recording; the UI reports that the previous recording is still processing.
- **If recording reaches an error state**: a tap resets the controller back to idle.
- **If no API key is configured**: recording is blocked and the bubble shows `Add an OpenAI API key in settings before recording.`

### Shortcuts

- **Double tap** expands the radial shortcut menu.
- Tapping a shortcut inserts that preset's text into the currently focused editable field.
- If insertion fails, the UI reports that accessibility and a focused text field are required.
- Shortcuts are editable from the settings screen and are persisted locally.

## Operator setup / verification flow

Use this repo-level flow before the device proof cases in `.codex/docs/plans/2026-05-13-vibetap-proof-plan.md`:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

When a device or emulator is attached, run the full Task 9 verification flow:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.frank.voiceoverlay/.MainActivity
```

After that, execute the proof cases from `.codex/docs/plans/2026-05-13-vibetap-proof-plan.md` on the attached tablet/emulator.

## Known limitations

- The connected Android test, install, launch, and proof-plan steps require a real device or emulator; they cannot pass in a device-less session.
- The README documents the intended overlay dictation interactions, but the final on-device proof is still required to confirm the complete overlay lifecycle on hardware.
- OpenAI API usage is direct from the client and is only acceptable for personal testing.
- Release signing and store-distribution hardening are out of scope for this MVP.

## Repository layout

- `app/` — Android application module
- `.codex/docs/plans/` — design, implementation, and proof-plan docs
- `gradle/` — Gradle wrapper files
