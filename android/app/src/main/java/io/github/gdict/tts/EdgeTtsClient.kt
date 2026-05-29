package io.github.gdict.tts

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object EdgeTtsClient {

    private const val TAG = "EdgeTtsClient"
    private const val WSS_URL =
        "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
        "?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4" +
        "&ConnectionId="

    private const val OUTPUT_FORMAT = "audio-24khz-48kbitrate-mono-mp3"
    private val VOICES = arrayOf(
        "en-US-AriaNeural",
        "en-US-JennyNeural",
        "en-US-GuyNeural"
    )

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    fun synthesize(text: String): ByteArray? {
        if (text.isBlank()) return null

        val voice = VOICES[0]
        val requestId = UUID.randomUUID().toString().replace("-", "")
        val connectionId = UUID.randomUUID().toString()

        val latch = CountDownLatch(1)
        val audioBuffer = ByteArrayOutputStream()
        var error: Throwable? = null

        val request = Request.Builder()
            .url("$WSS_URL$connectionId")
            .header("Origin", "https://azure.microsoft.com")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val configJson = """
                    {"context":{"synthesis":{"audio":{"metadataoptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"true"},"outputFormat":"$OUTPUT_FORMAT"}}}}
                """.trimIndent()

                webSocket.send("X-Timestamp:" + java.time.Instant.now().toString() + "\r\n" +
                    "Content-Type:application/json; charset=utf-8\r\n" +
                    "Path:speech.config\r\n\r\n$configJson")

                val ssml = buildSsml(text, voice)
                webSocket.send("X-RequestId:$requestId\r\n" +
                    "Content-Type:application/ssml+xml\r\n" +
                    "X-Timestamp:" + java.time.Instant.now().toString() + "Z\r\n" +
                    "Path:ssml\r\n\r\n$ssml")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("Path:turn.end")) {
                    latch.countDown()
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                val data = bytes.toByteArray()
                if (data.size > 2) {
                    val headerLen = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
                    if (headerLen in 0..data.size) {
                        audioBuffer.write(data, headerLen, data.size - headerLen)
                    } else {
                        audioBuffer.write(data)
                    }
                } else {
                    audioBuffer.write(data)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure: ${t.message}")
                error = t
                latch.countDown()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                latch.countDown()
            }
        })

        try {
            val completed = latch.await(15, TimeUnit.SECONDS)
            if (!completed) {
                Log.w(TAG, "TTS timeout for: $text")
                webSocket.close(1000, "timeout")
                return null
            }
        } catch (e: InterruptedException) {
            Log.w(TAG, "TTS interrupted: ${e.message}")
            webSocket.close(1000, "interrupted")
            return null
        }

        val result = audioBuffer.toByteArray()
        webSocket.close(1000, "done")

        if (error != null) {
            Log.w(TAG, "TTS failed: ${error!!.message}")
            return null
        }

        if (result.isEmpty()) {
            Log.w(TAG, "TTS returned empty audio for: $text")
            return null
        }

        return result
    }

    private fun buildSsml(text: String, voiceName: String): String {
        val escaped = text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
        return (
            "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
            "<voice name='$voiceName'>" +
            "<prosody rate='+0.00%' pitch='+0Hz'>" +
            escaped +
            "</prosody>" +
            "</voice>" +
            "</speak>"
        )
    }
}
