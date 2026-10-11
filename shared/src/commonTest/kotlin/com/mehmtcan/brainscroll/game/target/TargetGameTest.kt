package com.mehmtcan.brainscroll.game.target

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TargetGameTest {

    /** The example: 2, 3, 6, 7 and the target 48 (6*2, 7-3, then 12*4). */
    private fun game() = TargetGame(listOf(2, 3, 6, 7), 48, 3)

    private fun TargetGame.values() = state.tokens.map { it.value }

    @Test
    fun aGameStartsWithTheNumbersOnTheTable() {
        val state = game().state
        assertEquals(listOf(2, 3, 6, 7), state.tokens.map { it.value })
        assertEquals(listOf(0, 1, 2, 3), state.tokens.map { it.id })
        assertEquals(48, state.target)
        assertEquals(3, state.par)
        assertEquals(0, state.moves)
        assertEquals(TargetStatus.Playing, state.status)
        assertFalse(state.canUndo)
        assertFalse(state.canRestart)
    }

    @Test
    fun aMoveReplacesTheTwoNumbersByTheResultWithANewId() {
        val g = game()
        assertEquals(TargetResult.Combined, g.combine(2, Op.Mul, 0))
        assertEquals(listOf(3, 7, 12), g.values())
        assertEquals(listOf(1, 3, 4), g.state.tokens.map { it.id })
        assertEquals(listOf(Step(6, Op.Mul, 2, 12)), g.state.steps)
        assertEquals(1, g.state.moves)
    }

    @Test
    fun resultsCanBeUsedAgainAndTheExampleWins() {
        val g = game()
        g.combine(2, Op.Mul, 0)
        g.combine(3, Op.Sub, 1)
        assertEquals(listOf(12, 4), g.values())
        assertEquals(TargetResult.Solved, g.combine(4, Op.Mul, 5))
        assertEquals(TargetStatus.Won, g.state.status)
        assertTrue(g.isFinished)
        assertEquals(3, g.state.moves)
    }

    @Test
    fun notAllNumbersHaveToBeUsed() {
        val g = TargetGame(listOf(5, 9, 2, 1), 45, 1)
        assertEquals(TargetResult.Solved, g.combine(0, Op.Mul, 1))
        assertEquals(listOf(2, 1, 45), g.values(), "two numbers are left unused beside the result")
    }

    @Test
    fun theOrderOfASubtractionAndADivisionMatters() {
        val g = TargetGame(listOf(8, 2, 5, 3), 99, 4)
        assertEquals(6, g.preview(0, Op.Sub, 1))
        assertNull(g.preview(1, Op.Sub, 0))
        assertEquals(4, g.preview(0, Op.Div, 1))
        assertNull(g.preview(1, Op.Div, 0))
        assertEquals(10, g.preview(0, Op.Add, 1))
        assertEquals(10, g.preview(1, Op.Add, 0))
    }

    @Test
    fun movesThatAreNotAllowedAreIgnoredAndNotCounted() {
        val g = game()
        assertEquals(TargetResult.Ignored, g.combine(0, Op.Sub, 1), "2-3 goes below zero")
        val h = game()
        assertEquals(TargetResult.Ignored, h.combine(3, Op.Div, 0), "7/2 leaves a rest")
        assertEquals(TargetResult.Ignored, h.combine(0, Op.Add, 0), "a number can not be used with itself")
        assertEquals(TargetResult.Ignored, h.combine(0, Op.Add, 9), "there is no such number")
        assertEquals(0, h.state.moves)
        assertEquals(listOf(2, 3, 6, 7), h.values())
    }

    @Test
    fun aNumberIsUsedOnlyOnce() {
        val g = game()
        g.combine(0, Op.Add, 1)
        assertEquals(TargetResult.Ignored, g.combine(0, Op.Add, 2), "the 2 is gone, it is part of the 5 now")
        assertEquals(listOf(6, 7, 5), g.values())
    }

    @Test
    fun undoTakesTheLastOperationBackButTheMoveStaysCounted() {
        val g = game()
        g.combine(2, Op.Mul, 0)
        g.combine(3, Op.Sub, 1)
        assertTrue(g.state.canUndo)
        assertTrue(g.undo())
        assertEquals(listOf(3, 7, 12), g.values())
        assertEquals(1, g.state.steps.size)
        assertEquals(2, g.state.moves)
        // The ids are handed out again in the same order, so a saved game replays the same.
        g.combine(1, Op.Add, 3)
        assertEquals(listOf(12, 10), g.values())
        assertEquals(listOf(4, 5), g.state.tokens.map { it.id })
    }

    @Test
    fun restartPutsEverythingBackAndKeepsTheMoves() {
        val g = game()
        g.combine(2, Op.Mul, 0)
        g.combine(3, Op.Sub, 1)
        assertTrue(g.restart())
        assertEquals(listOf(2, 3, 6, 7), g.values())
        assertTrue(g.state.steps.isEmpty())
        assertEquals(2, g.state.moves)
        assertFalse(g.state.canRestart)
        assertFalse(g.restart())
        assertFalse(g.undo())
    }

    @Test
    fun aFinishedGameTakesNothingMore() {
        val g = TargetGame(listOf(5, 9, 2, 1), 45, 1)
        g.combine(0, Op.Mul, 1)
        assertEquals(TargetResult.Ignored, g.combine(2, Op.Add, 3))
        assertFalse(g.undo())
        assertFalse(g.restart())
        assertNull(g.preview(2, Op.Add, 3))
    }

    @Test
    fun aGameNeedsSaneSettings() {
        assertFailsWith<IllegalArgumentException> { TargetGame(listOf(5), 10, 1) }
        assertFailsWith<IllegalArgumentException> { TargetGame(listOf(5, 0), 10, 1) }
        assertFailsWith<IllegalArgumentException> { TargetGame(listOf(5, 6), 5, 1) }
    }
}
