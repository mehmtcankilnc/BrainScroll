package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome

/**
 * The answer streak of the endless feed, derived from the saved history (docs/decisions.md):
 * every win adds one, a loss resets it. Puzzles that were skipped and never finished are not in the history,
 * so skipping does not break it. [history] must be oldest first, as the repository returns it.
 */
object Streaks {
    fun current(history: List<FinishedRound>): Int {
        var streak = 0
        for (round in history.asReversed()) {
            if (round.mode != Mode.ENDLESS) continue
            if (round.outcome == Outcome.LOST) break
            streak++
        }
        return streak
    }

    fun best(history: List<FinishedRound>): Int {
        var best = 0
        var run = 0
        for (round in history) {
            if (round.mode != Mode.ENDLESS) continue
            run = if (round.outcome == Outcome.WON) run + 1 else 0
            if (run > best) best = run
        }
        return best
    }
}

/** What the statistics screen shows. */
data class Stats(
    val played: Int,
    val won: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    /** How many puzzles were won with 1, 2, ... guesses. Index 0 is "won in 1 guess". */
    val guessDistribution: List<Int>,
    /** Puzzles that were finished after the player had left them once. */
    val finishedAfterSkip: Int,
) {
    /** Share of puzzles that were won, from 0.0 to 1.0. Zero when nothing was played. */
    val winRate: Float get() = if (played == 0) 0f else won.toFloat() / played
}

/**
 * Statistics of the endless feed, optionally for one [language]. Everything is computed from [history],
 * so the numbers can never drift away from the saved results.
 */
fun computeStats(history: List<FinishedRound>, language: Language? = null, maxAttempts: Int = 6): Stats {
    val endless = history.filter { it.mode == Mode.ENDLESS && (language == null || it.language == language) }
    val wins = endless.filter { it.outcome == Outcome.WON }
    val distribution = MutableList(maxAttempts) { 0 }
    for (win in wins) {
        val attempts = win.guesses.size
        if (attempts in 1..maxAttempts) distribution[attempts - 1]++
    }
    return Stats(
        played = endless.size,
        won = wins.size,
        currentStreak = Streaks.current(endless),
        bestStreak = Streaks.best(endless),
        guessDistribution = distribution,
        finishedAfterSkip = endless.count { it.wasSkipped },
    )
}
