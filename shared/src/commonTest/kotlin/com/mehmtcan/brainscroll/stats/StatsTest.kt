package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsTest {

    private var clock = 0L

    private fun round(
        outcome: Outcome,
        guesses: Int = 3,
        language: Language = Language.EN,
        mode: Mode = Mode.ENDLESS,
        skipped: Boolean = false,
    ) = FinishedRound(
        id = "r${clock}",
        mode = mode,
        language = language,
        answer = "CRANE",
        guesses = List(guesses) { "WORD$it" },
        outcome = outcome,
        wasSkipped = skipped,
        finishedAt = clock++,
    )

    private fun won(guesses: Int = 3, language: Language = Language.EN) = round(Outcome.WON, guesses, language)
    private fun lost(language: Language = Language.EN) = round(Outcome.LOST, 6, language)

    @Test
    fun emptyHistoryHasNoStreakAndZeroStats() {
        val stats = computeStats(emptyList())
        assertEquals(0, stats.played)
        assertEquals(0, stats.currentStreak)
        assertEquals(0f, stats.winRate)
        assertEquals(List(6) { 0 }, stats.guessDistribution)
    }

    @Test
    fun currentStreakCountsTrailingWins() {
        assertEquals(3, Streaks.current(listOf(lost(), won(), won(), won())))
    }

    @Test
    fun aLossResetsTheCurrentStreak() {
        assertEquals(0, Streaks.current(listOf(won(), won(), lost())))
    }

    @Test
    fun bestStreakIsTheLongestRunEvenIfItIsOver() {
        val history = listOf(won(), won(), won(), lost(), won(), won())
        assertEquals(3, Streaks.best(history))
        assertEquals(2, Streaks.current(history))
    }

    @Test
    fun dailyResultsDoNotCountForTheAnswerStreak() {
        val history = listOf(won(), round(Outcome.LOST, mode = Mode.DAILY), won())
        assertEquals(2, Streaks.current(history))
        assertEquals(2, Streaks.best(history))
    }

    @Test
    fun winRateIsWonOverPlayed() {
        val stats = computeStats(listOf(won(), won(), lost(), won()))
        assertEquals(4, stats.played)
        assertEquals(3, stats.won)
        assertEquals(0.75f, stats.winRate)
    }

    @Test
    fun guessDistributionCountsWinsPerNumberOfGuesses() {
        val stats = computeStats(listOf(won(1), won(3), won(3), won(6), lost()))
        assertEquals(listOf(1, 0, 2, 0, 0, 1), stats.guessDistribution)
    }

    @Test
    fun statsCanBeLimitedToOneLanguage() {
        val history = listOf(won(language = Language.EN), lost(language = Language.TR), won(language = Language.TR))
        val tr = computeStats(history, Language.TR)
        assertEquals(2, tr.played)
        assertEquals(1, tr.won)
        assertEquals(1, tr.currentStreak)
        assertEquals(3, computeStats(history).played)
    }

    @Test
    fun finishedAfterSkipIsCounted() {
        val history = listOf(round(Outcome.WON, skipped = true), won(), round(Outcome.LOST, skipped = true))
        assertEquals(2, computeStats(history).finishedAfterSkip)
    }

    @Test
    fun dailyStatsCountOnlyDailyResultsAndFindTheBestTime() {
        fun daily(outcome: Outcome, guesses: Int, durationMs: Long?) =
            round(outcome, guesses, mode = Mode.DAILY).copy(durationMs = durationMs)

        val history = listOf(
            won(), won(), // endless, must not count
            daily(Outcome.WON, 3, 90_000),
            daily(Outcome.WON, 5, 45_000),
            daily(Outcome.LOST, 6, 30_000), // a loss is never the "best time"
        )
        val stats = computeStats(history, mode = Mode.DAILY)
        assertEquals(3, stats.played)
        assertEquals(2, stats.won)
        assertEquals(45_000L, stats.bestTimeMs)
        assertEquals(listOf(0, 0, 1, 0, 1, 0), stats.guessDistribution)
        assertEquals(0, stats.currentStreak) // the day streak lives in DayStreaks, not here
    }

    @Test
    fun endlessStatsIgnoreDailyResultsAndHaveNoBestTime() {
        val history = listOf(won(), round(Outcome.WON, 2, mode = Mode.DAILY).copy(durationMs = 10_000))
        val stats = computeStats(history)
        assertEquals(1, stats.played)
        assertEquals(null, stats.bestTimeMs)
    }

    @Test
    fun noWinsMeansNoBestTime() {
        assertEquals(null, computeStats(listOf(lost()), mode = Mode.ENDLESS).bestTimeMs)
        assertEquals(null, computeStats(emptyList(), mode = Mode.DAILY).bestTimeMs)
    }
}
