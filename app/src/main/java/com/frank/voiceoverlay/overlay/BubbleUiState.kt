package com.frank.voiceoverlay.overlay

import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.shortcuts.ShortcutPreset

data class BubbleUiState(
    val recordingState: RecordingState = RecordingState.IDLE,
    val shortcutsVisible: Boolean = false,
    val shortcuts: List<ShortcutPreset> = emptyList(),
    val hasPendingSingleTap: Boolean = false,
)
