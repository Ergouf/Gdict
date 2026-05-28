package io.github.gdict.tts

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object EdgeTtsClient {

    private const val EDGE_TTS_URL =
        "https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"

    private const val VOICE = "en-US-AriaNeural"
    private const val OUTPUT_FORMAT = "audio-24khz-48kbitrate-mono-mp3"

    fun synthesize(text: String): ByteArray? {
        val ssml = buildSsml(text, VOICE)
        val connection = URL(EDGE_TTS_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 8000
            connection.readTimeout = 12000
            connection.setRequestProperty("Content-Type", "application/ssml+xml")
            connection.setRequestProperty("X-Microsoft-OutputFormat", OUTPUT_FORMAT)
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; Gdict/1.0)")

            connection.outputStream.use { os ->
                os.write(ssml.toByteArray(Charsets.UTF_8))
            }

            if (connection.responseCode != 200) return null

            val buffer = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                input.copyTo(buffer)
            }
            return buffer.toByteArray()
        } catch (e: Exception) {
            return null
        } finally {
            connection.disconnect()
        }
    }

    private fun buildSsml(text: String, voiceName: String): String {
        val escaped = text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
        val rate = "+0.00%"
        val pitch = "+0Hz"
        return (
            "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
            "<voice name='$voiceName'>" +
            "<prosody rate='$rate' pitch='$pitch'>" +
            escaped +
            "</prosody>" +
            "</voice>" +
            "</speak>"
        )
    }
}
