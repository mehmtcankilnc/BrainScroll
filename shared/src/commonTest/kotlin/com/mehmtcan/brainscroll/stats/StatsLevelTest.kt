package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals

/** The per-game statistics are read for one level at a time. */
class StatsLevelTest {

    private var clock = 0L

    private fun sudoku(durationMs: Long, level: Difficulty) = FinishedRound(
        id = "s${clock}", mode = Mode.ENDLESS, language = Language.EN, answer = "9x9x36:7", guesses = listOf("0"),
        outcome = Outcome.WON, finishedAt = clock++, durationMs = durationMs, game = GameKind.Sudoku, difficulty = level,
    )

    private fun hanoi(durationMs: Long, moves: Int, level: Difficulty) = FinishedRound(
        id = "h${clock}", mode = Mode.ENDLESS, language = Language.EN, answer = "x", guesses = listOf(moves.toString()),
        outcome = Outcome.WON, finishedAt = clock++, durationMs = durationMs, game = GameKind.Hanoi, difficulty = level,
    )

    private val history = listOf(
        sudoku(30_000, Difficulty.Easy), sudoku(90_000, Difficulty.Medium), sudoku(80_000, Difficulty.Medium), sudoku(200_000, Difficulty.Hard),
        hanoi(10_000, 7, Difficulty.Easy), hanoi(40_000, 15, Difficulty.Hard), hanoi(35_000, 13, Difficulty.Hard),
    )

    @Test
    fun theHistoryAtALevelHoldsOnlyThatLevel() {
        assertEquals(listOf(Difficulty.Easy, Difficulty.Easy), history.atLevel(Difficulty.Easy).map { it.difficulty })
        assertEquals(2, history.atLevel(Difficulty.Medium).size)
        assertEquals(3, history.atLevel(Difficulty.Hard).size)
    }

    @Test
    fun theBestTimeOfALevelIsNotBeatenByAnotherLevel() {
        assertEquals(30_000L, computeSudokuStats(history.atLevel(Difficulty.Easy)).bestTimeMs)
        assertEquals(80_000L, computeSudokuStats(history.atLevel(Difficulty.Medium)).bestTimeMs)
        assertEquals(200_000L, computeSudokuStats(history.atLevel(Difficulty.Hard)).bestTimeMs)
        assertEquals(30_000L, computeSudokuStats(history).bestTimeMs, "with no filter the easy time would win everywhere")
    }

    @Test
    fun theCountsAndFewestMovesFollowTheLevel() {
        val hard = computeHanoiStats(history.atLevel(Difficulty.Hard))
        assertEquals(2, hard.played)
        assertEquals(13, hard.fewestMoves)
        assertEquals(1, computeHanoiStats(history.atLevel(Difficulty.Easy)).played)
        assertEquals(0, computeHanoiStats(history.atLevel(Difficulty.Medium)).played)
    }
}
