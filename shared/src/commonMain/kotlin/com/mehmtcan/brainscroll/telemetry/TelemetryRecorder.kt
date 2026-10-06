package com.mehmtcan.brainscroll.telemetry

import com.mehmtcan.brainscroll.data.GameRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Collects events in memory and sends them in small batches once there is an account session, which every install
 * has (an anonymous one counts). Offline, events wait and go with the next batch; if the app is closed first they
 * are lost, which is fine for counts. A crash is different: it is written to the device the moment it happens and
 * sent at the next start.
 *
 * Everything here runs on the main thread, except [recordCrash], which may run on whichever thread died.
 */
class TelemetryRecorder(
    private val repository: GameRepository,
    private val api: TelemetryApi,
    private val info: PlatformInfo,
    private val scope: CoroutineScope,
    private val signedIn: StateFlow<Boolean>,
    private val flushDelayMillis: Long = 3_000,
) : Telemetry {

    private val _enabled = MutableStateFlow(repository.telemetryEnabled())
    override val enabled: StateFlow<Boolean> = _enabled

    private val buffer = ArrayDeque<EventRecord>()
    private var flushJob: Job? = null

    /** Starts watching the account: whenever there is a session, a waiting crash and waiting events are sent. */
    fun start() {
        scope.launch {
            signedIn.collect { ready ->
                if (ready) {
                    sendPendingCrash()
                    flush()
                }
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        repository.setTelemetryEnabled(enabled)
        if (!enabled) {
            // Turning it off also forgets what was waiting to be sent.
            buffer.clear()
            flushJob?.cancel()
            repository.clearPendingCrash()
        }
    }

    override fun event(event: TelemetryEvent, props: Map<String, String>) {
        if (!_enabled.value) return
        if (buffer.size >= MAX_BUFFER) buffer.removeFirst() // a phone that stays offline for long must not grow without end
        buffer.addLast(EventRecord(event, props))
        if (signedIn.value && flushJob?.isActive != true) {
            flushJob = scope.launch {
                delay(flushDelayMillis) // a short wait so a burst of events goes out as one request
                flush()
            }
        }
    }

    override fun recordCrash(crash: CrashReport) {
        if (!_enabled.value) return
        repository.putPendingCrash(Json.encodeToString(CrashReport.serializer(), crash))
    }

    private suspend fun flush() {
        while (_enabled.value && signedIn.value && buffer.isNotEmpty()) {
            val batch = buffer.take(BATCH_SIZE)
            try {
                api.sendEvents(info, batch)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return // offline or refused: the events stay, the next trigger tries again
            }
            repeat(batch.size) { buffer.removeFirst() }
        }
    }

    private suspend fun sendPendingCrash() {
        val stored = repository.pendingCrash() ?: return
        if (!_enabled.value) {
            repository.clearPendingCrash()
            return
        }
        val crash = try {
            Json.decodeFromString(CrashReport.serializer(), stored)
        } catch (e: Exception) {
            repository.clearPendingCrash() // unreadable: nothing to send, nothing worth keeping
            return
        }
        try {
            api.sendCrash(info, crash)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return // kept for the next start
        }
        repository.clearPendingCrash()
    }

    private companion object {
        const val BATCH_SIZE = 50
        const val MAX_BUFFER = 200
    }
}
