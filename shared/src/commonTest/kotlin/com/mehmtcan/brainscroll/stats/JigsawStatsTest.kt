package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JigsawStatsTest {

    private var clock = 0L

    private fun result(game: GameKind, durationMs: Long, assisted: Boolean = false) = FinishedRound(
        id = "r$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "9x9x28:5",
        guesses = listOf("40"), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = game, assisted = assisted,
    )

    @Test
    fun noJigsawResultsMeansEmptyStats() {
        val stats = computeJigsawStats(listOf(result(GameKind.Sudoku, 10_000)))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
    }

    @Test
    fun theTwoSudokusAreCountedApartAndTheFastestWinIsKept() {
        val history = listOf(result(GameKind.Sudoku, 5_000), result(GameKind.Jigsaw, 90_000), result(GameKind.Jigsaw, 40_000), result(GameKind.Sudoku, 8_000))
        assertEquals(2, computeJigsawStats(history).played)
        assertEquals(40_000L, computeJigsawStats(history).bestTimeMs)
        assertEquals(2, computeSudokuStats(history).played)
        assertEquals(5_000L, computeSudokuStats(history).bestTimeMs)
    }

    @Test
    fun aGuidedBoardIsNeutral() {
        assertEquals(0, computeJigsawStats(listOf(result(GameKind.Jigsaw, 10_000, assisted = true))).played)
    }
}
