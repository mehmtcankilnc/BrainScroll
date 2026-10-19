package com.mehmtcan.brainscroll.game.arrows

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

/** Arrow escape as one of the games of the endless feed. */
class ArrowsFeedTest {

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
        games: List<GameKind> = listOf(GameKind.Arrows),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.board(index: Int): ArrowsSnapshot = assertIs(snapshot().pages[index])

    /** The cells in the order they can leave, read from what the page shows. */
    private fun EndlessFeed.order(index: Int): List<Int> {
        val state = board(index).state
        val arrows = state.arrows
        val alive = BooleanArray(arrows.size) { arrows[it].alive }
        val owner = IntArray(state.size * state.size) { -1 }
        arrows.forEachIndexed { i, a -> a.cells.forEach { owner[it] = i } }
        // A way out found on the picture: the first free arrow each time (an arrow that leaves only frees the others).
        val order = ArrayList<Int>()
        while (alive.any { it }) {
            val next = alive.indices.first { i ->
                alive[i] && run {
                    val a = arrows[i]
                    var r = a.head / state.size + ArrowDir.dRow[a.dir]
                    var c = a.head % state.size + ArrowDir.dColumn[a.dir]
                    var clear = true
                    while (r in 0 until state.size && c in 0 until state.size) {
                        val o = owner[r * state.size + c]
                        if (o >= 0 && alive[o]) clear = false
                        r += ArrowDir.dRow[a.dir]
                        c += ArrowDir.dColumn[a.dir]
                    }
                    clear
                }
            }
            alive[next] = false
            order += arrows[next].head
        }
        return order
    }

    @Test
    fun clearingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        clock += 2_000
        for (cell in f.order(0)) f.tapArrow(0, cell)
        assertEquals(1, f.snapshot().solvedCount)
        val result = store.results.single()
        assertEquals(GameKind.Arrows, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertFalse(store.progress.containsKey(result.id))
        f.tapArrow(0, 3)
        f.restartArrows(0)
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun aBoardInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val cells = f.order(0)
        f.tapArrow(0, cells[0])
        clock += 4_000
        f.tapArrow(0, cells[1])
        val saved = store.progress.values.single()
        assertEquals(GameKind.Arrows, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
    }

    @Test
    fun restartingABoardNothingLeftDoesNothing() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertFalse(f.restartArrows(0))
        assertTrue(store.progress.isEmpty(), "nothing was done, so nothing is saved")
        f.tapArrow(0, f.order(0).first())
        assertTrue(f.restartArrows(0))
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Arrows))
        f.ensureSize(2)
        assertEquals(ArrowsResult.Ignored, f.tapArrow(0, 3), "page 0 is a word puzzle")
        assertFalse(f.restartArrows(0))
    }

    @Test
    fun arrowsComesAfterBlockOutInTheOrderOfTheGames() {
        val f = feed(games = listOf(GameKind.BlockOut, GameKind.Arrows))
        f.ensureSize(3)
        assertEquals(listOf(GameKind.BlockOut, GameKind.Arrows, GameKind.BlockOut), f.snapshot().pages.map { it.kind })
    }
}
