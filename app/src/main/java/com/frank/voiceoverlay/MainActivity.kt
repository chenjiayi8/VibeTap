package com.frank.voiceoverlay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.lifecycleScope
import com.frank.voiceoverlay.permissions.PermissionGate
import com.frank.voiceoverlay.settings.SettingsScreen
import com.frank.voiceoverlay.settings.SettingsStore
import com.frank.voiceoverlay.settings.SettingsViewModel

class MainActivity : ComponentActivity() {
    private val permissionGate by lazy { PermissionGate(applicationContext) }
    private val settingsStore by lazy {
        SettingsStore(
            PreferenceDataStoreFactory.create {
                applicationContext.preferencesDataStoreFile(SETTINGS_FILE_NAME)
            },
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

    private companion object {
        const val SETTINGS_FILE_NAME = "voice_overlay_settings.preferences_pb"
    }
}
