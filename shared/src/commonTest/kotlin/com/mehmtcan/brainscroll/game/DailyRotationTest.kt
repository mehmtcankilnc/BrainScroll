package com.mehmtcan.brainscroll.game

import com.mehmtcan.brainscroll.game.daily.DailyRotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DailyRotationTest {

    private val size = GameKind.entries.size

    @Test
    fun everyGameComesOnceInEveryRoundOfDays() {
        for (round in 0L..60L) {
            val games = (0 until size).map { DailyRotation.gameFor((2_000L + round) * size + it) }
            assertEquals(GameKind.entries.toSet(), games.toSet(), "round $round has every game exactly once")
            assertEquals(size, games.size)
        }
    }

    @Test
    fun theSameDayIsTheSameGameEveryTime() {
        for (day in 20_000L..20_100L) assertEquals(DailyRotation.gameFor(day), DailyRotation.gameFor(day))
    }

    @Test
    fun aGameNeverComesTwoDaysInARow() {
        for (day in 20_000L..22_000L) assertNotEquals(DailyRotation.gameFor(day), DailyRotation.gameFor(day + 1), "day $day")
    }

    @Test
    fun theOrderChangesFromOneRoundToTheNext() {
        val orders = (0L..20L).map { round -> (0 until size).map { DailyRotation.gameFor(round * size + it) } }
        assertTrue(orders.toSet().size > 10, "the rounds do not all repeat the same order")
    }

    @Test
    fun daysBeforeTheEpochWork() {
        assertEquals(GameKind.entries.toSet(), (-size.toLong()..-1L).map { DailyRotation.gameFor(it) }.toSet())
    }
}
