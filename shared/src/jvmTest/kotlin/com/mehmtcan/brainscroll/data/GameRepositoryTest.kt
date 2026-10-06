package com.mehmtcan.brainscroll.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound
import com.mehmtcan.brainscroll.game.wordle.WordList
import com.mehmtcan.brainscroll.stats.Streaks
import com.mehmtcan.brainscroll.time.IstanbulDay
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Runs the repository against a real SQLite database that lives in memory. */
class GameRepositoryTest {

    private fun newRepository(): GameRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BrainScrollDatabase.Schema.create(driver)
        return GameRepository(BrainScrollDatabase(driver))
    }

    private fun finished(
        id: String,
        at: Long = 1_000L,
        outcome: Outcome = Outcome.WON,
        guesses: List<String> = listOf("SLATE", "CRANE"),
    ) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = Language.EN,
        answer = "CRANE",
        guesses = guesses,
        outcome = outcome,
        wasSkipped = false,
        finishedAt = at,
    )

    private fun stored(id: String, position: Int, input: String = "", guesses: List<String> = emptyList()) =
        StoredRound(id, Language.TR, "KİTAP", guesses, input, skipped = false, position = position)

    @Test
    fun anUnfinishedRoundSurvivesARoundTrip() {
        val repo = newRepository()
        val round = StoredRound("a", Language.TR, "ÇİÇEK", listOf("KİTAP", "KIRIK"), "ÇİÇ", skipped = true, position = 3)
        repo.saveProgress(round)
        assertEquals(listOf(round), repo.unfinishedRounds())
    }

    @Test
    fun aRoundWithNoGuessesAndNoInputIsStoredAndReadBack() {
        val repo = newRepository()
        val round = stored("a", position = 0)
        repo.saveProgress(round)
        assertEquals(listOf(round), repo.unfinishedRounds())
    }

    @Test
    fun savingTheSameRoundAgainReplacesIt() {
        val repo = newRepository()
        repo.saveProgress(stored("a", 0, input = "K"))
        repo.saveProgress(stored("a", 0, input = "KI"))
        assertEquals(listOf("KI"), repo.unfinishedRounds().map { it.input })
    }

    @Test
    fun unfinishedRoundsComeBackInFeedOrder() {
        val repo = newRepository()
        repo.saveProgress(stored("c", 2))
        repo.saveProgress(stored("a", 0))
        repo.saveProgress(stored("b", 1))
        assertEquals(listOf("a", "b", "c"), repo.unfinishedRounds().map { it.id })
    }

    @Test
    fun deletingProgressRemovesTheRound() {
        val repo = newRepository()
        repo.saveProgress(stored("a", 0))
        repo.deleteProgress("a")
        assertTrue(repo.unfinishedRounds().isEmpty())
    }

    @Test
    fun finishingMovesTheRoundFromProgressToHistoryInOneStep() {
        val repo = newRepository()
        repo.saveProgress(stored("a", 0, input = "K"))
        repo.finish(finished("a"))
        assertTrue(repo.unfinishedRounds().isEmpty())
        assertEquals(listOf(finished("a")), repo.history())
    }

    @Test
    fun finishingTwiceDoesNotDuplicateTheResult() {
        val repo = newRepository()
        repo.finish(finished("a"))
        repo.finish(finished("a"))
        assertEquals(1, repo.history().size)
    }

    @Test
    fun historyIsOldestFirst() {
        val repo = newRepository()
        repo.finish(finished("late", at = 3_000L))
        repo.finish(finished("early", at = 1_000L))
        repo.finish(finished("middle", at = 2_000L))
        assertEquals(listOf("early", "middle", "late"), repo.history().map { it.id })
    }

    @Test
    fun theIstanbulDayIsStoredWithTheResult() {
        val repo = newRepository()
        // 22:00 UTC is already the next day in Istanbul.
        val at = 22 * 3_600_000L
        repo.finish(finished("a", at = at))
        assertEquals(IstanbulDay.dayIndex(at), repo.history().single().dayIndex)
        assertEquals(1L, repo.history().single().dayIndex)
    }

    @Test
    fun theDurationOfADailyResultSurvivesARoundTrip() {
        val repo = newRepository()
        repo.finish(finished("d").copy(mode = Mode.DAILY, durationMs = 61_234L))
        assertEquals(61_234L, repo.history().single().durationMs)
        assertEquals(0, repo.pendingCount()) // daily results are written by the server, never uploaded
    }

    @Test
    fun aLostResultKeepsItsOutcome() {
        val repo = newRepository()
        repo.finish(finished("a", outcome = Outcome.LOST, guesses = listOf("A", "B")))
        assertEquals(Outcome.LOST, repo.history().single().outcome)
        assertEquals(listOf("A", "B"), repo.history().single().guesses)
    }

    @Test
    fun favoritesCanBeAddedAndRemoved() {
        val repo = newRepository()
        repo.finish(finished("a"))
        assertFalse(repo.isFavorite("a"))
        repo.setFavorite("a", true, now = 10L)
        assertTrue(repo.isFavorite("a"))
        repo.setFavorite("a", false, now = 20L)
        assertFalse(repo.isFavorite("a"))
        assertTrue(repo.favoriteResults().isEmpty())
    }

    @Test
    fun favoriteResultsAreNewestPuzzleFirstAndOnlyFavorites() {
        val repo = newRepository()
        repo.finish(finished("old", at = 1_000L))
        repo.finish(finished("new", at = 2_000L))
        repo.finish(finished("other", at = 3_000L))
        repo.setFavorite("old", true, 1L)
        repo.setFavorite("new", true, 2L)
        assertEquals(listOf("new", "old"), repo.favoriteResults().map { it.id })
    }

    @Test
    fun languageSettingIsRememberedAndEmptyByDefault() {
        val repo = newRepository()
        assertNull(repo.savedLanguage())
        repo.saveLanguage(Language.TR)
        assertEquals(Language.TR, repo.savedLanguage())
        repo.saveLanguage(Language.EN)
        assertEquals(Language.EN, repo.savedLanguage())
    }

    @Test
    fun closingAndReopeningTheAppKeepsEverything() {
        val words = listOf("crane", "slate", "ghost", "pound", "apple", "brick")
        val lists = mapOf(Language.EN to WordList(Language.EN, 5, words, words))
        val repo = newRepository()

        // First run: finish one puzzle, leave one half done and skip another.
        val first = EndlessFeed(lists, Language.EN, Random(3), store = repo, now = { 5_000L })
        first.ensureSize(3)
        for (w in words) {
            if (first.snapshot().rounds[0].isFinished) break
            w.forEach { first.type(0, it) }
            first.submit(0)
        }
        first.type(1, 'c')
        first.type(1, 'r')
        first.leave(2)
        val streakAfterFirst = first.answerStreak

        // Second run: a new feed built from what the repository saved.
        val streak = Streaks.current(repo.history())
        val second = EndlessFeed(
            lists, Language.EN, Random(4), store = repo,
            restored = repo.unfinishedRounds(), startStreak = streak, now = { 6_000L },
        )
        val rounds = second.snapshot().rounds

        assertEquals(streakAfterFirst, second.answerStreak)
        assertEquals(1, repo.history().size)
        assertEquals(2, rounds.size) // the finished puzzle is history, the other two come back
        assertEquals("CR", rounds[0].input)
        assertTrue(rounds[1].skipped)
        assertEquals(1, second.skipsLeft) // a new session has its skip again
    }

    @Test
    fun wipingRemovesEverythingAboutThePlayerButKeepsTheLanguage() {
        val repository = newRepository()
        repository.finish(finished("r1"))
        repository.finish(finished("r2", at = 2_000L))
        repository.setFavorite("r1", true, now = 5L)
        repository.saveProgress(stored("p1", position = 0, input = "KI"))
        repository.saveLanguage(Language.TR)
        repository.setSyncedUserId("user-1")
        repository.cacheText("social:user-1:friends", "{}")
        repository.enqueueEverything()
        assertTrue(repository.pendingCount() > 0)

        repository.wipeLocalData()

        assertTrue(repository.history().isEmpty())
        assertTrue(repository.favoriteResults().isEmpty())
        assertTrue(repository.unfinishedRounds().isEmpty())
        assertEquals(0, repository.pendingCount())
        assertNull(repository.syncedUserId())
        assertNull(repository.cachedText("social:user-1:friends"))
        assertEquals(Language.TR, repository.savedLanguage(), "the chosen puzzle language is not about the account")
        // And it keeps working afterwards.
        repository.finish(finished("r3"))
        assertEquals(1, repository.history().size)
    }
}
