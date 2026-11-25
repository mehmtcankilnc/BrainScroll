package com.mehmtcan.brainscroll.ui.pro

import com.mehmtcan.brainscroll.cloud.HintBalanceApi
import com.mehmtcan.brainscroll.cloud.HintSpend
import com.mehmtcan.brainscroll.cloud.NoHintBalanceApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Where the last known balance is kept on the device. It is only for showing a number: spending always asks the server. */
interface HintCache {
    fun cachedHints(): Int
    fun setCachedHints(count: Int)
}

/**
 * The bought hints as the screens see them (docs/pro-plan.md section 11). [balance] starts from the cached number and follows
 * what the server says. A hint is spent only with the server's yes ([spend]); the app never adds one: a purchase shows up
 * once the webhook has added it, which [awaitCredit] waits for.
 */
class HintWallet(
    private val api: HintBalanceApi = NoHintBalanceApi,
    private val cache: HintCache? = null,
) {
    private val _balance = MutableStateFlow(cache?.cachedHints() ?: 0)
    val balance: StateFlow<Int> = _balance

    private fun set(count: Int) {
        val safe = count.coerceAtLeast(0)
        _balance.value = safe
        cache?.setCachedHints(safe)
    }

    /** Reads the balance from the server. A failed read changes nothing. */
    suspend fun refresh() {
        api.balance()?.let(::set)
    }

    suspend fun spend(): HintSpend {
        val result = api.spend()
        when (result) {
            is HintSpend.Spent -> set(result.left)
            HintSpend.Empty -> set(0)
            HintSpend.Offline -> Unit
        }
        return result
    }

    /**
     * After a pack was bought: looks at the server every [stepMs] until the balance is above [before] (the webhook arrived), at
     * most [waitMs]. True when the hints are there; false when they are still on the way (the player is told to come back).
     */
    suspend fun awaitCredit(before: Int, waitMs: Long = 20_000, stepMs: Long = 2_000): Boolean {
        var waited = 0L
        while (true) {
            refresh()
            if (_balance.value > before) return true
            if (waited >= waitMs) return false
            delay(stepMs)
            waited += stepMs
        }
    }

    companion object {
        /** No hints, nothing to buy. */
        val None = HintWallet()
    }
}
