package com.frank.voiceoverlay.overlay

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import com.frank.voiceoverlay.testing.TestComposeActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlayBubbleSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<TestComposeActivity>()

    @Test
    fun overlayViewShowsBubbleAndShortcutLabels() {
        composeRule.setContent {
            MaterialTheme {
                OverlayBubbleView(
                    uiState = BubbleUiState(
                        interactionState = BubbleInteractionState.ShortcutsExpanded,
                        shortcuts = listOf(
                            ShortcutPreset(
                                id = "ship-pr",
                                label = "Ship-PR",
                                text = "Good, please proceed to use \$ship-pr",
                                order = 0,
                            ),
                        ),
                    ),
                    onBubbleTap = {},
                    onShortcutTap = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Voice input bubble").assertIsDisplayed()
        composeRule.onNodeWithText("Ship-PR").assertIsDisplayed()
    }
}
