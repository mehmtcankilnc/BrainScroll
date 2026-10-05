package com.mehmtcan.brainscroll.sync

import com.mehmtcan.brainscroll.data.FavoriteRecord
import com.mehmtcan.brainscroll.game.wordle.FinishedRound

/**
 * The cloud as the sync engine sees it: four operations for the signed-in user. The real implementation talks
 * to Supabase; tests use an in-memory fake. Every call acts on the account of the current session.
 *
 * Implementations must turn every failure into a [CloudException], so the engine can tell "try again later"
 * from "this data will never be accepted".
 */
interface CloudApi {
    /** Adds results. A result whose id already exists in the cloud is left as it is (no error, no duplicate). */
    suspend fun uploadResults(results: List<FinishedRound>)

    /** Adds or updates favorites. The cloud keeps the write with the newest `updatedAt`. */
    suspend fun uploadFavorites(favorites: List<FavoriteRecord>)

    /** Every result of the account. */
    suspend fun downloadResults(): List<FinishedRound>

    /** Every favorite of the account. */
    suspend fun downloadFavorites(): List<FavoriteRecord>
}

sealed class CloudException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** No network, a timeout, a server error, an expired session: the same request may work later. */
    class Transient(message: String, cause: Throwable? = null) : CloudException(message, cause)

    /** The server understood the request and refused it (validation, policy). Retrying will never help. */
    class Rejected(message: String, cause: Throwable? = null) : CloudException(message, cause)
}
