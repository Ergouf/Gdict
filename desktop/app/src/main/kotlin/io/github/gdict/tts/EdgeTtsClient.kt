package io.github.gdict.tts

import io.github.gdict.core.GdictLogger
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object EdgeTtsClient {

    private const val YOUDAO_TTS_URL = "https://dict.youdao.com/dictvoice"
    private const val MAX_TEXT_LENGTH = 200

    private val log = GdictLogger.get()

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun synthesize(text: String): ByteArray? {
        if (text.isBlank()) return null

        val cleanText = text.take(MAX_TEXT_LENGTH)
        val encodedText = URLEncoder.encode(cleanText, "UTF-8")

        // type: 0=英语, 1=日语, 2=中文, 3=韩语
        val url = "$YOUDAO_TTS_URL?audio=$encodedText&type=0"

        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://dict.youdao.com/")
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())

            if (response.statusCode() == 200) {
                val body = response.body()
                if (body.isNotEmpty()) {
                    log.i("EdgeTtsClient", "Youdao TTS success: '$cleanText' -> ${body.size} bytes")
                    body
                } else {
                    log.w("EdgeTtsClient", "Youdao TTS returned empty audio for: $cleanText")
                    null
                }
            } else {
                log.w("EdgeTtsClient", "Youdao TTS failed: HTTP ${response.statusCode()} for: $cleanText")
                null
            }
        } catch (e: Exception) {
            log.w("EdgeTtsClient", "Youdao TTS error: ${e.message}")
            null
        }
    }
}
