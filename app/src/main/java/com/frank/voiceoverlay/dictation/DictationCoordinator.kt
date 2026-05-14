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
    private val logger: DictationLogger = NoOpDictationLogger,
) {
    constructor(
        recorder: AudioRecorder,
        transcribe: suspend () -> String,
        clean: suspend () -> String,
        insertText: suspend (String) -> Unit,
        deleteFile: () -> Boolean,
        logger: DictationLogger = NoOpDictationLogger,
    ) : this(
        recorder = recorder,
        transcribeFile = { transcribe() },
        cleanText = { clean() },
        insertText = insertText,
        deleteFile = { deleteFile() },
        logger = logger,
    )

    private val mutableState = MutableStateFlow(RecordingState.IDLE)
    val state: StateFlow<RecordingState> = mutableState.asStateFlow()

    suspend fun startRecording() {
        require(mutableState.value == RecordingState.IDLE) {
            "Recording can only start from IDLE"
        }

        logger.debug("startRecording:requested")
        try {
            recorder.start()
            mutableState.value = RecordingState.LISTENING
            logger.debug("startRecording:succeeded")
        } catch (error: Throwable) {
            logger.error("startRecording:failed", error)
            throw error
        }
    }

    suspend fun stopRecording() {
        require(mutableState.value == RecordingState.LISTENING) {
            "Recording can only stop from LISTENING"
        }

        logger.debug("stopRecording:requested")
        mutableState.value = RecordingState.PROCESSING

        var audioFile: File? = null
        var pendingError: Throwable? = null

        try {
            audioFile = recorder.stop()
            logger.debug("stopRecording:audioCaptured")
            val transcript = transcribeFile(audioFile)
            logger.debug("stopRecording:transcriptionSucceeded")
            val cleanedText = cleanText(transcript)
            logger.debug("stopRecording:cleanupSucceeded")
            insertText(cleanedText)
            logger.debug("stopRecording:insertionSucceeded")
        } catch (error: Throwable) {
            when {
                audioFile == null -> logger.error("stopRecording:audioCaptureFailed", error)
                pendingError == null -> logger.error("stopRecording:transcriptionFailed", error)
            }
            pendingError = error
        }

        audioFile?.let { file ->
            try {
                if (!deleteFile(file)) {
                    throw IllegalStateException("Failed to delete recorded audio file")
                }
                logger.debug("stopRecording:fileDeleted")
            } catch (deleteError: Throwable) {
                logger.error("stopRecording:deleteFailed", deleteError)
                if (pendingError == null) {
                    pendingError = deleteError
                } else {
                    pendingError.addSuppressed(deleteError)
                }
            }
        }

        pendingError?.let { error ->
            mutableState.value = RecordingState.ERROR
            logger.error("stopRecording:failed", error)
            throw error
        }

        mutableState.value = RecordingState.IDLE
        logger.debug("stopRecording:succeeded")
    }

    fun reset() {
        require(mutableState.value == RecordingState.ERROR) {
            "Coordinator can only reset from ERROR"
        }

        mutableState.value = RecordingState.IDLE
    }
}
