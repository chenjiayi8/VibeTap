# VibeTap

VibeTap is an Android overlay app for fast voice-to-text insertion without becoming a keyboard. It stays above other apps as a floating bubble, records on tap, sends audio to OpenAI Whisper, cleans the transcript with `gpt-5-nano`, and inserts the final text into the currently focused field through an Accessibility service.

## Current status

This repository is initialized with the first public project scaffold:

- Android app module and Gradle wrapper
- placeholder activity, overlay service, and accessibility service
- domain models for settings, shortcuts, and recording state
- design and implementation docs under `.codex/docs/plans/`

The app is **not implemented yet**. This scaffold is meant to make the architecture explicit and provide a clean starting point for iterative development.

## MVP goals

- Floating overlay bubble visible above other apps
- Tap-to-start / tap-to-stop recording
- Whisper transcription + narrow cleanup pass with `gpt-5-nano`
- Accessibility-based insertion into the focused editable field
- Double-tap radial shortcut menu for predefined phrases
- Local, user-supplied OpenAI API key

## Repository layout

- `app/` — Android application module
- `.codex/docs/plans/` — design and implementation planning docs
- `gradle/` — Gradle wrapper files

## Getting started

1. Install Android Studio or a compatible Android SDK + JDK 17+ toolchain.
2. Ensure the Android SDK path is available locally (for example via `local.properties`).
3. Run:

   ```bash
   ./gradlew tasks
   ```

4. Open the project in Android Studio to continue implementation.

## Notes

- This project currently assumes a personal-use MVP.
- Direct client-side API-key usage is intentionally limited to early personal testing and is not production-safe.
- The design reference lives in `.codex/docs/plans/2026-05-13-vibetap-design.md`.
