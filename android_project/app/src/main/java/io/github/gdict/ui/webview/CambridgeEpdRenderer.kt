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
.cpepd .speaker-icon { display:inline-block;vertical-align:middle;padding:2px 6px;margin:0 2px;background:var(--speaker-hover);border-radius:12px;color:var(--link);font-size:14px;cursor:pointer; }
.cpepd .speaker-icon:hover { background:var(--speaker-hover);filter:brightness(1.2); }
.cpepd a[href^="sound://"] { text-decoration:none;display:inline-block;vertical-align:middle;padding:2px 8px;margin:0 2px;background:var(--speaker-hover);border-radius:12px;color:var(--link);font-size:13px; }
.cpepd a[href^="sound://"]::before { content:"▶";margin-right:4px;font-size:11px; }
.cpepd a[href^="sound://"]:hover { background:var(--speaker-hover);filter:brightness(1.2); }
.cpepd .capvar { color:var(--subtle);font-style:italic;font-size:.9em; }
.cpepd .inflection { display:block;margin:2px 0 2px 16px; }
</style>
"""

    override fun wrapBody(content: String): String = "<div class=\"cpepd\">$content</div>"
}
