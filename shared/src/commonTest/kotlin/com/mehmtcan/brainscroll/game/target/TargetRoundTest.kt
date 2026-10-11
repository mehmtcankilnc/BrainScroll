package com.mehmtcan.brainscroll.game.target

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

class TargetRoundTest {

    private var clock = 1_000L

    /** The example: 2, 3, 6, 7 and the target 48. */
    private fun round() = TargetRound(0, "t1", listOf(2, 3, 6, 7), 48, Language.EN, { clock })

    @Test
    fun aRoundIsUntouchedUntilTheFirstMove() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Target, r.kind)
        assertEquals(3, r.snapshot().state.par)
        r.combine(0, Op.Add, 1)
        assertTrue(!r.isUntouched)
    }

    @Test
    fun aTargetThatCanNotBeMadeIsRefused() {
        assertFailsWith<IllegalArgumentException> { TargetRound(0, "t", listOf(1, 1, 1, 1), 99, Language.EN) }
    }

    @Test
    fun anIgnoredActionChangesNothingNotEvenTheClock() {
        val r = round()
        r.combine(0, Op.Add, 1)
        clock += 4_000
        assertEquals(TargetResult.Ignored, r.combine(0, Op.Add, 2), "the 2 is gone")
        clock += 3_000
        r.combine(2, Op.Mul, 3)
        assertEquals(7_000, r.snapshot().durationMs)
    }

    @Test
    fun theTimeIsTheSumOfThePausesAndEachPauseIsCapped() {
        val r = round()
        clock = 10_000
        r.combine(0, Op.Add, 1)
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.undo()
        clock += 10 * 60_000
        r.combine(0, Op.Add, 2)
        assertEquals(2_500 + TargetRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsNumbersAndActions() {
        val r = round()
        r.combine(2, Op.Mul, 0)
        r.combine(3, Op.Sub, 1)
        r.undo()
        r.combine(1, Op.Add, 3)
        r.restart()
        r.combine(0, Op.Add, 1)
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.Target, stored.game)
        assertEquals("2367>48", stored.answer)
        assertEquals(listOf("2*0", "3-1", "u", "1+3", "r", "0+1"), stored.guesses)

        val back = assertNotNull(TargetRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(TargetRound.restore(0, stored.copy(answer = "2367"), { clock }))
        assertNull(TargetRound.restore(0, stored.copy(answer = "2367>x"), { clock }))
        assertNull(TargetRound.restore(0, stored.copy(answer = "23a7>48"), { clock }))
        assertNull(TargetRound.restore(0, stored.copy(answer = "2307>48"), { clock }), "a zero is not a number of the game")
        assertNull(TargetRound.restore(0, stored.copy(answer = "1111>99"), { clock }), "a target that can not be made")
        assertNull(TargetRound.restore(0, stored.copy(answer = "2367>2"), { clock }), "a target that is on the table")
        assertNull(TargetRound.restore(0, stored.copy(guesses = listOf("x")), { clock }))
        assertNull(TargetRound.restore(0, stored.copy(guesses = listOf("0+")), { clock }))
        assertNull(TargetRound.restore(0, stored.copy(guesses = listOf("+1")), { clock }))
        assertNull(TargetRound.restore(0, stored.copy(guesses = listOf("0?1")), { clock }))
    }

    @Test
    fun aSolvedRoundIsAWinWhoseOnlyGuessIsTheNumberOfMoves() {
        val r = round()
        clock = 5_000
        for (move in TargetSolver.solve(listOf(2, 3, 6, 7), 48)!!) {
            clock += 1_000
            r.combine(move.first, move.op, move.second)
        }
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.Target, finished.game)
        assertEquals(listOf("3"), finished.guesses)
        assertEquals(3, finished.moveCount)
        assertTrue(finished.answer.length <= 32, "the table limits answer to 32 characters")
        assertEquals(2_000L, finished.durationMs)
    }
}
