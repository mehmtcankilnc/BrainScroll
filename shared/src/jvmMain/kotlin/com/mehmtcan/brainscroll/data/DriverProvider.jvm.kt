package com.mehmtcan.brainscroll.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * Tests set this system property so they never touch the real database: `:memory:` is a throw-away database,
 * any other value is used as the path of the database file (to test closing and reopening the app).
 */
private const val DATABASE_PROPERTY = "brainscroll.database"

/** Desktop is for development only: by default the database lives in a hidden folder in the user's home. */
@Composable
actual fun rememberDriverProvider(): DriverProvider = remember {
    DriverProvider {
        when (val override = System.getProperty(DATABASE_PROPERTY)) {
            // A throw-away database. It is a temporary file, not SQLite's in-memory mode, because the JDBC
            // driver gives every thread its own private in-memory database, and the app uses several threads.
            ":memory:" -> openFile(File.createTempFile("brainscroll-throwaway", ".db").also { it.deleteOnExit() })
            else -> openFile(File(override ?: File(System.getProperty("user.home"), ".brainscroll/$DATABASE_FILE").path))
        }
    }
}

private fun openFile(file: File): JdbcSqliteDriver {
    file.absoluteFile.parentFile.mkdirs()
    return JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { prepareDatabase(it) }
}
