package com.frank.voiceoverlay.settings

import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val openAiApiKey: String = "",
    val overlayEnabled: Boolean = false,
    val overlayPermissionGranted: Boolean = false,
    val presets: List<ShortcutPreset> = emptyList(),
)

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val hasOverlayPermission: () -> Boolean,
    private val openOverlaySettings: () -> Unit,
    private val openAccessibilitySettings: () -> Unit,
    private val scope: CoroutineScope,
) {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            mutableUiState.value = settingsStore.readOnce().toUiState(
                overlayPermissionGranted = hasOverlayPermission(),
            )
        }
    }

    fun onApiKeyChanged(openAiApiKey: String) {
        updateSettings { it.copy(openAiApiKey = openAiApiKey) }
    }

    fun onOverlayEnabledChanged(enabled: Boolean) {
        if (enabled && !hasOverlayPermission()) {
            mutableUiState.update { it.copy(overlayPermissionGranted = false, overlayEnabled = false) }
            return
        }

        updateSettings { it.copy(overlayEnabled = enabled) }
    }

    fun onOpenOverlaySettings() {
        openOverlaySettings()
        refreshPermissionState()
    }

    fun onOpenAccessibilitySettings() {
        openAccessibilitySettings()
    }

    fun onPresetChanged(presetId: String, label: String, text: String) {
        updateSettings { settings ->
            settings.copy(
                presets = settings.presets.updatedPreset(
                    presetId = presetId,
                    label = label,
                    text = text,
                ),
            )
        }
    }

    fun refreshPermissionState() {
        mutableUiState.update {
            it.copy(overlayPermissionGranted = hasOverlayPermission())
        }
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        scope.launch {
            settingsStore.update(transform)
            mutableUiState.value = settingsStore.readOnce().toUiState(
                overlayPermissionGranted = hasOverlayPermission(),
            )
        }
    }

    private fun AppSettings.toUiState(overlayPermissionGranted: Boolean) = SettingsUiState(
        openAiApiKey = openAiApiKey,
        overlayEnabled = overlayEnabled,
        overlayPermissionGranted = overlayPermissionGranted,
        presets = presets,
    )
}

internal fun List<ShortcutPreset>.updatedPreset(
    presetId: String,
    label: String,
    text: String,
): List<ShortcutPreset> = map { preset ->
    if (preset.id == presetId) {
        preset.copy(label = label, text = text)
    } else {
        preset
    }
}
