package com.mehmtcan.brainscroll.game.wordle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordleRoundTest {

    private val en = WordList(Language.EN, 5, valid = listOf("crane", "slate", "ghost"), answers = listOf("crane"))
    private val tr = WordList(Language.TR, 5, valid = listOf("kırık", "çiçek", "kitap"), answers = listOf("çiçek"))

    private fun round(list: WordList = en, answer: String = "crane") =
        WordleRound(0, WordleGame(list, answer), list)

    private fun WordleRound.typeAll(word: String) = word.forEach { type(it) }

    @Test
    fun typingBuildsUppercaseInputUpToWordLength() {
        val r = round()
        r.typeAll("cranes")
        assertEquals("CRANE", r.snapshot().input)
    }

    @Test
    fun lettersOutsideTheAlphabetAreIgnored() {
        val r = round()
        r.type('1')
        r.type(' ')
        r.type('ç') // not an English letter
        assertEquals("", r.snapshot().input)
    }

    @Test
    fun turkishTypingKeepsDottedAndDotlessIApart() {
        val r = round(tr, "çiçek")
        r.type('i')
        r.type('ı')
        assertEquals("İI", r.snapshot().input)
    }

    @Test
    fun backspaceRemovesTheLastLetter() {
        val r = round()
        r.typeAll("cr")
        r.backspace()
        assertEquals("C", r.snapshot().input)
        r.backspace()
        r.backspace() // nothing left, must not crash
        assertEquals("", r.snapshot().input)
    }

    @Test
    fun submittingAShortWordKeepsLettersAndReportsTheError() {
        val r = round()
        r.typeAll("cra")
        r.submit()
        val s = r.snapshot()
        assertEquals(GuessError.WrongLength, s.error)
        assertEquals(1, s.errorTick)
        assertEquals("CRA", s.input)
        assertTrue(s.rows.isEmpty())
    }

    @Test
    fun theSameErrorTwiceIncreasesTheTick() {
        val r = round()
        r.submit()
        r.submit()
        assertEquals(2, r.snapshot().errorTick)
    }

    @Test
    fun typingClearsTheError() {
        val r = round()
        r.submit()
        r.type('c')
        assertNull(r.snapshot().error)
    }

    @Test
    fun wordNotInDictionaryIsRejected() {
        val r = round()
        r.typeAll("zzzzz")
        r.submit()
        assertEquals(GuessError.NotInDictionary, r.snapshot().error)
    }

    @Test
    fun acceptedGuessClearsInputAndAddsARow() {
        val r = round()
        r.typeAll("slate")
        r.submit()
        val s = r.snapshot()
        assertEquals("", s.input)
        assertEquals(1, s.rows.size)
        assertNull(s.error)
        assertEquals(WordleStatus.Playing, s.status)
    }

    @Test
    fun winningFinishesTheRoundAndRevealsTheAnswer() {
        val r = round()
        r.typeAll("crane")
        r.submit()
        val s = r.snapshot()
        assertTrue(s.isFinished)
        assertEquals("CRANE", s.answer)
        r.type('a') // ignored after the end
        assertEquals("", r.snapshot().input)
    }

    @Test
    fun keyStatesKeepTheBestResultPerLetter() {
        val r = round()
        r.typeAll("slate") // S L absent, A correct, T absent, E correct (answer CRANE)
        r.submit()
        r.typeAll("ghost") // all absent
        r.submit()
        val keys = r.snapshot().letterStates
        assertEquals(LetterResult.Correct, keys['A'])
        assertEquals(LetterResult.Correct, keys['E'])
        assertEquals(LetterResult.Absent, keys['S'])
        assertEquals(LetterResult.Absent, keys['G'])
        assertNull(keys['Z'])
    }

    @Test
    fun aLetterThatIsPresentAndAbsentInTheSameRowStaysPresent() {
        // Answer ABCDE has one B. In "JKBBL" the first B is Present and the second B is Absent.
        val list = WordList(Language.EN, 5, valid = listOf("abcde", "jkbbl"), answers = listOf("abcde"))
        val r = WordleRound(0, WordleGame(list, "abcde"), list)
        r.typeAll("jkbbl")
        r.submit()
        assertEquals(LetterResult.Present, r.snapshot().letterStates['B'])
    }

    @Test
    fun aLaterCorrectResultUpgradesAPresentLetter() {
        val list = WordList(Language.EN, 5, valid = listOf("abcde", "bfghi", "abjkl"), answers = listOf("abcde"))
        val r = WordleRound(0, WordleGame(list, "abcde"), list)
        r.typeAll("bfghi") // B is Present
        r.submit()
        assertEquals(LetterResult.Present, r.snapshot().letterStates['B'])
        r.typeAll("abjkl") // B is Correct now
        r.submit()
        assertEquals(LetterResult.Correct, r.snapshot().letterStates['B'])
    }

    @Test
    fun freshRoundIsUntouchedUntilSomethingIsTyped() {
        val r = round()
        assertTrue(r.isUntouched)
        r.type('c')
        assertFalse(r.isUntouched)
    }
}
