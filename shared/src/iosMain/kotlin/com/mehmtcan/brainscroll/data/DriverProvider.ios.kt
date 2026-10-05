package com.mehmtcan.brainscroll.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.mehmtcan.brainscroll.db.BrainScrollDatabase

@Composable
actual fun rememberDriverProvider(): DriverProvider = remember {
    DriverProvider { NativeSqliteDriver(BrainScrollDatabase.Schema, DATABASE_FILE) }
}
