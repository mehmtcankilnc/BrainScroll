package com.mehmtcan.brainscroll.game.wordle

import com.mehmtcan.brainscroll.game.GameKind
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The feed settings of the home page (docs/decisions.md, Ana Sayfa): games on or off, word language, opening a game. */
class FeedPreferencesTest {

    private val words = listOf("crane", "slate", "ghost", "pound", "apple", "brick")
    private val en = WordList(Language.EN, 5, valid = words, answers = words)
    private val trWords = listOf("çiçek", "kırık", "kitap")
    private val tr = WordList(Language.TR, 5, valid = trWords, answers = trWords)
    private var ids = 0

    private fun feed() = EndlessFeed(
        wordLists = mapOf(Language.EN to en, Language.TR to tr),
        startLanguage = Language.EN,
        random = Random(1),
        newId = { "id${ids++}" },
        games = listOf(GameKind.Word, GameKind.Memory),
    )

    private fun EndlessFeed.kinds() = snapshot().pages.map { it.kind }

    @Test
    fun `switching a game off replaces untouched pages and later pages`() {
        val feed = feed().also { it.ensureSize(4) }
        assertEquals(listOf(GameKind.Word, GameKind.Memory, GameKind.Word, GameKind.Memory), feed.kinds())

        feed.setEnabledGames(setOf(GameKind.Word), fromIndex = 0)
        feed.ensureSize(6)

        assertTrue(feed.kinds().all { it == GameKind.Word })
    }

    @Test
    fun `a started page keeps its game when that game is switched off`() {
        val feed = feed().also { it.ensureSize(3) }
        feed.type(0, 'c') // page 0 (word) is started

        feed.setEnabledGames(setOf(GameKind.Memory), fromIndex = 0)

        assertEquals(GameKind.Word, feed.kinds()[0])
        assertEquals(GameKind.Memory, feed.kinds()[1])
        assertEquals(GameKind.Memory, feed.kinds()[2])
    }

    @Test
    fun `the last game cannot be switched off`() {
        val feed = feed().also { it.ensureSize(2) }
        feed.setEnabledGames(setOf(GameKind.Memory), fromIndex = 0)

        feed.setEnabledGames(emptySet(), fromIndex = 0)

        assertEquals(listOf(GameKind.Memory), feed.enabledGames)
    }

    @Test
    fun `word language changes for new and untouched word pages only`() {
        val feed = feed().also { it.ensureSize(3) }
        feed.type(0, 'c') // started in EN

        feed.setWordLanguage(Language.TR, fromIndex = 0)
        feed.ensureSize(5)

        val word = feed.snapshot().rounds
        assertEquals(Language.EN, word[0].language)
        assertEquals(Language.TR, feed.language)
        assertTrue(word.drop(1).all { it.language == Language.TR })
    }
}
