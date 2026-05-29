package io.github.gdict.ui.webview

class CambridgeEpdRenderer : DictionaryRenderer {
    override fun matches(definition: String): Boolean = definition.contains("cepd18.css")

    override fun transformHtml(html: String): String {
        var result = html
            .replace(Regex("""<img[^>]*src=["'][^"']*uk_sound\.png[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "<span class='uk-flag' title='UK pronunciation'></span>")
            .replace(Regex("""<img[^>]*src=["'][^"']*us_sound\.png[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "<span class='us-flag' title='US pronunciation'></span>")
            .replace(Regex("""<img[^>]*class=["'][^"']*speaker[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "")
            .replace(Regex("""<img[^>]*src=["'][^"']*(?:speaker|play|sound|volume|audio|pron)[^"']*\.(?:png|gif|svg)[^"']*["'][^>]*>""", RegexOption.IGNORE_CASE),
                "")
            .replace(Regex("</img>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<SEP[^>]*>\s*,\s*</SEP>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<hw([^>]*)>(.*?)</hw>""", RegexOption.DOT_MATCHES_ALL)) { m ->
                "<hw${m.groupValues[1]}>${m.groupValues[2].replace("|", "")}</hw>"
            }

        val arlPattern = Regex("""(<arl[^>]*>)(.*?)(</arl>)""", RegexOption.DOT_MATCHES_ALL)
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
                val soundfileMatch = Regex("""<soundfile[^>]*>(.*?)</soundfile>""", RegexOption.DOT_MATCHES_ALL).find(content)
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
                    "<div class=\"cepd-forms-section\"><div class=\"forms-header\">Word Forms (" + formsEntries.size + ")</div>" +
                    "<table class=\"forms-table\"><thead><tr><th>Word</th><th>IPA</th><th>Audio</th></tr></thead><tbody>" +
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
            .replace(Regex("""<inf[^>]*>(.*?)</inf>"""), "<span class=\"main-ipa\">\$1</span>")
            .replace(Regex("""<prongrp[^>]*>(.*?)</prongrp>""", RegexOption.DOT_MATCHES_ALL), "<div class=\"main-pronunciation\">\$1</div>")
            .replace(Regex("""<soundfile[^>]*>(.*?)</soundfile>""", RegexOption.DOT_MATCHES_ALL), "<div class=\"main-audio-btns\">\$1</div>")
            .replace(Regex("""<comment[^>]*>(.*?)</comment>"""), "<span class=\"main-comment\">\$1</span>")
    }

    override fun getCssBlock(): String = """
<style>
.cpepd { padding:0; }

.cpepd .arl { display:none !important; }

.cpepd .di-head {
  display:block;
  text-align:left;
  padding:16px 20px 8px;
  margin-bottom:0;
  border-bottom:1px solid #e0e0e0;
}
.cpepd .di-title {
  display:block;
}
.cpepd .di-title .hw {
  font-size:clamp(1.2em, 5vw, 3em);
  font-weight:700;
  color:var(--header);
  letter-spacing:-0.01em;
  overflow-wrap:break-word;
  word-break:break-word;
}

.cpepd .di-body { padding:0; }

.cpepd .sense-block { margin:0; padding:0; }

.cpepd .sense-head {
  display:block;
  padding:4px 20px 0;
}
.cpepd .sense-info {
  display:flex;
  align-items:center;
  justify-content:flex-start;
  gap:8px;
  background:none !important;
}

.cpepd .sense-body {
  display:block;
  padding:4px 20px 8px;
}

.cpepd .prongrp {
  display:flex;
  align-items:center;
  flex-wrap:wrap;
  gap:8px;
  margin:6px 0;
  padding:10px 14px;
  border:1px solid var(--border);
  border-radius:12px;
  background:var(--card-bg, rgba(0,0,0,0.02));
  max-width:100%;
  overflow:hidden;
}
.cpepd .pron {
  display:inline-flex;
  align-items:center;
  gap:6px;
}
.cpepd .ipa {
  font-family:'Lucida Sans Unicode','Arial Unicode MS','Noto Sans',sans-serif;
  font-size:clamp(1em, 4vw, 2.2em);
  color:#2563EB;
  font-style:normal;
  font-weight:500;
  overflow-wrap:break-word;
  word-break:break-word;
}
.cpepd .ussymbol {
  display:none;
}

.cpepd .inflection {
  display:flex;
  align-items:center;
  flex-wrap:wrap;
  gap:6px;
  margin:4px 0;
  padding:8px 14px;
  border:1px solid var(--border);
  border-radius:12px;
  font-size:clamp(0.9em, 3.5vw, 2em);
  color:var(--text);
  line-height:1.5;
  background:var(--card-bg, rgba(0,0,0,0.02));
  overflow-wrap:break-word;
  word-break:break-word;
}
.cpepd .inflection .inf {
  font-style:italic;
}
.cpepd .inflection .prongrp {
  display:inline;
  border:none;
  padding:0;
  margin:0;
  background:none;
}
.cpepd .inflection .ipa {
  font-size:1em;
  color:#2563EB;
}

.cpepd .uk-flag {
  display:inline-flex;
  align-items:center;
  justify-content:center;
  width:32px;
  height:32px;
  border-radius:50%;
  cursor:pointer;
  vertical-align:middle;
  background:#012169;
  position:relative;
  box-shadow:0 1px 3px rgba(0,0,0,0.15);
  overflow:hidden;
  flex-shrink:0;
}
.cpepd .uk-flag::before {
  content:"";
  position:absolute;
  inset:0;
  background:
    linear-gradient(to right,transparent 10px,#FFF 10px,#FFF 22px,transparent 22px),
    linear-gradient(to bottom,transparent 10px,#FFF 10px,#FFF 22px,transparent 22px);
}
.cpepd .uk-flag::after {
  content:"";
  position:absolute;
  inset:0;
  background:
    linear-gradient(to right,transparent 12px,#C8102E 12px,#C8102E 20px,transparent 20px),
    linear-gradient(to bottom,transparent 12px,#C8102E 12px,#C8102E 20px,transparent 20px);
}
.cpepd .us-flag {
  display:inline-flex;
  align-items:center;
  justify-content:center;
  width:32px;
  height:32px;
  border-radius:50%;
  cursor:pointer;
  vertical-align:middle;
  background:#FFF;
  position:relative;
  box-shadow:0 1px 3px rgba(0,0,0,0.15);
  overflow:hidden;
  flex-shrink:0;
}
.cpepd .us-flag::before {
  content:"";
  position:absolute;
  top:0;left:0;right:0;bottom:0;
  background:
    linear-gradient(to bottom,#B22234 0,#B22234 3px,transparent 3px,transparent 6px) repeat-y,
    linear-gradient(to bottom,transparent 6px,transparent 6.5px,#B22234 6.5px,#B22234 9.5px,transparent 9.5px,transparent 12.5px) repeat-y,
    linear-gradient(to bottom,transparent 12.5px,transparent 13px,#B22234 13px,#B22234 16px,transparent 16px,transparent 19px) repeat-y,
    linear-gradient(to bottom,transparent 19px,transparent 19.5px,#B22234 19.5px,#B22234 22.5px,transparent 22.5px,transparent 26px) repeat-y;
  background-color:#FFF;
}
.cpepd .us-flag::after {
  content:"";
  position:absolute;
  top:0;left:0;
  width:40%;
  height:55%;
  background:#3C3B6E;
  border-right:1px solid rgba(255,255,255,0.2);
  border-bottom:1px solid rgba(255,255,255,0.2);
}

.cpepd .soundfile {
  display:inline-flex;
  align-items:center;
  margin:0;
}
.cpepd .soundfile a {
  display:inline-flex;
  align-items:center;
  justify-content:center;
  text-decoration:none;
  cursor:pointer;
  transition:transform 0.15s, filter 0.15s;
  background:none !important;
  border:none !important;
  padding:0 !important;
}
.cpepd .soundfile a::before {
  content:"🔊";
  font-size:13px;
  margin-right:1px;
}
.cpepd .soundfile a:hover {
  transform:scale(1.1);
  filter:brightness(1.15);
}

.cpepd .panel {
  display:block;
  margin:6px 20px 10px;
  padding:8px 12px;
  border-radius:10px;
  background:var(--accent-bg);
  font-size:.88em;
  color:var(--text);
  line-height:1.5;
}
.cpepd .panel-body {
  display:block;
}

.cpepd .di-info { display:none; }
.cpepd sp { font-style:normal; }

.cpepd .cepd-main-entry { padding:0 20px; }
.cpepd .main-headword {
  font-size:clamp(1.2em, 5vw, 3em);
  font-weight:700;
  color:var(--header);
  margin:0 0 4px;
  overflow-wrap:break-word;
  word-break:break-word;
}
.cpepd .main-ipa {
  font-family:'Lucida Sans Unicode','Arial Unicode MS','Noto Sans',sans-serif;
  font-size:clamp(1em, 4vw, 2.2em);
  color:#2563EB;
  font-style:normal;
  font-weight:500;
}
.cpepd .main-pronunciation {
  display:flex;
  flex-wrap:wrap;
  align-items:center;
  gap:8px;
}
.cpepd .main-audio-btns {
  display:flex;
  flex-wrap:wrap;
  gap:6px;
}

.cpepd .cepd-forms-section {
  margin:12px 0;
  padding:0 20px;
  overflow-x:auto;
}
.cpepd .forms-header {
  font-size:0.9em;
  font-weight:600;
  color:var(--header);
  margin-bottom:6px;
}
.cpepd .forms-table {
  width:100%;
  border-collapse:collapse;
  font-size:clamp(0.85em, 3vw, 1.1em);
}
.cpepd .forms-table th,
.cpepd .forms-table td {
  padding:6px 8px;
  border-bottom:1px solid var(--border);
  text-align:left;
}
.cpepd .forms-table .form-ipa {
  font-family:'Lucida Sans Unicode','Arial Unicode MS','Noto Sans',sans-serif;
  color:#2563EB;
  overflow-wrap:break-word;
  word-break:break-word;
}
.cpepd .forms-table .current-word {
  background:var(--accent-bg);
}
</style>
"""

    override fun wrapBody(content: String): String = "<div class=\"cpepd\">$content</div>"
}
