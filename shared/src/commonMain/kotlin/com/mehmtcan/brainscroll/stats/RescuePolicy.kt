package com.mehmtcan.brainscroll.stats

/**
 * When the "bring your streak back with Pro" paywall may come up by itself (docs/pro-plan.md section 7.3). Pure: the caller
 * keeps what was shown. Every condition is a way of not bothering the player; the offer is also never made when Pro would
 * not really bring a streak back.
 */
object RescuePolicy {
    /** A streak shorter than this is not worth interrupting anyone for. */
    const val MIN_STREAK = 3

    /**
     * The [Rescue] to offer, or null.
     *
     * @param shownBreak the break the offer was already shown for (the last day finished before it), or null
     * @param autoPaywallDay the local day an automatic paywall was last shown on, or null
     */
    fun offer(
        finishedDays: Collection<Long>,
        today: Long,
        pro: Boolean,
        shownBreak: Long?,
        autoPaywallDay: Long?,
        localToday: Long,
    ): Rescue? {
        if (pro) return null
        val rescue = DayStreaks.rescue(finishedDays, today) ?: return null
        // "Your streak broke" must be true: the free streak is gone, and Pro brings back a real one.
        if (rescue.freeStreak != 0 || rescue.proStreak < MIN_STREAK) return null
        if (shownBreak == breakOf(finishedDays)) return null
        if (autoPaywallDay == localToday) return null
        return rescue
    }

    /** Names a break by the last day finished before it. A new break has a new name. */
    fun breakOf(finishedDays: Collection<Long>): Long? = finishedDays.maxOrNull()
}
