package com.mehmtcan.brainscroll.game.nonogram

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

class NonogramRoundTest {

    private var clock = 1_000L

    private val puzzle = NonogramGenerator.generate(4)
    private val cells = puzzle.solution.sorted()

    private fun round() = NonogramRound(0, "n1", puzzle, Language.EN, { clock }).also { it.onShown() }

    private fun NonogramRound.finish(stepMillis: Long = 1_000) {
        for (cell in cells) {
            clock += stepMillis
            paint(cell, cell, NonogramMark.FILLED)
        }
    }

    @Test
    fun aRoundIsUntouchedUntilTheFirstStroke() {
        val r = round()
        assertTrue(r.isUntouched)
        assertEquals(GameKind.Nonogram, r.kind)
        r.paint(0, 0, NonogramMark.CROSS)
        assertTrue(!r.isUntouched)
    }

    @Test
    fun aRefusedStrokeChangesNothingNotEvenTheClock() {
        val r = round()
        r.paint(0, 0, NonogramMark.CROSS)
        clock += 4_000
        assertEquals(NonogramResult.Ignored, r.paint(0, 9, NonogramMark.CROSS), "not on one line")
        assertEquals(NonogramResult.Ignored, r.paint(0, 0, NonogramMark.CROSS), "changes nothing")
        clock += 3_000
        r.paint(1, 1, NonogramMark.CROSS)
        assertEquals(7_000, r.snapshot().durationMs)
        assertEquals(2, r.toStored().guesses.size)
    }

    @Test
    fun theTimeIsTheTimeThePageWasOnShowAndThinkingIsNotCapped() {
        clock = 10_000
        val r = round()
        r.paint(0, 0, NonogramMark.CROSS)
        assertEquals(0, r.snapshot().durationMs)
        clock += 2_500
        r.paint(1, 1, NonogramMark.CROSS)
        clock += 10 * 60_000
        r.paint(2, 2, NonogramMark.CROSS)
        assertEquals(2_500 + 10 * 60_000L, r.snapshot().durationMs)
    }

    @Test
    fun aSavedRoundIsRebuiltExactlyFromItsPictureAndStrokes() {
        val r = round()
        r.paint(0, 7, NonogramMark.FILLED)
        r.paint(1, 57, NonogramMark.CROSS)
        r.restart()
        r.paint(8, 15, NonogramMark.FILLED)
        clock += 3_000
        val stored = r.toStored()
        assertEquals(GameKind.Nonogram, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)
        assertEquals(listOf("p0-7:1", "p1-57:2", "r", "p8-15:1"), stored.guesses)

        val back = assertNotNull(NonogramRound.restore(2, stored, { clock }))
        assertEquals(2, back.index)
        assertEquals(r.snapshot().state, back.snapshot().state)
        assertEquals(r.snapshot().durationMs, back.snapshot().durationMs)
        assertEquals(stored.guesses, back.toStored().guesses)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val stored = round().toStored()
        assertNull(NonogramRound.restore(0, stored.copy(answer = "8:12:0"), { clock }))
        for (bad in listOf("x3", "p", "p1", "p1-2", "p1-2:x", "pa-2:1", "p1-b:1", "p1-2-3:1")) {
            assertNull(NonogramRound.restore(0, stored.copy(guesses = listOf(bad)), { clock }), bad)
        }
    }

    @Test
    fun aSolvedRoundIsAWinWhoseOnlyGuessIsTheNumberOfStrokes() {
        clock = 5_000
        val r = round()
        r.finish()
        assertTrue(r.isFinished)
        val finished = r.toFinished(finishedAt = 99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.Nonogram, finished.game)
        assertEquals(listOf(cells.size.toString()), finished.guesses)
        assertEquals(cells.size, finished.moveCount)
        assertTrue(finished.answer.length <= 32, "the table limits answer to 32 characters: ${finished.answer.length}")
    }

    @Test
    fun theGuideFillsACellOfThePictureAndEmptiesAWrongOneFirst() {
        val r = round()
        val step = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(GuideReason.NonogramFill, step.reason)
        val cell = step.cells.single()
        assertTrue(cell in cells)
        assertEquals(listOf(cell / 8 + 1, cell % 8 + 1), step.numbers.take(2))

        val wrong = (0 until 64).first { it !in cells }
        r.paint(wrong, wrong, NonogramMark.FILLED)
        val empty = assertNotNull(assertNotNull(r.guideTask())())
        assertEquals(GuideReason.NonogramEmpty, empty.reason)
        assertEquals(wrong, empty.cells.single())
        r.paint(wrong, wrong, NonogramMark.EMPTY)
        r.finish()
        assertNull(r.guideTask())
    }

    @Test
    fun aCrossIsNotAWrongCellForTheGuide() {
        val r = round()
        val wrong = (0 until 64).first { it !in cells }
        r.paint(wrong, wrong, NonogramMark.CROSS)
        assertEquals(GuideReason.NonogramFill, assertNotNull(r.guideTask())()?.reason)
    }
}
