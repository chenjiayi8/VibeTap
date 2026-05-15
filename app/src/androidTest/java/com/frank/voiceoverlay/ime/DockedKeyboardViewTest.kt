package com.frank.voiceoverlay.ime

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.ime.ui.DockedKeyboardView
import com.frank.voiceoverlay.ime.ui.VibeTapImeRoot
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
class DockedKeyboardViewTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<TestComposeActivity>()

    @Test
    fun dockedKeyboardShowsQwertyRowsAndEssentialControls() {
        composeRule.setContent {
            MaterialTheme {
                DockedKeyboardView(
                    onMicTapped = {},
                    onLayoutToggle = {},
                    onBackspace = { true },
                    onEnter = { true },
                    onCommitLetter = { true },
                    onCommitPhrase = { true },
                )
            }
        }

        listOf(
            "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P",
            "A", "S", "D", "F", "G", "H", "J", "K", "L",
            "Z", "X", "C", "V", "B", "N", "M",
            "Mic", "Float", "⌫", "Space", "Enter",
        ).forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun dockedKeyboardControlsExposeClickActions() {
        composeRule.setContent {
            MaterialTheme {
                DockedKeyboardView(
                    onMicTapped = {},
                    onLayoutToggle = {},
                    onBackspace = { true },
                    onEnter = { true },
                    onCommitLetter = { true },
                    onCommitPhrase = { true },
                )
            }
        }

        listOf("key_mic", "key_float", "key_backspace", "key_space", "key_enter").forEach { tag ->
            composeRule.onNodeWithTag(tag).assertHasClickAction()
        }
    }

    @Test
    fun imeRootFloatingModeCanToggleBackToDocked() {
        val controller = createController()
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

        composeRule.runOnIdle {
            controller.onLayoutToggle()
        }
        composeRule.onNodeWithText("Mic").assertIsDisplayed()
        composeRule.onNodeWithText("Dock").assertIsDisplayed()
        composeRule.onAllNodesWithText("Float").assertCountEquals(0)

        composeRule.runOnIdle {
            controller.onLayoutToggle()
        }
        composeRule.onNodeWithText("Float").assertIsDisplayed()
        composeRule.onAllNodesWithText("Dock").assertCountEquals(0)

        scope.cancel()
    }

    private fun createController(): VoiceKeyboardController = VoiceKeyboardController(
        recordingState = MutableStateFlow(RecordingState.IDLE),
        shortcutsProvider = { emptyList() },
        startRecording = {},
        stopRecording = {},
        resetRecording = {},
        commitPhrase = { true },
    )
}
