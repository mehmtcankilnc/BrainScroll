package com.mehmtcan.brainscroll.game.wordle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanguageTest {

    @Test
    fun turkishUppercaseKeepsDottedAndDotlessISeparate() {
        assertEquals("İ", Language.TR.uppercase("i"))
        assertEquals("I", Language.TR.uppercase("ı"))
        assertEquals("ISIRGAN", Language.TR.uppercase("ısırgan"))
        assertEquals("İŞÇİ", Language.TR.uppercase("işçi"))
        assertEquals("ÇĞÖŞÜ", Language.TR.uppercase("çğöşü"))
    }

    @Test
    fun englishUppercaseMapsIToPlainI() {
        assertEquals("I", Language.EN.uppercase("i"))
        assertEquals("CRANE", Language.EN.uppercase("crane"))
    }

    @Test
    fun uppercaseIsIdempotent() {
        assertEquals("İŞÇİ", Language.TR.uppercase("İŞÇİ"))
    }

    @Test
    fun alphabetsRejectForeignLetters() {
        assertTrue(Language.TR.isValidWord("ÇAĞRI"))
        assertFalse(Language.TR.isValidWord("WATER")) // W is not in the Turkish alphabet
        assertFalse(Language.EN.isValidWord("ÇAĞRI"))
        assertFalse(Language.EN.isValidWord("AB1DE"))
    }
}
