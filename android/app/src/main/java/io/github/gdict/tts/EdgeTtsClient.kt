package io.github.gdict.tts

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object EdgeTtsClient {

    private const val TAG = "EdgeTtsClient"
    private const val YOUDAO_TTS_URL = "https://dict.youdao.com/dictvoice"
    private const val MAX_TEXT_LENGTH = 200

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun synthesize(text: String): ByteArray? {
        if (text.isBlank()) return null

        val cleanText = text.take(MAX_TEXT_LENGTH)
        val encodedText = URLEncoder.encode(cleanText, "UTF-8")

        // type: 0=英语, 1=日语, 2=中文, 3=韩语
        val url = "$YOUDAO_TTS_URL?audio=$encodedText&type=0"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Referer", "https://dict.youdao.com/")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.bytes()
                if (body != null && body.isNotEmpty()) {
                    Log.i(TAG, "Youdao TTS success: '$cleanText' -> ${body.size} bytes")
                    body
                } else {
                    Log.w(TAG, "Youdao TTS returned empty audio for: $cleanText")
                    null
                }
            } else {
                Log.w(TAG, "Youdao TTS failed: HTTP ${response.code} for: $cleanText")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Youdao TTS error: ${e.message}")
            null
        }
    }
}
