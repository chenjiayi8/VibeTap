package com.frank.voiceoverlay.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadialShortcutMenu(
    presets: List<ShortcutPreset>,
    onPresetClick: (ShortcutPreset) -> Unit,
    modifier: Modifier = Modifier,
    radius: Dp = 96.dp,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (presets.isEmpty()) return@Box

        presets.forEachIndexed { index, preset ->
            val angleRadians = (2 * PI * index) / presets.size - (PI / 2)
            val xOffset = (radius.value * cos(angleRadians)).dp
            val yOffset = (radius.value * sin(angleRadians)).dp

            FilledTonalButton(
                onClick = { onPresetClick(preset) },
                modifier = Modifier.offset(x = xOffset, y = yOffset),
            ) {
                Text(text = preset.label)
            }
        }
    }
}
