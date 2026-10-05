package com.mehmtcan.brainscroll.data

import androidx.compose.runtime.Composable
import app.cash.sqldelight.db.SqlDriver

/** Creates the platform's SQLite driver. The database code itself is shared; only this part differs per platform. */
fun interface DriverProvider {
    fun create(): SqlDriver
}

/**
 * Android needs a `Context` to open a database, which common code does not have. A composable can get it from
 * the composition (`LocalContext`), so the entry points (MainActivity, iOS view controller) need no changes.
 */
@Composable
expect fun rememberDriverProvider(): DriverProvider
