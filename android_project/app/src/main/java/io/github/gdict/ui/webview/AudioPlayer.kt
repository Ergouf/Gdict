package io.github.gdict.ui.webview

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object AudioPlayer {
    fun play(context: Context, audioData: ByteArray) {
        val tempFile = File(context.cacheDir, "dict_audio_${System.currentTimeMillis()}.mp3")
        var mediaPlayer: android.media.MediaPlayer? = null
        var started = false
        try {
            FileOutputStream(tempFile).use { it.write(audioData) }
            mediaPlayer = android.media.MediaPlayer()
            mediaPlayer.setDataSource(tempFile.absolutePath)
            mediaPlayer.setOnCompletionListener {
                it.release()
                tempFile.delete()
            }
            mediaPlayer.setOnErrorListener { mp, _, _ ->
                mp.release()
                tempFile.delete()
                false
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
            started = true
        } catch (e: Exception) {
            mediaPlayer?.release()
            throw e
        } finally {
            if (!started) {
                tempFile.delete()
            }
        }
    }
}
