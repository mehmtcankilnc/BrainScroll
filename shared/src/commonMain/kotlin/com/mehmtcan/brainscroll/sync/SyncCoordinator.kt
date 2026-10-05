package com.mehmtcan.brainscroll.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Decides WHEN the [SyncEngine] runs. The engine knows how to sync; this knows when:
 * - right after a sign-in (and so also at every app start, because the session is restored);
 * - shortly after something changed locally ([request]), with a short wait so a burst of changes is one sync;
 * - again after a failure, with growing waits (5 s, 15 s, 1 min, then every 5 min) until it works.
 *
 * When the account changes, the running loop is cancelled and a new one starts for the new account.
 */
class SyncCoordinator(
    private val scope: CoroutineScope,
    private val engine: SyncEngine,
    private val signedInUser: StateFlow<String?>,
    /** Called after every successful sync, so the screens can reload what the cloud added. */
    private val onSynced: () -> Unit,
    private val debounceMillis: Long = 1_500,
    private val retryDelaysMillis: List<Long> = listOf(5_000, 15_000, 60_000, 300_000),
) {
    private val requests = Channel<Unit>(Channel.CONFLATED)

    fun start() {
        scope.launch {
            signedInUser.collectLatest { user ->
                if (user == null) return@collectLatest
                requests.trySend(Unit) // sync once right after signing in
                var failures = 0
                while (true) {
                    requests.receive()
                    delay(debounceMillis)
                    requests.tryReceive() // changes during the wait are covered by the sync that is about to run
                    when (engine.sync(user)) {
                        is SyncResult.Success -> {
                            failures = 0
                            onSynced()
                        }
                        is SyncResult.Failure -> {
                            delay(retryDelaysMillis[minOf(failures, retryDelaysMillis.lastIndex)])
                            failures++
                            requests.trySend(Unit)
                        }
                    }
                }
            }
        }
    }

    /** Something changed locally: sync soon. Cheap to call often. */
    fun request() {
        requests.trySend(Unit)
    }
}
