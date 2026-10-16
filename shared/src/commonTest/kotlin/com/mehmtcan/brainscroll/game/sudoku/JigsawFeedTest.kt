package com.mehmtcan.brainscroll.game.sudoku

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
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The jigsaw sudoku as one of the games of the endless feed. It shares the sudoku actions of the feed. */
class JigsawFeedTest {

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
        games: List<GameKind> = listOf(GameKind.Jigsaw),
    ) = EndlessFeed(lists, Language.EN, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games)

    private fun EndlessFeed.board(index: Int): SudokuSnapshot = assertIs(snapshot().pages[index])

    private fun EndlessFeed.solve(index: Int) {
        val state = board(index).state
        val solution = SudokuGenerator.solveBySingles(state.givens.toIntArray(), SudokuLayout.of(state.regions))!!
        for (cell in state.values.indices) if (state.values[cell] == 0) placeDigit(index, cell, solution[cell])
    }

    @Test
    fun theFeedMakesJigsawPagesWithTheirOwnKindAndRegions() {
        val f = feed()
        f.ensureSize(2)
        assertEquals(listOf(GameKind.Jigsaw, GameKind.Jigsaw), f.snapshot().pages.map { it.kind })
        assertTrue(!SudokuLayout.of(f.board(0).state.regions).isStandard)
    }

    @Test
    fun solvingABoardSavesAJigsawWinOnce() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        clock += 2_000
        f.solve(0)
        assertEquals(1, f.snapshot().solvedCount)
        val result = store.results.single()
        assertEquals(GameKind.Jigsaw, result.game)
        assertEquals(Outcome.WON, result.outcome)
    }

    @Test
    fun aBoardInProgressComesBackAfterARestartAsAJigsaw() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val state = f.board(0).state
        val cell = state.values.indices.first { state.values[it] == 0 }
        f.toggleNote(0, cell, 3)
        clock += 4_000
        f.placeDigit(0, cell, (1..9).first { it != 3 })
        val saved = store.progress.values.single()
        assertEquals(GameKind.Jigsaw, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(GameKind.Jigsaw, back.snapshot().pages.single().kind)
        assertEquals(f.board(0).state, back.board(0).state)
    }

    @Test
    fun jigsawComesAfterTentsInTheOrderOfTheGamesAndClassicSudokuStaysClassic() {
        val f = feed(games = listOf(GameKind.Tents, GameKind.Jigsaw, GameKind.Sudoku))
        f.ensureSize(4)
        assertEquals(listOf(GameKind.Tents, GameKind.Jigsaw, GameKind.Sudoku, GameKind.Tents), f.snapshot().pages.map { it.kind })
        assertTrue(SudokuLayout.of(f.board(2).state.regions).isStandard)
    }
}
