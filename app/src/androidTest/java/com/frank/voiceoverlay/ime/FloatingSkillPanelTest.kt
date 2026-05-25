package com.frank.voiceoverlay.ime

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.ime.ui.FloatingSkillPanel
import com.frank.voiceoverlay.ime.ui.VibeTapImeRoot
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import com.frank.voiceoverlay.testing.TestComposeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloatingSkillPanelTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<TestComposeActivity>()

    @Test
    fun collapsedOrbitShowsOnlyMicAndNoModeSwitch() {
        var micTapCount by mutableIntStateOf(0)
        var micDoubleTapCount by mutableIntStateOf(0)

        composeRule.setContent {
            MaterialTheme {
                FloatingSkillPanel(
                    orbitExpanded = false,
                    innerRingBubbles = emptyList(),
                    outerRingBubbles = emptyList(),
                    canPageBackward = false,
                    canPageForward = false,
                    statusMessage = null,
                    statusTone = ImeStatusTone.Neutral,
                    onMicTapped = { micTapCount += 1 },
                    onMicDoubleTapped = { micDoubleTapCount += 1 },
                    onDockTapped = {},
                    onSkillBubbleTapped = {},
                    onPreviousPageTapped = {},
                    onNextPageTapped = {},
                )
            }
        }

        composeRule.onNodeWithTag("floating_skill_panel_viewport").assertIsDisplayed()
        composeRule.onNodeWithTag("bubble_primary_mic").assertIsDisplayed().assertHasClickAction()
        composeRule.onAllNodesWithTag("bubble_mode_switch").assertCountEquals(0)
        composeRule.onAllNodesWithTag("bubble_page_next").assertCountEquals(0)
        composeRule.onAllNodesWithTag("bubble_page_prev").assertCountEquals(0)

        composeRule.onNodeWithTag("bubble_primary_mic").performTouchInput { doubleClick() }

        composeRule.runOnIdle {
            check(micTapCount == 0) { "Expected no single tap during double tap gesture, was $micTapCount" }
            check(micDoubleTapCount == 1) { "Expected one mic double tap, was $micDoubleTapCount" }
        }
    }

    @Test
    fun expandedOrbitShowsMicKeyboardSwitchInnerOuterAndPageNextWhenApplicable() {
        var dockTapCount by mutableIntStateOf(0)
        var previousPageTapCount by mutableIntStateOf(0)
        var nextPageTapCount by mutableIntStateOf(0)
        var tappedSkillLabel by mutableStateOf<String?>(null)

        composeRule.setContent {
            MaterialTheme {
                FloatingSkillPanel(
                    orbitExpanded = true,
                    innerRingBubbles = listOf(
                        preset("ship", "Ship", 0),
                        preset("review", "Review", 1),
                    ),
                    outerRingBubbles = listOf(
                        preset("proceed", "Proceed", 2),
                        preset("extra", "Extra", 3),
                    ),
                    canPageBackward = false,
                    canPageForward = true,
                    statusMessage = "Listening for your next instruction",
                    statusTone = ImeStatusTone.Warning,
                    onMicTapped = {},
                    onMicDoubleTapped = {},
                    onDockTapped = { dockTapCount += 1 },
                    onSkillBubbleTapped = { preset -> tappedSkillLabel = preset.label },
                    onPreviousPageTapped = { previousPageTapCount += 1 },
                    onNextPageTapped = { nextPageTapCount += 1 },
                )
            }
        }

        listOf(
            "bubble_primary_mic",
            "bubble_mode_switch",
            "bubble_skill_ship",
            "bubble_skill_review",
            "bubble_skill_proceed",
            "bubble_skill_extra",
            "bubble_page_next",
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsDisplayed().assertHasClickAction()
        }
        composeRule.onAllNodesWithTag("bubble_page_prev").assertCountEquals(0)

        composeRule.onNodeWithTag("bubble_mode_switch").performClick()
        composeRule.onNodeWithTag("bubble_skill_ship").performClick()
        composeRule.onNodeWithTag("bubble_page_next").performClick()

        composeRule.runOnIdle {
            check(dockTapCount == 1) { "Expected dock tap once, was $dockTapCount" }
            check(previousPageTapCount == 0) { "Expected previous page untouched, was $previousPageTapCount" }
            check(nextPageTapCount == 1) { "Expected next page tap once, was $nextPageTapCount" }
            check(tappedSkillLabel == "Ship") { "Expected Ship callback, was $tappedSkillLabel" }
        }
    }

    @Test
    fun imeRootFloatingModeStartsCollapsedAndExpandsOrbitOnMicDoubleTap() {
        val controller = createController(
            presets = listOf(
                preset("ship", "Ship", 0, isPinned = true),
                preset("review", "Review", 1, isPinned = true),
                preset("proceed", "Proceed", 2),
                preset("extra", "Extra", 3),
            ),
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        controller.bind(scope)

        composeRule.setContent {
            MaterialTheme {
                VibeTapImeRoot(
                    controller = controller,
                    onBackspace = { true },
                    onEnter = { true },
                    onCommitLetter = { true },
                    onCommitPhrase = { true },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            controller.uiState.value.skillBubbles.size == 4
        }
        composeRule.runOnIdle {
            controller.onLayoutToggle()
        }

        composeRule.onNodeWithTag("floating_skill_panel_viewport").assertIsDisplayed()
        composeRule.onNodeWithTag("bubble_primary_mic").assertIsDisplayed()
        composeRule.onAllNodesWithTag("bubble_mode_switch").assertCountEquals(0)
        composeRule.runOnIdle {
            check(!controller.uiState.value.orbitExpanded) {
                "Expected floating orbit to start collapsed"
            }
        }

        composeRule.onNodeWithTag("bubble_primary_mic").performTouchInput { doubleClick() }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            controller.uiState.value.orbitExpanded
        }
        listOf(
            "bubble_mode_switch",
            "bubble_skill_ship",
            "bubble_skill_review",
            "bubble_skill_proceed",
            "bubble_skill_extra",
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }

        composeRule.runOnIdle {
            check(controller.uiState.value.layoutMode == KeyboardLayoutMode.FLOATING) {
                "Expected layout mode to remain floating"
            }
            check(controller.uiState.value.orbitExpanded) {
                "Expected controller orbit state to be expanded after mic double tap"
            }
        }

        scope.cancel()
    }

    private fun createController(
        presets: List<ShortcutPreset>,
    ): VoiceKeyboardController = VoiceKeyboardController(
        recordingState = MutableStateFlow(RecordingState.IDLE),
        shortcutsProvider = { presets },
        startRecording = {},
        stopRecording = {},
        resetRecording = {},
        commitPhrase = { true },
    )

    private fun preset(id: String, label: String, order: Int, isPinned: Boolean = false): ShortcutPreset = ShortcutPreset(
        id = id,
        label = label,
        text = label,
        order = order,
        isPinned = isPinned,
    )
}
