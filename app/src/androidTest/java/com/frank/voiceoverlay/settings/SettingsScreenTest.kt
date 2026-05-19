package com.frank.voiceoverlay.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frank.voiceoverlay.ime.ui.VibeTapTheme
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
    fun screenExposesStableAutomationTargets() {
        composeRule.setContent {
            VibeTapTheme {
                SettingsScreen(
                    uiState = SettingsUiState(
                        openAiApiKey = "",
                        microphonePermissionGranted = false,
                        presets = listOf(
                            ShortcutPreset(
                                id = "ship-pr",
                                label = "Ship PR",
                                text = "vibetap saved phrase proof",
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

        composeRule.onNodeWithTag(SettingsScreenTestTags.KeyboardSetupSection).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.ApiKeySection).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.ApiKeyField).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.ShortcutPresetsSection).assertIsDisplayed()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetLabelFieldPrefix}ship-pr").assertIsDisplayed()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetTextFieldPrefix}ship-pr").assertIsDisplayed()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetSaveButtonPrefix}ship-pr").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("OpenAI API Key input").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Preset label input ship-pr").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Preset text input ship-pr").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Save preset ship-pr").assertIsDisplayed()
    }

    @Test
    fun settingsScreenExplainsManualModeSplit() {
        composeRule.setContent {
            VibeTapTheme {
                SettingsScreen(
                    uiState = SettingsUiState(
                        openAiApiKey = "",
                        microphonePermissionGranted = false,
                        presets = listOf(
                            ShortcutPreset(
                                id = "ship-pr",
                                label = "Ship PR",
                                text = "Please ship the PR after checks pass.",
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

        composeRule.onNodeWithText(
            "Keyboard mode is for typing and dictation. Action mode shows bubbles only.",
        ).assertIsDisplayed()
    }
}
