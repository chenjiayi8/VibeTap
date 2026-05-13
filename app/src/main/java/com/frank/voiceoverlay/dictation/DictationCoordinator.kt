package com.frank.voiceoverlay.dictation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DictationCoordinator(
    private val recorder: AudioRecorder,
    private val transcribeFile: suspend (File) -> String,
    private val cleanText: suspend (String) -> String,
    private val insertText: suspend (String) -> Unit,
    private val deleteFile: (File) -> Boolean,
) {
    constructor(
        recorder: AudioRecorder,
        transcribe: suspend () -> String,
        clean: suspend () -> String,
        insertText: suspend (String) -> Unit,
        deleteFile: () -> Boolean,
    ) : this(
        recorder = recorder,
        transcribeFile = { transcribe() },
        cleanText = { clean() },
        insertText = insertText,
        deleteFile = { deleteFile() },
    )

    private val mutableState = MutableStateFlow(RecordingState.IDLE)
    val state: StateFlow<RecordingState> = mutableState.asStateFlow()

    suspend fun startRecording() {
        require(mutableState.value == RecordingState.IDLE) {
            "Recording can only start from IDLE"
        }

        recorder.start()
        mutableState.value = RecordingState.LISTENING
    }

    suspend fun stopRecording() {
        require(mutableState.value == RecordingState.LISTENING) {
            "Recording can only stop from LISTENING"
        }

        mutableState.value = RecordingState.PROCESSING

        var audioFile: File? = null
        var pendingError: Throwable? = null

        try {
            audioFile = recorder.stop()
            val transcript = transcribeFile(audioFile)
            val cleanedText = cleanText(transcript)
            insertText(cleanedText)
        } catch (error: Throwable) {
            pendingError = error
        }

        audioFile?.let { file ->
            try {
                if (!deleteFile(file)) {
                    throw IllegalStateException("Failed to delete recorded audio file")
                }
            } catch (deleteError: Throwable) {
                if (pendingError == null) {
                    pendingError = deleteError
                } else {
                    pendingError.addSuppressed(deleteError)
                }
            }
        }

        pendingError?.let { error ->
            mutableState.value = RecordingState.ERROR
            throw error
        }

        mutableState.value = RecordingState.IDLE
    }

    fun reset() {
        require(mutableState.value == RecordingState.ERROR) {
            "Coordinator can only reset from ERROR"
        }

        mutableState.value = RecordingState.IDLE
    }
}
