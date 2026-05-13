package com.frank.vibetap.settings

import com.frank.vibetap.shortcuts.ShortcutPreset

data class AppSettings(
    val openAiApiKey: String = "",
    val overlayEnabled: Boolean = false,
    val presets: List<ShortcutPreset> = ShortcutPreset.defaultPresets(),
)
