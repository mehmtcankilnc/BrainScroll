package com.mehmtcan.brainscroll.game.pipes

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PipesRoundTest {

    private var clock = 1_000L

    private val puzzle = PipesGenerator.generate(4)

    private fun round() = PipesRound(0, "p1", puzzle.start.toList(), Language.EN, { clock }, puzzle.par)

    @Test
    fun aRoundIsUntouchedUntilTheFirstTap() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Pipes, r.kind)
        assertEquals(puzzle.par, r.snapshot().state.par)
        r.tap(3)
        assertTrue(!r.isUntouched)
    }

    @Test
    fun aBoardThatCanNotBeSolvedIsRefused() {
        assertFailsWith<IllegalArgumentException> { PipesRound(0, "p", List(9) { PipesGenerator.NORTH }, Language.EN) }
    }

    @Test
    fun anIgnoredActionChangesNothingNotEvenTheClock() {
        val r = round()
        r.tap(3)
        clock += 4_000
        assertEquals(PipesResult.Ignored, r.tap(99))
        clock += 3_000
        r.tap(5)
        assertEquals(7_000, r.snapshot().durationMs)
    }

    @Test
    fun theTimeIsTheSumOfThePausesAndEachPauseIsCapped() {
        val r = round()
        clock = 10_000
        r.tap(1)
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.tap(2)
        clock += 10 * 60_000
        r.tap(3)
        assertEquals(2_500 + PipesRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsTilesAndActions() {
        val r = round()
        r.tap(6)
        r.tap(12)
        r.restart()
        r.tap(0)
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.Pipes, stored.game)
        assertEquals(25, stored.answer.length)
        assertEquals(puzzle.start.joinToString("") { it.toString(16) }, stored.answer)
        assertEquals(listOf("t6", "t12", "r", "t0"), stored.guesses)

        val back = assertNotNull(PipesRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(PipesRound.restore(0, stored.copy(answer = stored.answer.dropLast(1)), { clock }), "not a square board")
        assertNull(PipesRound.restore(0, stored.copy(answer = "abcd"), { clock }), "too small")
        assertNull(PipesRound.restore(0, stored.copy(answer = stored.answer.replaceRange(0, 1, "z")), { clock }), "not a tile")
        assertNull(PipesRound.restore(0, stored.copy(answer = "1".repeat(25)), { clock }), "a board with no way to close the ends")
        assertNull(PipesRound.restore(0, stored.copy(guesses = listOf("x5")), { clock }))
        assertNull(PipesRound.restore(0, stored.copy(guesses = listOf("t")), { clock }))
    }

    @Test
    fun aSolvedRoundIsAWinWhoseOnlyGuessIsTheNumberOfMoves() {
        val r = round()
        val turns = PipesGenerator.solve(puzzle.start, 5)!!
        clock = 5_000
        var taps = 0
        for (cell in turns.indices) repeat(turns[cell]) {
            clock += 1_000
            r.tap(cell)
            taps++
        }
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.Pipes, finished.game)
        assertEquals(listOf(puzzle.par.toString()), finished.guesses)
        assertEquals(puzzle.par, finished.moveCount)
        assertTrue(finished.answer.length <= 32, "the table limits answer to 32 characters")
        assertEquals((taps - 1) * 1_000L, finished.durationMs)
    }
}
