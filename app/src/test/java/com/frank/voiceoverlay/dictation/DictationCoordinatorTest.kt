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
    fun stopRecording_fallsBackToLiteralTranscript_whenCleanupLooksAssistantLike() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val inserted = mutableListOf<String>()
        val coordinator = DictationCoordinator(
            recorder = fakeRecorder,
            transcribeFile = { "please check the PR and give me some suggestions" },
            cleanText = { "Q: Please check the PR.\nA: Here are some suggestions." },
            insertText = { inserted += it },
            deleteFile = { true },
        )

        coordinator.startRecording()
        coordinator.stopRecording()

        assertEquals(
            listOf("please check the PR and give me some suggestions"),
            inserted,
        )
    }

    @Test
    fun stopRecording_preservesRawSpacingAndNewlines_whenAssistantLikeCleanupIsRejected() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val inserted = mutableListOf<String>()
        val rawTranscript = "  hello\n\nworld  "
        val coordinator = DictationCoordinator(
            recorder = fakeRecorder,
            transcribeFile = { rawTranscript },
            cleanText = { "Here are some suggestions" },
            insertText = { inserted += it },
            deleteFile = { true },
        )

        coordinator.startRecording()
        coordinator.stopRecording()

        assertEquals(listOf(rawTranscript), inserted)
    }

    @Test
    fun stopRecording_fallsBackToBlankTranscript_whenTranscriptHasNoWords() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val inserted = mutableListOf<String>()
        val rawTranscript = "   "
        val coordinator = DictationCoordinator(
            recorder = fakeRecorder,
            transcribeFile = { rawTranscript },
            cleanText = { "hello there" },
            insertText = { inserted += it },
            deleteFile = { true },
        )

        coordinator.startRecording()
        coordinator.stopRecording()

        assertEquals(listOf(rawTranscript), inserted)
    }

    @Test
    fun finalize_acceptsPunctuationOnlyCleanup() {
        assertEquals(
            "hello world",
            DictationCleanupPolicy.finalize(
                rawTranscript = "hello, world",
                cleanedCandidate = "hello world",
            ),
        )
    }

    @Test
    fun finalize_acceptsCasingOnlyCleanup() {
        assertEquals(
            "Hello world",
            DictationCleanupPolicy.finalize(
                rawTranscript = "hello world",
                cleanedCandidate = "Hello world",
            ),
        )
    }

    @Test
    fun finalize_fallsBackWhenOverlapIsTooLow() {
        assertEquals(
            "please check the PR and give me some suggestions",
            DictationCleanupPolicy.finalize(
                rawTranscript = "please check the PR and give me some suggestions",
                cleanedCandidate = "totally different rewrite text",
            ),
        )
    }

    @Test
    fun finalize_fallsBackWhenCandidateIsTooLong() {
        assertEquals(
            "alpha beta",
            DictationCleanupPolicy.finalize(
                rawTranscript = "alpha beta",
                cleanedCandidate = "alpha beta gamma delta epsilon zeta eta",
            ),
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

    @Test
    fun stopRecording_cleansUpTempFileAndSetsErrorWhenTranscriptionFails() = runTest {
        val fakeRecorder = FakeAudioRecorder()
        val expectedFile = fakeRecorder.recordedFile
        val deletedFiles = mutableListOf<File>()
        val coordinator = DictationCoordinator(
            recorder = fakeRecorder,
            transcribeFile = { throw IllegalStateException("transcription failed") },
            cleanText = { "hello" },
            insertText = {},
            deleteFile = {
                deletedFiles += it
                true
            },
        )

        coordinator.startRecording()

        val error = org.junit.Assert.assertThrows(IllegalStateException::class.java, ThrowingRunnable {
            kotlinx.coroutines.runBlocking { coordinator.stopRecording() }
        })

        assertEquals("transcription failed", error.message)
        assertEquals(listOf(expectedFile), deletedFiles)
        assertEquals(RecordingState.ERROR, coordinator.state.value)
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
