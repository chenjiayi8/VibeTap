package com.frank.voiceoverlay.overlay

import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.settings.AppSettings
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OverlayBubbleCommand {
    data class ScheduleSingleTapResolution(val delayMillis: Long) : OverlayBubbleCommand
    data object CancelPendingSingleTapResolution : OverlayBubbleCommand
    data object None : OverlayBubbleCommand
}

class OverlayBubbleController(
    private val recordingState: StateFlow<RecordingState>,
    private val refreshSettingsSnapshot: suspend () -> AppSettings,
    private val startRecording: suspend () -> Unit,
    private val stopRecording: suspend () -> Unit,
    private val resetRecording: () -> Unit,
    private val insertShortcutText: suspend (String) -> Boolean,
    private val gestureInterpreter: BubbleGestureInterpreter = BubbleGestureInterpreter(DEFAULT_DOUBLE_TAP_WINDOW_MILLIS),
    private val doubleTapWindowMillis: Long = DEFAULT_DOUBLE_TAP_WINDOW_MILLIS,
) {
    private val mutableUiState = MutableStateFlow(BubbleUiState(shortcuts = ShortcutPreset.defaultPresets()))
    val uiState: StateFlow<BubbleUiState> = mutableUiState.asStateFlow()

    @Volatile
    private var latestSettings: AppSettings = AppSettings()

    fun bind(scope: CoroutineScope) {
        scope.launch {
            recordingState.collect { state ->
                mutableUiState.update { current -> current.copy(recordingState = state) }
            }
        }
        scope.launch { refreshSettings() }
    }

    suspend fun refreshSettings() {
        val settings = refreshSettingsSnapshot()
        latestSettings = settings
        mutableUiState.update { current ->
            current.copy(
                recordingState = recordingState.value,
                shortcuts = settings.presets,
            )
        }
    }

    suspend fun onBubbleTap(timestampMillis: Long): OverlayBubbleCommand {
        if (mutableUiState.value.interactionState == BubbleInteractionState.ShortcutsExpanded) {
            collapseShortcuts()
            return OverlayBubbleCommand.CancelPendingSingleTapResolution
        }

        return when (gestureInterpreter.onTap(timestampMillis)) {
            BubbleGesture.SingleTapPending -> {
                mutableUiState.update { current ->
                    current.copy(interactionState = BubbleInteractionState.PendingSingleTap)
                }
                OverlayBubbleCommand.ScheduleSingleTapResolution(doubleTapWindowMillis + 1)
            }

            BubbleGesture.DoubleTap -> {
                refreshSettings()
                mutableUiState.update { current ->
                    current.copy(
                        interactionState = BubbleInteractionState.ShortcutsExpanded,
                        statusMessage = null,
                    )
                }
                OverlayBubbleCommand.CancelPendingSingleTapResolution
            }

            BubbleGesture.SingleTapConfirmed -> OverlayBubbleCommand.None
        }
    }

    suspend fun resolvePendingBubbleTap(timestampMillis: Long) {
        if (gestureInterpreter.resolvePendingTap(timestampMillis) != BubbleGesture.SingleTapConfirmed) {
            return
        }

        when (recordingState.value) {
            RecordingState.IDLE -> handleStartRecording()
            RecordingState.LISTENING -> handleStopRecording()
            RecordingState.PROCESSING -> setStatus(PROCESSING_STATUS_MESSAGE)
            RecordingState.ERROR -> handleReset()
        }
    }

    suspend fun onShortcutTap(preset: ShortcutPreset) {
        collapseShortcuts()
        refreshSettings()
        val shortcutToInsert = latestSettings.presets.firstOrNull { it.id == preset.id } ?: preset

        val inserted = runCatching { insertShortcutText(shortcutToInsert.text) }
            .getOrElse {
                setStatus(it.message ?: SHORTCUT_INSERTION_STATUS_MESSAGE)
                return
            }

        if (inserted) {
            clearStatus()
        } else {
            setStatus(SHORTCUT_INSERTION_STATUS_MESSAGE)
        }
    }

    fun requireCurrentApiKey(): String =
        latestSettings.openAiApiKey
            .trim()
            .takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(MISSING_API_KEY_STATUS_MESSAGE)

    private suspend fun handleStartRecording() {
        refreshSettings()
        if (latestSettings.openAiApiKey.isBlank()) {
            setStatus(MISSING_API_KEY_STATUS_MESSAGE)
            return
        }

        runCatching { startRecording() }
            .onSuccess { clearStatus() }
            .onFailure { error -> setStatus(error.message ?: START_RECORDING_STATUS_MESSAGE) }
    }

    private suspend fun handleStopRecording() {
        runCatching { stopRecording() }
            .onSuccess { clearStatus() }
            .onFailure { error -> setStatus(error.message ?: STOP_RECORDING_STATUS_MESSAGE) }
    }

    private fun handleReset() {
        runCatching { resetRecording() }
            .onSuccess { clearStatus() }
            .onFailure { error -> setStatus(error.message ?: RESET_STATUS_MESSAGE) }
    }

    private fun collapseShortcuts() {
        gestureInterpreter.reset()
        mutableUiState.update { current ->
            current.copy(
                recordingState = recordingState.value,
                interactionState = BubbleInteractionState.Collapsed,
            )
        }
    }

    private fun clearStatus() {
        mutableUiState.update { current ->
            current.copy(
                recordingState = recordingState.value,
                interactionState = BubbleInteractionState.Collapsed,
                statusMessage = null,
            )
        }
    }

    private fun setStatus(message: String) {
        mutableUiState.update { current ->
            current.copy(
                recordingState = recordingState.value,
                interactionState = BubbleInteractionState.Collapsed,
                statusMessage = message,
            )
        }
    }

    companion object {
        const val DEFAULT_DOUBLE_TAP_WINDOW_MILLIS = 250L
        const val MISSING_API_KEY_STATUS_MESSAGE = "Add an OpenAI API key in settings before recording."
        const val PROCESSING_STATUS_MESSAGE = "Still processing the previous recording."
        const val SHORTCUT_INSERTION_STATUS_MESSAGE =
            "Enable the accessibility service and focus a text field before inserting."
        const val START_RECORDING_STATUS_MESSAGE = "Unable to start recording."
        const val STOP_RECORDING_STATUS_MESSAGE = "Unable to stop recording."
        const val RESET_STATUS_MESSAGE = "Unable to reset recording right now."
    }
}
