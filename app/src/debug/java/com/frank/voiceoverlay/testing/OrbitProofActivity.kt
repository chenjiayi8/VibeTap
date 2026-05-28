package com.frank.voiceoverlay.testing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.frank.voiceoverlay.dictation.RecordingState
import com.frank.voiceoverlay.ime.VoiceKeyboardController
import com.frank.voiceoverlay.ime.ui.VibeTapImeRoot
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.flow.MutableStateFlow

class OrbitProofActivity : ComponentActivity() {
    private val recordingState = MutableStateFlow(RecordingState.IDLE)

    private val proofPresets = listOf(
        proofPreset(id = "ship", label = "Ship", text = "SHIP-PROOF", order = 0, isPinned = true),
        proofPreset(id = "review", label = "Review", text = "REVIEW-PROOF", order = 1, isPinned = true),
        proofPreset(id = "proceed", label = "Proceed", text = "PROCEED-PROOF", order = 2, isPinned = true),
        proofPreset(id = "extra", label = "Extra", text = "EXTRA-PROOF", order = 3),
        proofPreset(id = "ask", label = "Ask", text = "ASK-PROOF", order = 4),
        proofPreset(id = "plan", label = "Plan", text = "PLAN-PROOF", order = 5),
        proofPreset(id = "deploy", label = "Deploy", text = "DEPLOY-PROOF", order = 6),
        proofPreset(id = "escalate", label = "Escalate", text = "ESCALATE-PROOF", order = 7),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var proofText by mutableStateOf("")
        val controller = VoiceKeyboardController(
            recordingState = recordingState,
            shortcutsProvider = { proofPresets },
            startRecording = { recordingState.value = RecordingState.LISTENING },
            stopRecording = { recordingState.value = RecordingState.PROCESSING },
            resetRecording = { recordingState.value = RecordingState.IDLE },
            commitPhrase = { text ->
                proofText += text
                true
            },
        )
        controller.bind(lifecycleScope)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Orbit Proof Harness",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = "Use Float, then double-tap Mic, page with Next, and tap a macro. Inserted output appears below.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                        ) {
                            Text(
                                text = if (proofText.isEmpty()) "Proof output will appear here." else proofText,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Button(onClick = { proofText = "" }) {
                            Text("Clear proof output")
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(760.dp),
                            tonalElevation = 1.dp,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                VibeTapImeRoot(
                                    controller = controller,
                                    onBackspace = {
                                        proofText = proofText.dropLast(1)
                                        true
                                    },
                                    onEnter = {
                                        proofText += "\n"
                                        true
                                    },
                                    onCommitLetter = { letter ->
                                        proofText += letter
                                        true
                                    },
                                    onCommitPhrase = { text ->
                                        proofText += text
                                        true
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun proofPreset(
    id: String,
    label: String,
    text: String,
    order: Int,
    isPinned: Boolean = false,
): ShortcutPreset = ShortcutPreset(
    id = id,
    label = label,
    text = text,
    order = order,
    isPinned = isPinned,
)
