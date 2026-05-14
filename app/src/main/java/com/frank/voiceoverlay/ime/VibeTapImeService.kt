package com.frank.voiceoverlay.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.frank.voiceoverlay.R
import com.frank.voiceoverlay.dictation.AndroidAudioRecorder
import com.frank.voiceoverlay.dictation.DictationCoordinator
import com.frank.voiceoverlay.dictation.OpenAiCleanupClient
import com.frank.voiceoverlay.dictation.OpenAiTranscriptionClient
import com.frank.voiceoverlay.settings.SettingsStore
import com.frank.voiceoverlay.settings.voiceOverlaySettingsDataStore
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class VibeTapImeService : InputMethodService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val settingsStore by lazy {
        SettingsStore(applicationContext.voiceOverlaySettingsDataStore)
    }
    private val committer by lazy {
        InputConnectionCommitter(::getCurrentInputConnection)
    }
    private val coordinator by lazy(::createCoordinator)
    private val controller by lazy {
        VoiceKeyboardController(
            recordingState = coordinator.state,
            shortcutsProvider = { settingsStore.readOnce().presets },
            startRecording = coordinator::startRecording,
            stopRecording = coordinator::stopRecording,
            resetRecording = coordinator::reset,
            commitPhrase = { text -> committer.commitText(text) },
        )
    }

    override fun onCreateInputView(): View {
        controller.bind(serviceScope)

        return ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val uiState by controller.uiState.collectAsState()
                MaterialTheme {
                    VibeTapImeRoot(
                        uiState = uiState,
                        onBackspace = { committer.backspace() },
                        onEnter = { committer.sendEnter() },
                        onCommitLetter = { letter -> committer.commitText(letter) },
                        onCommitPhrase = { phrase -> committer.commitText(phrase) },
                        onLayoutToggle = controller::onLayoutToggle,
                        onMicTapped = {
                            serviceScope.launch {
                                controller.onMicTapped()
                            }
                        },
                        onShortcutTapped = { preset ->
                            serviceScope.launch {
                                controller.onSkillBubbleTapped(preset)
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createCoordinator(): DictationCoordinator {
        val transcriptionClient = OpenAiTranscriptionClient(
            baseUrl = OPENAI_BASE_URL,
            apiKeyProvider = ::requireCurrentApiKey,
        )
        val cleanupClient = OpenAiCleanupClient(
            baseUrl = OPENAI_BASE_URL,
            apiKeyProvider = ::requireCurrentApiKey,
        )

        return DictationCoordinator(
            recorder = AndroidAudioRecorder(applicationContext),
            transcribeFile = transcriptionClient::transcribe,
            cleanText = cleanupClient::clean,
            insertText = { text ->
                if (!committer.commitText(text)) {
                    error(getString(R.string.no_active_input_connection))
                }
            },
            deleteFile = File::delete,
        )
    }

    private fun requireCurrentApiKey(): String {
        val apiKey = runBlocking { settingsStore.readOnce().openAiApiKey }.trim()
        if (apiKey.isBlank()) {
            error(getString(R.string.dictation_requires_api_key))
        }
        return apiKey
    }

    companion object {
        const val OPENAI_BASE_URL = "https://api.openai.com"
    }
}

@Composable
private fun VibeTapImeRoot(
    uiState: ImeUiState,
    onBackspace: () -> Boolean,
    onEnter: () -> Boolean,
    onCommitLetter: (String) -> Boolean,
    onCommitPhrase: (String) -> Boolean,
    onLayoutToggle: () -> Unit,
    onMicTapped: () -> Unit,
    onShortcutTapped: (ShortcutPreset) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onMicTapped) {
                Text(text = uiState.recordingState.name)
            }
            Button(onClick = onLayoutToggle) {
                Text(text = uiState.layoutMode.name)
            }
            Button(onClick = { onBackspace() }) {
                Text(text = "⌫")
            }
            Button(onClick = { onEnter() }) {
                Text(text = "Enter")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("A", "B", "C").forEach { letter ->
                Button(onClick = { onCommitLetter(letter) }) {
                    Text(text = letter)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onCommitPhrase(" ") }) {
                Text(text = "Space")
            }
            Button(onClick = { onCommitPhrase(".") }) {
                Text(text = ".")
            }
        }

        uiState.skillBubbles.forEach { preset ->
            Button(onClick = { onShortcutTapped(preset) }) {
                Text(text = preset.label)
            }
        }

        uiState.statusMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
