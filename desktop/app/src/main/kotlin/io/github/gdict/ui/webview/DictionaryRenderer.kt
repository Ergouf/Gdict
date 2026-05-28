package io.github.gdict.ui.webview

interface DictionaryRenderer {
    fun matches(definition: String): Boolean
    fun transformHtml(html: String): String
    fun getCssBlock(): String
    fun wrapBody(content: String): String
}

object DefaultRenderer : DictionaryRenderer {
    override fun matches(definition: String): Boolean = true
    override fun transformHtml(html: String): String = html
    override fun getCssBlock(): String = ""
    override fun wrapBody(content: String): String = content
}
