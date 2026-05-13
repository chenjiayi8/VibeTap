package com.frank.voiceoverlay.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frank.voiceoverlay.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun screenShowsApiKeyAndShortcutSections() {
        composeRule.onNodeWithText("OpenAI API Key").assertIsDisplayed()
        composeRule.onNodeWithText("Shortcut Presets").assertIsDisplayed()
        composeRule.onNodeWithText("Save preset").assertIsDisplayed()
    }
}
