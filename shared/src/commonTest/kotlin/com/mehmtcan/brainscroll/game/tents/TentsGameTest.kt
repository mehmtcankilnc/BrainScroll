package com.mehmtcan.brainscroll.game.tents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TentsGameTest {

    /**
     * A 4x4 board, cells numbered row by row: trees at 0 (top left), 7 (second row, right) and 8 (third row, left), and the
     * tents above, below and below them at 1, 11 and 12.
     */
    private val puzzle = TentsPuzzle(4, trees = setOf(0, 7, 8), tents = setOf(1, 11, 12))

    @Test
    fun theNumbersFollowFromTheTents() {
        assertEquals(listOf(1, 0, 1, 1), puzzle.rowTargets)
        assertEquals(listOf(1, 1, 0, 1), puzzle.columnTargets)
    }

    @Test
    fun aTapCyclesEmptyTentGrassEmptyAndCountsEveryTap() {
        val game = TentsGame(puzzle)
        assertEquals(TentsResult.Tent, game.tap(2))
        assertEquals(TentsMark.TENT, game.state.marks[2])
        assertEquals(TentsResult.Grass, game.tap(2))
        assertEquals(TentsMark.GRASS, game.state.marks[2])
        assertEquals(TentsResult.Cleared, game.tap(2))
        assertEquals(TentsMark.EMPTY, game.state.marks[2])
        assertEquals(3, game.state.taps)
    }

    @Test
    fun aTreeAndACellThatDoesNotExistAreIgnored() {
        val game = TentsGame(puzzle)
        assertEquals(TentsResult.Ignored, game.tap(0))
        assertEquals(TentsResult.Ignored, game.tap(99))
        assertEquals(TentsResult.Ignored, game.tap(-1))
        assertEquals(0, game.state.taps)
    }

    @Test
    fun theGrassTakesNoPartInTheRules() {
        val game = TentsGame(puzzle)
        game.tap(2); game.tap(2) // grass
        game.tap(5); game.tap(5) // grass
        assertEquals(0, game.state.tentCount)
        assertTrue(game.state.conflicts.none { it })
    }

    @Test
    fun touchingTentsAreMarkedAtACornerToo() {
        val game = TentsGame(puzzle)
        game.tap(1)
        game.tap(6) // 1 is (0,1) and 6 is (1,2): they touch at a corner
        assertTrue(game.state.conflicts[1] && game.state.conflicts[6])
        game.tap(6); game.tap(6) // grass, then empty
        assertTrue(game.state.conflicts.none { it })
    }

    @Test
    fun aTentInARowThatIsFullAlreadyIsMarked() {
        val game = TentsGame(puzzle)
        game.tap(1)
        game.tap(3) // row 0 wants one tent
        assertTrue(game.state.conflicts[1] && game.state.conflicts[3])
    }

    @Test
    fun aTentWithNoTreeNextToItIsMarked() {
        val game = TentsGame(puzzle)
        game.tap(13) // row 3, column 1: no tree around
        assertTrue(game.state.conflicts[13])
    }

    @Test
    fun theRightTentsWinTheGameAndAfterThatTapsAreIgnored() {
        val game = TentsGame(puzzle)
        assertEquals(TentsResult.Tent, game.tap(1))
        assertEquals(TentsResult.Tent, game.tap(11))
        assertEquals(TentsResult.Solved, game.tap(12))
        assertTrue(game.isFinished)
        assertEquals(TentsStatus.Won, game.state.status)
        assertEquals(TentsResult.Ignored, game.tap(5))
    }

    @Test
    fun takingAwayTheOneExtraTentCanFinishTheBoard() {
        val game = TentsGame(puzzle)
        game.tap(1); game.tap(11); game.tap(13) // 13 is an extra tent: no win yet
        assertFalse(game.isFinished)
        game.tap(12)
        game.tap(13) // grass: the extra tent goes, and the board is as it should be
        assertTrue(game.isFinished)
    }

    @Test
    fun aRestartClearsEveryMarkAndKeepsTheTaps() {
        val game = TentsGame(puzzle)
        game.tap(1)
        game.tap(2); game.tap(2)
        assertTrue(game.state.canRestart)
        assertTrue(game.restart())
        assertTrue(game.state.marks.all { it == TentsMark.EMPTY })
        assertEquals(3, game.state.taps)
        assertFalse(game.restart(), "nothing to clear")
    }

    @Test
    fun theBoardIsChecked() {
        assertFailsWith<IllegalArgumentException> { TentsPuzzle(2, setOf(0), setOf(1)) }
        assertFailsWith<IllegalArgumentException> { TentsPuzzle(4, setOf(0), setOf(0)) }
        assertFailsWith<IllegalArgumentException> { TentsPuzzle(4, setOf(0, 15), setOf(1)) }
        assertFailsWith<IllegalArgumentException> { TentsPuzzle(4, setOf(0, 2), setOf(1, 6)) }
        assertFailsWith<IllegalArgumentException> { TentsPuzzle(4, setOf(0), setOf(10)) }
    }

    @Test
    fun aBoardIsWrittenAsShortTextAndReadBack() {
        val text = puzzle.encode()
        assertTrue(text.length <= 32)
        val back = assertNotNull(TentsPuzzle.decode(text))
        assertEquals(puzzle.trees, back.trees)
        assertEquals(puzzle.tents, back.tents)
        assertEquals(puzzle.size, back.size)
        assertNull(TentsPuzzle.decode("4:1"))
        assertNull(TentsPuzzle.decode("x:1:2"))
        assertNull(TentsPuzzle.decode("9:1:2"))
        assertNull(TentsPuzzle.decode("4:zz:2"))
        assertNull(TentsPuzzle.decode("4:1:1"), "a tree and a tent on one cell")
    }
}
