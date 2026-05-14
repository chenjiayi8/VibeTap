package com.frank.voiceoverlay.ime

import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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

        val bindingScope = createBindingScope(testScheduler)
        val controller = environment.createController(bindingScope)
        advanceUntilIdle()

        assertEquals(KeyboardLayoutMode.DOCKED, controller.uiState.value.layoutMode)
        assertEquals(RecordingState.LISTENING, controller.uiState.value.recordingState)
        assertEquals(listOf("Zero", "One", "Two"), controller.uiState.value.skillBubbles.map { it.label })
        assertEquals(null, controller.uiState.value.statusMessage)
        bindingScope.cancel()
    }

    @Test
    fun bind_refreshesShortcutsOnSubsequentInvocations_and_surfacesShortcutLoadingFailure() = runTest {
        val firstPreset = ShortcutPreset(id = "first", label = "First", text = "1", order = 1)
        val refreshedPreset = ShortcutPreset(id = "refreshed", label = "Refreshed", text = "2", order = 0)
        val recoveredPreset = ShortcutPreset(id = "recovered", label = "Recovered", text = "3", order = 2)
        val environment = FakeImeEnvironment(
            initialRecordingState = RecordingState.IDLE,
            shortcutsSequence = listOf(
                listOf(firstPreset),
                listOf(refreshedPreset),
                listOf(recoveredPreset),
                listOf(recoveredPreset),
            ),
        )
        val bindingScope = createBindingScope(testScheduler)
        val controller = environment.createControllerWithoutBinding()

        controller.bind(bindingScope)
        advanceUntilIdle()
        assertEquals(listOf("First"), controller.uiState.value.skillBubbles.map { it.label })

        controller.bind(bindingScope)
        advanceUntilIdle()
        assertEquals(2, environment.shortcutsProviderCalls)
        assertEquals(listOf("Refreshed"), controller.uiState.value.skillBubbles.map { it.label })
        assertEquals(RecordingState.IDLE, controller.uiState.value.recordingState)

        environment.shortcutsFailureMessage = "Shortcuts unavailable"
        controller.bind(bindingScope)
        advanceUntilIdle()

        assertEquals(3, environment.shortcutsProviderCalls)
        assertEquals("Shortcuts unavailable", controller.uiState.value.statusMessage)
        assertEquals(listOf("Refreshed"), controller.uiState.value.skillBubbles.map { it.label })

        environment.shortcutsFailureMessage = null
        controller.bind(bindingScope)
        advanceUntilIdle()

        assertEquals(4, environment.shortcutsProviderCalls)
        assertEquals(null, controller.uiState.value.statusMessage)
        assertEquals(listOf("Recovered"), controller.uiState.value.skillBubbles.map { it.label })
        bindingScope.cancel()
    }

    @Test
    fun bind_ignoresStaleShortcutRefreshResults() = runTest {
        val stalePreset = ShortcutPreset(id = "stale", label = "Stale", text = "1", order = 1)
        val freshPreset = ShortcutPreset(id = "fresh", label = "Fresh", text = "2", order = 0)
        val environment = FakeImeEnvironment(
            shortcutsSequence = listOf(
                listOf(stalePreset),
                listOf(freshPreset),
            ),
            shortcutsProviderDelayMillisSequence = listOf(100L, 0L),
        )
        val bindingScope = createBindingScope(testScheduler)
        val controller = environment.createControllerWithoutBinding()

        controller.bind(bindingScope)
        runCurrent()
        controller.bind(bindingScope)
        advanceUntilIdle()

        assertEquals(2, environment.shortcutsProviderCalls)
        assertEquals(listOf("Fresh"), controller.uiState.value.skillBubbles.map { it.label })
        assertEquals(null, controller.uiState.value.statusMessage)
        bindingScope.cancel()
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
    fun recoverableHandlers_doNotConvertJvmErrorsIntoStatusMessages() = runTest {
        val micError = AssertionError("boom")
        val resetError = AssertionError("reset boom")
        val commitError = AssertionError("commit boom")
        val preset = ShortcutPreset(id = "ship", label = "Ship", text = "Ship it", order = 0)

        val micEnvironment = FakeImeEnvironment(startFailure = micError)
        val micController = micEnvironment.createController(backgroundScope)
        advanceUntilIdle()
        val thrownMicError = captureError { micController.onMicTapped() }
        assertEquals(micError, thrownMicError)
        assertEquals(null, micController.uiState.value.statusMessage)

        val resetEnvironment = FakeImeEnvironment(resetFailure = resetError)
        val resetController = resetEnvironment.createController(backgroundScope)
        resetEnvironment.recordingState.value = RecordingState.ERROR
        advanceUntilIdle()
        val thrownResetError = captureError { resetController.onMicTapped() }
        assertEquals(resetError, thrownResetError)
        assertEquals(null, resetController.uiState.value.statusMessage)

        val commitEnvironment = FakeImeEnvironment(
            shortcuts = listOf(preset),
            commitPhraseFailure = commitError,
        )
        val commitController = commitEnvironment.createController(backgroundScope)
        advanceUntilIdle()
        val thrownCommitError = captureError { commitController.onSkillBubbleTapped(preset) }
        assertEquals(commitError, thrownCommitError)
        assertEquals(null, commitController.uiState.value.statusMessage)
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

    @Test
    fun onSkillBubbleTapped_surfacesCommitFailure_andRethrowsCancellation() = runTest {
        val preset = ShortcutPreset(id = "ship", label = "Ship", text = "Ship it", order = 0)
        val failureEnvironment = FakeImeEnvironment(
            shortcuts = listOf(preset),
            commitPhraseFailure = IllegalStateException("Insertion failed"),
        )
        val failureController = failureEnvironment.createController(backgroundScope)
        advanceUntilIdle()

        failureController.onSkillBubbleTapped(preset)
        assertEquals("Insertion failed", failureController.uiState.value.statusMessage)
        assertEquals(listOf("Ship it"), failureEnvironment.committedPhrases)

        val cancellation = CancellationException("cancel insert")
        val cancellationEnvironment = FakeImeEnvironment(
            shortcuts = listOf(preset),
            commitPhraseFailure = cancellation,
        )
        val cancellationController = cancellationEnvironment.createController(backgroundScope)
        advanceUntilIdle()

        val thrown = captureCancellation { cancellationController.onSkillBubbleTapped(preset) }
        assertEquals(cancellation, thrown)
        assertEquals(null, cancellationController.uiState.value.statusMessage)
    }
    private fun createBindingScope(testScheduler: kotlinx.coroutines.test.TestCoroutineScheduler): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job())

}

private suspend fun captureCancellation(block: suspend () -> Unit): CancellationException {
    return try {
        block()
        throw AssertionError("Expected CancellationException")
    } catch (error: CancellationException) {
        error
    }
}

private suspend fun captureError(block: suspend () -> Unit): Error {
    return try {
        block()
        throw AssertionError("Expected Error")
    } catch (error: Error) {
        error
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class FakeImeEnvironment(
    initialRecordingState: RecordingState = RecordingState.IDLE,
    private val shortcuts: List<ShortcutPreset> = emptyList(),
    private val shortcutsSequence: List<List<ShortcutPreset>>? = null,
    private val shortcutsProviderDelayMillisSequence: List<Long>? = null,
    var shortcutsFailureMessage: String? = null,
    private val startFailure: Throwable? = null,
    private val resetFailure: Throwable? = null,
    private val commitPhraseFailure: Throwable? = null,
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
        return createControllerWithoutBinding().also {
            it.bind(scope)
        }
    }

    fun createControllerWithoutBinding(): VoiceKeyboardController {
        return VoiceKeyboardController(
            recordingState = recordingState,
            shortcutsProvider = {
                shortcutsProviderCalls += 1
                shortcutsProviderDelayMillisSequence
                    ?.getOrNull(shortcutsProviderCalls - 1)
                    ?.let { delay(it) }
                shortcutsFailureMessage?.let { throw IllegalStateException(it) }
                shortcutsSequence?.getOrNull(shortcutsProviderCalls - 1) ?: shortcuts
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
                commitPhraseFailure?.let { throw it }
                commitPhraseResult
            },
        )
    }
}
