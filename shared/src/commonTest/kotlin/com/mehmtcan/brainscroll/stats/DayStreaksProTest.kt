package com.mehmtcan.brainscroll.stats

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** The Pro freeze rule (every 7 days, up to 3) and what Pro would bring back of a broken streak. */
class DayStreaksProTest {

    @Test
    fun theDefaultIsTheFreeRule() {
        val days = (1L..10L).toList() + (12L..13L).toList()
        assertEquals(DayStreaks.compute(days, 13, FreezeRule.FREE), DayStreaks.compute(days, 13))
        assertEquals(DayStreaks.compute(days, 13, FreezeRule.FREE), DayStreaks.compute(days, 13, pro = false))
    }

    @Test
    fun theProRuleIsOneFreezeAWeekUpToThree() {
        assertEquals(FreezeRule(every = 7, max = 3), FreezeRule.PRO)
    }

    @Test
    fun proEarnsAFreezeAfterSevenDays() {
        // 1..7 earn a freeze under Pro, so the missed day 8 is bridged and day 9 goes on: eight days.
        val days = (1L..7L).toList() + 9L
        assertEquals(8, DayStreaks.compute(days, today = 9, pro = true).current)
        // Without Pro, 7 days earn nothing: the streak starts over at day 9.
        assertEquals(1, DayStreaks.compute(days, today = 9, pro = false).current)
    }

    @Test
    fun proKeepsUpToThreeFreezes() {
        val s = DayStreaks.compute((1L..21L).toList(), today = 21, pro = true)
        assertEquals(21, s.current)
        assertEquals(3, s.freezesAvailable)
        // A fourth week would earn a fourth: still three.
        assertEquals(3, DayStreaks.compute((1L..28L).toList(), today = 28, pro = true).freezesAvailable)
    }

    @Test
    fun proStillBreaksWhenTooManyDaysAreMissed() {
        // One freeze after seven days cannot bridge three missed days.
        val s = DayStreaks.compute((1L..7L).toList(), today = 11, pro = true)
        assertEquals(0, s.current)
        assertEquals(7, s.best)
    }

    @Test
    fun proBringsBackABrokenStreak() {
        // Days 1..14 earn two Pro freezes; days 15 and 16 are missed. Pro bridges them, the free rule (none yet) cannot.
        val days = (1L..14L).toList()
        val rescue = DayStreaks.rescue(days, today = 17)
        assertNotNull(rescue)
        assertEquals(Rescue(freeStreak = 0, proStreak = 14), rescue)
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
