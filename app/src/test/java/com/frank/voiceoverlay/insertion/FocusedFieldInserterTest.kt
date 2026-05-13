package com.frank.voiceoverlay.insertion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusedFieldInserterTest {
    @Test
    fun mergeText_appendsAtCursorWhenNoSelection() {
        val result = FocusedFieldInserter.mergeText(
            existing = "Hello",
            insertion = " world",
            selectionStart = 5,
            selectionEnd = 5,
        )
        assertEquals("Hello world", result)
    }

    @Test
    fun mergeText_replacesSelectedRangeWhenSelectionExists() {
        val result = FocusedFieldInserter.mergeText(
            existing = "Hello there",
            insertion = " world",
            selectionStart = 5,
            selectionEnd = 11,
        )
        assertEquals("Hello world", result)
    }

    @Test
    fun mergeText_handlesReversedIndices() {
        val result = FocusedFieldInserter.mergeText(
            existing = "abcdef",
            insertion = "Z",
            selectionStart = 4,
            selectionEnd = 2,
        )
        assertEquals("abZef", result)
    }

    @Test
    fun mergeText_clampsIndicesIntoExistingBounds() {
        val result = FocusedFieldInserter.mergeText(
            existing = "abcdef",
            insertion = "Z",
            selectionStart = -3,
            selectionEnd = 99,
        )
        assertEquals("Z", result)
    }

    @Test
    fun mergeText_keepsTextUnchangedWhenInsertionIsEmpty() {
        val result = FocusedFieldInserter.mergeText(
            existing = "abcdef",
            insertion = "",
            selectionStart = 3,
            selectionEnd = 3,
        )
        assertEquals("abcdef", result)
    }

    @Test
    fun editableFieldInsertion_appendsWhenSelectionIsUnknown() {
        var replacedText: String? = null
        val field = OverlayAccessibilityService.EditableField(
            existingText = "Hello",
            selectionStart = -1,
            selectionEnd = -1,
            replaceText = {
                replacedText = it
                true
            },
        )

        val result = field.insert(" world")

        assertTrue(result)
        assertEquals("Hello world", replacedText)
    }

    @Test
    fun editableFieldInsertion_returnsFalseWhenReplaceActionFails() {
        var recycleCalls = 0
        val field = OverlayAccessibilityService.EditableField(
            existingText = "Hello",
            selectionStart = 5,
            selectionEnd = 5,
            replaceText = { false },
            onRecycle = { recycleCalls++ },
        )

        field.use {
            assertFalse(it.insert(" world"))
        }

        assertEquals(1, recycleCalls)
    }
}
