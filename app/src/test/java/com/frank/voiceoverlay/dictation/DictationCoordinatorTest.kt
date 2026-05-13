package com.frank.voiceoverlay.dictation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
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
}

private class FakeAudioRecorder : AudioRecorder {
    override suspend fun start(): Unit = Unit
    override suspend fun stop(): File = kotlin.io.path.createTempFile(suffix = ".m4a").toFile()
}
