package com.mehmtcan.brainscroll.game.hanoi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HanoiGameTest {

    /** Three disks: the smallest and the biggest on tower 0, the middle one on tower 1; the goal is tower 2. */
    private fun game() = HanoiGame(listOf(0, 1, 0), goal = 2, par = HanoiSolver.distance(intArrayOf(0, 1, 0), 2))

    @Test
    fun theTowersListTheDisksFromTheBottomUp() {
        val state = game().state
        assertEquals(listOf(listOf(2, 0), listOf(1), listOf()), state.towers)
        assertEquals(listOf(0, 1, 0), state.pegs)
        assertEquals(HanoiStatus.Playing, state.status)
        assertEquals(0, state.moves)
        assertFalse(state.canRestart)
    }

    @Test
    fun aDiskGoesOntoAnEmptyTowerOrABiggerDiskOnly() {
        val g = game()
        assertEquals(HanoiResult.Ignored, g.move(1, 0), "the middle disk cannot go onto the smallest")
        assertEquals(HanoiResult.Ignored, g.move(2, 0), "an empty tower has no disk")
        assertEquals(HanoiResult.Ignored, g.move(0, 0), "the same tower")
        assertEquals(HanoiResult.Ignored, g.move(0, 3))
        assertEquals(HanoiResult.Ignored, g.move(-1, 1))
        assertEquals(0, g.state.moves, "nothing that was refused counts")
        assertEquals(HanoiResult.Moved, g.move(0, 1), "the smallest goes onto the middle one")
        assertEquals(listOf(listOf(2), listOf(1, 0), listOf()), g.state.towers)
        assertEquals(1, g.state.moves)
    }

    @Test
    fun theGameIsWonWhenEveryDiskIsOnTheGoalTowerAndThenStops() {
        val g = game()
        val way = HanoiSolver.shortest(intArrayOf(0, 1, 0), 2)
        val results = way.map { g.move(it.from, it.to) }
        assertEquals(HanoiResult.Solved, results.last())
        assertTrue(results.dropLast(1).all { it == HanoiResult.Moved })
        assertTrue(g.isFinished)
        assertEquals(HanoiStatus.Won, g.state.status)
        assertEquals(g.par, g.state.moves, "the shortest way took exactly the par")
        assertEquals(HanoiResult.Ignored, g.move(2, 0), "a finished game takes no more moves")
        assertFalse(g.restart())
    }

    @Test
    fun aRestartPutsTheDisksBackAndKeepsTheMoves() {
        val g = game()
        assertFalse(g.restart(), "nothing to put back")
        g.move(0, 1)
        g.move(0, 2)
        assertTrue(g.state.canRestart)
        assertTrue(g.restart())
        assertEquals(listOf(0, 1, 0), g.state.pegs)
        assertEquals(2, g.state.moves)
        assertFalse(g.state.canRestart)
    }

    @Test
    fun aPositionThatIsAlreadySolvedIsWon() {
        assertTrue(HanoiGame(listOf(2, 2, 2), 2, 0).isFinished)
    }

    @Test
    fun theTowersAndDisksMustBeInRange() {
        assertFailsWith<IllegalArgumentException> { HanoiGame(listOf(0), 0, 0) }
        assertFailsWith<IllegalArgumentException> { HanoiGame(listOf(0, 3), 0, 0) }
        assertFailsWith<IllegalArgumentException> { HanoiGame(listOf(0, 1), 3, 0) }
    }
}
