package com.mehmtcan.brainscroll.game.topicgroups

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TopicGroupsDataTest {

    @Test
    fun everyTopicHasAtLeastFourDifferentWordsAndAName() {
        for (language in Language.entries) {
            for (topic in TopicGroupsData.topics(language)) {
                assertTrue(topic.title.isNotBlank(), "$language: a topic has no name")
                assertTrue(topic.words.size >= TOPIC_WORDS_PER_GROUP, "$language ${topic.title}: ${topic.words.size} words")
                assertEquals(topic.words.size, topic.words.distinct().size, "$language ${topic.title}: a word twice")
                assertTrue(topic.tier in 0..2)
            }
        }
    }

    @Test
    fun aWordIsInOneTopicOfALanguageOnly() {
        for (language in Language.entries) {
            val all = TopicGroupsData.topics(language).flatMap { it.words }
            val twice = all.groupBy { it }.filterValues { it.size > 1 }.keys
            assertTrue(twice.isEmpty(), "$language: in more than one topic: $twice")
        }
    }

    @Test
    fun wordsAreCapitalsAndShortEnoughForATile() {
        for (language in Language.entries) {
            for (topic in TopicGroupsData.topics(language)) for (word in topic.words) {
                assertTrue(word.all { it.isLetter() && it.isUpperCase() }, "$language $word is not made of capitals")
                assertTrue(word.length <= 10, "$language $word is too long for a tile")
            }
        }
    }

    @Test
    fun everyLevelHasEnoughTopicsToMakeManyDifferentBoards() {
        for (language in Language.entries) {
            val topics = TopicGroupsData.topics(language)
            assertTrue(topics.count { it.tier == 0 } >= 10, "$language tier 0")
            assertTrue(topics.count { it.tier == 1 } >= 10, "$language tier 1")
            assertTrue(topics.count { it.tier == 2 } >= 6, "$language tier 2")
        }
    }

    /** The saved boards name topics by their place in the lists: if this fails, a topic was moved or changed and old games would change. */
    @Test
    fun theListsKeepTheirOrder() {
        val en = TopicGroupsData.topics(Language.EN)
        assertEquals("Fruits", en[0].title)
        assertEquals("Chess pieces", en[13].title)
        assertEquals("Car parts", en[29].title)
        val tr = TopicGroupsData.topics(Language.TR)
        assertEquals("Meyveler", tr[0].title)
        assertEquals("Satranç taşları", tr[13].title)
        assertEquals("Araba parçaları", tr[31].title)
        val puzzle = TopicGroupsPuzzle(Language.EN, listOf(0, 1, 2, 3), 1)
        assertEquals(16, puzzle.words.size)
        assertEquals(listOf("Fruits", "Farm animals", "Colors", "Body parts"), puzzle.groups.map { it.title })
        assertTrue(puzzle.words.take(4).all { it in en[0].words })
    }

    @Test
    fun aBoardHasFourGroupsOfFourAndTheStartOrderHasEveryWordOnce() {
        val puzzle = TopicGroupsGenerator.generate(5, Language.TR, Difficulty.Medium)
        assertEquals(16, puzzle.words.size)
        assertEquals(16, puzzle.words.distinct().size)
        assertEquals((0 until 16).toList(), puzzle.initialOrder.sorted())
        for (g in 0 until 4) assertTrue(puzzle.words.subList(g * 4, g * 4 + 4).all { it in puzzle.groups[g].words })
    }

    @Test
    fun aBoardIsSavedAndReadBackExactly() {
        for (seed in 0 until 60) {
            for (language in Language.entries) {
                val puzzle = TopicGroupsGenerator.generate(seed, language, Difficulty.entries[seed % 3])
                val text = puzzle.encode()
                assertTrue(text.length <= 32, "$text is ${text.length} long")
                val back = assertNotNull(TopicGroupsPuzzle.decode(text))
                assertEquals(puzzle.words, back.words)
                assertEquals(puzzle.initialOrder, back.initialOrder)
                assertEquals(puzzle.language, back.language)
            }
        }
        val longest = TopicGroupsPuzzle(Language.EN, listOf(38, 37, 36, 35), Int.MIN_VALUE).encode()
        assertTrue(longest.length <= 32, longest)
    }

    @Test
    fun textThatIsNotABoardIsRefused() {
        for (bad in listOf("", "x", "en:1.2.3:5", "en:1.1.2.3:5", "en:1.2.3.99:5", "fr:1.2.3.4:5", "en:1.2.3.4:x", "en:a.b.c.d:1", "en:1.2.3.4")) {
            assertNull(TopicGroupsPuzzle.decode(bad), bad)
        }
    }
}
