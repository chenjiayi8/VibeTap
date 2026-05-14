package com.frank.voiceoverlay.ime

interface TextCommitter {
    fun commitText(text: String): Boolean

    fun backspace(): Boolean

    fun sendEnter(): Boolean
}
