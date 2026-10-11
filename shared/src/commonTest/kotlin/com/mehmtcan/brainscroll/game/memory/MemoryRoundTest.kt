package com.mehmtcan.brainscroll.game.memory

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.memory.CardSymbol.Circle
import com.mehmtcan.brainscroll.game.memory.CardSymbol.Heart
import com.mehmtcan.brainscroll.game.memory.CardSymbol.Star
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemoryRoundTest {

    private val layout = listOf(Circle, Star, Heart, Circle, Star, Heart)
    private var clock = 1_000L

    private fun round(max: Int = 3) = MemoryRound(0, "m1", layout, Language.EN, { clock }, max)

    @Test
    fun anUntouchedRoundIsUntouchedUntilTheFirstFlip() {
        val r = round()
        assertTrue(r.isUntouched)
        r.flip(0)
        assertFalse(r.isUntouched)
        assertEquals(GameKind.Memory, r.kind)
    }

    @Test
    fun anIgnoredTapChangesNothingNotEvenTheClock() {
        val r = round()
        r.flip(0)
        clock += 4_000
        r.flip(0) // already face up
        clock += 3_000
        r.flip(3)
        assertEquals(7_000, r.snapshot().durationMs, "the ignored tap did not count as a flip")
    }

    @Test
    fun theTimeIsTheSumOfThePausesBetweenFlipsAndStartsWithTheFirst() {
        val r = round()
        clock = 10_000
        r.flip(0) // starts the clock
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.flip(3)
        clock += 1_000
        r.flip(1)
        assertEquals(3_500, r.snapshot().durationMs)
    }

    @Test
    fun aLongPauseCountsAtMostTenSeconds() {
        val r = round()
        r.flip(0)
        clock += 6 * 60 * 60 * 1000 // left in the background for six hours
        r.flip(3)
        assertEquals(MemoryRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aWonRoundBecomesAFinishedResultWithItsDurationAndFlips() {
        val r = round()
        r.flip(0); clock += 1_000; r.flip(3)
        clock += 1_000; r.flip(1); clock += 1_000; r.flip(4)
        clock += 1_000; r.flip(2); clock += 1_000; r.flip(5)
        assertTrue(r.isFinished)

        val result = r.toFinished(finishedAt = 99_000)
        assertEquals(GameKind.Memory, result.game)
        assertEquals(Mode.ENDLESS, result.mode)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(listOf("0", "3", "1", "4", "2", "5"), result.guesses)
        assertEquals(layout.joinToString(",") { it.ordinal.toString() }, result.answer, "the board is stored as symbol numbers")
        assertEquals(5_000, result.durationMs)
        assertEquals(99_000, result.finishedAt)
    }

    @Test
    fun enoughMistakesBecomeALostResult() {
        val r = round(max = 2)
        r.flip(0); r.flip(1)
        r.flip(0); r.flip(1)
        assertEquals(Outcome.LOST, r.toFinished(1).outcome)
    }

    @Test
    fun aRoundSurvivesBeingSavedAndRestored() {
        val r = round()
        r.flip(0); clock += 1_500; r.flip(3)       // match
        clock += 2_000; r.flip(1)                  // one card up, waiting for a second
        val stored = r.toStored()
        assertEquals(GameKind.Memory, stored.game)
        assertEquals(listOf("0", "3", "1"), stored.guesses)

        val restored = assertNotNull(MemoryRound.restore(5, stored, { clock }))
        val state = restored.snapshot().state
        assertEquals(setOf(0, 3), state.matched)
        assertEquals(setOf(1), state.faceUp)
        assertEquals(1, state.moves)
        assertEquals(1_500 + 2_000, restored.snapshot().durationMs, "the time so far comes back")
        assertEquals(5, restored.index, "renumbered by where it ends up in the feed")
        assertEquals(r.id, restored.id)
    }

    @Test
    fun aMismatchOnScreenWhenTheAppClosedIsHiddenAgainButStillCounts() {
        val r = round()
        r.flip(0); r.flip(1) // mismatch showing
        val restored = assertNotNull(MemoryRound.restore(0, r.toStored(), { clock }))
        val state = restored.snapshot().state
        assertTrue(state.faceUp.isEmpty() && !state.mismatchShown)
        assertEquals(1, state.mistakes)
    }

    @Test
    fun theTimeAfterARestoreOnlyCountsNewPlay() {
        val r = round()
        r.flip(0); clock += 3_000; r.flip(3)
        val restored = assertNotNull(MemoryRound.restore(0, r.toStored(), { clock }))
        clock += 5 * 60 * 1000 // the app was closed for five minutes
        restored.flip(1)
        assertEquals(3_000, restored.snapshot().durationMs, "the first flip after the restore starts counting afresh")
        clock += 2_000
        restored.flip(4)
        assertEquals(5_000, restored.snapshot().durationMs)
    }

    @Test
    fun aSavedBoardThatCannotBeReadIsRefused() {
        val good = round().also { it.flip(0) }.toStored()
        assertNull(MemoryRound.restore(0, good.copy(answer = "x,y"), { clock }))
        assertNull(MemoryRound.restore(0, good.copy(answer = "0,1,2"), { clock }), "not pairs")
        assertNull(MemoryRound.restore(0, good.copy(answer = "99,99"), { clock }), "no such symbol")
        assertNull(MemoryRound.restore(0, good.copy(guesses = listOf("abc")), { clock }))
    }
}
