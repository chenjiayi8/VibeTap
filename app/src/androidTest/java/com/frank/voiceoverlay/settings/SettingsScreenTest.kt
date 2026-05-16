package com.frank.voiceoverlay.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
    fun screenExposesStableAutomationTargets() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    uiState = SettingsUiState(
                        openAiApiKey = "",
                        microphonePermissionGranted = false,
                        presets = listOf(
                            ShortcutPreset(
                                id = "live-proof",
                                label = "LiveProof",
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

        composeRule.onNodeWithTag(SettingsScreenTestTags.ApiKeyField).assertIsDisplayed()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetLabelFieldPrefix}live-proof").assertIsDisplayed()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetTextFieldPrefix}live-proof").assertIsDisplayed()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetSaveButtonPrefix}live-proof").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("OpenAI API Key input").assertIsDisplayed()
    }
}
