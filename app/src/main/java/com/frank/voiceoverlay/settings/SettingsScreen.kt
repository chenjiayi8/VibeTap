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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.frank.voiceoverlay.shortcuts.ShortcutPreset

object SettingsScreenTestTags {
    const val ApiKeySection = "settings_api_key_section"
    const val OverlayBubbleLabel = "settings_overlay_bubble_label"
    const val ShortcutPresetsSection = "settings_shortcut_presets_section"
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onApiKeyChanged: (String) -> Unit,
    onOverlayEnabledChanged: (Boolean) -> Unit,
    onPresetChanged: (presetId: String, label: String, text: String) -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Voice Overlay Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            SettingsSection(
                title = "OpenAI API Key",
                modifier = Modifier.testTag(SettingsScreenTestTags.ApiKeySection),
            ) {
                OutlinedTextField(
                    value = uiState.openAiApiKey,
                    onValueChange = onApiKeyChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                )
            }

            SettingsSection(title = "Overlay") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable overlay bubble",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.testTag(SettingsScreenTestTags.OverlayBubbleLabel),
                        )
                        Text(
                            text = if (uiState.overlayPermissionGranted) {
                                "Overlay permission granted."
                            } else {
                                "Grant overlay permission before enabling the bubble."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = uiState.overlayEnabled,
                        onCheckedChange = onOverlayEnabledChanged,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onOpenOverlaySettings) {
                        Text("Open overlay settings")
                    }
                    Button(onClick = onOpenAccessibilitySettings) {
                        Text("Open accessibility settings")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Microphone access",
                            style = MaterialTheme.typography.titleMedium,
                        )
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
                title = "Shortcut Presets",
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
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.fillMaxWidth(),
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
