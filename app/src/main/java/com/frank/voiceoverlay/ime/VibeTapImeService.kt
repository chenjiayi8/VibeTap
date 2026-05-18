package com.frank.voiceoverlay.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.frank.voiceoverlay.R
import com.frank.voiceoverlay.dictation.AndroidAudioRecorder
import com.frank.voiceoverlay.dictation.DictationCoordinator
import com.frank.voiceoverlay.dictation.OpenAiCleanupClient
import com.frank.voiceoverlay.dictation.OpenAiTranscriptionClient
import com.frank.voiceoverlay.ime.ui.VibeTapImeRoot
import com.frank.voiceoverlay.settings.SettingsStore
import com.frank.voiceoverlay.settings.voiceOverlaySettingsDataStore
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
    private var inputViewOwners: ImeInputViewTreeOwners? = null

    override fun onCreateInputView(): View {
        controller.bind(serviceScope)
        inputViewOwners?.destroy()

        val owners = ImeInputViewTreeOwners().apply { resume() }
        val commitText: (String) -> Boolean = { text -> committer.commitText(text) }

        return ComposeView(this).apply {
            owners.installOn(this)
            this@VibeTapImeService.window?.window?.decorView?.let(owners::installOn)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
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
        }.also { inputViewOwners = owners }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        inputViewOwners?.resume()
        window?.window?.decorView?.let { rootView ->
            inputViewOwners?.installOn(rootView)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        inputViewOwners?.stop()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        inputViewOwners?.destroy()
        inputViewOwners = null
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

private class ImeInputViewTreeOwners : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun installOn(view: View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }

    fun resume() {
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            return
        }
        if (lifecycle.currentState == Lifecycle.State.CREATED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
        if (lifecycle.currentState == Lifecycle.State.STARTED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    fun stop() {
        if (lifecycle.currentState == Lifecycle.State.RESUMED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (lifecycle.currentState == Lifecycle.State.STARTED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    fun destroy() {
        stop()
        if (lifecycle.currentState == Lifecycle.State.CREATED) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }
}
