package com.mehmtcan.brainscroll.cloud

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The server's part of "Sign in with Apple" account deletion (App Store guideline 5.1.1(v)): an account that signed in with Apple must
 * be disconnected from the app at Apple when it is deleted. Only the server can do that (it holds our Apple key), so the app hands
 * it the one-time code at sign-in and asks it to revoke before deleting. See `supabase/functions/apple-token`.
 */
interface AppleTokenApi {
    /** Gives the server the authorization code of a sign-in. Never throws: a failure only means nothing is kept to revoke later. */
    suspend fun link(code: String)

    /**
     * Revokes the sign-in at Apple. True when it is revoked or there was nothing to revoke; false when Apple or the server could not
     * be reached or said no, and the account must then NOT be deleted yet (so it is not left half connected).
     */
    suspend fun revoke(): Boolean
}

/** No server (tests, or a build without cloud): nothing to link and nothing to revoke. */
object NoAppleTokenApi : AppleTokenApi {
    override suspend fun link(code: String) = Unit

    override suspend fun revoke(): Boolean = true
}

class SupabaseAppleTokenApi(private val client: SupabaseClient) : AppleTokenApi {
    override suspend fun link(code: String) {
        try {
            client.functions.invoke(function = FUNCTION, body = buildJsonObject {
                put("action", "link")
                put("code", code)
            })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The player is signed in either way; only the later revoke has nothing to work with.
        }
    }

    override suspend fun revoke(): Boolean = try {
        // A reply that is not a success is thrown as an exception by the library.
        client.functions.invoke(function = FUNCTION, body = buildJsonObject { put("action", "revoke") })
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    private companion object {
        const val FUNCTION = "apple-token"
    }
}
