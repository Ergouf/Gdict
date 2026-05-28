package io.github.gdict.ui.webview

class CambridgeEpdRenderer : DictionaryRenderer {
    override fun matches(definition: String): Boolean = definition.contains("cepd18.css")

    override fun transformHtml(html: String): String {
        return html
            .replace(Regex("""<img[^>]*src=["'][^"']*uk_sound\.png[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "<span class='uk-flag'>UK</span>")
            .replace(Regex("""<img[^>]*src=["'][^"']*us_sound\.png[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "<span class='us-flag'>US</span>")
            .replace(Regex("""<img[^>]*class=["'][^"']*speaker[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "<span class='speaker-icon'>▶</span>")
            .replace(Regex("""<img[^>]*src=["'][^"']*(?:speaker|play|sound|volume|audio|pron)[^"']*\.(?:png|gif|svg)[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "<span class='speaker-icon'>▶</span>")
            .replace(Regex("</img>", RegexOption.IGNORE_CASE), "")
    }

    override fun getCssBlock(): String = """
<style>
.cpepd .arl { display:block;margin:4px 0;padding:6px 8px;border-bottom:1px solid var(--border); }
.cpepd .hit { display:block;margin:2px 0;padding:1px 0; }
.cpepd .hit+.hit { border-top:1px solid var(--border-light);padding-top:4px; }
.cpepd .results { display:inline; }
.cpepd .base { display:inline;font-size:.95em; }
.cpepd .hw { font-weight:bold;font-size:1.1em;color:var(--header);display:inline; }
.cpepd .inf { color:var(--phon);font-style:italic;font-size:.9em; }
.cpepd .comment { color:var(--subtle);font-style:italic;font-size:.85em; }
.cpepd .forms { display:block;margin:1px 0 1px 14px; }
.cpepd .inflections { display:inline; }
.cpepd .inflections .base { color:var(--subtle);font-size:.9em; }
.cpepd .pron,.cpepd .ipa { color:var(--phon);font-family:'Lucida Sans Unicode','Arial Unicode MS',sans-serif;display:inline;font-size:.9em; }
.cpepd .prongrp { display:block;margin:2px 0;padding:1px 0; }
.cpepd .ussymbol { display:inline-block;padding:0 3px;margin:0 2px;font-size:.75em;font-weight:600; }
.cpepd .uk-flag { display:inline-block;padding:1px 4px;margin:0 2px;background:var(--flag-uk);color:#fff;border-radius:3px;font-size:9px;font-weight:bold;vertical-align:middle;letter-spacing:.5px; }
.cpepd .us-flag { display:inline-block;padding:1px 4px;margin:0 2px;background:var(--flag-us);color:#fff;border-radius:3px;font-size:9px;font-weight:bold;vertical-align:middle;letter-spacing:.5px; }
.cpepd .soundfile { display:inline;margin:0 2px; }
.cpepd .soundfile a { display:inline-block;vertical-align:middle;text-decoration:none; }
.cpepd .soundfile img { display:none; }
.cpepd .speaker-icon { display:inline-block;vertical-align:middle;padding:1px 5px;margin:0 2px;background:var(--speaker-hover);border-radius:12px;color:var(--link);font-size:13px;cursor:pointer; }
.cpepd .speaker-icon:hover { background:var(--speaker-hover);filter:brightness(1.2); }
.cpepd a[href^="sound://"] { text-decoration:none;display:inline-block;vertical-align:middle;padding:1px 6px;margin:0 2px;background:var(--speaker-hover);border-radius:12px;color:var(--link);font-size:12px; }
.cpepd a[href^="sound://"]::before { content:"▶";margin-right:3px;font-size:10px; }
.cpepd a[href^="sound://"]:hover { background:var(--speaker-hover);filter:brightness(1.2); }
.cpepd .capvar { color:var(--subtle);font-style:italic;font-size:.85em; }
.cpepd .inflection { display:block;margin:1px 0 1px 14px;font-size:.9em; }
</style>
"""

    override fun wrapBody(content: String): String = "<div class=\"cpepd\">$content</div>"
}
