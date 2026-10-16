package com.mehmtcan.brainscroll.game.tents

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

/** The tents puzzle as one of the games of the endless feed. */
class TentsFeedTest {

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
        games: List<GameKind> = listOf(GameKind.Tents),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.board(index: Int): TentsSnapshot = assertIs(snapshot().pages[index])

    /** The tents of the board on page [index], found by the solver from what the page shows. */
    private fun EndlessFeed.answer(index: Int): List<Int> {
        val state = board(index).state
        val trees = state.trees.indices.filter { state.trees[it] }.toSet()
        return TentsSolver.solutions(state.size, trees, state.rowTargets, state.columnTargets).single().sorted()
    }

    @Test
    fun solvingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        clock += 2_000
        for (cell in f.answer(0)) f.tapTent(0, cell)
        assertEquals(1, f.snapshot().solvedCount)
        val result = store.results.single()
        assertEquals(GameKind.Tents, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertFalse(store.progress.containsKey(result.id))
        f.tapTent(0, 3)
        f.restartTents(0)
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun aBoardInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val cells = f.answer(0)
        f.tapTent(0, cells[0])
        clock += 4_000
        f.tapTent(0, cells[1])
        val saved = store.progress.values.single()
        assertEquals(GameKind.Tents, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
    }

    @Test
    fun restartingAClearBoardDoesNothing() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertFalse(f.restartTents(0))
        assertTrue(store.progress.isEmpty(), "nothing was done, so nothing is saved")
        f.tapTent(0, f.answer(0).first())
        assertTrue(f.restartTents(0))
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Tents))
        f.ensureSize(2)
        assertEquals(TentsResult.Ignored, f.tapTent(0, 3), "page 0 is a word puzzle")
        assertFalse(f.restartTents(0))
    }

    @Test
    fun tentsComesAfterQueensInTheOrderOfTheGames() {
        val f = feed(games = listOf(GameKind.Queens, GameKind.Tents))
        f.ensureSize(3)
        assertEquals(listOf(GameKind.Queens, GameKind.Tents, GameKind.Queens), f.snapshot().pages.map { it.kind })
    }
}
