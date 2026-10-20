package com.mehmtcan.brainscroll.game.topicgroups

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.random.Random

/**
 * Makes the boards of the word groups game by choosing four topics:
 * - easy: four plain topics (tier 0), each of a different family;
 * - medium: four topics of the first two tiers, each of a different family, at least two of them of tier 1;
 * - hard: two topics of one family (they are easy to mix up) and two others, at least one of them from tier 2.
 * The board only depends on the seed and the language.
 */
object TopicGroupsGenerator {
    fun generate(seed: Int, language: Language, difficulty: Difficulty): TopicGroupsPuzzle {
        val random = Random(seed)
        val catalog = TopicGroupsData.topics(language)
        val topics = when (difficulty) {
            Difficulty.Easy -> distinctFamilies(catalog, random, allowed = { it.tier == 0 })
            Difficulty.Medium -> distinctFamilies(catalog, random, allowed = { it.tier <= 1 }, enough = { picked -> picked.count { catalog[it].tier == 1 } >= 2 })
            Difficulty.Hard -> hard(catalog, random)
        }
        return TopicGroupsPuzzle(language, topics.shuffled(random), random.nextInt())
    }

    private fun distinctFamilies(
        catalog: List<TopicGroup>,
        random: Random,
        allowed: (TopicGroup) -> Boolean,
        enough: (List<Int>) -> Boolean = { true },
    ): List<Int> {
        val pool = catalog.indices.filter { allowed(catalog[it]) }
        repeat(200) {
            val picked = mutableListOf<Int>()
            for (index in pool.shuffled(random)) {
                if (picked.none { catalog[it].family == catalog[index].family }) picked += index
                if (picked.size == TOPIC_GROUP_COUNT) break
            }
            if (picked.size == TOPIC_GROUP_COUNT && enough(picked)) return picked
        }
        error("no topics found")
    }

    private fun hard(catalog: List<TopicGroup>, random: Random): List<Int> {
        val siblings = catalog.indices.groupBy { catalog[it].family }.values.filter { it.size >= 2 }
        repeat(200) {
            val pair = siblings.random(random).shuffled(random).take(2)
            val others = mutableListOf<Int>()
            for (index in catalog.indices.filter { catalog[it].tier >= 1 && it !in pair }.shuffled(random)) {
                val family = catalog[index].family
                if (family != catalog[pair[0]].family && others.none { catalog[it].family == family }) others += index
                if (others.size == 2) break
            }
            val picked = pair + others
            if (picked.size == TOPIC_GROUP_COUNT && picked.any { catalog[it].tier == 2 }) return picked
        }
        error("no topics found")
    }
}
