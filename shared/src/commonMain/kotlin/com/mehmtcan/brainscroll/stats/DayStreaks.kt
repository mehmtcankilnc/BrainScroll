package com.mehmtcan.brainscroll.stats

/** The day streak as of a given day. */
data class DayStreak(
    /** Days in a row with a finished daily puzzle, counting freezes as bridges. 0 once the streak is broken. */
    val current: Int,
    val best: Int,
    /** Freezes the player could still use. */
    val freezesAvailable: Int,
)

/**
 * Day streak with freezes. THIS IS A LOCAL DRAFT: the exact freeze rules are decided in phase 6
 * (docs/decisions.md). The draft follows the suggestion there: every 7 days of streak earn one freeze, at most
 * one is kept, and a freeze is used automatically to bridge a missed day.
 *
 * Days are Istanbul day indexes ([com.mehmtcan.brainscroll.time.IstanbulDay.dayIndex]). Only finishing the
 * daily puzzle matters, winning or losing, so the caller passes the days on which it was finished.
 */
object DayStreaks {
    const val FREEZE_EVERY = 7
    const val MAX_FREEZES = 1

    fun compute(finishedDays: Collection<Long>, today: Long): DayStreak {
        val days = finishedDays.distinct().sorted()
        if (days.isEmpty()) return DayStreak(current = 0, best = 0, freezesAvailable = 0)

        var streak = 0
        var best = 0
        var freezes = 0
        var previous: Long? = null

        for (day in days) {
            val last = previous
            streak = when {
                last == null -> 1
                day - last - 1 == 0L -> streak + 1
                day - last - 1 <= freezes -> {
                    freezes -= (day - last - 1).toInt()
                    streak + 1
                }
                else -> 1 // too many missed days: a new streak starts
            }
            if (streak % FREEZE_EVERY == 0) freezes = minOf(freezes + 1, MAX_FREEZES)
            if (streak > best) best = streak
            previous = day
        }

        // The days since the last finished one (today is still open and is not missed yet) need freezes too.
        val missedSince = maxOf(0L, today - days.last() - 1)
        return if (missedSince <= freezes) {
            DayStreak(current = streak, best = best, freezesAvailable = freezes - missedSince.toInt())
        } else {
            DayStreak(current = 0, best = best, freezesAvailable = freezes)
        }
    }
}
