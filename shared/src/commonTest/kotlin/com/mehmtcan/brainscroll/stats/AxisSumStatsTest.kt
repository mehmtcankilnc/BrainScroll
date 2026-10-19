package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AxisSumStatsTest {

    private var clock = 0L

    private fun axis(durationMs: Long, taps: Int, assisted: Boolean = false) = FinishedRound(
        id = "a$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "0",
        guesses = listOf(taps.toString()), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.AxisSum, assisted = assisted,
    )

    @Test
    fun noResultsMeansEmptyStats() {
        val stats = computeAxisSumStats(emptyList())
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertNull(stats.fewestTaps)
    }

    @Test
    fun theStatsKeepTheFastestAndTheFewestTaps() {
        val stats = computeAxisSumStats(listOf(axis(90_000, 12), axis(40_000, 9), axis(70_000, 15)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
        assertEquals(9, stats.fewestTaps)
    }

    @Test
    fun aGuidedBoardIsNeutral() {
        assertEquals(0, computeAxisSumStats(listOf(axis(10_000, 5, assisted = true))).played)
    }
}
