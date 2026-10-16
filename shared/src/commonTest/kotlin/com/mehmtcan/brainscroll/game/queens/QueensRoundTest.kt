package com.mehmtcan.brainscroll.game.queens

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

class QueensRoundTest {

    private var clock = 1_000L

    private val puzzle = QueensGenerator.generate(4)

    private fun round() = QueensRound(0, "q1", puzzle.size, puzzle.locked, Language.EN, { clock })

    /** The cells of one full placement that holds the locked queens, without the locked ones. */
    private fun missing() = QueensSolver.solutions(puzzle.size, puzzle.locked).first().filter { it !in puzzle.locked }

    private fun QueensRound.finish(stepMillis: Long = 1_000) {
        for (cell in missing()) {
            clock += stepMillis
            toggle(cell)
        }
    }

    @Test
    fun aRoundIsUntouchedUntilTheFirstTap() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Queens, r.kind)
        assertEquals(3, r.snapshot().state.par)
        r.toggle(missing().first())
        assertTrue(!r.isUntouched)
    }

    @Test
    fun aRefusedTapChangesNothingNotEvenTheClock() {
        val r = round()
        val cells = missing()
        r.toggle(cells[0])
        clock += 4_000
        assertEquals(QueensResult.Ignored, r.toggle(puzzle.locked.first()))
        clock += 3_000
        r.toggle(cells[1])
        assertEquals(7_000, r.snapshot().durationMs)
        assertEquals(2, r.toStored().guesses.size)
    }

    @Test
    fun theTimeIsTheSumOfThePausesAndEachPauseIsCapped() {
        val r = round()
        val cells = missing()
        clock = 10_000
        r.toggle(cells[0])
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.toggle(cells[1])
        clock += 10 * 60_000
        r.toggle(cells[2])
        assertEquals(2_500 + QueensRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsBoardAndActions() {
        val r = round()
        val cells = missing()
        r.toggle(cells[0])
        r.toggle(cells[1])
        r.restart()
        r.toggle(cells[2])
        r.toggle(cells[2])
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.Queens, stored.game)
        assertEquals("5:" + puzzle.locked.sorted().joinToString(","), stored.answer)
        assertTrue(stored.answer.length <= 32)
        assertEquals(listOf("t${cells[0]}", "t${cells[1]}", "r", "t${cells[2]}", "t${cells[2]}"), stored.guesses)

        val back = assertNotNull(QueensRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(QueensRound.restore(0, stored.copy(answer = "5"), { clock }), "no colon")
        assertNull(QueensRound.restore(0, stored.copy(answer = "x:1,2"), { clock }), "no size")
        assertNull(QueensRound.restore(0, stored.copy(answer = "5:1,a"), { clock }), "not a cell")
        assertNull(QueensRound.restore(0, stored.copy(answer = "5:99"), { clock }), "no such cell")
        assertNull(QueensRound.restore(0, stored.copy(answer = "2:"), { clock }), "no such board")
        assertNull(QueensRound.restore(0, stored.copy(guesses = listOf("x3")), { clock }))
        assertNull(QueensRound.restore(0, stored.copy(guesses = listOf("t")), { clock }))
        assertNull(QueensRound.restore(0, stored.copy(guesses = listOf("tx")), { clock }))
    }

    @Test
    fun aSolvedRoundIsAWinWhoseOnlyGuessIsTheNumberOfMoves() {
        val r = round()
        clock = 5_000
        r.finish()
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.Queens, finished.game)
        assertEquals(listOf("3"), finished.guesses)
        assertEquals(3, finished.moveCount)
        assertTrue(finished.answer.length <= 32, "the table limits answer to 32 characters")
        assertEquals(2_000L, finished.durationMs)
    }

    @Test
    fun theGuidePointsAtAQueenOfAWayToFinishAndAtAWrongQueenWhenThereIsNoMoreToPlace() {
        val r = round()
        val step = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(GuideReason.QueensPlace, step.reason)
        val cell = step.cells.single()
        assertTrue(QueensSolver.solutions(puzzle.size, puzzle.locked).any { cell in it })
        assertEquals(listOf(cell / 5 + 1, cell % 5 + 1), step.numbers)

        // A queen of the player's that fits no solution: once the rest is placed, the guide asks to take it away.
        val all = QueensSolver.solutions(puzzle.size, puzzle.locked)
        val wrong = (0 until 25).first { c -> c !in puzzle.locked && all.none { c in it } }
        r.toggle(wrong)
        assertEquals(GuideReason.QueensPlace, assertNotNull(assertNotNull(r.guideTask())()).reason)
        for (c in missing()) r.toggle(c)
        val last = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(GuideReason.QueensRemove, last.reason)
        assertEquals(wrong, last.cells.single())
        r.restart()
        r.finish()
        assertNull(r.guideTask())
    }
}
