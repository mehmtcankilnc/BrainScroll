package com.mehmtcan.brainscroll.stats

/** The day streak as of a given day. */
data class DayStreak(
    /** Days in a row with a finished daily puzzle, counting freezes as bridges. 0 once the streak is broken. */
    val current: Int,
    val best: Int,
    /** Freezes the player could still use. */
    val freezesAvailable: Int,
)

/** How freezes are earned: one for every [every] days of streak, at most [max] kept. */
data class FreezeRule(val every: Int, val max: Int) {
    companion object {
        val FREE = FreezeRule(every = 15, max = 1)

        /** BrainScroll Pro (docs/pro-plan.md section 6): one a week, up to three kept. */
        val PRO = FreezeRule(every = 7, max = 3)
    }
}

/** What Pro would bring back: the streak with the free rule and with the Pro rule, when the Pro one is longer. */
data class Rescue(val freeStreak: Int, val proStreak: Int)

/**
 * Day streak with freezes. THIS IS A LOCAL DRAFT: the exact freeze rules are decided in phase 6
 * (docs/decisions.md). Every [FreezeRule.FREE] days of streak earn one freeze, at most one is kept, and a freeze
 * is used automatically to bridge a missed day. Pro earns them faster and keeps more ([FreezeRule.PRO]).
 *
 * Days are Istanbul day indexes ([com.mehmtcan.brainscroll.time.IstanbulDay.dayIndex]). Only finishing the
 * daily puzzle matters, winning or losing, so the caller passes the days on which it was finished.
 */
object DayStreaks {
    /** The rule of the player: Pro or not. The whole history is always counted with it. */
    fun ruleFor(pro: Boolean): FreezeRule = if (pro) FreezeRule.PRO else FreezeRule.FREE

    /** [Rescue] when Pro would give a longer streak than the free rule does today, else null. */
    fun rescue(finishedDays: Collection<Long>, today: Long): Rescue? {
        val free = compute(finishedDays, today, FreezeRule.FREE).current
        val pro = compute(finishedDays, today, FreezeRule.PRO).current
        return if (pro > free) Rescue(free, pro) else null
    }

    fun compute(finishedDays: Collection<Long>, today: Long, pro: Boolean): DayStreak = compute(finishedDays, today, ruleFor(pro))

    fun compute(finishedDays: Collection<Long>, today: Long, rule: FreezeRule = FreezeRule.FREE): DayStreak {
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
            if (streak % rule.every == 0) freezes = minOf(freezes + 1, rule.max)
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
