package com.mehmtcan.brainscroll.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
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
            ":memory:" -> JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { BrainScrollDatabase.Schema.create(it) }
            else -> openFile(File(override ?: File(System.getProperty("user.home"), ".brainscroll/$DATABASE_FILE").path))
        }
    }
}

private fun openFile(file: File): JdbcSqliteDriver {
    file.absoluteFile.parentFile.mkdirs()
    val isNew = !file.exists() || file.length() == 0L
    return JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").also { driver ->
        if (isNew) BrainScrollDatabase.Schema.create(driver)
    }
}
