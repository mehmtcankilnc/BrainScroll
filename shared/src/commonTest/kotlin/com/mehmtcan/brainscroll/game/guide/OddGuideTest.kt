package com.mehmtcan.brainscroll.game.guide

import com.mehmtcan.brainscroll.game.oddone.OddGame
import com.mehmtcan.brainscroll.game.oddone.OddPuzzle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OddGuideTest {

    @Test
    fun theStepNamesTheRuleAndRingsTwoNumbersThatFollowIt() {
        for (seed in 0 until 40) {
            val game = OddGame(OddPuzzle(3, 12, seed))
            val state = game.state
            val q = state.question
            val step = OddGuide.step(state)!!
            assertEquals(GuideReason.OddRule, step.reason)
            assertEquals(q.rule.ordinal, step.rule)
            val ringed = step.cells + step.evidence
            assertEquals(2, ringed.size)
            assertTrue(ringed.all { q.rule.holds(q.numbers[it]) }, "the numbers the guide rings follow the rule")
            assertTrue(q.oddIndex !in ringed, "the guide does not point at the odd one")
            assertNotEquals(step.cells[0], step.evidence[0])
        }
    }

    @Test
    fun thereIsNoStepOnARunThatIsOver() {
        val game = OddGame(OddPuzzle(1, 2, 3))
        game.pick((0 until 5).first { it != game.state.question.oddIndex })
        assertNull(OddGuide.step(game.state))
    }
}
