package com.mehmtcan.brainscroll.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.cloud.toFinishedRoundOrNull
import com.mehmtcan.brainscroll.cloud.toRow
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.blockout.BlockOutSnapshot
import com.mehmtcan.brainscroll.game.blockout.BlockOutSolver
import com.mehmtcan.brainscroll.game.blockout.PieceShape
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Block out results and unfinished boards go through the real database and the cloud row format. */
class BlockOutPersistenceTest {

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
        games = listOf(GameKind.BlockOut),
    )

    private fun EndlessFeed.board() = assertIs<BlockOutSnapshot>(snapshot().pages.first())

    private fun EndlessFeed.way() = board().state.let { state ->
        val shapes = state.pieces.map { PieceShape(it.horizontal, it.line, it.length, it.isTarget) }
        BlockOutSolver.shortest(state.size, shapes, IntArray(shapes.size) { state.pieces[it].pos })!!
    }

    @Test
    fun aBoardInProgressComesBackAfterARestartInTheRealDatabase() {
        val repository = newRepository()
        val first = feed(repository)
        first.ensureSize(1)
        val way = first.way()
        first.slideBlock(0, way[0].piece, way[0].to)
        clock += 2_000
        first.slideBlock(0, way[1].piece, way[1].to)
        first.restartBlockOut(0)
        first.slideBlock(0, way[0].piece, way[0].to)

        val second = feed(repository, restored = true)
        assertEquals(first.board().state, second.board().state)
        assertEquals(first.board().durationMs, second.board().durationMs)
    }

    @Test
    fun aFinishedBoardIsAHistoryRowQueuedForTheCloudWithinTheServersLimits() {
        val repository = newRepository()
        val f = feed(repository)
        f.ensureSize(1)
        for (move in f.way()) f.slideBlock(0, move.piece, move.to)
        assertTrue(f.board().isFinished)

        val result = repository.history().single()
        assertEquals(GameKind.BlockOut, result.game)
        assertEquals(1, repository.pendingCount())

        val row = result.toRow()
        assertEquals("blockout", row.game)
        assertTrue(row.answer.length in 1..32, "the table limits answer to 32 characters: ${row.answer.length}")
        assertTrue(row.guesses.length <= 256)
        val back = assertNotNull(row.toFinishedRoundOrNull())
        assertEquals(result.copy(puzzleDay = back.puzzleDay), back)
    }
}
