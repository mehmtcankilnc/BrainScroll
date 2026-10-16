package com.mehmtcan.brainscroll.game.crossword

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrosswordDailyTest {

    @Test
    fun starsFollowTheMistakes() {
        assertEquals(3, CrosswordScoring.stars(0))
        assertEquals(3, CrosswordScoring.stars(3))
        assertEquals(2, CrosswordScoring.stars(4))
        assertEquals(2, CrosswordScoring.stars(10))
        assertEquals(1, CrosswordScoring.stars(11))
        assertEquals(1, CrosswordScoring.stars(500))
    }

    @Test
    fun theScoreCodeKeepsTheMistakesAndIsNeverZero() {
        assertEquals(1, CrosswordScoring.encode(0))
        assertEquals(5, CrosswordScoring.encode(4))
        assertEquals(999, CrosswordScoring.encode(5000), "kept within what the server expects")
        for (mistakes in 0..1200) {
            val code = CrosswordScoring.encode(mistakes)
            assertTrue(code in 1..999)
            assertEquals(mistakes.coerceAtMost(998), CrosswordScoring.mistakesOf(code))
            assertEquals(CrosswordScoring.stars(mistakes), CrosswordScoring.starsOf(code), "mistakes $mistakes")
        }
    }

    @Test
    fun theStarsOfAFinishedDailyResultComeFromItsScoreCode() {
        fun result(code: String) = FinishedRound(
            id = "daily-1-crossword", mode = Mode.DAILY, language = Language.TR, answer = "b3", guesses = listOf(code),
            outcome = Outcome.WON, assisted = false, finishedAt = 1, durationMs = 600_000, game = GameKind.Crossword,
        )
        assertEquals(3, result("1").crosswordStars)
        assertEquals(2, result("6").crosswordStars)
        assertEquals(1, result("30").crosswordStars)
    }

    @Test
    fun theShareTextHasTheNameTheNumberTheStarsTheTimeAndTheStreak() {
        assertEquals("Çapraz Bulmaca #42 ⭐⭐⭐ 12:48 🔥7", CrosswordShare.text("Çapraz Bulmaca", 42, 3, "12:48", 7))
        assertEquals("Crossword #1 ⭐ 31:05", CrosswordShare.text("Crossword", 1, 1, "31:05", 0))
        assertEquals("Crossword #3 - 10:00", CrosswordShare.text("Crossword", 3, 0, "10:00", 0))
        assertFalse(CrosswordShare.text("Crossword", 3, 2, "10:00", 0).contains("🔥"))
    }

    @Test
    fun theNumberCountsFromTheLaunchDayAndNeverGoesBelowOne() {
        assertEquals(1, CrosswordSchedule.puzzleNumber(CrosswordSchedule.LAUNCH_DAY_INDEX))
        assertEquals(42, CrosswordSchedule.puzzleNumber(CrosswordSchedule.LAUNCH_DAY_INDEX + 41))
        assertEquals(1, CrosswordSchedule.puzzleNumber(CrosswordSchedule.LAUNCH_DAY_INDEX - 30))
    }

    @Test
    fun theDailyBoardsDoNotRepeatUntilEveryBoardHasBeenPlayed() {
        for (count in listOf(1, 2, 3, 7, 50, 100, 120, 121, 125)) {
            val seen = (0L until count).map { CrosswordSchedule.boardNumber(20_740L + it, count) }
            assertEquals(count, seen.toSet().size, "$count boards")
            assertTrue(seen.all { it in 0 until count })
            assertEquals(seen, (0L until count).map { CrosswordSchedule.boardNumber(20_740L + count + it, count) }, "the cycle starts again")
        }
    }

    @Test
    fun consecutiveDaysDoNotPlayNeighbouringBoards() {
        val count = CrosswordBoards.of(Language.TR).size
        val a = CrosswordSchedule.boardNumber(20_740L, count)
        val b = CrosswordSchedule.boardNumber(20_741L, count)
        assertTrue(minOf((a - b).mod(count), (b - a).mod(count)) > 1, "$a and $b")
    }

    @Test
    fun aDayAndALanguageAlwaysMakeTheSameBoard() {
        for (language in Language.entries) {
            val first = CrosswordSchedule.puzzleFor(20_745L, language)
            assertEquals(first.squares, CrosswordSchedule.puzzleFor(20_745L, language).squares)
            assertEquals(language, first.language)
            assertTrue(first.encode().startsWith("b"), "a board of the list is saved by its number")
        }
        assertTrue(CrosswordSchedule.puzzleFor(20_745L, Language.TR).squares != CrosswordSchedule.puzzleFor(20_746L, Language.TR).squares)
    }
}
