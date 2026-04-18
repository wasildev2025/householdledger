package com.example.householdledger.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

/**
 * Thin wrapper around Android's MediaRecorder for M4A/AAC voice notes.
 * Files are written to the app cache directory.
 */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    fun start(): File? {
        stop()
        return try {
            val outFile = File(context.cacheDir, "voice_${UUID.randomUUID()}.m4a")
            file = outFile

            @Suppress("DEPRECATION")
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
            else MediaRecorder()
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(44_100)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recorder = mr
            outFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun stop(): File? {
        return try {
            recorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
            recorder = null
            val out = file
            file = null
            out
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isRecording(): Boolean = recorder != null
}

/**
 * Tiny wrapper around MediaPlayer for streaming voice messages from a URL.
 */
class VoicePlayer {
    private var player: MediaPlayer? = null

    fun play(url: String, onComplete: () -> Unit = {}) {
        stop()
        try {
            player = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { start() }
                setOnCompletionListener {
                    onComplete()
                    stop()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete()
        }
    }

    fun stop() {
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
    }

    fun isPlaying(): Boolean = player?.isPlaying == true
}
