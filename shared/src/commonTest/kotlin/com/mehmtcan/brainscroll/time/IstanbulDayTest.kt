package com.mehmtcan.brainscroll.time

import kotlin.test.Test
import kotlin.test.assertEquals

class IstanbulDayTest {

    // 2026-10-05T00:00:00Z. Istanbul is UTC+3, so Istanbul midnight of the 5th is 2026-10-04T21:00:00Z.
    private val utcMidnightOct5 = 1_791_158_400_000L
    private val istanbulMidnightOct5 = utcMidnightOct5 - 3 * 3_600_000L

    @Test
    fun epochStartIsDayZeroInIstanbulAtThreeInTheMorning() {
        assertEquals(0L, IstanbulDay.dayIndex(0L))
    }

    @Test
    fun theBoundaryIsMidnightInIstanbulNotUtc() {
        val before = istanbulMidnightOct5 - 1
        val at = istanbulMidnightOct5
        assertEquals(IstanbulDay.dayIndex(at) - 1, IstanbulDay.dayIndex(before))
    }

    @Test
    fun lateEveningUtcAlreadyBelongsToTheNextIstanbulDay() {
        // 22:00 UTC on Oct 4 is 01:00 on Oct 5 in Istanbul.
        val tenPmUtcOct4 = utcMidnightOct5 - 2 * 3_600_000L
        assertEquals(IstanbulDay.dayIndex(utcMidnightOct5), IstanbulDay.dayIndex(tenPmUtcOct4))
    }

    @Test
    fun fullDayHasTheSameIndexUntilTheLastMillisecond() {
        val day = IstanbulDay.dayIndex(istanbulMidnightOct5)
        assertEquals(day, IstanbulDay.dayIndex(istanbulMidnightOct5 + IstanbulDay.DAY_MILLIS - 1))
        assertEquals(day + 1, IstanbulDay.dayIndex(istanbulMidnightOct5 + IstanbulDay.DAY_MILLIS))
    }

    @Test
    fun startOfDayIsTheInverseOfDayIndex() {
        val day = IstanbulDay.dayIndex(istanbulMidnightOct5)
        assertEquals(istanbulMidnightOct5, IstanbulDay.startOfDay(day))
        assertEquals(day, IstanbulDay.dayIndex(IstanbulDay.startOfDay(day)))
        assertEquals(day - 1, IstanbulDay.dayIndex(IstanbulDay.startOfDay(day) - 1))
    }

    @Test
    fun timesBeforeTheEpochRoundDownNotTowardZero() {
        val threeHours = 3 * 3_600_000L
        // Istanbul midnight of 1970-01-01 is 1969-12-31T21:00Z, which is -3 hours.
        assertEquals(0L, IstanbulDay.dayIndex(-threeHours))
        assertEquals(-1L, IstanbulDay.dayIndex(-threeHours - 1)) // a truncating division would give 0 here
        assertEquals(-2L, IstanbulDay.dayIndex(-threeHours - IstanbulDay.DAY_MILLIS - 1))
    }

    @Test
    fun datesAreCalendarCorrect() {
        assertEquals(Triple(1970, 1, 1), IstanbulDay.date(0))
        assertEquals(Triple(1969, 12, 31), IstanbulDay.date(-1))
        assertEquals(Triple(2000, 2, 29), IstanbulDay.date(11_016)) // a leap day
        assertEquals(Triple(2000, 3, 1), IstanbulDay.date(11_017))
        assertEquals(Triple(2026, 10, 5), IstanbulDay.date(IstanbulDay.dayIndex(istanbulMidnightOct5)))
        assertEquals(Triple(2100, 3, 1), IstanbulDay.date(47_541)) // 2100 is not a leap year
    }

    @Test
    fun formatPadsMonthAndDay() {
        assertEquals("1970-01-01", IstanbulDay.format(0))
        assertEquals("2026-10-05", IstanbulDay.format(IstanbulDay.dayIndex(istanbulMidnightOct5)))
    }

    @Test
    fun millisUntilNextDayCountsDownToMidnight() {
        assertEquals(IstanbulDay.DAY_MILLIS, IstanbulDay.millisUntilNextDay(istanbulMidnightOct5))
        assertEquals(1L, IstanbulDay.millisUntilNextDay(istanbulMidnightOct5 + IstanbulDay.DAY_MILLIS - 1))
        assertEquals(
            12 * 3_600_000L,
            IstanbulDay.millisUntilNextDay(istanbulMidnightOct5 + 12 * 3_600_000L),
        )
    }
}
