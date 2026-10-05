package com.mehmtcan.brainscroll.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.mehmtcan.brainscroll.db.BrainScrollDatabase

/**
 * Creates the tables of a new database or migrates an old one to the current version.
 *
 * Android and iOS drivers do this by themselves. The desktop JDBC driver does not, so the version is kept in
 * SQLite's `user_version` field. A database from before versions were tracked has `user_version` 0 but already
 * has the tables of version 1.
 */
internal fun prepareDatabase(driver: SqlDriver) {
    val schema = BrainScrollDatabase.Schema
    var version = readUserVersion(driver)

    if (version == 0L) {
        if (!hasTable(driver, "game_result")) {
            schema.create(driver)
            writeUserVersion(driver, schema.version)
            return
        }
        version = 1L
    }
    if (version < schema.version) schema.migrate(driver, version, schema.version)
    writeUserVersion(driver, schema.version)
}

private fun readUserVersion(driver: SqlDriver): Long = driver.executeQuery(
    identifier = null,
    sql = "PRAGMA user_version",
    mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L) },
    parameters = 0,
).value

private fun hasTable(driver: SqlDriver, name: String): Boolean = driver.executeQuery(
    identifier = null,
    sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$name'",
    mapper = { cursor -> QueryResult.Value(cursor.next().value) },
    parameters = 0,
).value

private fun writeUserVersion(driver: SqlDriver, version: Long) {
    driver.execute(identifier = null, sql = "PRAGMA user_version = $version", parameters = 0)
}
