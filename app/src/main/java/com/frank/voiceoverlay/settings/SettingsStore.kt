package com.frank.voiceoverlay.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
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
        writeSettings(settings)
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            val updatedSettings = transform(preferences.toAppSettings())
            preferences.writeSettings(updatedSettings)
        }
    }

    suspend fun readOnce(): AppSettings =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .first()
            .toAppSettings()

    private suspend fun writeSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences.writeSettings(settings)
        }
    }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        openAiApiKey = this[OPENAI_API_KEY].orEmpty(),
        presets = decodePresets(this[SHORTCUT_PRESETS]),
    )

    private fun MutablePreferences.writeSettings(settings: AppSettings) {
        remove(OVERLAY_ENABLED)
        this[OPENAI_API_KEY] = settings.openAiApiKey
        this[SHORTCUT_PRESETS] = json.encodeToString(settings.presets)
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
