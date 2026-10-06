package com.mehmtcan.brainscroll.telemetry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * The only events the app sends (the same list as `event_name` in `supabase/migrations/...telemetry.sql`).
 * An event is a name and a few small values. It never says who did it.
 */
enum class TelemetryEvent(val wireName: String) {
    AppOpen("app_open"),
    TabViewed("tab_viewed"),
    DailyStarted("daily_started"),
    DailyFinished("daily_finished"),
    EndlessFinished("endless_finished"),
    SignedIn("signed_in"),
    FriendAdded("friend_added"),
    AccountDeleted("account_deleted"),
}

/** One event with its small values, for example `outcome` to `WON`. */
data class EventRecord(val event: TelemetryEvent, val props: Map<String, String> = emptyMap())

/** Which platform and which app version a report comes from. The only things that describe the sender. */
data class PlatformInfo(
    /** `android`, `ios` or `desktop`. */
    val platform: String,
    val appVersion: String,
)

/** An uncaught error. The text is cut to what the server keeps (see [crashReportOf]). */
@Serializable
data class CrashReport(val kind: String, val message: String, val stack: String)

/** What the app tells about itself, and how it can be switched off. */
interface Telemetry {
    /** Whether anonymous usage counts and crash reports are sent. The player can turn this off in the profile. */
    val enabled: StateFlow<Boolean>

    fun setEnabled(enabled: Boolean)

    fun event(event: TelemetryEvent, props: Map<String, String> = emptyMap())

    /** Keeps a crash on the device until the next start, when it can be sent. Called while the app is dying. */
    fun recordCrash(crash: CrashReport)
}

/** For previews and for code that has no telemetry. */
object NoTelemetry : Telemetry {
    override val enabled: StateFlow<Boolean> = MutableStateFlow(false)
    override fun setEnabled(enabled: Boolean) = Unit
    override fun event(event: TelemetryEvent, props: Map<String, String>) = Unit
    override fun recordCrash(crash: CrashReport) = Unit
}

/** Sending to the server. Implementations throw on any failure, the caller keeps the data and tries again later. */
interface TelemetryApi {
    suspend fun sendEvents(info: PlatformInfo, events: List<EventRecord>)

    suspend fun sendCrash(info: PlatformInfo, crash: CrashReport)
}

private const val MAX_KIND = 200
private const val MAX_MESSAGE = 500
private const val MAX_STACK = 8000

/** Turns an exception into a report, cut to the same limits the server applies. */
fun crashReportOf(error: Throwable): CrashReport = CrashReport(
    kind = (error::class.qualifiedName ?: error::class.simpleName ?: "Throwable").take(MAX_KIND),
    message = (error.message ?: "").take(MAX_MESSAGE),
    stack = error.stackTraceToString().take(MAX_STACK),
)
