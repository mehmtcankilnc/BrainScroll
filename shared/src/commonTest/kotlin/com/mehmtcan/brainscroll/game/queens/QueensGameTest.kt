package com.mehmtcan.brainscroll.game.queens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueensGameTest {

    /** A full placement of five queens: rows 0..4 in columns 0, 2, 4, 1, 3. */
    private val solution = listOf(0, 7, 14, 16, 23)

    @Test
    fun theLockedQueensAreOnTheBoardFromTheStart() {
        val game = QueensGame(5, setOf(0, 7))
        val state = game.state
        assertEquals(listOf(0, 7), state.queens.indices.filter { state.queens[it] })
        assertEquals(listOf(0, 7), state.locked.indices.filter { state.locked[it] })
        assertEquals(3, state.par)
        assertEquals(QueensStatus.Playing, state.status)
        assertFalse(state.canRestart)
    }

    @Test
    fun aTapPlacesAQueenAndAnotherTapTakesItAway() {
        val game = QueensGame(5, setOf(0, 7))
        assertEquals(QueensResult.Placed, game.toggle(14))
        assertTrue(game.state.queens[14])
        assertEquals(QueensResult.Removed, game.toggle(14))
        assertFalse(game.state.queens[14])
        assertEquals(2, game.state.moves)
    }

    @Test
    fun aLockedQueenCannotBeTakenAwayAndDoesNotCount() {
        val game = QueensGame(5, setOf(0, 7))
        assertEquals(QueensResult.Ignored, game.toggle(0))
        assertEquals(QueensResult.Ignored, game.toggle(99))
        assertEquals(QueensResult.Ignored, game.toggle(-1))
        assertEquals(0, game.state.moves)
    }

    @Test
    fun queensThatShareARowAColumnOrADiagonalAreMarkedAndTheGameIsNotWon() {
        val game = QueensGame(5, setOf(0, 7))
        game.toggle(4) // same row as the queen on 0
        assertEquals(listOf(0, 4), game.state.attacked.indices.filter { game.state.attacked[it] })
        game.toggle(4)
        game.toggle(5) // same column as 0
        assertTrue(game.state.attacked[0] && game.state.attacked[5])
        game.toggle(5)
        game.toggle(6) // diagonal of 0 (and of 7: same row)
        assertTrue(game.state.attacked[6])
        assertEquals(QueensStatus.Playing, game.state.status)
    }

    @Test
    fun fiveQueensThatDoNotAttackEachOtherWinTheGame() {
        val game = QueensGame(5, setOf(0, 7))
        assertEquals(QueensResult.Placed, game.toggle(14))
        assertEquals(QueensResult.Placed, game.toggle(16))
        assertEquals(QueensResult.Solved, game.toggle(23))
        assertTrue(game.isFinished)
        assertEquals(QueensStatus.Won, game.state.status)
        assertEquals(3, game.state.moves)
        assertEquals(QueensResult.Ignored, game.toggle(10), "a finished game takes no more taps")
    }

    @Test
    fun fiveQueensWithAConflictAreNotAWin() {
        val game = QueensGame(5, setOf(0, 7))
        game.toggle(14)
        game.toggle(16)
        assertEquals(QueensResult.Placed, game.toggle(24)) // diagonal of 0 and 12... conflicts with 0 on the main diagonal
        assertFalse(game.isFinished)
    }

    @Test
    fun aMoreThanFullBoardIsNotAWin() {
        val game = QueensGame(5, setOf(0, 7))
        for (cell in listOf(10, 14, 16, 23)) game.toggle(cell)
        assertFalse(game.isFinished, "six queens, one of them attacks")
    }

    @Test
    fun aRestartTakesAwayThePlayersQueensAndKeepsTheMoves() {
        val game = QueensGame(5, setOf(0, 7))
        game.toggle(14)
        game.toggle(16)
        assertTrue(game.state.canRestart)
        assertTrue(game.restart())
        val state = game.state
        assertEquals(listOf(0, 7), state.queens.indices.filter { state.queens[it] })
        assertEquals(2, state.moves)
        assertFalse(game.restart(), "nothing to take away")
    }

    @Test
    fun theBoardIsChecked() {
        assertFailsWith<IllegalArgumentException> { QueensGame(2, emptySet()) }
        assertFailsWith<IllegalArgumentException> { QueensGame(5, setOf(25)) }
        assertFailsWith<IllegalArgumentException> { QueensGame(5, solution.toSet()) }
    }
}
