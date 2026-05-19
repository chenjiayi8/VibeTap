package com.frank.voiceoverlay.ime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.ime.ui.FloatingSkillPanel
import com.frank.voiceoverlay.ime.ui.VibeTapImeRoot
import com.frank.voiceoverlay.ime.ui.VibeTapTheme
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
    fun floatingSkillPanelShowsPrimaryMicKeyboardSwitchAndThreeActionBubbles() {
        var micTapCount by mutableIntStateOf(0)
        var dockTapCount by mutableIntStateOf(0)
        var tappedSkillLabel by mutableStateOf<String?>(null)

        composeRule.setContent {
            VibeTapTheme {
                FloatingSkillPanel(
                    skillBubbles = listOf(
                        preset("ship-pr", "Ship PR", 0),
                        preset("review-pr", "Review PR", 1),
                        preset("proceed", "Proceed", 2),
                        preset("extra", "Extra", 3),
                    ),
                    statusMessage = "Listening for your next instruction",
                    onMicTapped = { micTapCount += 1 },
                    onDockTapped = { dockTapCount += 1 },
                    onSkillBubbleTapped = { preset -> tappedSkillLabel = preset.label },
                )
            }
        }

        composeRule.onNodeWithTag("bubble_primary_mic").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag("bubble_mode_switch").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag("bubble_skill_ship-pr").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag("bubble_skill_review-pr").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag("bubble_skill_proceed").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithText("Keyboard").assertIsDisplayed()
        composeRule.onNodeWithText("Listening for your next instruction").assertIsDisplayed()
        composeRule.onAllNodesWithText("Dock").assertCountEquals(0)
        composeRule.onAllNodesWithText("Extra").assertCountEquals(0)
        composeRule.onAllNodesWithText("Q").assertCountEquals(0)
        composeRule.onAllNodesWithText("Space").assertCountEquals(0)

        composeRule.onNodeWithTag("bubble_primary_mic").performClick()
        composeRule.onNodeWithTag("bubble_mode_switch").performClick()
        composeRule.onNodeWithTag("bubble_skill_ship-pr").performClick()

        composeRule.runOnIdle {
            check(micTapCount == 1) { "Expected Mic to be clicked once, was $micTapCount" }
            check(dockTapCount == 1) { "Expected Keyboard switch to be clicked once, was $dockTapCount" }
            check(tappedSkillLabel == "Ship PR") { "Expected Ship PR callback, was $tappedSkillLabel" }
        }
    }

    @Test
    fun imeRootFloatingModeDoesNotShowKeyboardLettersOrSpaceRow() {
        val controller = createController(
            presets = listOf(
                preset("ship-pr", "Ship PR", 0),
                preset("review-pr", "Review PR", 1),
                preset("proceed", "Proceed", 2),
            ),
        )

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        controller.bind(scope)

        composeRule.setContent {
            VibeTapTheme {
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
            controller.uiState.value.skillBubbles.size == 3
        }
        composeRule.runOnIdle { controller.onLayoutToggle() }

        composeRule.onNodeWithTag("bubble_primary_mic").assertIsDisplayed()
        composeRule.onNodeWithTag("bubble_mode_switch").assertIsDisplayed()
        composeRule.onNodeWithTag("bubble_skill_ship-pr").assertIsDisplayed()
        composeRule.onNodeWithTag("bubble_skill_review-pr").assertIsDisplayed()
        composeRule.onNodeWithTag("bubble_skill_proceed").assertIsDisplayed()
        composeRule.onAllNodesWithText("Q").assertCountEquals(0)
        composeRule.onAllNodesWithText("Space").assertCountEquals(0)
        composeRule.onAllNodesWithText("Float").assertCountEquals(0)

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

    private fun preset(id: String, label: String, order: Int): ShortcutPreset = ShortcutPreset(
        id = id,
        label = label,
        text = label,
        order = order,
    )
}
