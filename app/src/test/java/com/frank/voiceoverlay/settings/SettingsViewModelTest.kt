package com.frank.voiceoverlay.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.frank.voiceoverlay.shortcuts.ShortcutPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
    fun createPreset_appendsNewMacroAndPersistsIt() = runTest {
        val settingsStore = SettingsStore(createStore(backgroundScope))
        val initialPresets = ShortcutPreset.defaultPresets().take(2)
        settingsStore.save(AppSettings(presets = initialPresets))
        val viewModelScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val viewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasMicrophonePermission = { true },
            openKeyboardSettings = {},
            showInputMethodPicker = {},
            requestMicrophonePermission = {},
            scope = viewModelScope,
        )

        advanceUntilIdle()
        viewModel.createPreset()
        runCurrent()
        advanceUntilIdle()

        val presets = settingsStore.readOnce().presets
        viewModel.reloadSettings()
        advanceUntilIdle()

        assertEquals(presets, viewModel.uiState.value.presets)
        assertEquals(3, presets.size)
        assertEquals(initialPresets, presets.take(2))
        val createdPreset = presets.last()
        assertTrue(createdPreset.id.startsWith("macro-"))
        assertEquals("New macro", createdPreset.label)
        assertEquals("Describe what this macro should insert.", createdPreset.text)
        assertFalse(createdPreset.isPinned)
        assertEquals(2, createdPreset.order)
        assertEquals(presets, settingsStore.readOnce().presets)
    }

    @Test
    fun deletePreset_removesMacroAndPersistsDeletion() = runTest {
        val settingsStore = SettingsStore(createStore(backgroundScope))
        val initialPresets = listOf(
            ShortcutPreset(id = "alpha", label = "Alpha", text = "First", order = 0, isPinned = true),
            ShortcutPreset(id = "beta", label = "Beta", text = "Second", order = 2, isPinned = false),
            ShortcutPreset(id = "gamma", label = "Gamma", text = "Third", order = 5, isPinned = true),
        )
        settingsStore.save(AppSettings(presets = initialPresets))
        val viewModelScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val viewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasMicrophonePermission = { true },
            openKeyboardSettings = {},
            showInputMethodPicker = {},
            requestMicrophonePermission = {},
            scope = viewModelScope,
        )

        advanceUntilIdle()
        viewModel.deletePreset("beta")
        runCurrent()
        advanceUntilIdle()

        val presets = settingsStore.readOnce().presets
        viewModel.reloadSettings()
        advanceUntilIdle()

        assertEquals(presets, viewModel.uiState.value.presets)
        assertEquals(listOf("alpha", "gamma"), presets.map { it.id })
        assertEquals(listOf(0, 1), presets.map { it.order })
        assertEquals(listOf(true, true), presets.map { it.isPinned })
        assertEquals(presets, settingsStore.readOnce().presets)
    }

    @Test
    fun movePresetUp_reordersMacrosAndNormalizesOrder() = runTest {
        val settingsStore = SettingsStore(createStore(backgroundScope))
        val initialPresets = listOf(
            ShortcutPreset(id = "third", label = "Third", text = "3", order = 2, isPinned = false),
            ShortcutPreset(id = "first", label = "First", text = "1", order = 0, isPinned = true),
            ShortcutPreset(id = "second", label = "Second", text = "2", order = 1, isPinned = false),
        )
        settingsStore.save(AppSettings(presets = initialPresets))
        val viewModelScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val viewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasMicrophonePermission = { true },
            openKeyboardSettings = {},
            showInputMethodPicker = {},
            requestMicrophonePermission = {},
            scope = viewModelScope,
        )

        advanceUntilIdle()
        viewModel.movePresetUp("third")
        runCurrent()
        advanceUntilIdle()

        val presets = settingsStore.readOnce().presets
        viewModel.reloadSettings()
        advanceUntilIdle()

        assertEquals(presets, viewModel.uiState.value.presets)
        assertEquals(listOf("first", "third", "second"), presets.map { it.id })
        assertEquals(listOf(0, 1, 2), presets.map { it.order })
        assertEquals(listOf(true, false, false), presets.map { it.isPinned })
        assertEquals(presets, settingsStore.readOnce().presets)
    }

    @Test
    fun togglePresetPinned_flipsPinStateAndPersistsIt() = runTest {
        val settingsStore = SettingsStore(createStore(backgroundScope))
        val initialPresets = listOf(
            ShortcutPreset(id = "alpha", label = "Alpha", text = "First", order = 0, isPinned = false),
            ShortcutPreset(id = "beta", label = "Beta", text = "Second", order = 1, isPinned = true),
        )
        settingsStore.save(AppSettings(presets = initialPresets))
        val viewModelScope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val viewModel = SettingsViewModel(
            settingsStore = settingsStore,
            hasMicrophonePermission = { true },
            openKeyboardSettings = {},
            showInputMethodPicker = {},
            requestMicrophonePermission = {},
            scope = viewModelScope,
        )

        advanceUntilIdle()
        viewModel.togglePresetPinned("alpha")
        runCurrent()
        advanceUntilIdle()

        val presets = settingsStore.readOnce().presets
        viewModel.reloadSettings()
        advanceUntilIdle()

        assertEquals(presets, viewModel.uiState.value.presets)
        assertEquals(listOf(true, true), presets.map { it.isPinned })
        assertEquals(listOf("alpha", "beta"), presets.map { it.id })
        assertEquals(listOf(0, 1), presets.map { it.order })
        assertEquals(presets, settingsStore.readOnce().presets)
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
