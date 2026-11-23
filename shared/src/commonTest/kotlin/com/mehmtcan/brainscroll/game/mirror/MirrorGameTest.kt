package com.mehmtcan.brainscroll.game.mirror

import com.mehmtcan.brainscroll.game.Difficulty
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

class MirrorGameTest {

    private val sizes = listOf(4, 6, 8)

    /** A board whose given half has the painted cells [painted] (cell numbers). */
    private fun board(size: Int, axis: MirrorAxis, painted: Set<Int>) = MirrorPuzzle(size, axis, List(size * size) { it in painted })

    @Test
    fun everyCellOfTheHalfToFillHasItsPartnerInTheGivenHalf() {
        for (n in sizes) for (axis in MirrorAxis.entries) {
            val p = MirrorPuzzle.shape(n, axis)
            for (cell in 0 until n * n) {
                assertEquals(cell, p.partner(p.partner(cell)), "$axis $n: the partner of the partner is the cell")
                if (!p.isGiven(cell)) assertTrue(p.isGiven(p.partner(cell)), "$axis $n: the partner of a cell to fill is given")
            }
            assertEquals(n * n - p.fillCells.size, (0 until n * n).count { p.isGiven(it) })
            assertTrue(p.fillCells.isNotEmpty())
        }
    }

    @Test
    fun theTwoHalvesAreAboutEqualExceptOnTheDiagonal() {
        for (n in sizes) {
            assertEquals(n * n / 2, MirrorPuzzle.shape(n, MirrorAxis.Vertical).fillCells.size)
            assertEquals(n * n / 2, MirrorPuzzle.shape(n, MirrorAxis.Horizontal).fillCells.size)
            assertEquals(n * n / 2, MirrorPuzzle.shape(n, MirrorAxis.Point).fillCells.size)
            assertEquals(n * (n - 1) / 2, MirrorPuzzle.shape(n, MirrorAxis.Diagonal).fillCells.size, "the diagonal itself is given")
        }
    }

    @Test
    fun theBoardIsSavedWithAFewBitsAndReadBack() {
        for (n in sizes) for (axis in MirrorAxis.entries) {
            val puzzle = MirrorPuzzle(n, axis, MirrorPuzzle.shape(n, axis).let { bare -> List(n * n) { bare.isGiven(it) && it % 3 == 0 } })
            val text = puzzle.encode()
            assertTrue(text.length <= 32, "$n $axis: ${text.length} characters")
            assertEquals(puzzle, MirrorPuzzle.decode(text))
        }
        assertNull(MirrorPuzzle.decode(""))
        assertNull(MirrorPuzzle.decode("!!"))
    }

    @Test
    fun aTapPaintsThenEmptiesACellOfTheHalfToFill() {
        val p = board(4, MirrorAxis.Vertical, setOf(0, 5))
        val game = MirrorGame(p)
        assertEquals(MirrorResult.Painted, game.tap(3))
        assertTrue(game.state.marks[3])
        assertEquals(MirrorResult.Erased, game.tap(3))
        assertFalse(game.state.marks[3])
        assertEquals(2, game.state.moves)
    }

    @Test
    fun theGivenHalfIsNeverChanged() {
        val p = board(4, MirrorAxis.Vertical, setOf(0, 5))
        val game = MirrorGame(p)
        assertEquals(MirrorResult.Ignored, game.tap(0))
        assertEquals(MirrorResult.Ignored, game.tap(1))
        assertEquals(MirrorResult.Ignored, game.tap(99))
        assertEquals(0, game.state.moves)
        assertTrue(game.state.marks[0] && game.state.marks[5] && !game.state.marks[1])
    }

    @Test
    fun paintingEveryPartnerOfAPaintedCellWins() {
        for (axis in MirrorAxis.entries) {
            val bare = MirrorPuzzle.shape(6, axis)
            val painted = bare.pattern.indices.filter { bare.isGiven(it) && it % 4 == 1 }.toSet()
            val p = board(6, axis, painted)
            val game = MirrorGame(p)
            val toPaint = p.fillCells.filter { p.target(it) }
            assertTrue(toPaint.isNotEmpty(), axis.name)
            toPaint.dropLast(1).forEach { assertNotNull(game.tap(it)); assertFalse(game.isFinished) }
            assertEquals(MirrorResult.Solved, game.tap(toPaint.last()), axis.name)
            assertTrue(game.isFinished)
            assertEquals(MirrorResult.Ignored, game.tap(p.fillCells.first()), "a won board is closed")
        }
    }

    @Test
    fun aWrongCellStopsTheBoardFromBeingWon() {
        val p = board(4, MirrorAxis.Horizontal, setOf(0, 5))
        val game = MirrorGame(p)
        p.fillCells.filter { p.target(it) }.forEach { game.tap(it) }
        assertTrue(game.isFinished)
        val again = MirrorGame(p)
        again.tap(p.fillCells.first { !p.target(it) })
        p.fillCells.filter { p.target(it) }.forEach { again.tap(it) }
        assertFalse(again.isFinished, "one extra painted cell")
        assertEquals(1, again.state.mismatched.size)
    }

    @Test
    fun restartEmptiesTheHalfToFillButNotTheGivenOne() {
        val p = board(4, MirrorAxis.Vertical, setOf(0, 5))
        val game = MirrorGame(p)
        assertFalse(game.restart(), "nothing painted yet")
        game.tap(3)
        assertTrue(game.restart())
        assertEquals(0, game.state.painted)
        assertTrue(game.state.marks[0])
    }

    @Test
    fun restoreRejectsWhatCannotBeAGameOfThisBoard() {
        val p = board(4, MirrorAxis.Vertical, setOf(0, 5))
        val game = MirrorGame(p)
        val n = p.fillCells.size
        assertFalse(game.restore(List(n + 1) { false }, 0), "wrong size")
        assertFalse(game.restore(List(n) { true }, 2), "fewer moves than painted cells")
        assertFalse(game.restore(p.fillCells.map { p.target(it) }, n), "a board that is already solved")
        assertTrue(game.restore(List(n) { it == 0 }, 3))
        assertEquals(3, game.state.moves)
    }

    private fun round(puzzle: MirrorPuzzle, clock: () -> Long = { 0L }) = MirrorRound(0, "m1", puzzle, Language.EN, clock)

    @Test
    fun aRoundIsSavedAndRebuiltAsTheyLeftIt() {
        var time = 1_000L
        val puzzle = MirrorGenerator.generate(5, Difficulty.Medium)
        val r = round(puzzle) { time }
        r.onShown()
        val cell = puzzle.fillCells[2]
        r.tap(cell)
        time += 7_000
        r.onHidden()
        val stored = r.toStored()
        assertEquals(GameKind.Mirror, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        val back = assertNotNull(MirrorRound.restore(3, stored) { time })
        assertEquals(3, back.index)
        assertEquals(r.snapshot().state.marks, back.snapshot().state.marks)
        assertEquals(7_000, back.snapshot().durationMs)
        assertFalse(back.isUntouched)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val puzzle = MirrorGenerator.generate(5, Difficulty.Easy)
        val stored = round(puzzle).also { it.tap(puzzle.fillCells.first()) }.toStored()
        assertNull(MirrorRound.restore(0, stored.copy(answer = "")) { 0 })
        assertNull(MirrorRound.restore(0, stored.copy(guesses = listOf("x"))) { 0 })
        assertNull(MirrorRound.restore(0, stored.copy(guesses = listOf("n1", "m10x"))) { 0 })
        assertNull(MirrorRound.restore(0, stored.copy(guesses = listOf("n1", "m1"))) { 0 }, "too few cells")
    }

    @Test
    fun theTimeIsThePageTimeAndStopsWhenTheBoardIsSolved() {
        var time = 0L
        val puzzle = MirrorGenerator.generate(8, Difficulty.Easy)
        val r = round(puzzle) { time }
        r.onShown()
        time += 30_000
        val toPaint = puzzle.fillCells.filter { puzzle.target(it) }
        toPaint.forEach { time += 1_000; r.tap(it) }
        assertTrue(r.isFinished)
        time += 600_000
        assertEquals(30_000L + toPaint.size * 1_000L, r.snapshot().durationMs, "looking at the board counts, and the clock stops at the end")
        val finished = r.toFinished(99)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.Mirror, finished.game)
        assertEquals(toPaint.size, finished.moveCount)
        assertEquals(puzzle.encode(), finished.answer)
    }

    @Test
    fun aBoardThatWasOnlyLookedAtIsStillUntouched() {
        val r = round(MirrorGenerator.generate(1, Difficulty.Easy))
        assertTrue(r.isUntouched)
        r.tap(0)
        assertTrue(r.isUntouched, "a tap on the given half does nothing")
    }
}
