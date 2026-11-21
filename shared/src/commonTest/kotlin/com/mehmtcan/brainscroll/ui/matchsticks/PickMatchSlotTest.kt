package com.mehmtcan.brainscroll.ui.matchsticks

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.matchsticks.MatchGenerator
import com.mehmtcan.brainscroll.game.matchsticks.MatchSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Which stick a tap means, over the boards the generator makes. */
class PickMatchSlotTest {

    /** A point a quarter of the way along the stick: the middle of a plus sign is where its two sticks cross. */
    private fun middle(slot: MatchSlot): Pair<Float, Float> {
        val l = matchSlotLine(slot)
        return (l[0] + (l[2] - l[0]) * 0.25f) to (l[1] + (l[3] - l[1]) * 0.25f)
    }

    @Test
    fun aTapOnTheStickToTakeAndOnThePlaceToLayItOnPicksThem() {
        for (level in Difficulty.entries) for (seed in 0 until 400) {
            val board = MatchGenerator.generate(seed, level)
            val cells = board.puzzle.cells
            val from = middle(board.solution.from)
            val to = middle(board.solution.to)
            assertEquals(board.solution.from, pickMatchSlot(cells, from.first, from.second, holding = false), "$level seed $seed: the stick to take")
            assertEquals(board.solution.to, pickMatchSlot(cells, to.first, to.second, holding = true), "$level seed $seed: the place to lay it on")
        }
    }

    @Test
    fun aTapFarFromEveryStickPicksNothing() {
        val cells = MatchGenerator.generate(1, Difficulty.Easy).puzzle.cells
        assertNull(pickMatchSlot(cells, 500f, 500f, holding = false))
    }
}
