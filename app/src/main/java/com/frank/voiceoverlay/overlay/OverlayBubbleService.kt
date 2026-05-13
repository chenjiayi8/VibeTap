package com.frank.voiceoverlay.overlay

import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.frank.voiceoverlay.dictation.AndroidAudioRecorder
import com.frank.voiceoverlay.dictation.DictationCoordinator
import com.frank.voiceoverlay.dictation.OpenAiCleanupClient
import com.frank.voiceoverlay.dictation.OpenAiTranscriptionClient
import com.frank.voiceoverlay.insertion.OverlayAccessibilityService
import com.frank.voiceoverlay.settings.SettingsStore
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayBubbleService : LifecycleService() {
    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView

    private val settingsStore by lazy {
        SettingsStore(
            PreferenceDataStoreFactory.create {
                applicationContext.preferencesDataStoreFile(SETTINGS_FILE_NAME)
            },
        )
    }
    private val coordinator by lazy(::createCoordinator)
    private val controller by lazy {
        OverlayBubbleController(
            recordingState = coordinator.state,
            refreshSettingsSnapshot = settingsStore::readOnce,
            startRecording = coordinator::startRecording,
            stopRecording = coordinator::stopRecording,
            resetRecording = coordinator::reset,
            insertShortcutText = ::insertIntoFocusedField,
        )
    }

    private var pendingTapResolutionJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val currentState by controller.uiState.collectAsState()
                MaterialTheme {
                    OverlayBubbleView(
                        uiState = currentState,
                        onBubbleTap = ::handleBubbleTap,
                        onShortcutTap = ::handleShortcutTap,
                    )
                }
            }
        }

        windowManager.addView(composeView, overlayLayoutParams())
        controller.bind(lifecycleScope)
    }

    override fun onDestroy() {
        pendingTapResolutionJob?.cancel()
        if (::composeView.isInitialized && composeView.parent != null) {
            windowManager.removeView(composeView)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun handleBubbleTap() {
        lifecycleScope.launch {
            applyBubbleCommand(controller.onBubbleTap(SystemClock.elapsedRealtime()))
        }
    }

    private fun handleShortcutTap(preset: com.frank.voiceoverlay.shortcuts.ShortcutPreset) {
        lifecycleScope.launch {
            controller.onShortcutTap(preset)
        }
    }

    private fun applyBubbleCommand(command: OverlayBubbleCommand) {
        when (command) {
            OverlayBubbleCommand.CancelPendingSingleTapResolution -> pendingTapResolutionJob?.cancel()
            OverlayBubbleCommand.None -> Unit
            is OverlayBubbleCommand.ScheduleSingleTapResolution -> {
                pendingTapResolutionJob?.cancel()
                pendingTapResolutionJob = lifecycleScope.launch {
                    delay(command.delayMillis)
                    controller.resolvePendingBubbleTap(SystemClock.elapsedRealtime())
                }
            }
        }
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
                check(insertIntoFocusedField(text)) {
                    DICTATION_INSERTION_STATUS_MESSAGE
                }
            },
            deleteFile = File::delete,
        )
    }

    private fun requireCurrentApiKey(): String = controller.requireCurrentApiKey()

    private fun insertIntoFocusedField(text: String): Boolean =
        OverlayAccessibilityService.activeInstance()?.insert(text) == true

    private fun overlayLayoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 32
        y = 240
    }

    private companion object {
        const val OPENAI_BASE_URL = "https://api.openai.com"
        const val SETTINGS_FILE_NAME = "voice_overlay_settings.preferences_pb"
        const val DICTATION_INSERTION_STATUS_MESSAGE =
            "Enable the accessibility service and focus a text field before inserting dictated text."
    }
}
