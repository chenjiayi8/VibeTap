package com.frank.voiceoverlay.dictation

import java.io.File

interface AudioRecorder {
    suspend fun start()
    suspend fun stop(): File
}
