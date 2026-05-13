# VibeTap Design

## Summary

Build a personal-use Android overlay app that provides voice-to-text input without acting as a keyboard. The app lives as a floating bubble above other apps. A single tap starts or stops recording. The captured audio is sent to OpenAI Whisper for transcription, then the raw transcript is sent to `gpt-5-nano` for narrow cleanup that removes duplicated fragments, breathy sounds, and similar dictation noise. The cleaned text is inserted directly into the currently focused text field through an Android Accessibility service. A double tap on the main bubble expands a radial menu of shortcut bubbles that each insert a predefined phrase immediately.

## Goals

- Provide a system-wide floating bubble for quick dictation.
- Insert cleaned transcript text into the focused input field without showing a keyboard.
- Support radial shortcut bubbles for predefined phrases.
- Keep the first version personal-use only, with a user-supplied OpenAI API key stored locally.

## Non-Goals

- Building a full Android `InputMethodService` keyboard.
- Supporting offline transcription or offline cleanup in MVP.
- Adding a backend, account system, sync, or billing.
- Supporting dynamic shortcut templates or command flows in MVP.

## Product Shape

The app is an overlay assistant, not an IME. It stays visible system-wide while enabled. The main interaction model is:

- Single tap on the bubble toggles recording on and off.
- After recording stops, the app transcribes audio with Whisper.
- The transcript is cleaned by `gpt-5-nano` using a strict cleanup instruction.
- The cleaned text is inserted immediately into the currently focused editable field through Accessibility.
- Double tap expands a radial shortcut menu around the main bubble.
- Tapping a shortcut inserts a predefined phrase immediately, then collapses the menu.

The first version assumes a single owner-user who provides their own OpenAI API key in settings.

## Architecture

### 1. Overlay UI

Responsible for:

- Rendering the floating bubble system-wide.
- Supporting drag-to-move behavior.
- Detecting single tap for record toggle.
- Detecting double tap for radial shortcut expansion.
- Showing visual state for idle, listening, processing, and error.
- Rendering and collapsing radial shortcut bubbles.

This layer should only coordinate user interactions and state display. It should not own API calls or insertion logic.

### 2. Accessibility Insertion Service

Responsible for:

- Inspecting the currently focused node.
- Verifying the node is editable.
- Inserting the final text at the current cursor position.
- Replacing selected text when a target selection clearly exists.
- Returning explicit failure when no editable target is focused.

This is the Android-specific integration boundary. It should be isolated so input targeting logic can evolve without affecting the overlay or AI pipeline.

### 3. Audio Capture Pipeline

Responsible for:

- Requesting microphone access.
- Starting and stopping a single active recording session.
- Writing a short-lived temporary audio artifact suitable for upload.
- Publishing recording state changes to the UI.
- Cleaning up temporary audio after success or failure.

Only one recording may exist at a time. New recordings are blocked while processing is active.

### 4. AI Text Pipeline

Responsible for:

- Uploading audio to Whisper.
- Receiving the raw transcript.
- Sending the transcript to `gpt-5-nano` for cleanup.
- Returning final cleaned text for insertion.

The cleanup prompt must be narrow. It should remove duplicate fragments, breath/noise artifacts, and obvious speech disfluencies, while preserving meaning and intended tone. It must not paraphrase, summarize, or rewrite content beyond necessary cleanup.

### 5. Shortcut Preset Store

Responsible for:

- Persisting user-defined preset labels and insertion strings.
- Persisting radial ordering.
- Supporting create, edit, reorder, and delete operations from settings.

For MVP, local on-device storage is sufficient.

## Main Flows

### Dictation Flow

1. The user enables overlay permission, accessibility permission, and microphone permission.
2. The floating bubble remains visible over other apps while enabled.
3. A single tap starts recording and the bubble enters the listening state.
4. A second single tap stops recording and the bubble enters the processing state.
5. Audio is sent to Whisper.
6. The returned transcript is sent to `gpt-5-nano` with the cleanup instruction.
7. The cleaned text is inserted into the focused editable field through Accessibility.
8. The bubble returns to idle.

### Shortcut Flow

1. The user double taps the main bubble.
2. The radial shortcut menu expands around the bubble.
3. The user taps a shortcut.
4. The shortcut text is inserted immediately into the focused editable field.
5. The radial menu collapses.

## Behavioral Rules

- Recording is single-session only.
- Shortcut expansion and insertion are disabled while recording is active.
- Dictation insertion appends at the cursor by default.
- If text is actively selected in the target field, replacement is allowed.
- Temporary audio is retained only as long as needed to complete the request.
- If no editable field is focused, the app must fail clearly rather than guessing.

## Error Handling

- If microphone permission is missing, record initiation must surface a clear permission path.
- If accessibility permission is missing, insertion must fail with a clear enablement prompt.
- If overlay permission is missing, the bubble cannot be shown and the app must explain why.
- If Whisper or `gpt-5-nano` fails because of network, API, or auth problems, the bubble returns to idle and shows a short visible failure message.
- If no editable field is focused, insertion is skipped and the user is informed that no text target was found.
- If the preset list is empty or malformed, the main bubble still works and the radial menu simply does not show items.

## Privacy and Security

- Audio is sent to OpenAI only after explicit user recording.
- The OpenAI API key is stored locally on device.
- MVP uses the local key directly from the app for the owner-user's requests.
- Temporary audio artifacts are deleted after each request completes or fails.
- No cloud account system, centralized storage, analytics pipeline, or sync is part of MVP.

This design accepts the tradeoff that API-key exposure risk is tolerable for a personal-use first version.

## Settings Surface

MVP settings should include:

- OpenAI API key entry and validation.
- Overlay enable/disable.
- Shortcut preset management: add, edit, delete, reorder.
- A simple test insertion or diagnostics area if Android permissions allow it.

The settings surface should stay narrow and operational, not become a full admin console.

## Testing Strategy

### Functional

- Bubble gestures: single tap, double tap, drag, collapse.
- Recording transitions: idle, listening, processing, error.
- Permission gating for microphone, overlay, and accessibility.
- Shortcut CRUD and persistence.
- Radial ordering persistence across app restarts.

### Integration

- Whisper request and transcript handling.
- `gpt-5-nano` cleanup behavior on duplicated words and breath-like artifacts.
- Accessibility insertion in common editable targets where Android permits it.

### Contract Tests

- Cleanup preserves intended meaning while removing duplicate fragments and obvious dictation noise.
- Processing failure does not leave the UI stuck in listening or processing state.
- Temporary audio is deleted on both success and failure paths.

## Risks and Constraints

- Accessibility-based insertion behavior varies by target app and Android version.
- Overlay interaction can conflict with app-specific gesture zones or OS restrictions.
- Immediate insertion increases speed but sends any recognition mistakes directly into the target app.
- Direct client-side API key use is acceptable for personal use but not for a public release.

## Recommended Delivery Scope

Keep MVP narrow:

- One always-on overlay bubble.
- Tap-to-start, tap-to-stop recording.
- Whisper transcription.
- `gpt-5-nano` cleanup.
- Immediate Accessibility insertion.
- User-editable radial shortcuts.
- Local settings for API key and presets.

Defer:

- Offline mode.
- Backend proxy or team distribution.
- Smart template shortcuts.
- Preview-before-insert flow.
- Full keyboard/IME support.

## Acceptance Criteria

- The user can enable the overlay and see the floating bubble over other apps.
- The user can start and stop recording from the bubble.
- A successful recording produces inserted text in the focused field.
- Cleanup removes obvious duplicate fragments and breath-like dictation artifacts without materially changing wording.
- Double tapping opens radial shortcut bubbles.
- Tapping a shortcut inserts the stored phrase immediately.
- User-defined shortcuts persist across app restarts.
- Failure states return the bubble to idle and communicate the reason clearly.
