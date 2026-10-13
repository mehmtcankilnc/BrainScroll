package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TubesStatsTest {

    private var clock = 0L

    private fun word(outcome: Outcome) = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = outcome, finishedAt = clock++,
    )

    /** A tubes result keeps its number of pours as its only guess. */
    private fun tubes(moves: Int, durationMs: Long) = FinishedRound(
        id = "t$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "0011|1100||",
        guesses = listOf(moves.toString()), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Tubes,
    )

    @Test
    fun noTubesResultsMeansEmptyStats() {
        val stats = computeTubesStats(listOf(word(Outcome.WON)))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertNull(stats.fewestMoves)
    }

    @Test
    fun tubesStatsCountOnlyTubesResultsAndTakeTheBestOfEach() {
        val stats = computeTubesStats(listOf(word(Outcome.WON), tubes(22, 90_000), tubes(15, 120_000), tubes(30, 60_000)))
        assertEquals(3, stats.played)
        assertEquals(60_000L, stats.bestTimeMs)
        assertEquals(15, stats.fewestMoves)
    }

    @Test
    fun aTubesWinDoesNotCountForTheGuessDistribution() {
        val history = listOf(word(Outcome.WON), tubes(20, 50_000), word(Outcome.WON))
        val all = computeStats(history)
        assertEquals(3, all.played, "the numbers span every game")
        assertEquals(2, all.guessDistribution.sum(), "only the two word puzzles have guesses")
    }
}
