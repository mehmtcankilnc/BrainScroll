package com.mehmtcan.brainscroll.game.wordle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordleGameTest {

    private val en = WordList(
        language = Language.EN,
        wordLength = 5,
        valid = listOf("crane", "slate", "ghost", "pound", "apple"),
        answers = listOf("crane"),
    )

    private fun game(answer: String = "CRANE", maxAttempts: Int = 6) = WordleGame(en, answer, maxAttempts)

    private fun WordleGame.guess(word: String) = perform(WordleAction.Submit(word))

    @Test
    fun startsInPlayingStateWithHiddenAnswer() {
        val state = game().state
        assertEquals(WordleStatus.Playing, state.status)
        assertTrue(state.rows.isEmpty())
        assertNull(state.answer)
    }

    @Test
    fun correctGuessWinsAndRevealsAnswer() {
        val g = game()
        val state = g.guess("crane")
        assertEquals(WordleStatus.Won, state.status)
        assertEquals("CRANE", state.answer)
        assertTrue(g.isFinished)
    }

    @Test
    fun usingAllAttemptsLoses() {
        val g = game(maxAttempts = 2)
        g.guess("slate")
        val state = g.guess("ghost")
        assertEquals(WordleStatus.Lost, state.status)
        assertEquals("CRANE", state.answer)
    }

    @Test
    fun winningOnTheLastAttemptIsAWin() {
        val g = game(maxAttempts = 2)
        g.guess("slate")
        assertEquals(WordleStatus.Won, g.guess("crane").status)
    }

    @Test
    fun rejectedGuessesDoNotUseAnAttempt() {
        val g = game()

        g.guess("cran")
        assertEquals(GuessError.WrongLength, g.lastError)

        g.guess("cr4ne")
        assertEquals(GuessError.InvalidLetters, g.lastError)

        g.guess("zzzzz")
        assertEquals(GuessError.NotInDictionary, g.lastError)

        assertTrue(g.state.rows.isEmpty())

        g.guess("slate")
        assertNull(g.lastError)
        assertEquals(1, g.state.rows.size)
    }

    @Test
    fun guessesAfterTheGameEndsAreRejected() {
        val g = game()
        g.guess("crane")
        val rowsBefore = g.state.rows.size
        g.guess("slate")
        assertEquals(GuessError.GameOver, g.lastError)
        assertEquals(rowsBefore, g.state.rows.size)
    }

    @Test
    fun guessIsCaseInsensitiveAndTrimmed() {
        val g = game()
        assertEquals(WordleStatus.Won, g.guess("  CrAnE ").status)
    }

    @Test
    fun rowsKeepFeedbackPerGuess() {
        val g = game()
        g.guess("slate")
        val row = g.state.rows.single()
        assertEquals("SLATE", row.word)
        assertEquals(
            listOf(LetterResult.Absent, LetterResult.Absent, LetterResult.Correct, LetterResult.Absent, LetterResult.Correct),
            row.results,
        )
    }

    @Test
    fun turkishGuessWithLowercaseDotlessIIsAccepted() {
        val list = WordList(Language.TR, 5, valid = listOf("ISLAK", "KITAP"), answers = listOf("KITAP"))
        val g = WordleGame(list, "kitap")
        g.perform(WordleAction.Submit("ıslak"))
        assertNull(g.lastError)
        assertEquals("ISLAK", g.state.rows.single().word)
    }

    @Test
    fun turkishLowercaseDottedIBecomesDottedCapital() {
        val list = WordList(Language.TR, 5, valid = listOf("İŞÇİL", "KITAP"), answers = listOf("KITAP"))
        val g = WordleGame(list, "KITAP")
        g.perform(WordleAction.Submit("işçil"))
        assertNull(g.lastError)
        assertEquals("İŞÇİL", g.state.rows.single().word)
    }

    @Test
    fun invalidAnswerIsRejectedAtConstruction() {
        assertFailsWith<IllegalArgumentException> { WordleGame(en, "cran") }
        assertFailsWith<IllegalArgumentException> { WordleGame(en, "çağrı") }
    }

    @Test
    fun wordListFiltersWrongLengthAndForeignWords() {
        val list = WordList(
            language = Language.EN,
            wordLength = 5,
            valid = listOf("crane", "toolong", "ab1de", "slate"),
            answers = listOf("crane"),
        )
        assertEquals(setOf("CRANE", "SLATE"), list.valid)
    }

    @Test
    fun wordListRequiresAnswersToBeGuessable() {
        assertFailsWith<IllegalArgumentException> {
            WordList(Language.EN, 5, valid = listOf("crane"), answers = listOf("slate"))
        }
    }

    @Test
    fun parseLinesSkipsBlankLinesAndComments() {
        assertEquals(listOf("kitap", "araba"), WordList.parseLines("# header\nkitap\n\n  araba  \n"))
    }
}
