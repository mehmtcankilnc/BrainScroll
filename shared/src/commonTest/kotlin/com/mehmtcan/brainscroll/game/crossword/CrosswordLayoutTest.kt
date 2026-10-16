package com.mehmtcan.brainscroll.game.crossword

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrosswordLayoutTest {

    private val size = CrosswordLayout.SIZE

    /** A layout from rows of `#` (clue) and `.` (letter); the rows that are not given are all letters, written from the left. */
    private fun layout(vararg rows: String): BooleanArray {
        val isClue = BooleanArray(CrosswordLayout.CELLS)
        rows.forEachIndexed { r, row -> row.forEachIndexed { c, ch -> isClue[r * size + c] = ch == '#' } }
        return isClue
    }

    /** A layout of `#` and `.` for the part at the top left, with the rest of the board made of clue squares in stripes that are usable. */
    private fun small(vararg rows: String): BooleanArray {
        // The part outside the rows given is clue squares at every second row: the cells in the other rows are then in runs.
        val isClue = BooleanArray(CrosswordLayout.CELLS) { true }
        rows.forEachIndexed { r, row -> row.forEachIndexed { c, ch -> isClue[r * size + c] = ch == '#' } }
        return isClue
    }

    @Test
    fun theRunsAreEveryRowAndColumnStretchOfTwoOrMoreLetters() {
        val isClue = small("#..#.", "..#..", "#....")
        val runs = CrosswordLayout.runs(isClue).map { Triple(it.start, it.across, it.length) }
        assertTrue(Triple(1, true, 2) in runs)
        assertTrue(Triple(size, true, 2) in runs, "row 1 begins at the left edge")
        assertTrue(Triple(size + 3, true, 2) in runs)
        assertTrue(Triple(2 * size + 1, true, 4) in runs)
        assertTrue(Triple(1, false, 3) in runs, "the column of square 1 runs three rows down to the clue square")
        assertFalse(runs.any { it.third < 2 })
        assertEquals(listOf(1, 2), runs.first { it.first == 1 && it.second }.let { CrosswordLayout.Run(it.first, it.second, it.third).cells })
    }

    @Test
    fun aLayoutWithARunThatHasNoClueSquareNextToItHasNoSlots() {
        // The run of letters in the first row starts at the left edge, and there is nothing above it or on its left.
        assertNull(CrosswordLayout.slots(layout("...#")))
    }

    @Test
    fun aClueSquareThatHoldsNothingMakesALayoutUnusable() {
        // Only clue squares: they have nothing to hold.
        assertFalse(CrosswordLayout.isUsable(BooleanArray(CrosswordLayout.CELLS) { true }))
        assertNull(CrosswordLayout.slots(BooleanArray(CrosswordLayout.CELLS) { true }))
    }

    @Test
    fun theReadyBoardsUseStraightArrowsAndArrowsThatTurn() {
        for (language in com.mehmtcan.brainscroll.game.wordle.Language.entries) {
            val slots = CrosswordBoards.of(language).take(20).flatMap { CrosswordPuzzle(language, it).slots }
            assertTrue(slots.any { it.bent }, "$language has arrows that turn")
            assertTrue(slots.any { !it.bent }, "$language has straight arrows")
            assertTrue(slots.any { it.across } && slots.any { !it.across })
        }
    }

    @Test
    fun theSlotsOfALayoutAreAlwaysTheSame() {
        val board = CrosswordBoards.of(com.mehmtcan.brainscroll.game.wordle.Language.TR).first()
        val isClue = BooleanArray(CrosswordLayout.CELLS) { board[it] == '#' }
        val a = CrosswordLayout.slots(isClue)!!.map { Triple(it.clueCell, it.across, it.cells.first()) }
        val b = CrosswordLayout.slots(isClue)!!.map { Triple(it.clueCell, it.across, it.cells.first()) }
        assertEquals(a, b)
    }

    @Test
    fun fourClueSquaresInABlockMakeALayoutUnusable() {
        val board = CrosswordBoards.of(com.mehmtcan.brainscroll.game.wordle.Language.EN).first()
        val isClue = BooleanArray(CrosswordLayout.CELLS) { board[it] == '#' }
        assertTrue(CrosswordLayout.isUsable(isClue))
        // Turn the squares around one clue square into clues: there is a block of four now.
        val at = isClue.indices.first { isClue[it] && it % size < size - 1 && it / size < size - 1 }
        val block = isClue.copyOf()
        block[at + 1] = true
        block[at + size] = true
        block[at + size + 1] = true
        assertFalse(CrosswordLayout.isUsable(block))
    }
}
