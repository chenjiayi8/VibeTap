package com.frank.voiceoverlay.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun saveAndRead_roundTripsApiKeyAndPresets() = runTest {
        val store = createStore(backgroundScope)
        val settingsStore = SettingsStore(store)
        val input = AppSettings(
            openAiApiKey = "sk-test",
            presets = listOf(
                ShortcutPreset(
                    id = "ship-pr",
                    label = "Ship PR",
                    text = "Please ship the PR after checks pass.",
                    order = 0,
                    isPinned = true,
                ),
            ),
        )

        settingsStore.save(input)

        assertEquals(input, settingsStore.readOnce())
    }

    @Test
    fun save_scrubsLegacyOverlayEnabledPreferenceFromPersistence() = runTest {
        val store = createStore(backgroundScope)
        store.edit { preferences ->
            preferences[booleanPreferencesKey("overlay_enabled")] = true
        }

        val settingsStore = SettingsStore(store)
        settingsStore.save(AppSettings(openAiApiKey = "sk-test"))

        val persistedPreferences = store.data.first()
        assertEquals("sk-test", settingsStore.readOnce().openAiApiKey)
        assertEquals(null, persistedPreferences[booleanPreferencesKey("overlay_enabled")])
    }

    @Test
    fun update_scrubsLegacyOverlayEnabledPreferenceFromPersistence() = runTest {
        val store = createStore(backgroundScope)
        store.edit { preferences ->
            preferences[stringPreferencesKey("openai_api_key")] = "sk-before"
            preferences[booleanPreferencesKey("overlay_enabled")] = true
        }

        val settingsStore = SettingsStore(store)
        settingsStore.update { it.copy(openAiApiKey = "sk-after") }

        val persistedPreferences = store.data.first()
        assertEquals("sk-after", settingsStore.readOnce().openAiApiKey)
        assertEquals(null, persistedPreferences[booleanPreferencesKey("overlay_enabled")])
    }

    @Test
    fun readOnce_scrubsLegacyOverlayEnabledPreferenceFromPersistence() = runTest {
        val store = createStore(backgroundScope)
        store.edit { preferences ->
            preferences[stringPreferencesKey("openai_api_key")] = "sk-legacy"
            preferences[booleanPreferencesKey("overlay_enabled")] = true
        }

        val settingsStore = SettingsStore(store)

        val settings = settingsStore.readOnce()
        val persistedPreferences = store.data.first()

        assertEquals("sk-legacy", settings.openAiApiKey)
        assertEquals(null, persistedPreferences[booleanPreferencesKey("overlay_enabled")])
    }

    @Test
    fun readOnce_usesDefaultPresetsWhenPresetJsonMissing() = runTest {
        val settingsStore = SettingsStore(createStore(backgroundScope))

        assertEquals(ShortcutPreset.defaultPresets(), settingsStore.readOnce().presets)
    }

    @Test
    fun readOnce_usesDefaultPresetsWhenPresetJsonIsMalformed() = runTest {
        val store = createStore(backgroundScope)
        store.edit { preferences ->
            preferences[stringPreferencesKey("shortcut_presets")] = "not-json"
        }

        val settingsStore = SettingsStore(store)

        assertEquals(ShortcutPreset.defaultPresets(), settingsStore.readOnce().presets)
    }

    @Test
    fun readOnce_decodesLegacyPresetJsonWithoutIsPinned() = runTest {
        val store = createStore(backgroundScope)
        store.edit { preferences ->
            preferences[stringPreferencesKey("shortcut_presets")] =
                """[{"id":"legacy","label":"Legacy","text":"Legacy text","order":4}]"""
        }

        val settingsStore = SettingsStore(store)

        assertEquals(
            listOf(
                ShortcutPreset(
                    id = "legacy",
                    label = "Legacy",
                    text = "Legacy text",
                    order = 4,
                    isPinned = false,
                ),
            ),
            settingsStore.readOnce().presets,
        )
    }

    private fun createStore(scope: kotlinx.coroutines.CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { temporaryFolder.newFile("settings-${System.nanoTime()}.preferences_pb") },
        )
}
