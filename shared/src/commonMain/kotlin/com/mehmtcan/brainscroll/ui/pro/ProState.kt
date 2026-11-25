package com.mehmtcan.brainscroll.ui.pro

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What the store sells: BrainScroll Pro, with the price as the store formats it for the player's country. */
data class ProProduct(val priceText: String)

/** A hint pack the store sells: [hints] of them for [priceText] (as the store formats it for the player's country). */
data class HintPack(val id: String, val hints: Int, val priceText: String, val priceMicros: Long = 0)

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object Cancelled : PurchaseResult

    /** Waiting for someone else (a parent's approval, a slow payment). Nothing is unlocked; it unlocks when the store says so. */
    data object Pending : PurchaseResult
    data class Failed(val reason: String) : PurchaseResult
}

sealed interface RestoreResult {
    data object Restored : RestoreResult
    data object NothingToRestore : RestoreResult
    data object Failed : RestoreResult
}

/**
 * BrainScroll Pro as the screens see it (docs/pro-plan.md section 8.2). One purchase, for good: there is no
 * renewal or trial in anything this says.
 */
interface ProState {
    /** Whether this build can sell Pro at all. False on the desktop and without store keys: no purchase screen is shown then. */
    val available: Boolean
    val isPro: StateFlow<Boolean>
    suspend fun product(): ProProduct?
    suspend fun purchase(): PurchaseResult
    suspend fun restore(): RestoreResult

    /** The app account changed: the purchase follows the player to [userId] (docs/pro-plan.md section 8.4). */
    suspend fun identify(userId: String) {}

    /** The bought hints (docs/pro-plan.md section 11). Empty where nothing can be bought. */
    val hints: HintWallet get() = HintWallet.None

    /** The hint packs the store offers, smallest first; empty when the store has none (not set up yet) or cannot be reached. */
    suspend fun packs(): List<HintPack> = emptyList()

    /**
     * Buys [pack]. [PurchaseResult.Success] means the store took the payment; the hints themselves arrive from the server a
     * moment later ([HintWallet.awaitCredit]).
     */
    suspend fun buyPack(pack: HintPack): PurchaseResult = PurchaseResult.Failed("unavailable")
}

/** Free, and nothing to buy. The default for previews, tests and the desktop. */
object NoPro : ProState {
    override val available = false
    override val isPro: StateFlow<Boolean> = MutableStateFlow(false)
    override suspend fun product(): ProProduct? = null
    override suspend fun purchase(): PurchaseResult = PurchaseResult.Failed("unavailable")
    override suspend fun restore(): RestoreResult = RestoreResult.Failed
}

val LocalPro = staticCompositionLocalOf<ProState> { NoPro }
