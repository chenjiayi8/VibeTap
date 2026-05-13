package com.frank.voiceoverlay

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.frank.voiceoverlay.overlay.OverlayBubbleService
import com.frank.voiceoverlay.overlay.OverlayBubbleServiceGate
import com.frank.voiceoverlay.permissions.PermissionGate
import com.frank.voiceoverlay.settings.SettingsScreen
import com.frank.voiceoverlay.settings.SettingsStore
import com.frank.voiceoverlay.settings.SettingsViewModel
import com.frank.voiceoverlay.settings.voiceOverlaySettingsDataStore

class MainActivity : ComponentActivity() {
    private val permissionGate by lazy { PermissionGate(applicationContext) }
    private val settingsStore by lazy {
        SettingsStore(
            applicationContext.voiceOverlaySettingsDataStore,
        )
    }
    private val settingsViewModel by lazy {
        SettingsViewModel(
            settingsStore = settingsStore,
            hasOverlayPermission = permissionGate::hasOverlayPermission,
            hasMicrophonePermission = permissionGate::hasMicrophonePermission,
            openOverlaySettings = permissionGate::openOverlaySettings,
            openAccessibilitySettings = permissionGate::openAccessibilitySettings,
            requestMicrophonePermission = {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    MICROPHONE_PERMISSION_REQUEST_CODE,
                )
            },
            scope = lifecycleScope,
        )
    }
    private val overlayBubbleServiceGate by lazy {
        OverlayBubbleServiceGate(
            startService = {
                startService(Intent(this, OverlayBubbleService::class.java))
            },
            stopService = {
                stopService(Intent(this, OverlayBubbleService::class.java))
            },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by settingsViewModel.uiState.collectAsState()
            LaunchedEffect(uiState.overlayEnabled) {
                overlayBubbleServiceGate.sync(uiState.overlayEnabled)
            }
            MaterialTheme {
                SettingsScreen(
                    uiState = uiState,
                    onApiKeyChanged = settingsViewModel::onApiKeyChanged,
                    onOverlayEnabledChanged = settingsViewModel::onOverlayEnabledChanged,
                    onPresetChanged = settingsViewModel::onPresetChanged,
                    onOpenOverlaySettings = settingsViewModel::onOpenOverlaySettings,
                    onOpenAccessibilitySettings = settingsViewModel::onOpenAccessibilitySettings,
                    onRequestMicrophonePermission = settingsViewModel::onRequestMicrophonePermission,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel.refreshPermissionState()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MICROPHONE_PERMISSION_REQUEST_CODE) {
            settingsViewModel.refreshPermissionState()
        }
    }

    private companion object {
        const val MICROPHONE_PERMISSION_REQUEST_CODE = 1001
    }
}
