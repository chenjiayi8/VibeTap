# VibeTap

VibeTap is an Android IME-first voice keyboard for personal use. It runs as a custom keyboard, records short microphone clips, sends them to OpenAI Whisper for transcription, runs a lightweight cleanup pass with `gpt-5-nano`, and commits the cleaned text into the active editor through the current `InputConnection`.

## What this app does

- Shows a settings screen for keyboard enablement, keyboard picker access, OpenAI API-key entry, microphone permission, and saved phrase editing.
- Stores a user-supplied OpenAI API key locally on the device.
- Presents two keyboard layouts:
  - a **docked keyboard** for normal typing with dictation controls
  - a **floating compact panel** for voice-first use with quick access to saved phrase skills
- Uses `InputConnection` commits for both cleaned dictation text and saved phrase insertion.

## Required Android permissions and capabilities

The current build depends on these Android capabilities:

- **Keyboard enablement** — required so Android can load VibeTap as an input method.
- **Active keyboard selection** — required so VibeTap becomes the keyboard for the current text field.
- **Microphone access** (`RECORD_AUDIO`) — required for dictation recording.
- **Network access** (`INTERNET`) — required for OpenAI transcription and cleanup requests.

Notes:
- Keyboard settings can be opened from the settings screen.
- The input-method picker can be opened from the settings screen.
- Microphone access can be requested from the settings screen before starting dictation.

## How to add the OpenAI API key

1. Launch the app.
2. In **OpenAI API Key**, paste your key in the `sk-...` field.
3. The key is saved locally as you edit the field.
4. Re-open the app to confirm the saved value is still present.

Security note: this is a personal-use prototype. The key is stored on-device and used directly by the client app. That is acceptable only for private testing and is not suitable for store distribution.

## Keyboard behavior

### Dictation
- **Mic tap when idle**: start recording, but only if an OpenAI API key is present.
- **Mic tap while listening**: stop recording and begin processing.
- **While processing**: another tap does not start a new recording; the UI reports that the previous recording is still processing.
- **Cleanup is conservative**: dictated text is only lightly corrected for punctuation, casing, filler words, and duplicate stutters.
- **Cleanup never answers for the user**: VibeTap does not intentionally convert dictated text into Q&A, bullets, or assistant-style responses.

### Layouts and saved phrase skills
- **Keyboard mode** is for typing and dictation.
- **Bubble mode** is for action bubbles only and does not show typing keys.
- **Mode switching is manual**.
- **Docked keyboard** must remain above tablet system navigation UI.
- **Saved phrase skills** are committed through the current `InputConnection`.

## Operator setup / verification flow

### Android SDK / emulator prerequisites

Configure Android tooling through either:

- `ANDROID_SDK_ROOT` (preferred), or
- `ANDROID_HOME`, or
- `local.properties` with `sdk.dir=...`

Verify an existing emulator setup with `--check`, or provision it with `--create` if it is missing. By default the script derives the emulator platform, system image, build-tools, and AVD name from the app's current `compileSdk` (currently Android 36), while still allowing overrides through `VIBETAP_ANDROID_PLATFORM`, `VIBETAP_SYSTEM_IMAGE`, `VIBETAP_BUILD_TOOLS`, and `VIBETAP_AVD_NAME`:

```bash
bash scripts/android/setup-emulator.sh --create
bash scripts/android/setup-emulator.sh --check
```

### Emulator-first testing

Use this as the default daily loop:

```bash
bash scripts/android/test-emulator.sh
bash scripts/android/capture-evidence.sh screenshot settings-home
```

`bash scripts/android/test-emulator.sh` already starts or reuses the named AVD. Use `bash scripts/android/start-emulator.sh` only when you want to boot/reuse the emulator without running the full verification lane yet.

The emulator workflow runs:

- `:app:testDebugUnitTest`
- `:app:assembleDebug`
- `:app:connectedDebugAndroidTest`
- `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- `adb shell am start -n com.frank.voiceoverlay/.MainActivity`

Use the checklist in `.codex/docs/plans/2026-05-14-vibetap-emulator-parity-checklist.md` to track emulator evidence and final tablet parity.

### Physical-tablet parity

Use the tablet only for final confirmation of keyboard enablement, microphone behavior, dictation insertion, and floating-panel ergonomics. This script requires `scrcpy`, builds the current debug APK, installs it on the selected physical device, launches `MainActivity`, and then opens the mirrored session:

```bash
bash scripts/android/tablet-parity.sh
```

### Full live proof

Run the full operator proof lane when you need the closest available end-to-end evidence for the real IME workflow:

```bash
bash scripts/android/live-proof.sh --env-file .env --evidence-dir captures/android/live-proof
```

This runner:

- requires the full live-proof variable set from `.env.example`, copied into `.env`
- loads the OpenAI API key and the rest of the live-proof configuration from `.env`
- drives the real Settings UI to enter the key
- enables and selects the VibeTap IME
- launches Termux as a real external target app
- proves docked typing
- attempts dictation through the real `MediaRecorder` + OpenAI path
- proves saved-phrase insertion through the floating panel

If the runner reports `emulator-mediarecorder-unsupported`, treat that as a real blocker for emulator dictation proof, not as a passing run. Keep that label honest in operator reports until the emulator `MediaRecorder` limitation is removed or the proof runs on hardware that supports the full dictation path.

## Known limitations

- The connected Android test, install, launch, and proof-plan steps require a real device or emulator; they cannot pass in a device-less session.
- `emulator-mediarecorder-unsupported` means the emulator could not provide a valid `MediaRecorder` dictation proof. That outcome is blocked, not green.
- The README documents the intended IME-first keyboard interactions, but final on-device proof is still required to confirm keyboard switching and floating-panel ergonomics on hardware.
- OpenAI API usage is direct from the client and is only acceptable for personal testing.
- Release signing and store-distribution hardening are out of scope for this MVP.

## Repository layout

- `app/` — Android application module
- `.codex/docs/plans/` — design, implementation, and proof-plan docs
- `gradle/` — Gradle wrapper files
