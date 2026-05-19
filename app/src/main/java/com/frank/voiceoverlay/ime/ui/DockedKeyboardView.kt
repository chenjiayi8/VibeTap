package com.frank.voiceoverlay.ime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val keyboardRows = listOf(
    "QWERTYUIOP",
    "ASDFGHJKL",
    "ZXCVBNM",
)

@Composable
fun DockedKeyboardView(
    bottomInsetPadding: Dp,
    onMicTapped: () -> Unit,
    onLayoutToggle: () -> Unit,
    onBackspace: () -> Boolean,
    onEnter: () -> Boolean,
    onCommitLetter: (String) -> Boolean,
    onCommitPhrase: (String) -> Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp + bottomInsetPadding)
            .testTag("docked_keyboard_root")
            .semantics { testTagsAsResourceId = true },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KeyButton(label = "Mic", onClick = onMicTapped)
            KeyButton(label = "Actions", onClick = onLayoutToggle)
        }

        keyboardRows.forEach { rowLetters ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rowLetters.forEach { letter ->
                    KeyButton(label = letter.toString(), onClick = { onCommitLetter(letter.toString()) })
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            KeyButton(label = "Space", onClick = { onCommitPhrase(" ") }, weight = 2.2f)
            KeyButton(label = "Enter", onClick = { onEnter() }, weight = 1f)
            KeyButton(label = "⌫", onClick = { onBackspace() }, weight = 0.8f)
        }
    }
}

@Composable
private fun RowScope.KeyButton(
    label: String,
    onClick: () -> Unit,
    weight: Float = 1f,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(weight)
            .testTag(keyTag(label))
            .semantics { contentDescription = keyContentDescription(label) },
        contentPadding = PaddingValues(vertical = 14.dp),
    ) {
        Text(text = label)
    }
}

private fun keyTag(label: String): String = when (label) {
    "Mic" -> "key_mic"
    "Actions" -> "key_actions"
    "⌫" -> "key_backspace"
    "Space" -> "key_space"
    "Enter" -> "key_enter"
    else -> "key_${label.lowercase()}"
}

private fun keyContentDescription(label: String): String = when (label) {
    "Mic" -> "Keyboard mic key"
    "Actions" -> "Keyboard actions key"
    "⌫" -> "Keyboard backspace key"
    "Space" -> "Keyboard space key"
    "Enter" -> "Keyboard enter key"
    else -> "Keyboard letter $label key"
}
