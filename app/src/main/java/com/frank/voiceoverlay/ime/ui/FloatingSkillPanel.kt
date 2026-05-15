package com.frank.voiceoverlay.ime.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
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

    Surface(
        modifier = modifier
            .offset { IntOffset(offsetX, offsetY) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x.roundToInt()
                    offsetY += dragAmount.y.roundToInt()
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
