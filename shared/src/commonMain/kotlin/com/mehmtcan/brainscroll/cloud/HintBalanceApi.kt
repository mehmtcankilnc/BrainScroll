package com.mehmtcan.brainscroll.cloud

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CancellationException

/** What came of spending one bought hint on the server. */
sealed interface HintSpend {
    /** One was spent; [left] remain. */
    data class Spent(val left: Int) : HintSpend

    /** The server has none left (the cached number was out of date). Nothing was spent. */
    data object Empty : HintSpend

    /** No answer (no network, a server error). Nothing was spent: a bought hint is never given without the server's yes. */
    data object Offline : HintSpend
}

/**
 * The bought hints (docs/pro-plan.md section 11, Asama B). The balance lives on the server
 * (`supabase/migrations/...hint_balance.sql`): the app can read it and spend one at a time, never add. The adding is done by the
 * RevenueCat webhook when a pack is bought.
 */
interface HintBalanceApi {
    /** The balance, or null when it could not be read (offline). */
    suspend fun balance(): Int?

    suspend fun spend(): HintSpend
}

class SupabaseHintBalanceApi(private val client: SupabaseClient) : HintBalanceApi {
    override suspend fun balance(): Int? =
        try {
            client.pluginManager.getPlugin(Postgrest).rpc("my_hint_balance").data.trim().toIntOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    override suspend fun spend(): HintSpend =
        try {
            // An integer, or `null` when there was nothing to spend.
            val left = client.pluginManager.getPlugin(Postgrest).rpc("spend_hint").data.trim().toIntOrNull()
            if (left == null) HintSpend.Empty else HintSpend.Spent(left)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            HintSpend.Offline
        }
}

/** No server to ask: the balance is unknown and nothing can be spent. The default for tests and previews. */
object NoHintBalanceApi : HintBalanceApi {
    override suspend fun balance(): Int? = null

    override suspend fun spend(): HintSpend = HintSpend.Offline
}
