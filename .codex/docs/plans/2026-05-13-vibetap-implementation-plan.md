# VibeTap Implementation Plan — Shipped IME-First Summary

> Historical planning note: this document now records the shipped IME-first state so repo-local file maps stay accurate after the keyboard-first migration.

**Shipped goal:** VibeTap is an Android IME-first voice keyboard that records short microphone clips, transcribes them with OpenAI audio transcription, runs cleanup with `gpt-5-nano`, and commits text through the active `InputConnection`.

**Shipped architecture:** The app now centers on one keyboard runtime plus a settings activity. The main packages are `settings`, `dictation`, `ime`, `permissions`, and `shortcuts`. Dictation remains behind `DictationCoordinator`, while text insertion happens through `InputConnectionCommitter` inside `VibeTapImeService`.

**Status:** Tasks 1-6 landed the keyboard-first migration and removed the old non-keyboard runtime.

---

## Current repository file map

### Main source

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/frank/voiceoverlay/MainActivity.kt`
- `app/src/main/java/com/frank/voiceoverlay/dictation/AndroidAudioRecorder.kt`
- `app/src/main/java/com/frank/voiceoverlay/dictation/AudioRecorder.kt`
- `app/src/main/java/com/frank/voiceoverlay/dictation/DictationCoordinator.kt`
- `app/src/main/java/com/frank/voiceoverlay/dictation/OpenAiCleanupClient.kt`
- `app/src/main/java/com/frank/voiceoverlay/dictation/OpenAiClientException.kt`
- `app/src/main/java/com/frank/voiceoverlay/dictation/OpenAiTranscriptionClient.kt`
- `app/src/main/java/com/frank/voiceoverlay/dictation/RecordingState.kt`
- `app/src/main/java/com/frank/voiceoverlay/ime/ImeUiState.kt`
- `app/src/main/java/com/frank/voiceoverlay/ime/InputConnectionCommitter.kt`
- `app/src/main/java/com/frank/voiceoverlay/ime/KeyboardLayoutMode.kt`
- `app/src/main/java/com/frank/voiceoverlay/ime/TextCommitter.kt`
- `app/src/main/java/com/frank/voiceoverlay/ime/VibeTapImeService.kt`
- `app/src/main/java/com/frank/voiceoverlay/ime/VoiceKeyboardController.kt`
- `app/src/main/java/com/frank/voiceoverlay/ime/ui/DockedKeyboardView.kt`
- `app/src/main/java/com/frank/voiceoverlay/ime/ui/FloatingSkillPanel.kt`
- `app/src/main/java/com/frank/voiceoverlay/ime/ui/VibeTapImeRoot.kt`
- `app/src/main/java/com/frank/voiceoverlay/permissions/PermissionGate.kt`
- `app/src/main/java/com/frank/voiceoverlay/settings/AppSettings.kt`
- `app/src/main/java/com/frank/voiceoverlay/settings/SettingsScreen.kt`
- `app/src/main/java/com/frank/voiceoverlay/settings/SettingsStore.kt`
- `app/src/main/java/com/frank/voiceoverlay/settings/SettingsViewModel.kt`
- `app/src/main/java/com/frank/voiceoverlay/settings/VoiceOverlaySettingsDataStore.kt`
- `app/src/main/java/com/frank/voiceoverlay/shortcuts/ShortcutPreset.kt`
- `app/src/main/java/com/frank/voiceoverlay/shortcuts/ShortcutPresetRepository.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/xml/vibetap_input_method.xml`

### Unit tests

- `app/src/test/java/com/frank/voiceoverlay/dictation/DictationCoordinatorTest.kt`
- `app/src/test/java/com/frank/voiceoverlay/dictation/OpenAiClientsTest.kt`
- `app/src/test/java/com/frank/voiceoverlay/ime/VoiceKeyboardControllerTest.kt`
- `app/src/test/java/com/frank/voiceoverlay/settings/SettingsStoreTest.kt`
- `app/src/test/java/com/frank/voiceoverlay/settings/SettingsViewModelTest.kt`
- `app/src/test/java/com/frank/voiceoverlay/shortcuts/ShortcutPresetRepositoryTest.kt`

### Instrumentation tests

- `app/src/androidTest/java/com/frank/voiceoverlay/MainActivitySmokeTest.kt`
- `app/src/androidTest/java/com/frank/voiceoverlay/ime/DockedKeyboardViewTest.kt`
- `app/src/androidTest/java/com/frank/voiceoverlay/ime/FloatingSkillPanelTest.kt`
- `app/src/androidTest/java/com/frank/voiceoverlay/ime/InputConnectionCommitterTest.kt`
- `app/src/androidTest/java/com/frank/voiceoverlay/settings/SettingsScreenTest.kt`

---

## Current verification lane

Use the shipped verification path for current work on this branch:

- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:connectedDebugAndroidTest`
- `bash scripts/android/test-emulator.sh`

These checks verify the IME runtime, settings flow, dictation pipeline, and input-connection insertion path.
