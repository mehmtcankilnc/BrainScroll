package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals

/** A puzzle done with the guide is in the history, but it is neutral: no streak and no statistic counts it. */
class AssistedTest {

    private var clock = 0L

    private fun round(outcome: Outcome, assisted: Boolean = false, game: GameKind = GameKind.Mines, moves: Int = 20, ms: Long = 30_000) = FinishedRound(
        id = "r${clock}",
        mode = Mode.ENDLESS,
        language = Language.EN,
        answer = "x",
        guesses = listOf(moves.toString()),
        outcome = outcome,
        assisted = assisted,
        finishedAt = clock++,
        durationMs = ms,
        game = game,
    )

    @Test
    fun theWinStreakSkipsGuidedPuzzlesInBothDirections() {
        val history = listOf(
            round(Outcome.WON),
            round(Outcome.WON, assisted = true),
            round(Outcome.LOST, assisted = true),
            round(Outcome.WON),
        )
        assertEquals(2, Streaks.current(history), "the two unguided wins, the guided loss does not break them")
        assertEquals(2, Streaks.best(history))
    }

    @Test
    fun aGuidedLossAtTheEndDoesNotResetTheStreak() {
        val history = listOf(round(Outcome.WON), round(Outcome.WON), round(Outcome.LOST, assisted = true))
        assertEquals(2, Streaks.current(history))
    }

    @Test
    fun theStatisticsOfAGameLeaveGuidedPuzzlesOut() {
        val history = listOf(
            round(Outcome.WON, moves = 40, ms = 90_000),
            round(Outcome.WON, assisted = true, moves = 10, ms = 5_000),
        )
        val stats = computeMinesStats(history)
        assertEquals(1, stats.played)
        assertEquals(90_000L, stats.bestTimeMs, "a guided win does not make a record")
    }

    @Test
    fun theTotalsLeaveGuidedPuzzlesOut() {
        val history = listOf(round(Outcome.WON), round(Outcome.WON, assisted = true), round(Outcome.LOST))
        val stats = computeStats(history)
        assertEquals(2, stats.played)
        assertEquals(1, stats.won)
    }
}
