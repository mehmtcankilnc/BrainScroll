package com.mehmtcan.brainscroll.game.sudoku

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SudokuRoundTest {

    private var clock = 1_000L

    private fun round(seed: Int = 4) = SudokuRound(0, "s1", seed, Language.EN, { clock })

    private fun solution(seed: Int = 4) = SudokuGenerator.generate(seed).solution

    private fun SudokuRound.emptyCells() = snapshot().state.let { s -> s.values.indices.filter { s.values[it] == 0 } }

    @Test
    fun aRoundIsUntouchedUntilTheFirstActionAndANoteIsAnAction() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Sudoku, r.kind)
        r.toggleNote(r.emptyCells().first(), 3)
        assertTrue(!r.isUntouched)
    }

    @Test
    fun anIgnoredActionChangesNothingNotEvenTheClock() {
        val r = round()
        val cell = r.emptyCells().first()
        r.place(cell, solution()[cell])
        clock += 4_000
        assertEquals(SudokuResult.Ignored, r.place(cell, 9), "a right digit is final")
        clock += 3_000
        r.toggleNote(r.emptyCells().first(), 1)
        assertEquals(7_000, r.snapshot().durationMs)
    }

    @Test
    fun theTimeIsTheSumOfThePausesAndEachPauseIsCapped() {
        val r = round()
        val cell = r.emptyCells().first()
        clock = 10_000
        r.toggleNote(cell, 1)
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.toggleNote(cell, 2)
        clock += 10 * 60_000
        r.toggleNote(cell, 3)
        assertEquals(2_500 + SudokuRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsSeedAndActions() {
        val r = round()
        val cells = r.emptyCells()
        val sol = solution()
        r.toggleNote(cells[0], 5)
        r.toggleNote(cells[1], 2)
        r.place(cells[2], sol[cells[2]])
        r.place(cells[3], if (sol[cells[3]] == 1) 2 else 1)
        r.erase(cells[3])
        r.erase(cells[1])
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.Sudoku, stored.game)
        assertEquals("9x9x36:4", stored.answer)

        val back = assertNotNull(SudokuRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(SudokuRound.restore(0, stored.copy(answer = "9x9x36"), { clock }))
        assertNull(SudokuRound.restore(0, stored.copy(answer = "9x9x36:seed"), { clock }))
        assertNull(SudokuRound.restore(0, stored.copy(answer = "8x8x36:4"), { clock }))
        assertNull(SudokuRound.restore(0, stored.copy(answer = "9x9xa:4"), { clock }))
        assertNull(SudokuRound.restore(0, stored.copy(answer = "9x9x3:4"), { clock }), "too few givens")
        assertNull(SudokuRound.restore(0, stored.copy(guesses = listOf("x5")), { clock }))
        assertNull(SudokuRound.restore(0, stored.copy(guesses = listOf("p5")), { clock }), "a place needs a digit")
        assertNull(SudokuRound.restore(0, stored.copy(guesses = listOf("e5:3")), { clock }), "an erase takes no digit")
        assertNull(SudokuRound.restore(0, stored.copy(guesses = listOf("p")), { clock }))
    }

    @Test
    fun aWonRoundIsAWinWhoseOnlyGuessIsTheNumberOfMoves() {
        val r = round()
        val sol = solution()
        clock = 5_000
        for (cell in r.emptyCells()) {
            clock += 1_000
            r.place(cell, sol[cell])
        }
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.Sudoku, finished.game)
        assertEquals(listOf(r.snapshot().state.moves.toString()), finished.guesses)
        assertEquals(r.snapshot().state.moves, finished.moveCount)
        assertTrue(finished.answer.length <= 32, "the table limits answer to 32 characters")
        assertTrue(finished.guesses.joinToString(",").length <= 256)
        assertEquals(snapshotTime(r), finished.durationMs)
    }

    private fun snapshotTime(r: SudokuRound) = r.snapshot().durationMs

    @Test
    fun aLostRoundIsALoss() {
        val r = round()
        val sol = solution()
        for (cell in r.emptyCells().take(3)) r.place(cell, if (sol[cell] == 1) 2 else 1)
        assertEquals(SudokuStatus.Lost, r.snapshot().state.status)
        assertEquals(Outcome.LOST, r.toFinished(finishedAt = 1).outcome)
    }
}
