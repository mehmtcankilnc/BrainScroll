package com.mehmtcan.brainscroll.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.cloud.toFinishedRoundOrNull
import com.mehmtcan.brainscroll.cloud.toRow
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.sudoku.SudokuGenerator
import com.mehmtcan.brainscroll.game.sudoku.SudokuLayout
import com.mehmtcan.brainscroll.game.sudoku.SudokuSnapshot
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Jigsaw results and unfinished boards go through the real database and the cloud row format. */
class JigsawPersistenceTest {

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
        games = listOf(GameKind.Jigsaw),
    )

    private fun EndlessFeed.board() = assertIs<SudokuSnapshot>(snapshot().pages.first())

    @Test
    fun aBoardInProgressComesBackAfterARestartInTheRealDatabase() {
        val repository = newRepository()
        val first = feed(repository)
        first.ensureSize(1)
        val cell = first.board().state.let { s -> s.values.indices.first { s.values[it] == 0 } }
        first.toggleNote(0, cell, 2)
        clock += 2_000
        first.placeDigit(0, cell, 9)

        val second = feed(repository, restored = true)
        assertEquals(GameKind.Jigsaw, second.board().kind)
        assertEquals(first.board().state, second.board().state)
        assertEquals(first.board().durationMs, second.board().durationMs)
    }

    @Test
    fun aFinishedBoardIsAHistoryRowQueuedForTheCloudWithinTheServersLimits() {
        val repository = newRepository()
        val f = feed(repository)
        f.ensureSize(1)
        val state = f.board().state
        val solution = SudokuGenerator.solveBySingles(state.givens.toIntArray(), SudokuLayout.of(state.regions))!!
        for (cell in state.values.indices) if (state.values[cell] == 0) f.placeDigit(0, cell, solution[cell])
        assertTrue(f.board().isFinished)

        val result = repository.history().single()
        assertEquals(GameKind.Jigsaw, result.game)
        assertEquals(1, repository.pendingCount())

        val row = result.toRow()
        assertEquals("jigsaw", row.game)
        assertTrue(row.answer.length in 1..32, "the table limits answer to 32 characters: ${row.answer.length}")
        assertTrue(row.guesses.length <= 256)
        val back = assertNotNull(row.toFinishedRoundOrNull())
        assertEquals(result.copy(puzzleDay = back.puzzleDay), back)
    }
}
