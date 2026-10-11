package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PipesStatsTest {

    private var clock = 0L

    private fun word(outcome: Outcome) = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = outcome, finishedAt = clock++,
    )

    private fun pipes(moves: Int, durationMs: Long) = FinishedRound(
        id = "l$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "0123456789abcdef0123456789",
        guesses = listOf(moves.toString()), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Pipes,
    )

    @Test
    fun noPipesResultsMeansEmptyStats() {
        val stats = computePipesStats(listOf(word(Outcome.WON)))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertNull(stats.fewestMoves)
    }

    @Test
    fun pipesStatsCountOnlyPipesResultsAndKeepTheFastestAndTheFewestMoves() {
        val stats = computePipesStats(listOf(word(Outcome.WON), pipes(9, 90_000), pipes(12, 40_000), pipes(7, 70_000)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
        assertEquals(7, stats.fewestMoves)
    }

    @Test
    fun aPipesWinRaisesTheAnswerStreakLikeAnyOtherWin() {
        val history = listOf(word(Outcome.WON), pipes(8, 50_000), word(Outcome.LOST), pipes(7, 10_000))
        assertEquals(1, computeStats(history).currentStreak)
        assertEquals(2, computeStats(history).bestStreak)
    }
}
