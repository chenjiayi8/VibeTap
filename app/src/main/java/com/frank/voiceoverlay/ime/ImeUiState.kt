package com.frank.voiceoverlay.ime

import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.shortcuts.ShortcutPreset

enum class ImeStatusTone {
    Neutral,
    Error,
}

data class ImeUiState(
    val layoutMode: KeyboardLayoutMode = KeyboardLayoutMode.DOCKED,
    val recordingState: RecordingState = RecordingState.IDLE,
    val skillBubbles: List<ShortcutPreset> = emptyList(),
    val statusMessage: String? = null,
    val statusTone: ImeStatusTone = ImeStatusTone.Neutral,
)
