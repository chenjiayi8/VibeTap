package com.frank.voiceoverlay.ime

import android.view.KeyEvent
import android.view.inputmethod.InputConnection

class InputConnectionCommitter(
    private val currentInputConnection: () -> InputConnection?,
) : TextCommitter {
    override fun commitText(text: String): Boolean =
        currentInputConnection()?.commitText(text, 1) == true

    override fun backspace(): Boolean =
        currentInputConnection()?.deleteSurroundingText(1, 0) == true

    override fun sendEnter(): Boolean {
        val inputConnection = currentInputConnection() ?: return false
        val didSendDown = inputConnection.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER),
        )
        val didSendUp = inputConnection.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER),
        )
        return didSendDown && didSendUp
    }
}
