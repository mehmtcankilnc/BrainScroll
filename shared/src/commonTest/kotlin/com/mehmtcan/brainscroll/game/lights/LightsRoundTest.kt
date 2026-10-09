package com.mehmtcan.brainscroll.game.lights

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

class LightsRoundTest {

    private var clock = 1_000L

    private fun round(seed: Int = 4) = LightsRound(0, "l1", seed, Language.EN, { clock })

    private fun taps(seed: Int = 4) = LightsGame.solutionTaps(seed, 5, 7)

    @Test
    fun aRoundIsUntouchedUntilTheFirstTap() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Lights, r.kind)
        r.tap(3)
        assertTrue(!r.isUntouched)
    }

    @Test
    fun anIgnoredActionChangesNothingNotEvenTheClock() {
        val r = round()
        r.tap(3)
        clock += 4_000
        assertEquals(LightsResult.Ignored, r.tap(99))
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
        assertEquals(2_500 + LightsRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsSeedAndActions() {
        val r = round()
        r.tap(6)
        r.tap(12)
        r.restart()
        r.tap(0)
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.Lights, stored.game)
        assertEquals("5x5x7:4", stored.answer)
        assertEquals(listOf("t6", "t12", "r", "t0"), stored.guesses)

        val back = assertNotNull(LightsRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(LightsRound.restore(0, stored.copy(answer = "5x5x7"), { clock }))
        assertNull(LightsRound.restore(0, stored.copy(answer = "5x5x7:seed"), { clock }))
        assertNull(LightsRound.restore(0, stored.copy(answer = "5x4x7:4"), { clock }))
        assertNull(LightsRound.restore(0, stored.copy(answer = "5x5x99:4"), { clock }), "too many taps for the board")
        assertNull(LightsRound.restore(0, stored.copy(guesses = listOf("x5")), { clock }))
        assertNull(LightsRound.restore(0, stored.copy(guesses = listOf("t")), { clock }))
    }

    @Test
    fun aSolvedRoundIsAWinWhoseOnlyGuessIsTheNumberOfMoves() {
        val r = round()
        clock = 5_000
        for (cell in taps()) {
            clock += 1_000
            r.tap(cell)
        }
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.Lights, finished.game)
        assertEquals(listOf("7"), finished.guesses)
        assertEquals(7, finished.moveCount)
        assertTrue(finished.answer.length <= 32, "the table limits answer to 32 characters")
        assertEquals(6_000L, finished.durationMs)
    }
}
