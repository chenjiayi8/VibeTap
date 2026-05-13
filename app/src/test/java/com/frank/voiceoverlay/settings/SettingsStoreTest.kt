package com.frank.voiceoverlay.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
        val store: DataStore<androidx.datastore.preferences.core.Preferences> =
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { temporaryFolder.newFile("settings.preferences_pb") },
            )

        val settingsStore = SettingsStore(store)
        val input = AppSettings(
            openAiApiKey = "sk-test",
            overlayEnabled = true,
            presets = listOf(ShortcutPreset("ship-pr", "Ship-PR", "Good, please proceed to use \$ship-pr", 0)),
        )

        settingsStore.save(input)

        assertEquals(input, settingsStore.readOnce())
    }
}
