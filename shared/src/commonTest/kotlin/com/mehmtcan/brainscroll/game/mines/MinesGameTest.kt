package com.mehmtcan.brainscroll.game.mines

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Plays a game to the end by logic alone, starting with [first]. Tests use it as a perfect player. */
fun MinesGame.solveByLogic(first: Int) {
    open(first)
    while (!isFinished) {
        val numbers = state.numbers.toIntArray()
        val safe = MinesSolver.deduce(rows, cols, numbers).safe.filter { numbers[it] < 0 }
        check(safe.isNotEmpty()) { "the board needs a guess" }
        safe.forEach { open(it) }
    }
}

class MinesGameTest {

    private fun game(seed: Int = 1) = MinesGame(seed = seed)

    @Test
    fun theFirstOpenIsNeverAMineAndOpensAnArea() {
        for (seed in 0 until 40) {
            val first = (seed * 7) % 64
            val g = game(seed)
            assertEquals(MinesResult.Opened, g.open(first))
            assertEquals(0, g.state.numbers[first], "the first cell has no mines around it, so it flooded")
            assertTrue(g.state.numbers.count { it >= 0 } > 1)
        }
    }

    @Test
    fun theBoardHasTheRightNumberOfMinesAndKeepsTheFirstCellAndItsNeighborsFree() {
        for (seed in 0 until 40) {
            val first = (seed * 11) % 64
            val board = MinesGame.generate(8, 8, 10, seed, first)
            assertEquals(10, board.count { it })
            assertFalse(board[first])
            assertTrue(MinesGame.neighbors(first, 8, 8).none { board[it] })
        }
    }

    @Test
    fun everyBoardCanBeFinishedByLogicAlone() {
        for (seed in 0 until 60) {
            val first = (seed * 5) % 64
            val board = MinesGame.generate(8, 8, 10, seed, first)
            assertTrue(MinesGame.solvableByLogic(8, 8, board, first), "seed $seed first $first")
            val g = game(seed)
            g.solveByLogic(first)
            assertEquals(MinesStatus.Won, g.state.status, "seed $seed")
        }
    }

    @Test
    fun theSameSeedAndFirstCellGiveTheSameBoardAndAnotherSeedDoesNot() {
        val a = MinesGame.generate(8, 8, 10, 5, 20).toList()
        assertEquals(a, MinesGame.generate(8, 8, 10, 5, 20).toList())
        assertNotEquals(a, MinesGame.generate(8, 8, 10, 6, 20).toList())
    }

    @Test
    fun openingAMineLosesAndShowsEveryMine() {
        val first = 27
        val board = MinesGame.generate(8, 8, 10, 3, first)
        val g = game(3)
        g.open(first)
        val mine = board.indices.first { board[it] }
        assertEquals(MinesResult.Exploded, g.open(mine))
        val state = g.state
        assertEquals(MinesStatus.Lost, state.status)
        assertEquals(mine, state.exploded)
        assertEquals(board.indices.filter { board[it] }.toSet(), state.mineCells)
        assertEquals(MinesResult.Ignored, g.open(0), "a lost game takes no more actions")
        assertFalse(g.toggleFlag(0))
    }

    @Test
    fun theMinesAreNotShownWhileThePlayerIsStillPlaying() {
        val g = game()
        g.open(27)
        assertTrue(g.state.mineCells.isEmpty())
        assertEquals(-1, g.state.exploded)
    }

    @Test
    fun flagsToggleOnClosedCellsAndAreNotMovesAndBlockOpening() {
        val g = game()
        assertTrue(g.toggleFlag(5))
        assertEquals(setOf(5), g.state.flagged)
        assertEquals(9, g.state.minesLeft)
        assertEquals(0, g.state.moves)
        assertEquals(MinesResult.Ignored, g.open(5), "a flagged cell is not opened by a tap")
        assertTrue(g.toggleFlag(5))
        assertTrue(g.state.flagged.isEmpty())
        // More flags than mines is allowed: the counter goes below zero.
        repeat(12) { g.toggleFlag(it) }
        assertEquals(-2, g.state.minesLeft)
    }

    @Test
    fun anOpenCellCannotBeFlagged() {
        val g = game()
        g.open(27)
        assertFalse(g.toggleFlag(27))
    }

    @Test
    fun movesCountOpensAndChordsOnly() {
        val g = game()
        g.open(27)
        assertEquals(1, g.state.moves)
        g.open(27)
        assertEquals(1, g.state.moves, "an open cell is ignored")
        g.toggleFlag(0)
        assertEquals(1, g.state.moves)
    }

    @Test
    fun aChordOpensTheOtherNeighborsWhenTheFlagsMatchTheNumber() {
        val seed = 12
        val first = 27
        val board = MinesGame.generate(8, 8, 10, seed, first)
        val g = game(seed)
        g.open(first)
        // Find an open number with unopened neighbors and flag its real mines.
        val numbers = g.state.numbers
        val cell = numbers.indices.first { numbers[it] > 0 && MinesGame.neighbors(it, 8, 8).any { n -> numbers[n] < 0 } }
        val around = MinesGame.neighbors(cell, 8, 8)
        assertEquals(MinesResult.Ignored, g.chord(cell), "no flags yet")
        around.filter { board[it] }.forEach { g.toggleFlag(it) }
        val closedBefore = around.count { g.state.numbers[it] < 0 && it !in g.state.flagged }
        assertTrue(closedBefore > 0)
        val movesBefore = g.state.moves
        assertEquals(MinesResult.Opened, g.chord(cell))
        assertEquals(movesBefore + 1, g.state.moves, "a chord is one move")
        assertTrue(around.none { g.state.numbers[it] < 0 && it !in g.state.flagged }, "all the others are open now")
    }

    @Test
    fun aChordWithAWrongFlagOpensAMineAndLoses() {
        val first = 27
        // Look for a board with an open 1 that has one real mine and another closed neighbor around it.
        for (seed in 0 until 200) {
            val board = MinesGame.generate(8, 8, 10, seed, first)
            val g = game(seed)
            g.open(first)
            val numbers = g.state.numbers
            val cell = numbers.indices.firstOrNull { c ->
                numbers[c] == 1 && MinesGame.neighbors(c, 8, 8).let { around ->
                    around.count { numbers[it] < 0 && board[it] } == 1 && around.any { numbers[it] < 0 && !board[it] }
                }
            } ?: continue
            val wrong = MinesGame.neighbors(cell, 8, 8).first { numbers[it] < 0 && !board[it] }
            g.toggleFlag(wrong)
            assertEquals(MinesResult.Exploded, g.chord(cell), "the one flag is on the wrong cell, so the mine is opened")
            assertEquals(MinesStatus.Lost, g.state.status)
            return
        }
        error("no suitable board among 200 seeds")
    }

    @Test
    fun aChordOnAClosedCellOrAnEmptyOneDoesNothing() {
        val g = game()
        g.open(27)
        assertEquals(MinesResult.Ignored, g.chord(27), "a zero has nothing to chord")
        val closed = g.state.numbers.indices.first { g.state.numbers[it] < 0 }
        assertEquals(MinesResult.Ignored, g.chord(closed))
    }

    @Test
    fun clearingTheBoardWinsAndFlagsEveryMine() {
        val g = game(9)
        g.solveByLogic(27)
        val state = g.state
        assertEquals(MinesStatus.Won, state.status)
        assertEquals(10, state.flagged.size)
        assertEquals(0, state.minesLeft)
        assertEquals(64 - 10, state.numbers.count { it >= 0 })
        assertTrue(state.mineCells.isEmpty(), "a win does not show the mines as lost ones")
    }

    @Test
    fun theSolverFindsCertainSafeCellsAndMines() {
        // A corner: the one above the closed pair is a 1 with two closed neighbors, and so on. Rows of 3, 3 columns.
        // 1 . .
        // . . .   (cell 0 is open and shows 1; its closed neighbors are 1, 3, 4)
        val numbers = intArrayOf(1, -1, -1, -1, -1, -1, -1, -1, -1)
        val deduction = MinesSolver.deduce(3, 3, numbers)
        assertTrue(deduction.safe.isEmpty() && deduction.mines.isEmpty(), "three candidates for one mine: nothing is certain")
        val certain = MinesSolver.deduce(2, 2, intArrayOf(3, -1, -1, -1))
        assertEquals(setOf(1, 2, 3), certain.mines)
        val safe = MinesSolver.deduce(2, 2, intArrayOf(0, -1, -1, -1))
        assertEquals(setOf(1, 2, 3), safe.safe)
    }

    /**
     * Plays with chords as much as possible (flagging the real mines around a number first) and checks after every
     * action that "won" means exactly "every cell that is not a mine is open". A chord whose targets overlap used to
     * count a cell twice and end the game with a safe cell still closed.
     */
    @Test
    fun theGameIsWonExactlyWhenEverySafeCellIsOpenEvenWithChords() {
        fun MinesGame.assertConsistent(context: String) {
            val openCells = state.numbers.count { it >= 0 }
            assertEquals(state.status == MinesStatus.Won, openCells == size - mineCount, "$context: $openCells open")
        }
        for (seed in 0 until 150) {
            val first = (seed * 13) % 64
            val board = MinesGame.generate(8, 8, 10, seed, first)
            val g = game(seed)
            g.open(first)
            g.assertConsistent("seed $seed after the first open")
            var guard = 0
            while (!g.isFinished && guard++ < 200) {
                val numbers = g.state.numbers
                // Chord on every number whose real mines are all flagged and that still has closed neighbors.
                for (cell in numbers.indices) {
                    if (numbers[cell] <= 0) continue
                    val around = MinesGame.neighbors(cell, 8, 8)
                    around.filter { board[it] && g.state.numbers[it] < 0 }.forEach { if (it !in g.state.flagged) g.toggleFlag(it) }
                    g.chord(cell)
                    g.assertConsistent("seed $seed chord on $cell")
                    if (g.isFinished) break
                }
                if (g.isFinished) break
                val open = g.state.numbers.toIntArray()
                val safe = MinesSolver.deduce(8, 8, open).safe.filter { open[it] < 0 }
                if (safe.isEmpty()) break
                safe.forEach { g.open(it); g.assertConsistent("seed $seed open $it") }
            }
            assertEquals(MinesStatus.Won, g.state.status, "seed $seed")
        }
    }
}
