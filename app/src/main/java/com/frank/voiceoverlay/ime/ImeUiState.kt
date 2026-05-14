package com.frank.voiceoverlay.ime

import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.shortcuts.ShortcutPreset

data class ImeUiState(
    val layoutMode: KeyboardLayoutMode = KeyboardLayoutMode.DOCKED,
    val recordingState: RecordingState = RecordingState.IDLE,
    val skillBubbles: List<ShortcutPreset> = emptyList(),
    val statusMessage: String? = null,
)
