package com.frank.voiceoverlay.ime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

@Composable
private fun RowScope.KeyButton(
    label: String,
    onClick: () -> Unit,
    weight: Float = 1f,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(weight),
    ) {
        Text(text = label)
    }
}
