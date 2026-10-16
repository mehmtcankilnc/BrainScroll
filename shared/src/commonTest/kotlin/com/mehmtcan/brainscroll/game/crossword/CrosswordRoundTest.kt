package com.mehmtcan.brainscroll.game.crossword

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrosswordRoundTest {

    private var clock = 1_000L

    private val puzzle = CrosswordGenerator.generate(4, Language.TR)

    private fun round() = CrosswordRound(0, "c1", puzzle, { clock })

    private fun CrosswordRound.cells() = snapshot().state.let { s -> s.isClue.indices.filter { !s.isClue[it] } }

    private fun solution(): List<Char> = puzzle.squares.toList()

    private fun CrosswordRound.finish(stepMillis: Long = 1_000) {
        for (cell in cells()) {
            clock += stepMillis
            place(cell, solution()[cell])
        }
    }

    @Test
    fun aRoundIsUntouchedUntilTheFirstLetterAndKnowsItsLanguage() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Crossword, r.kind)
        assertEquals(Language.TR, r.language)
        assertEquals(Language.TR, r.snapshot().language)
        r.place(r.cells().first(), solution()[r.cells().first()])
        assertTrue(!r.isUntouched)
    }

    @Test
    fun anIgnoredActionChangesNothingNotEvenTheClock() {
        val r = round()
        val cells = r.cells()
        r.place(cells[0], solution()[cells[0]])
        clock += 4_000
        assertEquals(CrosswordResult.Ignored, r.place(cells[0], 'A'), "a right letter is final")
        clock += 3_000
        r.place(cells[1], solution()[cells[1]])
        assertEquals(7_000, r.snapshot().durationMs)
        assertEquals(2, r.toStored().guesses.size)
    }

    @Test
    fun theTimeIsTheSumOfThePausesAndEachPauseIsCapped() {
        val r = round()
        val cells = r.cells()
        clock = 10_000
        r.place(cells[0], solution()[cells[0]])
        clock += 2_500
        r.place(cells[1], solution()[cells[1]])
        clock += 10 * 60_000
        r.place(cells[2], solution()[cells[2]])
        assertEquals(2_500 + CrosswordRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsSquaresAndActions() {
        val r = round()
        val cells = r.cells()
        val wrong = "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ".first { it != solution()[cells[0]] }
        r.place(cells[0], wrong)
        clock += 2_000
        r.erase(cells[0])
        r.place(cells[0], solution()[cells[0]])
        r.place(cells[1], solution()[cells[1]])
        val stored = r.toStored()
        assertEquals(GameKind.Crossword, stored.game)
        assertEquals(Language.TR, stored.language)
        assertEquals(puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)

        val back = assertNotNull(CrosswordRound.restore(3, stored, { clock }))
        assertEquals(3, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(CrosswordRound.restore(0, stored.copy(answer = "short"), { clock }))
        assertNull(CrosswordRound.restore(0, stored.copy(answer = "b99999"), { clock }), "there is no such board")
        assertNull(CrosswordRound.restore(0, stored.copy(answer = "bx"), { clock }))
        assertNull(CrosswordRound.restore(0, stored.copy(guesses = listOf("x3")), { clock }))
        assertNull(CrosswordRound.restore(0, stored.copy(guesses = listOf("p3")), { clock }))
        assertNull(CrosswordRound.restore(0, stored.copy(guesses = listOf("p3:AB")), { clock }))
        assertNull(CrosswordRound.restore(0, stored.copy(guesses = listOf("pz:A")), { clock }))
    }

    @Test
    fun aSolvedRoundIsAWinWhoseGuessesAreTheLettersAndTheMistakes() {
        val r = round()
        clock = 5_000
        r.finish()
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.Crossword, finished.game)
        assertEquals(Language.TR, finished.language)
        assertEquals(listOf(r.cells().size.toString(), "0"), finished.guesses)
        assertEquals(r.cells().size, finished.moveCount)
        assertTrue(finished.answer.length <= 32)
    }

    @Test
    fun theGuideTakesBackAWrongLetterFirstAndThenGivesTheLetterOfTheFirstEmptySquare() {
        val r = round()
        val cells = r.cells()
        val first = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(GuideReason.CrosswordLetter, first.reason)
        assertEquals(listOf(cells.first()), first.cells)
        assertEquals(solution()[cells.first()].toString(), first.text)

        val wrong = "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ".first { it != solution()[cells[2]] }
        r.place(cells[2], wrong)
        val erase = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(GuideReason.CrosswordErase, erase.reason)
        assertEquals(listOf(cells[2]), erase.cells)

        r.erase(cells[2])
        r.place(cells[0], solution()[cells[0]])
        assertEquals(listOf(cells[1]), assertNotNull(assertNotNull(r.guideTask())()).cells)
        r.finish()
        assertNull(r.guideTask())
    }
}
