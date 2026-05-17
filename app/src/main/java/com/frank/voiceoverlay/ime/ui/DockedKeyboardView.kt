package com.frank.voiceoverlay.ime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp

private val keyboardRows = listOf(
    "QWERTYUIOP",
    "ASDFGHJKL",
    "ZXCVBNM",
)

@Composable
fun DockedKeyboardView(
    onMicTapped: () -> Unit,
    onLayoutToggle: () -> Unit,
    onBackspace: () -> Boolean,
    onEnter: () -> Boolean,
    onCommitLetter: (String) -> Boolean,
    onCommitPhrase: (String) -> Boolean,
) {
    Column(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            KeyButton(label = "Mic", onClick = onMicTapped)
            KeyButton(label = "Float", onClick = onLayoutToggle)
            KeyButton(label = "⌫", onClick = { onBackspace() })
        }

        keyboardRows.forEach { rowLetters ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                rowLetters.forEach { letter ->
                    KeyButton(label = letter.toString(), onClick = { onCommitLetter(letter.toString()) })
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            KeyButton(label = "Space", onClick = { onCommitPhrase(" ") }, weight = 2f)
            KeyButton(label = "Enter", onClick = { onEnter() })
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
    ) {
        Text(text = label)
    }
}

private fun keyTag(label: String): String = when (label) {
    "Mic" -> "key_mic"
    "Float" -> "key_float"
    "⌫" -> "key_backspace"
    "Space" -> "key_space"
    "Enter" -> "key_enter"
    else -> "key_${label.lowercase()}"
}

private fun keyContentDescription(label: String): String = when (label) {
    "Mic" -> "Keyboard mic key"
    "Float" -> "Keyboard float key"
    "⌫" -> "Keyboard backspace key"
    "Space" -> "Keyboard space key"
    "Enter" -> "Keyboard enter key"
    else -> "Keyboard letter $label key"
}
