package com.frank.voiceoverlay.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.IOException

class SettingsStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = Json,
) {
    suspend fun save(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[OPENAI_API_KEY] = settings.openAiApiKey
            preferences[OVERLAY_ENABLED] = settings.overlayEnabled
            preferences[SHORTCUT_PRESETS] = json.encodeToString(settings.presets)
        }
    }

    suspend fun readOnce(): AppSettings {
        val preferences = dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .first()

        return AppSettings(
            openAiApiKey = preferences[OPENAI_API_KEY].orEmpty(),
            overlayEnabled = preferences[OVERLAY_ENABLED] ?: false,
            presets = decodePresets(preferences[SHORTCUT_PRESETS]),
        )
    }

    private fun decodePresets(rawPresets: String?): List<ShortcutPreset> =
        rawPresets
            ?.takeIf { it.isNotBlank() }
            ?.let { encoded -> runCatching { json.decodeFromString<List<ShortcutPreset>>(encoded) }.getOrNull() }
            ?: ShortcutPreset.defaultPresets()

    private companion object {
        val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val SHORTCUT_PRESETS = stringPreferencesKey("shortcut_presets")
    }
}
