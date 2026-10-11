package com.mehmtcan.brainscroll.game.wordle

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.memory.FlipResult
import com.mehmtcan.brainscroll.game.memory.MemorySnapshot
import com.mehmtcan.brainscroll.game.memory.MemoryStatus
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The feed with two games: word puzzles and memory match, one after the other (docs/decisions.md). */
class MixedFeedTest {

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

    private val words = listOf("crane", "slate", "ghost", "pound", "apple", "brick")
    private val en = WordList(Language.EN, 5, valid = words, answers = words)
    private val tr = WordList(Language.TR, 5, valid = listOf("çiçek", "kırık", "kitap"), answers = listOf("çiçek", "kırık", "kitap"))
    private val lists = mapOf(Language.EN to en, Language.TR to tr)
    private val games = listOf(GameKind.Word, GameKind.Memory)
    private var clock = 1_000L
    private var ids = 0

    private fun feed(
        store: FeedStore = FakeStore(),
        restored: List<StoredRound> = emptyList(),
        streak: Int = 0,
    ) = EndlessFeed(
        wordLists = lists,
        startLanguage = Language.EN,
        random = Random(1),
        store = store,
        restored = restored,
        startStreak = streak,
        now = { clock },
        newId = { "id${ids++}" },
        games = games,
    )

    private fun EndlessFeed.page(index: Int) = snapshot().pages[index]

    private fun EndlessFeed.memory(index: Int): MemorySnapshot = assertIs(page(index))

    /** Plays a memory page perfectly: every pair is found without a mistake. */
    private fun EndlessFeed.solveMemory(index: Int) {
        val layout = memory(index).state.layout
        layout.indices.groupBy { layout[it] }.values.forEach { flip(index, it[0]); flip(index, it[1]) }
    }

    /** Loses a memory page: the first two cards that differ are flipped over and over. */
    private fun EndlessFeed.loseMemory(index: Int) {
        val layout = memory(index).state.layout
        val other = layout.indices.first { layout[it] != layout[0] }
        repeat(6) { flip(index, 0); flip(index, other) }
    }

    private fun EndlessFeed.playWord(index: Int) {
        for (w in words) {
            if (page(index).isFinished) return
            w.forEach { type(index, it) }
            submit(index)
        }
    }

    @Test
    fun theGamesComeOneAfterTheOtherInTheOrderGiven() {
        val f = feed()
        f.ensureSize(5)
        assertEquals(
            listOf(GameKind.Word, GameKind.Memory, GameKind.Word, GameKind.Memory, GameKind.Word),
            f.snapshot().pages.map { it.kind },
        )
    }

    @Test
    fun aFeedWithOnlyWordPuzzlesIsWhatItWasBefore() {
        val f = EndlessFeed(lists, Language.EN, Random(1))
        f.ensureSize(4)
        assertTrue(f.snapshot().pages.all { it.kind == GameKind.Word })
        assertEquals(4, f.snapshot().rounds.size)
    }

    @Test
    fun theRoundsViewHoldsOnlyTheWordPages() {
        val f = feed()
        f.ensureSize(4)
        assertEquals(listOf(0, 2), f.snapshot().rounds.map { it.index })
    }

    @Test
    fun aMemoryBoardHasTwelveCardsAndNoneFaceUp() {
        val f = feed()
        f.ensureSize(2)
        val state = f.memory(1).state
        assertEquals(12, state.layout.size)
        assertTrue(state.faceUp.isEmpty())
        assertEquals(6, state.maxMistakes)
    }

    @Test
    fun actionsOfTheOtherGameAreIgnored() {
        val f = feed()
        f.ensureSize(2)
        f.type(1, 'A') // page 1 is a memory board
        f.submit(1)
        assertTrue(f.memory(1).state.faceUp.isEmpty())
        assertEquals(FlipResult.Ignored, f.flip(0, 0), "page 0 is a word puzzle")
        assertTrue(f.snapshot().rounds.single().rows.isEmpty())
    }

    @Test
    fun winningMemoryRaisesTheStreakAndIsSaved() {
        val store = FakeStore()
        val f = feed(store, streak = 2)
        f.ensureSize(2)
        f.solveMemory(1)
        assertEquals(MemoryStatus.Won, f.memory(1).state.status)
        assertEquals(3, f.snapshot().answerStreak)
        val result = store.results.single()
        assertEquals(GameKind.Memory, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertTrue(store.progress.isEmpty(), "a finished board is not kept as progress")
    }

    @Test
    fun losingMemoryResetsTheStreakJustLikeAWordLoss() {
        val store = FakeStore()
        val f = feed(store, streak = 5)
        f.ensureSize(2)
        f.loseMemory(1)
        assertEquals(MemoryStatus.Lost, f.memory(1).state.status)
        assertEquals(0, f.snapshot().answerStreak)
        assertEquals(Outcome.LOST, store.results.single().outcome)
    }

    @Test
    fun theStreakSpansBothGames() {
        val f = feed()
        f.ensureSize(4)
        f.playWord(0)
        f.solveMemory(1)
        f.playWord(2)
        assertEquals(3, f.snapshot().answerStreak, "word win, memory win, word win")
    }

    @Test
    fun aFinishedBoardIsCountedOnlyOnce() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        f.solveMemory(1)
        f.flip(1, 0) // too late
        assertEquals(1, store.results.size)
        assertEquals(1, f.snapshot().answerStreak)
    }

    @Test
    fun changingTheWordLanguageDoesNotTouchMemoryBoards() {
        val f = feed()
        f.ensureSize(4)
        val boards = listOf(f.memory(1).state.layout, f.memory(3).state.layout)
        f.setWordLanguage(Language.TR, fromIndex = 0)
        assertEquals(boards, listOf(f.memory(1).state.layout, f.memory(3).state.layout))
        assertTrue(f.snapshot().rounds.all { it.language == Language.TR })
    }

    @Test
    fun aStartedBoardIsSavedAndAnUntouchedOneIsNot() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(4)
        assertTrue(store.progress.isEmpty())
        f.flip(3, 2)
        val saved = store.progress.values.single()
        assertEquals(GameKind.Memory, saved.game)
        assertEquals(3, saved.position)
        assertEquals(listOf("2"), saved.guesses)
    }

    @Test
    fun mixedProgressComesBackInOrderAfterARestartAndTheRotationGoesOn() {
        val store = FakeStore()
        val first = feed(store)
        first.ensureSize(4)
        first.type(0, 'C')      // page 0: a word puzzle, started
        first.flip(1, 0)        // page 1: a memory board, one card up
        clock += 2_000
        first.flip(3, 3)        // page 3: another memory board, started (page 2, a word puzzle, is untouched)
        assertEquals(listOf(GameKind.Word, GameKind.Memory, GameKind.Memory), store.progress.values.map { it.game })

        val second = feed(FakeStore(), restored = store.progress.values.toList())
        second.ensureSize(4)
        val pages = second.snapshot().pages
        assertEquals(listOf(GameKind.Word, GameKind.Memory, GameKind.Memory, GameKind.Word), pages.map { it.kind },
            "the page after the last memory board is a word puzzle again")
        assertEquals("C", (pages[0] as RoundSnapshot).input)
        assertEquals(setOf(0), (pages[1] as MemorySnapshot).state.faceUp)
        assertEquals(setOf(3), (pages[2] as MemorySnapshot).state.faceUp)
    }

    @Test
    fun aMemoryBoardWithUnreadableSavedDataIsLeftOut() {
        val broken = StoredRound("bad", Language.EN, "not a board", emptyList(), "", 0, GameKind.Memory)
        val f = feed(restored = listOf(broken))
        assertEquals(0, f.size)
    }

    @Test
    fun aRestoredBoardKeepsItsTime() {
        val store = FakeStore()
        val first = feed(store)
        first.ensureSize(2)
        first.flip(1, 0)
        clock += 4_000
        first.flip(1, 1)
        val second = feed(FakeStore(), restored = store.progress.values.toList())
        assertEquals(4_000, second.memory(0).durationMs)
    }

    @Test
    fun theTimeOfAFinishedBoardIsInItsResult() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        val layout = f.memory(1).state.layout
        val groups = layout.indices.groupBy { layout[it] }.values.toList()
        for (pair in groups) {
            f.flip(1, pair[0])
            clock += 1_000
            f.flip(1, pair[1])
            clock += 1_000
        }
        assertEquals(11_000, store.results.single().durationMs, "twelve flips have eleven gaps of one second between them")
    }
}
