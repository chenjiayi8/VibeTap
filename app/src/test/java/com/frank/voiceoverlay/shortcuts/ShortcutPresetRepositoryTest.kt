package com.frank.voiceoverlay.shortcuts

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutPresetRepositoryTest {
    @Test
    fun defaultPresets_includeShipPrPhrase() {
        val presets = ShortcutPreset.defaultPresets()
        assertEquals("Good, please proceed to use \$ship-pr", presets.first { it.id == "ship-pr" }.text)
    }
}
