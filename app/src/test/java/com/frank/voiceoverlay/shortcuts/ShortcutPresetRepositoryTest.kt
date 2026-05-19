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
                    label = "Ship PR",
                    text = "Please ship the PR after checks pass.",
                    order = 0,
                ),
                ShortcutPreset(
                    id = "review-pr",
                    label = "Review PR",
                    text = "Please review the PR and call out the highest-risk issues first.",
                    order = 1,
                ),
                ShortcutPreset(
                    id = "proceed",
                    label = "Proceed",
                    text = "Please proceed with the approved plan.",
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
                presets = ShortcutPreset.defaultPresets(),
            ),
        )

        val unsortedPresets = listOf(
            ShortcutPreset("proceed", "Proceed", "Please proceed with the approved plan.", 2),
            ShortcutPreset("ship-pr", "Ship PR", "Please ship the PR after checks pass.", 0),
            ShortcutPreset("review-pr", "Review PR", "Please review the PR and call out the highest-risk issues first.", 1),
        )

        repository.savePresets(unsortedPresets)

        assertEquals(unsortedPresets.sortedBy(ShortcutPreset::order), repository.listPresets())
        assertEquals(
            AppSettings(
                openAiApiKey = "sk-test",
                presets = unsortedPresets.sortedBy(ShortcutPreset::order),
            ),
            settingsStore.readOnce(),
        )
    }
}
