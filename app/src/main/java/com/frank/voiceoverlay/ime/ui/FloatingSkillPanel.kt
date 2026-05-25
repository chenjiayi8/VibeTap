package com.frank.voiceoverlay.ime.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.frank.voiceoverlay.ime.ImeStatusTone
import com.frank.voiceoverlay.shortcuts.ShortcutPreset

private val innerRingOffsets = listOf(
    DpOffset(0.dp, (-92).dp),
    DpOffset((-82).dp, (-24).dp),
    DpOffset(82.dp, (-24).dp),
)

private val outerRingOffsets = listOf(
    DpOffset((-122).dp, (-98).dp),
    DpOffset(122.dp, (-98).dp),
    DpOffset((-122).dp, 76.dp),
    DpOffset(122.dp, 76.dp),
)

private val modeSwitchOffset = DpOffset((-122).dp, 0.dp)
private val pagePreviousOffset = DpOffset((-122).dp, 132.dp)
private val pageNextOffset = DpOffset(122.dp, 132.dp)

@Composable
fun FloatingSkillPanel(
    orbitExpanded: Boolean,
    innerRingBubbles: List<ShortcutPreset>,
    outerRingBubbles: List<ShortcutPreset>,
    canPageBackward: Boolean,
    canPageForward: Boolean,
    statusMessage: String?,
    statusTone: ImeStatusTone,
    onMicTapped: () -> Unit,
    onMicDoubleTapped: () -> Unit,
    onDockTapped: () -> Unit,
    onSkillBubbleTapped: (ShortcutPreset) -> Unit,
    onPreviousPageTapped: () -> Unit,
    onNextPageTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("floating_skill_panel_viewport"),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (orbitExpanded) {
                OrbitBubble(
                    label = "Dock",
                    tag = "bubble_mode_switch",
                    offset = modeSwitchOffset,
                    onClick = onDockTapped,
                )

                innerRingBubbles.take(innerRingOffsets.size).forEachIndexed { index, preset ->
                    OrbitBubble(
                        label = preset.label,
                        tag = "bubble_skill_${preset.id}",
                        offset = innerRingOffsets[index],
                        onClick = { onSkillBubbleTapped(preset) },
                    )
                }

                outerRingBubbles.take(outerRingOffsets.size).forEachIndexed { index, preset ->
                    OrbitBubble(
                        label = preset.label,
                        tag = "bubble_skill_${preset.id}",
                        offset = outerRingOffsets[index],
                        onClick = { onSkillBubbleTapped(preset) },
                    )
                }

                if (canPageBackward) {
                    OrbitBubble(
                        label = "Prev",
                        tag = "bubble_page_prev",
                        offset = pagePreviousOffset,
                        onClick = onPreviousPageTapped,
                    )
                }

                if (canPageForward) {
                    OrbitBubble(
                        label = "Next",
                        tag = "bubble_page_next",
                        offset = pageNextOffset,
                        onClick = onNextPageTapped,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .testTag("bubble_primary_mic")
                    .combinedClickable(
                        role = Role.Button,
                        onClick = onMicTapped,
                        onDoubleClick = onMicDoubleTapped,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Mic",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            statusMessage?.let { message ->
                Text(
                    text = message,
                    color = when (statusTone) {
                        ImeStatusTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
                        ImeStatusTone.Warning -> MaterialTheme.colorScheme.tertiary
                        ImeStatusTone.Error -> MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.offset(y = 184.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun OrbitBubble(
    label: String,
    tag: String,
    offset: DpOffset,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .offset(x = offset.x, y = offset.y)
            .testTag(tag),
    ) {
        Text(text = label, textAlign = TextAlign.Center)
    }
}
