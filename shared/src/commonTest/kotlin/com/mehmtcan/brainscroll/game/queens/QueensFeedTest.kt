package com.mehmtcan.brainscroll.game.queens

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

/** The queens puzzle as one of the games of the endless feed. */
class QueensFeedTest {

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
        games: List<GameKind> = listOf(GameKind.Queens),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.board(index: Int): QueensSnapshot = assertIs(snapshot().pages[index])

    /** The cells the player still has to fill for one placement that keeps the locked queens. */
    private fun EndlessFeed.missing(index: Int): List<Int> {
        val state = board(index).state
        val locked = state.locked.indices.filter { state.locked[it] }.toSet()
        return QueensSolver.solutions(state.size, locked).first().filter { it !in locked }
    }

    @Test
    fun solvingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        clock += 2_000
        for (cell in f.missing(0)) f.toggleQueen(0, cell)
        assertEquals(1, f.snapshot().solvedCount)
        val result = store.results.single()
        assertEquals(GameKind.Queens, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals("3", result.guesses.single())
        assertFalse(store.progress.containsKey(result.id))
        f.toggleQueen(0, 10)
        f.restartQueens(0)
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun aBoardInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val cells = f.missing(0)
        f.toggleQueen(0, cells[0])
        clock += 4_000
        f.toggleQueen(0, cells[1])
        val saved = store.progress.values.single()
        assertEquals(GameKind.Queens, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
    }

    @Test
    fun restartingABoardWithNoQueensOfThePlayerDoesNothing() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertFalse(f.restartQueens(0))
        assertTrue(store.progress.isEmpty(), "nothing was done, so nothing is saved")
        f.toggleQueen(0, f.missing(0).first())
        assertTrue(f.restartQueens(0))
        assertEquals(1, f.board(0).state.moves)
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Queens))
        f.ensureSize(2)
        assertEquals(QueensResult.Ignored, f.toggleQueen(0, 3), "page 0 is a word puzzle")
        assertFalse(f.restartQueens(0))
    }

    @Test
    fun queensComesAfterHanoiInTheOrderOfTheGames() {
        val f = feed(games = listOf(GameKind.Hanoi, GameKind.Queens))
        f.ensureSize(3)
        assertEquals(listOf(GameKind.Hanoi, GameKind.Queens, GameKind.Hanoi), f.snapshot().pages.map { it.kind })
    }
}
