package com.mehmtcan.brainscroll.social

import com.mehmtcan.brainscroll.game.wordle.Language

/**
 * A stand-in for the leaderboard and friends functions on the server, following the same rules
 * (supabase/migrations/...leaderboards_friends): who may do what, instant friendship by code, requests by name.
 * Tests set [member], the tables and the friends, and read [calls].
 */
class FakeSocialApi(
    var member: Boolean = false,
    var username: String? = null,
    var inviteCode: String = "ABCD2345",
    var speedToday: List<DailyRow> = emptyList(),
    var speedAllTime: List<DailyRow> = emptyList(),
    var streaksCurrent: List<StreakRow> = emptyList(),
    var streaksLongest: List<StreakRow> = emptyList(),
    var friendsState: FriendsState = FriendsState(),
    /** Invite code -> username of its owner. */
    val codes: MutableMap<String, String> = mutableMapOf("ZZZZ9999" to "mert"),
    val takenNames: MutableSet<String> = mutableSetOf("taken"),
) : SocialApi {
    val calls = mutableListOf<String>()

    /** Thrown by every call while set, to simulate being offline. */
    var failure: SocialException? = null

    private fun enter(call: String) {
        calls += call
        failure?.let { throw it }
    }

    private fun requireReady() {
        if (!member || username == null) throw SocialException.NotMember()
    }

    override suspend fun profile(): MyProfile {
        enter("profile")
        return MyProfile(member, username, if (username != null) inviteCode else null)
    }

    override suspend fun setUsername(name: String): MyProfile {
        enter("setUsername:$name")
        if (!member) throw SocialException.NotMember()
        if (!isValidUsername(name)) throw SocialException.InvalidUsername()
        if (name.lowercase() in listOf("fuck", "admin")) throw SocialException.UsernameNotAllowed()
        if (name.lowercase() in takenNames) throw SocialException.UsernameTaken()
        username = name
        return profile()
    }

    override suspend fun dailyBoard(language: Language, scope: DailyScope, friendsOnly: Boolean): Board<DailyRow> {
        enter("dailyBoard:${language.name}:${scope.name}:$friendsOnly")
        if (friendsOnly) requireReady()
        val rows = if (scope == DailyScope.Today) speedToday else speedAllTime
        return Board(rows.take(50), rows.firstOrNull { it.isMe }?.takeIf { it !in rows.take(50) })
    }

    override suspend fun streakBoard(kind: StreakKind, friendsOnly: Boolean): Board<StreakRow> {
        enter("streakBoard:${kind.name}:$friendsOnly")
        if (friendsOnly) requireReady()
        val rows = if (kind == StreakKind.Current) streaksCurrent else streaksLongest
        return Board(rows.take(50), rows.firstOrNull { it.isMe }?.takeIf { it !in rows.take(50) })
    }

    override suspend fun friends(): FriendsState {
        enter("friends")
        requireReady()
        return friendsState
    }

    override suspend fun addFriendByCode(code: String): String {
        enter("addFriendByCode:$code")
        requireReady()
        if (code == inviteCode) throw SocialException.IsSelf()
        val name = codes[code] ?: throw SocialException.NotFound()
        friendsState = friendsState.copy(friends = (friendsState.friends + name).distinct().sortedBy { it.lowercase() })
        return name
    }

    override suspend fun sendRequest(username: String): FriendResult {
        enter("sendRequest:$username")
        requireReady()
        if (username.equals(this.username, ignoreCase = true)) throw SocialException.IsSelf()
        if (username in friendsState.incoming) {
            friendsState = friendsState.copy(incoming = friendsState.incoming - username, friends = friendsState.friends + username)
            return FriendResult(username, RequestStatus.Friends)
        }
        if (username.lowercase() !in codes.values.map { it.lowercase() } + takenNames) throw SocialException.NotFound()
        friendsState = friendsState.copy(outgoing = (friendsState.outgoing + username).distinct())
        return FriendResult(username, RequestStatus.Requested)
    }

    override suspend fun respond(username: String, accept: Boolean) {
        enter("respond:$username:$accept")
        requireReady()
        if (username !in friendsState.incoming) throw SocialException.NotFound()
        friendsState = friendsState.copy(
            incoming = friendsState.incoming - username,
            friends = if (accept) friendsState.friends + username else friendsState.friends,
        )
    }

    override suspend fun removeFriend(username: String) {
        enter("removeFriend:$username")
        requireReady()
        friendsState = friendsState.copy(friends = friendsState.friends - username)
    }

    override suspend fun cancelRequest(username: String) {
        enter("cancelRequest:$username")
        requireReady()
        friendsState = friendsState.copy(outgoing = friendsState.outgoing - username)
    }
}
