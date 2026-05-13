package com.frank.voiceoverlay.shortcuts

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutPresetRepositoryTest {
    @Test
    fun defaultPresets_matchApprovedBaseline() {
        assertEquals(
            listOf(
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
            ),
            ShortcutPreset.defaultPresets(),
        )
    }
}
