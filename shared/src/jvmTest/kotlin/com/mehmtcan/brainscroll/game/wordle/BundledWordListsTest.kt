package com.mehmtcan.brainscroll.game.wordle

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Checks the generated word files in the repo. Lives in jvmTest because it reads plain files
 * (Gradle runs tests from the `shared` module directory); the loader itself uses Compose resources.
 */
class BundledWordListsTest {

    private class Files(val language: Language, val code: String, val minValid: Int, val minAnswers: Int) {
        private fun read(path: String) = WordList.parseLines(File(path).readText())
        val valid = read("src/commonMain/composeResources/files/words/${code}_valid.txt")
        val answers = read("src/commonMain/composeResources/files/words/${code}_answers.txt")
    }

    private val en = Files(Language.EN, "en", minValid = 5_000, minAnswers = 1_000)
    private val tr = Files(Language.TR, "tr", minValid = 20_000, minAnswers = 1_000)
    private val all = listOf(en, tr)

    @Test
    fun filesAreSizedAsExpected() {
        for (f in all) {
            assertTrue(f.valid.size > f.minValid, "${f.code} valid has ${f.valid.size}")
            assertTrue(f.answers.size > f.minAnswers, "${f.code} answers has ${f.answers.size}")
        }
    }

    @Test
    fun everyWordIsFiveLettersOfItsAlphabetInLowercase() {
        for (f in all) {
            val bad = (f.valid + f.answers).filterNot {
                it.length == 5 && it == it.lowercase() && f.language.isValidWord(f.language.uppercase(it))
            }
            assertEquals(emptyList(), bad, f.code)
        }
    }

    @Test
    fun noDuplicates() {
        for (f in all) {
            assertEquals(f.valid.size, f.valid.toSet().size, f.code)
            assertEquals(f.answers.size, f.answers.toSet().size, f.code)
        }
    }

    @Test
    fun everyAnswerIsAValidGuess() {
        for (f in all) {
            val valid = f.valid.toSet()
            assertEquals(emptyList(), f.answers.filterNot { it in valid }, f.code)
        }
    }

    @Test
    fun blockedEnglishWordsAreNotAnswers() {
        val blocklist = WordList.parseLines(File("../tools/wordlists/blocklist_en.txt").readText())
        assertTrue(blocklist.isNotEmpty())
        assertEquals(emptyList(), blocklist.filter { it in en.answers.toSet() })
        assertFalse("bitch" in en.answers)
    }

    @Test
    fun bundledFilesBuildWordLists() {
        // Building a WordList also checks the "every answer is guessable" invariant after case mapping.
        for (f in all) WordList(f.language, 5, f.valid, f.answers)
        assertTrue("crane" in WordList(Language.EN, 5, en.valid, en.answers))
        assertFalse("zzzzz" in WordList(Language.EN, 5, en.valid, en.answers))
    }

    @Test
    fun turkishLookupWorksWithDottedAndDotlessI() {
        val list = WordList(Language.TR, 5, tr.valid, tr.answers)
        assertTrue("çiçek" in list)
        assertTrue("ÇİÇEK" in list)
        assertTrue("sıcak" in list) // ı must stay ı, not turn into i
        assertTrue("SICAK" in list)
        assertFalse("sicak" in list) // "SİCAK" is a different word from "SICAK"
    }
}
