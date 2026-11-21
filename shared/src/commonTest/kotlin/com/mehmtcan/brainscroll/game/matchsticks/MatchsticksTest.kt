package com.mehmtcan.brainscroll.game.matchsticks

import com.mehmtcan.brainscroll.game.Difficulty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchsticksTest {

    private fun sum(first: Int, plus: Boolean, second: Int, result: Int) = MatchRules.cellsOf(first, plus, second, result)

    @Test
    fun theDigitsAndSignsReadBackAndTheSumIsChecked() {
        for (d in 0..9) assertEquals(d, MatchRules.digitOf(MatchRules.digitMask(d)))
        assertTrue(MatchRules.isTrue(sum(3, true, 4, 7)))
        assertTrue(MatchRules.isTrue(sum(9, false, 4, 5)))
        assertTrue(MatchRules.isTrue(sum(8, true, 9, 17)))
        assertFalse(MatchRules.isTrue(sum(3, true, 4, 8)))
        // A sign with only the upright stick, or a digit shape that is not one, does not read.
        assertNull(MatchRules.read(listOf(MatchRules.digitMask(3), 2, MatchRules.digitMask(4), MatchRules.digitMask(7))))
        assertNull(MatchRules.read(listOf(0b0000011, MatchRules.PLUS, MatchRules.digitMask(4), MatchRules.digitMask(7))))
        // A two-digit result does not start with a 0.
        assertNull(MatchRules.read(listOf(MatchRules.digitMask(3), MatchRules.PLUS, MatchRules.digitMask(4), MatchRules.digitMask(0), MatchRules.digitMask(7))))
    }

    @Test
    fun aMoveTakesOneStickAndLaysItOnAnEmptyPlace() {
        val cells = sum(6, false, 4, 2)
        val moves = MatchRules.moves(cells)
        val sticks = cells.sumOf { it.countOneBits() }
        for (m in moves) {
            val next = MatchRules.apply(cells, m)
            assertEquals(sticks, next.sumOf { it.countOneBits() }, "no stick is lost or made")
            assertTrue(next != cells)
        }
        // A move from a place with no stick, or onto a taken place, is not on the list.
        assertTrue(moves.none { (cells[it.from.cell] shr it.from.bit) and 1 == 0 })
        assertTrue(moves.none { (cells[it.to.cell] shr it.to.bit) and 1 == 1 })
    }

    @Test
    fun everyBrokenSumHasItsWayBackInItsWinningMoves() {
        var checked = 0
        for (a in 0..9) for (b in 0..9) for (plus in listOf(true, false)) {
            val c = if (plus) a + b else a - b
            if (c !in 0..9) continue
            val right = sum(a, plus, b, c)
            for (m in MatchRules.moves(right)) {
                val broken = MatchRules.apply(right, m)
                if (!MatchRules.isValid(broken) || MatchRules.isTrue(broken)) continue
                val back = MatchMove(m.to, m.from)
                assertTrue(back in MatchRules.winningMoves(broken), "$a $plus $b = $c")
                checked++
            }
        }
        assertTrue(checked > 100, "there are plenty of sums to make: $checked")
    }

    @Test
    fun theBoardIsSavedAsTextAndReadBack() {
        for (seed in 0 until 40) for (level in Difficulty.entries) {
            val puzzle = MatchGenerator.generate(seed, level).puzzle
            val text = puzzle.encode()
            assertTrue(text.length <= 6, text)
            assertEquals(puzzle, MatchPuzzle.decode(text))
        }
        assertNull(MatchPuzzle.decode("!!"))
        assertNull(MatchPuzzle.decode(""))
    }

    @Test
    fun everyGeneratedSumIsWrongReadsAsASumAndHasExactlyOneWayToPutItRight() {
        for (level in Difficulty.entries) for (seed in 0 until 150) {
            val board = MatchGenerator.generate(seed, level)
            val cells = board.puzzle.cells
            assertTrue(MatchRules.isValid(cells), "reads as a sum")
            assertFalse(MatchRules.isTrue(cells), "is wrong")
            assertEquals(listOf(board.solution), MatchRules.winningMoves(cells), "one way only")
        }
    }

    @Test
    fun theLevelsDecideWhereTheStickGoesAndHowLongTheResultIs() {
        for (seed in 0 until 100) {
            val easy = MatchGenerator.generate(seed, Difficulty.Easy)
            assertTrue(MatchRules.staysInSymbol(easy.solution), "easy: inside one symbol")
            assertEquals(MatchPuzzle.SHORT, easy.puzzle.cells.size)
            val medium = MatchGenerator.generate(seed, Difficulty.Medium)
            assertFalse(MatchRules.staysInSymbol(medium.solution), "medium: to another symbol")
            assertEquals(MatchPuzzle.SHORT, medium.puzzle.cells.size)
            val hard = MatchGenerator.generate(seed, Difficulty.Hard)
            assertFalse(MatchRules.staysInSymbol(hard.solution), "hard: to another symbol")
            assertEquals(MatchPuzzle.LONG, hard.puzzle.cells.size)
        }
    }

    @Test
    fun theSameSeedMakesTheSameSum() {
        for (level in Difficulty.entries) assertEquals(MatchGenerator.generate(7, level).puzzle, MatchGenerator.generate(7, level).puzzle)
    }

    @Test
    fun movingTheRightStickWinsAndAWrongOneHasToBePutBack() {
        val board = MatchGenerator.generate(3, Difficulty.Medium)
        val game = MatchGame(board.puzzle)
        assertEquals(MatchStatus.Playing, game.state.status)
        assertFalse(game.state.canUndo)
        // A move that does not win.
        val wrong = MatchRules.moves(board.puzzle.cells).first { it != board.solution && !MatchRules.isTrue(MatchRules.apply(board.puzzle.cells, it)) }
        assertEquals(MatchResult.Moved, game.move(wrong))
        assertTrue(game.state.moved)
        assertTrue(game.state.canUndo)
        assertEquals(1, game.state.moves)
        // Until it is put back there is no other move.
        assertEquals(MatchResult.Ignored, game.move(board.solution))
        assertTrue(game.undo())
        assertEquals(board.puzzle.cells, game.state.cells)
        assertEquals(1, game.state.moves, "the move stays counted")
        assertFalse(game.undo(), "nothing to put back")
        assertEquals(MatchResult.Solved, game.move(board.solution))
        assertEquals(MatchStatus.Won, game.state.status)
        assertTrue(game.isFinished)
        assertEquals(2, game.state.moves)
        assertEquals(MatchResult.Ignored, game.move(wrong), "the game is over")
        assertFalse(game.undo())
    }

    @Test
    fun movesThatCannotBeMadeAreIgnored() {
        val puzzle = MatchGenerator.generate(5, Difficulty.Easy).puzzle
        val game = MatchGame(puzzle)
        val cells = puzzle.cells
        val taken = MatchSlot(0, cells[0].countTrailingZeroBits())
        val empty = (0 until 7).first { (cells[0] shr it) and 1 == 0 }
        assertEquals(MatchResult.Ignored, game.move(MatchMove(MatchSlot(0, empty), MatchSlot(2, 0))), "no stick there")
        assertEquals(MatchResult.Ignored, game.move(MatchMove(taken, taken)), "onto its own place")
        assertEquals(MatchResult.Ignored, game.move(MatchMove(MatchSlot(9, 0), MatchSlot(0, empty))), "no such symbol")
        assertEquals(MatchResult.Ignored, game.move(MatchMove(MatchSlot(1, 5), MatchSlot(0, empty))), "no such place")
        assertEquals(0, game.state.moves)
    }

    @Test
    fun aSavedGameIsRestoredOnlyIfItCouldHaveHappened() {
        val board = MatchGenerator.generate(11, Difficulty.Medium)
        val moved = MatchRules.apply(board.puzzle.cells, board.solution)
        val game = MatchGame(board.puzzle)
        assertTrue(game.restore(moved, 4))
        assertEquals(4, game.state.moves)
        assertTrue(game.isFinished)
        // A stick more or fewer, two sticks moved, the wrong number of symbols or a negative count.
        assertFalse(MatchGame(board.puzzle).restore(board.puzzle.cells.mapIndexed { i, m -> if (i == 0) m xor 1 else m }, 1))
        assertFalse(MatchGame(board.puzzle).restore(moved.take(3), 1))
        assertFalse(MatchGame(board.puzzle).restore(moved, -1))
        // A second, different stick moved on top of the first (not the same stick again, and not one laid on the first one's old place).
        val other = MatchRules.moves(moved).first { it.from != board.solution.to && it.to != board.solution.from }
        val twice = MatchRules.apply(moved, other)
        assertFalse(MatchGame(board.puzzle).restore(twice, 2))
        val untouched = MatchGame(board.puzzle)
        assertTrue(untouched.restore(board.puzzle.cells, 0))
        assertNotNull(untouched.state)
    }
}
