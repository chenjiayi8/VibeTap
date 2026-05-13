package com.frank.voiceoverlay.shortcuts

import com.frank.voiceoverlay.settings.SettingsStore

class ShortcutPresetRepository(
    private val settingsStore: SettingsStore,
) {
    suspend fun listPresets(): List<ShortcutPreset> =
        settingsStore.readOnce().presets.sortedBy(ShortcutPreset::order)

    suspend fun savePresets(presets: List<ShortcutPreset>) {
        val currentSettings = settingsStore.readOnce()
        settingsStore.save(currentSettings.copy(presets = presets.sortedBy(ShortcutPreset::order)))
    }
}
