package com.mehmtcan.brainscroll.game.lamps

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LampGeneratorTest {
    /** The board of the how to card: walls on 0, 5 and 15 (the last with a 2); the answer has lamps on 1, 4, 11 and 14. */
    private val demo = LampPuzzle(
        4,
        listOf(
            LampPuzzle.BLANK_WALL, LampPuzzle.WHITE, LampPuzzle.WHITE, LampPuzzle.WHITE,
            LampPuzzle.WHITE, LampPuzzle.BLANK_WALL, LampPuzzle.WHITE, LampPuzzle.WHITE,
            LampPuzzle.WHITE, LampPuzzle.WHITE, LampPuzzle.WHITE, LampPuzzle.WHITE,
            LampPuzzle.WHITE, LampPuzzle.WHITE, LampPuzzle.WHITE, 2,
        ),
    )

    private fun lampsOf(vararg cells: Int) = BooleanArray(16) { it in cells }

    @Test
    fun everyLevelMakesBoardsWithExactlyOneSolution() {
        for (difficulty in Difficulty.entries) {
            val shape = GameLevels.lamps(difficulty)
            for (seed in 0 until 20) {
                val puzzle = LampGenerator.generate(seed, difficulty)
                assertEquals(shape.size, puzzle.size)
                assertEquals(shape.walls, puzzle.wallCount)
                val solutions = LampSolver.solutions(puzzle, 3)
                assertEquals(1, solutions.size, "$difficulty seed $seed has one solution")
                assertTrue(LampRules.isSolved(puzzle, solutions[0]), "$difficulty seed $seed")
                assertTrue(solutions[0].any { it }, "a board with no lamp is no puzzle")
            }
        }
    }

    @Test
    fun aBoardCanBeLitTheWayAPlayerDoesIt() {
        for (difficulty in Difficulty.entries) {
            val shape = GameLevels.lamps(difficulty)
            for (seed in 0 until 15) {
                val puzzle = LampGenerator.generate(seed, difficulty)
                val byLogic = assertNotNull(LampSolver.solveByLogic(puzzle, shape.lookahead), "$difficulty seed $seed")
                assertContentEquals(LampSolver.uniqueSolution(puzzle), byLogic)
            }
        }
    }

    @Test
    fun theSameSeedMakesTheSameBoard() {
        for (difficulty in Difficulty.entries) assertEquals(LampGenerator.generate(7, difficulty), LampGenerator.generate(7, difficulty))
    }

    @Test
    fun aBoardIsSavedAndReadBackInAtMost32Characters() {
        for (difficulty in Difficulty.entries) {
            for (seed in 0 until 20) {
                val puzzle = LampGenerator.generate(seed, difficulty)
                val text = puzzle.encode()
                assertTrue(text.length <= 32, "$difficulty seed $seed: ${text.length} characters")
                assertEquals(puzzle, LampPuzzle.decode(text))
            }
        }
        assertNull(LampPuzzle.decode(""))
        assertNull(LampPuzzle.decode("!!!"))
        assertNull(LampPuzzle.decode("000"), "a board that runs out")
    }

    @Test
    fun theBiggestBoardWithTheMostWallsFitsTheSavedAnswer() {
        val n = LampPuzzle.MAX_SIZE
        val cells = List(n * n) { if (it < LampPuzzle.maxWalls(n)) LampPuzzle.BLANK_WALL else LampPuzzle.WHITE }
        assertTrue(LampPuzzle(n, cells).encode().length <= 32)
        assertTrue(runCatching { LampPuzzle(n, List(n * n) { if (it <= LampPuzzle.maxWalls(n)) LampPuzzle.BLANK_WALL else LampPuzzle.WHITE }) }.isFailure)
    }

    @Test
    fun aBoardThatCannotBeIsRefused() {
        fun refused(block: () -> LampPuzzle) = runCatching(block).isFailure
        assertTrue(refused { LampPuzzle(3, List(9) { LampPuzzle.WHITE }) })
        assertTrue(refused { LampPuzzle(11, List(121) { LampPuzzle.WHITE }) })
        assertTrue(refused { LampPuzzle(4, List(15) { LampPuzzle.WHITE }) })
        assertTrue(refused { LampPuzzle(4, List(16) { 9 }) })
    }

    @Test
    fun theHowToBoardHasOneAnswer() {
        assertContentEquals(lampsOf(1, 4, 11, 14), LampSolver.uniqueSolution(demo))
        assertContentEquals(lampsOf(1, 4, 11, 14), LampSolver.solveByLogic(demo, lookahead = false))
    }

    @Test
    fun aBoardWithTwoWaysIsNotUnique() {
        // A board with no walls at all: any lamps that do not see each other and light everything will do, and there are several.
        val open = LampPuzzle(4, List(16) { LampPuzzle.WHITE })
        assertTrue(LampSolver.solutions(open, 5).size > 1)
        assertNull(LampSolver.uniqueSolution(open))
    }

    @Test
    fun aLampLightsItsRowAndColumnUpToTheWalls() {
        assertEquals(listOf(13, 12, 10, 6, 2).sorted(), demo.seenFrom(14).sorted())
        val lit = LampRules.lit(demo, lampsOf(14))
        for (cell in listOf(14, 13, 12, 10, 6, 2)) assertTrue(lit[cell], "cell $cell is lit")
        for (cell in listOf(1, 3, 4, 7, 8, 9, 11)) assertFalse(lit[cell], "cell $cell is not lit")
    }

    @Test
    fun theRulesSeeLampsThatSeeEachOtherAndNumbersThatAreOff() {
        assertEquals(setOf(13, 14), LampRules.clashing(demo, lampsOf(13, 14)))
        assertTrue(LampRules.clashing(demo, lampsOf(14, 11)).isEmpty())
        assertEquals(2, LampRules.lampsAround(demo, lampsOf(11, 14), 15))
        assertTrue(15 in LampRules.satisfied(demo, lampsOf(11, 14)))
        // A wall with a 1 and a lamp on both of its free sides has too many.
        val one = LampPuzzle(4, List(16) { if (it == 0) 1 else LampPuzzle.WHITE })
        assertEquals(setOf(0), LampRules.overfull(one, BooleanArray(16) { it == 1 || it == 4 }))
        assertTrue(LampRules.overfull(one, BooleanArray(16) { it == 1 }).isEmpty())
        assertTrue(LampRules.isSolved(demo, lampsOf(1, 4, 11, 14)))
        assertFalse(LampRules.isSolved(demo, lampsOf(1, 4, 11)), "a cell is dark and the 2 has one lamp")
        assertFalse(LampRules.isSolved(demo, lampsOf(1, 4, 11, 14, 0)), "a lamp on a wall")
    }

    @Test
    fun theRulesForceTheCellsThePlayerSees() {
        // The 2 on the wall at the corner has two free cells next to it: both get lamps.
        val state = IntArray(16)
        val forced = LampSolver.forcedOnce(demo, state)
        assertTrue((11 to LampSolver.LAMP) in forced)
        assertTrue((14 to LampSolver.LAMP) in forced)
        // A lamp lights its row, so no other lamp can go there.
        state[14] = LampSolver.LAMP
        assertTrue((13 to LampSolver.NONE) in LampSolver.forcedOnce(demo, state))
    }
}
