package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TopicGroupsStatsTest {

    private var clock = 0L

    private fun word() = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = Outcome.WON, finishedAt = clock++,
    )

    private fun groups(durationMs: Long, mistakes: Int = 0, assisted: Boolean = false) = FinishedRound(
        id = "a$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "en:1.2.3.4:5",
        guesses = listOf(mistakes.toString()), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.TopicGroups, assisted = assisted,
    )

    @Test
    fun noTopicGroupsResultsMeansEmptyStats() {
        val stats = computeTopicGroupsStats(listOf(word()))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertEquals(0, stats.flawless)
    }

    @Test
    fun theStatsCountOnlyTopicGroupsResultsKeepTheFastestAndCountFlawlessBoards() {
        val stats = computeTopicGroupsStats(listOf(word(), groups(90_000, 2), groups(40_000, 0), groups(70_000, 0)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
        assertEquals(2, stats.flawless)
    }

    @Test
    fun aGuidedBoardIsNeutral() {
        assertEquals(0, computeTopicGroupsStats(listOf(groups(10_000, assisted = true))).played)
    }
}
