# VibeTap Emulator and Tablet Parity Checklist

## Emulator daily loop

1. Run `bash scripts/android/test-emulator.sh`.
2. Confirm `MainActivity` launches and shows:
   - `Keyboard setup`
   - `OpenAI API Key`
   - `Saved phrase skills`
3. Capture a settings screenshot:
   - `bash scripts/android/capture-evidence.sh screenshot settings-home`
   - If more than one device is attached, target the emulator explicitly with `bash scripts/android/capture-evidence.sh --serial <serial> screenshot settings-home` or `ANDROID_SERIAL=<serial> bash scripts/android/capture-evidence.sh screenshot settings-home`.
4. Open Android keyboard settings from the app.
5. Confirm VibeTap can be enabled as a keyboard.
6. Open the input-method picker from the app and switch to VibeTap.
7. Verify the docked keyboard renders without crashing.
8. Verify the floating compact panel can be opened and used.
9. Attempt one dictation run if the emulator audio path is available.
10. Verify dictated text or a saved phrase commits into an editable field.
11. Record any emulator-only failures before moving to the tablet.

## Tablet parity loop

1. Connect the USB debugging-enabled tablet.
2. Run `bash scripts/android/tablet-parity.sh`.
3. If more than one physical device is attached, re-run with the intended serial: `bash scripts/android/tablet-parity.sh <serial>` or `ANDROID_SERIAL=<serial> bash scripts/android/tablet-parity.sh`.
4. Verify VibeTap can be enabled and selected as the active keyboard.
5. Verify the docked keyboard over a real target app.
6. Verify the floating compact panel over a real target app.
7. Verify a live microphone dictation round-trip.
8. Verify text commits into a real editable field through the keyboard path.
9. Capture at least one mirrored screenshot or short recording if a regression appears.
10. Mark whether the issue reproduces on emulator, tablet, or both.
