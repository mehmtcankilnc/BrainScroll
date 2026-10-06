package com.mehmtcan.brainscroll.social

import com.mehmtcan.brainscroll.game.wordle.Language
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Calls the functions of `supabase/migrations/20261007090000_leaderboards_friends.sql`. */
class SupabaseSocialApi(private val client: SupabaseClient) : SocialApi {

    override suspend fun profile(): MyProfile = parse(call("get_my_profile") {})

    override suspend fun setUsername(name: String): MyProfile =
        parse(call("set_username", invalid = SocialException.InvalidUsername()) { put("p_name", name) })

    override suspend fun dailyBoard(language: Language, scope: DailyScope, friendsOnly: Boolean): Board<DailyRow> =
        parse(call("get_daily_leaderboard") {
            put("p_language", language.name)
            put("p_scope", if (scope == DailyScope.Today) "TODAY" else "ALL_TIME")
            put("p_friends", friendsOnly)
        })

    override suspend fun streakBoard(kind: StreakKind, friendsOnly: Boolean): Board<StreakRow> =
        parse(call("get_streak_leaderboard") {
            put("p_kind", if (kind == StreakKind.Current) "CURRENT" else "LONGEST")
            put("p_friends", friendsOnly)
        })

    override suspend fun friends(): FriendsState = parse(call("get_friends") {})

    override suspend fun addFriendByCode(code: String): String =
        parse<UsernameDto>(call("add_friend_by_code", invalid = SocialException.IsSelf()) { put("p_code", code) }).username

    override suspend fun sendRequest(username: String): FriendResult =
        parse(call("send_friend_request", invalid = SocialException.IsSelf()) { put("p_username", username) })

    override suspend fun respond(username: String, accept: Boolean) {
        call("respond_friend_request") {
            put("p_username", username)
            put("p_accept", accept)
        }
    }

    override suspend fun removeFriend(username: String) {
        call("remove_friend") { put("p_username", username) }
    }

    override suspend fun cancelRequest(username: String) {
        call("cancel_friend_request") { put("p_username", username) }
    }

    /** Runs one function and returns what it answered (JSON text). [invalid] is what the code 22023 means here. */
    private suspend fun call(
        function: String,
        invalid: SocialException = SocialException.Unavailable(),
        parameters: JsonObjectBuilder.() -> Unit,
    ): String =
        try {
            client.pluginManager.getPlugin(Postgrest).rpc(function, buildJsonObject(parameters)).data
        } catch (e: CancellationException) {
            throw e
        } catch (e: PostgrestRestException) {
            // The database's own error codes (see the RAISE statements in the migration).
            throw when (e.code) {
                "PT403" -> SocialException.NotMember()
                "PT404" -> SocialException.NotFound()
                "PT409" -> SocialException.UsernameTaken()
                "PT422" -> SocialException.UsernameNotAllowed()
                "22023" -> invalid
                else -> when (e.statusCode) {
                    403 -> SocialException.NotMember()
                    404 -> SocialException.NotFound()
                    else -> SocialException.Unavailable()
                }
            }
        } catch (e: RestException) {
            throw SocialException.Unavailable()
        } catch (e: Exception) {
            throw SocialException.Offline() // the request never got an answer: no network, a timeout, ...
        }
}

internal val socialJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class UsernameDto(val username: String)

private inline fun <reified T> parse(json: String): T =
    try {
        socialJson.decodeFromString<T>(json)
    } catch (e: Exception) {
        throw SocialException.Unavailable() // an answer we cannot read is as good as no answer
    }
