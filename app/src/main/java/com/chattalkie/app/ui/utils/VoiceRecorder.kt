package com.chattalkie.app.ui.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class VoiceRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    var isRecording = false
        private set

    fun startRecording(): Boolean {
        if (isRecording) return false

        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val file = File(cacheDir, "voice_msg_${System.currentTimeMillis()}.m4a")
        outputFile = file

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
            // If stop fails (e.g. very short recording), delete file
            outputFile?.delete()
            outputFile = null
        } finally {
            mediaRecorder = null
            isRecording = false
        }
        return outputFile
    }

    fun cleanup() {
        if (isRecording) {
            stopRecording()
        }
    }
}
