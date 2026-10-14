package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.line.LineScoring
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

    private fun line(undos: Int, hints: Int, durationMs: Long, mode: Mode = Mode.DAILY) = FinishedRound(
        id = "l$clock", mode = mode, language = Language.EN, answer = "5x5",
        guesses = listOf(LineScoring.encode(undos, hints).toString()), outcome = Outcome.WON, finishedAt = clock++,
        durationMs = durationMs, game = GameKind.Line, puzzleDay = clock,
    )

    @Test
    fun noLineResultsMeansEmptyStats() {
        val stats = computeLineStats(listOf(word(Outcome.WON)))
        assertEquals(0, stats.played)
        assertNull(stats.bestTimeMs)
        assertEquals(0, stats.threeStars)
    }

    @Test
    fun theStatsCountTheDailyResultsKeepTheFastestAndCountThreeStarBoards() {
        val stats = computeLineStats(listOf(word(Outcome.WON), line(0, 0, 90_000), line(2, 0, 40_000), line(0, 1, 70_000), line(0, 0, 80_000)))
        assertEquals(4, stats.played)
        assertEquals(40_000L, stats.bestTimeMs)
        assertEquals(2, stats.threeStars, "a hint takes a star off, so that board is not a three-star one")
    }

    @Test
    fun aHintedBoardStillCountsAsPlayed() {
        assertEquals(1, computeLineStats(listOf(line(0, 3, 50_000))).played)
    }

    @Test
    fun aLineResultThatIsNotADailyOneIsLeftOut() {
        assertEquals(0, computeLineStats(listOf(line(0, 0, 10_000, Mode.ENDLESS))).played)
    }
}
