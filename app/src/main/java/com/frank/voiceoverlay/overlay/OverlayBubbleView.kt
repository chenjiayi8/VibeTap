package com.frank.voiceoverlay.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.shortcuts.ShortcutPreset

@Composable
fun OverlayBubbleView(
    uiState: BubbleUiState = BubbleUiState(),
    onBubbleTap: () -> Unit = {},
    onShortcutTap: (ShortcutPreset) -> Unit = {},
) {
    Box(contentAlignment = Alignment.Center) {
        if (uiState.interactionState == BubbleInteractionState.ShortcutsExpanded) {
            RadialShortcutMenu(
                presets = uiState.shortcuts,
                onPresetClick = onShortcutTap,
            )
        }

        Surface(
            onClick = onBubbleTap,
            modifier = Modifier
                .size(64.dp)
                .semantics { contentDescription = "Voice input bubble" },
            shape = CircleShape,
            color = bubbleColor(uiState.recordingState),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = bubbleGlyph(uiState.recordingState))
            }
        }
    }
}

@Composable
private fun bubbleColor(recordingState: RecordingState) = when (recordingState) {
    RecordingState.IDLE -> MaterialTheme.colorScheme.primary
    RecordingState.LISTENING -> MaterialTheme.colorScheme.tertiary
    RecordingState.PROCESSING -> MaterialTheme.colorScheme.secondary
    RecordingState.ERROR -> MaterialTheme.colorScheme.error
}

private fun bubbleGlyph(recordingState: RecordingState): String = when (recordingState) {
    RecordingState.IDLE -> "🎙️"
    RecordingState.LISTENING -> "⏺️"
    RecordingState.PROCESSING -> "…"
    RecordingState.ERROR -> "!"
}
