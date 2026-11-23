package com.mehmtcan.brainscroll.stats

import kotlin.test.Test
import kotlin.test.assertEquals

/** The free rule: one freeze for every 15 days of streak, at most one kept. */
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
    fun theFreeRuleIsOneFreezeEveryFifteenDays() {
        assertEquals(FreezeRule(every = 15, max = 1), FreezeRule.FREE)
    }

    @Test
    fun fifteenDaysEarnOneFreeze() {
        val s = DayStreaks.compute((1L..15L).toList(), today = 15)
        assertEquals(15, s.current)
        assertEquals(1, s.freezesAvailable)
    }

    @Test
    fun fourteenDaysEarnNoFreeze() {
        assertEquals(0, DayStreaks.compute((1L..14L).toList(), today = 14).freezesAvailable)
    }

    @Test
    fun aFreezeBridgesOneMissedDay() {
        // 15 days earn a freeze, day 16 is missed, day 17 is finished: the streak continues.
        val s = DayStreaks.compute((1L..15L).toList() + 17L, today = 17)
        assertEquals(16, s.current)
        assertEquals(0, s.freezesAvailable)
    }

    @Test
    fun aFreezeCannotBridgeTwoMissedDays() {
        val s = DayStreaks.compute((1L..15L).toList() + 18L, today = 18)
        assertEquals(1, s.current)
        assertEquals(15, s.best)
    }

    @Test
    fun aFreezeIsUsedForTheCurrentGapToo() {
        // 15 days earn a freeze; day 16 is missed, today is 17 and still open. The freeze covers day 16.
        val s = DayStreaks.compute((1L..15L).toList(), today = 17)
        assertEquals(15, s.current)
        assertEquals(0, s.freezesAvailable)
    }

    @Test
    fun theStreakBreaksIfTheCurrentGapIsLongerThanTheFreezes() {
        val s = DayStreaks.compute((1L..15L).toList(), today = 18)
        assertEquals(0, s.current)
        assertEquals(15, s.best)
    }

    @Test
    fun atMostOneFreezeIsKept() {
        // 30 days in a row would earn two freezes, but only one is kept.
        val s = DayStreaks.compute((1L..30L).toList(), today = 30)
        assertEquals(30, s.current)
        assertEquals(1, s.freezesAvailable)
    }

    @Test
    fun aUsedFreezeCanBeEarnedAgain() {
        // Days 1..15 earn a freeze, day 16 is bridged, days 17..31 continue; the streak reaches 30 and earns again.
        val finished = (1L..15L).toList() + (17L..31L).toList()
        val s = DayStreaks.compute(finished, today = 31)
        assertEquals(30, s.current)
        assertEquals(1, s.freezesAvailable)
    }

    @Test
    fun aClockThatMovedBackDoesNotBreakAnything() {
        assertEquals(2, DayStreaks.compute(days(10, 11), today = 5).current)
    }
}
