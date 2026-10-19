package com.mehmtcan.brainscroll.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.cloud.toFinishedRoundOrNull
import com.mehmtcan.brainscroll.cloud.toRow
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.nonogram.NonogramSnapshot
import com.mehmtcan.brainscroll.game.nonogram.NonogramSolver
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Nonogram results and unfinished boards go through the real database and the cloud row format. */
class NonogramPersistenceTest {

    private val words = listOf("crane", "slate", "ghost")
    private val lists = mapOf(
        Language.EN to WordList(Language.EN, 5, valid = words, answers = words),
        Language.TR to WordList(Language.TR, 5, valid = listOf("kitap"), answers = listOf("kitap")),
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
        games = listOf(GameKind.Nonogram),
    )

    private fun EndlessFeed.board() = assertIs<NonogramSnapshot>(snapshot().pages.first())

    /** The cells of the picture, worked out from the numbers on the page. */
    private fun EndlessFeed.answer(): List<Int> {
        val state = board().state
        return NonogramSolver.deduce(state.size, state.rowClues, state.columnClues).first.filter { it.second == NonogramSolver.FILLED }.map { it.first }.sorted()
    }

    @Test
    fun aBoardInProgressComesBackAfterARestartInTheRealDatabase() {
        val repository = newRepository()
        val first = feed(repository)
        first.ensureSize(1)
        val cells = first.answer()
        first.paintNonogram(0, cells[0], cells[0], 1)
        first.paintNonogram(0, cells[1], cells[1], 2)
        clock += 2_000
        first.restartNonogram(0)
        first.paintNonogram(0, cells[0], cells[0], 1)

        val second = feed(repository, restored = true)
        assertEquals(first.board().state, second.board().state)
        assertEquals(first.board().durationMs, second.board().durationMs)
    }

    @Test
    fun aFinishedBoardIsAHistoryRowQueuedForTheCloudWithinTheServersLimits() {
        val repository = newRepository()
        val f = feed(repository)
        f.ensureSize(1)
        for (cell in f.answer()) f.paintNonogram(0, cell, cell, 1)
        assertTrue(f.board().isFinished)

        val result = repository.history().single()
        assertEquals(GameKind.Nonogram, result.game)
        assertEquals(1, repository.pendingCount())

        val row = result.toRow()
        assertEquals("nonogram", row.game)
        assertTrue(row.answer.length in 1..32, "the table limits answer to 32 characters: ${row.answer.length}")
        assertTrue(row.guesses.length <= 256)
        val back = assertNotNull(row.toFinishedRoundOrNull())
        assertEquals(result.copy(puzzleDay = back.puzzleDay), back)
    }
}
