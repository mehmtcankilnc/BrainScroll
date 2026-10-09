package com.mehmtcan.brainscroll.game.mines

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

/** Minesweeper as one of the games of the endless feed. */
class MinesFeedTest {

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
        games: List<GameKind> = listOf(GameKind.Mines),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.board(index: Int): MinesSnapshot = assertIs(snapshot().pages[index])

    /** Plays a page to the end by logic. */
    private fun EndlessFeed.solve(index: Int) {
        openCell(index, 27)
        while (!board(index).isFinished) {
            val state = board(index).state
            val numbers = state.numbers.toIntArray()
            MinesSolver.deduce(state.rows, state.cols, numbers).safe.filter { numbers[it] < 0 }.forEach { openCell(index, it) }
        }
    }

    /** Opens a mine on page [index] (after the first open, the board is known from the saved seed). */
    private fun EndlessFeed.hitMine(index: Int, store: FakeStore) {
        openCell(index, 27)
        val seed = store.progress.values.single { it.game == GameKind.Mines }.answer.split(':')[1].toInt()
        val board = MinesGame.generate(8, 8, 10, seed, 27)
        openCell(index, board.indices.first { board[it] })
    }

    @Test
    fun theGamesComeOneAfterTheOtherAndMinesAreFourth() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Memory, GameKind.Tubes, GameKind.Mines))
        f.ensureSize(5)
        assertEquals(
            listOf(GameKind.Word, GameKind.Memory, GameKind.Tubes, GameKind.Mines, GameKind.Word),
            f.snapshot().pages.map { it.kind },
        )
    }

    @Test
    fun clearingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        clock += 2_000
        f.solve(0)
        assertEquals(1, f.snapshot().answerStreak)
        val result = store.results.single()
        assertEquals(GameKind.Mines, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertFalse(store.progress.containsKey(result.id))
        f.openCell(0, 3)
        f.flagCell(0, 3)
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun hittingAMineIsALossAndResetsTheStreak() {
        val store = FakeStore()
        val f = feed(store, games = listOf(GameKind.Mines))
        f.ensureSize(3)
        f.solve(0)
        assertEquals(1, f.snapshot().answerStreak)
        f.hitMine(1, store)
        assertEquals(0, f.snapshot().answerStreak)
        assertEquals(Outcome.LOST, store.results.last().outcome)
    }

    @Test
    fun aBoardInProgressWithFlagsComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.flagCell(0, 60)
        f.openCell(0, 27)
        clock += 4_000
        f.flagCell(0, 61)
        val saved = store.progress.values.single()
        assertEquals(GameKind.Mines, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
    }

    @Test
    fun aFlagOnAnUntouchedBoardStartsItSoItIsSaved() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertTrue(store.progress.isEmpty())
        f.flagCell(0, 5)
        assertEquals(1, store.progress.size)
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Mines))
        f.ensureSize(2)
        assertEquals(MinesResult.Ignored, f.openCell(0, 27), "page 0 is a word puzzle")
        assertFalse(f.flagCell(0, 1))
        assertEquals(MinesResult.Ignored, f.chordCell(0, 1))
    }
}
