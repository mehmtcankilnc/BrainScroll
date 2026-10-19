package com.mehmtcan.brainscroll.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.cloud.toFinishedRoundOrNull
import com.mehmtcan.brainscroll.cloud.toRow
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.arrows.ArrowsRules
import com.mehmtcan.brainscroll.game.arrows.ArrowsSnapshot
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Arrow escape results and unfinished boards go through the real database and the cloud row format. */
class ArrowsPersistenceTest {

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
        games = listOf(GameKind.Arrows),
    )

    private fun EndlessFeed.board() = assertIs<ArrowsSnapshot>(snapshot().pages.first())

    private fun EndlessFeed.order(): List<Int> {
        val state = board().state
        val arrows = state.arrows
        val alive = BooleanArray(arrows.size) { arrows[it].alive }
        val owner = IntArray(state.size * state.size) { -1 }
        arrows.forEachIndexed { i, a -> a.cells.forEach { owner[it] = i } }
        val order = ArrayList<Int>()
        while (alive.any { it }) {
            val next = alive.indices.first { i ->
                alive[i] && run {
                    val a = arrows[i]
                    var r = a.head / state.size + com.mehmtcan.brainscroll.game.arrows.ArrowDir.dRow[a.dir]
                    var c = a.head % state.size + com.mehmtcan.brainscroll.game.arrows.ArrowDir.dColumn[a.dir]
                    var clear = true
                    while (r in 0 until state.size && c in 0 until state.size) {
                        val o = owner[r * state.size + c]
                        if (o >= 0 && alive[o]) clear = false
                        r += com.mehmtcan.brainscroll.game.arrows.ArrowDir.dRow[a.dir]
                        c += com.mehmtcan.brainscroll.game.arrows.ArrowDir.dColumn[a.dir]
                    }
                    clear
                }
            }
            alive[next] = false
            order += arrows[next].head
        }
        return order
    }

    @Test
    fun aBoardInProgressComesBackAfterARestartInTheRealDatabase() {
        val repository = newRepository()
        val first = feed(repository)
        first.ensureSize(1)
        val cells = first.order()
        first.tapArrow(0, cells[0])
        first.tapArrow(0, cells[1])
        clock += 2_000
        first.restartArrows(0)
        first.tapArrow(0, cells[0])

        val second = feed(repository, restored = true)
        assertEquals(first.board().state, second.board().state)
        assertEquals(first.board().durationMs, second.board().durationMs)
    }

    @Test
    fun aFinishedBoardIsAHistoryRowQueuedForTheCloudWithinTheServersLimits() {
        val repository = newRepository()
        val f = feed(repository)
        f.ensureSize(1)
        for (cell in f.order()) f.tapArrow(0, cell)
        assertTrue(f.board().isFinished)

        val result = repository.history().single()
        assertEquals(GameKind.Arrows, result.game)
        assertEquals(1, repository.pendingCount())

        val row = result.toRow()
        assertEquals("arrows", row.game)
        assertTrue(row.answer.length in 1..32, "the table limits answer to 32 characters: ${row.answer.length}")
        assertTrue(row.guesses.length <= 256)
        val back = assertNotNull(row.toFinishedRoundOrNull())
        assertEquals(result.copy(puzzleDay = back.puzzleDay), back)
    }
}
