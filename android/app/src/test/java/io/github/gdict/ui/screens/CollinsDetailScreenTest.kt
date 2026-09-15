package io.github.gdict.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollinsDetailScreenTest {
    @Test
    fun keepsInlineHeadwordTextInSenseDefinition() {
        val html = """◆◆◆◆◇<br><img src="header.png"><b>cook  cooks  cooking  cooked </b><font color="#669900">[VB]</font><br>When you <b>cook</b> a meal, you prepare food by heating it.<br><img src="bullet.png"><font color="#004080"><i>I have to go and cook the dinner.</i></font>"""

        val entry = parseCollinsEntry(html, "cook")
        val sense = entry.definitions.single()

        assertEquals("VB", sense.pos)
        assertTrue(sense.definition.contains("When you cook a meal"))
        assertEquals(listOf("I have to go and cook the dinner."), sense.examples)
    }
}
