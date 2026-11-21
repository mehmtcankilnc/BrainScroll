package com.mehmtcan.brainscroll.game.untangle

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UntangleTest {

    // Four dots in the corners of a square: 0 top left, 1 top right, 2 bottom right, 3 bottom left.
    private val square = listOf(100, 100, 900, 100, 900, 900, 100, 900)

    @Test
    fun twoLinesCrossOnlyWhenTheyMeetAtAPointThatIsNoDotOfEither() {
        // The two diagonals of the square cross; two opposite sides, and two sides that share a dot, do not.
        assertTrue(UntangleRules.cross(square, 0, 2, 1, 3))
        assertFalse(UntangleRules.cross(square, 0, 1, 3, 2))
        assertFalse(UntangleRules.cross(square, 0, 1, 1, 2))
        // A dot lying exactly on the other line does not count as a crossing (no two dots can be on the same place).
        val touching = listOf(100, 500, 900, 500, 500, 500, 500, 900)
        assertFalse(UntangleRules.cross(touching, 0, 1, 2, 3))
        val edges = listOf(0 to 2, 1 to 3, 0 to 1)
        assertEquals(setOf(0, 1), UntangleRules.crossingEdges(square, edges))
        assertEquals(1, UntangleRules.crossingPairs(square, edges))
    }

    @Test
    fun theBoardIsSavedAsTextWithinTheLimitAndReadBack() {
        for (level in Difficulty.entries) for (seed in 0 until 30) {
            val puzzle = UntangleGenerator.generate(seed, level).puzzle
            val text = puzzle.encode()
            assertTrue(text.length <= 32, "$level $text is ${text.length} long")
            assertEquals(puzzle, UntanglePuzzle.decode(text))
        }
        assertNull(UntanglePuzzle.decode("!!"))
        assertNull(UntanglePuzzle.decode(""))
    }

    @Test
    fun everyGeneratedBoardCanBeUntangledIsJoinedAndIsTangledAtTheStart() {
        for (level in Difficulty.entries) {
            val shape = GameLevels.untangle(level)
            for (seed in 0 until 80) {
                val board = UntangleGenerator.generate(seed, level)
                val p = board.puzzle
                assertEquals(shape.nodes, p.nodeCount)
                assertTrue(p.edges.size >= shape.nodes + 2, "$level seed $seed has ${p.edges.size} lines")
                assertEquals(0, UntangleRules.crossingPairs(board.solution, p.edges), "$level seed $seed: the drawing it was made from has no crossing")
                assertTrue(UntangleRules.crossingPairs(p.start, p.edges) >= shape.minCrossings, "$level seed $seed is tangled at the start")
                val degree = IntArray(p.nodeCount)
                p.edges.forEach { degree[it.first]++; degree[it.second]++ }
                assertTrue(degree.all { it >= 2 }, "$level seed $seed: every dot has two lines at least")
                // All dots are joined.
                val seen = HashSet<Int>().also { it += 0 }
                var grown = true
                while (grown) {
                    grown = false
                    for ((a, b) in p.edges) if ((a in seen) != (b in seen)) { seen += a; seen += b; grown = true }
                }
                assertEquals(p.nodeCount, seen.size, "$level seed $seed is in one piece")
            }
        }
    }

    @Test
    fun theSameSeedMakesTheSameBoard() {
        for (level in Difficulty.entries) assertEquals(UntangleGenerator.generate(6, level).puzzle, UntangleGenerator.generate(6, level).puzzle)
    }

    @Test
    fun theEmbedderFindsADrawingWithNoCrossingEveryTime() {
        for (level in Difficulty.entries) for (seed in 0 until 40) {
            val puzzle = UntangleGenerator.generate(seed, level).puzzle
            val drawing = assertNotNull(UntangleEmbedder.embed(puzzle), "$level seed $seed")
            assertEquals(0, UntangleRules.crossingPairs(drawing, puzzle.edges))
            assertEquals(drawing, UntangleEmbedder.embed(puzzle), "the same drawing every time")
        }
    }

    @Test
    fun aDragCountsOnlyWhenADotEndsSomewhereElseAndTheBoardIsWonWhenADotIsLetGoOfWithNoCrossing() {
        val board = UntangleGenerator.generate(2, Difficulty.Easy)
        val game = UntangleGame(board.puzzle)
        assertEquals(UntangleResult.Ignored, game.drop(), "nothing was dragged")
        // A drag that ends where it began.
        val x = board.puzzle.start[0]
        val y = board.puzzle.start[1]
        assertTrue(game.move(0, x + 50, y + 50))
        assertTrue(game.isDragging)
        game.move(0, x, y)
        assertEquals(UntangleResult.Ignored, game.drop())
        assertEquals(0, game.state.moves)
        assertFalse(game.move(0, x, y), "no change is no move")
        // Every dot to its place in the drawing it was made from.
        var result = UntangleResult.Ignored
        for (node in 0 until board.puzzle.nodeCount) {
            game.move(node, board.solution[2 * node], board.solution[2 * node + 1])
            if (node < board.puzzle.nodeCount - 1) assertEquals(UntangleStatus.Playing, game.state.status, "not won in the middle of a drag or before the last drop")
            result = game.drop()
            if (result == UntangleResult.Solved) break
        }
        assertEquals(UntangleResult.Solved, result)
        assertEquals(UntangleStatus.Won, game.state.status)
        assertTrue(game.isFinished)
        assertFalse(game.move(0, 300, 300), "a won board is locked")
        assertFalse(game.restart())
    }

    @Test
    fun noCrossingInTheMiddleOfADragIsNotWonUntilTheDotIsLetGo() {
        val board = UntangleGenerator.generate(4, Difficulty.Easy)
        val game = UntangleGame(board.puzzle)
        // Put every dot but the last in its place, one drop each (the board may be won earlier than the last, then there is nothing to test).
        for (node in 0 until board.puzzle.nodeCount) {
            game.move(node, board.solution[2 * node], board.solution[2 * node + 1])
            if (node == board.puzzle.nodeCount - 1) {
                // The dot is on its place and no line crosses another, but the finger is still down.
                if (game.state.crossingPairs == 0) {
                    assertEquals(UntangleStatus.Playing, game.state.status)
                    assertFalse(game.isFinished)
                }
            }
            if (game.drop() == UntangleResult.Solved) return
        }
    }

    @Test
    fun restartPutsTheDotsBackAndTheMovesStayCounted() {
        val board = UntangleGenerator.generate(5, Difficulty.Medium)
        val game = UntangleGame(board.puzzle)
        assertFalse(game.restart(), "nothing moved yet")
        assertFalse(game.state.canRestart)
        game.move(0, 500, 500)
        assertEquals(UntangleResult.Moved, game.drop())
        assertTrue(game.state.canRestart)
        assertTrue(game.restart())
        assertEquals(board.puzzle.start, game.state.positions)
        assertEquals(1, game.state.moves)
    }

    @Test
    fun aDotCannotBeLeftOutsideTheBoard() {
        val game = UntangleGame(UntangleGenerator.generate(8, Difficulty.Easy).puzzle)
        game.move(1, -500, 5_000)
        assertEquals(UNTANGLE_MARGIN, game.state.positions[2])
        assertEquals(UNTANGLE_BOARD - UNTANGLE_MARGIN, game.state.positions[3])
        assertFalse(game.move(99, 100, 100), "no such dot")
    }

    @Test
    fun aSavedGameIsRestoredOnlyIfItCouldHaveHappened() {
        val board = UntangleGenerator.generate(9, Difficulty.Easy)
        val moved = board.puzzle.start.toMutableList().also { it[0] = 300; it[1] = 300 }
        val game = UntangleGame(board.puzzle)
        if (UntangleRules.crossingPairs(moved, board.puzzle.edges) > 0) {
            assertTrue(game.restore(moved, 3))
            assertEquals(3, game.state.moves)
            assertEquals(moved, game.state.positions)
        }
        assertFalse(UntangleGame(board.puzzle).restore(board.puzzle.start.take(4), 0), "the wrong number of dots")
        assertFalse(UntangleGame(board.puzzle).restore(board.puzzle.start.toMutableList().also { it[0] = 5 }, 0), "off the board")
        assertFalse(UntangleGame(board.puzzle).restore(board.puzzle.start, -1))
        assertFalse(UntangleGame(board.puzzle).restore(board.solution, 1), "a board with no crossing would have been won")
    }
}
