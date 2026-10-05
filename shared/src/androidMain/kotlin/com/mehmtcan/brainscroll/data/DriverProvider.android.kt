package com.mehmtcan.brainscroll.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.mehmtcan.brainscroll.db.BrainScrollDatabase

@Composable
actual fun rememberDriverProvider(): DriverProvider {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        DriverProvider { AndroidSqliteDriver(BrainScrollDatabase.Schema, context, DATABASE_FILE) }
    }
}
