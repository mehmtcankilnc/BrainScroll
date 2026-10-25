package com.mehmtcan.brainscroll.game.guide

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GuideQuotaTest {
    private val today = 100L

    private fun spendTimes(times: Int, start: GuideUse = GuideUse.None): GuideUse =
        (1..times).fold(start) { use, _ -> GuideQuota.spend(today, use, pro = false) }

    @Test
    fun aFreshDayHasFiveFreeSteps() {
        val state = GuideQuota.state(today, GuideUse.None, pro = false)
        assertEquals(5, state.freeLeft)
        assertTrue(state.canGuide)
        assertFalse(state.unlimited)
    }

    @Test
    fun eachStepTakesOne() {
        assertEquals(4, GuideQuota.state(today, spendTimes(1), pro = false).freeLeft)
        assertEquals(1, GuideQuota.state(today, spendTimes(4), pro = false).freeLeft)
    }

    @Test
    fun theSixthStepIsNotAllowed() {
        val state = GuideQuota.state(today, spendTimes(5), pro = false)
        assertEquals(0, state.freeLeft)
        assertFalse(state.canGuide)
    }

    @Test
    fun spendingPastTheLimitNeverGoesBelowZero() {
        val use = spendTimes(8)
        assertEquals(GuideQuota.FREE_PER_DAY, use.used)
        assertEquals(0, GuideQuota.state(today, use, pro = false).freeLeft)
    }

    @Test
    fun aNewDayGivesTheStepsBack() {
        val yesterday = GuideUse(day = today - 1, used = 5)
        assertEquals(5, GuideQuota.state(today, yesterday, pro = false).freeLeft)
        // The first step of the new day starts the count from one, not from yesterday's five.
        assertEquals(GuideUse(today, 1), GuideQuota.spend(today, yesterday, pro = false))
    }

    @Test
    fun proIsUnlimitedAndSpendsNothing() {
        val used = spendTimes(5)
        val state = GuideQuota.state(today, used, pro = true)
        assertTrue(state.unlimited)
        assertTrue(state.canGuide)
        assertEquals(used, GuideQuota.spend(today, used, pro = true))
    }

    @Test
    fun aBonusBalanceLetsAPlayerGuideAfterTheFreeStepsAreGone() {
        val state = GuideQuota.state(today, spendTimes(5), pro = false, bonus = 3)
        assertEquals(0, state.freeLeft)
        assertEquals(3, state.bonusLeft)
        assertTrue(state.canGuide)
    }
}
