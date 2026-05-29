package io.github.gdict.tts

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object EdgeTtsClient {

    private const val TAG = "EdgeTtsClient"
    private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    private const val SEC_MS_GEC_VERSION = "1-143.0.3650.75"
    private const val WIN_EPOCH = 11644473600L

    private val WSS_URLS = listOf(
        "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1",
        "wss://eastus.tts.speech.microsoft.com/cognitiveservices/websocket/v1"
    )

    private const val OUTPUT_FORMAT = "audio-24khz-48kbitrate-mono-mp3"
    private val VOICES = arrayOf(
        "en-US-AriaNeural",
        "en-US-JennyNeural",
        "en-US-GuyNeural"
    )

    private const val MAX_RETRIES = 2

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(5, TimeUnit.SECONDS)
            .build()
    }

    private val lastWorkingUrlIndex = AtomicInteger(0)

    private fun generateSecMsGec(): String {
        var ticks = System.currentTimeMillis() / 1000
        ticks += WIN_EPOCH
        ticks -= ticks % 300
        val ticks100ns = ticks * 10000000L
        val hashInput = "$ticks100ns$TRUSTED_CLIENT_TOKEN"
        return runCatching {
            val md = MessageDigest.getInstance("SHA-256")
            val hashBytes = md.digest(hashInput.toByteArray(Charsets.US_ASCII))
            hashBytes.joinToString("") { "%02X".format(it) }
        }.onFailure {
            Log.w(TAG, "Failed to generate Sec-MS-GEC: ${it.message}")
        }.getOrDefault("")
    }

    private fun buildWssUrl(baseUrl: String, connectionId: String): String {
        val secMsGec = generateSecMsGec()
        return "$baseUrl?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
            "&Sec-MS-GEC=$secMsGec" +
            "&Sec-MS-GEC-Version=$SEC_MS_GEC_VERSION" +
            "&ConnectionId=$connectionId"
    }

    fun synthesize(text: String): ByteArray? {
        if (text.isBlank()) return null

        val snapshotIndex = lastWorkingUrlIndex.get()
        val orderedUrls = buildList {
            add(WSS_URLS[snapshotIndex])
            for (i in WSS_URLS.indices) {
                if (i != snapshotIndex) add(WSS_URLS[i])
            }
        }

        for (urlIndex in orderedUrls.indices) {
            val wssUrl = orderedUrls[urlIndex]
            for (attempt in 0..MAX_RETRIES) {
                val result = trySynthesize(text, wssUrl)
                if (result != null) {
                    lastWorkingUrlIndex.set(WSS_URLS.indexOf(wssUrl).coerceAtLeast(0))
                    return result
                }
                if (attempt < MAX_RETRIES) {
                    Log.i(TAG, "Retry ${attempt + 1}/$MAX_RETRIES for: $text")
                    Thread.sleep(500L * (attempt + 1))
                }
            }
            Log.w(TAG, "All retries failed for URL index $urlIndex, trying next URL")
        }

        Log.w(TAG, "All EdgeTTS attempts failed for: $text")
        return null
    }

    private fun trySynthesize(text: String, wssBaseUrl: String): ByteArray? {
        val voice = VOICES[0]
        val requestId = UUID.randomUUID().toString().replace("-", "")
        val connectionId = UUID.randomUUID().toString()
        val wssUrl = buildWssUrl(wssBaseUrl, connectionId)

        val latch = CountDownLatch(1)
        val audioBuffer = ByteArrayOutputStream()
        var error: Throwable? = null
        var receivedAudio = false

        val request = Request.Builder()
            .url(wssUrl)
            .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0")
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Accept", "*/*")
            .header("Sec-CH-UA", "\"Chromium\";v=\"143\", \"Not:A-Brand\";v=\"99\"")
            .header("Sec-CH-UA-Mobile", "?0")
            .header("Sec-CH-UA-Platform", "\"Windows\"")
            .build()

        val webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened, response code: ${response.code}")
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
                } else if (text.contains("Path:response") && text.contains("StatusCode")) {
                    val statusCodeRegex = Regex("""StatusCode["\s:]+(\d+)""")
                    val match = statusCodeRegex.find(text)
                    if (match != null) {
                        val statusCode = match.groupValues[1].toIntOrNull()
                        if (statusCode != null && statusCode != 200) {
                            Log.w(TAG, "TTS response error, StatusCode: $statusCode")
                            error = RuntimeException("TTS server returned StatusCode: $statusCode")
                            latch.countDown()
                        }
                    }
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
                receivedAudio = true
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure: ${t.javaClass.simpleName}: ${t.message}")
                error = t
                latch.countDown()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                latch.countDown()
            }
        })

        try {
            val completed = latch.await(20, TimeUnit.SECONDS)
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

        if (result.isEmpty() || !receivedAudio) {
            Log.w(TAG, "TTS returned empty audio for: $text")
            return null
        }

        Log.i(TAG, "TTS success for: $text, audio size: ${result.size}")
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
