package io.github.gdict.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gdict.ui.theme.GdictColors
import io.github.gdict.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

private val TRANSPARENT_PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15.toByte(), 0xC4.toByte(), 0x89.toByte(), 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, 0x9C.toByte(), 0x62, 0x00, 0x02, 0x00, 0x01, 0x00, 0x05, 0x18, 0x8D.toByte(), 0xD4.toByte(), 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte())

@Composable
fun WordDetailScreen(
    word: String,
    definition: String,
    dictionaryName: String,
    css: String = "",
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Origin", "Examples", "Synonyms")

    val darkMode by settingsViewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)
    val bgColor = if (darkMode) GdictColors.DarkBackground else GdictColors.LightGray
    val cardColor = if (darkMode) GdictColors.DarkSurface else Color.White
    val textColor = if (darkMode) GdictColors.DarkOnSurface else GdictColors.DarkGray

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.setLanguage(Locale.US)
                ttsReady = true
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GdictColors.NavyBlue)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        "Word Definition",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    IconButton(onClick = { /* share */ }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "分享",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Result",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            word,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val partOfSpeech = remember(definition) { extractPartOfSpeech(definition) }
                        if (partOfSpeech.isNotEmpty()) {
                            Text(
                                partOfSpeech,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GdictColors.TealAccent)
                            .clickable {
                                if (isPlaying) return@clickable
                                isPlaying = true
                                coroutineScope.launch {
                                    try {
                                        val audioData = settingsViewModel.getAudioResource(word)
                                        if (audioData != null) {
                                            withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                playAudioBytes(context, audioData)
                                            }
                                        } else {
                                            val engine = tts
                                            if (engine != null && ttsReady) {
                                                engine.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word_${System.currentTimeMillis()}")
                                            }
                                        }
                                    } catch (_: Exception) {
                                        val engine = tts
                                        if (engine != null && ttsReady) {
                                            engine.speak(word, TextToSpeech.QUEUE_FLUSH, null, "word_${System.currentTimeMillis()}")
                                        }
                                    } finally {
                                        delay(500)
                                        isPlaying = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "发音",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FakeAudioWaveform()

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        TabButton(
                            text = tab,
                            isSelected = selectedTab == index,
                            onClick = { selectedTab = index }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ActionButtonsRow(
                isBookmarked = isBookmarked,
                cardColor = cardColor,
                onToggleBookmark = onToggleBookmark
            )

            Spacer(modifier = Modifier.height(16.dp))

            DefinitionCard(
                word = word,
                definition = definition,
                css = css,
                cardColor = cardColor,
                textColor = textColor,
                darkMode = darkMode,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) Color.White.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = Color.White
        )
    }
}

@Composable
private fun FakeAudioWaveform() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = listOf(8, 16, 24, 32, 20, 12, 28, 36, 16, 8, 20, 32, 24, 12, 28, 16, 8, 20, 32, 24, 12, 28, 16, 8)
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        if (h > 20) GdictColors.TealAccent
                        else Color.White.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    isBookmarked: Boolean,
    cardColor: Color,
    onToggleBookmark: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionButton(
            icon = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            text = if (isBookmarked) "Saved" else "Add to Favorites",
            cardColor = cardColor,
            modifier = Modifier.weight(1f),
            onClick = onToggleBookmark
        )
        ActionButton(
            icon = Icons.Default.Share,
            text = "Share",
            cardColor = cardColor,
            modifier = Modifier.weight(1f),
            onClick = { }
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    cardColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GdictColors.NavyBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = GdictColors.NavyBlue
            )
        }
    }
}

@Composable
private fun DefinitionCard(
    word: String,
    definition: String,
    css: String,
    cardColor: Color,
    textColor: Color,
    darkMode: Boolean,
    settingsViewModel: SettingsViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "Definitions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            HtmlContent(
                definition = definition,
                css = css,
                darkMode = darkMode,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

@Composable
private fun HtmlContent(
    definition: String,
    css: String,
    darkMode: Boolean,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentDarkMode by rememberUpdatedState(darkMode)
    val currentDef by rememberUpdatedState(definition)
    val currentCss by rememberUpdatedState(css)

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
                        request: android.webkit.WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString() ?: return false
                        if (url.startsWith("sound://")) {
                            val audioPath = url.removePrefix("sound://")
                            coroutineScope.launch {
                                try {
                                    val audioData = settingsViewModel.getAudioResourceByPath(audioPath)
                                    if (audioData != null) {
                                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            playAudioBytes(context, audioData)
                                        }
                                    } else {
                                        val word = audioPath.removeSuffix(".mp3")
                                            .removeSuffix(".wav")
                                            .removeSuffix(".ogg")
                                            .removeSuffix(".spx")
                                            .substringAfterLast("/")
                                            .substringAfterLast("\\")
                                        val fallbackData = settingsViewModel.getAudioResource(word)
                                        if (fallbackData != null) {
                                            withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                playAudioBytes(context, fallbackData)
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            return true
                        }
                        return false
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): android.webkit.WebResourceResponse? {
                        val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                        if (url.startsWith("file:///android_asset/") || url.startsWith("data:")) {
                            return super.shouldInterceptRequest(view, request)
                        }
                        val path = request.url?.path ?: return super.shouldInterceptRequest(view, request)
                        val lowerPath = path.lowercase()
                        if (lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") ||
                            lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".gif") ||
                            lowerPath.endsWith(".svg") || lowerPath.endsWith(".webp") ||
                            lowerPath.endsWith(".css") || lowerPath.endsWith(".js")
                        ) {
                            try {
                                val resourcePath = "\\" + path.trimStart('/')
                                android.util.Log.d("MdxWebView", "Intercepting resource: $resourcePath")
                                val data = settingsViewModel.getResourceByPathSync(resourcePath)
                                if (data != null) {
                                    android.util.Log.d("MdxWebView", "Resource loaded successfully: ${data.size} bytes")
                                    val mimeType = when {
                                        lowerPath.endsWith(".png") -> "image/png"
                                        lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") -> "image/jpeg"
                                        lowerPath.endsWith(".gif") -> "image/gif"
                                        lowerPath.endsWith(".svg") -> "image/svg+xml"
                                        lowerPath.endsWith(".webp") -> "image/webp"
                                        lowerPath.endsWith(".css") -> "text/css"
                                        lowerPath.endsWith(".js") -> "application/javascript"
                                        else -> "application/octet-stream"
                                    }
                                    return android.webkit.WebResourceResponse(
                                        mimeType, "UTF-8", java.io.ByteArrayInputStream(data)
                                    )
                                } else {
                                    android.util.Log.w("MdxWebView", "Resource not found, returning transparent placeholder: $resourcePath")
                                    val isImage = lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") ||
                                        lowerPath.endsWith(".jpeg") || lowerPath.endsWith(".gif") ||
                                        lowerPath.endsWith(".svg") || lowerPath.endsWith(".webp")
                                    if (isImage) {
                                        return android.webkit.WebResourceResponse(
                                            "image/png", "UTF-8", java.io.ByteArrayInputStream(TRANSPARENT_PNG)
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MdxWebView", "Error loading resource: $path", e)
                            }
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }
            }
        },
        update = { webView ->
            val htmlContent = buildHtmlContent(currentDef, currentCss)
            webView.loadDataWithBaseURL("https://mdx.local/", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun buildHtmlContent(definition: String, css: String): String {
    val isCambridgeEpd = definition.contains("cepd18.css")
    val transformedDef = transformMdxTags(definition).let { def ->
        if (isCambridgeEpd) {
            def.replace(Regex("""<img[^>]*src=["'][^"']*uk_sound\.png[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "<span class='uk-flag'>UK</span>")
               .replace(Regex("""<img[^>]*src=["'][^"']*us_sound\.png[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "<span class='us-flag'>US</span>")
               .replace(Regex("</img>", RegexOption.IGNORE_CASE), "")
        } else def
    }

    val dictCssBlock = if (css.isNotEmpty()) {
        "<style>\n$css\n</style>\n"
    } else if (isCambridgeEpd) {
        """
<style>
.cpepd .arl { display:block;margin:6px 0;padding:8px 10px;border-bottom:1px solid var(--border); }
.cpepd .hit { display:block;margin:4px 0;padding:2px 0; }
.cpepd .hit+.hit { border-top:1px solid var(--border-light);padding-top:6px; }
.cpepd .results { display:inline; }
.cpepd .base { display:inline;font-size:1em; }
.cpepd .hw { font-weight:bold;font-size:1.2em;color:var(--header);display:inline; }
.cpepd .inf { color:var(--phon);font-style:italic; }
.cpepd .comment { color:var(--subtle);font-style:italic;font-size:.9em; }
.cpepd .forms { display:block;margin:2px 0 2px 16px; }
.cpepd .inflections { display:inline; }
.cpepd .inflections .base { color:var(--subtle); }
.cpepd .pron,.cpepd .ipa { color:var(--phon);font-family:'Lucida Sans Unicode','Arial Unicode MS',sans-serif;display:inline; }
.cpepd .prongrp { display:block;margin:3px 0;padding:2px 0; }
.cpepd .ussymbol { display:inline-block;padding:0 4px;margin:0 2px;font-size:.8em;font-weight:600; }
.cpepd .uk-flag { display:inline-block;padding:1px 5px;margin:0 2px;background:var(--flag-uk);color:#fff;border-radius:3px;font-size:10px;font-weight:bold;vertical-align:middle;letter-spacing:.5px; }
.cpepd .us-flag { display:inline-block;padding:1px 5px;margin:0 2px;background:var(--flag-us);color:#fff;border-radius:3px;font-size:10px;font-weight:bold;vertical-align:middle;letter-spacing:.5px; }
.cpepd .soundfile { display:inline;margin:0 2px; }
.cpepd .soundfile a { display:inline-block;vertical-align:middle;text-decoration:none; }
.cpepd .soundfile img { display:none; }
.cpepd a[href^="sound://"] { text-decoration:none;display:inline-block;vertical-align:middle;padding:2px 8px;margin:0 2px;background:var(--speaker-hover);border-radius:12px;color:var(--link);font-size:13px; }
.cpepd a[href^="sound://"]::before { content:"\01F50A";margin-right:4px; }
.cpepd a[href^="sound://"]:hover { background:var(--speaker-hover);filter:brightness(1.2); }
.cpepd .capvar { color:var(--subtle);font-style:italic;font-size:.9em; }
.cpepd .inflection { display:block;margin:2px 0 2px 16px; }
</style>
"""
    } else {
        ""
    }

    val varCss = """
:root {
  --bg:#FFFFFF;--text:#424242;--header:#2C4A6E;--link:#4ECDC4;--phon:#1565C0;
  --border:rgba(128,128,128,0.15);--border-light:rgba(128,128,128,0.08);
  --accent-bg:rgba(44,74,110,0.1);--speaker-bg:rgba(78,205,196,0.15);
  --speaker-hover:rgba(78,205,196,0.3);--def-border:rgba(78,205,196,0.3);
  --example:#666;--subtle:#9E9E9E;
  --flag-uk:#012169;--flag-us:#B31942;
  --tag-bg:rgba(244,67,54,0.1);--tag-color:#D32F2F;
  --pos-bg:rgba(44,74,110,0.1);--table-border:#E0E0E0;
  --uk-badge-bg:rgba(33,150,243,0.1);--uk-badge-color:#1976D2;
  --di-head-border:var(--header);
}
body.dark {
  --bg:#1A1C17;--text:#E1E4DA;--header:#8BB8E8;--link:#4ECDC4;--phon:#A8D8EA;
  --border:rgba(255,255,255,0.1);--border-light:rgba(255,255,255,0.06);
  --accent-bg:rgba(139,184,232,0.12);--speaker-bg:rgba(78,205,196,0.2);
  --speaker-hover:rgba(78,205,196,0.35);--def-border:rgba(78,205,196,0.4);
  --example:#A0A0A0;--subtle:#888;
  --flag-uk:#1A3A8A;--flag-us:#8B1A2B;
  --tag-bg:rgba(244,67,54,0.15);--tag-color:#EF9A9A;
  --pos-bg:rgba(139,184,232,0.12);--table-border:rgba(255,255,255,0.12);
  --uk-badge-bg:rgba(100,181,246,0.12);--uk-badge-color:#64B5F6;
  --di-head-border:var(--header);
}

body{font-family:-apple-system,'Segoe UI',Roboto,sans-serif;font-size:15px;line-height:1.7;color:var(--text);background:var(--bg);margin:0;padding:8px 12px;word-wrap:break-word;}
h1,h2,h3{color:var(--header);font-weight:600;margin:12px 0 8px;}
a{color:var(--link);text-decoration:none;}
img{max-width:100%;height:auto;}
table{border-collapse:collapse;width:100%;}
td,th{border:1px solid var(--table-border);padding:8px;}
hw{font-weight:bold;color:var(--header);}
inf{font-style:italic;}
.arl{display:block;margin-bottom:12px;padding-bottom:8px;border-bottom:1px solid var(--border);}
.hit{display:block;margin:2px 0;padding:2px 0;}
.comment{font-style:italic;color:var(--subtle);}
.capvar{color:var(--subtle);font-style:italic;}
.phon,.pron,.ipa{color:var(--phon);font-family:'Lucida Sans Unicode','Arial Unicode MS',sans-serif;}
.speaker,.sound,.audio-play{cursor:pointer;display:inline-block;padding:2px 6px;margin:0 4px;background:var(--speaker-bg);border-radius:12px;color:var(--link);font-size:14px;}
.speaker:hover,.sound:hover,.audio-play:hover{background:var(--speaker-hover);}
.soundfile{display:inline;margin:0 2px;}
.soundfile a{cursor:pointer;display:inline-block;padding:2px 8px;margin:0 4px;background:var(--speaker-bg);border-radius:12px;color:var(--link);text-decoration:none;font-size:13px;}
.soundfile a:hover{background:var(--speaker-hover);}
a[href^="sound://"]{cursor:pointer;display:inline-block;padding:2px 8px;margin:0 4px;background:var(--speaker-bg);border-radius:12px;color:var(--link);text-decoration:none;}
a[href^="sound://"]:hover{background:var(--speaker-hover);}
.pos,.pos2{display:inline-block;padding:2px 8px;margin:2px 4px;background:var(--pos-bg);border-radius:4px;font-style:italic;color:var(--header);font-size:.9em;}
.bre,.ame,.gb,.us{display:inline-block;padding:1px 6px;margin:0 4px;border-radius:4px;font-size:.8em;font-weight:600;}
.bre,.gb{background:var(--uk-badge-bg);color:var(--uk-badge-color);}
.ame,.us{background:var(--tag-bg);color:var(--tag-color);}
.ussymbol{display:inline-block;padding:1px 6px;margin:0 4px;border-radius:4px;font-size:.8em;font-weight:600;background:var(--tag-bg);color:var(--tag-color);}
.label,.sense{margin:4px 0;padding:2px 0;}
.definition,.def{margin:4px 0 8px;padding-left:12px;border-left:3px solid var(--def-border);}
.example,.ex{color:var(--example);font-style:italic;margin:4px 0 4px 16px;}
.di-head{display:block;margin:16px 0 8px;padding-bottom:4px;border-bottom:2px solid var(--di-head-border);}
.di-title{display:block;font-size:1.3em;font-weight:700;}
.di-info{display:inline;}
.di-body{display:block;margin:4px 0;}
.sense-block{display:block;margin:8px 0;padding:4px 0;}
.sense-head{display:block;margin:6px 0 2px;}
.sense-body{display:block;margin:2px 0;padding-left:8px;}
.sense-info{display:inline;}
.prongrp{display:block;margin:2px 0;}
.inflection{display:block;margin:4px 0;padding-left:12px;}
.INFLX{display:inline;margin:0 4px;color:var(--subtle);}
.base{display:inline;}
.results{display:inline;}
.forms{display:inline;}
.inflections{display:inline;}
.hw{font-size:1.1em;color:var(--header);}
.inf{font-style:italic;}
.cm{font-weight:600;}
"""

    val jsBlock = """
<script>
function setTheme(d){if(d){document.body.classList.add('dark')}else{document.body.classList.remove('dark')}}
function fixInlineStyles(){if(!document.body.classList.contains('dark'))return;var els=document.querySelectorAll('[style]');for(var i=0;i<els.length;i++){var s=els[i].style;var bg=s.backgroundColor||'';if(bg){var lb=bg.toLowerCase();if(lb.indexOf('#fff')>=0||lb.indexOf('#ffffff')>=0||lb.indexOf('white')>=0||lb.indexOf('rgb(255,')>=0){s.backgroundColor=''}}var bi=s.backgroundImage||'';if(bi&&bi.indexOf('url(')>=0&&bi.indexOf('data:')<0){s.backgroundImage='none'}var co=s.color||'';if(co){var lc=co.toLowerCase();if(lc.indexOf('#000')>=0||lc.indexOf('#000000')>=0||lc.indexOf('black')>=0||lc.indexOf('rgb(0,')>=0){s.color=''}}var b=s.background||'';if(b&&b.indexOf('#fff')>=0){s.background=''}}}
document.addEventListener('DOMContentLoaded',fixInlineStyles)
</script>
"""

    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
$varCss
</style>
$dictCssBlock
</head>
<body>
$transformedDef
$jsBlock
</body>
</html>
    """.trimIndent()
}

private fun transformMdxTags(input: String): String {
    var result = input
    result = result.replace(Regex("<SEP[^>]*>([^<]*)</SEP>", RegexOption.IGNORE_CASE)) { match ->
        val content = match.groupValues[1].trim()
        if (content.isEmpty()) " " else " $content "
    }
    result = result.replace(Regex("<SEP\\s*/?>", RegexOption.IGNORE_CASE), " ")
    result = result.replace(Regex("</SEP>", RegexOption.IGNORE_CASE), "")
    result = result.replace(Regex("<hw>", RegexOption.IGNORE_CASE), "<b class='hw'>")
    result = result.replace(Regex("</hw>", RegexOption.IGNORE_CASE), "</b>")
    result = result.replace(Regex("<inf>", RegexOption.IGNORE_CASE), "<i class='inf'>")
    result = result.replace(Regex("</inf>", RegexOption.IGNORE_CASE), "</i>")
    result = result.replace(Regex("<ex>", RegexOption.IGNORE_CASE), "<span class='ex'>")
    result = result.replace(Regex("</ex>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<hit[^>]*>", RegexOption.IGNORE_CASE), "<div class='hit'>")
    result = result.replace(Regex("</hit>", RegexOption.IGNORE_CASE), "</div>")
    result = result.replace(Regex("<link\\s+rel=stylesheet[^>]*>", RegexOption.IGNORE_CASE), "")
    result = result.replace(Regex("<meta[^>]*>", RegexOption.IGNORE_CASE), "")
    result = result.replace(Regex("<soundfile>", RegexOption.IGNORE_CASE), "<span class='soundfile'>")
    result = result.replace(Regex("</soundfile>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<pronunciation-practice\\s*/?>", RegexOption.IGNORE_CASE), "")
    result = result.replace(Regex("<di-info\\s*/?>", RegexOption.IGNORE_CASE), "")
    result = result.replace(Regex("<sense-head>", RegexOption.IGNORE_CASE), "<div class='sense-head'>")
    result = result.replace(Regex("</sense-head>", RegexOption.IGNORE_CASE), "</div>")
    result = result.replace(Regex("<ipa>", RegexOption.IGNORE_CASE), "<span class='ipa'>")
    result = result.replace(Regex("</ipa>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<prongrp>", RegexOption.IGNORE_CASE), "<span class='prongrp'>")
    result = result.replace(Regex("</prongrp>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<inflection>", RegexOption.IGNORE_CASE), "<span class='inflection'>")
    result = result.replace(Regex("</inflection>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<capvar>", RegexOption.IGNORE_CASE), "<span class='capvar'>")
    result = result.replace(Regex("</capvar>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<sense-block>", RegexOption.IGNORE_CASE), "<span class='sense-block'>")
    result = result.replace(Regex("</sense-block>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<sense-body>", RegexOption.IGNORE_CASE), "<span class='sense-body'>")
    result = result.replace(Regex("</sense-body>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<di-head>", RegexOption.IGNORE_CASE), "<span class='di-head'>")
    result = result.replace(Regex("</di-head>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<di-title>", RegexOption.IGNORE_CASE), "<span class='di-title'>")
    result = result.replace(Regex("</di-title>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<di-body>", RegexOption.IGNORE_CASE), "<span class='di-body'>")
    result = result.replace(Regex("</di-body>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<arl>", RegexOption.IGNORE_CASE), "<span class='arl'>")
    result = result.replace(Regex("</arl>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<base>", RegexOption.IGNORE_CASE), "<span class='base'>")
    result = result.replace(Regex("</base>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<results>", RegexOption.IGNORE_CASE), "<span class='results'>")
    result = result.replace(Regex("</results>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<forms>", RegexOption.IGNORE_CASE), "<span class='forms'>")
    result = result.replace(Regex("</forms>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<inflections>", RegexOption.IGNORE_CASE), "<span class='inflections'>")
    result = result.replace(Regex("</inflections>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<pron>", RegexOption.IGNORE_CASE), "<span class='pron'>")
    result = result.replace(Regex("</pron>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<ussymbol>", RegexOption.IGNORE_CASE), "<span class='ussymbol'>")
    result = result.replace(Regex("</ussymbol>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<sense-info>", RegexOption.IGNORE_CASE), "<span class='sense-info'>")
    result = result.replace(Regex("</sense-info>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)) { match ->
        "href=\"sound://${match.groupValues[1]}\" onclick=\"event.preventDefault(); window.location.href=this.href;\""
    }
    return result
}

private fun extractPartOfSpeech(definition: String): String {
    if (definition.isBlank()) return ""
    val posPatterns = listOf(
        Regex("<pos>([^<]+)</pos>", RegexOption.IGNORE_CASE),
        Regex("<(?:span|font)[^>]*>(adj|adv|n|v|pron|prep|conj|interj|art|num|modal|det)[.;]?\\s*</(?:span|font)>", RegexOption.IGNORE_CASE),
        Regex("\\b(adj\\.|adv\\.|n\\.|v\\.|pron\\.|prep\\.|conj\\.|interj\\.|art\\.|num\\.|modal\\.|det\\.)\\s*", RegexOption.IGNORE_CASE),
        Regex("<(?:b|strong)[^>]*>([^<]{1,20})</(?:b|strong)>", RegexOption.IGNORE_CASE),
        Regex("(noun|verb|adjective|adverb|pronoun|preposition|conjunction|interjection|article|numeral|determiner|modal verb)[.,;]?\\s*", RegexOption.IGNORE_CASE)
    )
    for (pattern in posPatterns) {
        val match = pattern.find(definition)
        if (match != null) {
            val raw = match.groupValues[1].trim().lowercase()
            return when {
                raw.startsWith("adj") -> "adj."
                raw.startsWith("adv") -> "adv."
                raw.startsWith("n") && !raw.startsWith("num") -> "n."
                raw.startsWith("v") -> "v."
                raw.startsWith("pron") -> "pron."
                raw.startsWith("prep") -> "prep."
                raw.startsWith("conj") -> "conj."
                raw.startsWith("interj") -> "interj."
                raw.startsWith("art") -> "art."
                raw.startsWith("num") -> "num."
                raw.startsWith("modal") -> "modal."
                raw.startsWith("det") -> "det."
                raw == "noun" -> "n."
                raw == "verb" -> "v."
                raw == "adjective" -> "adj."
                raw == "adverb" -> "adv."
                raw == "pronoun" -> "pron."
                raw == "preposition" -> "prep."
                raw == "conjunction" -> "conj."
                raw == "interjection" -> "interj."
                raw == "article" -> "art."
                raw == "numeral" -> "num."
                raw == "determiner" -> "det."
                else -> raw
            }
        }
    }
    return ""
}

private fun playAudioBytes(context: Context, audioData: ByteArray) {
    val tempFile = File(context.cacheDir, "dict_audio_${System.currentTimeMillis()}.mp3")
    var mediaPlayer: android.media.MediaPlayer? = null
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
    } catch (e: Exception) {
        mediaPlayer?.release()
        tempFile.delete()
        throw e
    }
}
