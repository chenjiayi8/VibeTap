package com.frank.voiceoverlay.shortcuts

import kotlinx.serialization.Serializable

@Serializable
data class ShortcutPreset(
    val id: String,
    val label: String,
    val text: String,
    val order: Int,
    val isPinned: Boolean = false,
) {
    companion object {
        fun defaultPresets(): List<ShortcutPreset> = listOf(
            ShortcutPreset(
                id = "ship-pr",
                label = "Ship PR",
                text = "Please ship the PR after checks pass.",
                order = 0,
                isPinned = true,
            ),
            ShortcutPreset(
                id = "review-pr",
                label = "Review PR",
                text = "Please review the PR and call out the highest-risk issues first.",
                order = 1,
                isPinned = true,
            ),
            ShortcutPreset(
                id = "proceed",
                label = "Proceed",
                text = "Please proceed with the approved plan.",
                order = 2,
                isPinned = false,
            ),
        )
    }
}
