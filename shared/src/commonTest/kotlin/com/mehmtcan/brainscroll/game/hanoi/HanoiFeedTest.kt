package com.mehmtcan.brainscroll.game.hanoi

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

/** The tower of Hanoi as one of the games of the endless feed. */
class HanoiFeedTest {

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
        games: List<GameKind> = listOf(GameKind.Hanoi),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.board(index: Int): HanoiSnapshot = assertIs(snapshot().pages[index])

    private fun EndlessFeed.solve(index: Int) {
        val state = board(index).state
        for (step in HanoiSolver.shortest(state.pegs.toIntArray(), state.goal)) moveDisk(index, step.from, step.to)
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
        assertEquals(GameKind.Hanoi, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(f.board(0).state.par.toString(), result.guesses.single())
        assertFalse(store.progress.containsKey(result.id))
        f.moveDisk(0, 0, 1)
        f.restartHanoi(0)
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun aBoardInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val state = f.board(0).state
        val way = HanoiSolver.shortest(state.pegs.toIntArray(), state.goal)
        f.moveDisk(0, way[0].from, way[0].to)
        clock += 4_000
        f.moveDisk(0, way[1].from, way[1].to)
        val saved = store.progress.values.single()
        assertEquals(GameKind.Hanoi, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
    }

    @Test
    fun restartingABoardThatIsAtItsStartDoesNothing() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertFalse(f.restartHanoi(0))
        assertTrue(store.progress.isEmpty(), "nothing was done, so nothing is saved")
        val way = HanoiSolver.shortest(f.board(0).state.pegs.toIntArray(), f.board(0).state.goal)
        f.moveDisk(0, way[0].from, way[0].to)
        assertTrue(f.restartHanoi(0))
        assertEquals(1, f.board(0).state.moves)
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Hanoi))
        f.ensureSize(2)
        assertEquals(HanoiResult.Ignored, f.moveDisk(0, 0, 1), "page 0 is a word puzzle")
        assertFalse(f.restartHanoi(0))
    }

    @Test
    fun hanoiComesAfterPipesInTheOrderOfTheGames() {
        val f = feed(games = listOf(GameKind.Pipes, GameKind.Hanoi))
        f.ensureSize(3)
        assertEquals(listOf(GameKind.Pipes, GameKind.Hanoi, GameKind.Pipes), f.snapshot().pages.map { it.kind })
    }
}
