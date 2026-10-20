package com.mehmtcan.brainscroll.game.topicgroups

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TopicGroupsGeneratorTest {

    private fun tiers(p: TopicGroupsPuzzle) = p.groups.map { it.tier }
    private fun families(p: TopicGroupsPuzzle) = p.groups.map { it.family }

    @Test
    fun aBoardOnlyDependsOnTheSeedTheLanguageAndTheLevel() {
        for (level in Difficulty.entries) {
            val a = TopicGroupsGenerator.generate(11, Language.EN, level)
            val b = TopicGroupsGenerator.generate(11, Language.EN, level)
            assertEquals(a.encode(), b.encode())
            assertNotEquals(a.encode(), TopicGroupsGenerator.generate(12, Language.EN, level).encode())
        }
    }

    @Test
    fun easyBoardsHavePlainTopicsOfDifferentFamilies() {
        for (language in Language.entries) for (seed in 0 until 200) {
            val p = TopicGroupsGenerator.generate(seed, language, Difficulty.Easy)
            assertTrue(tiers(p).all { it == 0 }, "${p.encode()} ${tiers(p)}")
            assertEquals(4, families(p).distinct().size, "${p.encode()} ${families(p)}")
        }
    }

    @Test
    fun mediumBoardsHaveDifferentFamiliesAndAtLeastTwoTopicsOfTierOne() {
        for (language in Language.entries) for (seed in 0 until 200) {
            val p = TopicGroupsGenerator.generate(seed, language, Difficulty.Medium)
            assertTrue(tiers(p).all { it <= 1 })
            assertTrue(tiers(p).count { it == 1 } >= 2, "${p.encode()} ${tiers(p)}")
            assertEquals(4, families(p).distinct().size, "${p.encode()} ${families(p)}")
        }
    }

    @Test
    fun hardBoardsHaveTwoTopicsOfOneFamilyAndOneOfTierTwo() {
        for (language in Language.entries) for (seed in 0 until 200) {
            val p = TopicGroupsGenerator.generate(seed, language, Difficulty.Hard)
            assertTrue(tiers(p).any { it == 2 }, "${p.encode()} ${tiers(p)}")
            assertTrue(families(p).groupBy { it }.values.any { it.size >= 2 }, "${p.encode()} ${families(p)}")
            assertEquals(4, p.topics.distinct().size)
        }
    }

    @Test
    fun theTopicsAreInADifferentOrderFromBoardToBoard() {
        val firsts = (0 until 40).map { TopicGroupsGenerator.generate(it, Language.EN, Difficulty.Easy).topics.first() }.distinct()
        assertTrue(firsts.size > 3, "$firsts")
    }
}
