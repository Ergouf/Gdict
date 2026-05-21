package io.github.gdict

import androidx.compose.ui.graphics.Color
import io.github.gdict.ui.theme.GdictColors
import org.junit.Test
import org.junit.Assert.*

class ThemeTest {

    @Test
    fun testGdictColors() {
        val primary = GdictColors.Primary
        assertTrue(primary != Color.Unspecified)

        val primaryContainer = GdictColors.PrimaryContainer
        assertTrue(primaryContainer != Color.Unspecified)

        val secondary = GdictColors.Secondary
        assertTrue(secondary != Color.Unspecified)

        val tertiary = GdictColors.Tertiary
        assertTrue(tertiary != Color.Unspecified)

        val surface = GdictColors.Surface
        assertTrue(surface != Color.Unspecified)

        val outline = GdictColors.Outline
        assertTrue(outline != Color.Unspecified)
    }

    @Test
    fun testDifferentColorValues() {
        assertNotEquals(GdictColors.Primary, GdictColors.Secondary)
        assertNotEquals(GdictColors.Primary, GdictColors.Tertiary)
        assertNotEquals(GdictColors.PrimaryContainer, GdictColors.Primary)
    }
}
