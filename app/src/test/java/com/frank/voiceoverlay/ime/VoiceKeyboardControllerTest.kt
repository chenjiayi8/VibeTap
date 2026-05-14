package com.frank.voiceoverlay.ime

import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import java.util.concurrent.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceKeyboardControllerTest {
    @Test
    fun bind_loadsTopThreeShortcutsSortedByOrder_andMirrorsRecordingState() = runTest {
        val presets = listOf(
            ShortcutPreset(id = "four", label = "Four", text = "4", order = 4),
            ShortcutPreset(id = "two", label = "Two", text = "2", order = 2),
            ShortcutPreset(id = "one", label = "One", text = "1", order = 1),
            ShortcutPreset(id = "zero", label = "Zero", text = "0", order = 0),
        )
        val environment = FakeImeEnvironment(
            initialRecordingState = RecordingState.LISTENING,
            shortcuts = presets,
        )

        val controller = environment.createController(backgroundScope)
        advanceUntilIdle()

        assertEquals(KeyboardLayoutMode.DOCKED, controller.uiState.value.layoutMode)
        assertEquals(RecordingState.LISTENING, controller.uiState.value.recordingState)
        assertEquals(listOf("Zero", "One", "Two"), controller.uiState.value.skillBubbles.map { it.label })
        assertEquals(null, controller.uiState.value.statusMessage)
    }

    @Test
    fun bind_ignoresDuplicateInvocation_and_surfacesShortcutLoadingFailure() = runTest {
        val environment = FakeImeEnvironment(
            initialRecordingState = RecordingState.IDLE,
            shortcutsFailureMessage = "Shortcuts unavailable",
        )
        val controller = environment.createControllerWithoutBinding(backgroundScope)

        controller.bind(backgroundScope)
        controller.bind(backgroundScope)
        advanceUntilIdle()

        assertEquals(1, environment.shortcutsProviderCalls)
        assertEquals("Shortcuts unavailable", controller.uiState.value.statusMessage)
        assertEquals(emptyList<ShortcutPreset>(), controller.uiState.value.skillBubbles)
        assertEquals(RecordingState.IDLE, controller.uiState.value.recordingState)
    }

    @Test
    fun onLayoutToggle_togglesBetweenDockedAndFloating_andClearsStatus() = runTest {
        val environment = FakeImeEnvironment(initialRecordingState = RecordingState.PROCESSING)
        val controller = environment.createController(backgroundScope)
        advanceUntilIdle()

        controller.onMicTapped()
        assertEquals("Still processing the previous recording.", controller.uiState.value.statusMessage)

        controller.onLayoutToggle()
        assertEquals(KeyboardLayoutMode.FLOATING, controller.uiState.value.layoutMode)
        assertEquals(null, controller.uiState.value.statusMessage)

        controller.onLayoutToggle()
        assertEquals(KeyboardLayoutMode.DOCKED, controller.uiState.value.layoutMode)
        assertEquals(null, controller.uiState.value.statusMessage)
    }

    @Test
    fun onMicTapped_handlesStateTransitionsAndFailures() = runTest {
        val environment = FakeImeEnvironment()
        val controller = environment.createController(backgroundScope)
        advanceUntilIdle()

        controller.onMicTapped()
        assertEquals(1, environment.startCalls)
        assertEquals(null, controller.uiState.value.statusMessage)

        environment.recordingState.value = RecordingState.LISTENING
        advanceUntilIdle()
        controller.onMicTapped()
        assertEquals(1, environment.stopCalls)
        assertEquals(null, controller.uiState.value.statusMessage)

        environment.recordingState.value = RecordingState.ERROR
        advanceUntilIdle()
        controller.onMicTapped()
        assertEquals(1, environment.resetCalls)
        assertEquals(null, controller.uiState.value.statusMessage)

        environment.recordingState.value = RecordingState.IDLE
        environment.startFailureMessage = "Mic unavailable"
        advanceUntilIdle()
        controller.onMicTapped()
        assertEquals("Mic unavailable", controller.uiState.value.statusMessage)
    }

    @Test
    fun onMicTapped_rethrowsCancellationForMicAndResetPaths() = runTest {
        val startCancellation = CancellationException("stop starting")
        val resetCancellation = CancellationException("stop resetting")
        val environment = FakeImeEnvironment(
            startFailure = startCancellation,
            resetFailure = resetCancellation,
        )
        val controller = environment.createController(backgroundScope)
        advanceUntilIdle()

        val thrownWhileIdle = captureCancellation { controller.onMicTapped() }
        assertEquals(startCancellation, thrownWhileIdle)
        assertEquals(null, controller.uiState.value.statusMessage)

        environment.recordingState.value = RecordingState.ERROR
        advanceUntilIdle()

        val thrownWhileError = captureCancellation { controller.onMicTapped() }
        assertEquals(resetCancellation, thrownWhileError)
        assertEquals(null, controller.uiState.value.statusMessage)
    }

    @Test
    fun onSkillBubbleTapped_commitsPhrase_andReportsMissingField() = runTest {
        val preset = ShortcutPreset(id = "ship", label = "Ship", text = "Ship it", order = 0)
        val environment = FakeImeEnvironment(shortcuts = listOf(preset))
        val controller = environment.createController(backgroundScope)
        advanceUntilIdle()

        controller.onSkillBubbleTapped(preset)
        assertEquals(listOf("Ship it"), environment.committedPhrases)
        assertEquals(null, controller.uiState.value.statusMessage)

        environment.commitPhraseResult = false
        controller.onSkillBubbleTapped(preset)
        assertEquals(
            "No active text field for phrase insertion.",
            controller.uiState.value.statusMessage,
        )
    }
}

private suspend fun captureCancellation(block: suspend () -> Unit): CancellationException {
    return try {
        block()
        throw AssertionError("Expected CancellationException")
    } catch (error: CancellationException) {
        error
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class FakeImeEnvironment(
    initialRecordingState: RecordingState = RecordingState.IDLE,
    private val shortcuts: List<ShortcutPreset> = emptyList(),
    private val shortcutsFailureMessage: String? = null,
    private val startFailure: Throwable? = null,
    private val resetFailure: Throwable? = null,
) {
    val recordingState = MutableStateFlow(initialRecordingState)
    var startCalls = 0
    var stopCalls = 0
    var resetCalls = 0
    var commitPhraseCalls = 0
    var commitPhraseResult = true
    var startFailureMessage: String? = null
    var shortcutsProviderCalls = 0
    val committedPhrases = mutableListOf<String>()

    fun createController(scope: kotlinx.coroutines.CoroutineScope): VoiceKeyboardController {
        return createControllerWithoutBinding(scope).also {
            it.bind(scope)
        }
    }

    fun createControllerWithoutBinding(scope: kotlinx.coroutines.CoroutineScope): VoiceKeyboardController {
        return VoiceKeyboardController(
            recordingState = recordingState,
            shortcutsProvider = {
                shortcutsProviderCalls += 1
                shortcutsFailureMessage?.let { throw IllegalStateException(it) }
                shortcuts
            },
            startRecording = {
                startCalls += 1
                startFailure?.let { throw it }
                startFailureMessage?.let { throw IllegalStateException(it) }
                recordingState.value = RecordingState.LISTENING
            },
            stopRecording = {
                stopCalls += 1
                recordingState.value = RecordingState.IDLE
            },
            resetRecording = {
                resetCalls += 1
                resetFailure?.let { throw it }
                recordingState.value = RecordingState.IDLE
            },
            commitPhrase = { phrase ->
                commitPhraseCalls += 1
                committedPhrases += phrase
                commitPhraseResult
            },
        )
    }
}
