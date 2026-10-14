package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HanoiStatsTest {

    private var clock = 0L

    private fun word(outcome: Outcome) = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = outcome, finishedAt = clock++,
    )

    private fun hanoi(moves: Int, durationMs: Long, assisted: Boolean = false) = FinishedRound(
        id = "h$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "0120>2",
        guesses = listOf(moves.toString()), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Hanoi, assisted = assisted,
    )

    @Test
    fun noHanoiResultsMeansEmptyStats() {
        val stats = computeHanoiStats(listOf(word(Outcome.WON)))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertNull(stats.fewestMoves)
    }

    @Test
    fun theStatsCountOnlyHanoiResultsAndKeepTheFastestAndTheFewestMoves() {
        val stats = computeHanoiStats(listOf(word(Outcome.WON), hanoi(9, 90_000), hanoi(12, 40_000), hanoi(7, 70_000)))
        assertEquals(3, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
        assertEquals(7, stats.fewestMoves)
    }

    @Test
    fun aGuidedBoardIsNeutral() {
        assertEquals(0, computeHanoiStats(listOf(hanoi(5, 10_000, assisted = true))).played)
    }
}
