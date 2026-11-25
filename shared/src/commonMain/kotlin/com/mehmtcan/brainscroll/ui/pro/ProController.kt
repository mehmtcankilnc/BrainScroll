package com.mehmtcan.brainscroll.ui.pro

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
    override val hints: HintWallet = HintWallet.None,
) : ProState {
    override val available: Boolean get() = billing != null

    /**
     * Whether the store knows the player by the account id yet. A pack bought before that is reported to the server under the
     * store's own anonymous id, which no balance belongs to: the money would be taken and no hints given. So no pack is sold
     * until [identify] has worked. A store opened a moment after the app started waits a few seconds for it instead of showing no packs.
     */
    private val identified = MutableStateFlow(false)

    private val _isPro = MutableStateFlow(billing != null && cache.cached())
    override val isPro: StateFlow<Boolean> = _isPro

    init {
        if (billing != null) {
            scope.launch { billing.entitlement.collect(::apply) }
            scope.launch { runCatching { billing.refresh() } }
        }
    }

    /** TEMPORARY, for the preview buttons: shows the app as Pro or not, without touching the store or the saved answer. */
    fun previewSet(pro: Boolean) {
        _isPro.value = pro
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
        identified.value = false
        runCatching { billing?.identify(userId) }.onSuccess { identified.value = billing != null }
    }

    override suspend fun packs(): List<HintPack> {
        val store = billing ?: return emptyList()
        if (!identified.value) withTimeoutOrNull(IDENTIFY_WAIT_MILLIS) { identified.first { it } }
        if (!identified.value) return emptyList()
        return runCatching { store.packs() }.getOrNull().orEmpty()
    }

    override suspend fun buyPack(pack: HintPack): PurchaseResult {
        val store = billing ?: return PurchaseResult.Failed("unavailable")
        if (!identified.value) return PurchaseResult.Failed("not_identified")
        return runCatching { store.buyPack(pack) }.getOrElse { PurchaseResult.Failed(it.message ?: "error") }
    }

    private companion object {
        const val IDENTIFY_WAIT_MILLIS = 6_000L
    }
}
