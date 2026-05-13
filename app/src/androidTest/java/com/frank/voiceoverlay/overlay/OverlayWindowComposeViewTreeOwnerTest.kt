package com.frank.voiceoverlay.overlay

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frank.voiceoverlay.testing.TestComposeActivity
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlayWindowComposeViewTreeOwnerTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(TestComposeActivity::class.java)

    @Test
    fun installedOwnersAllowComposeViewToAttachToSeparateWindow() {
        activityRule.scenario.onActivity { activity ->
            val windowManager = activity.getSystemService(WindowManager::class.java)
            val composeView = ComposeView(activity).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    MaterialTheme {
                        Text("Overlay")
                    }
                }
            }
            val owner = OverlayWindowComposeViewTreeOwner()
            owner.attachTo(composeView)

            try {
                windowManager.addView(composeView, overlayLayoutParams(activity))

                assertNotNull(OverlayViewTreeOwnerBridge.lifecycleOwner(composeView))
                assertNotNull(OverlayViewTreeOwnerBridge.savedStateRegistryOwner(composeView))
                assertNotNull(OverlayViewTreeOwnerBridge.viewModelStoreOwner(composeView))
            } finally {
                if (composeView.parent != null) {
                    windowManager.removeViewImmediate(composeView)
                }
                owner.dispose()
            }
        }
    }

    private fun overlayLayoutParams(activity: TestComposeActivity) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        token = activity.window.decorView.applicationWindowToken
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 0
    }
}
