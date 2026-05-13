package com.frank.voiceoverlay.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object RadialShortcutMenuLayout {
    val DefaultButtonDiameter: Dp = 56.dp
    val DefaultOuterPadding: Dp = 8.dp

    fun menuDiameter(
        radius: Dp,
        buttonDiameter: Dp = DefaultButtonDiameter,
        outerPadding: Dp = DefaultOuterPadding,
    ): Dp {
        val furthestButtonEdge = radius + (buttonDiameter / 2)
        return (furthestButtonEdge + outerPadding) * 2
    }

    fun shortcutOffsets(count: Int, radius: Dp): List<DpOffset> {
        if (count <= 0) return emptyList()

        return List(count) { index ->
            val angleRadians = (2 * PI * index) / count - (PI / 2)
            DpOffset(
                x = (radius.value * cos(angleRadians)).dp,
                y = (radius.value * sin(angleRadians)).dp,
            )
        }
    }
}

@Composable
fun RadialShortcutMenu(
    presets: List<ShortcutPreset>,
    onPresetClick: (ShortcutPreset) -> Unit,
    modifier: Modifier = Modifier,
    radius: Dp = 96.dp,
    buttonDiameter: Dp = RadialShortcutMenuLayout.DefaultButtonDiameter,
    outerPadding: Dp = RadialShortcutMenuLayout.DefaultOuterPadding,
) {
    val menuDiameter = RadialShortcutMenuLayout.menuDiameter(
        radius = radius,
        buttonDiameter = buttonDiameter,
        outerPadding = outerPadding,
    )
    val offsets = RadialShortcutMenuLayout.shortcutOffsets(count = presets.size, radius = radius)

    Box(
        modifier = modifier.requiredSize(menuDiameter),
        contentAlignment = Alignment.Center,
    ) {
        presets.zip(offsets).forEach { (preset, offset) ->
            FilledTonalButton(
                onClick = { onPresetClick(preset) },
                modifier = Modifier
                    .size(buttonDiameter)
                    .offset(x = offset.x, y = offset.y),
            ) {
                Text(text = preset.label)
            }
        }
    }
}
