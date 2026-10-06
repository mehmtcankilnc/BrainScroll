package com.mehmtcan.brainscroll.social

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Who the server thinks I am. [username] and [inviteCode] are null until a username was chosen. */
@Serializable
data class MyProfile(
    @SerialName("is_member") val isMember: Boolean,
    val username: String? = null,
    @SerialName("invite_code") val inviteCode: String? = null,
)

/** How far back the daily speed table looks. */
enum class DailyScope { Today, AllTime }

enum class StreakKind { Current, Longest }

/** One line of the daily speed table: fewest guesses first, then the shortest time. */
@Serializable
data class DailyRow(
    val rank: Int,
    val username: String,
    val guesses: Int,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("is_me") val isMe: Boolean = false,
)

@Serializable
data class StreakRow(
    val rank: Int,
    val username: String,
    val value: Int,
    @SerialName("is_me") val isMe: Boolean = false,
)

/** The top of a table, and my own line when I am not in the top (null when I am not on the table at all). */
@Serializable
data class Board<R>(val rows: List<R> = emptyList(), val me: R? = null)

@Serializable
data class FriendsState(
    val friends: List<String> = emptyList(),
    val incoming: List<String> = emptyList(),
    val outgoing: List<String> = emptyList(),
)

@Serializable
enum class RequestStatus {
    @SerialName("FRIENDS") Friends,
    @SerialName("REQUESTED") Requested,
}

@Serializable
data class FriendResult(val username: String, val status: RequestStatus)

/** What can go wrong when talking to the leaderboards and friends. */
sealed class SocialException(message: String) : Exception(message) {
    /** No network or no answer in time. */
    class Offline : SocialException("offline")

    /** The server answered with an error, or there is no session yet. */
    class Unavailable : SocialException("unavailable")

    /** Anonymous, or no username yet: friends and a place on the tables need Google or Apple and a username. */
    class NotMember : SocialException("not a member")

    class InvalidUsername : SocialException("invalid username")
    class UsernameNotAllowed : SocialException("username not allowed")
    class UsernameTaken : SocialException("username taken")

    /** No such invite code, player or request. */
    class NotFound : SocialException("not found")

    /** The invite code or the name is my own. */
    class IsSelf : SocialException("that is you")
}

/**
 * The leaderboards and friends on the server (`supabase/migrations/20261007090000_leaderboards_friends.sql`).
 * Implementations throw [SocialException].
 */
interface SocialApi {
    suspend fun profile(): MyProfile

    /** Takes a username or changes it. Returns the new profile, with the invite code. */
    suspend fun setUsername(name: String): MyProfile

    suspend fun dailyBoard(language: com.mehmtcan.brainscroll.game.wordle.Language, scope: DailyScope, friendsOnly: Boolean): Board<DailyRow>

    suspend fun streakBoard(kind: StreakKind, friendsOnly: Boolean): Board<StreakRow>

    suspend fun friends(): FriendsState

    /** Becomes friends at once with whoever owns the code. Returns their username. */
    suspend fun addFriendByCode(code: String): String

    suspend fun sendRequest(username: String): FriendResult

    suspend fun respond(username: String, accept: Boolean)

    suspend fun removeFriend(username: String)

    suspend fun cancelRequest(username: String)
}

/** The rules for a username, the same as the server's: 3 to 16 letters, digits or underscores. */
fun isValidUsername(name: String): Boolean =
    name.length in 3..16 && name.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' }
