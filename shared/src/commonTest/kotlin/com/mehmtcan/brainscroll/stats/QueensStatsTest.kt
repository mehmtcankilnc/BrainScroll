package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QueensStatsTest {

    private var clock = 0L

    private fun word(outcome: Outcome) = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = outcome, finishedAt = clock++,
    )

    private fun queens(moves: Int, durationMs: Long, assisted: Boolean = false) = FinishedRound(
        id = "q$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "5:0,7",
        guesses = listOf(moves.toString()), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Queens, assisted = assisted,
    )

    @Test
    fun noQueensResultsMeansEmptyStats() {
        val stats = computeQueensStats(listOf(word(Outcome.WON)))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertNull(stats.fewestMoves)
    }

    @Test
    fun theStatsCountOnlyQueensResultsAndKeepTheFastestAndTheFewestMoves() {
        val stats = computeQueensStats(listOf(word(Outcome.WON), queens(9, 90_000), queens(3, 40_000), queens(5, 70_000)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
        assertEquals(3, stats.fewestMoves)
    }

    @Test
    fun aGuidedBoardIsNeutral() {
        assertEquals(0, computeQueensStats(listOf(queens(3, 10_000, assisted = true))).played)
    }
}
