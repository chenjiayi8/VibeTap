package com.frank.voiceoverlay.ime

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.ime.ui.DockedKeyboardView
import com.frank.voiceoverlay.ime.ui.VibeTapImeRoot
import com.frank.voiceoverlay.ime.ui.VibeTapTheme
import com.frank.voiceoverlay.testing.TestComposeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class DockedKeyboardViewTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<TestComposeActivity>()

    @Test
    fun dockedKeyboardShowsQwertyRowsAndEssentialControls() {
        composeRule.setContent {
            VibeTapTheme {
                DockedKeyboardView(
                    bottomInsetPadding = 0.dp,
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
            "Mic", "Actions", "⌫", "Space", "Enter",
        ).forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun dockedKeyboardControlsExposeClickActions() {
        composeRule.setContent {
            VibeTapTheme {
                DockedKeyboardView(
                    bottomInsetPadding = 0.dp,
                    onMicTapped = {},
                    onLayoutToggle = {},
                    onBackspace = { true },
                    onEnter = { true },
                    onCommitLetter = { true },
                    onCommitPhrase = { true },
                )
            }
        }

        listOf("key_mic", "key_actions", "key_backspace", "key_space", "key_enter").forEach { tag ->
            composeRule.onNodeWithTag(tag).assertHasClickAction()
        }
    }

    @Test
    fun dockedKeyboardUsesActionsLabelInsteadOfFloat() {
        composeRule.setContent {
            VibeTapTheme {
                DockedKeyboardView(
                    bottomInsetPadding = 0.dp,
                    onMicTapped = {},
                    onLayoutToggle = {},
                    onBackspace = { true },
                    onEnter = { true },
                    onCommitLetter = { true },
                    onCommitPhrase = { true },
                )
            }
        }

        composeRule.onNodeWithText("Actions").assertIsDisplayed()
        composeRule.onAllNodesWithText("Float").assertCountEquals(0)
    }

    @Test
    fun dockedKeyboardAppliesBottomInsetPaddingToKeepBottomRowAboveSystemUi() {
        var bottomInsetPadding by mutableStateOf(0.dp)

        composeRule.setContent {
            VibeTapTheme {
                Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    DockedKeyboardView(
                        bottomInsetPadding = bottomInsetPadding,
                        onMicTapped = {},
                        onLayoutToggle = {},
                        onBackspace = { true },
                        onEnter = { true },
                        onCommitLetter = { true },
                        onCommitPhrase = { true },
                    )
                }
            }
        }

        fun currentSpaceBottom(): Float {
            return composeRule.onNodeWithTag("key_space").fetchSemanticsNode().boundsInRoot.bottom
        }

        val zeroInsetSpaceBottomPx = currentSpaceBottom()
        composeRule.runOnIdle {
            bottomInsetPadding = 32.dp
        }
        val insetSpaceBottomPx = currentSpaceBottom()
        val density = composeRule.activity.resources.displayMetrics.density
        val expectedShiftPx = 32f * density

        composeRule.runOnIdle {
            val upwardShiftPx = zeroInsetSpaceBottomPx - insetSpaceBottomPx
            check(upwardShiftPx > 0f) {
                "Expected bottom inset to move the bottom row upward, got zeroInset=$zeroInsetSpaceBottomPx inset=$insetSpaceBottomPx"
            }
            check(abs(upwardShiftPx - expectedShiftPx) <= 1.5f) {
                "Expected upward shift near $expectedShiftPx px, got $upwardShiftPx px (zeroInset=$zeroInsetSpaceBottomPx inset=$insetSpaceBottomPx)"
            }
        }
    }

    @Test
    fun imeRootDockedModeForwardsInsetChangesIntoKeyboardLayout() {
        val controller = createController()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        controller.bind(scope)
        var bottomInsetPadding by mutableStateOf(0.dp)

        composeRule.setContent {
            VibeTapTheme {
                Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    VibeTapImeRoot(
                        controller = controller,
                        onBackspace = { true },
                        onEnter = { true },
                        onCommitLetter = { true },
                        onCommitPhrase = { true },
                        dockedBottomInsetOverride = bottomInsetPadding,
                    )
                }
            }
        }

        fun currentSpaceBottom(): Float {
            return composeRule.onNodeWithTag("key_space").fetchSemanticsNode().boundsInRoot.bottom
        }

        val zeroInsetSpaceBottomPx = currentSpaceBottom()
        composeRule.runOnIdle {
            bottomInsetPadding = 32.dp
        }
        val insetSpaceBottomPx = currentSpaceBottom()
        val density = composeRule.activity.resources.displayMetrics.density
        val expectedShiftPx = 32f * density

        composeRule.runOnIdle {
            val upwardShiftPx = zeroInsetSpaceBottomPx - insetSpaceBottomPx
            check(upwardShiftPx > 0f) {
                "Expected IME root inset forwarding to move the bottom row upward, got zeroInset=$zeroInsetSpaceBottomPx inset=$insetSpaceBottomPx"
            }
            check(abs(upwardShiftPx - expectedShiftPx) <= 1.5f) {
                "Expected IME root upward shift near $expectedShiftPx px, got $upwardShiftPx px (zeroInset=$zeroInsetSpaceBottomPx inset=$insetSpaceBottomPx)"
            }
        }

        scope.cancel()
    }

    @Test
    fun imeRootFloatingModeCanToggleBackToDocked() {
        val controller = createController()
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

        composeRule.runOnIdle {
            controller.onLayoutToggle()
        }
        composeRule.onNodeWithText("Mic").assertIsDisplayed()
        composeRule.onNodeWithText("Keyboard").assertIsDisplayed()
        composeRule.onAllNodesWithText("Float").assertCountEquals(0)

        composeRule.runOnIdle {
            controller.onLayoutToggle()
        }
        composeRule.onNodeWithText("Actions").assertIsDisplayed()
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
