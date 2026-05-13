package com.frank.vibetap.shortcuts

data class ShortcutPreset(
    val id: String,
    val label: String,
    val text: String,
    val order: Int,
) {
    companion object {
        fun defaultPresets(): List<ShortcutPreset> = listOf(
            ShortcutPreset(
                id = "proceed",
                label = "Proceed",
                text = "Good, please proceed.",
                order = 0,
            ),
            ShortcutPreset(
                id = "follow-up",
                label = "Follow up",
                text = "Thanks — I will follow up shortly.",
                order = 1,
            ),
            ShortcutPreset(
                id = "ship-pr",
                label = "Ship PR",
                text = "Good, please proceed to use \$ship-pr",
                order = 2,
            ),
        )
    }
}
