package io.github.gdict.util

object HtmlUtils {

    fun stripHtmlForPreview(html: String, maxLength: Int = 300): String {
        return html
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>"), "")
            .replace(Regex("<img[^>]*>"), "")
            .replace(Regex("<br\\s*/?>"), "\n")
            .replace(Regex("<p\\s*/?>|</p>|<div[^>]*>|</div>"), "\n")
            .replace(Regex("<li\\s*>"), "• ")
            .replace(Regex("</li>"), "\n")
            .replace(Regex("<a[^>]*href=\"[^\"]*\"[^>]*>(.*?)</a>"), "$1")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&#39;"), "'")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
            .take(maxLength)
    }

    fun stripHtml(html: String): String {
        return html
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>"), "")
            .replace(Regex("<img[^>]*>"), "")
            .replace(Regex("<br\\s*/?>"), "\n")
            .replace(Regex("<p\\s*/?>|</p>|<div[^>]*>|</div>"), "\n")
            .replace(Regex("<li\\s*>"), "• ")
            .replace(Regex("</li>"), "\n")
            .replace(Regex("<a[^>]*href=\"[^\"]*\"[^>]*>(.*?)</a>"), "$1")
            .replace(Regex("<font[^>]*>|</font>"), "")
            .replace(Regex("<b\\s*>|<strong\\s*>|</b>|</strong>"), "")
            .replace(Regex("<i\\s*>|<em\\s*>|</i>|</em>"), "")
            .replace(Regex("<u\\s*>|</u>"), "")
            .replace(Regex("<span[^>]*>|</span>"), "")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&#39;"), "'")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }
}
