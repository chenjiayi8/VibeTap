package com.frank.voiceoverlay.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.runtime.R as LifecycleRuntimeR
import androidx.lifecycle.viewmodel.R as LifecycleViewModelR
import androidx.savedstate.R as SavedStateR

class VibeTapImeService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val recomposer by lazy { Recomposer(AndroidUiDispatcher.Main) }
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val serviceViewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

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

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = serviceViewModelStore

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        serviceScope.launch(AndroidUiDispatcher.Main) {
            recomposer.runRecomposeAndApplyChanges()
        }
    }

    override fun onCreateInputView(): View {
        controller.bind(serviceScope)
        val commitText: (String) -> Boolean = { text -> committer.commitText(text) }

        return ComposeView(this).apply {
            setTag(LifecycleRuntimeR.id.view_tree_lifecycle_owner, this@VibeTapImeService)
            setTag(LifecycleViewModelR.id.view_tree_view_model_store_owner, this@VibeTapImeService)
            setTag(SavedStateR.id.view_tree_saved_state_registry_owner, this@VibeTapImeService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setParentCompositionContext(recomposer)
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
        recomposer.cancel()
        serviceScope.cancel()
        serviceViewModelStore.clear()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
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
