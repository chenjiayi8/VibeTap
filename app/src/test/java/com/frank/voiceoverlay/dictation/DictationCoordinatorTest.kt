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
        val coordinator = DictationCoordinator(
            recorder = fakeRecorder,
            transcribe = { "um hello hello" },
            clean = { "hello" },
            insertText = { inserted += it },
            deleteFile = { true },
        )

        coordinator.startRecording()
        coordinator.stopRecording()

        assertEquals(RecordingState.IDLE, coordinator.state.value)
        assertEquals(listOf("hello"), inserted)
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
        val coordinator = DictationCoordinator(
            recorder = fakeRecorder,
            transcribe = { "um hello hello" },
            clean = { "hello" },
            insertText = {},
            deleteFile = { throw IllegalStateException("delete failed") },
        )

        coordinator.startRecording()

        val error = org.junit.Assert.assertThrows(IllegalStateException::class.java, ThrowingRunnable {
            kotlinx.coroutines.runBlocking { coordinator.stopRecording() }
        })

        assertEquals("delete failed", error.message)
        assertEquals(RecordingState.ERROR, coordinator.state.value)
    }
}

private class FakeAudioRecorder : AudioRecorder {
    override suspend fun start(): Unit = Unit
    override suspend fun stop(): File = kotlin.io.path.createTempFile(suffix = ".m4a").toFile()
}
