package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BallSlideStatsTest {

    private var clock = 0L

    private fun ball(durationMs: Long, moves: Int, assisted: Boolean = false) = FinishedRound(
        id = "b$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "0",
        guesses = listOf(moves.toString()), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.BallSlide, assisted = assisted,
    )

    @Test
    fun noResultsMeansEmptyStats() {
        val stats = computeBallSlideStats(emptyList())
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertNull(stats.fewestMoves)
    }

    @Test
    fun theStatsKeepTheFastestAndTheFewestSlides() {
        val stats = computeBallSlideStats(listOf(ball(90_000, 9), ball(40_000, 6), ball(70_000, 8)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
        assertEquals(6, stats.fewestMoves)
    }

    @Test
    fun aGuidedBoardIsNeutral() {
        assertEquals(0, computeBallSlideStats(listOf(ball(10_000, 5, assisted = true))).played)
    }
}
