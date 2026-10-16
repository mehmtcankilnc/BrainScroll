package com.mehmtcan.brainscroll.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.cloud.toFinishedRoundOrNull
import com.mehmtcan.brainscroll.cloud.toRow
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.crossword.CrosswordSnapshot
import com.mehmtcan.brainscroll.game.crossword.CrosswordTestSolver
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Crossword results and unfinished boards go through the real database and the cloud row format. */
class CrosswordPersistenceTest {

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

    private fun feed(repository: GameRepository, language: Language, restored: Boolean = false) = EndlessFeed(
        wordLists = lists,
        startLanguage = language,
        random = Random(4),
        store = repository,
        restored = if (restored) repository.unfinishedRounds() else emptyList(),
        now = { clock },
        games = listOf(GameKind.Crossword),
    )

    private fun EndlessFeed.board() = assertIs<CrosswordSnapshot>(snapshot().pages.first())

    private fun EndlessFeed.letters() = board().let { CrosswordTestSolver.solve(it.language, it.state.isClue, it.state.clues)!! }

    @Test
    fun aBoardInProgressComesBackAfterARestartInTheRealDatabaseInItsLanguage() {
        for (language in Language.entries) {
            val repository = newRepository()
            val first = feed(repository, language)
            first.ensureSize(1)
            val letters = first.letters()
            val cells = letters.indices.filter { letters[it] != ' ' }
            first.placeLetter(0, cells[0], letters[cells[0]])
            clock += 2_000
            first.placeLetter(0, cells[1], language.alphabet.first { it != letters[cells[1]] })
            first.eraseLetter(0, cells[1])

            val second = feed(repository, language, restored = true)
            assertEquals(language, second.board().language)
            assertEquals(first.board().state, second.board().state)
            assertEquals(first.board().durationMs, second.board().durationMs)
        }
    }

    @Test
    fun aFinishedBoardIsAHistoryRowQueuedForTheCloudWithinTheServersLimits() {
        val repository = newRepository()
        val f = feed(repository, Language.TR)
        f.ensureSize(1)
        val letters = f.letters()
        for (cell in letters.indices) if (letters[cell] != ' ') f.placeLetter(0, cell, letters[cell])
        assertTrue(f.board().isFinished)

        val result = repository.history().single()
        assertEquals(GameKind.Crossword, result.game)
        assertEquals(Language.TR, result.language)
        assertEquals(1, repository.pendingCount())

        val row = result.toRow()
        assertEquals("crossword", row.game)
        assertTrue(row.answer.length in 1..32, "the table limits answer to 32 characters: ${row.answer.length}")
        assertTrue(row.guesses.length <= 256)
        val back = assertNotNull(row.toFinishedRoundOrNull())
        assertEquals(result.copy(puzzleDay = back.puzzleDay), back)
    }
}
