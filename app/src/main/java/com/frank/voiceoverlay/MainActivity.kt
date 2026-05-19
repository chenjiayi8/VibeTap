package com.frank.voiceoverlay

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.frank.voiceoverlay.ime.ui.VibeTapTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
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
            hasMicrophonePermission = permissionGate::hasMicrophonePermission,
            openKeyboardSettings = {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            },
            showInputMethodPicker = {
                getSystemService(InputMethodManager::class.java)?.showInputMethodPicker()
            },
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by settingsViewModel.uiState.collectAsState()
            VibeTapTheme {
                SettingsScreen(
                    uiState = uiState,
                    onApiKeyChanged = settingsViewModel::onApiKeyChanged,
                    onPresetChanged = settingsViewModel::onPresetChanged,
                    onOpenKeyboardSettings = settingsViewModel::onOpenKeyboardSettings,
                    onShowInputMethodPicker = settingsViewModel::onShowInputMethodPicker,
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
