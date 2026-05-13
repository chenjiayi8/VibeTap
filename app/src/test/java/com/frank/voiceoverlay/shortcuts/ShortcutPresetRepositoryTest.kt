package com.frank.voiceoverlay.shortcuts

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.frank.voiceoverlay.settings.AppSettings
import com.frank.voiceoverlay.settings.SettingsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ShortcutPresetRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun defaultPresets_matchApprovedBaseline() {
        assertEquals(
            listOf(
                ShortcutPreset(
                    id = "ship-pr",
                    label = "Ship-PR",
                    text = "Good, please proceed to use \$ship-pr",
                    order = 0,
                ),
                ShortcutPreset(
                    id = "review",
                    label = "Review",
                    text = "Please review the latest changes carefully.",
                    order = 1,
                ),
                ShortcutPreset(
                    id = "proceed",
                    label = "Proceed",
                    text = "Good, please proceed.",
                    order = 2,
                ),
            ),
            ShortcutPreset.defaultPresets(),
        )
    }

    @Test
    fun savePresets_sortsAndPreservesOtherSettings() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { temporaryFolder.newFile("shortcut-presets.preferences_pb") },
        )
        val settingsStore = SettingsStore(dataStore)
        val repository = ShortcutPresetRepository(settingsStore)

        settingsStore.save(
            AppSettings(
                openAiApiKey = "sk-test",
                overlayEnabled = true,
                presets = ShortcutPreset.defaultPresets(),
            ),
        )

        val unsortedPresets = listOf(
            ShortcutPreset("proceed", "Proceed", "Good, please proceed.", 2),
            ShortcutPreset("ship-pr", "Ship-PR", "Good, please proceed to use \$ship-pr", 0),
            ShortcutPreset("review", "Review", "Please review the latest changes carefully.", 1),
        )

        repository.savePresets(unsortedPresets)

        assertEquals(unsortedPresets.sortedBy(ShortcutPreset::order), repository.listPresets())
        assertEquals(
            AppSettings(
                openAiApiKey = "sk-test",
                overlayEnabled = true,
                presets = unsortedPresets.sortedBy(ShortcutPreset::order),
            ),
            settingsStore.readOnce(),
        )
    }
}
