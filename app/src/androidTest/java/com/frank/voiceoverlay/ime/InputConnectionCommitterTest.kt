package com.frank.voiceoverlay.ime

import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputConnectionCommitterTest {
    @Test
    fun commitTextDelegatesToCurrentInputConnection() {
        val inputConnection = RecordingInputConnection()
        val committer = InputConnectionCommitter { inputConnection }

        val didCommit = committer.commitText("hello")

        assertTrue(didCommit)
        assertEquals(listOf("hello"), inputConnection.committedTexts)
    }

    @Test
    fun backspaceDeletesOneCharacterBeforeCursor() {
        val inputConnection = RecordingInputConnection()
        val committer = InputConnectionCommitter { inputConnection }

        val didDelete = committer.backspace()

        assertTrue(didDelete)
        assertEquals(listOf(1 to 0), inputConnection.deletedRanges)
    }

    @Test
    fun sendEnterSendsDownAndUpEvents() {
        val inputConnection = RecordingInputConnection()
        val committer = InputConnectionCommitter { inputConnection }

        val didSendEnter = committer.sendEnter()

        assertTrue(didSendEnter)
        assertEquals(
            listOf(
                KeyEvent.ACTION_DOWN to KeyEvent.KEYCODE_ENTER,
                KeyEvent.ACTION_UP to KeyEvent.KEYCODE_ENTER,
            ),
            inputConnection.sentKeyEvents,
        )
    }

    @Test
    fun actionsReturnFalseWhenThereIsNoActiveInputConnection() {
        val committer = InputConnectionCommitter { null }

        assertFalse(committer.commitText("hello"))
        assertFalse(committer.backspace())
        assertFalse(committer.sendEnter())
    }

    private class RecordingInputConnection : BaseInputConnection(
        View(InstrumentationRegistry.getInstrumentation().targetContext),
        true,
    ) {
        val committedTexts = mutableListOf<String>()
        val deletedRanges = mutableListOf<Pair<Int, Int>>()
        val sentKeyEvents = mutableListOf<Pair<Int, Int>>()

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            committedTexts += text.toString()
            return true
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            deletedRanges += beforeLength to afterLength
            return true
        }

        override fun sendKeyEvent(event: KeyEvent): Boolean {
            sentKeyEvents += event.action to event.keyCode
            return true
        }
    }
}
