package com.mehmtcan.brainscroll.game.wordle

import com.mehmtcan.brainscroll.game.wordle.LetterResult.Absent
import com.mehmtcan.brainscroll.game.wordle.LetterResult.Correct
import com.mehmtcan.brainscroll.game.wordle.LetterResult.Present
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GuessEvaluatorTest {

    @Test
    fun exactMatchIsAllCorrect() {
        assertEquals(List(5) { Correct }, evaluateGuess("CRANE", "CRANE"))
    }

    @Test
    fun noSharedLettersIsAllAbsent() {
        assertEquals(List(5) { Absent }, evaluateGuess("CRANE", "MOGUL"))
    }

    @Test
    fun misplacedLettersArePresent() {
        assertEquals(List(5) { Present }, evaluateGuess("ABCDE", "BCDEA"))
    }

    @Test
    fun repeatedGuessLetterIsMarkedOnlyAsOftenAsAnswerHasIt() {
        // The answer has a single L: the first L is Present, the second one is Absent.
        assertEquals(
            listOf(Present, Absent, Absent, Absent, Absent),
            evaluateGuess("LLABC", "XYLZW"),
        )
    }

    @Test
    fun documentedExampleWithTwoBs() {
        assertEquals(
            listOf(Present, Present, Correct, Absent, Correct),
            evaluateGuess("BABBY", "ABBEY"),
        )
    }

    @Test
    fun exactMatchTakesPriorityOverPresentForSameLetter() {
        // The only E of the answer is at the end and is matched exactly, so the earlier E is Absent.
        assertEquals(
            listOf(Absent, Absent, Absent, Absent, Correct),
            evaluateGuess("EXXXE", "ABCDE"),
        )
    }

    @Test
    fun turkishDottedAndDotlessIAreDifferentLetters() {
        assertEquals(
            listOf(Correct, Absent, Correct, Correct, Correct),
            evaluateGuess("KİTAP", "KITAP"),
        )
    }

    @Test
    fun turkishSpecialLettersDoNotMatchTheirAsciiLookalikes() {
        assertEquals(
            listOf(Absent, Correct, Absent, Correct, Correct),
            evaluateGuess("ÇAĞRI", "CAGRI"),
        )
    }

    @Test
    fun lengthMismatchIsRejected() {
        assertFailsWith<IllegalArgumentException> { evaluateGuess("ABC", "ABCDE") }
    }
}
