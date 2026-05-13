package com.frank.voiceoverlay.insertion

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class OverlayAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: the service reacts only when insertion is explicitly requested.
    }

    override fun onInterrupt() {
        // No-op: required by AccessibilityService.
    }

    fun canInsertIntoFocusedField(): Boolean = withFocusedEditableField { true } ?: false

    fun insert(text: String): Boolean = withFocusedEditableField { field ->
        field.insert(text)
    } ?: false

    @Suppress("DEPRECATION")
    private inline fun <T> withFocusedEditableField(block: (EditableField) -> T): T? {
        val rootNode = rootInActiveWindow ?: return null
        val focusedNode = try {
            rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        } finally {
            rootNode.recycle()
        } ?: return null

        return EditableField.fromNode(focusedNode)?.use(block)
    }

    class EditableField(
        private val existingText: String,
        private val selectionStart: Int,
        private val selectionEnd: Int,
        private val replaceText: (String) -> Boolean,
        private val onRecycle: () -> Unit = {},
    ) : AutoCloseable {
        fun insert(insertion: String): Boolean {
            val mergedText = FocusedFieldInserter.mergeText(
                existing = existingText,
                insertion = insertion,
                selectionStart = resolvedSelectionStart(),
                selectionEnd = resolvedSelectionEnd(),
            )
            return replaceText(mergedText)
        }

        override fun close() {
            onRecycle()
        }

        private fun resolvedSelectionStart(): Int =
            selectionStart.takeIf { it >= 0 } ?: existingText.length

        private fun resolvedSelectionEnd(): Int =
            selectionEnd.takeIf { it >= 0 } ?: existingText.length

        companion object {
            @Suppress("DEPRECATION")
            fun fromNode(node: AccessibilityNodeInfo): EditableField? {
                if (!node.isEditable) {
                    node.recycle()
                    return null
                }

                return EditableField(
                    existingText = node.text?.toString().orEmpty(),
                    selectionStart = node.textSelectionStart,
                    selectionEnd = node.textSelectionEnd,
                    replaceText = { mergedText -> FocusedFieldInserter.replaceText(node, mergedText) },
                    onRecycle = node::recycle,
                )
            }
        }
    }
}
