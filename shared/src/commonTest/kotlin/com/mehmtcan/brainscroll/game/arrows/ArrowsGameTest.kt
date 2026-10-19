package com.mehmtcan.brainscroll.game.arrows

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArrowsGameTest {

    private val U = ArrowDir.UP
    private val R = ArrowDir.RIGHT
    private val D = ArrowDir.DOWN
    private val L = ArrowDir.LEFT

    /** A board of the given size from arrows of cells (numbered row by row) and the way each head points. */
    private fun puzzle(size: Int, vararg arrows: Pair<List<Int>, Int>) = ArrowsPuzzle(size, arrows.map { ArrowPath(it.first, it.second) })

    @Test
    fun anArrowWithAClearWayLeavesAndOneWithAnArrowInFrontIsAMistake() {
        // The arrow on cell 0 looks right along row 0 at the arrow on cell 2, which looks down.
        val game = ArrowsGame(puzzle(3, listOf(0) to R, listOf(2) to D))
        assertEquals(ArrowsResult.Blocked, game.tap(0))
        assertEquals(1, game.state.mistakes)
        assertEquals(1, game.state.lastBlocker)
        assertEquals(2, game.state.lastBlockCell)
        assertEquals(2, game.state.remaining, "a blocked arrow stays")
        assertEquals(ArrowsResult.Escaped, game.tap(2))
        assertEquals(ArrowsResult.Solved, game.tap(0))
        assertEquals(ArrowsStatus.Won, game.state.status)
        assertEquals(1, game.state.mistakes)
    }

    @Test
    fun aWindingArrowIsTappedOnAnyOfItsCellsAndItsHeadDecidesIfItCanLeave() {
        // Arrow 0 runs along row 1 to the right (cells 4, 5, 6); its head is on cell 6, and arrow 1 on cell 7 is in front of it.
        val game = ArrowsGame(puzzle(4, listOf(4, 5, 6) to R, listOf(7) to U))
        assertEquals(listOf(false, true), game.state.arrows.map { it.free })
        assertEquals(ArrowsResult.Blocked, game.tap(4), "the tail counts as the arrow")
        assertEquals(7, game.state.lastBlockCell)
        assertEquals(ArrowsResult.Escaped, game.tap(7))
        assertEquals(ArrowsResult.Solved, game.tap(5), "the middle counts too")
    }

    @Test
    fun theBodyOfAnArrowBlocksTheOnesLookingAtIt() {
        // Arrow 1 looks down column 1 at the middle of arrow 0.
        val game = ArrowsGame(puzzle(3, listOf(3, 4, 5) to R, listOf(1) to D))
        assertEquals(ArrowsResult.Blocked, game.tap(1))
        assertEquals(0, game.state.lastBlocker)
        assertEquals(4, game.state.lastBlockCell)
        assertEquals(ArrowsResult.Escaped, game.tap(3))
        assertEquals(ArrowsResult.Solved, game.tap(1))
    }

    @Test
    fun theBlockerIsTheNearestArrowEvenFarAway() {
        val game = ArrowsGame(puzzle(3, listOf(0) to D, listOf(3) to D, listOf(6) to L))
        assertEquals(ArrowsResult.Blocked, game.tap(0))
        assertEquals(1, game.state.lastBlocker)
        assertEquals(ArrowsResult.Blocked, game.tap(3))
        assertEquals(2, game.state.lastBlocker)
    }

    @Test
    fun tapsOnNothingAreIgnored() {
        val game = ArrowsGame(puzzle(3, listOf(0) to U))
        assertEquals(ArrowsResult.Ignored, game.tap(4))
        assertEquals(ArrowsResult.Ignored, game.tap(-1))
        assertEquals(ArrowsResult.Ignored, game.tap(9))
        assertEquals(ArrowsResult.Solved, game.tap(0))
        assertEquals(ArrowsResult.Ignored, game.tap(0), "the game is over")
        assertEquals(0, game.state.mistakes)
    }

    @Test
    fun mistakesNeverEndTheGame() {
        val game = ArrowsGame(puzzle(3, listOf(0) to R, listOf(2) to D))
        repeat(5) { assertEquals(ArrowsResult.Blocked, game.tap(0)) }
        assertEquals(5, game.state.mistakes)
        assertEquals(ArrowsStatus.Playing, game.state.status)
        assertEquals(ArrowsResult.Escaped, game.tap(2))
        assertEquals(ArrowsResult.Solved, game.tap(0))
    }

    @Test
    fun restartPutsTheArrowsBackAndKeepsTheMistakes() {
        val game = ArrowsGame(puzzle(3, listOf(0) to R, listOf(2) to D, listOf(8) to D))
        assertFalse(game.restart(), "nothing left yet")
        game.tap(0)
        game.tap(8)
        assertTrue(game.state.canRestart)
        assertTrue(game.restart())
        assertEquals(3, game.state.remaining)
        assertEquals(1, game.state.mistakes)
        assertFalse(game.state.canRestart)
    }

    @Test
    fun aBoardThatCannotBeClearedIsRefused() {
        // Two arrows looking at each other.
        assertFailsWith<IllegalArgumentException> { puzzle(3, listOf(0) to R, listOf(2) to L) }
        // An arrow whose head looks at its own body: a spiral of eight cells ending under the first.
        assertFailsWith<IllegalArgumentException> { puzzle(3, listOf(0, 1, 2, 5, 8, 7, 6, 3) to U) }
        assertFailsWith<IllegalArgumentException> { ArrowsPuzzle(3, emptyList()) }
    }

    @Test
    fun aBoardWithABrokenPathIsRefused() {
        assertFailsWith<IllegalArgumentException> { puzzle(3, listOf(0, 4) to R) }
        assertFailsWith<IllegalArgumentException> { puzzle(3, listOf(0, 1) to D) }
        assertFailsWith<IllegalArgumentException> { puzzle(3, listOf(0, 1) to R, listOf(1, 4) to D) }
    }

    @Test
    fun theTextOfABoardIsReadBackAndBadTextIsRefused() {
        val p = puzzle(4, listOf(4, 5, 6) to R, listOf(7) to U, listOf(8, 12) to D, listOf(0, 1, 2) to R, listOf(15) to D)
        val back = assertNotNull(ArrowsPuzzle.decode(p.encode()))
        assertEquals(p.arrows, back.arrows)
        assertTrue(p.encode().length <= 32)
        assertNull(ArrowsPuzzle.decode(""))
        assertNull(ArrowsPuzzle.decode("s"))
        assertNull(ArrowsPuzzle.decode("3:0:0"))
        assertNull(ArrowsPuzzle.decode("9:1:2"))
        assertNull(ArrowsPuzzle.decode("3:zzzzzzzzzzzz:0"))
        assertNull(ArrowsPuzzle.decode("s!!"))
    }

    @Test
    fun aBoardOfTheOlderKindIsStillReadAsArrowsOfOneCell() {
        // The first text of the game: a direction per cell, "6:1k2f4m9a:o3b8cu1x" style. This one is 3 by 3: R . D / . . . / . . L.
        val dirs = listOf(1, -1, 2, -1, -1, -1, -1, -1, 3)
        val half = (dirs.size + 1) / 2
        fun pack(cells: List<Int>) = cells.fold(0L) { total, d -> total * 5 + (d + 1) }.toString(36)
        val back = assertNotNull(ArrowsPuzzle.decode("3:${pack(dirs.subList(0, half))}:${pack(dirs.subList(half, dirs.size))}"))
        assertEquals(listOf(listOf(0), listOf(2), listOf(8)), back.arrows.map { it.cells })
        assertEquals(listOf(R, D, L), back.arrows.map { it.dir })
    }
}
