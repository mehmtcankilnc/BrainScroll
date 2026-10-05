package com.mehmtcan.brainscroll.cloud

import com.mehmtcan.brainscroll.data.FavoriteRecord
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.sync.CloudApi
import com.mehmtcan.brainscroll.sync.CloudException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The two tables of `supabase/migrations/20261005120000_initial_schema.sql`, seen from the app. */
class SupabaseCloudApi(private val client: SupabaseClient) : CloudApi {

    override suspend fun uploadResults(results: List<FinishedRound>) = guarded {
        client.from(RESULTS_TABLE).upsert(results.map { it.toRow() }) {
            // user_id comes from the session (a column default), so it is not sent. A known id is left untouched.
            onConflict = "user_id,id"
            ignoreDuplicates = true
        }
        Unit
    }

    override suspend fun uploadFavorites(favorites: List<FavoriteRecord>) = guarded {
        client.from(FAVORITES_TABLE).upsert(favorites.map { it.toRow() }) {
            // On a conflict the row is updated; a database trigger keeps whichever write has the newest updated_at.
            onConflict = "user_id,result_id"
        }
        Unit
    }

    override suspend fun downloadResults(): List<FinishedRound> = guarded {
        fetchAll<ResultRow>(RESULTS_TABLE, orderBy = "id").mapNotNull { it.toFinishedRoundOrNull() }
    }

    override suspend fun downloadFavorites(): List<FavoriteRecord> = guarded {
        fetchAll<FavoriteRow>(FAVORITES_TABLE, orderBy = "result_id").map { it.toRecord() }
    }

    /** The server returns at most 1000 rows per request, so everything is read page by page in a stable order. */
    private suspend inline fun <reified T : Any> fetchAll(table: String, orderBy: String): List<T> {
        val all = mutableListOf<T>()
        var from = 0L
        while (true) {
            val page = client.from(table).select {
                order(orderBy, Order.ASCENDING)
                range(from, from + PAGE_SIZE - 1)
            }.decodeList<T>()
            all += page
            if (page.size < PAGE_SIZE) return all
            from += PAGE_SIZE
        }
    }

    private companion object {
        const val RESULTS_TABLE = "game_result"
        const val FAVORITES_TABLE = "favorite"
        const val PAGE_SIZE = 1000
    }
}

/**
 * Turns whatever the network layer throws into the two kinds the sync engine understands.
 * An HTTP answer that says "your data is wrong or not allowed" will never succeed on retry; everything else
 * (no network, timeout, server error, expired login) might.
 */
private suspend inline fun <T> guarded(block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: RestException) {
    val permanent = e.statusCode in PERMANENT_STATUS_CODES
    throw if (permanent) CloudException.Rejected(e.message ?: "refused", e) else CloudException.Transient(e.message ?: "server error", e)
} catch (e: Exception) {
    throw CloudException.Transient(e.message ?: "network error", e)
}

/** 400 invalid data, 403 blocked by a policy, 404 no such table, 409 conflict, 413 too large, 422 unprocessable. */
private val PERMANENT_STATUS_CODES = setOf(400, 403, 404, 409, 413, 422)

@Serializable
private data class ResultRow(
    val id: String,
    val game: String,
    val mode: String,
    val language: String,
    val answer: String,
    val guesses: String,
    val outcome: String,
    @SerialName("was_skipped") val wasSkipped: Boolean,
    @SerialName("finished_at") val finishedAt: Long,
    @SerialName("day_index") val dayIndex: Int,
    @SerialName("duration_ms") val durationMs: Long? = null,
)

@Serializable
private data class FavoriteRow(
    @SerialName("result_id") val resultId: String,
    @SerialName("is_favorite") val isFavorite: Boolean,
    @SerialName("updated_at") val updatedAt: Long,
)

private const val GAME_WORD = "word"

private fun FinishedRound.toRow() = ResultRow(
    id = id,
    game = GAME_WORD,
    mode = mode.name,
    language = language.name,
    answer = answer,
    guesses = guesses.joinToString(","),
    outcome = outcome.name,
    wasSkipped = wasSkipped,
    finishedAt = finishedAt,
    dayIndex = dayIndex.toInt(),
    durationMs = durationMs,
)

/** A row this version of the app does not understand (a newer game or language) is skipped, not fatal. */
private fun ResultRow.toFinishedRoundOrNull(): FinishedRound? {
    if (game != GAME_WORD) return null
    return FinishedRound(
        id = id,
        mode = Mode.entries.firstOrNull { it.name == mode } ?: return null,
        language = Language.entries.firstOrNull { it.name == language } ?: return null,
        answer = answer,
        guesses = if (guesses.isEmpty()) emptyList() else guesses.split(","),
        outcome = Outcome.entries.firstOrNull { it.name == outcome } ?: return null,
        wasSkipped = wasSkipped,
        finishedAt = finishedAt,
        durationMs = durationMs,
        puzzleDay = this.dayIndex.toLong().takeIf { it != com.mehmtcan.brainscroll.time.IstanbulDay.dayIndex(finishedAt) },
    )
}

private fun FavoriteRecord.toRow() = FavoriteRow(resultId, isFavorite, updatedAt)

private fun FavoriteRow.toRecord() = FavoriteRecord(resultId, isFavorite, updatedAt)
