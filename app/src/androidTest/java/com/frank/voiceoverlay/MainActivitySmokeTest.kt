package com.frank.voiceoverlay

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
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
    fun appLaunchExportsApiKeyFieldTagToAccessibilitySurface() {
        composeRule.waitForIdle()

        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = SystemClock.uptimeMillis() + 5_000
        var node: AccessibilityNodeInfo? = null
        while (SystemClock.uptimeMillis() < deadline && node == null) {
            automation.waitForIdle(500, 2_000)
            node = findNodeByViewIdResourceName(
                root = automation.rootInActiveWindow,
                viewIdResourceName = SettingsScreenTestTags.ApiKeyField,
            )
            if (node == null) {
                SystemClock.sleep(100)
            }
        }

        assertNotNull(node)
        assertTrue(node!!.isVisibleToUser)
    }

    private fun findNodeByViewIdResourceName(
        root: AccessibilityNodeInfo?,
        viewIdResourceName: String,
    ): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.viewIdResourceName == viewIdResourceName) return root
        for (index in 0 until root.childCount) {
            val match = findNodeByViewIdResourceName(root.getChild(index), viewIdResourceName)
            if (match != null) return match
        }
        return null
    }
}
