package com.frank.voiceoverlay.ime.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun FloatingSkillPanel(
    skillBubbles: List<ShortcutPreset>,
    statusMessage: String?,
    onMicTapped: () -> Unit,
    onDockTapped: () -> Unit,
    onSkillBubbleTapped: (ShortcutPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var offsetX by rememberSaveable { mutableIntStateOf(0) }
    var offsetY by rememberSaveable { mutableIntStateOf(0) }
    var panelWidthPx by rememberSaveable { mutableIntStateOf(0) }
    var panelHeightPx by rememberSaveable { mutableIntStateOf(0) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("floating_skill_panel_viewport"),
    ) {
        val viewportWidthPx = constraints.maxWidth
        val viewportHeightPx = constraints.maxHeight
        val maxOffsetX = max(0, viewportWidthPx - panelWidthPx)
        val maxOffsetY = max(0, viewportHeightPx - panelHeightPx)

        Surface(
            modifier = Modifier
                .offset { IntOffset(offsetX, offsetY) }
                .onSizeChanged { size ->
                    panelWidthPx = size.width
                    panelHeightPx = size.height
                    offsetX = offsetX.coerceIn(0, max(0, viewportWidthPx - size.width))
                    offsetY = offsetY.coerceIn(0, max(0, viewportHeightPx - size.height))
                }
                .pointerInput(viewportWidthPx, viewportHeightPx, panelWidthPx, panelHeightPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x.roundToInt()).coerceIn(0, maxOffsetX)
                        offsetY = (offsetY + dragAmount.y.roundToInt()).coerceIn(0, maxOffsetY)
                    }
                }
                .testTag("floating_skill_panel"),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onMicTapped,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("floating_mic"),
                    ) {
                        Text(text = "Mic")
                    }
                    Button(
                        onClick = onDockTapped,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("floating_dock"),
                    ) {
                        Text(text = "Dock")
                    }
                }

                if (skillBubbles.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        skillBubbles.take(3).forEach { preset ->
                            Button(
                                onClick = { onSkillBubbleTapped(preset) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("floating_skill_${preset.id}"),
                            ) {
                                Text(text = preset.label)
                            }
                        }
                    }
                }

                statusMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
