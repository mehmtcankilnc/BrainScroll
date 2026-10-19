package com.mehmtcan.brainscroll.game.nonogram

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

/** The nonogram puzzle as one of the games of the endless feed. */
class NonogramFeedTest {

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
        games: List<GameKind> = listOf(GameKind.Nonogram),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.board(index: Int): NonogramSnapshot = assertIs(snapshot().pages[index])

    /** The cells of the picture on page [index], worked out from the numbers the page shows. */
    private fun EndlessFeed.answer(index: Int): List<Int> {
        val state = board(index).state
        return NonogramSolver.deduce(state.size, state.rowClues, state.columnClues).first.filter { it.second == NonogramSolver.FILLED }.map { it.first }.sorted()
    }

    @Test
    fun solvingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        clock += 2_000
        for (cell in f.answer(0)) f.paintNonogram(0, cell, cell, 1)
        assertEquals(1, f.snapshot().solvedCount)
        val result = store.results.single()
        assertEquals(GameKind.Nonogram, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertFalse(store.progress.containsKey(result.id))
        f.paintNonogram(0, 3, 3, 1)
        f.restartNonogram(0)
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun aBoardInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val cells = f.answer(0)
        f.paintNonogram(0, cells[0], cells[0], 1)
        clock += 4_000
        f.paintNonogram(0, cells[1], cells[1], 1)
        val saved = store.progress.values.single()
        assertEquals(GameKind.Nonogram, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
    }

    @Test
    fun restartingAClearBoardDoesNothing() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertFalse(f.restartNonogram(0))
        assertTrue(store.progress.isEmpty(), "nothing was done, so nothing is saved")
        f.paintNonogram(0, f.answer(0).first(), f.answer(0).first(), 1)
        assertTrue(f.restartNonogram(0))
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Nonogram))
        f.ensureSize(2)
        assertEquals(NonogramResult.Ignored, f.paintNonogram(0, 3, 3, 1), "page 0 is a word puzzle")
        assertFalse(f.restartNonogram(0))
    }

    @Test
    fun nonogramComesAfterArrowsInTheOrderOfTheGames() {
        val f = feed(games = listOf(GameKind.Arrows, GameKind.Nonogram))
        f.ensureSize(3)
        assertEquals(listOf(GameKind.Arrows, GameKind.Nonogram, GameKind.Arrows), f.snapshot().pages.map { it.kind })
    }
}
