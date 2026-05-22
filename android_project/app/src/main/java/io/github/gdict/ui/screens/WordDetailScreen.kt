package io.github.gdict.ui.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
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
import io.github.gdict.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun WordDetailScreen(
    word: String,
    definition: String,
    dictionaryName: String,
    css: String = "",
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    viewModel: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Origin", "Examples", "Synonyms")

    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle(initialValue = false)
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
                                        val audioData = viewModel.getAudioResource(word)
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
                darkMode = darkMode
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
    darkMode: Boolean
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
                darkMode = darkMode
            )
        }
    }
}

@Composable
private fun HtmlContent(
    definition: String,
    css: String,
    darkMode: Boolean
) {
    val htmlContent = buildHtmlContent(definition, css, darkMode)

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun buildHtmlContent(definition: String, css: String, isDarkTheme: Boolean): String {
    val bgColor = if (isDarkTheme) "#1A1C17" else "#FFFFFF"
    val textColor = if (isDarkTheme) "#E1E4DA" else "#424242"
    val headerColor = if (isDarkTheme) "#8BB8E8" else "#2C4A6E"
    val linkColor = "#4ECDC4"

    val transformedDef = transformMdxTags(definition)

    val cssBlock = if (css.isNotEmpty()) {
        "<style>\n$css\n</style>\n"
    } else {
        ""
    }

    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
${cssBlock}<style>
  body {
    font-family: -apple-system, 'Segoe UI', Roboto, sans-serif;
    font-size: 15px;
    line-height: 1.7;
    color: $textColor;
    background: $bgColor;
    margin: 0;
    padding: 0;
    word-wrap: break-word;
  }
  h1, h2, h3 { color: $headerColor; font-weight: 600; margin: 12px 0 8px; }
  a { color: $linkColor; text-decoration: none; }
  img { max-width: 100%; height: auto; }
  table { border-collapse: collapse; width: 100%; }
  td, th { border: 1px solid #E0E0E0; padding: 8px; }
  hw { font-weight: bold; color: $headerColor; }
  inf { font-style: italic; }
  .arl { display: block; margin-bottom: 12px; }
  .comment { font-style: italic; color: #9E9E9E; }
</style>
</head>
<body>
  $transformedDef
</body>
</html>
    """.trimIndent()
}

private fun transformMdxTags(input: String): String {
    var result = input
    result = result.replace(Regex("<SEP\\s*/?>", RegexOption.IGNORE_CASE), " <span style='color:#9E9E9E'>|</span> ")
    result = result.replace(Regex("<hw>", RegexOption.IGNORE_CASE), "<b class='hw'>")
    result = result.replace(Regex("</hw>", RegexOption.IGNORE_CASE), "</b>")
    result = result.replace(Regex("<inf>", RegexOption.IGNORE_CASE), "<i class='inf'>")
    result = result.replace(Regex("</inf>", RegexOption.IGNORE_CASE), "</i>")
    result = result.replace(Regex("<hit[^>]*>", RegexOption.IGNORE_CASE), "")
    result = result.replace(Regex("</hit>", RegexOption.IGNORE_CASE), "")
    result = result.replace(Regex("<link\\s+rel=stylesheet[^>]*>", RegexOption.IGNORE_CASE), "")
    result = result.replace(Regex("<meta[^>]*>", RegexOption.IGNORE_CASE), "")
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
