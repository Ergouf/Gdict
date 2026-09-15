package io.github.gdict.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class PronunciationDetailScreenTest {
    @Test
    fun keepsEachEpdSoundWithItsRegion() {
        val html = """
            <span class="di-head"><hw>cook</hw></span>
            <span class="di-body"><prongrp>
                <soundfile><a href="sound://UKCOOK.mp3"><img src="uk_sound.png"></a></soundfile>
                <soundfile><a href="sound://COOK.mp3"><img src="us_sound.png"></a></soundfile>
                <pron><ipa>kʊk</ipa></pron>
            </prongrp></span>
            <span class="di-head"><hw>cookbook</hw></span>
            <span class="di-body"><prongrp>
                <soundfile><a href="sound://UKCOOKBOOK.mp3"><img src="uk_sound.png"></a></soundfile>
                <soundfile><a href="sound://COOKBOOK.mp3"><img src="us_sound.png"></a></soundfile>
                <pron><ipa>ˈkʊkbʊk</ipa></pron>
            </prongrp></span>
        """.trimIndent()

        val pronunciations = parsePronunciations(html)

        assertEquals(2, pronunciations.size)
        assertEquals("UK", pronunciations[0].region)
        assertEquals("UKCOOK.mp3", pronunciations[0].audioPath)
        assertEquals("kʊk", pronunciations[0].ipa)
        assertEquals("US", pronunciations[1].region)
        assertEquals("COOK.mp3", pronunciations[1].audioPath)
        assertEquals("kʊk", pronunciations[1].ipa)
    }
}
