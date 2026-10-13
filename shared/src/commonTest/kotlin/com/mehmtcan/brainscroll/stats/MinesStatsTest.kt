package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MinesStatsTest {

    private var clock = 0L

    private fun word(outcome: Outcome) = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = outcome, finishedAt = clock++,
    )

    private fun mines(outcome: Outcome, durationMs: Long) = FinishedRound(
        id = "s$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "8x8x10:5:27",
        guesses = listOf("12"), outcome = outcome, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Mines,
    )

    @Test
    fun noMinesResultsMeansEmptyStats() {
        val stats = computeMinesStats(listOf(word(Outcome.WON)))
        assertEquals(0, stats.played)
        assertEquals(0f, stats.winRate)
        assertNull(stats.bestTimeMs)
    }

    @Test
    fun minesStatsCountOnlyMinesResultsAndTheBestTimeIsOfAWin() {
        val stats = computeMinesStats(
            listOf(word(Outcome.WON), mines(Outcome.WON, 90_000), mines(Outcome.LOST, 5_000), mines(Outcome.WON, 70_000)),
        )
        assertEquals(3, stats.played)
        assertEquals(2, stats.won)
        assertEquals(2f / 3f, stats.winRate)
        assertEquals(70_000L, stats.bestTimeMs, "the fast loss does not count")
    }

    @Test
    fun aMinesLossBreaksTheAnswerStreakLikeAnyOtherLoss() {
        val history = listOf(word(Outcome.WON), mines(Outcome.WON, 50_000), mines(Outcome.LOST, 10_000), word(Outcome.WON))
    }
}
