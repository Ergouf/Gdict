package io.github.gdict.ui.webview

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object AudioPlayer {

    private const val TAG = "AudioPlayer"

    fun play(context: Context, audioData: ByteArray): Boolean {
        val tempFile = File(context.cacheDir, "dict_audio_${System.currentTimeMillis()}.mp3")
        var mediaPlayer: android.media.MediaPlayer? = null
        var started = false
        return try {
            FileOutputStream(tempFile).use { it.write(audioData) }
            mediaPlayer = android.media.MediaPlayer()
            mediaPlayer.setDataSource(tempFile.absolutePath)
            mediaPlayer.setOnCompletionListener {
                it.release()
                tempFile.delete()
            }
            mediaPlayer.setOnErrorListener { mp, what, extra ->
                Log.w(TAG, "MediaPlayer error: what=$what extra=$extra")
                mp.release()
                tempFile.delete()
                false
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
            started = true
            true
        } catch (e: Exception) {
            Log.w(TAG, "Playback failed: ${e.message}")
            mediaPlayer?.release()
            false
        } finally {
            if (!started) {
                tempFile.delete()
            }
        }
    }
}
