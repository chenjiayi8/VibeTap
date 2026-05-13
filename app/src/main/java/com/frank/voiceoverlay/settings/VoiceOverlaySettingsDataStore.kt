package com.frank.voiceoverlay.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

private const val SETTINGS_FILE_NAME = "voice_overlay_settings"

val Context.voiceOverlaySettingsDataStore by preferencesDataStore(
    name = SETTINGS_FILE_NAME,
)
