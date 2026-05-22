package io.github.gdict.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WordDetailScreen(
    word: String,
    definition: String,
    dictionaryName: String,
    css: String = "",
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (dictionaryName.isNotEmpty()) {
                    Text(
                        text = dictionaryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isBookmarked) "取消收藏" else "收藏",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            val isDarkTheme = isSystemInDarkTheme()
            val htmlContent = buildHtmlContent(definition, css, isDarkTheme)

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun buildHtmlContent(definition: String, css: String = "", isDarkTheme: Boolean): String {
    val bgColor = if (isDarkTheme) "#1C1B1F" else "#FFFBFE"
    val textColor = if (isDarkTheme) "#E6E1E5" else "#1C1B1F"
    val headerColor = if (isDarkTheme) "#D0BCFF" else "#6750A4"
    val linkColor = if (isDarkTheme) "#D0BCFF" else "#6750A4"
    val borderColor = if (isDarkTheme) "#49454F" else "#CAC4D0"

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
    font-size: 16px;
    line-height: 1.7;
    color: $textColor;
    background: $bgColor;
    padding: 16px;
    margin: 0;
    word-wrap: break-word;
  }
  h1, h2, h3 { color: $headerColor; font-weight: 600; }
  a { color: $linkColor; text-decoration: none; }
  img { max-width: 100%; height: auto; }
  table { border-collapse: collapse; width: 100%; }
  td, th { border: 1px solid $borderColor; padding: 8px; }
  SEP { display: inline-block; width: 8px; }
  hw { font-weight: bold; color: $headerColor; }
  inf { font-style: italic; }
  .arl { display: block; margin-bottom: 12px; }
  .results { display: inline-block; }
  .base { font-weight: bold; }
  .comment { font-style: italic; color: #666; margin-left: 6px; }
  .inflections { margin-left: 4px; }
  .forms { display: inline; }
  br.sep { display: block; content: ''; margin: 4px 0; }
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
    result = result.replace(Regex("<SEP\\s*/?>", RegexOption.IGNORE_CASE), " <span class='sep'>|</span> ")
    result = result.replace(Regex("<hw>", RegexOption.IGNORE_CASE), "<b class='hw'>")
    result = result.replace(Regex("</hw>", RegexOption.IGNORE_CASE), "</b>")
    result = result.replace(Regex("<inf>", RegexOption.IGNORE_CASE), "<i class='inf'>")
    result = result.replace(Regex("</inf>", RegexOption.IGNORE_CASE), "</i>")
    result = result.replace(Regex("<ex>", RegexOption.IGNORE_CASE), "<span class='ex'>")
    result = result.replace(Regex("</ex>", RegexOption.IGNORE_CASE), "</span>")
    result = result.replace(Regex("<hit[^>]*>", RegexOption.IGNORE_CASE), "")
    result = result.replace(Regex("</hit>", RegexOption.IGNORE_CASE), "")
    result = result.replace(Regex("<link\\s+rel=stylesheet[^>]*>", RegexOption.IGNORE_CASE), "")
    result = result.replace(Regex("<meta[^>]*>", RegexOption.IGNORE_CASE), "")
    return result
}
