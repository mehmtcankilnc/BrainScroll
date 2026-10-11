package com.mehmtcan.brainscroll.game.pipes

import com.mehmtcan.brainscroll.game.pipes.PipesGenerator.EAST
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator.NORTH
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator.WEST
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PipesGameTest {

    /** A 3x3 board made by the generator that needs at least three taps. */
    private fun board(): PipesGame {
        val puzzle = PipesGenerator.generate(5, size = 3, minPar = 3)
        return PipesGame(puzzle.start, puzzle.par)
    }

    @Test
    fun aTapTurnsTheTileAQuarterClockwiseAndCountsAMove() {
        val g = board()
        val before = g.state.tiles
        assertEquals(PipesResult.Turned, g.tap(0))
        assertEquals(PipesGenerator.rotate(before[0]), g.state.tiles[0])
        assertEquals(1, g.state.moves)
        for (cell in 1 until 9) assertEquals(before[cell], g.state.tiles[cell], "only the tapped tile turns")
    }

    @Test
    fun twoSidesThatMeetAreMatchedAndASideThatMeetsNothingIsAnOpenEnd() {
        val meeting = PipesGame(intArrayOf(EAST, WEST, 0, 0, 0, 0, 0, 0, 0), 0)
        assertEquals(listOf(EAST, WEST, 0, 0, 0, 0, 0, 0, 0), meeting.state.matched)
        assertTrue(meeting.isFinished)

        val apart = PipesGame(intArrayOf(EAST, EAST, 0, 0, 0, 0, 0, 0, 0), 1)
        assertEquals(List(9) { 0 }, apart.state.matched)
        assertFalse(apart.isFinished)
    }

    @Test
    fun aSideThatLeavesTheBoardIsAnOpenEnd() {
        val g = PipesGame(intArrayOf(NORTH, 0, 0, 0, 0, 0, 0, 0, 0), 1)
        assertEquals(0, g.state.matched[0])
        assertFalse(g.isFinished)
    }

    @Test
    fun solvingTheBoardEndsTheGameAndTakesNothingMore() {
        val g = board()
        val turns = PipesGenerator.solve(g.state.tiles.toIntArray(), 3)!!
        var last = PipesResult.Ignored
        for (cell in turns.indices) repeat(turns[cell]) { last = g.tap(cell) }
        assertEquals(PipesResult.Solved, last)
        assertEquals(PipesStatus.Won, g.state.status)
        assertTrue(g.isFinished)
        assertEquals(g.par, g.state.moves)
        assertEquals(PipesResult.Ignored, g.tap(0))
        assertFalse(g.restart())
        assertFalse(g.state.canRestart)
    }

    @Test
    fun aTapOutsideTheBoardIsIgnoredAndNotCounted() {
        val g = board()
        assertEquals(PipesResult.Ignored, g.tap(-1))
        assertEquals(PipesResult.Ignored, g.tap(9))
        assertEquals(0, g.state.moves)
    }

    @Test
    fun restartPutsTheTilesBackButKeepsTheMoves() {
        val g = board()
        val start = g.state.tiles
        assertFalse(g.state.canRestart)
        assertFalse(g.restart(), "nothing to undo yet")
        g.tap(0)
        g.tap(5)
        assertTrue(g.state.canRestart)
        assertTrue(g.restart())
        assertEquals(start, g.state.tiles)
        assertEquals(2, g.state.moves)
        assertFalse(g.state.canRestart)
    }

    @Test
    fun fourTapsOnATileBringItBackAndStillCountAsFourMoves() {
        val g = board()
        val start = g.state.tiles
        repeat(4) { g.tap(1) }
        assertEquals(start, g.state.tiles)
        assertEquals(4, g.state.moves)
    }

    @Test
    fun theParAndTheSizeAreWhatTheGameWasMadeWith() {
        val puzzle = PipesGenerator.generate(5, size = 3, minPar = 3)
        val g = PipesGame(puzzle.start, puzzle.par)
        assertEquals(puzzle.par, g.state.par)
        assertEquals(3, g.state.size)
    }

    @Test
    fun aGameNeedsASquareBoardOfRealTiles() {
        assertFailsWith<IllegalArgumentException> { PipesGame(IntArray(8), 0) }
        assertFailsWith<IllegalArgumentException> { PipesGame(IntArray(4), 0) }
        assertFailsWith<IllegalArgumentException> { PipesGame(IntArray(9) { 16 }, 0) }
    }
}
