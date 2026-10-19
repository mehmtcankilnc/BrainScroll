package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArrowsStatsTest {

    private var clock = 0L

    private fun word() = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = Outcome.WON, finishedAt = clock++,
    )

    private fun arrows(durationMs: Long, mistakes: Int = 0, assisted: Boolean = false) = FinishedRound(
        id = "a$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "6:1:2",
        guesses = listOf(mistakes.toString()), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Arrows, assisted = assisted,
    )

    @Test
    fun noArrowsResultsMeansEmptyStats() {
        val stats = computeArrowsStats(listOf(word()))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertEquals(0, stats.flawless)
    }

    @Test
    fun theStatsCountOnlyArrowsResultsKeepTheFastestAndCountFlawlessBoards() {
        val stats = computeArrowsStats(listOf(word(), arrows(90_000, 2), arrows(40_000, 0), arrows(70_000, 0)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
        assertEquals(2, stats.flawless)
    }

    @Test
    fun aGuidedBoardIsNeutral() {
        assertEquals(0, computeArrowsStats(listOf(arrows(10_000, assisted = true))).played)
    }
}
