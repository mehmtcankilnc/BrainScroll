package com.mehmtcan.brainscroll.ui.pro

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What the store sells: BrainScroll Pro, with the price as the store formats it for the player's country. */
data class ProProduct(val priceText: String)

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
