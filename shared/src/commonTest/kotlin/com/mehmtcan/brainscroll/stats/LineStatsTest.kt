package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LineStatsTest {

    private var clock = 0L

    private fun word(outcome: Outcome) = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = outcome, finishedAt = clock++,
    )

    private fun line(moves: Int, durationMs: Long, assisted: Boolean = false, mode: Mode = Mode.ENDLESS) = FinishedRound(
        id = "l$clock", mode = mode, language = Language.EN, answer = "5x5",
        guesses = listOf(moves.toString()), outcome = Outcome.WON, assisted = assisted, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Line,
    )

    @Test
    fun noLineResultsMeansEmptyStats() {
        val stats = computeLineStats(listOf(word(Outcome.WON)))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertNull(stats.fewestMoves)
    }

    @Test
    fun theStatsCountTheFeedResultsAndKeepTheFastestAndTheFewestMoves() {
        val stats = computeLineStats(listOf(word(Outcome.WON), line(31, 90_000), line(27, 40_000), line(25, 70_000)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
        assertEquals(25, stats.fewestMoves)
    }

    @Test
    fun aBoardThatNeededAHintIsLeftOut() {
        assertEquals(0, computeLineStats(listOf(line(25, 50_000, assisted = true))).played)
    }

    @Test
    fun aDailyResultIsLeftOut() {
        assertEquals(0, computeLineStats(listOf(line(25, 10_000, mode = Mode.DAILY))).played)
    }
}
