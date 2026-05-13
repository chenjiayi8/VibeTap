package com.frank.voiceoverlay.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun reloadSettings_clearsPersistedOverlayFlagWhenPermissionMissing() = runTest {
        val settingsStore = SettingsStore(createStore(backgroundScope))
        settingsStore.save(
            AppSettings(
                openAiApiKey = "sk-test",
                overlayEnabled = true,
                presets = ShortcutPreset.defaultPresets(),
            ),
        )
        val viewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasOverlayPermission = { false },
            openOverlaySettings = {},
            openAccessibilitySettings = {},
            scope = this,
        )

        viewModel.reloadSettings()

        assertFalse(viewModel.uiState.value.overlayEnabled)
        assertFalse(settingsStore.readOnce().overlayEnabled)
    }

    @Test
    fun savePresetChange_trimsAndPersistsEditsAcrossReload() = runTest {
        val settingsStore = SettingsStore(createStore(backgroundScope))
        val initialPreset = ShortcutPreset(
            id = "ship-pr",
            label = "Ship-PR",
            text = "Good, please proceed to use \$ship-pr",
            order = 0,
        )
        settingsStore.save(
            AppSettings(
                openAiApiKey = "sk-test",
                overlayEnabled = false,
                presets = listOf(initialPreset),
            ),
        )

        val viewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasOverlayPermission = { true },
            openOverlaySettings = {},
            openAccessibilitySettings = {},
            scope = this,
        )

        assertTrue(
            viewModel.savePresetChange(
                presetId = initialPreset.id,
                label = "  Ship now  ",
                text = "  Please land and ship this PR.  ",
            ),
        )

        val reloadedViewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasOverlayPermission = { true },
            openOverlaySettings = {},
            openAccessibilitySettings = {},
            scope = this,
        )
        reloadedViewModel.reloadSettings()

        val updatedPreset = reloadedViewModel.uiState.value.presets.single()
        assertEquals("Ship now", updatedPreset.label)
        assertEquals("Please land and ship this PR.", updatedPreset.text)
        assertEquals(listOf(updatedPreset), settingsStore.readOnce().presets)
    }

    @Test
    fun savePresetChange_rejectsBlankValuesWithoutPersisting() = runTest {
        val settingsStore = SettingsStore(createStore(backgroundScope))
        val initialPreset = ShortcutPreset.defaultPresets().first()
        settingsStore.save(AppSettings(presets = listOf(initialPreset)))
        val viewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasOverlayPermission = { true },
            openOverlaySettings = {},
            openAccessibilitySettings = {},
            scope = this,
        )

        val saved = viewModel.savePresetChange(
            presetId = initialPreset.id,
            label = "   ",
            text = "kept",
        )

        assertFalse(saved)
        assertEquals(listOf(initialPreset), settingsStore.readOnce().presets)
    }

    private fun createStore(scope: kotlinx.coroutines.CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { temporaryFolder.newFile("settings-viewmodel-${System.nanoTime()}.preferences_pb") },
        )
}
