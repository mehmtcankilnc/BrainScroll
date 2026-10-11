package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TargetStatsTest {

    private var clock = 0L

    private fun word(outcome: Outcome) = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = outcome, wasSkipped = false, finishedAt = clock++,
    )

    private fun target(moves: Int, durationMs: Long) = FinishedRound(
        id = "t$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "2367>48",
        guesses = listOf(moves.toString()), outcome = Outcome.WON, wasSkipped = false, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Target,
    )

    @Test
    fun noTargetResultsMeansEmptyStats() {
        val stats = computeTargetStats(listOf(word(Outcome.WON)))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertNull(stats.fewestMoves)
    }

    @Test
    fun targetStatsCountOnlyTargetResultsAndKeepTheFastestAndTheFewestMoves() {
        val stats = computeTargetStats(listOf(word(Outcome.WON), target(5, 90_000), target(3, 40_000), target(4, 70_000)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
        assertEquals(3, stats.fewestMoves)
    }

    @Test
    fun aTargetWinRaisesTheAnswerStreakLikeAnyOtherWin() {
        val history = listOf(word(Outcome.WON), target(3, 50_000), word(Outcome.LOST), target(2, 10_000))
        assertEquals(1, computeStats(history).currentStreak)
        assertEquals(2, computeStats(history).bestStreak)
    }
}
