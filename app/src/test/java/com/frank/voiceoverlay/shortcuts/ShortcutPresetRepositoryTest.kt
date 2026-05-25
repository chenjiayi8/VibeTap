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
                    isPinned = true,
                ),
                ShortcutPreset(
                    id = "review-pr",
                    label = "Review PR",
                    text = "Please review the PR and call out the highest-risk issues first.",
                    order = 1,
                    isPinned = true,
                ),
                ShortcutPreset(
                    id = "proceed",
                    label = "Proceed",
                    text = "Please proceed with the approved plan.",
                    order = 2,
                    isPinned = false,
                ),
            ),
            ShortcutPreset.defaultPresets(),
        )
    }

    @Test
    fun savePresets_normalizesPinnedOrderingAndPreservesOtherSettings() = runTest {
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
            ShortcutPreset("proceed", "Proceed", "Please proceed with the approved plan.", 8, true),
            ShortcutPreset("review-pr", "Review PR", "Please review the PR and call out the highest-risk issues first.", 9, false),
            ShortcutPreset("ship-pr", "Ship PR", "Please ship the PR after checks pass.", 3, true),
        )

        repository.savePresets(unsortedPresets)

        val persistedPresets = repository.listPresets()
        assertEquals(listOf("ship-pr", "proceed", "review-pr"), persistedPresets.map(ShortcutPreset::id))
        assertEquals(listOf(true, true, false), persistedPresets.map(ShortcutPreset::isPinned))
        assertEquals(listOf(0, 1, 2), persistedPresets.map(ShortcutPreset::order))
        assertEquals(
            AppSettings(
                openAiApiKey = "sk-test",
                presets = persistedPresets,
            ),
            settingsStore.readOnce(),
        )
    }

    @Test
    fun addPreset_normalizesOrderWithinSingleUpdateTransaction() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { temporaryFolder.newFile("shortcut-presets-add.preferences_pb") },
        )
        val settingsStore = SettingsStore(dataStore)
        val repository = ShortcutPresetRepository(settingsStore)

        settingsStore.save(
            AppSettings(
                openAiApiKey = "sk-test",
                presets = listOf(
                    ShortcutPreset("ship-pr", "Ship PR", "Please ship the PR after checks pass.", 5, true),
                    ShortcutPreset("proceed", "Proceed", "Please proceed with the approved plan.", 9, false),
                ),
            ),
        )

        repository.addPreset(
            ShortcutPreset(
                id = "review-pr",
                label = "Review PR",
                text = "Please review the PR and call out the highest-risk issues first.",
                order = 7,
                isPinned = true,
            ),
        )

        val persistedPresets = repository.listPresets()
        assertEquals(listOf("ship-pr", "review-pr", "proceed"), persistedPresets.map(ShortcutPreset::id))
        assertEquals(listOf(true, true, false), persistedPresets.map(ShortcutPreset::isPinned))
        assertEquals(listOf(0, 1, 2), persistedPresets.map(ShortcutPreset::order))
        assertEquals("sk-test", settingsStore.readOnce().openAiApiKey)
    }
}
