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
import kotlin.math.min

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
    private var orbitPages: List<OrbitPage> = emptyList()

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

    fun onOrbitExpandRequested() {
        clearStatus()
        applyOrbitPage(pageIndex = 0, expanded = true)
    }

    fun onOrbitCollapseRequested() {
        collapseOrbit()
    }

    fun onNextOrbitPageRequested() {
        val currentPage = orbitPages.getOrNull(uiState.value.orbitPageIndex) ?: return
        if (!currentPage.hasNextPage) return
        applyOrbitPage(pageIndex = currentPage.pageIndex + 1, expanded = true)
    }

    fun onPreviousOrbitPageRequested() {
        val currentPage = orbitPages.getOrNull(uiState.value.orbitPageIndex) ?: return
        if (!currentPage.hasPreviousPage) return
        applyOrbitPage(pageIndex = currentPage.pageIndex - 1, expanded = true)
    }

    private fun refreshShortcuts(scope: CoroutineScope) {
        val generation = synchronized(bindLock) {
            shortcutsRefreshJob?.cancel()
            shortcutsRefreshGeneration += 1
            shortcutsRefreshGeneration
        }

        val refreshJob = scope.launch {
            try {
                val shortcuts = shortcutsProvider().sortedBy { it.order }
                if (!isLatestShortcutsRefresh(generation)) {
                    return@launch
                }
                val refreshedOrbitPages = buildOrbitPages(shortcuts)
                orbitPages = refreshedOrbitPages
                mutableUiState.update { state ->
                    val pageCount = refreshedOrbitPages.size.coerceAtLeast(1)
                    val pageToShow = if (state.orbitExpanded) {
                        refreshedOrbitPages.getOrNull(min(state.orbitPageIndex, pageCount - 1))
                    } else {
                        null
                    }
                    state.copy(
                        skillBubbles = shortcuts,
                        statusMessage = null,
                        statusTone = ImeStatusTone.Neutral,
                        orbitExpanded = pageToShow != null,
                        orbitPageIndex = pageToShow?.pageIndex ?: 0,
                        orbitPageCount = pageCount,
                        innerRingBubbles = pageToShow?.innerRing ?: emptyList(),
                        outerRingBubbles = pageToShow?.outerRing ?: emptyList(),
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
            RecordingState.PROCESSING -> {
                mutableUiState.update { state ->
                    state.copy(
                        statusMessage = "Still processing the previous recording.",
                        statusTone = ImeStatusTone.Warning,
                    )
                }
            }
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
                collapseOrbit()
            } else {
                mutableUiState.update { state ->
                    state.copy(
                        statusMessage = "No active text field for phrase insertion.",
                        statusTone = ImeStatusTone.Neutral,
                    )
                }
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

    private fun applyOrbitPage(pageIndex: Int, expanded: Boolean) {
        val page = orbitPages.getOrNull(pageIndex) ?: return
        mutableUiState.update { state ->
            state.copy(
                orbitExpanded = expanded,
                orbitPageIndex = page.pageIndex,
                orbitPageCount = page.pageCount,
                innerRingBubbles = page.innerRing,
                outerRingBubbles = page.outerRing,
            )
        }
    }

    private fun collapseOrbit() {
        mutableUiState.update { state ->
            state.copy(
                orbitExpanded = false,
                orbitPageIndex = 0,
                orbitPageCount = orbitPages.size.coerceAtLeast(1),
                innerRingBubbles = emptyList(),
                outerRingBubbles = emptyList(),
            )
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

    private fun setStatus(message: String?) {
        mutableUiState.update { state ->
            state.copy(
                statusMessage = message ?: "Something went wrong.",
                statusTone = ImeStatusTone.Error,
            )
        }
    }
}
