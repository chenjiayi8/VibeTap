package com.frank.voiceoverlay.shortcuts

import com.frank.voiceoverlay.settings.SettingsStore

class ShortcutPresetRepository(
    private val settingsStore: SettingsStore,
) {
    suspend fun listPresets(): List<ShortcutPreset> =
        settingsStore.readOnce().presets.sortedBy(ShortcutPreset::order)

    suspend fun savePresets(presets: List<ShortcutPreset>) {
        settingsStore.update { settings ->
            settings.copy(presets = presets.normalizedOrder())
        }
    }

    suspend fun addPreset(preset: ShortcutPreset) {
        settingsStore.update { settings ->
            settings.copy(presets = (settings.presets + preset).normalizedOrder())
        }
    }

    private fun List<ShortcutPreset>.normalizedOrder(): List<ShortcutPreset> =
        sortedBy(ShortcutPreset::order).mapIndexed { index, preset ->
            preset.copy(order = index)
        }
}
