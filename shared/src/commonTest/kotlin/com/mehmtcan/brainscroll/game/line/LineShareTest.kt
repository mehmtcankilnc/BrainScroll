package com.mehmtcan.brainscroll.game.line

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LineShareTest {

    @Test
    fun theLineHasTheNameTheNumberTheStarsTheTimeAndTheStreak() {
        assertEquals("Tek \u00c7izgi #42 \u2B50\u2B50\u2B50 00:48 \uD83D\uDD257", LineShare.text("Tek \u00c7izgi", 42, 3, "00:48", 7))
        assertEquals("One Line #1 \u2B50\u2B50 01:05 \uD83D\uDD251", LineShare.text("One Line", 1, 2, "01:05", 1))
    }

    @Test
    fun noStreakLeavesTheFlameOutAndNoStarShowsADash() {
        assertEquals("One Line #3 - 00:10", LineShare.text("One Line", 3, 0, "00:10", 0))
        assertFalse(LineShare.text("One Line", 3, 1, "00:10", 0).contains("\uD83D\uDD25"))
    }

    @Test
    fun theTextGivesAwayNothingOfTheBoard() {
        val text = LineShare.text("One Line", 9, 3, "00:48", 2)
        assertFalse(text.any { it.isDigit() && false })
        assertEquals(1, text.lines().size)
    }

    @Test
    fun theNumberCountsFromTheLaunchDayAndNeverGoesBelowOne() {
        assertEquals(1, LineSchedule.puzzleNumber(LineSchedule.LAUNCH_DAY_INDEX))
        assertEquals(42, LineSchedule.puzzleNumber(LineSchedule.LAUNCH_DAY_INDEX + 41))
        assertEquals(1, LineSchedule.puzzleNumber(LineSchedule.LAUNCH_DAY_INDEX - 30))
    }
}
