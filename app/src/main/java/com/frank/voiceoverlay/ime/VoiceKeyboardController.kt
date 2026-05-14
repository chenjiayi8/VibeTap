package com.frank.voiceoverlay.ime

import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VoiceKeyboardController(
    private val recordingState: StateFlow<RecordingState>,
    private val shortcutsProvider: suspend () -> List<ShortcutPreset>,
    private val startRecording: suspend () -> Unit,
    private val stopRecording: suspend () -> Unit,
    private val resetRecording: () -> Unit,
    private val commitPhrase: suspend (String) -> Boolean,
) {
    private val mutableUiState = MutableStateFlow(ImeUiState(recordingState = recordingState.value))
    val uiState: StateFlow<ImeUiState> = mutableUiState.asStateFlow()

    fun bind(scope: CoroutineScope) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val shortcuts = shortcutsProvider()
                .sortedBy { it.order }
                .take(3)
            mutableUiState.update { state ->
                state.copy(skillBubbles = shortcuts)
            }
        }

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            recordingState.collectLatest { state ->
                mutableUiState.update { current ->
                    current.copy(recordingState = state)
                }
            }
        }
    }

    fun onLayoutToggle() {
        mutableUiState.update { state ->
            state.copy(
                layoutMode = when (state.layoutMode) {
                    KeyboardLayoutMode.DOCKED -> KeyboardLayoutMode.FLOATING
                    KeyboardLayoutMode.FLOATING -> KeyboardLayoutMode.DOCKED
                },
                statusMessage = null,
            )
        }
    }

    suspend fun onMicTapped() {
        when (recordingState.value) {
            RecordingState.IDLE -> runMicAction(startRecording)
            RecordingState.LISTENING -> runMicAction(stopRecording)
            RecordingState.PROCESSING -> {
                mutableUiState.update { state ->
                    state.copy(statusMessage = "Still processing the previous recording.")
                }
            }
            RecordingState.ERROR -> {
                try {
                    resetRecording()
                    clearStatus()
                } catch (error: Throwable) {
                    setStatus(error.message)
                }
            }
        }
    }

    suspend fun onSkillBubbleTapped(preset: ShortcutPreset) {
        val didCommit = commitPhrase(preset.text)
        if (didCommit) {
            clearStatus()
        } else {
            mutableUiState.update { state ->
                state.copy(statusMessage = "No active text field for phrase insertion.")
            }
        }
    }

    private suspend fun runMicAction(action: suspend () -> Unit) {
        try {
            action()
            clearStatus()
        } catch (error: Throwable) {
            setStatus(error.message)
        }
    }

    private fun clearStatus() {
        mutableUiState.update { state ->
            state.copy(statusMessage = null)
        }
    }

    private fun setStatus(message: String?) {
        mutableUiState.update { state ->
            state.copy(statusMessage = message ?: "Something went wrong.")
        }
    }
}
