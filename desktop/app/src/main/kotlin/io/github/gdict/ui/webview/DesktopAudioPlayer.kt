package io.github.gdict.ui.webview

import io.github.gdict.core.GdictLogger
import javazoom.jl.player.Player
import java.io.File
import java.io.FileOutputStream
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine

object DesktopAudioPlayer {
    private var currentClip: Clip? = null
    private var currentMp3Player: Player? = null
    private var currentMp3Thread: Thread? = null
    private val log = GdictLogger.get()

    fun play(audioData: ByteArray) {
        try {
            log.i("DesktopAudioPlayer", "play: ${audioData.size} bytes")

            stop()

            val isMp3 = isMp3Data(audioData)

            if (isMp3) {
                playMp3(audioData)
                return
            }

            val tempFile = File(System.getProperty("java.io.tmpdir"), "gdict_audio_${System.currentTimeMillis()}.tmp")
            FileOutputStream(tempFile).use { it.write(audioData) }

            try {
                val audioInputStream = AudioSystem.getAudioInputStream(tempFile)
                val format = audioInputStream.format
                val info = DataLine.Info(Clip::class.java, format)

                if (AudioSystem.isLineSupported(info)) {
                    val clip = AudioSystem.getClip() as Clip
                    clip.open(audioInputStream)
                    currentClip = clip

                    clip.addLineListener { event ->
                        if (event.type == javax.sound.sampled.LineEvent.Type.STOP) {
                            clip.close()
                            currentClip = null
                            tempFile.delete()
                        }
                    }

                    clip.start()
                    log.i("DesktopAudioPlayer", "play: Clip started")
                } else {
                    log.w("DesktopAudioPlayer", "play: audio format not supported, trying conversion")
                    playWithConversion(audioInputStream, tempFile)
                }
            } catch (e: Throwable) {
                log.w("DesktopAudioPlayer", "play: AudioSystem failed (${e.javaClass.simpleName}), trying JLayer")
                playMp3WithJLayer(tempFile)
            }
        } catch (e: Throwable) {
            log.e("DesktopAudioPlayer", "play: failed: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun isMp3Data(data: ByteArray): Boolean {
        if (data.size < 3) return false
        if (data[0] == 0xFF.toByte() && (data[1].toInt() and 0xE0) == 0xE0) return true
        if (data[0] == 'I'.code.toByte() && data[1] == 'D'.code.toByte() && data[2] == '3'.code.toByte()) return true
        return false
    }

    private fun playMp3(data: ByteArray) {
        val tempFile = File(System.getProperty("java.io.tmpdir"), "gdict_audio_${System.currentTimeMillis()}.mp3")
        FileOutputStream(tempFile).use { it.write(data) }
        tempFile.deleteOnExit()
        playMp3WithJLayer(tempFile)
    }

    private fun playMp3WithJLayer(tempFile: File) {
        try {
            val player = Player(tempFile.inputStream())
            currentMp3Player = player

            val thread = Thread({
                try {
                    player.play()
                } catch (e: Throwable) {
                    log.w("DesktopAudioPlayer", "JLayer play interrupted: ${e.message}")
                } finally {
                    currentMp3Player = null
                    try { tempFile.delete() } catch (_: Throwable) {}
                }
            }, "gdict-mp3-player")
            currentMp3Thread = thread
            thread.isDaemon = true
            thread.start()
            log.i("DesktopAudioPlayer", "playMp3WithJLayer: started")
        } catch (e: Throwable) {
            log.e("DesktopAudioPlayer", "playMp3WithJLayer failed: ${e.javaClass.simpleName}: ${e.message}")
            tempFile.delete()
        }
    }

    private fun playWithConversion(audioInputStream: AudioInputStream, tempFile: File) {
        try {
            val baseFormat = audioInputStream.format
            val decodedFormat = javax.sound.sampled.AudioFormat(
                javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.sampleRate,
                16,
                baseFormat.channels,
                baseFormat.channels * 2,
                baseFormat.sampleRate,
                false
            )
            val decodedStream = AudioSystem.getAudioInputStream(decodedFormat, audioInputStream)
            val clip = AudioSystem.getClip()
            clip.open(decodedStream)
            currentClip = clip

            clip.addLineListener { event ->
                if (event.type == javax.sound.sampled.LineEvent.Type.STOP) {
                    clip.close()
                    currentClip = null
                    tempFile.delete()
                }
            }

            clip.start()
            log.i("DesktopAudioPlayer", "playWithConversion: Clip started")
        } catch (e: Throwable) {
            log.e("DesktopAudioPlayer", "playWithConversion failed: ${e.javaClass.simpleName}: ${e.message}")
            tempFile.delete()
        }
    }

    fun stop() {
        currentMp3Player?.let { player ->
            try { player.close() } catch (_: Throwable) {}
            currentMp3Player = null
        }
        currentMp3Thread?.let { thread ->
            try { thread.interrupt() } catch (_: Throwable) {}
            currentMp3Thread = null
        }
        currentClip?.let { clip ->
            try {
                if (clip.isRunning) clip.stop()
                clip.close()
            } catch (e: Throwable) {
                log.w("DesktopAudioPlayer", "stop: error: ${e.javaClass.simpleName}: ${e.message}")
            }
            currentClip = null
        }
    }
}
