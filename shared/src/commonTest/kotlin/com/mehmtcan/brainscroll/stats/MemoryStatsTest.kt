package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Statistics when the history holds results of more than one game. */
class MemoryStatsTest {

    private var clock = 0L

    private fun word(outcome: Outcome, guesses: Int = 3) = FinishedRound(
        id = "w${clock}", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = List(guesses) { "WORD$it" }, outcome = outcome, finishedAt = clock++,
    )

    /** A memory result: the flips are the guesses, two flips make a move. */
    private fun memory(outcome: Outcome, moves: Int = 8, durationMs: Long? = 30_000) = FinishedRound(
        id = "m${clock}", mode = Mode.ENDLESS, language = Language.EN, answer = "0,1,2,0,1,2",
        guesses = List(moves * 2) { it.toString() }, outcome = outcome, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Memory,
    )

    @Test
    fun noMemoryResultsMeansEmptyStats() {
        val stats = computeMemoryStats(listOf(word(Outcome.WON)))
        assertEquals(0, stats.played)
        assertEquals(0f, stats.winRate)
        assertNull(stats.bestTimeMs)
        assertNull(stats.fewestMoves)
    }

    @Test
    fun memoryStatsCountOnlyMemoryResults() {
        val history = listOf(
            word(Outcome.WON), word(Outcome.LOST),
            memory(Outcome.WON, moves = 9, durationMs = 41_000),
            memory(Outcome.WON, moves = 7, durationMs = 52_000),
            memory(Outcome.LOST, moves = 12, durationMs = 20_000),
        )
        val stats = computeMemoryStats(history)
        assertEquals(3, stats.played)
        assertEquals(2, stats.won)
        assertEquals(2f / 3f, stats.winRate)
        assertEquals(41_000L, stats.bestTimeMs, "the fastest WIN: the fast loss does not count")
        assertEquals(7, stats.fewestMoves, "the fewest moves in a win")
    }

    @Test
    fun theEndlessStatsAreForAllGamesButGuessesPerWinAreWordsOnly() {
        val history = listOf(word(Outcome.WON, guesses = 2), memory(Outcome.WON, moves = 3), word(Outcome.LOST), memory(Outcome.WON))
        val all = computeStats(history)
        assertEquals(4, all.played)
        assertEquals(3, all.won)
        assertEquals(listOf(0, 1, 0, 0, 0, 0), all.guessDistribution, "only the word win, in 2 guesses")
        assertNull(all.bestTimeMs, "memory times are not word times")
    }

    @Test
    fun aGameCanBeAskedForAlone() {
        val history = listOf(word(Outcome.WON), memory(Outcome.WON), memory(Outcome.LOST))
        assertEquals(1, computeStats(history, game = GameKind.Word).played)
        assertEquals(2, computeStats(history, game = GameKind.Memory).played)
    }

    @Test
    fun dailyResultsAreNeverMemory() {
        val daily = FinishedRound("d1", Mode.DAILY, Language.EN, "CRANE", listOf("CRANE"), Outcome.WON, 5L, durationMs = 9_000)
        assertEquals(0, computeMemoryStats(listOf(daily)).played)
    }
}
