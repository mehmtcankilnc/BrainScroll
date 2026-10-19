package com.mehmtcan.brainscroll.game.arrows

import com.mehmtcan.brainscroll.game.Difficulty
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

class ArrowsRoundTest {

    private var clock = 1_000L

    private val puzzle = ArrowsGenerator.generate(4, Difficulty.Medium)

    private fun round() = ArrowsRound(0, "a1", puzzle, Language.EN, { clock })

    /** A tap on an arrow that is in the way of another one. */
    private fun blockedCell(): Int {
        val present = BooleanArray(puzzle.arrowCount) { true }
        return puzzle.arrows[present.indices.first { ArrowsRules.blocker(puzzle, present, it) != null }].head
    }

    private fun ArrowsRound.finish(stepMillis: Long = 1_000) {
        for (arrow in ArrowsSolver.order(puzzle)) {
            clock += stepMillis
            tap(puzzle.arrows[arrow].head)
        }
    }

    @Test
    fun aRoundIsUntouchedUntilTheFirstTap() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Arrows, r.kind)
        r.tap(blockedCell())
        assertTrue(!r.isUntouched)
    }

    @Test
    fun aTapOnNothingChangesNothingNotEvenTheClock() {
        val r = round()
        val empty = puzzle.owner.indexOf(-1)
        r.tap(blockedCell())
        clock += 4_000
        assertEquals(ArrowsResult.Ignored, r.tap(empty))
        clock += 3_000
        r.tap(blockedCell())
        assertEquals(7_000, r.snapshot().durationMs)
        assertEquals(2, r.toStored().guesses.size)
    }

    @Test
    fun theTimeIsTheSumOfThePausesAndEachPauseIsCapped() {
        val r = round()
        clock = 10_000
        r.tap(blockedCell())
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.tap(blockedCell())
        clock += 10 * 60_000
        r.tap(blockedCell())
        assertEquals(2_500 + ArrowsRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsBoardAndActions() {
        val r = round()
        val order = ArrowsSolver.order(puzzle).map { puzzle.arrows[it].head }
        r.tap(blockedCell())
        r.tap(order[0])
        r.tap(order[1])
        r.restart()
        r.tap(order[0])
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.Arrows, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)
        assertEquals(listOf("t${blockedCell()}", "t${order[0]}", "t${order[1]}", "r", "t${order[0]}"), stored.guesses)

        val back = assertNotNull(ArrowsRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(1, back.snapshot().state.mistakes)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(ArrowsRound.restore(0, stored.copy(answer = "6:1"), { clock }))
        assertNull(ArrowsRound.restore(0, stored.copy(guesses = listOf("x3")), { clock }))
        assertNull(ArrowsRound.restore(0, stored.copy(guesses = listOf("t")), { clock }))
        assertNull(ArrowsRound.restore(0, stored.copy(guesses = listOf("tx")), { clock }))
    }

    @Test
    fun aSolvedRoundIsAWinWhoseOnlyGuessIsTheNumberOfMistakes() {
        val r = round()
        r.tap(blockedCell())
        r.tap(blockedCell())
        assertEquals(2, r.snapshot().state.mistakes)
        clock = 5_000
        r.finish()
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.Arrows, finished.game)
        assertEquals(listOf("2"), finished.guesses)
        assertEquals(2, finished.moveCount)
        assertTrue(finished.answer.length <= 32)
    }

    @Test
    fun theGuidePointsAtAnArrowThatCanLeaveAndStopsWhenDone() {
        val r = round()
        val step = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(GuideReason.ArrowsEscape, step.reason)
        val cell = step.cells.single()
        assertTrue(cell in ArrowsRules.free(puzzle, BooleanArray(puzzle.arrowCount) { true }).map { puzzle.arrows[it].head })
        assertEquals(listOf(cell / puzzle.size + 1, cell % puzzle.size + 1), step.numbers)
        r.finish()
        assertNull(r.guideTask())
    }
}
