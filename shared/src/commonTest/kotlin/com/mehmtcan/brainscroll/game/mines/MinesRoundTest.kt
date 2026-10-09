package com.mehmtcan.brainscroll.game.mines

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

class MinesRoundTest {

    private var clock = 1_000L

    private fun round(seed: Int = 4) = MinesRound(0, "m1", seed, Language.EN, { clock })

    @Test
    fun aRoundIsUntouchedUntilTheFirstActionAndAFlagIsAnAction() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Mines, r.kind)
        r.flag(3)
        assertTrue(!r.isUntouched)
    }

    @Test
    fun anIgnoredActionChangesNothingNotEvenTheClock() {
        val r = round()
        r.open(27)
        clock += 4_000
        assertEquals(MinesResult.Ignored, r.open(27))
        clock += 3_000
        r.flag(0)
        assertEquals(7_000, r.snapshot().durationMs)
    }

    @Test
    fun theTimeIsTheSumOfThePausesAndEachPauseIsCapped() {
        val r = round()
        clock = 10_000
        r.open(27)
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.flag(1)
        clock += 10 * 60_000
        r.flag(1)
        assertEquals(2_500 + MinesRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsSeedAndActions() {
        val r = round()
        r.flag(60)
        r.open(27)
        r.flag(61)
        r.flag(61)
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.Mines, stored.game)
        assertEquals("8x8x10:4:27", stored.answer)

        val back = assertNotNull(MinesRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aRoundWithNoOpenYetIsSavedWithoutAFirstCell() {
        val r = round()
        r.flag(5)
        assertEquals("8x8x10:4:-1", r.toStored().answer)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(MinesRound.restore(0, stored.copy(answer = "8x8:4:1"), { clock }))
        assertNull(MinesRound.restore(0, stored.copy(answer = "axbxc:4:1"), { clock }))
        assertNull(MinesRound.restore(0, stored.copy(answer = "8x8x10:seed:1"), { clock }))
        assertNull(MinesRound.restore(0, stored.copy(answer = "8x8x99:4:1"), { clock }), "too many mines for the board")
        assertNull(MinesRound.restore(0, stored.copy(guesses = listOf("x5")), { clock }))
        assertNull(MinesRound.restore(0, stored.copy(guesses = listOf("o")), { clock }))
    }

    @Test
    fun aWonRoundIsAWinWhoseOnlyGuessIsTheNumberOfMoves() {
        val r = round(8)
        clock = 5_000
        r.open(27)
        clock += 7_000
        // Play on by logic: the game inside the round is reached through its actions.
        while (!r.isFinished) {
            val state = r.snapshot().state
            val numbers = state.numbers.toIntArray()
            val safe = MinesSolver.deduce(state.rows, state.cols, numbers).safe.filter { numbers[it] < 0 }
            safe.forEach { r.open(it) }
        }
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.Mines, finished.game)
        assertEquals(listOf(r.snapshot().state.moves.toString()), finished.guesses)
        assertEquals(r.snapshot().state.moves, finished.moveCount)
        assertTrue(finished.answer.length <= 32, "the table limits answer to 32 characters")
        assertTrue(finished.guesses.joinToString(",").length <= 256)
    }

    @Test
    fun aLostRoundIsALoss() {
        val first = 27
        val board = MinesGame.generate(8, 8, 10, 4, first)
        val r = round()
        r.open(first)
        r.open(board.indices.first { board[it] })
        val finished = r.toFinished(finishedAt = 1)
        assertEquals(Outcome.LOST, finished.outcome)
    }
}
