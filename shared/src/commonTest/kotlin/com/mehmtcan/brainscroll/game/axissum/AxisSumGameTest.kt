package com.mehmtcan.brainscroll.game.axissum

import com.mehmtcan.brainscroll.game.BitText
import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AxisSumGameTest {

    // 4 7 2 / 6 1 8 / 3 9 5 with the 7 and the 8 to erase: the rows make 6, 7, 17 and the columns 13, 10, 7.
    private val digits = listOf(4, 7, 2, 6, 1, 8, 3, 9, 5)
    private val puzzle = AxisSumPuzzle(3, digits, digits.indices.map { it != 1 && it != 5 })

    @Test
    fun theSumsAreWorkedOutFromTheNumbersThatStay() {
        assertEquals(listOf(6, 7, 17), puzzle.rowTargets)
        assertEquals(listOf(13, 10, 7), puzzle.columnTargets)
        assertEquals(2, puzzle.eraseCount)
    }

    @Test
    fun aTapErasesANumberAndAnotherBringsItBack() {
        val game = AxisSumGame(puzzle)
        assertEquals(AxisSumResult.Erased, game.tap(0))
        assertTrue(game.state.erased[0])
        assertEquals(9, game.state.rowSums[0])
        assertEquals(9, game.state.columnSums[0])
        assertEquals(AxisSumResult.Restored, game.tap(0))
        assertFalse(game.state.erased[0])
        assertEquals(2, game.state.taps)
        assertEquals(13, game.state.rowSums[0])
    }

    @Test
    fun theBoardIsWonWhenEverySumIsRightAndNotBefore() {
        val game = AxisSumGame(puzzle)
        assertFalse(game.isFinished)
        assertEquals(AxisSumResult.Erased, game.tap(1))
        assertFalse(game.isFinished)
        assertEquals(AxisSumResult.Solved, game.tap(5))
        assertEquals(AxisSumStatus.Won, game.state.status)
        assertEquals(AxisSumResult.Ignored, game.tap(0), "the game is over")
    }

    @Test
    fun anotherWayOfErasingThatMakesAllTheSumsAlsoWins() {
        // The board has one way; a board made by hand may have more, and the rules only look at the sums.
        val game = AxisSumGame(puzzle)
        game.tap(1)
        game.tap(5)
        assertTrue(game.isFinished)
    }

    @Test
    fun tapsOnNothingAreIgnoredAndRestartBringsTheNumbersBack() {
        val game = AxisSumGame(puzzle)
        assertEquals(AxisSumResult.Ignored, game.tap(-1))
        assertEquals(AxisSumResult.Ignored, game.tap(9))
        assertFalse(game.restart(), "nothing is erased yet")
        game.tap(0)
        game.tap(2)
        assertTrue(game.state.canRestart)
        assertTrue(game.restart())
        assertEquals(0, game.state.erasedCount)
        assertEquals(2, game.state.taps, "the taps stay counted")
    }

    @Test
    fun theSolverFindsTheOneWayAndNoOtherForThisBoard() {
        val ways = AxisSumSolver.solutions(3, digits, puzzle.rowTargets, puzzle.columnTargets, limit = 5)
        assertEquals(1, ways.size)
        assertEquals(puzzle.keep, ways.single())
        assertEquals(puzzle.keep, AxisSumSolver.unique(puzzle))
    }

    @Test
    fun aBoardWithTwoWaysIsNotUnique() {
        // Nine ones with one kept in every row and column: any of the six permutations of the diagonal fits.
        val twin = AxisSumPuzzle(3, List(9) { 1 }, List(9) { it % 4 == 0 })
        assertNull(AxisSumSolver.unique(twin))
    }

    @Test
    fun theTextOfABoardIsReadBackAndBadTextIsRefused() {
        val back = assertNotNull(AxisSumPuzzle.decode(puzzle.encode()))
        assertEquals(puzzle.digits, back.digits)
        assertEquals(puzzle.keep, back.keep)
        assertNull(AxisSumPuzzle.decode(""))
        assertNull(AxisSumPuzzle.decode("!!"))
        assertNull(AxisSumPuzzle.decode("0"))
        assertTrue(AxisSumPuzzle(6, List(36) { 9 }, List(36) { it % 2 == 0 }).encode().length <= 32)
    }

    @Test
    fun aBrokenBoardIsRefused() {
        assertFailsWith<IllegalArgumentException> { AxisSumPuzzle(3, List(8) { 1 }, List(8) { true }) }
        assertFailsWith<IllegalArgumentException> { AxisSumPuzzle(3, List(9) { 10 }, List(9) { true }) }
        assertFailsWith<IllegalArgumentException> { AxisSumPuzzle(3, List(9) { 1 }, List(9) { false }) }
    }

    @Test
    fun theBitTextWritesAndReadsBack() {
        val writer = BitText.Writer()
        writer.put(5, 3)
        writer.put(0, 2)
        writer.put(31, 5)
        val reader = assertNotNull(BitText.Reader.of(writer.text()))
        assertEquals(5, reader.take(3))
        assertEquals(0, reader.take(2))
        assertEquals(31, reader.take(5))
        assertNull(BitText.Reader.of("a b"))
    }

    @Test
    fun everyLevelHasABoardThatFitsTheTextAndTheNumbers() {
        for (level in Difficulty.entries) {
            val shape = GameLevels.axisSum(level)
            assertTrue(shape.size in AxisSumPuzzle.MIN_SIZE..AxisSumPuzzle.MAX_SIZE)
            assertTrue(shape.maxDigit in 2..9)
        }
    }
}
