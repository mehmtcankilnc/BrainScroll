package com.mehmtcan.brainscroll.game.guide

/** What was spent on one local day: the day (see [com.mehmtcan.brainscroll.time.localDayIndex]) and how many guide steps were shown. */
data class GuideUse(val day: Long, val used: Int) {
    companion object {
        val None = GuideUse(day = Long.MIN_VALUE, used = 0)
    }
}

/** What is left today. [bonusLeft] is for the hint packs of a later phase and is 0 until then. */
data class QuotaState(val freeLeft: Int, val bonusLeft: Int, val unlimited: Boolean) {
    val canGuide: Boolean get() = unlimited || freeLeft > 0 || bonusLeft > 0
}

/**
 * The free guide steps of a day (docs/pro-plan.md section 5). Pure: the caller keeps the [GuideUse]. A stored day that is
 * not today counts as nothing spent, so the new day needs no write to start.
 */
object GuideQuota {
    const val FREE_PER_DAY = 3

    fun state(today: Long, stored: GuideUse, pro: Boolean, bonus: Int = 0): QuotaState {
        val used = if (stored.day == today) stored.used else 0
        return QuotaState(freeLeft = (FREE_PER_DAY - used).coerceAtLeast(0), bonusLeft = bonus.coerceAtLeast(0), unlimited = pro)
    }

    /** The use after one step was shown. Pro spends nothing, and nothing is spent below zero. */
    fun spend(today: Long, stored: GuideUse, pro: Boolean): GuideUse {
        if (pro) return stored
        val used = if (stored.day == today) stored.used else 0
        return GuideUse(today, minOf(used + 1, FREE_PER_DAY))
    }
}
