package com.frank.voiceoverlay.ime

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frank.voiceoverlay.ime.ui.DockedKeyboardView
import com.frank.voiceoverlay.testing.TestComposeActivity
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
}
