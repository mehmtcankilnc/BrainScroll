package com.mehmtcan.brainscroll.telemetry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook
import platform.Foundation.NSBundle

@OptIn(ExperimentalNativeApi::class)
internal actual fun installPlatformCrashHandler(onError: (Throwable) -> Unit) {
    // After the hook returns, Kotlin/Native ends the process the usual way.
    setUnhandledExceptionHook { error -> onError(error) }
}

@Composable
actual fun rememberPlatformInfo(): PlatformInfo = remember {
    val info = NSBundle.mainBundle.infoDictionary
    val version = info?.get("CFBundleShortVersionString") as? String ?: "unknown"
    val build = info?.get("CFBundleVersion") as? String
    PlatformInfo(platform = "ios", appVersion = (if (build != null) "$version ($build)" else version).take(32))
}
