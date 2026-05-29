package io.github.gdict.ui.webview

class CambridgeEpdRenderer : DictionaryRenderer {
    override fun matches(definition: String): Boolean = definition.contains("cepd18.css")

    override fun transformHtml(html: String): String {
        var result = html
            .replace(Regex("""<img[^>]*src=["'][^"']*uk_sound\.png[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "<span class='uk-flag' role='img' aria-label='UK'>🇬🇧</span>")
            .replace(Regex("""<img[^>]*src=["'][^"']*us_sound\.png[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "<span class='us-flag' role='img' aria-label='US'>🇺🇸</span>")
            .replace(Regex("""<img[^>]*class=["'][^"']*speaker[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "")
            .replace(Regex("""<img[^>]*src=["'][^"']*(?:speaker|play|sound|volume|audio|pron)[^"']*\.(?:png|gif|svg)[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "")
            .replace(Regex("</img>", RegexOption.IGNORE_CASE), "")

        val arlPattern = Regex("""(<div[^>]*class="arl"[^>]*>)(.*?)(</div>)""", RegexOption.DOT_MATCHES_ALL)
        val arlMatches = arlPattern.findAll(result).toList()

        if (arlMatches.size > 1) {
            val lastMatch = arlMatches.last()
            val mainEntryContent = lastMatch.groupValues[2]
            val formsEntries = arlMatches.dropLast(1)

            val mainEntryProcessed = processMainEntry(mainEntryContent)

            val formsRows = formsEntries.mapIndexed { index, match ->
                val content = match.groupValues[2]
                val hwMatch = Regex("""<hw[^>]*>(.*?)</hw>""").find(content)
                val word = hwMatch?.groupValues?.get(1)?.trim() ?: ""
                val infMatch = Regex("""<inf[^>]*>(.*?)</inf>""").find(content)
                val ipa = infMatch?.groupValues?.get(1)?.trim() ?: ""
                val soundfileMatch = Regex("""<div[^>]*class="soundfile"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL).find(content)
                val audioHtml = soundfileMatch?.groupValues?.get(1) ?: ""

                """<tr class="form-row${if (index == 0) " current-word" else ""}">
  <td class="form-word"><a href="entry://$word">$word</a></td>
  <td class="form-ipa">$ipa</td>
  <td class="form-audio">$audioHtml</td>
</tr>"""
            }.joinToString("\n")

            val beforeMain = result.substring(0, lastMatch.range.first)
            val afterMain = result.substring(lastMatch.range.last + 1)

            result = beforeMain +
                    "<div class=\"cepd-main-entry\">" + mainEntryProcessed + "</div>" +
                    "<div class=\"cepd-forms-section\"><div class=\"forms-header\">词形变化 (" + formsEntries.size + ")</div>" +
                    "<table class=\"forms-table\"><thead><tr><th>单词</th><th>音标</th><th>发音</th></tr></thead><tbody>" +
                    formsRows + "</tbody></table></div>" + afterMain
        } else if (arlMatches.size == 1) {
            val match = arlMatches[0]
            val content = match.groupValues[2]
            val processed = processMainEntry(content)
            result = result.substring(0, match.range.first) +
                    "<div class=\"cepd-main-entry\">" + processed + "</div>" +
                    result.substring(match.range.last + 1)
        }

        return result
    }

    private fun processMainEntry(content: String): String {
        return content
            .replace(Regex("""^\s*<hw[^>]*>(.*?)</hw>"""), "<h1 class=\"main-headword\">\$1</h1>")
            .replace(Regex("""<inf[^>]*>(.*?)</inf>""), "<span class=\"main-ipa\">\$1</span>")
            .replace(Regex("""<div[^>]*class="prongrp"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL), "<div class=\"main-pronunciation\">\$1</div>")
            .replace(Regex("""<div[^>]*class="soundfile"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL), "<div class=\"main-audio-btns\">\$1</div>")
            .replace(Regex("""<comment[^>]*>(.*?)</comment>""), "<span class=\"main-comment\">\$1</span>")
    }

    override fun getCssBlock(): String = """
<style>
.cpepd { padding:0; }
.cpepd-main-entry {
  display:block;
  text-align:center;
  padding:24px 16px 20px;
  margin-bottom:0;
  border-bottom:2px solid var(--di-head-border);
}
.cpepd .main-headword {
  font-size:1.8em;
  font-weight:700;
  color:var(--header);
  margin:0 0 6px;
  letter-spacing:-0.02em;
}
.cpepd .main-ipa {
  font-family:'Lucida Sans Unicode','Arial Unicode MS','Noto Sans',sans-serif;
  font-size:1.25em;
  color:var(--phon);
  font-style:normal;
  display:block;
  margin:4px 0 10px;
}
.cpepd .main-pronunciation {
  display:flex;
  justify-content:center;
  align-items:center;
  gap:4px;
  flex-wrap:wrap;
  margin:8px 0 4px;
}
.cpepd .main-audio-btns {
  display:flex;
  justify-content:center;
  align-items:center;
  gap:12px;
  margin:10px 0 0;
}
.cpepd .main-audio-btns a[href^="sound://"],
.cpepd .main-audio-btns .uk-flag,
.cpepd .main-audio-btns .us-flag {
  display:inline-flex;
  align-items:center;
  justify-content:center;
  width:44px;
  height:36px;
  border-radius:10px;
  cursor:pointer;
  text-decoration:none;
  transition:transform 0.15s, filter 0.15s;
  font-size:22px;
  line-height:1;
  background:var(--accent-bg);
  padding:4px 8px;
  width:auto;
  min-width:44px;
}
.cpepd .main-audio-btns a[href^="sound://"]:hover,
.cpepd .main-audio-btns .uk-flag:hover,
.cpepd .main-audio-btns .us-flag:hover {
  transform:scale(1.1);
  filter:brightness(1.15);
}
.cpepd .main-comment {
  display:block;
  color:var(--subtle);
  font-size:.88em;
  font-style:italic;
  margin-top:8px;
}

.cpepd-forms-section {
  display:block;
  padding:16px 4px 8px;
}
.cpepd .forms-header {
  font-size:.9em;
  font-weight:600;
  color:var(--subtle);
  text-transform:uppercase;
  letter-spacing:0.05em;
  margin:0 0 10px 8px;
  padding-left:8px;
  border-left:3px solid var(--header);
}
.cpepd .forms-table {
  width:100%;
  border-collapse:collapse;
  font-size:.92em;
}
.cpepd .forms-table thead th {
  background:var(--accent-bg);
  color:var(--header);
  font-weight:600;
  font-size:.82em;
  text-transform:uppercase;
  letter-spacing:0.04em;
  padding:8px 12px;
  text-align:left;
  border:none;
}
.cpepd .forms-table tbody tr {
  border-bottom:1px solid var(--border-light);
  transition:background 0.12s;
}
.cpepd .forms-table tbody tr:hover {
  background:var(--accent-bg);
}
.cpepd .forms-table tbody tr.current-word {
  background:rgba(78,205,196,0.08);
}
.cpepd .forms-table td {
  padding:10px 12px;
  vertical-align:middle;
}
.cpepd .form-word {
  font-weight:600;
  color:var(--header);
  min-width:100px;
}
.cpepd .form-word a {
  color:var(--link);
  text-decoration:none;
  font-weight:600;
}
.cpepd .form-word a:hover {
  text-decoration:underline;
}
.cpepd .form-ipa {
  font-family:'Lucida Sans Unicode','Arial Unicode MS','Noto Sans',sans-serif;
  color:var(--phon);
  font-size:.95em;
}
.cpepd .form-audio {
  white-space:nowrap;
  text-align:center;
}
.cpepd .form-audio .uk-flag,
.cpepd .form-audio .us-flag,
.cpepd .form-audio a[href^="sound://"] {
  display:inline-flex;
  align-items:center;
  justify-content:center;
  width:32px;
  height:28px;
  border-radius:7px;
  font-size:16px;
  cursor:pointer;
  background:var(--accent-bg);
  text-decoration:none;
  margin:0 2px;
  transition:transform 0.15s;
}
.cpepd .form-audio .uk-flag:hover,
.cpepd .form-audio .us-flag:hover,
.cpepd .form-audio a[href^="sound://"]:hover {
  transform:scale(1.15);
}

.cpepd .arl,
.cpepd .hit,
.cpepd .results,
.cpepd .base,
.cpepd .hw,
.cpepd .inf,
.cpepd .comment,
.cpepd .forms,
.cpepd .inflections,
.cpepd .pron,
.cpepd .ipa,
.cpepd .prongrp,
.cpepd .ussymbol,
.cpepd .soundfile,
.cpepd .capvar,
.cpepd .inflection { display:none !important; }
</style>
"""

    override fun wrapBody(content: String): String = "<div class=\"cpepd\">$content</div>"
}