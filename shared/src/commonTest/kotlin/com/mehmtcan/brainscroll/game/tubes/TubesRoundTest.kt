package com.mehmtcan.brainscroll.game.tubes

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TubesRoundTest {

    /** Four 0s and four 1s, tube 3 is empty. */
    private val layout = listOf(listOf(0, 1, 0), listOf(1, 0, 1, 1), listOf(0), emptyList<Int>())
    private var clock = 1_000L

    private fun round() = TubesRound(0, "t1", layout, Language.EN, { clock })

    @Test
    fun aRoundIsUntouchedUntilTheFirstAction() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Tubes, r.kind)
        r.pour(0, 3)
        assertFalse(r.isUntouched)
    }

    @Test
    fun anIgnoredPourChangesNothingNotEvenTheClock() {
        val r = round()
        r.pour(0, 3)
        clock += 4_000
        assertEquals(PourResult.Ignored, r.pour(3, 3))
        clock += 3_000
        r.undo()
        assertEquals(7_000, r.snapshot().durationMs, "the ignored pour did not count as an action")
    }

    @Test
    fun theTimeIsTheSumOfThePausesBetweenActionsAndEachPauseIsCapped() {
        val r = round()
        clock = 10_000
        r.pour(0, 3) // starts the clock
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.undo()
        assertEquals(2_500, r.snapshot().durationMs)
        clock += 10 * 60_000 // a long pause (the app was in the background)
        r.pour(0, 3)
        assertEquals(2_500 + TubesRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyIncludingUndoAndRestart() {
        val r = round()
        r.pour(0, 3)
        r.pour(2, 3)
        r.undo()
        r.pour(1, 3)
        r.restart()
        r.pour(0, 3)
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.Tubes, stored.game)
        assertEquals("010|1011|0|", stored.answer)

        val back = assertNotNull(TubesRound.restore(4, stored, { clock }))
        assertEquals(4, back.index)
        val before = r.snapshot()
        val after = back.snapshot()
        assertEquals(before.state, after.state)
        assertEquals(before.durationMs, after.durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(TubesRound.restore(0, stored.copy(answer = "01x|11"), { clock }))
        assertNull(TubesRound.restore(0, stored.copy(answer = "0|1"), { clock }), "colors must fill their tubes")
        assertNull(TubesRound.restore(0, stored.copy(guesses = listOf("0>")), { clock }))
        assertNull(TubesRound.restore(0, stored.copy(guesses = listOf("a>b")), { clock }))
    }

    @Test
    fun aFinishedRoundIsAWinWhoseOnlyGuessIsTheNumberOfPours() {
        val easy = TubesRound(1, "t2", listOf(listOf(0, 0, 0), listOf(0), emptyList()), Language.TR, { clock })
        clock = 5_000
        easy.pour(1, 0)
        clock += 7_000
        assertTrue(easy.isFinished)
        val finished = easy.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.Tubes, finished.game)
        assertEquals(listOf("1"), finished.guesses)
        assertEquals(1, finished.moveCount)
        assertEquals(0L, finished.durationMs, "one pour only: the clock started with it")
    }

    @Test
    fun theCloudKeepsAtMost256CharactersOfGuessesSoALongGameStillFits() {
        val easy = TubesRound(0, "t3", listOf(listOf(0, 0, 0, 1), listOf(1, 1, 1, 0), emptyList(), emptyList()), Language.EN, { clock })
        repeat(200) { easy.pour(0, 2); easy.undo() }
        val finished = easy.run {
            pour(0, 2)
            pour(1, 3)
            pour(2, 1); pour(3, 0)
            toFinished(0)
        }
        assertEquals(listOf("204"), finished.guesses, "undone pours still count as moves")
        assertTrue(finished.guesses.joinToString(",").length <= 256)
        assertTrue(finished.answer.length <= 32)
    }
}
