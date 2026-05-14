# VibeTap Emulator and Tablet Parity Checklist

## Emulator daily loop

1. Run `bash scripts/android/test-emulator.sh`.
2. Confirm `MainActivity` launches and shows:
   - `OpenAI API Key`
   - `Enable overlay bubble`
   - `Shortcut Presets`
3. Capture a settings screenshot:
   - `bash scripts/android/capture-evidence.sh screenshot settings-home`
   - If more than one device is attached, target the emulator explicitly with `bash scripts/android/capture-evidence.sh --serial <serial> screenshot settings-home` or `ANDROID_SERIAL=<serial> bash scripts/android/capture-evidence.sh screenshot settings-home`.
4. Enable the overlay bubble from settings.
5. Confirm the overlay service can be launched without crashing.
6. Capture an overlay screenshot or 15-second recording.
7. Attempt one host-microphone dictation run if the emulator audio path is available.
8. Record any emulator-only failures before moving to the tablet.

## Tablet parity loop

1. Connect the USB debugging-enabled tablet.
2. Run `bash scripts/android/tablet-parity.sh`.
3. If more than one physical device is attached, re-run with the intended serial: `bash scripts/android/tablet-parity.sh <serial>` or `ANDROID_SERIAL=<serial> bash scripts/android/tablet-parity.sh`.
4. Verify overlay behavior over a real target app.
5. Verify accessibility insertion into a real editable field.
6. Verify a live microphone dictation round-trip.
7. Capture at least one mirrored screenshot or short recording if a regression appears.
8. Mark whether the issue reproduces on emulator, tablet, or both.
