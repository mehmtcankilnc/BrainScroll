package com.mehmtcan.brainscroll.ui.pro

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

    override suspend fun refresh() {
        updates.emit(refreshAnswer ?: error("offline"))
    }

    override suspend fun product(): ProProduct? = price

    override suspend fun purchase(): PurchaseResult {
        purchases += 1
        return purchaseResult
    }

    override suspend fun restore(): RestoreResult = restoreResult

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
