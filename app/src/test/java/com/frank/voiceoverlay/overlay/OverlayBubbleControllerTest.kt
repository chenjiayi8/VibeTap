package com.frank.voiceoverlay.overlay

import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.settings.AppSettings
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OverlayBubbleControllerTest {
    @Test
    fun singleTapAfterTimeout_startsRecordingWhenApiKeyPresent() = runTest {
        val environment = FakeOverlayEnvironment(
            settings = AppSettings(openAiApiKey = "key", presets = ShortcutPreset.defaultPresets()),
        )
        val controller = environment.createController(backgroundScope)

        val command = controller.onBubbleTap(timestampMillis = 1_000)
        assertEquals(OverlayBubbleCommand.ScheduleSingleTapResolution(251), command)

        controller.resolvePendingBubbleTap(timestampMillis = 1_251)
        advanceUntilIdle()

        assertEquals(1, environment.startCalls)
        assertEquals(RecordingState.LISTENING, controller.uiState.value.recordingState)
        assertEquals(BubbleInteractionState.Collapsed, controller.uiState.value.interactionState)
        assertEquals(null, controller.uiState.value.statusMessage)
    }

    @Test
    fun doubleTap_expandsShortcutsWithoutStartingRecording() = runTest {
        val environment = FakeOverlayEnvironment(
            settings = AppSettings(openAiApiKey = "key", presets = ShortcutPreset.defaultPresets()),
        )
        val controller = environment.createController(backgroundScope)

        val firstCommand = controller.onBubbleTap(timestampMillis = 1_000)
        val secondCommand = controller.onBubbleTap(timestampMillis = 1_180)
        advanceUntilIdle()

        assertEquals(OverlayBubbleCommand.ScheduleSingleTapResolution(251), firstCommand)
        assertEquals(OverlayBubbleCommand.CancelPendingSingleTapResolution, secondCommand)
        assertEquals(0, environment.startCalls)
        assertEquals(BubbleInteractionState.ShortcutsExpanded, controller.uiState.value.interactionState)
        assertEquals(listOf("Ship-PR", "Review", "Proceed"), controller.uiState.value.shortcuts.map { it.label })
    }

    @Test
    fun singleTapWithoutApiKey_setsRecoverableStatusMessage() = runTest {
        val environment = FakeOverlayEnvironment(settings = AppSettings(openAiApiKey = "", presets = ShortcutPreset.defaultPresets()))
        val controller = environment.createController(backgroundScope)

        controller.onBubbleTap(timestampMillis = 1_000)
        controller.resolvePendingBubbleTap(timestampMillis = 1_251)
        advanceUntilIdle()

        assertEquals(0, environment.startCalls)
        assertEquals(
            "Add an OpenAI API key in settings before recording.",
            controller.uiState.value.statusMessage,
        )
        assertEquals(RecordingState.IDLE, controller.uiState.value.recordingState)
    }

    @Test
    fun shortcutInsertionFailure_setsRecoverableStatusMessage() = runTest {
        val customPreset = ShortcutPreset("custom", "Custom", "Hello", 0)
        val environment = FakeOverlayEnvironment(
            settings = AppSettings(openAiApiKey = "key", presets = listOf(customPreset)),
            insertShortcutResult = false,
        )
        val controller = environment.createController(backgroundScope)

        controller.onBubbleTap(timestampMillis = 1_000)
        controller.onBubbleTap(timestampMillis = 1_180)
        controller.onShortcutTap(customPreset)
        advanceUntilIdle()

        assertEquals(1, environment.insertShortcutCalls)
        assertEquals(BubbleInteractionState.Collapsed, controller.uiState.value.interactionState)
        assertTrue(controller.uiState.value.statusMessage!!.contains("Enable the accessibility service"))
    }

    @Test
    fun singleTapFromError_resetsCoordinatorState() = runTest {
        val environment = FakeOverlayEnvironment(
            settings = AppSettings(openAiApiKey = "key", presets = ShortcutPreset.defaultPresets()),
            initialRecordingState = RecordingState.ERROR,
        )
        val controller = environment.createController(backgroundScope)

        controller.onBubbleTap(timestampMillis = 1_000)
        controller.resolvePendingBubbleTap(timestampMillis = 1_251)
        advanceUntilIdle()

        assertEquals(1, environment.resetCalls)
        assertEquals(RecordingState.IDLE, controller.uiState.value.recordingState)
        assertEquals(null, controller.uiState.value.statusMessage)
    }

    @Test
    fun refreshSettings_updatesShortcutsUsedByExpandedMenu() = runTest {
        val initialPreset = ShortcutPreset("initial", "Initial", "One", 0)
        val refreshedPreset = ShortcutPreset("refreshed", "Refreshed", "Two", 0)
        val environment = FakeOverlayEnvironment(
            settings = AppSettings(openAiApiKey = "", presets = listOf(initialPreset)),
        )
        val controller = environment.createController(backgroundScope)

        environment.settings = AppSettings(openAiApiKey = "key", presets = listOf(refreshedPreset))
        controller.onBubbleTap(timestampMillis = 1_000)
        controller.onBubbleTap(timestampMillis = 1_180)
        advanceUntilIdle()

        assertEquals(listOf("Refreshed"), controller.uiState.value.shortcuts.map { it.label })
        controller.onBubbleTap(timestampMillis = 2_000)
        controller.onBubbleTap(timestampMillis = 3_000)
        controller.resolvePendingBubbleTap(timestampMillis = 3_251)
        advanceUntilIdle()
        assertEquals(1, environment.startCalls)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class FakeOverlayEnvironment(
    var settings: AppSettings,
    initialRecordingState: RecordingState = RecordingState.IDLE,
    private val insertShortcutResult: Boolean = true,
) {
    val recordingState = MutableStateFlow(initialRecordingState)
    var startCalls = 0
    var stopCalls = 0
    var resetCalls = 0
    var insertShortcutCalls = 0

    fun createController(scope: kotlinx.coroutines.CoroutineScope): OverlayBubbleController {
        return OverlayBubbleController(
            recordingState = recordingState,
            refreshSettingsSnapshot = { settings },
            startRecording = {
                startCalls += 1
                recordingState.value = RecordingState.LISTENING
            },
            stopRecording = {
                stopCalls += 1
                recordingState.value = RecordingState.IDLE
            },
            resetRecording = {
                resetCalls += 1
                recordingState.value = RecordingState.IDLE
            },
            insertShortcutText = {
                insertShortcutCalls += 1
                insertShortcutResult
            },
        ).also {
            it.bind(scope)
        }
    }
}
