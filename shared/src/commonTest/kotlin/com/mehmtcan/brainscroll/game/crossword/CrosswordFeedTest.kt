package com.mehmtcan.brainscroll.game.crossword

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

/** The crossword as one of the games of the endless feed. It takes the languages of the word puzzles. */
class CrosswordFeedTest {

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
        games: List<GameKind> = listOf(GameKind.Crossword),
        start: Language = Language.EN,
        startLanguages: List<Language> = listOf(start),
    ) = EndlessFeed(lists, start, random = Random(7), store = store, restored = restored, now = { clock }, newId = { "id${ids++}" }, games = games, startLanguages = startLanguages)

    private fun EndlessFeed.board(index: Int): CrosswordSnapshot = assertIs(snapshot().pages[index])

    private fun EndlessFeed.solve(index: Int) {
        val page = board(index)
        val letters = CrosswordTestSolver.solve(page.language, page.state.isClue, page.state.clues)!!
        for (cell in letters.indices) if (letters[cell] != ' ') placeLetter(index, cell, letters[cell])
    }

    @Test
    fun theBoardsComeInTheLanguageOfTheWordPuzzles() {
        val en = feed(start = Language.EN)
        en.ensureSize(3)
        assertTrue(en.snapshot().pages.all { (it as CrosswordSnapshot).language == Language.EN })
        val tr = feed(start = Language.TR)
        tr.ensureSize(3)
        assertTrue(tr.snapshot().pages.all { (it as CrosswordSnapshot).language == Language.TR })
    }

    @Test
    fun withBothLanguagesOnEachBoardGetsOneOfThem() {
        val f = feed(startLanguages = listOf(Language.EN, Language.TR))
        f.ensureSize(12)
        val languages = f.snapshot().pages.map { (it as CrosswordSnapshot).language }.toSet()
        assertEquals(setOf(Language.EN, Language.TR), languages)
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
        assertEquals(GameKind.Crossword, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(Language.EN, result.language)
        assertFalse(store.progress.containsKey(result.id))
        f.placeLetter(0, 6, 'A')
        assertEquals(1, store.results.size, "a finished board takes no more actions")
    }

    @Test
    fun aBoardInProgressComesBackAfterARestart() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        val page = f.board(0)
        val letters = CrosswordTestSolver.solve(page.language, page.state.isClue, page.state.clues)!!
        val cells = letters.indices.filter { letters[it] != ' ' }
        f.placeLetter(0, cells[0], letters[cells[0]])
        clock += 4_000
        f.placeLetter(0, cells[1], 'Q')
        f.eraseLetter(0, cells[1])
        val saved = store.progress.values.single()
        assertEquals(GameKind.Crossword, saved.game)

        val back = feed(store, restored = listOf(saved))
        assertEquals(f.board(0).state, back.board(0).state)
        assertEquals(f.board(0).durationMs, back.board(0).durationMs)
        assertEquals(Language.EN, back.board(0).language)
    }

    @Test
    fun theActionsAreIgnoredOnAPageOfAnotherGame() {
        val f = feed(games = listOf(GameKind.Word, GameKind.Crossword))
        f.ensureSize(2)
        assertEquals(CrosswordResult.Ignored, f.placeLetter(0, 6, 'A'), "page 0 is a word puzzle")
        assertEquals(CrosswordResult.Ignored, f.eraseLetter(0, 6))
    }

    @Test
    fun changingTheLanguageDrawsAgainTheBoardsThatWereNotStarted() {
        val f = feed(start = Language.EN)
        f.ensureSize(3)
        f.setWordLanguages(setOf(Language.TR), fromIndex = 0)
        assertTrue(f.snapshot().pages.all { (it as CrosswordSnapshot).language == Language.TR })
    }

    @Test
    fun crosswordComesAfterBlockOutInTheOrderOfTheGames() {
        val f = feed(games = listOf(GameKind.BlockOut, GameKind.Crossword))
        f.ensureSize(3)
        assertEquals(listOf(GameKind.BlockOut, GameKind.Crossword, GameKind.BlockOut), f.snapshot().pages.map { it.kind })
    }
}
