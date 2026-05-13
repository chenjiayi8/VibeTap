package com.frank.voiceoverlay.dictation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DictationCoordinator(
    private val recorder: AudioRecorder,
    private val transcribeFile: suspend (File) -> String,
    private val cleanText: suspend (String) -> String,
    private val insertText: (String) -> Unit,
    private val deleteFile: (File) -> Boolean,
) {
    constructor(
        recorder: AudioRecorder,
        transcribe: suspend () -> String,
        clean: suspend () -> String,
        insertText: (String) -> Unit,
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

        try {
            val audioFile = recorder.stop()
            val transcript = transcribeFile(audioFile)
            val cleanedText = cleanText(transcript)
            insertText(cleanedText)
            deleteFile(audioFile)
            mutableState.value = RecordingState.IDLE
        } catch (error: Throwable) {
            mutableState.value = RecordingState.ERROR
            throw error
        }
    }
}
