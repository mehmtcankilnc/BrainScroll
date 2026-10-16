package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TentsStatsTest {

    private var clock = 0L

    private fun word() = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = Outcome.WON, finishedAt = clock++,
    )

    private fun tents(durationMs: Long, assisted: Boolean = false) = FinishedRound(
        id = "t$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "6:1:2",
        guesses = listOf("12"), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Tents, assisted = assisted,
    )

    @Test
    fun noTentsResultsMeansEmptyStats() {
        val stats = computeTentsStats(listOf(word()))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
    }

    @Test
    fun theStatsCountOnlyTentsResultsAndKeepTheFastest() {
        val stats = computeTentsStats(listOf(word(), tents(90_000), tents(40_000), tents(70_000)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
    }

    @Test
    fun aGuidedBoardIsNeutral() {
        assertEquals(0, computeTentsStats(listOf(tents(10_000, assisted = true))).played)
    }
}
