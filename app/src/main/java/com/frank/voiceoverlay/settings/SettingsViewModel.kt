package com.frank.voiceoverlay.settings

import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

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

    fun createPreset() {
        scope.launch {
            createPresetAndReload()
        }
    }

    fun deletePreset(presetId: String) {
        scope.launch {
            deletePresetAndReload(presetId)
        }
    }

    fun movePresetUp(presetId: String) {
        movePreset(presetId = presetId, direction = -1)
    }

    fun movePresetDown(presetId: String) {
        movePreset(presetId = presetId, direction = 1)
    }

    fun togglePresetPinned(presetId: String) {
        scope.launch {
            togglePresetPinnedAndReload(presetId)
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

    private suspend fun createPresetAndReload() {
        settingsStore.update { settings ->
            val normalizedPresets = settings.presets.sortedByOrder().normalizedOrder()
            settings.copy(
                presets = normalizedPresets +
                    ShortcutPreset(
                        id = "macro-${UUID.randomUUID()}",
                        label = "New macro",
                        text = "Describe what this macro should insert.",
                        order = normalizedPresets.size,
                        isPinned = false,
                    ),
            )
        }
        reloadSettings()
    }

    private suspend fun deletePresetAndReload(presetId: String) {
        settingsStore.update { settings ->
            settings.copy(
                presets = settings.presets
                    .sortedByOrder()
                    .filterNot { it.id == presetId }
                    .normalizedOrder(),
            )
        }
        reloadSettings()
    }

    private suspend fun movePresetAndReload(presetId: String, direction: Int) {
        settingsStore.update { settings ->
            settings.copy(
                presets = settings.presets.movePreset(
                    presetId = presetId,
                    direction = direction,
                ),
            )
        }
        reloadSettings()
    }

    private suspend fun togglePresetPinnedAndReload(presetId: String) {
        settingsStore.update { settings ->
            settings.copy(
                presets = settings.presets.map { preset ->
                    if (preset.id == presetId) {
                        preset.copy(isPinned = !preset.isPinned)
                    } else {
                        preset
                    }
                },
            )
        }
        reloadSettings()
    }

    private fun movePreset(presetId: String, direction: Int) {
        scope.launch {
            movePresetAndReload(presetId = presetId, direction = direction)
        }
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

internal fun List<ShortcutPreset>.movePreset(presetId: String, direction: Int): List<ShortcutPreset> {
    val orderedPresets = sortedByOrder()
    val currentIndex = orderedPresets.indexOfFirst { it.id == presetId }
    if (currentIndex == -1) {
        return orderedPresets.normalizedOrder()
    }

    val targetIndex = (currentIndex + direction).coerceIn(0, orderedPresets.lastIndex)
    if (targetIndex == currentIndex) {
        return orderedPresets.normalizedOrder()
    }

    val mutablePresets = orderedPresets.toMutableList()
    val preset = mutablePresets.removeAt(currentIndex)
    mutablePresets.add(targetIndex, preset)
    return mutablePresets.normalizedOrder()
}

private fun List<ShortcutPreset>.sortedByOrder(): List<ShortcutPreset> = sortedBy { it.order }

private fun List<ShortcutPreset>.normalizedOrder(): List<ShortcutPreset> =
    mapIndexed { index, preset -> preset.copy(order = index) }
