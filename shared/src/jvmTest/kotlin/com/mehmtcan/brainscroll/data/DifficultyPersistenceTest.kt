package com.mehmtcan.brainscroll.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.cloud.ResultRow
import com.mehmtcan.brainscroll.cloud.toFinishedRoundOrNull
import com.mehmtcan.brainscroll.cloud.toRow
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** The level of a board goes through the real database, the upgrade of an older database and the cloud row. */
class DifficultyPersistenceTest {

    private fun finished(id: String, difficulty: Difficulty) = FinishedRound(
        id = id, mode = Mode.ENDLESS, language = Language.EN, answer = "9x9x36:7", guesses = listOf("2"),
        outcome = Outcome.WON, finishedAt = 5_000L, game = GameKind.Sudoku, difficulty = difficulty,
    )

    private fun newRepository(): GameRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BrainScrollDatabase.Schema.create(driver)
        return GameRepository(BrainScrollDatabase(driver))
    }

    @Test
    fun theLevelOfAFinishedResultSurvivesTheDatabase() {
        val repo = newRepository()
        repo.finish(finished("e", Difficulty.Easy))
        repo.finish(finished("m", Difficulty.Medium))
        repo.finish(finished("h", Difficulty.Hard))
        assertEquals(mapOf("e" to Difficulty.Easy, "m" to Difficulty.Medium, "h" to Difficulty.Hard), repo.history().associate { it.id to it.difficulty })
    }

    @Test
    fun theLevelOfAnUnfinishedRoundSurvivesTheDatabase() {
        val repo = newRepository()
        repo.saveProgress(StoredRound("p", Language.EN, "9x9x36:7", listOf("d1"), "", position = 0, game = GameKind.Sudoku, difficulty = Difficulty.Hard))
        assertEquals(Difficulty.Hard, repo.unfinishedRounds().single().difficulty)
        repo.saveProgress(StoredRound("p", Language.EN, "9x9x36:7", listOf("d1"), "", position = 0, game = GameKind.Sudoku))
        assertEquals(Difficulty.Medium, repo.unfinishedRounds().single().difficulty)
    }

    @Test
    fun theLevelOfAFavoriteResultIsKept() {
        val repo = newRepository()
        repo.finish(finished("h", Difficulty.Hard))
        repo.setFavorite("h", true, 6_000L)
        assertEquals(Difficulty.Hard, repo.favoriteResults().single().difficulty)
    }

    @Test
    fun theChosenLevelOfEachGameIsRemembered() {
        val repo = newRepository()
        assertEquals(Difficulty.Medium, repo.savedDifficulty(GameKind.Hanoi))
        assertEquals(emptyMap(), repo.savedDifficulties())
        repo.saveDifficulty(GameKind.Hanoi, Difficulty.Hard)
        repo.saveDifficulty(GameKind.Memory, Difficulty.Easy)
        repo.saveDifficulty(GameKind.Hanoi, Difficulty.Easy)
        assertEquals(mapOf(GameKind.Hanoi to Difficulty.Easy, GameKind.Memory to Difficulty.Easy), repo.savedDifficulties())
        assertEquals(Difficulty.Medium, repo.savedDifficulty(GameKind.Tents))
    }

    @Test
    fun aDatabaseFromBeforeLevelsKeepsItsDataAsMedium() {
        // Version 3: the first two versions plus the duration column, as the last build before levels left it.
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        listOf(
            """CREATE TABLE game_result (id TEXT NOT NULL PRIMARY KEY, game TEXT NOT NULL, mode TEXT NOT NULL, language TEXT NOT NULL,
               answer TEXT NOT NULL, guesses TEXT NOT NULL, outcome TEXT NOT NULL, was_skipped INTEGER NOT NULL,
               finished_at INTEGER NOT NULL, day_index INTEGER NOT NULL, duration_ms INTEGER)""",
            """CREATE TABLE in_progress_round (id TEXT NOT NULL PRIMARY KEY, game TEXT NOT NULL, mode TEXT NOT NULL, language TEXT NOT NULL,
               answer TEXT NOT NULL, guesses TEXT NOT NULL, input TEXT NOT NULL, was_skipped INTEGER NOT NULL, position INTEGER NOT NULL)""",
            "CREATE TABLE favorite (result_id TEXT NOT NULL PRIMARY KEY REFERENCES game_result(id), is_favorite INTEGER NOT NULL, updated_at INTEGER NOT NULL)",
            "CREATE TABLE setting (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)",
            "CREATE TABLE sync_queue (kind TEXT NOT NULL, key TEXT NOT NULL, seq INTEGER NOT NULL, PRIMARY KEY (kind, key))",
            "INSERT INTO game_result VALUES ('old','sudoku','ENDLESS','EN','9x9x36:7','2','WON',0,1000,5,NULL)",
            "INSERT INTO in_progress_round VALUES ('p1','sudoku','ENDLESS','EN','9x9x36:7','d1','',0,0)",
            "PRAGMA user_version = 3",
        ).forEach { driver.execute(null, it, 0) }

        prepareDatabase(driver)
        val repo = GameRepository(BrainScrollDatabase(driver))
        assertEquals(Difficulty.Medium, repo.history().single().difficulty)
        assertEquals(Difficulty.Medium, repo.unfinishedRounds().single().difficulty)

        repo.finish(finished("new", Difficulty.Hard))
        assertEquals(Difficulty.Hard, repo.history().first { it.id == "new" }.difficulty)
    }

    @Test
    fun theLevelGoesToTheCloudAndComesBack() {
        for (level in Difficulty.entries) {
            val row = finished("r", level).toRow()
            assertEquals(level.id, row.difficulty)
            assertEquals(level, assertNotNull(row.toFinishedRoundOrNull()).difficulty)
        }
    }

    @Test
    fun aLevelTheAppDoesNotKnowReadsAsMedium() {
        val row = finished("r", Difficulty.Hard).toRow().copy(difficulty = 7)
        assertEquals(Difficulty.Medium, assertNotNull(row.toFinishedRoundOrNull()).difficulty)
        assertEquals(Difficulty.Medium, Difficulty.fromId(null))
        assertEquals(Difficulty.Medium, Difficulty.fromId(-1))
    }

    @Test
    fun aRowWithoutALevelFromTheServerIsMedium() {
        val row = ResultRow("r", "word", "ENDLESS", "EN", "CRANE", "SLATE,CRANE", "WON", false, 1_000L, 5)
        assertEquals(Difficulty.Medium, assertNotNull(row.toFinishedRoundOrNull()).difficulty)
    }
}
