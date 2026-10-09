package com.mehmtcan.brainscroll.game.slide

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

class SlideRoundTest {

    private var clock = 1_000L

    private fun round(seed: Int = 4) = SlideRound(0, "s1", seed, Language.EN, { clock })

    /** A cell next to the empty cell, so a tap on it always slides. */
    private fun SlideRound.movable(): Int {
        val tiles = snapshot().state.tiles
        val blank = tiles.indexOf(0)
        return if (blank % 3 > 0) blank - 1 else blank + 1
    }

    @Test
    fun aRoundIsUntouchedUntilTheFirstSlide() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Slide, r.kind)
        r.slide(r.movable())
        assertTrue(!r.isUntouched)
    }

    @Test
    fun anIgnoredActionChangesNothingNotEvenTheClock() {
        val r = round()
        r.slide(r.movable())
        clock += 4_000
        assertEquals(SlideResult.Ignored, r.slide(99))
        clock += 3_000
        r.slide(r.movable())
        assertEquals(7_000, r.snapshot().durationMs)
    }

    @Test
    fun theTimeIsTheSumOfThePausesAndEachPauseIsCapped() {
        val r = round()
        clock = 10_000
        r.slide(r.movable())
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.slide(r.movable())
        clock += 10 * 60_000
        r.slide(r.movable())
        assertEquals(2_500 + SlideRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsSeedAndActions() {
        val r = round()
        r.slide(r.movable())
        r.restart()
        r.slide(r.movable())
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.Slide, stored.game)
        assertEquals("3x4x80:4", stored.answer)
        assertEquals(3, stored.guesses.size)
        assertEquals("r", stored.guesses[1])

        val back = assertNotNull(SlideRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(SlideRound.restore(0, stored.copy(answer = "3x4x80"), { clock }))
        assertNull(SlideRound.restore(0, stored.copy(answer = "3x4x80:seed"), { clock }))
        assertNull(SlideRound.restore(0, stored.copy(answer = "3x4:4"), { clock }))
        assertNull(SlideRound.restore(0, stored.copy(answer = "1x4x80:4"), { clock }), "too narrow")
        assertNull(SlideRound.restore(0, stored.copy(answer = "3x4x0:4"), { clock }), "no shuffling")
        assertNull(SlideRound.restore(0, stored.copy(guesses = listOf("x5")), { clock }))
        assertNull(SlideRound.restore(0, stored.copy(guesses = listOf("m")), { clock }))
    }

    @Test
    fun aSolvedRoundIsAWinWhoseOnlyGuessIsTheNumberOfMoves() {
        val r = round()
        val taps = SlideSolver.solve(r.snapshot().state.tiles, 3, 4)
        clock = 5_000
        for (cell in taps) {
            clock += 1_000
            r.slide(cell)
        }
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.Slide, finished.game)
        assertEquals(listOf(taps.size.toString()), finished.guesses)
        assertEquals(taps.size, finished.moveCount)
        assertTrue(finished.answer.length <= 32, "the table limits answer to 32 characters")
        assertEquals((taps.size - 1) * 1_000L, finished.durationMs)
    }
}
