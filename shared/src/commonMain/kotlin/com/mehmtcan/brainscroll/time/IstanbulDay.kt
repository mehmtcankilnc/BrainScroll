package com.mehmtcan.brainscroll.time

/**
 * The app's definition of "a day" (docs/decisions.md): every day boundary is midnight in Europe/Istanbul.
 * Turkey is on permanent UTC+3 with no daylight saving, so a day is always exactly 24 hours and plain
 * arithmetic on epoch milliseconds is enough (no time zone database, same result on every platform).
 *
 * A day is identified by its [dayIndex]: the number of days since 1970-01-01 in Istanbul time.
 * Streaks and the daily puzzle compare these indexes, never raw timestamps.
 */
object IstanbulDay {
    private const val HOUR_MILLIS = 3_600_000L
    const val DAY_MILLIS = 24 * HOUR_MILLIS
    private const val OFFSET_MILLIS = 3 * HOUR_MILLIS

    /** Day index of the moment [epochMillis] (milliseconds since 1970-01-01T00:00:00Z). */
    fun dayIndex(epochMillis: Long): Long = (epochMillis + OFFSET_MILLIS).floorDiv(DAY_MILLIS)

    /** The first instant (epoch millis) of day [dayIndex], i.e. midnight in Istanbul. */
    fun startOfDay(dayIndex: Long): Long = dayIndex * DAY_MILLIS - OFFSET_MILLIS

    /**
     * The calendar date (year, month 1-12, day 1-31) of day [dayIndex]. Uses the standard "civil from days"
     * arithmetic for the proleptic Gregorian calendar, so no date library is needed.
     */
    fun date(dayIndex: Long): Triple<Int, Int, Int> {
        val z = dayIndex + 719_468
        val era = z.floorDiv(146_097L)
        val dayOfEra = z - era * 146_097
        val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
        val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        val monthIndex = (5 * dayOfYear + 2) / 153 // 0 = March ... 11 = February
        val day = (dayOfYear - (153 * monthIndex + 2) / 5 + 1).toInt()
        val month = (if (monthIndex < 10) monthIndex + 3 else monthIndex - 9).toInt()
        val year = (yearOfEra + era * 400).toInt() + if (month <= 2) 1 else 0
        return Triple(year, month, day)
    }

    /** The date as `2026-10-05`. */
    fun format(dayIndex: Long): String {
        val (year, month, day) = date(dayIndex)
        return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }

    /** Milliseconds from [epochMillis] until the next midnight in Istanbul (always 1..[DAY_MILLIS]). */
    fun millisUntilNextDay(epochMillis: Long): Long = startOfDay(dayIndex(epochMillis) + 1) - epochMillis
}
