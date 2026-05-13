package com.frank.voiceoverlay.settings

import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {
    @Test
    fun updatedPreset_replacesMatchingPresetContentOnly() {
        val presets = listOf(
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
        )

        val updated = presets.updatedPreset(
            presetId = "ship-pr",
            label = "Ship now",
            text = "Please land and ship this PR.",
        )

        assertEquals(
            listOf(
                presets.first().copy(label = "Ship now", text = "Please land and ship this PR."),
                presets.last(),
            ),
            updated,
        )
    }
}
