package com.mehmtcan.brainscroll.game.line

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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The one-line game as a page of the feed that the daily puzzle runs on. */
class LineFeedTest {

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
    private val daily = LineFixtures.board

    private fun feed(
        store: FeedStore = FakeStore(),
        restored: List<StoredRound> = emptyList(),
        games: List<GameKind> = listOf(GameKind.Line),
    ) = EndlessFeed(
        lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock },
        newId = { "id${ids++}" }, games = games, lineBoard = { daily },
    )

    private fun EndlessFeed.board(index: Int): LineSnapshot = assertIs(snapshot().pages[index])

    private fun EndlessFeed.solve(index: Int) {
        for (cell in board(index).puzzle.solution) dragLine(index, cell)
    }

    @Test
    fun aNewPageGetsTheBoardTheFeedWasGiven() {
        val f = feed()
        f.ensureSize(1)
        assertContentEquals(daily.checkpoints, f.board(0).puzzle.checkpoints)
    }

    @Test
    fun withoutABoardGivenAPageIsAFiveByFiveBoardWithOneLine() {
        val f = EndlessFeed(lists, Language.EN, random = Random(3), games = listOf(GameKind.Line))
        f.ensureSize(1)
        val puzzle = f.board(0).puzzle
        assertEquals(5, puzzle.size)
        assertEquals(1, LineSolver.count(puzzle.size, puzzle.checkpoints, 2))
    }

    @Test
    fun solvingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        clock += 2_000
        f.solve(0)
        assertEquals(1, f.snapshot().solvedCount)
        val result = store.results.single()
        assertEquals(GameKind.Line, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(listOf(daily.cells.toString()), result.guesses, "the number of moves")
        assertFalse(store.progress.containsKey(result.id))
        f.dragLine(0, daily.solution[1])
        f.undoLine(0)
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun aBoardInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.dragLine(0, daily.solution[0])
        clock += 4_000
        f.dragLine(0, daily.solution[1])
        f.dragLine(0, daily.solution[2])
        f.undoLine(0)
        f.hintLine(0)
        val saved = store.progress.values.single()
        assertEquals(GameKind.Line, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
        assertEquals(1, back.board(0).state.hintsUsed)
    }

    @Test
    fun clearingOrUndoingABoardThatIsAtItsStartDoesNothing() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertFalse(f.restartLine(0))
        assertFalse(f.undoLine(0))
        assertTrue(store.progress.isEmpty(), "nothing was done, so nothing is saved")
        f.dragLine(0, daily.solution[0])
        assertTrue(f.restartLine(0))
        assertEquals(1, f.board(0).state.undos)
    }

    @Test
    fun aHintOnABoardThatWasNotStartedStartsItsClockAndIsSaved() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val hint = assertNotNull(f.hintLine(0))
        assertContentEquals(daily.solution.take(3), hint)
        assertEquals(1, f.board(0).state.hintsUsed)
        assertTrue(store.progress.isNotEmpty(), "a hint is something done, so the board is saved")
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Line))
        f.ensureSize(2)
        assertEquals(LineResult.Ignored, f.dragLine(0, 0), "page 0 is a word puzzle")
        assertEquals(LineResult.Ignored, f.tapLine(0, 0))
        assertFalse(f.undoLine(0))
        assertFalse(f.restartLine(0))
        assertNull(f.hintLine(0))
    }
}
