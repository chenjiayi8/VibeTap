package com.frank.voiceoverlay.ime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
) {
    val uiState by controller.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (uiState.layoutMode) {
            KeyboardLayoutMode.DOCKED -> DockedKeyboardView(
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

            KeyboardLayoutMode.FLOATING -> {
                Text(text = "Floating mode coming soon")
                Button(onClick = controller::onLayoutToggle) {
                    Text(text = "Dock")
                }
            }
        }

        uiState.statusMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
