package com.frank.voiceoverlay.settings

import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val openAiApiKey: String = "",
    val microphonePermissionGranted: Boolean = false,
    val presets: List<ShortcutPreset> = emptyList(),
)

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val hasMicrophonePermission: () -> Boolean,
    private val openKeyboardSettings: () -> Unit,
    private val showInputMethodPicker: () -> Unit,
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

    fun onOpenKeyboardSettings() {
        openKeyboardSettings()
    }

    fun onShowInputMethodPicker() {
        showInputMethodPicker()
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
        val microphonePermissionGranted = hasMicrophonePermission()
        val settings = settingsStore.readOnce()
        mutableUiState.value = settings.toUiState(
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

    private fun AppSettings.toUiState(
        microphonePermissionGranted: Boolean,
    ) = SettingsUiState(
        openAiApiKey = openAiApiKey,
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
