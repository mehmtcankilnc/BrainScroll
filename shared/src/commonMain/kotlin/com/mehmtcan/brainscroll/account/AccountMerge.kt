package com.mehmtcan.brainscroll.account

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject

/** What came of handing an anonymous account's data over to the account the player signed in to. */
enum class MergeResult {
    /** Everything was moved. */
    Done,

    /** The server does not know the ticket (used, expired, or the account changed). It will never work: forget it. */
    Invalid,

    /** No answer (offline, a server error). Keep the ticket and try again later. */
    Retry,
}

/**
 * Moves what an anonymous account holds on the server (above all the daily results) into the Google or Apple
 * account the player signs in to, when that account already existed (`supabase/migrations/...account_merge.sql`).
 * It takes two steps because it must be proved that the same person is behind both accounts: a ticket is asked
 * for while still anonymous and handed over after signing in.
 */
interface AccountMergeApi {
    /** Asks for a one-time ticket as the anonymous account. Null if that did not work (the sign-in goes on without). */
    suspend fun start(): String?

    /** Hands the ticket over as the account that was signed in to. */
    suspend fun complete(ticket: String): MergeResult
}

class SupabaseAccountMergeApi(private val client: SupabaseClient) : AccountMergeApi {

    override suspend fun start(): String? =
        try {
            // The function returns a plain text value, which PostgREST sends as a JSON string.
            client.pluginManager.getPlugin(Postgrest).rpc("start_account_merge").data.trim().removeSurrounding("\"").ifEmpty { null }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    override suspend fun complete(ticket: String): MergeResult =
        try {
            client.pluginManager.getPlugin(Postgrest).rpc(
                "complete_account_merge",
                JsonObject(mapOf("p_token" to kotlinx.serialization.json.JsonPrimitive(ticket))),
            )
            MergeResult.Done
        } catch (e: CancellationException) {
            throw e
        } catch (e: PostgrestRestException) {
            // PT404: unknown or expired. PT403 and 22023: this ticket can never be used by this account.
            if (e.code == "PT404" || e.code == "PT403" || e.code == "22023" || e.statusCode == 404 || e.statusCode == 403) MergeResult.Invalid
            else MergeResult.Retry
        } catch (e: RestException) {
            MergeResult.Retry
        } catch (e: Exception) {
            MergeResult.Retry
        }
}

/** A ticket kept on the device while the player is away signing in (the browser round trip can end the app). */
data class MergeTicket(val fromUser: String, val token: String)
