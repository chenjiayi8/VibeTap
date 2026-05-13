package com.frank.voiceoverlay.overlay

import com.frank.voiceoverlay.shortcuts.ShortcutPreset

data class BubbleUiState(
    val isRecording: Boolean = false,
    val shortcutsVisible: Boolean = false,
    val shortcuts: List<ShortcutPreset> = emptyList(),
)
