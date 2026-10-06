package com.mehmtcan.brainscroll.telemetry

import androidx.compose.runtime.Composable

internal actual fun installPlatformCrashHandler(onError: (Throwable) -> Unit) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        onError(error)
        previous?.uncaughtException(thread, error) // the usual crash behavior goes on
    }
}

@Composable
actual fun rememberPlatformInfo(): PlatformInfo = PlatformInfo(platform = "desktop", appVersion = "dev")
