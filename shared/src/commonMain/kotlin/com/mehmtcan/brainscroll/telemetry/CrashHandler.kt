package com.mehmtcan.brainscroll.telemetry

import androidx.compose.runtime.Composable

/**
 * Catches errors nobody caught. The platform hook is installed once for the whole process; [install] only says
 * who receives the reports, so installing again (a new screen, a new test) does not stack handlers.
 *
 * Only errors in our own (Kotlin) code arrive here. A crash inside the system or in native code is reported by
 * App Store Connect (TestFlight and the App Store) and by Google Play Console.
 */
object CrashHandler {
    private var sink: ((CrashReport) -> Unit)? = null
    private var installed = false

    fun install(onCrash: (CrashReport) -> Unit) {
        sink = onCrash
        if (!installed) {
            installed = true
            installPlatformCrashHandler { error ->
                // Whatever happens in here must never hide the original crash.
                try {
                    sink?.invoke(crashReportOf(error))
                } catch (ignored: Throwable) {
                }
            }
        }
    }
}

/** Sets the platform's "uncaught error" hook. [onError] is called on the thread that died, then the app ends as usual. */
internal expect fun installPlatformCrashHandler(onError: (Throwable) -> Unit)

/** The app's platform and version, for the reports. */
@Composable
expect fun rememberPlatformInfo(): PlatformInfo
