package com.frank.voiceoverlay

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.frank.voiceoverlay.settings.SettingsScreenTestTags
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchShowsLiveProofAutomationAnchors() {
        composeRule.onNodeWithTag(SettingsScreenTestTags.KeyboardSetupSection).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.ApiKeySection).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.ApiKeyField).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.ShortcutPresetsSection).assertIsDisplayed()
    }

    @Test
    fun appLaunchExportsApiKeyFieldTagToUiAutomator() {
        composeRule.waitForIdle()

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertTrue(device.wait(Until.hasObject(By.res(SettingsScreenTestTags.ApiKeyField)), 5_000))
        assertNotNull(device.findObject(By.res(SettingsScreenTestTags.ApiKeyField)))
    }
}
