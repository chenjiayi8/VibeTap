package com.frank.voiceoverlay.insertion

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class OverlayAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Placeholder: actual insertion flow will be implemented in a later task.
    }

    override fun onInterrupt() {
        // Placeholder: required by AccessibilityService.
    }

    fun canInsertIntoFocusedField(): Boolean {
        val focusedNode = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        return focusedNode?.isEditable == true
    }
}
