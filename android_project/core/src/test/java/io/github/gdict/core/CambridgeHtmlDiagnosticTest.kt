package io.github.gdict.core

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class CambridgeHtmlDiagnosticTest {

    private var parserCambridge: MdxParser? = null
    private var parserCollins: MdxParser? = null

    private val cambridgePath = System.getProperty("cambridge.path")
        ?: """D:\workspace\Gdict\Cambridge_English_Pronouncing_Dictionary_18th.mdx"""
    private val collinsPath = System.getProperty("collins.path")
        ?: """D:\workspace\Gdict\Collins COBUILD Advanced English Dictionary Online.mdx"""

    @Before
    fun setUp() {
        println("======= Cambridge HTML 诊断 =======")
        val cf = File(cambridgePath)
        if (cf.exists()) {
            println("Cambridge: ${cf.name} (${cf.length()} bytes)")
            parserCambridge = MdxParser(cf)
        } else {
            println("WARNING: Cambridge not found at $cambridgePath")
        }

        val clf = File(collinsPath)
        if (clf.exists()) {
            println("Collins: ${clf.name} (${clf.length()} bytes)")
            parserCollins = MdxParser(clf)
        } else {
            println("WARNING: Collins not found at $collinsPath")
        }
    }

    @After
    fun tearDown() {
        parserCambridge?.close()
        parserCollins?.close()
    }

    @Test
    fun compareRawHtmlContent() {
        val pc = parserCambridge ?: return
        val pl = parserCollins ?: return

        println("\n=== Cambridge 'read' RAW ===")
        val camArticles = pc.readArticles("read")
        for ((word, def) in camArticles) {
            println("  word='$word' len=${def?.length}")
            if (def != null) {
                println("  --- FULL RAW (first 1000 chars) ---")
                println(def.take(1000))
                println("  --- END ---")
                println()
                println("  Has <br> tag: ${def.contains("<br", ignoreCase = true)}")
                println("  Has <b> tag: ${def.contains("<b>", ignoreCase = true)}")
                println("  Has <p> tag: ${def.contains("<p", ignoreCase = true)}")
                println("  Has <div> tag: ${def.contains("<div", ignoreCase = true)}")
                println("  Has <span> tag: ${def.contains("<span", ignoreCase = true)}")
                println("  Has <font> tag: ${def.contains("<font", ignoreCase = true)}")
                println("  Has <img> tag: ${def.contains("<img", ignoreCase = true)}")
                println("  Has <a> tag: ${def.contains("<a ", ignoreCase = true) || def.contains("<a>", ignoreCase = true)}")
                println("  Has <html> tag: ${def.contains("<html", ignoreCase = true)}")
                println("  Has any <: ${def.contains("<")}")
                println("  First 20 chars hex: ${def.take(20).toByteArray().joinToString(" ") { "%02x".format(it) }}")

                println("\n  === Transformed HTML ===")
                val transformed = pc.transformHtml(def)
                println(transformed.take(800))
            }
        }

        println("\n=== Collins 'read' RAW ===")
        val colArticles = pl.readArticles("read")
        for ((word, def) in colArticles) {
            println("  word='$word' len=${def?.length}")
            if (def != null) {
                println("  --- FULL RAW (first 500 chars) ---")
                println(def.take(500))
                println("  --- END ---")
                println()
                println("  Has <br> tag: ${def.contains("<br", ignoreCase = true)}")
                println("  Has <b> tag: ${def.contains("<b>", ignoreCase = true)}")
                println("  Has <img> tag: ${def.contains("<img", ignoreCase = true)}")
            }
        }

        println("\n=== Cambridge CSS ===")
        val camCss = pc.companionCss
        if (camCss.isNotEmpty()) {
            println("  CSS length: ${camCss.length} chars")
            println("  --- First 1500 chars of CSS ---")
            println(camCss.take(1500))
        } else {
            println("  NO CSS FOUND!")
        }

        println("\n=== Collins CSS ===")
        val colCss = pl.companionCss
        if (colCss.isNotEmpty()) {
            println("  CSS length: ${colCss.length} chars")
            println("  --- First 500 chars of CSS ---")
            println(colCss.take(500))
        } else {
            println("  NO CSS FOUND!")
        }

        println("\n======= 结论 =======")
        if (camArticles.isNotEmpty()) {
            val def = camArticles.values.first()
            if (def != null && def.contains("<")) {
                println("Cambridge 数据包含 HTML 标签 → WebView 应该能渲染")
                if (camCss.isNotEmpty()) {
                    println("Cambridge CSS 已加载 (${camCss.length} chars) → 样式应该生效")
                } else {
                    println("⚠️ Cambridge CSS 未加载 → 需要检查 .mdd 文件是否正确导入")
                }
            } else {
                println("Cambridge 数据不含 HTML 标签 → 纯文本词典，需要后处理添加格式")
            }
        }
    }
}
