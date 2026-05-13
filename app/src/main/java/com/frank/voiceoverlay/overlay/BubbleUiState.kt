package com.frank.voiceoverlay.overlay

import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.shortcuts.ShortcutPreset

sealed interface BubbleInteractionState {
    data object Collapsed : BubbleInteractionState
    data object PendingSingleTap : BubbleInteractionState
    data object ShortcutsExpanded : BubbleInteractionState
}

data class BubbleUiState(
    val recordingState: RecordingState = RecordingState.IDLE,
    val interactionState: BubbleInteractionState = BubbleInteractionState.Collapsed,
    val shortcuts: List<ShortcutPreset> = emptyList(),
)
