package com.mehmtcan.brainscroll.game.lights

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

/** Lights Out as one of the games of the endless feed. */
class LightsFeedTest {

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
        games: List<GameKind> = listOf(GameKind.Lights),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.board(index: Int): LightsSnapshot = assertIs(snapshot().pages[index])

    /** Solves page [index]: the board is rebuilt from the seed the store holds. */
    private fun EndlessFeed.solve(index: Int, store: FakeStore) {
        for (cell in solutionOf(index, store)) tapLight(index, cell)
    }

    private fun solutionOf(index: Int, store: FakeStore): List<Int> {
        val seed = store.progress.values.first { it.position == index }.answer.split(':')[1].toInt()
        return LightsGame.solutionTaps(seed, 5, 7)
    }

    @Test
    fun theGamesComeOneAfterTheOtherAndLightsAreSixth() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Memory, GameKind.Tubes, GameKind.Mines, GameKind.Sudoku, GameKind.Lights))
        f.ensureSize(7)
        assertEquals(
            listOf(GameKind.Word, GameKind.Memory, GameKind.Tubes, GameKind.Mines, GameKind.Sudoku, GameKind.Lights, GameKind.Word),
            f.snapshot().pages.map { it.kind },
        )
    }

    @Test
    fun solvingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        f.tapLight(0, 0)
        clock += 2_000
        f.tapLight(0, 0)
        f.solve(0, store)
        assertEquals(1, f.snapshot().answerStreak)
        val result = store.results.single()
        assertEquals(GameKind.Lights, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(listOf("9"), result.guesses, "two cancelled taps and seven real ones")
        assertFalse(store.progress.containsKey(result.id))
        f.tapLight(0, 3)
        f.restartLights(0)
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun aBoardInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.tapLight(0, 6)
        clock += 4_000
        f.tapLight(0, 12)
        f.restartLights(0)
        f.tapLight(0, 3)
        val saved = store.progress.values.single()
        assertEquals(GameKind.Lights, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
    }

    @Test
    fun restartingABoardThatIsAtItsStartDoesNothing() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertFalse(f.restartLights(0))
        assertTrue(store.progress.isEmpty(), "nothing was done, so nothing is saved")
        f.tapLight(0, 1)
        assertTrue(f.restartLights(0))
        assertEquals(1, f.board(0).state.moves)
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Lights))
        f.ensureSize(2)
        assertEquals(LightsResult.Ignored, f.tapLight(0, 0), "page 0 is a word puzzle")
        assertFalse(f.restartLights(0))
    }
}
