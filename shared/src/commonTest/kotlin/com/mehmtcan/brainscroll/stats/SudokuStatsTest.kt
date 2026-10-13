package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SudokuStatsTest {

    private var clock = 0L

    private fun word(outcome: Outcome) = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = outcome, finishedAt = clock++,
    )

    private fun sudoku(outcome: Outcome, durationMs: Long) = FinishedRound(
        id = "s$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "9x9x36:5",
        guesses = listOf("12"), outcome = outcome, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Sudoku,
    )

    @Test
    fun noSudokuResultsMeansEmptyStats() {
        val stats = computeSudokuStats(listOf(word(Outcome.WON)))
        assertEquals(0, stats.played)
        assertEquals(0f, stats.winRate)
        assertNull(stats.bestTimeMs)
    }

    @Test
    fun sudokuStatsCountOnlySudokuResultsAndTheBestTimeIsOfAWin() {
        val stats = computeSudokuStats(
            listOf(word(Outcome.WON), sudoku(Outcome.WON, 90_000), sudoku(Outcome.LOST, 5_000), sudoku(Outcome.WON, 70_000)),
        )
        assertEquals(3, stats.played)
        assertEquals(2, stats.won)
        assertEquals(2f / 3f, stats.winRate)
        assertEquals(70_000L, stats.bestTimeMs, "the fast loss does not count")
    }

    @Test
    fun aSudokuLossBreaksTheAnswerStreakLikeAnyOtherLoss() {
        val history = listOf(word(Outcome.WON), sudoku(Outcome.WON, 50_000), sudoku(Outcome.LOST, 10_000), word(Outcome.WON))
    }
}
