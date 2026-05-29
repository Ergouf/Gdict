package io.github.gdict.ui.webview

import io.github.gdict.core.MdxParser

object HtmlContentBuilder {

    private val renderers = listOf(CambridgeEpdRenderer())

    private val BASE_CSS = """
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
.speaker-icon{cursor:pointer;display:inline-block;padding:2px 6px;margin:0 4px;background:var(--speaker-bg);border-radius:12px;color:var(--link);font-size:14px;}
.speaker-icon:hover{background:var(--speaker-hover);}
.soundfile{display:inline;margin:0 2px;}
.soundfile a{cursor:pointer;display:inline-block;padding:2px 8px;margin:0 4px;background:var(--speaker-bg);border-radius:12px;color:var(--link);text-decoration:none;font-size:13px;}
.soundfile a:hover{background:var(--speaker-hover);}
a[href^="sound://"]{cursor:pointer;display:inline-block;padding:2px 8px;margin:0 4px;background:var(--speaker-bg);border-radius:12px;color:var(--link);text-decoration:none;}
a[href^="sound://"]::before{content:"▶";margin-right:4px;font-size:11px;}
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

    private val THEME_JS = """
<script>
function setTheme(d){if(d){document.body.classList.add('dark')}else{document.body.classList.remove('dark')}}
function fixInlineStyles(){if(!document.body.classList.contains('dark'))return;var els=document.querySelectorAll('[style]');for(var i=0;i<els.length;i++){var s=els[i].style;var bg=s.backgroundColor||'';if(bg){var lb=bg.toLowerCase();if(lb.indexOf('#fff')>=0||lb.indexOf('#ffffff')>=0||lb.indexOf('white')>=0||lb.indexOf('rgb(255,')>=0){s.backgroundColor=''}}var bi=s.backgroundImage||'';if(bi&&bi.indexOf('url(')>=0&&bi.indexOf('data:')<0){s.backgroundImage='none'}var co=s.color||'';if(co){var lc=co.toLowerCase();if(lc.indexOf('#000')>=0||lc.indexOf('#000000')>=0||lc.indexOf('black')>=0||lc.indexOf('rgb(0,')>=0){s.color=''}}var b=s.background||'';if(b&&b.indexOf('#fff')>=0){s.background=''}}}
document.addEventListener('DOMContentLoaded',fixInlineStyles)
</script>
"""

    fun build(definition: String, css: String): String {
        val renderer = renderers.find { it.matches(definition) } ?: DefaultRenderer

        val transformedDef = renderer.transformHtml(definition).let { def ->
            var result = MdxParser.transformHtmlStatic(def)
            result = result.replace(Regex("""href=["']entry://([^"']+)["']""", RegexOption.IGNORE_CASE)) { match ->
                "href=\"entry://${match.groupValues[1]}\""
            }
            result = result.replace(Regex("""href=["']bword://([^"']+)["']""", RegexOption.IGNORE_CASE)) { match ->
                "href=\"bword://${match.groupValues[1]}\""
            }
            result = result.replace(Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)) { match ->
                "href=\"sound://${match.groupValues[1]}\""
            }
            if (css.isNotEmpty()) {
                result = result.replace(Regex("""<link[^>]*rel=["']stylesheet["'][^>]*>""", RegexOption.IGNORE_CASE), "")
            }
            result
        }

        val rendererCssBlock = renderer.getCssBlock()
        val dictCssBlock = if (css.isNotEmpty()) "<style>\n$css\n</style>\n" else ""
        val bodyContent = renderer.wrapBody(transformedDef)

        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
$BASE_CSS
</style>
$dictCssBlock
$rendererCssBlock
</head>
<body>
$bodyContent
$THEME_JS
</body>
</html>
        """.trimIndent()
    }
}
