package com.frank.voiceoverlay.overlay

import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleUiStateTest {
    @Test
    fun defaultState_isCollapsed() {
        assertEquals(
            BubbleUiState(
                recordingState = RecordingState.IDLE,
                interactionState = BubbleInteractionState.Collapsed,
                shortcuts = emptyList(),
            ),
            BubbleUiState(),
        )
    }

    @Test
    fun expandedState_keepsInteractionStateExplicit() {
        val preset = ShortcutPreset(
            id = "ship-pr",
            label = "Ship-PR",
            text = "Good, please proceed to use \$ship-pr",
            order = 0,
        )

        val state = BubbleUiState(
            recordingState = RecordingState.LISTENING,
            interactionState = BubbleInteractionState.ShortcutsExpanded,
            shortcuts = listOf(preset),
        )

        assertEquals(RecordingState.LISTENING, state.recordingState)
        assertEquals(BubbleInteractionState.ShortcutsExpanded, state.interactionState)
        assertEquals(listOf(preset), state.shortcuts)
    }
}
