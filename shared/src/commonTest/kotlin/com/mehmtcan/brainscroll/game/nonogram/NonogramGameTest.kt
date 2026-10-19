package com.mehmtcan.brainscroll.game.nonogram

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NonogramGameTest {

    private val fill = NonogramMark.FILLED
    private val cross = NonogramMark.CROSS
    private val empty = NonogramMark.EMPTY

    /** A 3 by 3 plus sign: the middle row and the middle column. */
    private val plus = NonogramPuzzle(3, setOf(1, 3, 4, 5, 7))

    @Test
    fun theNumbersFollowThePicture() {
        assertEquals(listOf(listOf(1), listOf(3), listOf(1)), plus.rowClues)
        assertEquals(listOf(listOf(1), listOf(3), listOf(1)), plus.columnClues)
    }

    @Test
    fun aStrokeSetsEveryCellOfTheStretch() {
        val game = NonogramGame(plus)
        assertEquals(NonogramResult.Changed, game.paint(3, 5, fill))
        assertEquals(listOf(empty, empty, empty, fill, fill, fill, empty, empty, empty), game.state.marks)
        assertEquals(NonogramResult.Changed, game.paint(1, 7, cross))
        assertEquals(cross, game.state.marks[4], "a stroke along a column overwrites")
    }

    @Test
    fun aStrokeThatIsNotOnOneLineOrChangesNothingIsIgnored() {
        val game = NonogramGame(plus)
        assertEquals(NonogramResult.Ignored, game.paint(0, 4, fill), "a diagonal")
        assertEquals(NonogramResult.Ignored, game.paint(-1, 3, fill))
        assertEquals(NonogramResult.Ignored, game.paint(0, 9, fill))
        assertEquals(NonogramResult.Ignored, game.paint(0, 0, 3))
        assertEquals(NonogramResult.Ignored, game.paint(0, 0, empty), "already empty")
        game.paint(3, 3, fill)
        assertEquals(NonogramResult.Ignored, game.paint(3, 3, fill), "already filled")
    }

    @Test
    fun aLineFadesOnlyWhenItsNumbersMatchExactly() {
        val game = NonogramGame(plus)
        assertFalse(game.state.rowDone[1])
        game.paint(3, 5, fill)
        assertTrue(game.state.rowDone[1])
        // A cell that only matches the numbers of its own line also fades them, right or wrong for the picture.
        game.paint(0, 0, fill)
        assertTrue(game.state.rowDone[0])
        game.paint(0, 2, fill)
        assertFalse(game.state.rowDone[0], "three in a row do not match the number 1")
    }

    @Test
    fun theBoardIsSolvedWhenTheFilledCellsAreThePictureAndCrossesDoNotMatter() {
        val game = NonogramGame(plus)
        game.paint(0, 0, cross)
        game.paint(3, 5, fill)
        assertEquals(NonogramStatus.Playing, game.state.status)
        assertNull(game.state.picture)
        assertEquals(NonogramResult.Solved, game.paint(1, 7, fill))
        assertEquals(NonogramStatus.Won, game.state.status)
        assertTrue(game.isFinished)
        assertEquals(NonogramResult.Ignored, game.paint(0, 0, fill), "the game is over")
    }

    @Test
    fun anExtraFilledCellIsNotASolution() {
        val game = NonogramGame(plus)
        game.paint(0, 0, fill)
        game.paint(3, 5, fill)
        game.paint(1, 7, fill)
        assertEquals(NonogramStatus.Playing, game.state.status)
        game.paint(0, 0, empty)
        assertEquals(NonogramStatus.Won, game.state.status)
    }

    @Test
    fun restartClearsTheBoard() {
        val game = NonogramGame(plus)
        assertFalse(game.restart())
        game.paint(3, 5, cross)
        assertTrue(game.state.canRestart)
        assertTrue(game.restart())
        assertTrue(game.state.marks.all { it == empty })
    }

    @Test
    fun theTextOfABoardIsReadBackAndBadTextIsRefused() {
        val picture = NonogramPictures.all.first()
        val puzzle = NonogramPuzzle.of(picture, true)
        val back = assertNotNull(NonogramPuzzle.decode(puzzle.encode()))
        assertEquals(puzzle.solution, back.solution)
        assertEquals(true, back.mirrored)
        assertTrue(puzzle.encode().length <= 32)
        assertNull(NonogramPuzzle.decode(""))
        assertNull(NonogramPuzzle.decode("8:123:0"))
        assertNull(NonogramPuzzle.decode("10:0000000000000001:0"), "the big board has 25 digits")
        assertNull(NonogramPuzzle.decode("5:ffffffffffffffff:0"), "bits beyond the board")
        assertNull(NonogramPuzzle.decode("8:0000000000000000:0"))
        assertNull(NonogramPuzzle.decode("8:ffffffffffffffff:2"))
        assertNull(NonogramPuzzle.decode("12:ffffffffffffffff:0"))
    }

    @Test
    fun aSolvedBoardOfADrawnPictureKnowsItsName() {
        val picture = NonogramPictures.all.first()
        val game = NonogramGame(NonogramPuzzle.of(picture, false))
        for (cell in picture.cells) game.paint(cell, cell, fill)
        assertEquals(picture.id, game.state.picture?.id)
    }
}
