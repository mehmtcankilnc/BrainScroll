package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CrosswordStatsTest {

    private var clock = 0L

    private fun word() = FinishedRound(
        id = "w$clock", mode = Mode.ENDLESS, language = Language.EN, answer = "CRANE",
        guesses = listOf("A", "B"), outcome = Outcome.WON, finishedAt = clock++,
    )

    /** A result of the daily crossword: its only guess is the score code, 1 + the mistakes. */
    private fun daily(durationMs: Long, code: Int = 1, language: Language = Language.EN, mode: Mode = Mode.DAILY) = FinishedRound(
        id = "x$clock", mode = mode, language = language, answer = "b3",
        guesses = listOf(code.toString()), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Crossword,
    )

    @Test
    fun noCrosswordResultsMeansEmptyStats() {
        val stats = computeCrosswordStats(listOf(word()))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertEquals(0, stats.threeStars)
    }

    @Test
    fun theStatsCountTheDailyResultsOfBothLanguagesAndKeepTheFastest() {
        val stats = computeCrosswordStats(listOf(word(), daily(900_000), daily(400_000, language = Language.TR), daily(700_000)))
        assertEquals(3, stats.played)
        assertEquals(400_000L, stats.bestTimeMs)
    }

    @Test
    fun threeStarBoardsAreCountedFromTheScoreCode() {
        val stats = computeCrosswordStats(listOf(daily(1, code = 1), daily(1, code = 4), daily(1, code = 5), daily(1, code = 12)))
        assertEquals(2, stats.threeStars, "up to three mistakes (codes 1 to 4)")
    }

    @Test
    fun aResultThatIsNotFromTheDailyPuzzleIsLeftOut() {
        assertEquals(0, computeCrosswordStats(listOf(daily(10_000, mode = Mode.ENDLESS))).played)
    }
}
