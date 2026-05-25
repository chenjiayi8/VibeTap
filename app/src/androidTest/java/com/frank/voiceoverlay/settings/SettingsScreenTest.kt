package com.frank.voiceoverlay.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import com.frank.voiceoverlay.testing.TestComposeActivity
import org.junit.Assert.assertEquals
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
                    onCreatePreset = {},
                    onDeletePreset = {},
                    onMovePresetUp = {},
                    onMovePresetDown = {},
                    onTogglePresetPinned = {},
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
        composeRule.onNodeWithTag(SettingsScreenTestTags.AddMacroButton).assertIsDisplayed()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetLabelFieldPrefix}live-proof").assertIsDisplayed()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetTextFieldPrefix}live-proof").assertIsDisplayed()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetSaveButtonPrefix}live-proof").assertIsDisplayed()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetDeleteButtonPrefix}live-proof").assertIsDisplayed()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetPinButtonPrefix}live-proof").assertIsDisplayed()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetMoveUpButtonPrefix}live-proof").assertIsDisplayed()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetMoveDownButtonPrefix}live-proof").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("OpenAI API Key input").assertIsDisplayed()
    }

    @Test
    fun crudControlsInvokeCallbacks() {
        var createCalls = 0
        var deletedPresetId: String? = null
        var movedUpPresetId: String? = null
        var movedDownPresetId: String? = null
        var toggledPinnedPresetId: String? = null

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
                    onCreatePreset = { createCalls += 1 },
                    onDeletePreset = { deletedPresetId = it },
                    onMovePresetUp = { movedUpPresetId = it },
                    onMovePresetDown = { movedDownPresetId = it },
                    onTogglePresetPinned = { toggledPinnedPresetId = it },
                    onOpenKeyboardSettings = {},
                    onShowInputMethodPicker = {},
                    onRequestMicrophonePermission = {},
                )
            }
        }

        composeRule.onNodeWithTag(SettingsScreenTestTags.AddMacroButton).performClick()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetDeleteButtonPrefix}live-proof").performClick()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetPinButtonPrefix}live-proof").performClick()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetMoveUpButtonPrefix}live-proof").performClick()
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetMoveDownButtonPrefix}live-proof").performClick()

        composeRule.runOnIdle {
            assertEquals(1, createCalls)
            assertEquals("live-proof", deletedPresetId)
            assertEquals("live-proof", toggledPinnedPresetId)
            assertEquals("live-proof", movedUpPresetId)
            assertEquals("live-proof", movedDownPresetId)
        }
    }

    @Test
    fun draftStateStaysAttachedToPresetIdAfterReorder() {
        fun movePresetUp(presets: List<ShortcutPreset>, presetId: String): List<ShortcutPreset> {
            val index = presets.indexOfFirst { it.id == presetId }
            if (index <= 0) return presets
            val reordered = presets.toMutableList()
            val current = reordered[index]
            reordered[index] = reordered[index - 1]
            reordered[index - 1] = current
            return reordered.mapIndexed { newIndex, preset -> preset.copy(order = newIndex) }
        }

        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    SettingsUiState(
                    openAiApiKey = "",
                    microphonePermissionGranted = false,
                    presets = listOf(
                        ShortcutPreset(
                            id = "alpha",
                            label = "Alpha",
                            text = "Alpha text",
                            order = 0,
                        ),
                        ShortcutPreset(
                            id = "beta",
                            label = "Beta",
                            text = "Beta text",
                            order = 1,
                        ),
                    ),
                    ),
                )
            }

            MaterialTheme {
                SettingsScreen(
                    uiState = uiState,
                    onApiKeyChanged = {},
                    onPresetChanged = { _, _, _ -> },
                    onCreatePreset = {},
                    onDeletePreset = {},
                    onMovePresetUp = { presetId ->
                        uiState = uiState.copy(presets = movePresetUp(uiState.presets, presetId))
                    },
                    onMovePresetDown = {},
                    onTogglePresetPinned = {},
                    onOpenKeyboardSettings = {},
                    onShowInputMethodPicker = {},
                    onRequestMicrophonePermission = {},
                )
            }
        }

        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetLabelFieldPrefix}beta")
            .performTextReplacement("Beta draft")
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetMoveUpButtonPrefix}beta")
            .performClick()

        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetLabelFieldPrefix}beta")
            .assertTextContains("Beta draft")
        composeRule.onNodeWithTag("${SettingsScreenTestTags.PresetLabelFieldPrefix}alpha")
            .assertTextContains("Alpha")
    }
}
