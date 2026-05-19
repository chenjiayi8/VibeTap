package com.frank.voiceoverlay.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frank.voiceoverlay.shortcuts.ShortcutPreset

object SettingsScreenTestTags {
    const val KeyboardSetupSection = "settings_keyboard_setup_section"
    const val ApiKeySection = "settings_api_key_section"
    const val ApiKeyField = "settings_api_key_field"
    const val ShortcutPresetsSection = "settings_shortcut_presets_section"
    const val PresetLabelFieldPrefix = "settings_preset_label_field_"
    const val PresetTextFieldPrefix = "settings_preset_text_field_"
    const val PresetSaveButtonPrefix = "settings_preset_save_button_"
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onApiKeyChanged: (String) -> Unit,
    onPresetChanged: (presetId: String, label: String, text: String) -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    onShowInputMethodPicker: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "VibeTap Keyboard Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            SettingsSection(
                title = "Keyboard setup",
                modifier = Modifier.testTag(SettingsScreenTestTags.KeyboardSetupSection),
            ) {
                Text(
                    text = "Enable the VibeTap keyboard in Android settings, then choose it as your active keyboard before dictating.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Keyboard mode is for typing and dictation. Action mode shows bubbles only.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onOpenKeyboardSettings,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open keyboard settings")
                    }
                    Button(
                        onClick = onShowInputMethodPicker,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Choose active keyboard")
                    }
                }
            }

            SettingsSection(
                title = "OpenAI API Key",
                modifier = Modifier.testTag(SettingsScreenTestTags.ApiKeySection),
            ) {
                OutlinedTextField(
                    value = uiState.openAiApiKey,
                    onValueChange = onApiKeyChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(SettingsScreenTestTags.ApiKeyField)
                        .semantics { contentDescription = "OpenAI API Key input" },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                )
            }

            SettingsSection(title = "Microphone access") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.microphonePermissionGranted) {
                                "Microphone permission granted."
                            } else {
                                "Grant microphone access before starting dictation."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = onRequestMicrophonePermission) {
                        Text(
                            text = if (uiState.microphonePermissionGranted) {
                                "Refresh mic access"
                            } else {
                                "Grant microphone access"
                            },
                        )
                    }
                }
            }

            SettingsSection(
                title = "Saved phrase skills",
                modifier = Modifier.testTag(SettingsScreenTestTags.ShortcutPresetsSection),
            ) {
                uiState.presets.forEach { preset ->
                    EditablePresetCard(
                        preset = preset,
                        onSave = { label, text -> onPresetChanged(preset.id, label, text) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EditablePresetCard(
    preset: ShortcutPreset,
    onSave: (label: String, text: String) -> Unit,
) {
    var label by remember(preset.id, preset.label) { mutableStateOf(preset.label) }
    var text by remember(preset.id, preset.text) { mutableStateOf(preset.text) }
    var validationMessage by remember(preset.id) { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = preset.id,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = label,
            onValueChange = {
                label = it
                validationMessage = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${SettingsScreenTestTags.PresetLabelFieldPrefix}${preset.id}")
                .semantics { contentDescription = "Preset label input ${preset.id}" },
            label = { Text("Preset label") },
            singleLine = true,
            isError = validationMessage != null && label.trim().isBlank(),
        )
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                validationMessage = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${SettingsScreenTestTags.PresetTextFieldPrefix}${preset.id}")
                .semantics { contentDescription = "Preset text input ${preset.id}" },
            label = { Text("Preset text") },
            isError = validationMessage != null && text.trim().isBlank(),
        )
        if (validationMessage != null) {
            Text(
                text = validationMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = {
                if (label.trim().isBlank() || text.trim().isBlank()) {
                    validationMessage = "Preset label and text cannot be blank."
                } else {
                    validationMessage = null
                    onSave(label.trim(), text.trim())
                }
            },
            modifier = Modifier
                .testTag("${SettingsScreenTestTags.PresetSaveButtonPrefix}${preset.id}")
                .semantics { contentDescription = "Save preset ${preset.id}" },
        ) {
            Text("Save preset")
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}
