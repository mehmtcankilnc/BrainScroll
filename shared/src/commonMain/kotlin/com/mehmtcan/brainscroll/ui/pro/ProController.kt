package com.mehmtcan.brainscroll.ui.pro

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Where the last known Pro answer is kept on the device, so the app opened offline still knows the player is Pro. */
interface ProCache {
    fun cached(): Boolean
    fun setCached(pro: Boolean)
}

/**
 * Pro for the rest of the app: starts from the cached answer, follows what the store says, and keeps the cache. Only a
 * definite answer from the store changes it: a failed request (no network) changes nothing, so an offline start never loses
 * Pro, while a refund (the store says "not Pro") takes it away (docs/pro-plan.md section 8.3).
 */
class ProController(
    private val billing: StoreBilling?,
    private val cache: ProCache,
    scope: CoroutineScope,
) : ProState {
    override val available: Boolean get() = billing != null

    private val _isPro = MutableStateFlow(billing != null && cache.cached())
    override val isPro: StateFlow<Boolean> = _isPro

    init {
        if (billing != null) {
            scope.launch { billing.entitlement.collect(::apply) }
            scope.launch { runCatching { billing.refresh() } }
        }
    }

    private fun apply(pro: Boolean) {
        _isPro.value = pro
        cache.setCached(pro)
    }

    override suspend fun product(): ProProduct? = billing?.let { runCatching { it.product() }.getOrNull() }

    override suspend fun purchase(): PurchaseResult {
        val store = billing ?: return PurchaseResult.Failed("unavailable")
        val result = runCatching { store.purchase() }.getOrElse { PurchaseResult.Failed(it.message ?: "error") }
        if (result == PurchaseResult.Success) apply(true)
        return result
    }

    override suspend fun restore(): RestoreResult {
        val store = billing ?: return RestoreResult.Failed
        val result = runCatching { store.restore() }.getOrElse { RestoreResult.Failed }
        if (result == RestoreResult.Restored) apply(true)
        return result
    }

    override suspend fun identify(userId: String) {
        runCatching { billing?.identify(userId) }
    }
}
