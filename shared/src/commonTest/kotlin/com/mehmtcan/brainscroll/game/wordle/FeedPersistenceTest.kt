package com.mehmtcan.brainscroll.game.wordle

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedPersistenceTest {

    /** Keeps everything the feed saves in memory so the tests can inspect it. */
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

    private var idCounter = 0
    private fun feed(
        store: FeedStore,
        restored: List<StoredRound> = emptyList(),
        startStreak: Int = 0,
        time: () -> Long = { 1_000L },
    ) = EndlessFeed(
        wordLists = lists,
        startLanguage = Language.EN,
        random = Random(1),
        store = store,
        restored = restored,
        startStreak = startStreak,
        now = time,
        newId = { "id${idCounter++}" },
    )

    private fun EndlessFeed.guess(index: Int, word: String) {
        word.forEach { type(index, it) }
        submit(index)
    }

    /** Plays every word until the round is over (one of the six words is the answer). */
    private fun EndlessFeed.playToTheEnd(index: Int) {
        for (w in words) {
            if (snapshot().rounds[index].isFinished) return
            guess(index, w)
        }
    }

    @Test
    fun untouchedRoundsAreNotSaved() {
        val store = FakeStore()
        feed(store).ensureSize(3)
        assertTrue(store.progress.isEmpty())
    }

    @Test
    fun typingSavesTheRoundWithItsInput() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.type(0, 'c')
        f.type(0, 'r')
        val saved = store.progress.values.single()
        assertEquals("CR", saved.input)
        assertEquals(emptyList(), saved.guesses)
        assertEquals(Language.EN, saved.language)
    }

    @Test
    fun deletingAllLettersRemovesTheRoundFromStorage() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.type(0, 'c')
        f.backspace(0)
        assertTrue(store.progress.isEmpty())
    }

    @Test
    fun anAcceptedGuessIsSavedAndTheInputIsCleared() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.guess(0, "slate")
        // "slate" may be the answer; in that case the round is already in the history instead.
        val saved = store.progress.values.singleOrNull() ?: return
        assertEquals(listOf("SLATE"), saved.guesses)
        assertEquals("", saved.input)
    }

    @Test
    fun skippingSavesTheRoundEvenWhenNothingWasTyped() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        f.leave(0)
        assertTrue(store.progress.values.single().skipped)
    }

    @Test
    fun aFinishedRoundMovesToTheHistory() {
        val store = FakeStore()
        val f = feed(store, time = { 42_000L })
        f.ensureSize(1)
        f.playToTheEnd(0)
        val result = store.results.single()
        assertTrue(store.progress.isEmpty())
        assertEquals(42_000L, result.finishedAt)
        assertEquals(Mode.ENDLESS, result.mode)
        assertEquals(Language.EN, result.language)
        assertEquals(if (result.guesses.last() == result.answer) Outcome.WON else Outcome.LOST, result.outcome)
        assertFalse(result.wasSkipped)
    }

    @Test
    fun aRoundThatWasSkippedAndLaterFinishedIsMarkedAsSkipped() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(2)
        f.leave(0)
        f.playToTheEnd(0)
        assertTrue(store.results.single().wasSkipped)
    }

    @Test
    fun aFinishedRoundIsReportedOnlyOnce() {
        val store = FakeStore()
        val f = feed(store)
        f.ensureSize(1)
        f.playToTheEnd(0)
        f.submit(0)
        f.submit(0)
        assertEquals(1, store.results.size)
    }

    @Test
    fun restoredRoundsComeBackExactlyAsTheyWereLeft() {
        val stored = listOf(
            StoredRound("a", Language.EN, "CRANE", listOf("SLATE"), "GH", skipped = true, position = 4),
            StoredRound("b", Language.TR, "KİTAP", emptyList(), "KI", skipped = false, position = 9),
        )
        val f = feed(FakeStore(), restored = stored)
        val rounds = f.snapshot().rounds

        assertEquals(2, f.size)
        assertEquals(listOf(0, 1), rounds.map { it.index })
        assertEquals(1, rounds[0].rows.size)
        assertEquals("GH", rounds[0].input)
        assertTrue(rounds[0].skipped)
        assertEquals(Language.TR, rounds[1].language)
        assertEquals("KI", rounds[1].input)
    }

    @Test
    fun restoredRoundsAreSavedAgainWithTheirNewPositions() {
        val store = FakeStore()
        val stored = listOf(
            StoredRound("a", Language.EN, "CRANE", emptyList(), "C", skipped = false, position = 7),
            StoredRound("b", Language.EN, "SLATE", emptyList(), "S", skipped = false, position = 12),
        )
        feed(store, restored = stored)
        assertEquals(listOf(0, 1), store.progress.values.map { it.position })
    }

    @Test
    fun newRoundsAreCreatedAfterTheRestoredOnes() {
        val stored = listOf(StoredRound("a", Language.EN, "CRANE", emptyList(), "C", skipped = false, position = 0))
        val f = feed(FakeStore(), restored = stored)
        f.ensureSize(3)
        assertEquals(3, f.size)
        assertEquals("C", f.snapshot().rounds[0].input) // the restored round stays first
    }

    @Test
    fun theLanguageIsLockedWhenTouchedRoundsWereRestored() {
        val stored = listOf(StoredRound("a", Language.EN, "CRANE", emptyList(), "C", skipped = false, position = 0))
        val f = feed(FakeStore(), restored = stored)
        assertFalse(f.snapshot().canChangeLanguage)
        assertFalse(f.setLanguage(Language.TR))
    }

    @Test
    fun theStartStreakIsKeptAndContinues() {
        val f = feed(FakeStore(), startStreak = 4)
        f.ensureSize(1)
        assertEquals(4, f.answerStreak)
        f.playToTheEnd(0)
        val won = f.snapshot().rounds[0].status == WordleStatus.Won
        assertEquals(if (won) 5 else 0, f.answerStreak)
    }

    @Test
    fun skipsAreAvailableAgainInANewSession() {
        // A new feed (cold start) starts with the full skip allowance, even if progress was restored.
        val stored = listOf(StoredRound("a", Language.EN, "CRANE", emptyList(), "C", skipped = true, position = 0))
        assertEquals(1, feed(FakeStore(), restored = stored).skipsLeft)
    }

    @Test
    fun roundsInALanguageWithoutAWordListAreIgnored() {
        val onlyEnglish = EndlessFeed(
            wordLists = mapOf(Language.EN to en),
            startLanguage = Language.EN,
            restored = listOf(StoredRound("t", Language.TR, "KİTAP", emptyList(), "K", skipped = false, position = 0)),
        )
        assertEquals(0, onlyEnglish.size)
    }
}
