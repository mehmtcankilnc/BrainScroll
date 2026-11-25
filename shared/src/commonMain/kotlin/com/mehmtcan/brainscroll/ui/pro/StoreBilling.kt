package com.mehmtcan.brainscroll.ui.pro

import kotlinx.coroutines.flow.Flow

/**
 * The store behind Pro: RevenueCat on Android and iOS (`RevenueCatBilling` in their source sets). [ProController] is the
 * only user. Kept this small so the rules around it (cache, restore, pending) are tested with a fake.
 */
interface StoreBilling {
    /**
     * Every answer the store gives about the `pro` right: true or false. A failed request gives nothing, so a bad
     * network never reads as "not Pro". Also carries a purchase approved later (Ask to Buy).
     */
    val entitlement: Flow<Boolean>

    /** Asks the store again; the answer arrives on [entitlement]. */
    suspend fun refresh()
    suspend fun product(): ProProduct?
    suspend fun purchase(): PurchaseResult
    suspend fun restore(): RestoreResult

    /** The account of the app changed (guest, or signed in). The purchase follows the player to the new id. */
    suspend fun identify(userId: String)

    /** The hint packs (consumables), smallest first. Empty when the store does not have them yet. */
    suspend fun packs(): List<HintPack> = emptyList()

    /** Buys a pack. The store only takes the payment; the server adds the hints (the RevenueCat webhook). */
    suspend fun buyPack(pack: HintPack): PurchaseResult = PurchaseResult.Failed("unavailable")
}

/** The store of this platform, or null when there is none (desktop) or no key was put in [ProConfig]. */
expect fun createStoreBilling(): StoreBilling?
