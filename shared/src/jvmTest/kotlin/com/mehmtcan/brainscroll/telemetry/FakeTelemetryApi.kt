package com.mehmtcan.brainscroll.telemetry

/** A stand-in for the server's two logging functions. Tests read what was "sent" and can make sending fail. */
class FakeTelemetryApi : TelemetryApi {
    val events = mutableListOf<EventRecord>()
    val batchSizes = mutableListOf<Int>()
    val crashes = mutableListOf<CrashReport>()
    val infos = mutableListOf<PlatformInfo>()

    /** Thrown by every call while set, to imitate being offline. */
    var failure: Exception? = null

    override suspend fun sendEvents(info: PlatformInfo, events: List<EventRecord>) {
        failure?.let { throw it }
        infos += info
        batchSizes += events.size
        this.events += events
    }

    override suspend fun sendCrash(info: PlatformInfo, crash: CrashReport) {
        failure?.let { throw it }
        infos += info
        crashes += crash
    }

    fun names() = events.map { it.event }
}
