package com.mehmtcan.brainscroll.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phones that already have the app hold a version 1 database. These tests build exactly that and check that
 * updating the app keeps the player's data and adds what version 2 needs.
 */
class DatabaseMigrationTest {

    /** The tables exactly as phase 4 created them (version 1). Never change this text: it is the past. */
    private val versionOneStatements = listOf(
        """CREATE TABLE game_result (
            id TEXT NOT NULL PRIMARY KEY, game TEXT NOT NULL, mode TEXT NOT NULL, language TEXT NOT NULL,
            answer TEXT NOT NULL, guesses TEXT NOT NULL, outcome TEXT NOT NULL, was_skipped INTEGER NOT NULL,
            finished_at INTEGER NOT NULL, day_index INTEGER NOT NULL)""",
        """CREATE TABLE in_progress_round (
            id TEXT NOT NULL PRIMARY KEY, game TEXT NOT NULL, mode TEXT NOT NULL, language TEXT NOT NULL,
            answer TEXT NOT NULL, guesses TEXT NOT NULL, input TEXT NOT NULL, was_skipped INTEGER NOT NULL,
            position INTEGER NOT NULL)""",
        """CREATE TABLE favorite (
            result_id TEXT NOT NULL PRIMARY KEY REFERENCES game_result(id),
            is_favorite INTEGER NOT NULL, updated_at INTEGER NOT NULL)""",
        """CREATE TABLE setting (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)""",
    )

    private fun versionOneDatabase(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        versionOneStatements.forEach { driver.execute(null, it, 0) }
        driver.execute(
            null,
            "INSERT INTO game_result VALUES ('r1','word','ENDLESS','EN','CRANE','SLATE,CRANE','WON',0,1000,5)",
            0,
        )
        driver.execute(null, "INSERT INTO game_result VALUES ('r2','word','ENDLESS','TR','KİTAP','KIRIK','LOST',1,2000,5)", 0)
        driver.execute(null, "INSERT INTO favorite VALUES ('r1', 1, 1500)", 0)
        driver.execute(null, "INSERT INTO setting VALUES ('word_language', 'TR')", 0)
        driver.execute(null, "INSERT INTO in_progress_round VALUES ('p1','word','ENDLESS','EN','APPLE','','AP',0,0)", 0)
        return driver
    }

    private fun userVersion(driver: SqlDriver): Long = driver.executeQuery(
        null, "PRAGMA user_version",
        { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L) }, 0,
    ).value

    @Test
    fun aVersionOneDatabaseKeepsAllItsData() {
        val driver = versionOneDatabase()
        prepareDatabase(driver)
        val repo = GameRepository(BrainScrollDatabase(driver))

        assertEquals(listOf("r1", "r2"), repo.history().map { it.id })
        assertEquals(true, repo.isFavorite("r1"))
        assertEquals(com.mehmtcan.brainscroll.game.wordle.Language.TR, repo.savedLanguage())
        assertEquals(listOf("p1"), repo.unfinishedRounds().map { it.id })
    }

    @Test
    fun theUpgradedDatabaseHasTheUploadQueueAndItWorks() {
        val driver = versionOneDatabase()
        prepareDatabase(driver)
        val repo = GameRepository(BrainScrollDatabase(driver))

        assertEquals(0, repo.pendingCount())
        // The history that existed before the upgrade can be offered to the cloud.
        repo.enqueueEverything()
        assertEquals(3, repo.pendingCount()) // 2 results + 1 favorite
    }

    @Test
    fun theVersionNumberIsRecordedAfterTheUpgrade() {
        val driver = versionOneDatabase()
        prepareDatabase(driver)
        assertEquals(BrainScrollDatabase.Schema.version, userVersion(driver))
        assertTrue(BrainScrollDatabase.Schema.version >= 2)
    }

    @Test
    fun aBrandNewDatabaseGetsTheCurrentSchema() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        prepareDatabase(driver)
        assertEquals(BrainScrollDatabase.Schema.version, userVersion(driver))
        val repo = GameRepository(BrainScrollDatabase(driver))
        assertEquals(0, repo.pendingCount())
    }

    @Test
    fun preparingAnUpToDateDatabaseAgainChangesNothing() {
        val driver = versionOneDatabase()
        prepareDatabase(driver)
        prepareDatabase(driver) // a second start of the app
        val repo = GameRepository(BrainScrollDatabase(driver))
        assertEquals(2, repo.history().size)
    }
}
