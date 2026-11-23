package com.mehmtcan.brainscroll.game.crosssum

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.guide.CrossGuide
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrossGameTest {

    /** The board of the how to card: 9 7 1 / 8 9 6 / 1 4 2 with no digit given, all cells white (sums 17, 23, 7 across and 18, 20, 9 down). */
    private val small = CrossPuzzle(3, 3, List(9) { true }, listOf(9, 7, 1, 8, 9, 6, 1, 4, 2), List(9) { false })

    @Test
    fun theBlocksOfABoardAndTheirSumsComeFromTheAnswer() {
        val runs = small.runs
        assertEquals(6, runs.size)
        assertEquals(listOf(17, 23, 7), runs.filter { it.across }.map { it.sum })
        assertEquals(listOf(18, 20, 9), runs.filter { !it.across }.map { it.sum })
        // The sum of the first block along a row is written just to the left of it, on the frame.
        val first = runs.first { it.across }
        assertEquals(1 to 0, first.clueRow to first.clueColumn)
        val down = runs.first { !it.across }
        assertEquals(0 to 1, down.clueRow to down.clueColumn)
        assertEquals(2, small.runsOf(4).size)
    }

    @Test
    fun aBoardWithAWhiteCellOnItsOwnOrADigitTwiceInABlockIsRefused() {
        val white = listOf(true, false, true, true, true, true, true, true, true)
        runCatching { CrossPuzzle(3, 3, white, listOf(1, 0, 2, 3, 4, 5, 6, 7, 8), List(9) { false }).runs }.let { assertTrue(it.isFailure, "cell 0 stands alone in its row") }
        runCatching { CrossPuzzle(3, 3, List(9) { true }, listOf(1, 1, 3, 4, 5, 6, 7, 8, 9), List(9) { false }).runs }.let { assertTrue(it.isFailure, "a digit twice") }
    }

    @Test
    fun puttingTheAnswerWinsAndAWrongDigitDoesNot() {
        val game = CrossGame(small)
        small.whiteCells.dropLast(1).forEach { assertEquals(CrossResult.Placed, game.put(it, small.solution[it])); assertFalse(game.isFinished) }
        assertEquals(CrossResult.Placed, game.put(8, 3), "a wrong digit is allowed, the board is not won")
        assertFalse(game.isFinished)
        assertEquals(CrossResult.Solved, game.put(8, 2))
        assertTrue(game.isFinished)
        assertEquals(CrossResult.Ignored, game.put(0, 1), "a won board is closed")
    }

    @Test
    fun theSameDigitAgainDoesNothingAndZeroEmptiesTheCell() {
        val game = CrossGame(small)
        assertEquals(CrossResult.Placed, game.put(0, 4))
        assertEquals(CrossResult.Ignored, game.put(0, 4))
        assertEquals(CrossResult.Cleared, game.put(0, 0))
        assertEquals(CrossResult.Ignored, game.put(0, 0))
        assertEquals(CrossResult.Ignored, game.put(0, 10))
        assertEquals(CrossResult.Ignored, game.put(99, 1))
        assertEquals(2, game.state.moves)
    }

    @Test
    fun aGivenCellCannotBeChangedAndStartsFilled() {
        val given = CrossPuzzle(3, 3, List(9) { true }, small.solution, List(9) { it == 4 })
        val game = CrossGame(given)
        assertEquals(9, game.state.values[4])
        assertEquals(CrossResult.Ignored, game.put(4, 1))
        assertEquals(1, game.state.filled)
        assertFalse(game.restart(), "the given digit stays, nothing else was put")
    }

    @Test
    fun aBlockThatBreaksARuleIsMarked() {
        val game = CrossGame(small)
        game.put(0, 9); game.put(1, 9)
        assertEquals(setOf(0, 1, 2), game.state.conflicts.filter { it < 3 }.toSet(), "a digit twice marks the block along the row")
        game.put(1, 7)
        assertTrue(game.state.conflicts.none { it < 3 }, "9 and 7 are different and under 17: nothing is marked in that block")
        val over = CrossGame(small)
        over.put(0, 9); over.put(1, 8); over.put(2, 7)
        assertTrue(0 in over.state.conflicts, "17 is passed: 9 + 8 + 7")
        val full = CrossGame(small)
        full.put(0, 9); full.put(1, 7); full.put(2, 2)
        assertTrue(1 in full.state.conflicts, "the block is full and adds up to 18, not 17")
    }

    @Test
    fun restartTakesOutWhatThePlayerPut() {
        val game = CrossGame(small)
        assertFalse(game.restart())
        game.put(0, 4); game.put(5, 6)
        assertTrue(game.restart())
        assertEquals(0, game.state.filled)
    }

    @Test
    fun restoreRejectsWhatCannotBeAGameOfThisBoard() {
        val game = CrossGame(small)
        assertFalse(game.restore(List(8) { 0 }, 0), "wrong size")
        assertFalse(game.restore(List(9) { 10 }, 9), "a digit above 9")
        assertFalse(game.restore(List(9) { 1 }, 2), "fewer moves than digits")
        assertFalse(game.restore(small.solution, 9), "a board that is already solved")
        assertTrue(game.restore(List(9) { if (it == 3) 5 else 0 }, 4))
        assertEquals(4, game.state.moves)
        assertEquals(5, game.state.values[3])
    }

    private fun round(puzzle: CrossPuzzle, clock: () -> Long = { 0L }) = CrossRound(0, "c1", puzzle, Language.EN, clock)

    @Test
    fun aRoundIsSavedAndRebuiltAsTheyLeftIt() {
        var time = 1_000L
        val puzzle = CrossGenerator.generate(4, Difficulty.Medium)
        val r = round(puzzle) { time }
        r.onShown()
        val cell = puzzle.openCells[1]
        r.put(cell, 7)
        time += 6_000
        r.onHidden()
        val stored = r.toStored()
        assertEquals(GameKind.CrossSum, stored.game)
        assertEquals(puzzle.encode(), stored.answer)
        assertTrue(stored.answer.length <= 32)
        val back = assertNotNull(CrossRound.restore(2, stored) { time })
        assertEquals(2, back.index)
        assertEquals(7, back.snapshot().state.values[cell])
        assertEquals(6_000, back.snapshot().durationMs)
        assertFalse(back.isUntouched)
    }

    @Test
    fun aSavedRoundThatCannotBeReadIsDropped() {
        val puzzle = CrossGenerator.generate(1, Difficulty.Easy)
        val stored = round(puzzle).also { it.put(puzzle.openCells.first(), 3) }.toStored()
        assertNull(CrossRound.restore(0, stored.copy(answer = "")) { 0 })
        assertNull(CrossRound.restore(0, stored.copy(guesses = listOf("x1"))) { 0 })
        assertNull(CrossRound.restore(0, stored.copy(guesses = listOf("n1", "m1"))) { 0 }, "too few cells")
        assertNull(CrossRound.restore(0, stored.copy(guesses = listOf("n1", "m" + "x".repeat(puzzle.openCells.size)))) { 0 })
    }

    @Test
    fun theTimeIsThePageTimeAndStopsWhenTheBoardIsSolved() {
        var time = 0L
        val puzzle = CrossGenerator.generate(2, Difficulty.Easy)
        val r = round(puzzle) { time }
        r.onShown()
        time += 25_000
        puzzle.openCells.forEach { time += 1_000; r.put(it, puzzle.solution[it]) }
        assertTrue(r.isFinished)
        time += 600_000
        assertEquals(25_000L + puzzle.openCells.size * 1_000L, r.snapshot().durationMs, "looking at the board counts, the clock stops at the end")
        val finished = r.toFinished(9)
        assertEquals(Outcome.WON, finished.outcome)
        assertEquals(Mode.ENDLESS, finished.mode)
        assertEquals(GameKind.CrossSum, finished.game)
        assertEquals(puzzle.openCells.size, finished.moveCount)
    }

    @Test
    fun followingTheHintsSolvesEveryBoardWithTheRightDigits() {
        for (level in Difficulty.entries) for (seed in 0 until 12) {
            val puzzle = CrossGenerator.generate(seed, level)
            val game = CrossGame(puzzle)
            var guard = 0
            while (!game.isFinished && guard++ < 60) {
                val step = assertNotNull(CrossGuide.step(game.state), "$level $seed: a step")
                assertEquals(GuideReason.CrossDigit, step.reason)
                val cell = step.cells.single()
                val digit = step.numbers.single()
                assertEquals(puzzle.solution[cell], digit, "$level $seed: the hint is the answer")
                assertTrue(game.state.values[cell] == 0, "an empty cell")
                val near = puzzle.runsOf(cell).flatMap { it.cells }.toSet()
                assertTrue(step.evidence.all { it in near && it != cell }, "the evidence is in the blocks of the cell")
                assertNotEquals(CrossResult.Ignored, game.put(cell, digit))
            }
            assertTrue(game.isFinished, "$level $seed: the hints finish the board")
        }
    }

    @Test
    fun aWrongDigitIsTakenOutBeforeAnythingElse() {
        val puzzle = CrossGenerator.generate(3, Difficulty.Medium)
        val game = CrossGame(puzzle)
        val cell = puzzle.openCells.first()
        game.put(cell, puzzle.solution[cell] % 9 + 1)
        val step = assertNotNull(CrossGuide.step(game.state))
        assertEquals(GuideReason.CrossClear, step.reason)
        assertEquals(listOf(cell), step.cells)
    }

    @Test
    fun thereIsNoStepOnASolvedBoard() {
        val puzzle = CrossGenerator.generate(1, Difficulty.Easy)
        val game = CrossGame(puzzle)
        puzzle.openCells.forEach { game.put(it, puzzle.solution[it]) }
        assertNull(CrossGuide.step(game.state))
    }

    @Test
    fun theReasoningNeverContradictsTheAnswerOnRandomPartialBoards() {
        // Sound reasoning: from digits that are right, the candidates of every cell still hold the digit of the answer.
        val random = kotlin.random.Random(5)
        for (level in Difficulty.entries) for (seed in 0 until 10) {
            val puzzle = CrossGenerator.generate(seed, level)
            repeat(10) {
                val start = IntArray(puzzle.white.size) { if (puzzle.white[it] && random.nextInt(3) == 0) puzzle.solution[it] else 0 }
                val cand = assertNotNull(CrossSolver.candidates(puzzle, start))
                for (cell in puzzle.whiteCells) assertTrue(cand[cell] and (1 shl puzzle.solution[cell]) != 0, "$level $seed cell $cell lost its digit")
            }
        }
    }
}
