package com.mehmtcan.brainscroll.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.cloud.toFinishedRoundOrNull
import com.mehmtcan.brainscroll.cloud.toRow
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.memory.MemorySnapshot
import com.mehmtcan.brainscroll.game.memory.MemoryStatus
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Memory match results and unfinished boards go through the real database and the cloud row format. */
class MemoryPersistenceTest {

    private val words = listOf("crane", "slate", "ghost", "pound", "apple", "brick")
    private val lists = mapOf(
        Language.EN to WordList(Language.EN, 5, valid = words, answers = words),
        Language.TR to WordList(Language.TR, 5, valid = listOf("kitap", "kırık", "çiçek"), answers = listOf("kitap", "kırık", "çiçek")),
    )
    private var clock = 5_000L

    private fun newRepository(): GameRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BrainScrollDatabase.Schema.create(driver)
        return GameRepository(BrainScrollDatabase(driver))
    }

    private fun feed(repository: GameRepository, restored: Boolean = false) = EndlessFeed(
        wordLists = lists,
        startLanguage = Language.EN,
        random = Random(4),
        store = repository,
        restored = if (restored) repository.unfinishedRounds() else emptyList(),
        now = { clock },
        games = listOf(GameKind.Word, GameKind.Memory),
    )

    private fun memoryResult(id: String = "m1") = FinishedRound(
        id = id, mode = Mode.ENDLESS, language = Language.EN, answer = "0,1,2,0,1,2",
        guesses = listOf("0", "3", "1", "4", "2", "5"), outcome = Outcome.WON,
        finishedAt = 9_000, durationMs = 12_345, game = GameKind.Memory,
    )

    @Test
    fun aMemoryResultKeepsItsGameThroughTheDatabase() {
        val repository = newRepository()
        repository.finish(memoryResult())
        val back = repository.history().single()
        assertEquals(GameKind.Memory, back.game)
        assertEquals(memoryResult().guesses, back.guesses)
        assertEquals(12_345L, back.durationMs)
    }

    @Test
    fun aMemoryResultCanBeAFavoriteAndComesBackWithItsGame() {
        val repository = newRepository()
        repository.finish(memoryResult())
        repository.setFavorite("m1", true, now = 1L)
        assertEquals(GameKind.Memory, repository.favoriteResults().single().game)
    }

    @Test
    fun aFinishedBoardIsQueuedForTheCloudLikeAnyEndlessResult() {
        val repository = newRepository()
        repository.finish(memoryResult())
        assertEquals(1, repository.pendingCount())
        assertEquals("m1", repository.pendingItems(10).single().key)
        assertEquals(GameKind.Memory, repository.resultsByIds(listOf("m1")).single().game)
    }

    @Test
    fun aCloudRowOfAGameThisVersionDoesNotKnowIsSkipped() {
        val unknown = memoryResult("chess1").toRow().copy(game = "chess").toFinishedRoundOrNull()
        assertNull(unknown, "a game from a newer version of the app is skipped, not fatal")
    }

    @Test
    fun theCloudRowOfAMemoryResultRoundTripsAndFitsTheServersLimits() {
        val result = memoryResult()
        val row = result.toRow()
        assertEquals("memory", row.game)
        assertTrue(row.answer.length in 1..32, "the table limits answer to 32 characters: ${row.answer.length}")
        assertTrue(row.guesses.length <= 256, "and guesses to 256 characters")
        val back = assertNotNull(row.toFinishedRoundOrNull())
        assertEquals(result.copy(puzzleDay = back.puzzleDay), back)
    }

    @Test
    fun theLongestPossibleGameStillFitsTheServersLimits() {
        // Six pairs and five allowed mistakes: at most 6 + 5 moves, which is 22 flips.
        val result = memoryResult().copy(answer = List(12) { "7" }.joinToString(","), guesses = List(22) { (it % 12).toString() })
        val row = result.toRow()
        assertTrue(row.answer.length <= 32)
        assertTrue(row.guesses.length <= 256, "22 flips take ${row.guesses.length} characters")
    }

    @Test
    fun aMemoryBoardInProgressComesBackAfterARestartInTheRealDatabase() {
        val repository = newRepository()
        val first = feed(repository)
        first.ensureSize(2)
        first.flip(1, 4)
        clock += 3_000
        first.flip(1, 7)
        val layoutBefore = (first.snapshot().pages[1] as MemorySnapshot).state.layout

        val second = feed(repository, restored = true)
        val page = assertIs<MemorySnapshot>(second.snapshot().pages.single())
        assertEquals(layoutBefore, page.state.layout, "the same board")
        assertEquals(3_000L, page.durationMs)
        assertEquals(1, page.state.moves)
        assertEquals(MemoryStatus.Playing, page.state.status)
    }

    @Test
    fun finishingABoardRemovesItFromProgressAndAddsAResult() {
        val repository = newRepository()
        val f = feed(repository)
        f.ensureSize(2)
        val layout = (f.snapshot().pages[1] as MemorySnapshot).state.layout
        layout.indices.groupBy { layout[it] }.values.forEach { f.flip(1, it[0]); f.flip(1, it[1]) }

        assertTrue(repository.unfinishedRounds().isEmpty())
        val result = repository.history().single()
        assertEquals(GameKind.Memory, result.game)
        assertEquals(Outcome.WON, result.outcome)
        assertEquals(12, result.guesses.size)
    }
}
