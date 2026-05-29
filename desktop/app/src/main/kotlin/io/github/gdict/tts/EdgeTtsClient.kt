package io.github.gdict.tts

import io.github.gdict.core.GdictLogger
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit

object EdgeTtsClient {

    private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    private const val WSS_BASE_URL =
        "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"
    private const val SEC_MS_GEC_VERSION = "1-143.0.3650.75"
    private const val WIN_EPOCH = 11644473600L

    private const val OUTPUT_FORMAT = "audio-24khz-48kbitrate-mono-mp3"
    private val VOICES = arrayOf(
        "en-US-AriaNeural",
        "en-US-JennyNeural",
        "en-US-GuyNeural"
    )

    private val log = GdictLogger.get()

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
            log.w("EdgeTtsClient", "Failed to generate Sec-MS-GEC: ${it.message}")
        }.getOrDefault("")
    }

    private fun buildWssUrl(connectionId: String): String {
        val secMsGec = generateSecMsGec()
        return "$WSS_BASE_URL?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
            "&Sec-MS-GEC=$secMsGec" +
            "&Sec-MS-GEC-Version=$SEC_MS_GEC_VERSION" +
            "&ConnectionId=$connectionId"
    }

    fun synthesize(text: String): ByteArray? {
        if (text.isBlank()) return null

        val voice = VOICES[0]
        val requestId = UUID.randomUUID().toString().replace("-", "")
        val connectionId = UUID.randomUUID().toString()
        val wssUrl = buildWssUrl(connectionId)

        val audioBuffer = ByteArrayOutputStream()
        val doneFuture = CompletableFuture<Void>()
        var ttsError: Throwable? = null

        val client = HttpClient.newBuilder()
            .build()

        val webSocket: WebSocket
        try {
            webSocket = client.newWebSocketBuilder()
                .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0")
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .buildAsync(URI.create(wssUrl), object : WebSocket.Listener {
                    private val buffer = StringBuilder()

                    override fun onOpen(webSocket: WebSocket) {
                        val configJson = """
                            {"context":{"synthesis":{"audio":{"metadataoptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"true"},"outputFormat":"$OUTPUT_FORMAT"}}}}
                        """.trimIndent()

                        webSocket.sendText(
                            "X-Timestamp:${java.time.Instant.now()}\r\n" +
                            "Content-Type:application/json; charset=utf-8\r\n" +
                            "Path:speech.config\r\n\r\n$configJson",
                            true
                        )

                        val ssml = buildSsml(text, voice)
                        webSocket.sendText(
                            "X-RequestId:$requestId\r\n" +
                            "Content-Type:application/ssml+xml\r\n" +
                            "X-Timestamp:${java.time.Instant.now()}Z\r\n" +
                            "Path:ssml\r\n\r\n$ssml",
                            true
                        )
                    }

                    override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                        buffer.append(data)
                        if (last) {
                            val message = buffer.toString()
                            buffer.clear()
                            if (message.contains("Path:turn.end")) {
                                doneFuture.complete(null)
                            }
                        }
                        return null
                    }

                    override fun onBinary(webSocket: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage<*>? {
                        val bytes = ByteArray(data.remaining())
                        data.get(bytes)
                        if (bytes.size > 2) {
                            val headerLen = (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
                            if (headerLen in 0..bytes.size) {
                                audioBuffer.write(bytes, headerLen, bytes.size - headerLen)
                            } else {
                                audioBuffer.write(bytes)
                            }
                        } else {
                            audioBuffer.write(bytes)
                        }
                        return null
                    }

                    override fun onError(webSocket: WebSocket, error: Throwable) {
                        log.w("EdgeTtsClient", "WebSocket error: ${error.message}")
                        ttsError = error
                        doneFuture.complete(null)
                    }
                }).get(8, TimeUnit.SECONDS)
        } catch (e: Exception) {
            log.w("EdgeTtsClient", "WebSocket connect failed: ${e.message}")
            return null
        }

        try {
            doneFuture.get(15, TimeUnit.SECONDS)
        } catch (e: Exception) {
            log.w("EdgeTtsClient", "TTS timeout for: $text")
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "timeout")
            return null
        }

        val result = audioBuffer.toByteArray()
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done")

        if (ttsError != null) {
            log.w("EdgeTtsClient", "TTS failed: ${ttsError!!.message}")
            return null
        }

        if (result.isEmpty()) {
            log.w("EdgeTtsClient", "TTS returned empty audio for: $text")
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