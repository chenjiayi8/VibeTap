package com.frank.voiceoverlay.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
            overlayEnabled = true,
            presets = listOf(ShortcutPreset("ship-pr", "Ship-PR", "Good, please proceed to use \$ship-pr", 0)),
        )

        settingsStore.save(input)

        assertEquals(input, settingsStore.readOnce())
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

    private fun createStore(scope: kotlinx.coroutines.CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { temporaryFolder.newFile("settings-${System.nanoTime()}.preferences_pb") },
        )
}
