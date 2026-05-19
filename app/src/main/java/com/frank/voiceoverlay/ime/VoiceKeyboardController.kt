package com.frank.voiceoverlay.ime

import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
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
    private val bindLock = Any()
    private var isBound = false
    private var shortcutsRefreshJob: Job? = null
    private var shortcutsRefreshGeneration = 0L

    val uiState: StateFlow<ImeUiState> = mutableUiState.asStateFlow()

    fun bind(scope: CoroutineScope) {
        var shouldBindRecordingState = false
        synchronized(bindLock) {
            if (!isBound) {
                isBound = true
                shouldBindRecordingState = true
            }
        }

        if (shouldBindRecordingState) {
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                recordingState.collectLatest { state ->
                    mutableUiState.update { current ->
                        current.copy(recordingState = state)
                    }
                }
            }
        }

        refreshShortcuts(scope)
    }

    private fun refreshShortcuts(scope: CoroutineScope) {
        val generation = synchronized(bindLock) {
            shortcutsRefreshJob?.cancel()
            shortcutsRefreshGeneration += 1
            shortcutsRefreshGeneration
        }

        val refreshJob = scope.launch {
            try {
                val shortcuts = shortcutsProvider()
                    .sortedBy { it.order }
                    .take(3)
                if (!isLatestShortcutsRefresh(generation)) {
                    return@launch
                }
                mutableUiState.update { state ->
                    state.copy(
                        skillBubbles = shortcuts,
                        statusMessage = null,
                        statusTone = ImeStatusTone.Neutral,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (!isLatestShortcutsRefresh(generation)) {
                    return@launch
                }
                setStatus(error.message)
            }
        }

        synchronized(bindLock) {
            if (generation == shortcutsRefreshGeneration) {
                shortcutsRefreshJob = refreshJob
            }
        }
    }

    private fun isLatestShortcutsRefresh(generation: Long): Boolean = synchronized(bindLock) {
        generation == shortcutsRefreshGeneration
    }

    fun onLayoutToggle() {
        mutableUiState.update { state ->
            state.copy(
                layoutMode = when (state.layoutMode) {
                    KeyboardLayoutMode.DOCKED -> KeyboardLayoutMode.FLOATING
                    KeyboardLayoutMode.FLOATING -> KeyboardLayoutMode.DOCKED
                },
                statusMessage = null,
                statusTone = ImeStatusTone.Neutral,
            )
        }
    }

    suspend fun onMicTapped() {
        when (recordingState.value) {
            RecordingState.IDLE -> runMicAction(startRecording)
            RecordingState.LISTENING -> runMicAction(stopRecording)
            RecordingState.PROCESSING -> setNeutralStatus("Still processing the previous recording.")
            RecordingState.ERROR -> {
                try {
                    resetRecording()
                    clearStatus()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    setStatus(error.message)
                }
            }
        }
    }

    suspend fun onSkillBubbleTapped(preset: ShortcutPreset) {
        try {
            val didCommit = commitPhrase(preset.text)
            if (didCommit) {
                clearStatus()
            } else {
                setNeutralStatus("No active text field for phrase insertion.")
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            setStatus(error.message)
        }
    }

    private suspend fun runMicAction(action: suspend () -> Unit) {
        try {
            action()
            clearStatus()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            setStatus(error.message)
        }
    }

    private fun clearStatus() {
        mutableUiState.update { state ->
            state.copy(
                statusMessage = null,
                statusTone = ImeStatusTone.Neutral,
            )
        }
    }

    private fun setNeutralStatus(message: String) {
        mutableUiState.update { state ->
            state.copy(
                statusMessage = message,
                statusTone = ImeStatusTone.Neutral,
            )
        }
    }

    private fun setStatus(message: String?) {
        mutableUiState.update { state ->
            state.copy(
                statusMessage = message ?: "Something went wrong.",
                statusTone = ImeStatusTone.Error,
            )
        }
    }
}
