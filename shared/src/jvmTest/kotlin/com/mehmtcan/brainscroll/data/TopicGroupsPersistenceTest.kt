package com.mehmtcan.brainscroll.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.cloud.toFinishedRoundOrNull
import com.mehmtcan.brainscroll.cloud.toRow
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsSnapshot
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Word groups results and unfinished boards go through the real database and the cloud row format. */
class TopicGroupsPersistenceTest {

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

    private fun feed(repository: GameRepository, restored: Boolean = false, language: Language = Language.TR) = EndlessFeed(
        wordLists = lists,
        startLanguage = language,
        random = Random(4),
        store = repository,
        restored = if (restored) repository.unfinishedRounds() else emptyList(),
        now = { clock },
        games = listOf(GameKind.TopicGroups),
    )

    private fun EndlessFeed.board() = assertIs<TopicGroupsSnapshot>(snapshot().pages.first())

    @Test
    fun aBoardInProgressComesBackAfterARestartInTheRealDatabase() {
        val repository = newRepository()
        val first = feed(repository)
        first.ensureSize(1)
        for (w in listOf(0, 1, 2, 4)) first.tapTopicWord(0, w)
        first.tapTopicWord(0, 4)
        clock += 2_000
        first.shuffleTopicWords(0)
        first.tapTopicWord(0, 3)
        first.tapTopicWord(0, 8)

        val second = feed(repository, restored = true, language = Language.EN)
        assertEquals(first.board().state, second.board().state)
        assertEquals(first.board().durationMs, second.board().durationMs)
        // The board keeps the language it was made in, whatever the feed starts with now.
        assertEquals(Language.TR, second.board().language)
        assertEquals(1, second.board().state.mistakes)
    }

    @Test
    fun aFinishedBoardIsAHistoryRowQueuedForTheCloudWithinTheServersLimits() {
        val repository = newRepository()
        val f = feed(repository)
        f.ensureSize(1)
        for (g in 0 until 3) for (w in g * 4 until g * 4 + 4) f.tapTopicWord(0, w)
        assertTrue(f.board().isFinished)

        val result = repository.history().single()
        assertEquals(GameKind.TopicGroups, result.game)
        assertEquals(1, repository.pendingCount())

        val row = result.toRow()
        assertEquals("topicgroups", row.game)
        assertTrue(row.answer.length in 1..32, "the table limits answer to 32 characters: ${row.answer.length}")
        assertTrue(row.guesses.length <= 256)
        val back = assertNotNull(row.toFinishedRoundOrNull())
        assertEquals(result.copy(puzzleDay = back.puzzleDay), back)
    }
}
