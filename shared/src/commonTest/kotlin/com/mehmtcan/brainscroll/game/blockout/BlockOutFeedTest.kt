package com.mehmtcan.brainscroll.game.blockout

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

/** Block out as one of the games of the endless feed. */
class BlockOutFeedTest {

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
        games: List<GameKind> = listOf(GameKind.BlockOut),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.board(index: Int): BlockOutSnapshot = assertIs(snapshot().pages[index])

    private fun EndlessFeed.way(index: Int): List<BlockMove> {
        val state = board(index).state
        val shapes = state.pieces.map { PieceShape(it.horizontal, it.line, it.length, it.isTarget) }
        return BlockOutSolver.shortest(state.size, shapes, IntArray(shapes.size) { state.pieces[it].pos })!!
    }

    @Test
    fun solvingABoardSavesAWinOnceAndRaisesTheStreak() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        clock += 2_000
        val par = f.board(0).state.par
        for (move in f.way(0)) f.slideBlock(0, move.piece, move.to)
        assertEquals(1, f.snapshot().solvedCount)
        val result = store.results.single()
        assertEquals(GameKind.BlockOut, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(par.toString(), result.guesses.single())
        assertFalse(store.progress.containsKey(result.id))
        f.slideBlock(0, 0, 0)
        f.restartBlockOut(0)
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun aBoardInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val way = f.way(0)
        f.slideBlock(0, way[0].piece, way[0].to)
        clock += 4_000
        f.slideBlock(0, way[1].piece, way[1].to)
        val saved = store.progress.values.single()
        assertEquals(GameKind.BlockOut, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
    }

    @Test
    fun restartingABoardThatIsAtItsStartDoesNothing() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        assertFalse(f.restartBlockOut(0))
        assertTrue(store.progress.isEmpty(), "nothing was done, so nothing is saved")
        val way = f.way(0)
        f.slideBlock(0, way[0].piece, way[0].to)
        assertTrue(f.restartBlockOut(0))
        assertEquals(1, f.board(0).state.moves)
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.BlockOut))
        f.ensureSize(2)
        assertEquals(BlockOutResult.Ignored, f.slideBlock(0, 0, 1), "page 0 is a word puzzle")
        assertFalse(f.restartBlockOut(0))
    }

    @Test
    fun blockOutComesAfterJigsawInTheOrderOfTheGames() {
        val f = feed(games = listOf(GameKind.Jigsaw, GameKind.BlockOut))
        f.ensureSize(3)
        assertEquals(listOf(GameKind.Jigsaw, GameKind.BlockOut, GameKind.Jigsaw), f.snapshot().pages.map { it.kind })
    }
}
