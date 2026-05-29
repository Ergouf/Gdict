package io.github.gdict.ui.webview

import io.github.gdict.core.MdxParser

object HtmlContentBuilder {

    private val renderers = listOf(CambridgeEpdRenderer())

    private val BASE_CSS = """
:root {
  --bg:#FFFFFF;--text:#424242;--header:#2C4A6E;--link:#63BD04;--phon:#1565C0;
  --border:rgba(128,128,128,0.15);--border-light:rgba(128,128,128,0.08);
  --accent-bg:rgba(99,189,4,0.1);--speaker-bg:rgba(99,189,4,0.15);
  --speaker-hover:rgba(99,189,4,0.3);--def-border:rgba(99,189,4,0.3);
  --example:#666;--subtle:#9E9E9E;
  --flag-uk:#012169;--flag-us:#B31942;
  --tag-bg:rgba(244,67,54,0.1);--tag-color:#D32F2F;
  --pos-bg:rgba(44,74,110,0.1);--table-border:#E0E0E0;
  --uk-badge-bg:rgba(33,150,243,0.1);--uk-badge-color:#1976D2;
  --di-head-border:var(--header);
}
body.dark {
  --bg:#1A1C17;--text:#E1E4DA;--header:#8BB8E8;--link:#7ED321;--phon:#A8D8EA;
  --border:rgba(255,255,255,0.1);--border-light:rgba(255,255,255,0.06);
  --accent-bg:rgba(126,211,33,0.12);--speaker-bg:rgba(126,211,33,0.2);
  --speaker-hover:rgba(126,211,33,0.35);--def-border:rgba(126,211,33,0.4);
  --example:#A0A0A0;--subtle:#888;
  --flag-uk:#1A3A8A;--flag-us:#8B1A2B;
  --tag-bg:rgba(244,67,54,0.15);--tag-color:#EF9A9A;
  --pos-bg:rgba(139,184,232,0.12);--table-border:rgba(255,255,255,0.12);
  --uk-badge-bg:rgba(100,181,246,0.12);--uk-badge-color:#64B5F6;
  --di-head-border:var(--header);
}

body{font-family:-apple-system,'Segoe UI',Roboto,sans-serif;font-size:14px;line-height:1.5;color:var(--text);background:var(--bg);margin:0;padding:6px 10px;word-wrap:break-word;}
h1,h2,h3{color:var(--header);font-weight:600;margin:10px 0 6px;font-size:1.1em;}
a{color:var(--link);text-decoration:none;}
img{max-width:100%;height:auto;}
table{border-collapse:collapse;width:100%;}
td,th{border:1px solid var(--table-border);padding:6px 8px;}
hw{font-weight:bold;color:var(--header);}
inf{font-style:italic;}
.arl{display:block;margin-bottom:8px;padding-bottom:6px;border-bottom:1px solid var(--border);}
.hit{display:block;margin:1px 0;padding:1px 0;}
.comment{font-style:italic;color:var(--subtle);font-size:.9em;}
.capvar{color:var(--subtle);font-style:italic;font-size:.9em;}
.phon,.pron,.ipa{color:var(--phon);font-family:'Lucida Sans Unicode','Arial Unicode MS',sans-serif;font-size:.95em;}
.speaker,.sound,.audio-play{cursor:pointer;display:inline-block;padding:1px 5px;margin:0 3px;background:var(--speaker-bg);border-radius:12px;color:var(--link);font-size:13px;}
.speaker:hover,.sound:hover,.audio-play:hover{background:var(--speaker-hover);}
.speaker-icon{cursor:pointer;display:inline-block;padding:1px 5px;margin:0 3px;background:var(--speaker-bg);border-radius:12px;color:var(--link);font-size:13px;}
.speaker-icon:hover{background:var(--speaker-hover);}
.soundfile{display:inline;margin:0 2px;}
.soundfile a{cursor:pointer;display:inline-block;padding:1px 6px;margin:0 3px;background:var(--speaker-bg);border-radius:12px;color:var(--link);text-decoration:none;font-size:12px;}
.soundfile a:hover{background:var(--speaker-hover);}
a[href^="sound://"]{cursor:pointer;display:inline-block;padding:1px 6px;margin:0 3px;background:var(--speaker-bg);border-radius:12px;color:var(--link);text-decoration:none;font-size:13px;}
a[href^="sound://"]::before{content:"▶";margin-right:3px;font-size:10px;}
a[href^="sound://"]:hover{background:var(--speaker-hover);}
.pos,.pos2{display:inline-block;padding:1px 6px;margin:1px 3px;background:var(--pos-bg);border-radius:4px;font-style:italic;color:var(--header);font-size:.85em;}
.bre,.ame,.gb,.us{display:inline-block;padding:1px 5px;margin:0 3px;border-radius:4px;font-size:.75em;font-weight:600;}
.bre,.gb{background:var(--uk-badge-bg);color:var(--uk-badge-color);}
.ame,.us{background:var(--tag-bg);color:var(--tag-color);}
.ussymbol{display:inline-block;padding:1px 5px;margin:0 3px;border-radius:4px;font-size:.75em;font-weight:600;background:var(--tag-bg);color:var(--tag-color);}
.label,.sense{margin:3px 0;padding:1px 0;}
.definition,.def{margin:3px 0 6px;padding-left:10px;border-left:3px solid var(--def-border);font-size:.95em;}
.example,.ex{color:var(--example);font-style:italic;margin:3px 0 3px 14px;font-size:.9em;}
.di-head{display:block;margin:12px 0 6px;padding-bottom:3px;border-bottom:2px solid var(--di-head-border);}
.di-title{display:block;font-size:1.2em;font-weight:700;}
.di-info{display:inline;font-size:.95em;}
.di-body{display:block;margin:3px 0;}
.sense-block{display:block;margin:6px 0;padding:3px 0;}
.sense-head{display:block;margin:4px 0 1px;}
.sense-body{display:block;margin:1px 0;padding-left:6px;}
.sense-info{display:inline;font-size:.95em;}
.prongrp{display:block;margin:1px 0;}
.inflection{display:block;margin:3px 0;padding-left:10px;}
.INFLX{display:inline;margin:0 3px;color:var(--subtle);font-size:.9em;}
.base{display:inline;font-size:1em;}
.results{display:inline;}
.forms{display:inline;}
.inflections{display:inline;}
.hw{font-size:1.05em;color:var(--header);}
.inf{font-style:italic;font-size:.95em;}
.cm{font-weight:600;}
::-webkit-scrollbar{width:6px;height:6px;}
::-webkit-scrollbar-track{background:transparent;}
::-webkit-scrollbar-thumb{background:rgba(128,128,128,0.35);border-radius:3px;}
::-webkit-scrollbar-thumb:hover{background:rgba(128,128,128,0.55);}
::-webkit-scrollbar-corner{background:transparent;}
body.dark ::-webkit-scrollbar-thumb{background:rgba(255,255,255,0.2);}
body.dark ::-webkit-scrollbar-thumb:hover{background:rgba(255,255,255,0.35);}
"""

    private val THEME_JS = """
<script>
function setTheme(d){if(d){document.body.classList.add('dark')}else{document.body.classList.remove('dark')}}
function fixInlineStyles(){if(!document.body.classList.contains('dark'))return;var els=document.querySelectorAll('[style]');for(var i=0;i<els.length;i++){var s=els[i].style;var bg=s.backgroundColor||'';if(bg){var lb=bg.toLowerCase();if(lb.indexOf('#fff')>=0||lb.indexOf('#ffffff')>=0||lb.indexOf('white')>=0||lb.indexOf('rgb(255,')>=0){s.backgroundColor=''}}var bi=s.backgroundImage||'';if(bi&&bi.indexOf('url(')>=0&&bi.indexOf('data:')<0){s.backgroundImage='none'}var co=s.color||'';if(co){var lc=co.toLowerCase();if(lc.indexOf('#000')>=0||lc.indexOf('#000000')>=0||lc.indexOf('black')>=0||lc.indexOf('rgb(0,')>=0){s.color=''}}var b=s.background||'';if(b&&b.indexOf('#fff')>=0){s.background=''}}}
document.addEventListener('DOMContentLoaded',fixInlineStyles)
</script>
"""

    fun build(definition: String, css: String, darkMode: Boolean = false, resourcePrefix: String = "mdxres://"): String {
        val renderer = renderers.find { it.matches(definition) } ?: DefaultRenderer

        val cleanDefinition = definition.replace(Regex("[\\x00-\\x1f\\x7f]"), "")

        val transformedDef = renderer.transformHtml(cleanDefinition).let { def ->
            var result = MdxParser.transformHtmlStatic(def)
            result = result.replace(Regex("""href=["']entry://([^"']+)["']""", RegexOption.IGNORE_CASE)) { match ->
                val entry = match.groupValues[1]
                "href=\"entry://$entry\""
            }
            result = result.replace(Regex("""href=["']bword://([^"']+)["']""", RegexOption.IGNORE_CASE)) { match ->
                val entry = match.groupValues[1]
                "href=\"bword://$entry\""
            }
            result = result.replace(Regex("""href=["']sound://([^"']+)["']""", RegexOption.IGNORE_CASE)) { match ->
                val soundPath = match.groupValues[1]
                "href=\"sound://$soundPath\""
            }
            if (css.isNotEmpty()) {
                result = result.replace(Regex("""<link[^>]*rel=["']stylesheet["'][^>]*>""", RegexOption.IGNORE_CASE), "")
            }
            result = result.replace(Regex("""(?i)(src|background|poster)=["']([^"']*(?:\.png|\.jpg|\.jpeg|\.gif|\.svg|\.webp|\.bmp|\.ico))["']""")) { match ->
                val attr = match.groupValues[1]
                val path = match.groupValues[2]
                "$attr=\"${resourcePrefix}${path}\""
            }
            result = result.replace(Regex("""(?i)url\(\s*['"]?([^"')]*(?:\.png|\.jpg|\.jpeg|\.gif|\.svg|\.webp|\.bmp|\.ttf|\.woff|\.woff2|\.eot|\.otf)[^"']*)['"]?\s*\)""")) { match ->
                val path = match.groupValues[1].trim()
                "url(${resourcePrefix}${path})"
            }
            result
        }

        val rendererCssBlock = renderer.getCssBlock()
        val dictCssBlock = if (css.isNotEmpty()) "<style>\n$css\n</style>\n" else ""
        val bodyContent = renderer.wrapBody(transformedDef)

        val bodyClass = if (darkMode) "dark" else ""

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
<body class="$bodyClass">
$bodyContent
$THEME_JS
<script>fixInlineStyles();</script>
</body>
</html>
        """.trimIndent()
    }
}
