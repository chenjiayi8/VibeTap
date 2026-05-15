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
    fun reloadSettings_reportsKeyboardFirstState() = runTest {
        val settingsStore = SettingsStore(createStore(backgroundScope))
        val initialPreset = ShortcutPreset.defaultPresets().first()
        settingsStore.save(
            AppSettings(
                openAiApiKey = "sk-test",
                presets = listOf(initialPreset),
            ),
        )
        val viewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasMicrophonePermission = { false },
            openKeyboardSettings = {},
            showInputMethodPicker = {},
            requestMicrophonePermission = {},
            scope = this,
        )

        viewModel.reloadSettings()

        assertEquals("sk-test", viewModel.uiState.value.openAiApiKey)
        assertFalse(viewModel.uiState.value.microphonePermissionGranted)
        assertEquals(listOf(initialPreset), viewModel.uiState.value.presets)
    }

    @Test
    fun onOpenKeyboardSettings_invokesCallback() = runTest {
        val settingsStore = SettingsStore(createStore(backgroundScope))
        var opened = 0
        val viewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasMicrophonePermission = { true },
            openKeyboardSettings = { opened += 1 },
            showInputMethodPicker = {},
            requestMicrophonePermission = {},
            scope = this,
        )

        viewModel.onOpenKeyboardSettings()

        assertEquals(1, opened)
    }

    @Test
    fun onShowInputMethodPicker_invokesCallback() = runTest {
        val settingsStore = SettingsStore(createStore(backgroundScope))
        var shown = 0
        val viewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasMicrophonePermission = { true },
            openKeyboardSettings = {},
            showInputMethodPicker = { shown += 1 },
            requestMicrophonePermission = {},
            scope = this,
        )

        viewModel.onShowInputMethodPicker()

        assertEquals(1, shown)
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
                presets = listOf(initialPreset),
            ),
        )

        val viewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasMicrophonePermission = { true },
            openKeyboardSettings = {},
            showInputMethodPicker = {},
            requestMicrophonePermission = {},
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
            hasMicrophonePermission = { true },
            openKeyboardSettings = {},
            showInputMethodPicker = {},
            requestMicrophonePermission = {},
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
            hasMicrophonePermission = { true },
            openKeyboardSettings = {},
            showInputMethodPicker = {},
            requestMicrophonePermission = {},
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

    @Test
    fun reloadSettings_reportsMicrophonePermissionState() = runTest {
        val settingsStore = SettingsStore(createStore(backgroundScope))
        val viewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasMicrophonePermission = { false },
            openKeyboardSettings = {},
            showInputMethodPicker = {},
            requestMicrophonePermission = {},
            scope = this,
        )

        viewModel.reloadSettings()

        assertFalse(viewModel.uiState.value.microphonePermissionGranted)
    }

    @Test
    fun onRequestMicrophonePermission_invokesPermissionCallback() = runTest {
        val settingsStore = SettingsStore(createStore(backgroundScope))
        var requested = 0
        val viewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasMicrophonePermission = { false },
            openKeyboardSettings = {},
            showInputMethodPicker = {},
            requestMicrophonePermission = { requested += 1 },
            scope = this,
        )

        viewModel.onRequestMicrophonePermission()

        assertEquals(1, requested)
    }

    private fun createStore(scope: kotlinx.coroutines.CoroutineScope): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { temporaryFolder.newFile("settings-viewmodel-${System.nanoTime()}.preferences_pb") },
        )
}
