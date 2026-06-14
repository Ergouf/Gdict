package io.github.gdict.util

object HtmlUtils {

    // Precompiled regex patterns for hot-path HTML stripping
    private val RE_STYLE = Regex("<style[^>]*>[\\s\\S]*?</style>")
    private val RE_IMG = Regex("<img[^>]*>")
    private val RE_BR = Regex("<br\\s*/?>")
    private val RE_BLOCK = Regex("<p\\s*/?>|</p>|<div[^>]*>|</div>")
    private val RE_LI_OPEN = Regex("<li\\s*>")
    private val RE_LI_CLOSE = Regex("</li>")
    private val RE_LINK = Regex("<a[^>]*href=\"[^\"]*\"[^>]*>(.*?)</a>")
    private val RE_TAG = Regex("<[^>]+>")
    private val RE_WHITESPACE = Regex("\\s{2,}")
    private val RE_FONT = Regex("<font[^>]*>|</font>")
    private val RE_BOLD = Regex("<b\\s*>|<strong\\s*>|</b>|</strong>")
    private val RE_ITALIC = Regex("<i\\s*>|<em\\s*>|</i>|</em>")
    private val RE_UNDERLINE = Regex("<u\\s*>|</u>")
    private val RE_SPAN = Regex("<span[^>]*>|</span>")

    fun stripHtmlForPreview(html: String, maxLength: Int = 300): String {
        return html
            .replace(RE_STYLE, "")
            .replace(RE_IMG, "")
            .replace(RE_BR, "\n")
            .replace(RE_BLOCK, "\n")
            .replace(RE_LI_OPEN, "• ")
            .replace(RE_LI_CLOSE, "\n")
            .replace(RE_LINK, "$1")
            .replace(RE_TAG, "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(RE_WHITESPACE, " ")
            .trim()
            .take(maxLength)
    }

    fun stripHtml(html: String): String {
        return html
            .replace(RE_STYLE, "")
            .replace(RE_IMG, "")
            .replace(RE_BR, "\n")
            .replace(RE_BLOCK, "\n")
            .replace(RE_LI_OPEN, "• ")
            .replace(RE_LI_CLOSE, "\n")
            .replace(RE_LINK, "$1")
            .replace(RE_FONT, "")
            .replace(RE_BOLD, "")
            .replace(RE_ITALIC, "")
            .replace(RE_UNDERLINE, "")
            .replace(RE_SPAN, "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(RE_TAG, "")
            .replace(RE_WHITESPACE, " ")
            .trim()
    }
}
