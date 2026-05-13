package com.frank.voiceoverlay.insertion

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

object FocusedFieldInserter {
    fun mergeText(
        existing: String,
        insertion: String,
        selectionStart: Int,
        selectionEnd: Int,
    ): String {
        val textLength = existing.length
        val start = minOf(selectionStart.coerceIn(0, textLength), selectionEnd.coerceIn(0, textLength))
        val end = maxOf(selectionStart.coerceIn(0, textLength), selectionEnd.coerceIn(0, textLength))

        return buildString(existing.length + insertion.length) {
            append(existing.substring(0, start))
            append(insertion)
            append(existing.substring(end))
        }
    }

    fun replaceText(node: AccessibilityNodeInfo, mergedText: String): Boolean {
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, mergedText)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }
}
