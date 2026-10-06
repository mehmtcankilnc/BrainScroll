package com.mehmtcan.brainscroll.telemetry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

internal actual fun installPlatformCrashHandler(onError: (Throwable) -> Unit) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        onError(error)
        previous?.uncaughtException(thread, error) // Android still shows its crash dialog and reports to Play
    }
}

@Composable
actual fun rememberPlatformInfo(): PlatformInfo {
    val context = LocalContext.current
    return remember(context) {
        val version = try {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            "${info.versionName} (${info.versionCode})"
        } catch (e: Exception) {
            "unknown"
        }
        PlatformInfo(platform = "android", appVersion = version.take(32))
    }
}
