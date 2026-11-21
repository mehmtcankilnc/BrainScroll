package com.mehmtcan.brainscroll.game.lasers

import com.mehmtcan.brainscroll.game.Difficulty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LasersTest {

    private val slash = LaserRules.SLASH
    private val back = LaserRules.BACKSLASH

    // A 4 by 4 board, the laser on row 1. A mirror on cell 6 (row 1, column 2) and a target above it on cell 2 and one before it on cell 5.
    private fun board(mirror: Int) = LaserPuzzle(
        size = 4,
        row = 1,
        mirrors = List(16) { if (it == 6) mirror else 0 },
        targets = setOf(2, 5),
    )

    @Test
    fun aMirrorSendsTheBeamUpOrDownAndTheBeamLeavesTheBoard() {
        val up = LaserRules.trace(4, 1, board(slash).mirrors)
        assertEquals(setOf(4, 5, 6, 2), up.lit, "a / turns a beam going right upwards")
        val down = LaserRules.trace(4, 1, board(back).mirrors)
        assertEquals(setOf(4, 5, 6, 10, 14), down.lit, "a \\ turns it downwards")
        // The line of the beam: it comes in, turns at the middle of the mirror cell and leaves at the edge.
        assertEquals(BeamPoint(-0.45f, 1.5f), up.points.first())
        assertTrue(BeamPoint(2.5f, 1.5f) in up.points)
        assertEquals(BeamPoint(2.5f, 0f), up.points.last())
    }

    @Test
    fun withNoMirrorTheBeamGoesStraightAcrossTheRow() {
        val beam = LaserRules.trace(4, 2, List(16) { 0 })
        assertEquals(setOf(8, 9, 10, 11), beam.lit)
        assertEquals(BeamPoint(4f, 2.5f), beam.points.last())
    }

    @Test
    fun theBoardIsWonWhenTheBeamPassesEveryTarget() {
        val game = LaserGame(board(back))
        assertEquals(LaserStatus.Playing, game.state.status)
        assertEquals(1, game.state.litCount, "only the target before the mirror is lit")
        assertEquals(LaserResult.Ignored, game.turn(0), "no mirror on that cell")
        assertEquals(LaserResult.Solved, game.turn(6))
        assertEquals(LaserStatus.Won, game.state.status)
        assertEquals(2, game.state.litCount)
        assertEquals(1, game.state.turns)
        assertEquals(LaserResult.Ignored, game.turn(6), "the game is over")
        assertFalse(game.restart())
    }

    @Test
    fun turningAMirrorTwiceIsTurningItBackAndARestartPutsEverythingBack() {
        val game = LaserGame(LaserGenerator.generate(3, Difficulty.Medium).puzzle)
        val start = game.state.mirrors
        assertFalse(game.state.canRestart)
        val cell = start.indexOfFirst { it != 0 }
        game.turn(cell)
        assertTrue(game.state.mirrors != start)
        game.turn(cell)
        assertEquals(start, game.state.mirrors, "turned back")
        assertEquals(2, game.state.turns)
        game.turn(cell)
        assertTrue(game.state.canRestart)
        assertTrue(game.restart())
        assertEquals(start, game.state.mirrors)
        assertEquals(3, game.state.turns, "the turns stay counted")
        assertFalse(game.restart(), "already at the start")
    }

    @Test
    fun theBoardIsSavedAsTextAndReadBack() {
        for (level in Difficulty.entries) for (seed in 0 until 40) {
            val puzzle = LaserGenerator.generate(seed, level).puzzle
            val text = puzzle.encode()
            assertTrue(text.length <= 32, "$level $text is ${text.length} long")
            assertEquals(puzzle, LaserPuzzle.decode(text))
        }
        assertNull(LaserPuzzle.decode("!!"))
        assertNull(LaserPuzzle.decode(""))
    }

    @Test
    fun everyGeneratedBoardCanBeWonButIsNotWonAtTheStart() {
        for (level in Difficulty.entries) {
            val shape = com.mehmtcan.brainscroll.game.GameLevels.lasers(level)
            for (seed in 0 until 120) {
                val board = LaserGenerator.generate(seed, level)
                val p = board.puzzle
                assertEquals(shape.size, p.size)
                assertEquals(shape.targets, p.targets.size)
                assertFalse(LaserRules.lightsAll(p.size, p.row, p.mirrors, p.targets), "$level seed $seed is not won at the start")
                val turns = assertNotNull(LaserRules.fewestTurns(p.size, p.row, p.mirrors, p.targets), "$level seed $seed can be won")
                assertEquals(board.par, turns.count { it })
                assertTrue(board.par >= shape.minTurns, "$level seed $seed takes ${board.par} turns")
                assertEquals(p.mirrorCells.size, turns.size)
                assertEquals(shape.pathMirrors + shape.decoys, p.mirrorCells.size)
            }
        }
    }

    @Test
    fun turningTheMirrorsTheSolverSaysWinsTheBoard() {
        for (level in Difficulty.entries) for (seed in 0 until 30) {
            val p = LaserGenerator.generate(seed, level).puzzle
            val turns = assertNotNull(LaserRules.fewestTurns(p.size, p.row, p.mirrors, p.targets))
            val game = LaserGame(p)
            p.mirrorCells.zip(turns).filter { it.second }.forEach { game.turn(it.first) }
            assertEquals(LaserStatus.Won, game.state.status, "$level seed $seed")
        }
    }

    @Test
    fun turningAMirrorTheBeamNeverMeetsLeavesTheBeamAsItWas() {
        var checked = 0
        for (level in Difficulty.entries) for (seed in 0 until 40) {
            val p = LaserGenerator.generate(seed, level).puzzle
            val game = LaserGame(p)
            val beam = game.state.beam
            val apart = p.mirrorCells.filter { it !in beam.lit }
            for (cell in apart) {
                val g = LaserGame(p)
                g.turn(cell)
                assertEquals(beam, g.state.beam, "$level seed $seed: turning cell $cell, which the beam does not meet, leaves it as it was")
                checked++
            }
            // A mirror on the beam's way does change it (the beam turns the other way there).
            val onWay = p.mirrorCells.firstOrNull { it in beam.lit }
            if (onWay != null) {
                val g = LaserGame(p)
                g.turn(onWay)
                assertTrue(g.state.beam != beam, "$level seed $seed: turning cell $onWay, which the beam meets, changes it")
            }
        }
        assertTrue(checked > 20, "there are mirrors the beam does not meet to check: $checked")
    }

    @Test
    fun theSameSeedMakesTheSameBoard() {
        for (level in Difficulty.entries) assertEquals(LaserGenerator.generate(9, level).puzzle, LaserGenerator.generate(9, level).puzzle)
    }

    @Test
    fun aSavedGameIsRestoredOnlyIfItCouldHaveHappened() {
        val p = LaserGenerator.generate(4, Difficulty.Easy).puzzle
        val turned = p.mirrors.map { LaserRules.turned(it) }
        val game = LaserGame(p)
        assertTrue(game.restore(turned, 5))
        assertEquals(5, game.state.turns)
        assertEquals(turned, game.state.mirrors)
        // A mirror on an empty cell, a missing mirror, the wrong size or a negative count.
        val empty = p.mirrors.indexOfFirst { it == 0 }
        assertFalse(LaserGame(p).restore(p.mirrors.toMutableList().also { it[empty] = slash }, 0))
        val mirror = p.mirrors.indexOfFirst { it != 0 }
        assertFalse(LaserGame(p).restore(p.mirrors.toMutableList().also { it[mirror] = 0 }, 0))
        assertFalse(LaserGame(p).restore(p.mirrors.take(5), 0))
        assertFalse(LaserGame(p).restore(p.mirrors, -1))
    }
}
