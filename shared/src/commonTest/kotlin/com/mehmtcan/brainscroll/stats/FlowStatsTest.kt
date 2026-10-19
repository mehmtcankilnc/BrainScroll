package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FlowStatsTest {

    private var clock = 0L

    private fun flow(durationMs: Long, moves: Int, assisted: Boolean = false) = FinishedRound(
        id = "f$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "0",
        guesses = listOf(moves.toString()), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.ColorFlow, assisted = assisted,
    )

    @Test
    fun noResultsMeansEmptyStats() {
        val stats = computeFlowStats(emptyList())
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertNull(stats.fewestMoves)
    }

    @Test
    fun theStatsKeepTheFastestAndTheFewestStrokes() {
        val stats = computeFlowStats(listOf(flow(90_000, 12), flow(40_000, 9), flow(70_000, 10)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
        assertEquals(9, stats.fewestMoves)
    }

    @Test
    fun aGuidedBoardIsNeutral() {
        assertEquals(0, computeFlowStats(listOf(flow(10_000, 5, assisted = true))).played)
    }
}
