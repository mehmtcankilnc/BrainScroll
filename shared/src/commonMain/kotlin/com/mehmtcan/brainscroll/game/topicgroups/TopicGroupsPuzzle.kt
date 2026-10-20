package com.mehmtcan.brainscroll.game.topicgroups

import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.random.Random

/** The groups of a board and the words of each: four of each. */
const val TOPIC_GROUP_COUNT = 4
const val TOPIC_WORDS_PER_GROUP = 4

/**
 * One board of the word groups game: sixteen words that fall into four groups of four, each group a topic.
 *
 * It is made of the [language], the positions of its four [topics] in [TopicGroupsData], and a [seed] that picks which four words
 * of each topic are used and how the sixteen start out. Words are numbered by group: word `g * 4 + k` is the k-th word of
 * group `g`, so the group of a word is `word / 4`. [initialOrder] is how the words lie on the board at the start.
 */
class TopicGroupsPuzzle(val language: Language, val topics: List<Int>, val seed: Int) {
    val groups: List<TopicGroup>
    val words: List<String>
    val initialOrder: List<Int>

    init {
        val catalog = TopicGroupsData.topics(language)
        require(topics.size == TOPIC_GROUP_COUNT && topics.distinct().size == TOPIC_GROUP_COUNT && topics.all { it in catalog.indices })
        val random = Random(seed)
        groups = topics.map { catalog[it] }
        words = groups.flatMap { group -> group.words.shuffled(random).take(TOPIC_WORDS_PER_GROUP) }
        initialOrder = words.indices.shuffled(random)
    }

    /** The saved form, at most 32 characters: `en:3.14.22.30:123456`. */
    fun encode(): String = "${language.name.lowercase()}:${topics.joinToString(".")}:$seed"

    companion object {
        /** The board of [encode], or null if the text is not one (or names a topic this version does not have). */
        fun decode(text: String): TopicGroupsPuzzle? {
            val parts = text.split(':')
            if (parts.size != 3) return null
            val language = Language.entries.firstOrNull { it.name.equals(parts[0], ignoreCase = true) } ?: return null
            val topics = parts[1].split('.').map { it.toIntOrNull() ?: return null }
            val seed = parts[2].toIntOrNull() ?: return null
            val catalog = TopicGroupsData.topics(language)
            if (topics.size != TOPIC_GROUP_COUNT || topics.distinct().size != TOPIC_GROUP_COUNT || topics.any { it !in catalog.indices }) return null
            return TopicGroupsPuzzle(language, topics, seed)
        }
    }
}
