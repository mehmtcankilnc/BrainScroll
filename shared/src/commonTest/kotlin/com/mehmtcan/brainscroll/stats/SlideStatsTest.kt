package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SlideStatsTest {

    private var clock = 0L

    private fun word(outcome: Outcome) = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = outcome, finishedAt = clock++,
    )

    private fun slide(moves: Int, durationMs: Long) = FinishedRound(
        id = "l$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "3x4x80:5",
        guesses = listOf(moves.toString()), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Slide,
    )

    @Test
    fun noSlideResultsMeansEmptyStats() {
        val stats = computeSlideStats(listOf(word(Outcome.WON)))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertNull(stats.fewestMoves)
    }

    @Test
    fun slideStatsCountOnlySlideResultsAndKeepTheFastestAndTheFewestMoves() {
        val stats = computeSlideStats(listOf(word(Outcome.WON), slide(9, 90_000), slide(12, 40_000), slide(7, 70_000)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
        assertEquals(7, stats.fewestMoves)
    }

    @Test
    fun aSlideWinRaisesTheAnswerStreakLikeAnyOtherWin() {
        val history = listOf(word(Outcome.WON), slide(8, 50_000), word(Outcome.LOST), slide(7, 10_000))
        assertEquals(1, computeStats(history).currentStreak)
        assertEquals(2, computeStats(history).bestStreak)
    }
}
