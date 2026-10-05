package com.mehmtcan.brainscroll.stats

import kotlin.test.Test
import kotlin.test.assertEquals

class DayStreaksTest {

    /** Day indexes: days 10, 11, 12, ... are consecutive days. */
    private fun days(vararg d: Long) = d.toList()

    @Test
    fun noFinishedDaysMeansNoStreak() {
        assertEquals(DayStreak(0, 0, 0), DayStreaks.compute(emptyList(), today = 10))
    }

    @Test
    fun consecutiveDaysBuildAStreak() {
        val s = DayStreaks.compute(days(10, 11, 12), today = 12)
        assertEquals(3, s.current)
        assertEquals(3, s.best)
    }

    @Test
    fun duplicateDaysCountOnce() {
        assertEquals(2, DayStreaks.compute(days(10, 10, 11, 11), today = 11).current)
    }

    @Test
    fun todayNotFinishedYetDoesNotBreakTheStreak() {
        // Finished 10, 11; today is 12 and still open: the streak is alive.
        assertEquals(2, DayStreaks.compute(days(10, 11), today = 12).current)
    }

    @Test
    fun aMissedDayWithoutAFreezeBreaksTheStreak() {
        // Finished 10, 11; day 12 was missed and today is 13.
        val s = DayStreaks.compute(days(10, 11), today = 13)
        assertEquals(0, s.current)
        assertEquals(2, s.best)
    }

    @Test
    fun theStreakRestartsAtOneAfterABreak() {
        val s = DayStreaks.compute(days(10, 11, 14), today = 14)
        assertEquals(1, s.current)
        assertEquals(2, s.best)
    }

    @Test
    fun sevenDaysEarnOneFreeze() {
        val s = DayStreaks.compute(days(1, 2, 3, 4, 5, 6, 7), today = 7)
        assertEquals(7, s.current)
        assertEquals(1, s.freezesAvailable)
    }

    @Test
    fun aFreezeBridgesOneMissedDay() {
        // 7 days earn a freeze, day 8 is missed, day 9 is finished: the streak continues.
        val s = DayStreaks.compute(days(1, 2, 3, 4, 5, 6, 7, 9), today = 9)
        assertEquals(8, s.current)
        assertEquals(0, s.freezesAvailable)
    }

    @Test
    fun aFreezeCannotBridgeTwoMissedDays() {
        val s = DayStreaks.compute(days(1, 2, 3, 4, 5, 6, 7, 10), today = 10)
        assertEquals(1, s.current)
        assertEquals(7, s.best)
    }

    @Test
    fun aFreezeIsUsedForTheCurrentGapToo() {
        // 7 days earn a freeze; day 8 is missed, today is 9 and still open. The freeze covers day 8.
        val s = DayStreaks.compute(days(1, 2, 3, 4, 5, 6, 7), today = 9)
        assertEquals(7, s.current)
        assertEquals(0, s.freezesAvailable)
    }

    @Test
    fun theStreakBreaksIfTheCurrentGapIsLongerThanTheFreezes() {
        val s = DayStreaks.compute(days(1, 2, 3, 4, 5, 6, 7), today = 10)
        assertEquals(0, s.current)
        assertEquals(7, s.best)
    }

    @Test
    fun atMostOneFreezeIsKept() {
        // 14 days in a row would earn two freezes, but only one is kept.
        val s = DayStreaks.compute((1L..14L).toList(), today = 14)
        assertEquals(14, s.current)
        assertEquals(1, s.freezesAvailable)
    }

    @Test
    fun aUsedFreezeCanBeEarnedAgain() {
        // Days 1..7 earn a freeze, day 8 is bridged, days 9..14 continue; the streak reaches 14 and earns again.
        val finished = (1L..7L).toList() + (9L..15L).toList()
        val s = DayStreaks.compute(finished, today = 15)
        assertEquals(14, s.current)
        assertEquals(1, s.freezesAvailable)
    }

    @Test
    fun aClockThatMovedBackDoesNotBreakAnything() {
        assertEquals(2, DayStreaks.compute(days(10, 11), today = 5).current)
    }
}
