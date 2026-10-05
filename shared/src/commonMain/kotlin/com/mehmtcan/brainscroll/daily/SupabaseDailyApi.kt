package com.mehmtcan.brainscroll.daily

import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.LetterResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Calls the three functions of `supabase/migrations/20261006090000_daily_puzzle.sql`. */
class SupabaseDailyApi(private val client: SupabaseClient) : DailyApi {

    override suspend fun state(language: Language) = call("get_daily_state") { put("p_language", language.name) }

    override suspend fun start(language: Language) = call("start_daily") { put("p_language", language.name) }

    override suspend fun guess(language: Language, word: String) = call("submit_daily_guess") {
        put("p_language", language.name)
        put("p_guess", word)
    }

    private suspend fun call(function: String, parameters: JsonObjectBuilder.() -> Unit): DailyState =
        try {
            val result = client.pluginManager.getPlugin(Postgrest).rpc(function, buildJsonObject(parameters))
            parseDailyState(result.data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: DailyException) {
            throw e
        } catch (e: PostgrestRestException) {
            // The database's own error codes (see the RAISE statements in the migration).
            throw when (e.code) {
                "PT409" -> DailyException.AlreadyFinished()
                "PT412" -> DailyException.NotStarted()
                "22023" -> DailyException.InvalidGuess()
                // The HTTP status says the same (PTxyz codes ask for status xyz), as a second way to recognise them.
                else -> when (e.statusCode) {
                    409 -> DailyException.AlreadyFinished()
                    412 -> DailyException.NotStarted()
                    else -> DailyException.Unavailable()
                }
            }
        } catch (e: RestException) {
            throw DailyException.Unavailable()
        } catch (e: Exception) {
            throw DailyException.Offline() // the request never got an answer: no network, a timeout, ...
        }
}

private val lenientJson = Json { ignoreUnknownKeys = true }

/** Reads what the database functions return. */
internal fun parseDailyState(json: String): DailyState = lenientJson.decodeFromString<DailyStateDto>(json).toState()

@Serializable
internal data class DailyStateDto(
    @SerialName("day_index") val dayIndex: Long,
    val language: String,
    val status: String,
    @SerialName("max_attempts") val maxAttempts: Int,
    val guesses: List<GuessDto> = emptyList(),
    @SerialName("started_at_ms") val startedAtMs: Long? = null,
    @SerialName("finished_at_ms") val finishedAtMs: Long? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
    val answer: String? = null,
    @SerialName("result_id") val resultId: String? = null,
    @SerialName("server_now_ms") val serverNowMs: Long,
)

@Serializable
internal data class GuessDto(val word: String, val feedback: String)

internal fun DailyStateDto.toState() = DailyState(
    dayIndex = dayIndex,
    language = Language.entries.firstOrNull { it.name == language } ?: error("unknown language $language"),
    status = when (status) {
        "NOT_STARTED" -> DailyStatus.NotStarted
        "PLAYING" -> DailyStatus.Playing
        "WON" -> DailyStatus.Won
        "LOST" -> DailyStatus.Lost
        else -> error("unknown status $status")
    },
    maxAttempts = maxAttempts,
    guesses = guesses.map { DailyGuess(it.word, it.feedback.map(::letterResult)) },
    startedAtMs = startedAtMs,
    finishedAtMs = finishedAtMs,
    durationMs = durationMs,
    answer = answer,
    resultId = resultId,
    serverNowMs = serverNowMs,
)

private fun letterResult(c: Char) = when (c) {
    'C' -> LetterResult.Correct
    'P' -> LetterResult.Present
    'A' -> LetterResult.Absent
    else -> error("unknown feedback letter $c")
}
