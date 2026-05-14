package com.frank.voiceoverlay.ime

interface TextCommitter {
    fun commitText(text: String)

    fun backspace()

    fun sendEnter()
}
