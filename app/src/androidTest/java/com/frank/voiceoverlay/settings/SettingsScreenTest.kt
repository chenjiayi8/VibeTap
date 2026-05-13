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
    fun screenShowsApiKeyAndShortcutSections() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    uiState = SettingsUiState(
                        openAiApiKey = "",
                        overlayEnabled = false,
                        overlayPermissionGranted = false,
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
                    onOverlayEnabledChanged = {},
                    onPresetChanged = { _, _, _ -> },
                    onOpenOverlaySettings = {},
                    onOpenAccessibilitySettings = {},
                    onRequestMicrophonePermission = {},
                )
            }
        }

        composeRule.onNodeWithText("OpenAI API Key").assertIsDisplayed()
        composeRule.onNodeWithText("Shortcut Presets").assertIsDisplayed()
        composeRule.onNodeWithText("Grant microphone access").assertIsDisplayed()
        composeRule.onNodeWithText("Save preset").assertIsDisplayed()
    }
}
