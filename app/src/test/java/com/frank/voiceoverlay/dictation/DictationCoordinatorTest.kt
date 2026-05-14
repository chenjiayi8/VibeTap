package com.frank.voiceoverlay.dictation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.function.ThrowingRunnable
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DictationCoordinatorTest {
    @Test
    fun stopRecording_runsTranscriptionCleanupAndInsertion() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val inserted = mutableListOf<String>()
        val logger = FakeDictationLogger()
        val coordinator = DictationCoordinator(
            recorder = fakeRecorder,
            transcribe = { "um hello hello" },
            clean = { "hello" },
            insertText = { inserted += it },
            deleteFile = { true },
            logger = logger,
        )

        coordinator.startRecording()
        coordinator.stopRecording()

        assertEquals(RecordingState.IDLE, coordinator.state.value)
        assertEquals(listOf("hello"), inserted)
        assertEquals(
            listOf(
                "startRecording:requested",
                "startRecording:succeeded",
                "stopRecording:requested",
                "stopRecording:audioCaptured",
                "stopRecording:transcriptionSucceeded",
                "stopRecording:cleanupSucceeded",
                "stopRecording:insertionSucceeded",
                "stopRecording:fileDeleted",
                "stopRecording:succeeded",
            ),
            logger.events,
        )
    }

    @Test
    fun stopRecording_deletesFileBeforeReturningToIdle() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val statesDuringDelete = mutableListOf<RecordingState>()
        lateinit var coordinator: DictationCoordinator
        coordinator = DictationCoordinator(
            recorder = fakeRecorder,
            transcribe = { "um hello hello" },
            clean = { "hello" },
            insertText = {},
            deleteFile = {
                statesDuringDelete += coordinator.state.value
                true
            },
        )

        coordinator.startRecording()
        coordinator.stopRecording()

        assertEquals(listOf(RecordingState.PROCESSING), statesDuringDelete)
        assertEquals(RecordingState.IDLE, coordinator.state.value)
    }

    @Test
    fun stopRecording_setsErrorAndRethrowsWhenDeleteFails() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val logger = FakeDictationLogger()
        val coordinator = DictationCoordinator(
            recorder = fakeRecorder,
            transcribe = { "um hello hello" },
            clean = { "hello" },
            insertText = {},
            deleteFile = { throw IllegalStateException("delete failed") },
            logger = logger,
        )

        coordinator.startRecording()

        val error = org.junit.Assert.assertThrows(IllegalStateException::class.java, ThrowingRunnable {
            kotlinx.coroutines.runBlocking { coordinator.stopRecording() }
        })

        assertEquals("delete failed", error.message)
        assertEquals(RecordingState.ERROR, coordinator.state.value)
        org.junit.Assert.assertTrue(
            logger.events.contains("stopRecording:deleteFailed:delete failed"),
        )
    }

    @Test
    fun stopRecording_cleansUpTempFileAndSetsErrorWhenTranscriptionFails() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val expectedFile = fakeRecorder.recordedFile
        val deletedFiles = mutableListOf<File>()
        val logger = FakeDictationLogger()
        val coordinator = DictationCoordinator(
            recorder = fakeRecorder,
            transcribeFile = { throw IllegalStateException("transcription failed") },
            cleanText = { "hello" },
            insertText = {},
            deleteFile = {
                deletedFiles += it
                true
            },
            logger = logger,
        )

        coordinator.startRecording()

        val error = org.junit.Assert.assertThrows(IllegalStateException::class.java, ThrowingRunnable {
            kotlinx.coroutines.runBlocking { coordinator.stopRecording() }
        })

        assertEquals("transcription failed", error.message)
        assertEquals(listOf(expectedFile), deletedFiles)
        assertEquals(RecordingState.ERROR, coordinator.state.value)
        assertEquals(
            listOf(
                "startRecording:requested",
                "startRecording:succeeded",
                "stopRecording:requested",
                "stopRecording:audioCaptured",
                "stopRecording:transcriptionFailed:transcription failed",
                "stopRecording:fileDeleted",
                "stopRecording:failed:transcription failed",
            ),
            logger.events,
        )
    }

    @Test
    fun stopRecording_logsCleanupFailureSeparately() = runTest {
        val logger = FakeDictationLogger()
        val coordinator = DictationCoordinator(
            recorder = FakeAudioRecorder(),
            transcribe = { "um hello hello" },
            clean = { throw IllegalStateException("cleanup failed") },
            insertText = {},
            deleteFile = { true },
            logger = logger,
        )

        coordinator.startRecording()

        val error = org.junit.Assert.assertThrows(IllegalStateException::class.java, ThrowingRunnable {
            kotlinx.coroutines.runBlocking { coordinator.stopRecording() }
        })

        assertEquals("cleanup failed", error.message)
        assertEquals(RecordingState.ERROR, coordinator.state.value)
        assertEquals(
            listOf(
                "startRecording:requested",
                "startRecording:succeeded",
                "stopRecording:requested",
                "stopRecording:audioCaptured",
                "stopRecording:transcriptionSucceeded",
                "stopRecording:cleanupFailed:cleanup failed",
                "stopRecording:fileDeleted",
                "stopRecording:failed:cleanup failed",
            ),
            logger.events,
        )
    }

    @Test
    fun stopRecording_logsInsertionFailureSeparately() = runTest {
        val logger = FakeDictationLogger()
        val coordinator = DictationCoordinator(
            recorder = FakeAudioRecorder(),
            transcribe = { "um hello hello" },
            clean = { "hello" },
            insertText = { throw IllegalStateException("insertion failed") },
            deleteFile = { true },
            logger = logger,
        )

        coordinator.startRecording()

        val error = org.junit.Assert.assertThrows(IllegalStateException::class.java, ThrowingRunnable {
            kotlinx.coroutines.runBlocking { coordinator.stopRecording() }
        })

        assertEquals("insertion failed", error.message)
        assertEquals(RecordingState.ERROR, coordinator.state.value)
        assertEquals(
            listOf(
                "startRecording:requested",
                "startRecording:succeeded",
                "stopRecording:requested",
                "stopRecording:audioCaptured",
                "stopRecording:transcriptionSucceeded",
                "stopRecording:cleanupSucceeded",
                "stopRecording:insertionFailed:insertion failed",
                "stopRecording:fileDeleted",
                "stopRecording:failed:insertion failed",
            ),
            logger.events,
        )
    }

    @Test
    fun stopRecording_treatsFalseDeleteResultAsFailure() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val coordinator = DictationCoordinator(
            recorder = fakeRecorder,
            transcribe = { "um hello hello" },
            clean = { "hello" },
            insertText = {},
            deleteFile = { false },
        )

        coordinator.startRecording()

        val error = org.junit.Assert.assertThrows(IllegalStateException::class.java, ThrowingRunnable {
            kotlinx.coroutines.runBlocking { coordinator.stopRecording() }
        })

        assertEquals("Failed to delete recorded audio file", error.message)
        assertEquals(RecordingState.ERROR, coordinator.state.value)
    }

    @Test
    fun startRecording_requiresIdleState() = runTest {
        val coordinator = DictationCoordinator(
            recorder = FakeAudioRecorder(),
            transcribe = { "um hello hello" },
            clean = { "hello" },
            insertText = {},
            deleteFile = { true },
        )

        coordinator.startRecording()

        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java, ThrowingRunnable {
            kotlinx.coroutines.runBlocking { coordinator.startRecording() }
        })

        assertEquals("Recording can only start from IDLE", error.message)
        assertEquals(RecordingState.LISTENING, coordinator.state.value)
    }

    @Test
    fun stopRecording_requiresListeningState() = runTest {
        val coordinator = DictationCoordinator(
            recorder = FakeAudioRecorder(),
            transcribe = { "um hello hello" },
            clean = { "hello" },
            insertText = {},
            deleteFile = { true },
        )

        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java, ThrowingRunnable {
            kotlinx.coroutines.runBlocking { coordinator.stopRecording() }
        })

        assertEquals("Recording can only stop from LISTENING", error.message)
        assertEquals(RecordingState.IDLE, coordinator.state.value)
    }

    @Test
    fun reset_requiresErrorState() = runTest {
        val coordinator = DictationCoordinator(
            recorder = FakeAudioRecorder(),
            transcribe = { "um hello hello" },
            clean = { "hello" },
            insertText = {},
            deleteFile = { true },
        )

        val error = org.junit.Assert.assertThrows(IllegalArgumentException::class.java, ThrowingRunnable {
            coordinator.reset()
        })

        assertEquals("Coordinator can only reset from ERROR", error.message)
        assertEquals(RecordingState.IDLE, coordinator.state.value)
    }

    @Test
    fun reset_afterError_returnsToIdleAndAllowsRetry() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val inserted = mutableListOf<String>()
        var shouldFail = true
        val coordinator = DictationCoordinator(
            recorder = fakeRecorder,
            transcribe = {
                if (shouldFail) throw IllegalStateException("transcription failed")
                "um hello hello"
            },
            clean = { "hello" },
            insertText = { inserted += it },
            deleteFile = { true },
        )

        coordinator.startRecording()
        org.junit.Assert.assertThrows(IllegalStateException::class.java, ThrowingRunnable {
            kotlinx.coroutines.runBlocking { coordinator.stopRecording() }
        })
        assertEquals(RecordingState.ERROR, coordinator.state.value)

        coordinator.reset()
        assertEquals(RecordingState.IDLE, coordinator.state.value)

        shouldFail = false
        coordinator.startRecording()
        coordinator.stopRecording()

        assertEquals(listOf("hello"), inserted)
        assertEquals(RecordingState.IDLE, coordinator.state.value)
    }
}

private class FakeDictationLogger : DictationLogger {
    val events = mutableListOf<String>()

    override fun debug(event: String) {
        events += event
    }

    override fun error(event: String, throwable: Throwable) {
        events += "$event:${throwable.message}"
    }
}

private class FakeAudioRecorder(
    var recordedFile: File = kotlin.io.path.createTempFile(suffix = ".m4a").toFile(),
) : AudioRecorder {
    override suspend fun start(): Unit = Unit

    override suspend fun stop(): File {
        return recordedFile.also {
            recordedFile = kotlin.io.path.createTempFile(suffix = ".m4a").toFile()
        }
    }
}
