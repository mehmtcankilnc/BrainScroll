package com.mehmtcan.brainscroll.game.topicgroups

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.FeedStore
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Word groups as one of the games of the endless feed. */
class TopicGroupsFeedTest {

    private class FakeStore : FeedStore {
        val progress = linkedMapOf<String, StoredRound>()
        val results = mutableListOf<FinishedRound>()
        override fun saveProgress(round: StoredRound) { progress[round.id] = round }
        override fun deleteProgress(id: String) { progress.remove(id) }
        override fun finish(result: FinishedRound) {
            progress.remove(result.id)
            results += result
        }
    }

    private val words = listOf("crane", "slate", "ghost")
    private val lists = mapOf(
        Language.EN to WordList(Language.EN, 5, valid = words, answers = words),
        Language.TR to WordList(Language.TR, 5, valid = listOf("kitap"), answers = listOf("kitap")),
    )
    private var clock = 1_000L
    private var ids = 0

    private fun feed(
        store: FeedStore = FakeStore(),
        restored: List<StoredRound> = emptyList(),
        games: List<GameKind> = listOf(GameKind.TopicGroups),
        language: Language = Language.EN,
        levels: Map<GameKind, Difficulty> = emptyMap(),
    ) = EndlessFeed(lists, language, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games, difficulties = levels)

    private fun EndlessFeed.board(index: Int): TopicGroupsSnapshot = assertIs(snapshot().pages[index])

    /** The words of group [g] of the page, read from the picture (words are numbered by group). */
    private fun group(g: Int) = (g * 4 until g * 4 + 4).toList()

    private fun EndlessFeed.solve(index: Int) {
        for (g in 0 until 3) for (w in group(g)) tapTopicWord(index, w)
    }

    @Test
    fun theFeedMakesBoardsInTheLanguageOfTheFeedAtTheLevelOfTheGame() {
        for (language in Language.entries) {
            val f = feed(language = language, levels = mapOf(GameKind.TopicGroups to Difficulty.Hard))
            f.ensureSize(3)
            for (i in 0 until 3) {
                val page = f.board(i)
                assertEquals(language, page.language)
                assertEquals(16, page.state.words.size)
                assertTrue(page.state.words.all { w -> TopicGroupsData.topics(language).any { w in it.words } })
            }
        }
    }

    @Test
    fun solvingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.tapTopicWord(0, 0)
        f.tapTopicWord(0, 4)
        f.tapTopicWord(0, 1)
        f.tapTopicWord(0, 5)
        assertEquals(1, f.board(0).state.mistakes)
        f.tapTopicWord(0, 4)
        f.tapTopicWord(0, 5)
        f.tapTopicWord(0, 2)
        f.tapTopicWord(0, 3)
        assertEquals(TopicGroupsStatus.Playing, f.board(0).state.status)
        // Whatever the picks were, finish the board.
        val g = f.board(0).state
        f.clearTopicSelection(0)
        for (gi in 0 until 3) if (gi !in f.board(0).state.solved) for (w in group(gi)) f.tapTopicWord(0, w)
        assertTrue(f.board(0).isFinished)
        assertEquals(1, store.results.size)
        val result = store.results.single()
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(GameKind.TopicGroups, result.game)
        assertEquals(listOf("1"), result.guesses)
        assertEquals(1, f.solvedCount)
        assertTrue(store.progress.isEmpty())
        // Tapping on a finished board changes nothing and saves nothing new.
        f.tapTopicWord(0, 0)
        assertEquals(1, store.results.size)
        assertEquals(g.mistakes, f.board(0).state.mistakes)
    }

    @Test
    fun aStartedBoardIsSavedAndComesBackAsItWas() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.tapTopicWord(0, 0)
        f.shuffleTopicWords(0)
        f.tapTopicWord(0, 1)
        val saved = store.progress.values.single()
        assertEquals(GameKind.TopicGroups, saved.game)
        val back = feed(restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
    }

    @Test
    fun anUntouchedBoardIsNotSavedAndAnActionOnAnotherGameDoesNothing() {
        val store = FakeStore()
        val f = feed(store, games = listOf(GameKind.Word, GameKind.TopicGroups))
        f.ensureSize(2)
        assertTrue(store.progress.isEmpty())
        assertEquals(com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsResult.Ignored, f.tapTopicWord(0, 0))
        assertFalse(f.shuffleTopicWords(0))
        assertFalse(f.clearTopicSelection(0))
        assertEquals(GameKind.TopicGroups, f.snapshot().pages[1].kind)
    }

    @Test
    fun switchingTheLanguageMakesUntouchedBoardsAgainAndKeepsStartedOnes() {
        val f = feed(games = listOf(GameKind.TopicGroups), language = Language.EN)
        f.ensureSize(3)
        f.tapTopicWord(1, 0)
        val started = f.board(1).state
        f.setWordLanguage(Language.TR, fromIndex = 0)
        assertEquals(Language.TR, f.board(0).language)
        assertEquals(Language.EN, f.board(1).language)
        assertEquals(started, f.board(1).state)
        assertEquals(Language.TR, f.board(2).language)
    }

    @Test
    fun aGuidedBoardIsNeutral() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.markGuided(0)
        f.solve(0)
        f.clearTopicSelection(0)
        assertTrue(f.board(0).isFinished)
        assertTrue(store.results.single().assisted)
        assertEquals(0, f.solvedCount)
    }
}
