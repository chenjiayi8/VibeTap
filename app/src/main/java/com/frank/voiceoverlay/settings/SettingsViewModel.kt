package com.frank.voiceoverlay.settings

import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val openAiApiKey: String = "",
    val overlayEnabled: Boolean = false,
    val overlayPermissionGranted: Boolean = false,
    val microphonePermissionGranted: Boolean = false,
    val presets: List<ShortcutPreset> = emptyList(),
)

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val hasOverlayPermission: () -> Boolean,
    private val hasMicrophonePermission: () -> Boolean,
    private val openOverlaySettings: () -> Unit,
    private val openAccessibilitySettings: () -> Unit,
    private val requestMicrophonePermission: () -> Unit,
    private val scope: CoroutineScope,
) {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            reloadSettings()
        }
    }

    fun onApiKeyChanged(openAiApiKey: String) {
        scope.launch {
            settingsStore.update { it.copy(openAiApiKey = openAiApiKey) }
            reloadSettings()
        }
    }

    fun onOverlayEnabledChanged(enabled: Boolean) {
        scope.launch {
            if (enabled && !hasOverlayPermission()) {
                reloadSettings()
                return@launch
            }

            settingsStore.update { it.copy(overlayEnabled = enabled) }
            reloadSettings()
        }
    }

    fun onOpenOverlaySettings() {
        openOverlaySettings()
        refreshPermissionState()
    }

    fun onOpenAccessibilitySettings() {
        openAccessibilitySettings()
    }

    fun onRequestMicrophonePermission() {
        requestMicrophonePermission()
        refreshPermissionState()
    }

    fun onPresetChanged(presetId: String, label: String, text: String) {
        scope.launch {
            savePresetChange(presetId, label, text)
        }
    }

    fun refreshPermissionState() {
        scope.launch {
            reloadSettings()
        }
    }

    internal suspend fun reloadSettings() {
        val overlayPermissionGranted = hasOverlayPermission()
        val microphonePermissionGranted = hasMicrophonePermission()
        val normalizedSettings = settingsStore.readOnce().normalizedForPermission(overlayPermissionGranted)
        mutableUiState.value = normalizedSettings.toUiState(
            overlayPermissionGranted = overlayPermissionGranted,
            microphonePermissionGranted = microphonePermissionGranted,
        )
    }

    internal suspend fun savePresetChange(presetId: String, label: String, text: String): Boolean {
        val sanitizedLabel = label.trim()
        val sanitizedText = text.trim()
        if (sanitizedLabel.isBlank() || sanitizedText.isBlank()) {
            return false
        }

        settingsStore.update { settings ->
            settings.copy(
                presets = settings.presets.updatedPreset(
                    presetId = presetId,
                    label = sanitizedLabel,
                    text = sanitizedText,
                ),
            )
        }
        reloadSettings()
        return true
    }

    private suspend fun AppSettings.normalizedForPermission(
        overlayPermissionGranted: Boolean,
    ): AppSettings {
        val normalizedSettings = if (overlayPermissionGranted || !overlayEnabled) {
            this
        } else {
            copy(overlayEnabled = false)
        }
        if (normalizedSettings != this) {
            settingsStore.save(normalizedSettings)
        }
        return normalizedSettings
    }

    private fun AppSettings.toUiState(
        overlayPermissionGranted: Boolean,
        microphonePermissionGranted: Boolean,
    ) = SettingsUiState(
        openAiApiKey = openAiApiKey,
        overlayEnabled = overlayEnabled,
        overlayPermissionGranted = overlayPermissionGranted,
        microphonePermissionGranted = microphonePermissionGranted,
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
