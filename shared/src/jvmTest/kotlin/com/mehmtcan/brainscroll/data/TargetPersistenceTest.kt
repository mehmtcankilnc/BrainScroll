package com.mehmtcan.brainscroll.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.cloud.toFinishedRoundOrNull
import com.mehmtcan.brainscroll.cloud.toRow
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.target.Op
import com.mehmtcan.brainscroll.game.target.TargetSnapshot
import com.mehmtcan.brainscroll.game.target.TargetSolver
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Target number results and unfinished games go through the real database and the cloud row format. */
class TargetPersistenceTest {

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
        games = listOf(GameKind.Target),
    )

    private fun EndlessFeed.game() = assertIs<TargetSnapshot>(snapshot().pages.first())

    @Test
    fun aGameInProgressComesBackAfterARestartInTheRealDatabase() {
        val repository = newRepository()
        val first = feed(repository)
        first.ensureSize(1)
        first.combineNumbers(0, 0, Op.Add, 1)
        first.combineNumbers(0, 2, Op.Mul, 3)
        clock += 2_000
        first.undoNumbers(0)
        first.restartNumbers(0)
        first.combineNumbers(0, 1, Op.Add, 2)

        val second = feed(repository, restored = true)
        assertEquals(first.game().state, second.game().state)
        assertEquals(first.game().durationMs, second.game().durationMs)
    }

    @Test
    fun aFinishedGameIsAHistoryRowQueuedForTheCloudWithinTheServersLimits() {
        val repository = newRepository()
        val f = feed(repository)
        f.ensureSize(1)
        val state = f.game().state
        for (move in TargetSolver.solve(state.tokens.map { it.value }, state.target)!!) f.combineNumbers(0, move.first, move.op, move.second)
        assertTrue(f.game().isFinished)

        val result = repository.history().single()
        assertEquals(GameKind.Target, result.game)
        assertEquals(1, repository.pendingCount())

        val row = result.toRow()
        assertEquals("target", row.game)
        assertTrue(row.answer.length in 1..32, "the table limits answer to 32 characters: ${row.answer.length}")
        assertTrue(row.guesses.length <= 256)
        val back = assertNotNull(row.toFinishedRoundOrNull())
        assertEquals(result.copy(puzzleDay = back.puzzleDay), back)
    }
}
