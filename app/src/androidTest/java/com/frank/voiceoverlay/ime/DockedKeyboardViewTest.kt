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
    fun imeRootFloatingPlaceholderCanToggleBackToDocked() {
        val controller = createController()

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
        composeRule.onNodeWithText("Floating mode coming soon").assertIsDisplayed()
        composeRule.onNodeWithText("Dock").assertIsDisplayed()

        composeRule.runOnIdle {
            controller.onLayoutToggle()
        }
        composeRule.onAllNodesWithText("Floating mode coming soon").assertCountEquals(0)
        composeRule.onNodeWithText("Float").assertIsDisplayed()
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
