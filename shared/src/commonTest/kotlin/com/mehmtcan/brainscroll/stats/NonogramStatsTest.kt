package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NonogramStatsTest {

    private var clock = 0L

    private fun word() = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = Outcome.WON, finishedAt = clock++,
    )

    private fun nonogram(durationMs: Long, assisted: Boolean = false) = FinishedRound(
        id = "t$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "8:0000000000000001:0",
        guesses = listOf("12"), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Nonogram, assisted = assisted,
    )

    @Test
    fun noNonogramResultsMeansEmptyStats() {
        val stats = computeNonogramStats(listOf(word()))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
    }

    @Test
    fun theStatsCountOnlyNonogramResultsAndKeepTheFastest() {
        val stats = computeNonogramStats(listOf(word(), nonogram(90_000), nonogram(40_000), nonogram(70_000)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
    }

    @Test
    fun aGuidedBoardIsNeutral() {
        assertEquals(0, computeNonogramStats(listOf(nonogram(10_000, assisted = true))).played)
    }
}
