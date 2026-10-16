package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlockOutStatsTest {

    private var clock = 0L

    private fun word() = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = Outcome.WON, finishedAt = clock++,
    )

    private fun block(moves: Int, durationMs: Long, assisted: Boolean = false) = FinishedRound(
        id = "b$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "XX...................XX...".take(25),
        guesses = listOf(moves.toString()), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.BlockOut, assisted = assisted,
    )

    @Test
    fun noBlockOutResultsMeansEmptyStats() {
        val stats = computeBlockOutStats(listOf(word()))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertNull(stats.fewestMoves)
    }

    @Test
    fun theStatsCountOnlyBlockOutResultsAndKeepTheFastestAndTheFewestMoves() {
        val stats = computeBlockOutStats(listOf(word(), block(19, 90_000), block(12, 40_000), block(15, 70_000)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
        assertEquals(12, stats.fewestMoves)
    }

    @Test
    fun aGuidedBoardIsNeutral() {
        assertEquals(0, computeBlockOutStats(listOf(block(9, 10_000, assisted = true))).played)
    }
}
