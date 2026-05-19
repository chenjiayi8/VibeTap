package com.frank.voiceoverlay.ime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frank.voiceoverlay.ime.ImeStatusTone
import com.frank.voiceoverlay.ime.KeyboardLayoutMode
import com.frank.voiceoverlay.ime.VoiceKeyboardController
import kotlinx.coroutines.launch

@Composable
fun VibeTapImeRoot(
    controller: VoiceKeyboardController,
    onBackspace: () -> Boolean,
    onEnter: () -> Boolean,
    onCommitLetter: (String) -> Boolean,
    onCommitPhrase: (String) -> Boolean,
    dockedBottomInsetOverride: Dp? = null,
) {
    val uiState by controller.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val bottomInsetPadding = dockedBottomInsetOverride ?: with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    when (uiState.layoutMode) {
        KeyboardLayoutMode.DOCKED -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DockedKeyboardView(
                    bottomInsetPadding = bottomInsetPadding,
                    onMicTapped = {
                        coroutineScope.launch {
                            controller.onMicTapped()
                        }
                    },
                    onLayoutToggle = controller::onLayoutToggle,
                    onBackspace = onBackspace,
                    onEnter = onEnter,
                    onCommitLetter = onCommitLetter,
                    onCommitPhrase = onCommitPhrase,
                )

                uiState.statusMessage?.let { message ->
                    Text(
                        text = message,
                        color = when (uiState.statusTone) {
                            ImeStatusTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
                            ImeStatusTone.Error -> MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        KeyboardLayoutMode.FLOATING -> FloatingSkillPanel(
            skillBubbles = uiState.skillBubbles,
            statusMessage = uiState.statusMessage,
            statusTone = uiState.statusTone,
            onMicTapped = {
                coroutineScope.launch {
                    controller.onMicTapped()
                }
            },
            onDockTapped = controller::onLayoutToggle,
            onSkillBubbleTapped = { preset ->
                coroutineScope.launch {
                    controller.onSkillBubbleTapped(preset)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        )
    }
}
