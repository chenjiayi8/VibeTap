package com.frank.voiceoverlay.settings

import com.frank.voiceoverlay.shortcuts.ShortcutPreset

data class AppSettings(
    val openAiApiKey: String = "",
    val overlayEnabled: Boolean = false,
    val presets: List<ShortcutPreset> = ShortcutPreset.defaultPresets(),
)
