package com.mehmtcan.brainscroll.game.pipes

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

/** The pipes game as one of the games of the endless feed. */
class PipesFeedTest {

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
        games: List<GameKind> = listOf(GameKind.Pipes),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.board(index: Int): PipesSnapshot = assertIs(snapshot().pages[index])

    /** Solves page [index] with the fewest taps. */
    private fun EndlessFeed.solve(index: Int) {
        val turns = PipesGenerator.solve(board(index).state.tiles.toIntArray(), 5)!!
        for (cell in turns.indices) repeat(turns[cell]) { turnPipe(index, cell) }
    }

    @Test
    fun theGamesComeOneAfterTheOtherAndPipesIsNinth() {
        val all = listOf(
            GameKind.Word, GameKind.Memory, GameKind.Tubes, GameKind.Mines, GameKind.Sudoku,
            GameKind.Lights, GameKind.Slide, GameKind.Target, GameKind.Pipes,
        )
        val f = feed(games = all)
        f.ensureSize(10)
        assertEquals(all + GameKind.Word, f.snapshot().pages.map { it.kind })
    }

    @Test
    fun solvingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        clock += 2_000
        f.solve(0)
        assertEquals(1, f.snapshot().solvedCount)
        val result = store.results.single()
        assertEquals(GameKind.Pipes, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(f.board(0).state.par.toString(), result.guesses.single())
        assertFalse(store.progress.containsKey(result.id))
        f.turnPipe(0, 3)
        f.restartPipes(0)
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun aBoardInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.turnPipe(0, 6)
        clock += 4_000
        f.turnPipe(0, 12)
        f.restartPipes(0)
        f.turnPipe(0, 3)
        val saved = store.progress.values.single()
        assertEquals(GameKind.Pipes, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
    }

    @Test
    fun restartingABoardThatIsAtItsStartDoesNothing() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertFalse(f.restartPipes(0))
        assertTrue(store.progress.isEmpty(), "nothing was done, so nothing is saved")
        f.turnPipe(0, 1)
        assertTrue(f.restartPipes(0))
        assertEquals(1, f.board(0).state.moves)
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Pipes))
        f.ensureSize(2)
        assertEquals(PipesResult.Ignored, f.turnPipe(0, 0), "page 0 is a word puzzle")
        assertFalse(f.restartPipes(0))
    }
}
