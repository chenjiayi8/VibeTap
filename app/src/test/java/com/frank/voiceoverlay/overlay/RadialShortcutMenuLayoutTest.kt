package com.frank.voiceoverlay.overlay

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class RadialShortcutMenuLayoutTest {
    @Test
    fun menuDiameter_includesRadiusButtonSizeAndPadding() {
        val diameter = RadialShortcutMenuLayout.menuDiameter(
            radius = 96.dp,
            buttonDiameter = 56.dp,
            outerPadding = 8.dp,
        )

        assertEquals(264f, diameter.value, 0.001f)
    }

    @Test
    fun shortcutOffsets_forFourButtons_matchCardinalPositions() {
        val offsets = RadialShortcutMenuLayout.shortcutOffsets(count = 4, radius = 96.dp)

        assertEquals(4, offsets.size)
        assertEquals(0f, offsets[0].x.value, 0.001f)
        assertEquals(-96f, offsets[0].y.value, 0.001f)
        assertEquals(96f, offsets[1].x.value, 0.001f)
        assertEquals(0f, offsets[1].y.value, 0.001f)
        assertEquals(0f, offsets[2].x.value, 0.001f)
        assertEquals(96f, offsets[2].y.value, 0.001f)
        assertEquals(-96f, offsets[3].x.value, 0.001f)
        assertEquals(0f, offsets[3].y.value, 0.001f)
    }
}
