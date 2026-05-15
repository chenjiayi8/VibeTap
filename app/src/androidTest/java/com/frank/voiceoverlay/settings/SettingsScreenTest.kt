package com.frank.voiceoverlay.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import com.frank.voiceoverlay.testing.TestComposeActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<TestComposeActivity>()

    @Test
    fun screenShowsKeyboardSetupApiKeyAndPhraseSkillsSections() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    uiState = SettingsUiState(
                        openAiApiKey = "",
                        microphonePermissionGranted = false,
                        presets = listOf(
                            ShortcutPreset(
                                id = "ship-pr",
                                label = "Ship-PR",
                                text = "Good, please proceed to use \$ship-pr",
                                order = 0,
                            ),
                        ),
                    ),
                    onApiKeyChanged = {},
                    onPresetChanged = { _, _, _ -> },
                    onOpenKeyboardSettings = {},
                    onShowInputMethodPicker = {},
                    onRequestMicrophonePermission = {},
                )
            }
        }

        composeRule.onNodeWithText("VibeTap Keyboard Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Keyboard setup").assertIsDisplayed()
        composeRule.onNodeWithText("Open keyboard settings").assertIsDisplayed()
        composeRule.onNodeWithText("Choose active keyboard").assertIsDisplayed()
        composeRule.onNodeWithText("OpenAI API Key").assertIsDisplayed()
        composeRule.onNodeWithText("Microphone access").assertIsDisplayed()
        composeRule.onNodeWithText("Saved phrase skills").assertIsDisplayed()
        composeRule.onNodeWithText("Grant microphone access").assertIsDisplayed()
        composeRule.onNodeWithText("Save preset").assertIsDisplayed()
    }
}
