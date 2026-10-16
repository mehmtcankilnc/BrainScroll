package com.mehmtcan.brainscroll.game.crossword

import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrosswordBankTest {

    @Test
    fun everyWordIsTwoToEightLettersOfItsAlphabetWithAShortClueAndNoneIsTwice() {
        for (language in Language.entries) {
            val entries = CrosswordBank.entries(language)
            assertTrue(entries.size >= 1500, "$language has ${entries.size} words")
            for (entry in entries) {
                assertTrue(entry.word.length in 2..8, "${entry.word} is ${entry.word.length} letters")
                assertTrue(entry.word.all { it in language.alphabet }, "${entry.word} has a letter that is not in the alphabet")
                assertTrue(entry.clue.isNotBlank() && entry.clue.length <= 30, "${entry.word}: '${entry.clue}'")
            }
            assertEquals(entries.size, entries.map { it.word }.toSet().size, "$language has a word twice")
        }
    }

    @Test
    fun thereAreEnoughWordsOfEveryLengthToBuildFrom() {
        for (language in Language.entries) {
            val byLength = CrosswordBank.entries(language).groupBy { it.word.length }
            assertTrue((byLength[2]?.size ?: 0) >= 30, "$language two-letter words")
            assertTrue((byLength[3]?.size ?: 0) >= 150, "$language three-letter words")
            assertTrue((byLength[4]?.size ?: 0) >= 250, "$language four-letter words")
            assertTrue((byLength[5]?.size ?: 0) >= 600, "$language five-letter words")
            assertTrue((byLength[6]?.size ?: 0) >= 100, "$language six-letter words")
        }
    }

    @Test
    fun aClueIsFoundFromItsWordAndAnUnknownWordHasNone() {
        assertEquals("Bir binek hayvanı", CrosswordBank.clueOf(Language.TR, "AT"))
        assertEquals("Cardboard container", CrosswordBank.clueOf(Language.EN, "BOX"))
        assertNull(CrosswordBank.clueOf(Language.EN, "ZZZZ"))
        assertNotNull(CrosswordBank.clueOf(Language.TR, "İNEK"))
    }

    @Test
    fun theTwoLanguagesDoNotShareTheIndex() {
        assertNull(CrosswordBank.clueOf(Language.EN, "AYI"))
        assertNull(CrosswordBank.clueOf(Language.TR, "BOX"))
    }
}
