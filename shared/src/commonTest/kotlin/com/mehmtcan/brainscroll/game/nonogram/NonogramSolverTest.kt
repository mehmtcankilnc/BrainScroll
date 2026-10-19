package com.mehmtcan.brainscroll.game.nonogram

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NonogramSolverTest {

    private val u = NonogramSolver.UNKNOWN
    private val e = NonogramSolver.EMPTY
    private val f = NonogramSolver.FILLED

    private fun picture(vararg rows: String): List<List<Boolean>> = rows.map { row -> row.map { it == '#' } }

    @Test
    fun theNumbersOfALineAreItsRunsInOrder() {
        assertEquals(listOf(2, 3), lineClues("##.###..".map { it == '#' }))
        assertEquals(emptyList(), lineClues("........".map { it == '#' }))
        assertEquals(listOf(8), lineClues("########".map { it == '#' }))
        assertEquals(listOf(1, 1, 1), lineClues("#.#.#...".map { it == '#' }))
    }

    @Test
    fun aFullLineIsKnownFromItsNumbersAlone() {
        assertContentEquals(intArrayOf(f, f, f, f, f), NonogramSolver.solveLine(IntArray(5) { u }, listOf(5)))
        assertContentEquals(intArrayOf(f, e, f, e, f), NonogramSolver.solveLine(IntArray(5) { u }, listOf(1, 1, 1)))
        assertContentEquals(IntArray(4) { e }, NonogramSolver.solveLine(IntArray(4) { u }, emptyList()))
    }

    @Test
    fun theMiddleOfALongRunIsCertainAndTheEndsAreNot() {
        // A run of 4 in 6 cells always covers the middle two.
        assertContentEquals(intArrayOf(u, u, f, f, u, u), NonogramSolver.solveLine(IntArray(6) { u }, listOf(4)))
    }

    @Test
    fun knownCellsNarrowTheLine() {
        // 3 in 6 cells, the first cell is filled: it must be the first three.
        val known = intArrayOf(f, u, u, u, u, u)
        assertContentEquals(intArrayOf(f, f, f, e, e, e), NonogramSolver.solveLine(known, listOf(3)))
    }

    @Test
    fun aContradictionGivesNull() {
        assertNull(NonogramSolver.solveLine(intArrayOf(f, f, f, u), listOf(2)))
        assertNull(NonogramSolver.solveLine(IntArray(3) { u }, listOf(2, 2)))
    }

    @Test
    fun aPictureThatLogicSolvesIsFoundAndTheOrderStartsWithTheCertainCells() {
        val heart = picture(
            ".##..##.",
            "########",
            "########",
            "########",
            ".######.",
            "..####..",
            "...##...",
            "........",
        )
        assertTrue(NonogramSolver.isSolvable(heart))
        val rows = heart.map(::lineClues)
        val columns = List(8) { c -> lineClues(List(8) { r -> heart[r][c] }) }
        val (order, complete) = NonogramSolver.deduce(8, rows, columns)
        assertTrue(complete)
        assertEquals(64, order.size)
        assertEquals(64, order.map { it.first }.toSet().size, "every cell once")
        // What it says is what the picture holds.
        for ((cell, value) in order) assertEquals(heart[cell / 8][cell % 8], value == f)
    }

    @Test
    fun aPictureThatNeedsGuessingIsRejected() {
        // Two diagonals: the numbers fit both, so logic cannot choose.
        assertFalse(NonogramSolver.isSolvable(picture("#.", ".#")))
        assertFalse(NonogramSolver.isSolvable(picture("#.#.", ".#.#", "#.#.", ".#.#")))
    }
}
