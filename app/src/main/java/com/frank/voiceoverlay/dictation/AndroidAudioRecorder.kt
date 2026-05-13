package com.frank.voiceoverlay.dictation

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AndroidAudioRecorder(
    private val context: Context,
) : AudioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    override suspend fun start() {
        check(mediaRecorder == null) { "Recording already in progress" }

        val file = kotlin.io.path.createTempFile(
            directory = context.cacheDir.toPath(),
            suffix = ".m4a",
        ).toFile()

        var recorder: MediaRecorder? = null
        try {
            recorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (error: Throwable) {
            recorder?.let {
                runCatching { it.reset() }
                runCatching { it.release() }
            }
            file.delete()
            throw error
        }

        outputFile = file
        mediaRecorder = recorder
    }

    override suspend fun stop(): File {
        val recorder = checkNotNull(mediaRecorder) { "Recording has not started" }
        val file = checkNotNull(outputFile) { "Recording output missing" }

        try {
            recorder.stop()
        } finally {
            recorder.reset()
            recorder.release()
            mediaRecorder = null
            outputFile = null
        }

        return file
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }
}
