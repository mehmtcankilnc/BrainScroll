package com.mehmtcan.brainscroll.game.lights

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LightsGameTest {

    private val seeds = (1..60).toList()

    private fun game(seed: Int = 3) = LightsGame(seed)

    private fun solve(g: LightsGame, seed: Int = 3) {
        for (cell in LightsGame.solutionTaps(seed, LightsGame.DEFAULT_SIZE, LightsGame.DEFAULT_TAPS)) g.tap(cell)
    }

    @Test
    fun everyBoardCanBeSolvedByTappingTheCellsItWasMadeWith() {
        for (seed in seeds) {
            val g = game(seed)
            assertFalse(g.isFinished, "seed $seed starts dark")
            solve(g, seed)
            assertTrue(g.isFinished, "seed $seed")
            assertEquals(LightsGame.DEFAULT_TAPS, g.state.moves)
        }
    }

    @Test
    fun aBoardAlwaysHasSomeLightsOn() {
        for (seed in seeds) {
            assertTrue(game(seed).state.lit.count { it } >= 4, "seed $seed")
        }
    }

    @Test
    fun theSameSeedAlwaysGivesTheSameBoardAndOtherSeedsGiveOtherOnes() {
        assertContentEquals(LightsGame.generate(9, 5, 7), LightsGame.generate(9, 5, 7))
        assertTrue(!LightsGame.generate(9, 5, 7).contentEquals(LightsGame.generate(10, 5, 7)))
    }

    @Test
    fun aTapSwitchesTheCellAndItsFourNeighborsOnly() {
        val g = game()
        val before = g.state.lit
        assertEquals(LightsResult.Toggled, g.tap(12))
        val after = g.state.lit
        val changed = after.indices.filter { after[it] != before[it] }.toSet()
        assertEquals(setOf(12, 7, 17, 11, 13), changed)
    }

    @Test
    fun aTapOnACornerSwitchesOnlyThreeCellsAndAnEdgeFour() {
        val g = game()
        var before = g.state.lit
        g.tap(0)
        var after = g.state.lit
        assertEquals(setOf(0, 1, 5), after.indices.filter { after[it] != before[it] }.toSet())
        before = after
        g.tap(2)
        after = g.state.lit
        assertEquals(setOf(2, 1, 3, 7), after.indices.filter { after[it] != before[it] }.toSet())
    }

    @Test
    fun tappingTheSameCellTwiceCancelsOutButBothTapsAreMoves() {
        val g = game()
        val before = g.state.lit
        g.tap(7)
        g.tap(7)
        assertEquals(before, g.state.lit)
        assertEquals(2, g.state.moves)
    }

    @Test
    fun theLastLightGoingOutIsSolvedAndTheBoardTakesNothingMore() {
        val g = game()
        val taps = LightsGame.solutionTaps(3, 5, 7)
        for (cell in taps.dropLast(1)) assertEquals(LightsResult.Toggled, g.tap(cell))
        assertEquals(LightsResult.Solved, g.tap(taps.last()))
        assertEquals(LightsStatus.Won, g.state.status)
        assertEquals(LightsResult.Ignored, g.tap(0))
        assertEquals(7, g.state.moves)
        assertFalse(g.restart())
    }

    @Test
    fun outOfRangeTapsAreIgnoredAndNotCounted() {
        val g = game()
        assertEquals(LightsResult.Ignored, g.tap(-1))
        assertEquals(LightsResult.Ignored, g.tap(25))
        assertEquals(0, g.state.moves)
    }

    @Test
    fun restartPutsTheLightsBackButKeepsTheMoves() {
        val g = game()
        val start = g.state.lit
        assertFalse(g.state.canRestart)
        assertFalse(g.restart(), "nothing to undo yet")
        g.tap(4)
        g.tap(10)
        assertTrue(g.state.canRestart)
        assertTrue(g.restart())
        assertEquals(start, g.state.lit)
        assertEquals(2, g.state.moves)
        assertFalse(g.state.canRestart)
    }

    @Test
    fun theParIsTheNumberOfTapsTheBoardWasMadeWith() {
        assertEquals(7, game().state.par)
        assertEquals(4, LightsGame(2, taps = 4).state.par)
    }

    @Test
    fun aGameNeedsSaneSettings() {
        assertFailsWith<IllegalArgumentException> { LightsGame(1, size = 2) }
        assertFailsWith<IllegalArgumentException> { LightsGame(1, taps = 0) }
        assertFailsWith<IllegalArgumentException> { LightsGame(1, taps = 26) }
    }
}
