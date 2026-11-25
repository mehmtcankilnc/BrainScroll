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
    fun aFreshDayHasThreeFreeSteps() {
        val state = GuideQuota.state(today, GuideUse.None, pro = false)
        assertEquals(3, state.freeLeft)
        assertTrue(state.canGuide)
        assertFalse(state.unlimited)
    }

    @Test
    fun eachStepTakesOne() {
        assertEquals(2, GuideQuota.state(today, spendTimes(1), pro = false).freeLeft)
        assertEquals(1, GuideQuota.state(today, spendTimes(2), pro = false).freeLeft)
    }

    @Test
    fun theFourthStepIsNotAllowed() {
        val state = GuideQuota.state(today, spendTimes(3), pro = false)
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
        val yesterday = GuideUse(day = today - 1, used = 3)
        assertEquals(3, GuideQuota.state(today, yesterday, pro = false).freeLeft)
        // The first step of the new day starts the count from one, not from yesterday's three.
        assertEquals(GuideUse(today, 1), GuideQuota.spend(today, yesterday, pro = false))
    }

    @Test
    fun proIsUnlimitedAndSpendsNothing() {
        val used = spendTimes(3)
        val state = GuideQuota.state(today, used, pro = true)
        assertTrue(state.unlimited)
        assertTrue(state.canGuide)
        assertEquals(used, GuideQuota.spend(today, used, pro = true))
    }

    @Test
    fun aBonusBalanceLetsAPlayerGuideAfterTheFreeStepsAreGone() {
        val state = GuideQuota.state(today, spendTimes(3), pro = false, bonus = 3)
        assertEquals(0, state.freeLeft)
        assertEquals(3, state.bonusLeft)
        assertTrue(state.canGuide)
    }
}
