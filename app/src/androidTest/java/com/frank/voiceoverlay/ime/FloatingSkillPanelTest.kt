package com.frank.voiceoverlay.ime

import androidx.compose.material3.MaterialTheme
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
    fun floatingSkillPanelShowsMicDockAndOnlyThreeSkillBubbles() {
        var micTapCount by mutableIntStateOf(0)
        var dockTapCount by mutableIntStateOf(0)
        var tappedSkillLabel by mutableStateOf<String?>(null)

        composeRule.setContent {
            MaterialTheme {
                FloatingSkillPanel(
                    skillBubbles = listOf(
                        preset("ship", "Ship", 0),
                        preset("review", "Review", 1),
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

        listOf("Mic", "Dock", "Ship", "Review", "Proceed", "Listening for your next instruction").forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
        composeRule.onAllNodesWithText("Extra").assertCountEquals(0)
        composeRule.onAllNodesWithText("Q").assertCountEquals(0)
        composeRule.onAllNodesWithText("Space").assertCountEquals(0)

        listOf("floating_mic", "floating_dock", "floating_skill_ship", "floating_skill_review", "floating_skill_proceed").forEach { tag ->
            composeRule.onNodeWithTag(tag).assertHasClickAction()
        }

        composeRule.onNodeWithTag("floating_mic").performClick()
        composeRule.onNodeWithTag("floating_dock").performClick()
        composeRule.onNodeWithTag("floating_skill_ship").performClick()

        composeRule.runOnIdle {
            check(micTapCount == 1) { "Expected Mic to be clicked once, was $micTapCount" }
            check(dockTapCount == 1) { "Expected Dock to be clicked once, was $dockTapCount" }
            check(tappedSkillLabel == "Ship") { "Expected Ship callback, was $tappedSkillLabel" }
        }
    }

    @Test
    fun imeRootFloatingModeRendersOnlyFloatingPanelWithoutDockedKeyboard() {
        val controller = createController(
            presets = listOf(
                preset("ship", "Ship", 0),
                preset("review", "Review", 1),
                preset("proceed", "Proceed", 2),
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
            controller.uiState.value.skillBubbles.size == 3
        }
        composeRule.runOnIdle {
            controller.onLayoutToggle()
        }

        listOf("Mic", "Dock", "Ship", "Review", "Proceed").forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
        composeRule.onAllNodesWithText("Float").assertCountEquals(0)
        composeRule.onAllNodesWithText("Q").assertCountEquals(0)
        composeRule.onAllNodesWithText("Space").assertCountEquals(0)
        composeRule.onAllNodesWithText("Mic").assertCountEquals(1)

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
