package com.mehmtcan.brainscroll.ui.pro

import com.mehmtcan.brainscroll.cloud.HintBalanceApi
import com.mehmtcan.brainscroll.cloud.HintSpend
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** A store that answers what the test says. [updates] is the store pushing an answer on its own (a refund, an approval). */
class FakeStoreBilling : StoreBilling {
    val updates = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)
    override val entitlement: Flow<Boolean> = updates

    /** What [refresh] finds; null means the request fails (no network). */
    var refreshAnswer: Boolean? = null
    var price: ProProduct? = ProProduct("$4.99")
    var purchaseResult: PurchaseResult = PurchaseResult.Success
    var restoreResult: RestoreResult = RestoreResult.Restored
    var identifyFails = false
    val identified = mutableListOf<String>()
    var purchases = 0

    /** What the store sells besides Pro; empty means the packs are not set up in the store. */
    var packs: List<HintPack> = emptyList()
    var packResult: PurchaseResult = PurchaseResult.Success
    var packPurchases = 0

    /** Runs when a pack purchase went through, like the webhook adding the hints on the server. */
    var onPackBought: (HintPack) -> Unit = {}

    override suspend fun refresh() {
        updates.emit(refreshAnswer ?: error("offline"))
    }

    override suspend fun product(): ProProduct? = price

    override suspend fun purchase(): PurchaseResult {
        purchases += 1
        return purchaseResult
    }

    var restores = 0

    override suspend fun restore(): RestoreResult {
        restores += 1
        return restoreResult
    }

    /** While set, the store has not answered about the packs yet: [packs] waits until the test completes it. */
    var packsGate: CompletableDeferred<Unit>? = null

    override suspend fun packs(): List<HintPack> {
        packsGate?.await()
        return packs
    }

    override suspend fun buyPack(pack: HintPack): PurchaseResult {
        packPurchases += 1
        if (packResult == PurchaseResult.Success) onPackBought(pack)
        return packResult
    }

    override suspend fun identify(userId: String) {
        if (identifyFails) error("offline")
        identified += userId
    }
}

class MemoryProCache(var value: Boolean = false) : ProCache {
    override fun cached() = value
    override fun setCached(pro: Boolean) {
        value = pro
    }
}

class MemoryHintCache(var value: Int = 0) : HintCache {
    override fun cachedHints() = value
    override fun setCachedHints(count: Int) {
        value = count
    }
}

/** The server's hint balance as the test wants it: [balance] is what is stored, [offline] makes every call fail. */
class FakeHintBalanceApi(var balance: Int = 0, var offline: Boolean = false) : HintBalanceApi {
    var spendCalls = 0

    override suspend fun balance(): Int? = if (offline) null else balance

    override suspend fun spend(): HintSpend {
        spendCalls += 1
        return when {
            offline -> HintSpend.Offline
            balance <= 0 -> HintSpend.Empty
            else -> HintSpend.Spent(--balance)
        }
    }
}
