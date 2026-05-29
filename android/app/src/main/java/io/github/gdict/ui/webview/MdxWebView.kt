package io.github.gdict.ui.webview

import android.os.Build
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.gdict.R
import io.github.gdict.data.AndroidDictionaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import kotlin.math.roundToInt

val TRANSPARENT_PNG = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
    0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15.toByte(), 0xC4.toByte(),
    0x89.toByte(), 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
    0x54, 0x78, 0x9C.toByte(), 0x62, 0x00, 0x02, 0x00, 0x01,
    0x00, 0x05, 0x18, 0x8D.toByte(), 0xD4.toByte(), 0x00, 0x00, 0x00,
    0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(), 0x42, 0x60,
    0x82.toByte()
)

@Composable
fun MdxWebView(
    definition: String,
    css: String,
    darkMode: Boolean,
    contentScale: Float = 1f,
    dictionaryRepository: AndroidDictionaryRepository,
    onEntryClick: (String) -> Unit = {},
    onPlayAudio: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentDarkMode by rememberUpdatedState(darkMode)
    val currentDef by rememberUpdatedState(definition)
    val currentCss by rememberUpdatedState(css)
    val currentOnEntryClick by rememberUpdatedState(onEntryClick)
    val currentOnPlayAudio by rememberUpdatedState(onPlayAudio)
    val currentScale by rememberUpdatedState(contentScale)

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.domStorageEnabled = true
                settings.blockNetworkLoads = true
                setBackgroundColor(0x00000000)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    settings.isAlgorithmicDarkeningAllowed = true
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript("setTheme($currentDarkMode);fixInlineStyles();", null)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        if (url.startsWith("entry://")) {
                            val entry = url.removePrefix("entry://")
                            currentOnEntryClick(entry)
                            return true
                        }
                        if (url.startsWith("bword://")) {
                            val entry = url.removePrefix("bword://")
                            currentOnEntryClick(entry)
                            return true
                        }
                        if (url.startsWith("sound://")) {
                            val audioPath = url.removePrefix("sound://")
                            coroutineScope.launch {
                                try {
                                    val audioData = dictionaryRepository.getAudioResourceByPath(audioPath)
                                        ?: dictionaryRepository.getAudioResource(
                                            audioPath.removeSuffix(".mp3")
                                                .removeSuffix(".wav")
                                                .removeSuffix(".ogg")
                                                .removeSuffix(".spx")
                                                .substringAfterLast("/")
                                                .substringAfterLast("\\")
                                        )
                                    var played = false
                                    if (audioData != null) {
                                        played = withContext(Dispatchers.IO) {
                                            AudioPlayer.play(context, audioData)
                                        }
                                    }
                                    if (!played) {
                                        currentOnPlayAudio(audioPath)
                                    }
                                } catch (_: Exception) {}
                            }
                            return true
                        }
                        return false
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url?.toString()
                            ?: return super.shouldInterceptRequest(view, request)
                        if (url.startsWith("file:///android_asset/") || url.startsWith("data:")) {
                            return super.shouldInterceptRequest(view, request)
                        }
                        if (url.startsWith("entry://")) {
                            return WebResourceResponse(
                                "text/plain", "UTF-8",
                                ByteArrayInputStream(ByteArray(0))
                            )
                        }
                        if (url.startsWith("bword://")) {
                            return WebResourceResponse(
                                "text/plain", "UTF-8",
                                ByteArrayInputStream(ByteArray(0))
                            )
                        }
                        val rawPath = request.url?.path
                            ?: return super.shouldInterceptRequest(view, request)
                        val path = URLDecoder.decode(rawPath, "UTF-8")
                        val lowerPath = path.lowercase()
                        if (!isInterceptableExtension(lowerPath)) {
                            return super.shouldInterceptRequest(view, request)
                        }
                        try {
                            val mimeType = mimeTypeForPath(lowerPath)
                            val normalizedPath = path.replace("/", "\\")
                            val trimmedPath = normalizedPath.trimStart('\\')
                            val candidates = buildList {
                                add("\\$trimmedPath")
                                add("\\\\$trimmedPath")
                                val fileName = path.substringAfterLast("/")
                                if (fileName.isNotEmpty()) {
                                    add("\\$fileName")
                                }
                                val pathWithForwardSlash = "/" + path.trimStart('/')
                                add(pathWithForwardSlash)
                            }
                            for (candidate in candidates) {
                                val data = dictionaryRepository.getAudioResourceByPathSync(candidate)
                                if (data != null) {
                                    android.util.Log.d(
                                        "MdxWebView",
                                        "Resource loaded via '$candidate': ${data.size} bytes"
                                    )
                                    return WebResourceResponse(
                                        mimeType, "UTF-8",
                                        ByteArrayInputStream(data)
                                    )
                                }
                            }
                            android.util.Log.w(
                                "MdxWebView",
                                "Resource not found: $path (tried: $candidates)"
                            )
                            if (isImageExtension(lowerPath)) {
                                return WebResourceResponse(
                                    "image/png", "UTF-8",
                                    ByteArrayInputStream(TRANSPARENT_PNG)
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MdxWebView", "Error loading resource: $path", e)
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }
            }
        },
        update = { webView ->
            val textZoomPercent = (currentScale * 100).roundToInt()
            if (webView.settings.textZoom != textZoomPercent) {
                webView.settings.textZoom = textZoomPercent
            }
            val htmlContent = HtmlContentBuilder.build(currentDef, currentCss)
            val tag = webView.getTag(R.id.tag_webview_content) as? String
            if (tag != htmlContent) {
                webView.setTag(R.id.tag_webview_content, htmlContent)
                webView.loadDataWithBaseURL(
                    "https://mdx.local/", htmlContent,
                    "text/html", "UTF-8", null
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun isInterceptableExtension(lowerPath: String): Boolean {
    return lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") ||
            lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".gif") ||
            lowerPath.endsWith(".svg") || lowerPath.endsWith(".webp") ||
            lowerPath.endsWith(".css") || lowerPath.endsWith(".js") ||
            lowerPath.endsWith(".ttf") || lowerPath.endsWith(".woff") ||
            lowerPath.endsWith(".woff2") || lowerPath.endsWith(".mp3") ||
            lowerPath.endsWith(".wav") || lowerPath.endsWith(".ogg") ||
            lowerPath.endsWith(".spx")
}

private fun isImageExtension(lowerPath: String): Boolean {
    return lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") ||
            lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".gif") ||
            lowerPath.endsWith(".svg") || lowerPath.endsWith(".webp")
}

private fun mimeTypeForPath(lowerPath: String): String = when {
    lowerPath.endsWith(".png") -> "image/png"
    lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") -> "image/jpeg"
    lowerPath.endsWith(".gif") -> "image/gif"
    lowerPath.endsWith(".svg") -> "image/svg+xml"
    lowerPath.endsWith(".webp") -> "image/webp"
    lowerPath.endsWith(".css") -> "text/css"
    lowerPath.endsWith(".js") -> "application/javascript"
    lowerPath.endsWith(".ttf") -> "font/ttf"
    lowerPath.endsWith(".woff") -> "font/woff"
    lowerPath.endsWith(".woff2") -> "font/woff2"
    lowerPath.endsWith(".mp3") -> "audio/mpeg"
    lowerPath.endsWith(".wav") -> "audio/wav"
    lowerPath.endsWith(".ogg") -> "audio/ogg"
    lowerPath.endsWith(".spx") -> "audio/speex"
    else -> "application/octet-stream"
}
