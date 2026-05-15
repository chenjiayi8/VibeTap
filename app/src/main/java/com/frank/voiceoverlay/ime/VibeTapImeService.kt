package com.frank.voiceoverlay.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.frank.voiceoverlay.R
import com.frank.voiceoverlay.dictation.AndroidAudioRecorder
import com.frank.voiceoverlay.dictation.DictationCoordinator
import com.frank.voiceoverlay.dictation.OpenAiCleanupClient
import com.frank.voiceoverlay.dictation.OpenAiTranscriptionClient
import com.frank.voiceoverlay.settings.SettingsStore
import com.frank.voiceoverlay.settings.voiceOverlaySettingsDataStore
import com.frank.voiceoverlay.ime.ui.VibeTapImeRoot
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
        val commitText: (String) -> Boolean = { text -> committer.commitText(text) }

        return ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                MaterialTheme {
                    VibeTapImeRoot(
                        controller = controller,
                        onBackspace = { committer.backspace() },
                        onEnter = { committer.sendEnter() },
                        onCommitLetter = commitText,
                        onCommitPhrase = commitText,
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
