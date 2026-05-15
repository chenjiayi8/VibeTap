package com.frank.voiceoverlay

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frank.voiceoverlay.settings.SettingsScreenTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchShowsPrimarySettingsSections() {
        composeRule.onNodeWithTag(SettingsScreenTestTags.KeyboardSetupSection).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.ApiKeySection).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsScreenTestTags.ShortcutPresetsSection).assertIsDisplayed()
    }
}
