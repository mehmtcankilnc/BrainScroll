package com.mehmtcan.brainscroll.game.tents

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

class TentsRoundTest {

    private var clock = 1_000L

    private val puzzle = TentsGenerator.generate(4)

    private fun round() = TentsRound(0, "t1", puzzle, Language.EN, { clock })

    private fun TentsRound.finish(stepMillis: Long = 1_000) {
        for (cell in puzzle.tents.sorted()) {
            clock += stepMillis
            tap(cell)
        }
    }

    @Test
    fun aRoundIsUntouchedUntilTheFirstTap() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Tents, r.kind)
        r.tap(puzzle.tents.first())
        assertTrue(!r.isUntouched)
    }

    @Test
    fun aRefusedTapChangesNothingNotEvenTheClock() {
        val r = round()
        val cells = puzzle.tents.sorted()
        r.tap(cells[0])
        clock += 4_000
        assertEquals(TentsResult.Ignored, r.tap(puzzle.trees.first()))
        clock += 3_000
        r.tap(cells[1])
        assertEquals(7_000, r.snapshot().durationMs)
        assertEquals(2, r.toStored().guesses.size)
    }

    @Test
    fun theTimeIsTheSumOfThePausesAndEachPauseIsCapped() {
        val r = round()
        val cells = puzzle.tents.sorted()
        clock = 10_000
        r.tap(cells[0])
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.tap(cells[1])
        clock += 10 * 60_000
        r.tap(cells[2])
        assertEquals(2_500 + TentsRound.MAX_THINK_MILLIS, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsBoardAndActions() {
        val r = round()
        val cells = puzzle.tents.sorted()
        r.tap(cells[0])
        r.tap(cells[1]); r.tap(cells[1])
        r.restart()
        r.tap(cells[2])
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.Tents, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)
        assertEquals(listOf("t${cells[0]}", "t${cells[1]}", "t${cells[1]}", "r", "t${cells[2]}"), stored.guesses)

        val back = assertNotNull(TentsRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(TentsRound.restore(0, stored.copy(answer = "6:1"), { clock }))
        assertNull(TentsRound.restore(0, stored.copy(answer = "6:1:1"), { clock }))
        assertNull(TentsRound.restore(0, stored.copy(guesses = listOf("x3")), { clock }))
        assertNull(TentsRound.restore(0, stored.copy(guesses = listOf("t")), { clock }))
        assertNull(TentsRound.restore(0, stored.copy(guesses = listOf("tx")), { clock }))
    }

    @Test
    fun aSolvedRoundIsAWinWhoseOnlyGuessIsTheNumberOfTaps() {
        val r = round()
        clock = 5_000
        r.finish()
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.Tents, finished.game)
        assertEquals(listOf("8"), finished.guesses)
        assertEquals(8, finished.moveCount)
        assertTrue(finished.answer.length <= 32, "the table limits answer to 32 characters: ${finished.answer.length}")
        assertEquals(7_000L, finished.durationMs)
    }

    @Test
    fun theGuidePointsAtATentOfTheSolutionAndAtAWrongTentFirst() {
        val r = round()
        val step = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(GuideReason.TentsPlace, step.reason)
        val cell = step.cells.single()
        assertTrue(cell in puzzle.tents)
        assertEquals(listOf(cell / 6 + 1, cell % 6 + 1), step.numbers)

        // A tent that is not in the solution is to be taken away before anything else.
        val wrong = (0 until 36).first { it !in puzzle.tents && it !in puzzle.trees }
        r.tap(wrong)
        val remove = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(GuideReason.TentsRemove, remove.reason)
        assertEquals(wrong, remove.cells.single())
        r.tap(wrong); r.tap(wrong)
        r.finish()
        assertNull(r.guideTask())
    }
}
