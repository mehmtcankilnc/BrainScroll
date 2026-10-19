package com.mehmtcan.brainscroll.game.colorflow

import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FlowGameTest {

    // 4x4: red down the left column, blue over the top and down, green in the middle, yellow along the bottom right.
    private val way = listOf(listOf(0, 4, 8, 12), listOf(1, 2, 3, 7), listOf(5, 6, 10, 9), listOf(11, 15, 14, 13))
    private val puzzle = FlowPuzzle(4, way.map { it.first() to it.last() })

    private fun FlowGame.stroke(path: List<Int>): FlowResult {
        assertTrue(start(path.first()))
        path.drop(1).forEach { extend(it) }
        return end()
    }

    @Test
    fun drawingEveryColorFromDotToDotWins() {
        val game = FlowGame(puzzle)
        way.dropLast(1).forEach { assertEquals(FlowResult.Moved, game.stroke(it)) }
        assertFalse(game.isFinished)
        assertEquals(FlowResult.Solved, game.stroke(way.last()))
        assertTrue(game.isFinished)
        assertEquals(FlowStatus.Won, game.state.status)
        assertEquals(4, game.state.moves)
    }

    @Test
    fun aStrokeStartsOnlyOnADotOrAPath() {
        val game = FlowGame(puzzle)
        assertFalse(game.start(2))
        assertTrue(game.start(0))
    }

    @Test
    fun aTapOnADotLeavesNoPathAndCountsNoStroke() {
        val game = FlowGame(puzzle)
        assertTrue(game.start(0))
        assertEquals(FlowResult.Ignored, game.end())
        assertEquals(0, game.state.moves)
        assertEquals(0, game.state.covered)
    }

    @Test
    fun aStepBackTakesTheStepBack() {
        val game = FlowGame(puzzle)
        game.start(0)
        game.extend(4)
        game.extend(8)
        game.extend(4)
        assertEquals(listOf(0, 4), game.state.paths[0])
    }

    @Test
    fun aPathCutsTheOtherPathItCrosses() {
        val game = FlowGame(puzzle)
        game.stroke(listOf(1, 2, 6, 7))
        assertTrue(game.state.joined[1])
        game.stroke(listOf(9, 10, 6, 5))
        assertEquals(listOf(1, 2), game.state.paths[1])
        assertFalse(game.state.joined[1])
        assertTrue(game.state.joined[2])
    }

    @Test
    fun aPathCannotEnterTheDotOfAnotherColor() {
        val game = FlowGame(puzzle)
        game.start(0)
        assertTrue(game.extend(4))
        assertFalse(game.extend(5)) // 5 is the dot of green
    }

    @Test
    fun aJoinedPathDoesNotGoOn() {
        val game = FlowGame(puzzle)
        game.stroke(way[0])
        game.start(12)
        assertFalse(game.extend(13))
    }

    @Test
    fun restartClearsThePathsButKeepsTheStrokes() {
        val game = FlowGame(puzzle)
        game.stroke(way[0])
        assertTrue(game.restart())
        assertEquals(0, game.state.covered)
        assertEquals(1, game.state.moves)
        assertFalse(game.restart())
    }

    @Test
    fun restoreRejectsPathsThatDoNotFit() {
        val game = FlowGame(puzzle)
        assertFalse(game.restore(listOf(listOf(0, 5), emptyList(), emptyList(), emptyList()), 1))
        assertFalse(game.restore(listOf(listOf(4, 8), emptyList(), emptyList(), emptyList()), 1))
        assertTrue(game.restore(way, 5))
        assertTrue(game.isFinished)
    }

    @Test
    fun aRoundSavesAndRestoresTheBoardAsItStands() {
        var clock = 0L
        val round = FlowRound(0, "r1", puzzle, Language.EN) { clock }
        round.start(0); round.extend(4); round.extend(8)
        clock += 2_000
        round.end()
        val stored = round.toStored()
        assertTrue(stored.guesses.joinToString("").length <= 256)
        val back = assertNotNull(FlowRound.restore(0, stored) { clock })
        assertEquals(round.snapshot().state, back.snapshot().state)
        assertFalse(back.isUntouched)
        assertNull(FlowRound.restore(0, stored.copy(answer = "")) { clock })
    }

    @Test
    fun aFinishedRoundHasTheNumberOfStrokes() {
        val round = FlowRound(0, "r2", puzzle, Language.EN)
        way.forEach { path ->
            round.start(path.first())
            path.drop(1).forEach { round.extend(it) }
            round.end()
        }
        assertTrue(round.isFinished)
        assertEquals(listOf("4"), round.toFinished(1L).guesses)
    }
}
