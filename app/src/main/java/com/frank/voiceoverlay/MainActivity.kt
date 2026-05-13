package com.frank.voiceoverlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
            hasOverlayPermission = permissionGate::hasOverlayPermission,
            openOverlaySettings = permissionGate::openOverlaySettings,
            openAccessibilitySettings = permissionGate::openAccessibilitySettings,
            scope = lifecycleScope,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by settingsViewModel.uiState.collectAsState()
            MaterialTheme {
                SettingsScreen(
                    uiState = uiState,
                    onApiKeyChanged = settingsViewModel::onApiKeyChanged,
                    onOverlayEnabledChanged = settingsViewModel::onOverlayEnabledChanged,
                    onPresetChanged = settingsViewModel::onPresetChanged,
                    onOpenOverlaySettings = settingsViewModel::onOpenOverlaySettings,
                    onOpenAccessibilitySettings = settingsViewModel::onOpenAccessibilitySettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel.refreshPermissionState()
    }
}
