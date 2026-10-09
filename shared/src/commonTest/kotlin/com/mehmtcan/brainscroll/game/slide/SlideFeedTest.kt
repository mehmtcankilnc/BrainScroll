package com.mehmtcan.brainscroll.game.slide

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

/** The sliding puzzle as one of the games of the endless feed. */
class SlideFeedTest {

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
        games: List<GameKind> = listOf(GameKind.Slide),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.board(index: Int): SlideSnapshot = assertIs(snapshot().pages[index])

    private fun EndlessFeed.solve(index: Int) {
        for (cell in SlideSolver.solve(board(index).state.tiles, 3, 4)) slideTile(index, cell)
    }

    private fun EndlessFeed.movable(index: Int): Int {
        val blank = board(index).state.tiles.indexOf(0)
        return if (blank % 3 > 0) blank - 1 else blank + 1
    }

    @Test
    fun theGamesComeOneAfterTheOtherAndSlideIsSeventh() {
        val all = listOf(GameKind.Word, GameKind.Memory, GameKind.Tubes, GameKind.Mines, GameKind.Sudoku, GameKind.Lights, GameKind.Slide)
        val f = feed(games = all)
        f.ensureSize(8)
        assertEquals(all + GameKind.Word, f.snapshot().pages.map { it.kind })
    }

    @Test
    fun solvingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        clock += 2_000
        f.solve(0)
        assertEquals(1, f.snapshot().answerStreak)
        val result = store.results.single()
        assertEquals(GameKind.Slide, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertFalse(store.progress.containsKey(result.id))
        f.slideTile(0, 3)
        f.restartSlide(0)
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun aBoardInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.slideTile(0, f.movable(0))
        clock += 4_000
        f.slideTile(0, f.movable(0))
        f.restartSlide(0)
        f.slideTile(0, f.movable(0))
        val saved = store.progress.values.single()
        assertEquals(GameKind.Slide, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
    }

    @Test
    fun restartingABoardThatIsAtItsStartDoesNothing() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertFalse(f.restartSlide(0))
        assertTrue(store.progress.isEmpty(), "nothing was done, so nothing is saved")
        f.slideTile(0, f.movable(0))
        assertTrue(f.restartSlide(0))
        assertEquals(1, f.board(0).state.moves)
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Slide))
        f.ensureSize(2)
        assertEquals(SlideResult.Ignored, f.slideTile(0, 0), "page 0 is a word puzzle")
        assertFalse(f.restartSlide(0))
    }
}
