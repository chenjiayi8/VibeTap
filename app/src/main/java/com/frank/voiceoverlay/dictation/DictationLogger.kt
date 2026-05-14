package com.frank.voiceoverlay.dictation

import android.util.Log

interface DictationLogger {
    fun debug(event: String)

    fun error(event: String, throwable: Throwable)
}

class AndroidDictationLogger(
    private val tag: String,
) : DictationLogger {
    override fun debug(event: String) {
        Log.d(tag, event)
    }

    override fun error(event: String, throwable: Throwable) {
        Log.e(tag, event, throwable)
    }
}

object NoOpDictationLogger : DictationLogger {
    override fun debug(event: String) = Unit

    override fun error(event: String, throwable: Throwable) = Unit
}
