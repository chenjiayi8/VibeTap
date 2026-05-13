package com.frank.voiceoverlay.shortcuts

data class ShortcutPreset(
    val id: String,
    val label: String,
    val text: String,
    val order: Int,
) {
    companion object {
        fun defaultPresets(): List<ShortcutPreset> = listOf(
            ShortcutPreset(
                id = "ship-pr",
                label = "Ship-PR",
                text = "Good, please proceed to use \$ship-pr",
                order = 0,
            ),
            ShortcutPreset(
                id = "review",
                label = "Review",
                text = "Please review the latest changes carefully.",
                order = 1,
            ),
            ShortcutPreset(
                id = "proceed",
                label = "Proceed",
                text = "Good, please proceed.",
                order = 2,
            ),
        )
    }
}
