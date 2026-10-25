package com.mehmtcan.brainscroll.stats

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** The Pro freeze rule (every 4 days, up to 3) and what Pro would bring back of a broken streak. */
class DayStreaksProTest {

    @Test
    fun theDefaultIsTheFreeRule() {
        val days = (1L..10L).toList() + (12L..13L).toList()
        assertEquals(DayStreaks.compute(days, 13, FreezeRule.FREE), DayStreaks.compute(days, 13))
        assertEquals(DayStreaks.compute(days, 13, FreezeRule.FREE), DayStreaks.compute(days, 13, pro = false))
    }

    @Test
    fun proEarnsAFreezeAfterFourDays() {
        // 1..4 earn a freeze under Pro, so the missed day 5 is bridged and day 6 goes on: five days.
        val days = listOf(1L, 2L, 3L, 4L, 6L)
        assertEquals(5, DayStreaks.compute(days, today = 6, pro = true).current)
        // Without Pro, 4 days earn nothing: the streak starts over at day 6.
        assertEquals(1, DayStreaks.compute(days, today = 6, pro = false).current)
    }

    @Test
    fun proKeepsUpToThreeFreezes() {
        val s = DayStreaks.compute((1L..12L).toList(), today = 12, pro = true)
        assertEquals(12, s.current)
        assertEquals(3, s.freezesAvailable)
        // A thirteenth to sixteenth day would earn a fourth: still three.
        assertEquals(3, DayStreaks.compute((1L..16L).toList(), today = 16, pro = true).freezesAvailable)
    }

    @Test
    fun proStillBreaksWhenTooManyDaysAreMissed() {
        // One freeze after four days cannot bridge three missed days.
        val s = DayStreaks.compute(listOf(1L, 2L, 3L, 4L), today = 8, pro = true)
        assertEquals(0, s.current)
        assertEquals(4, s.best)
    }

    @Test
    fun proBringsBackABrokenStreak() {
        // Days 1..8 earn two Pro freezes; days 9 and 10 are missed. Pro bridges them, the free rule (one freeze) cannot.
        val days = (1L..8L).toList()
        val rescue = DayStreaks.rescue(days, today = 11)
        assertNotNull(rescue)
        assertEquals(Rescue(freeStreak = 0, proStreak = 8), rescue)
    }

    @Test
    fun noRescueWhenProWouldNotAddAnything() {
        // The streak is alive either way.
        assertNull(DayStreaks.rescue(listOf(1L, 2L, 3L), today = 4))
        // The streak is gone either way.
        assertNull(DayStreaks.rescue(listOf(1L, 2L, 3L), today = 20))
        // Never played.
        assertNull(DayStreaks.rescue(emptyList(), today = 20))
    }
}
