package com.mehmtcan.brainscroll.stats

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** When the "bring your streak back" paywall may come up by itself (docs/pro-plan.md section 7.3). */
class RescuePolicyTest {
    /** Days 1..8 finished, then two missed: Pro would bring back 8 days, the free rule leaves 0. */
    private val days = (1L..8L).toList()
    private val today = 11L
    private val localToday = 500L

    private fun offer(
        finished: List<Long> = days,
        pro: Boolean = false,
        shownBreak: Long? = null,
        autoPaywallDay: Long? = null,
    ) = RescuePolicy.offer(finished, today, pro, shownBreak, autoPaywallDay, localToday)

    @Test
    fun aBrokenStreakThatProWouldBringBackIsOffered() {
        assertEquals(Rescue(0, 8), offer())
    }

    @Test
    fun neverToAProPlayer() {
        assertNull(offer(pro = true))
    }

    @Test
    fun neverWhenProWouldNotBringAStreakBack() {
        assertNull(RescuePolicy.offer(listOf(1L, 2L, 3L), today = 20, pro = false, shownBreak = null, autoPaywallDay = null, localToday = localToday))
        assertNull(RescuePolicy.offer(emptyList(), today = 20, pro = false, shownBreak = null, autoPaywallDay = null, localToday = localToday))
    }

    @Test
    fun notWhileTheFreeStreakIsAlive() {
        // Days 1..8 and today finished again at 9: the streak is alive, nothing broke.
        assertNull(RescuePolicy.offer((1L..9L).toList(), today = 9, pro = false, shownBreak = null, autoPaywallDay = null, localToday = localToday))
    }

    @Test
    fun onlyOnceForTheSameBreak() {
        assertNull(offer(shownBreak = 8L))
    }

    @Test
    fun aNewBreakOffersAgain() {
        // The offer was shown for an older break (last finished day 3); this break is named by day 8.
        assertNotNull(offer(shownBreak = 3L))
    }

    @Test
    fun atMostOneAutomaticPaywallADay() {
        assertNull(offer(autoPaywallDay = localToday))
        assertNotNull(offer(autoPaywallDay = localToday - 1))
    }

    @Test
    fun aBreakIsNamedByTheLastFinishedDay() {
        assertEquals(8L, RescuePolicy.breakOf(days))
        assertNull(RescuePolicy.breakOf(emptyList()))
    }
}
