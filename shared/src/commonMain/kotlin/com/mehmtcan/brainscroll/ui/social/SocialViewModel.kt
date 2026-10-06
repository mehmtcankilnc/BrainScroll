package com.mehmtcan.brainscroll.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.social.Board
import com.mehmtcan.brainscroll.social.DailyRow
import com.mehmtcan.brainscroll.social.DailyScope
import com.mehmtcan.brainscroll.social.FriendsState
import com.mehmtcan.brainscroll.social.MyProfile
import com.mehmtcan.brainscroll.social.RequestStatus
import com.mehmtcan.brainscroll.social.SocialApi
import com.mehmtcan.brainscroll.social.SocialException
import com.mehmtcan.brainscroll.social.StreakKind
import com.mehmtcan.brainscroll.social.StreakRow
import com.mehmtcan.brainscroll.social.isValidUsername
import com.mehmtcan.brainscroll.social.socialJson
import com.mehmtcan.brainscroll.telemetry.NoTelemetry
import com.mehmtcan.brainscroll.telemetry.Telemetry
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The three parts of the ranks tab. */
enum class Section { Speed, Streak, Friends }

/** What the signed-in state allows (docs/decisions.md: a place on the tables and friends need Google or Apple and a username). */
enum class Access {
    /** No session yet. The tables need a connection anyway. */
    Offline,

    /** An anonymous account: can look at the tables, cannot appear in them. */
    Anonymous,

    /** Signed in with Google or Apple, and the profile (username) is still being looked up. */
    Checking,

    /** Signed in, but no username yet. */
    NeedsUsername,

    Ready,
}

data class SocialUiState(
    val access: Access,
    val username: String?,
    val inviteCode: String?,
    val section: Section,
    val language: Language,
    val scope: DailyScope,
    val kind: StreakKind,
    /** Only my friends (and me) on the table instead of everyone. Only for [Access.Ready]. */
    val friendsOnly: Boolean,
    val speed: Board<DailyRow>?,
    val streak: Board<StreakRow>?,
    val friends: FriendsState?,
    val loading: Boolean,
    /** The request failed and the screen shows the last view it had. */
    val stale: Boolean,
    /** The request failed and there is nothing to show. */
    val failed: Boolean,
)

/** One-off messages for the screen. [name] is the player the message is about, when there is one. */
data class SocialEvent(val kind: Kind, val name: String? = null) {
    enum class Kind {
        UsernameSaved, UsernameInvalid, UsernameNotAllowed, UsernameTaken,
        FriendAdded, RequestSent, NowFriends,
        CodeNotFound, PlayerNotFound, IsSelf,
        Offline, Unavailable,

        /** An invite arrived but the player is anonymous. The code is kept for after the sign-in. */
        InviteNeedsSignIn,

        /** An invite arrived but there is no username yet. The code is kept for after choosing one. */
        InviteNeedsUsername,
    }
}

/**
 * The ranks tab's logic: which table is open, loading it (the last view is cached for offline), the username,
 * and the friends. Everything real happens on the server ([SocialApi]); this only asks and shows.
 */
class SocialViewModel(
    private val api: SocialApi,
    private val repository: GameRepository,
    private val accountState: StateFlow<AccountState>,
    initialLanguage: Language,
    /** Friend invite codes that arrived through a link. */
    invites: SharedFlow<String> = MutableSharedFlow(),
    private val telemetry: Telemetry = NoTelemetry,
) : ViewModel() {

    private var userId: String? = null
    private var isMember = false
    private var profile: MyProfile? = null
    private var profileChecked = false

    private var section = Section.Speed
    private var language = initialLanguage
    private var scope = DailyScope.Today
    private var kind = StreakKind.Current
    private var friendsOnly = false

    private var speed: Board<DailyRow>? = null
    private var streak: Board<StreakRow>? = null
    private var friends: FriendsState? = null
    private var loading = false
    private var stale = false
    private var failed = false

    private var pendingInvite: String? = null
    private var job: Job? = null

    private val _ui = MutableStateFlow(snapshot())
    val ui: StateFlow<SocialUiState> = _ui

    private val _events = MutableSharedFlow<SocialEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SocialEvent> = _events

    init {
        viewModelScope.launch {
            accountState.collect { state -> onAccount(state) }
        }
        viewModelScope.launch {
            invites.collect { code -> onInvite(code) }
        }
    }

    // --- Screen actions ---

    /** The tab was opened: look again, quietly. */
    fun onShown() {
        if (userId == null) return
        if (isMember && profile?.username == null) refreshProfile() else reload()
    }

    /** The retry button. */
    fun refresh() = reload()

    fun selectSection(next: Section) {
        if (next == section) return
        section = next
        reload()
    }

    fun selectLanguage(next: Language) {
        if (next == language) return
        language = next
        reload()
    }

    fun selectScope(next: DailyScope) {
        if (next == scope) return
        scope = next
        reload()
    }

    fun selectKind(next: StreakKind) {
        if (next == kind) return
        kind = next
        reload()
    }

    fun selectFriendsOnly(only: Boolean) {
        if (only == friendsOnly || (only && !isReady())) return
        friendsOnly = only
        reload()
    }

    fun saveUsername(name: String) {
        val trimmed = name.trim()
        if (!isValidUsername(trimmed)) return emit(SocialEvent.Kind.UsernameInvalid)
        viewModelScope.launch {
            try {
                val saved = api.setUsername(trimmed)
                profile = saved
                profileChecked = true
                cache("profile", socialJson.encodeToString(saved))
                emit(SocialEvent.Kind.UsernameSaved, saved.username)
                reload()
                applyPendingInvite()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SocialException) {
                emit(
                    when (e) {
                        is SocialException.InvalidUsername -> SocialEvent.Kind.UsernameInvalid
                        is SocialException.UsernameNotAllowed -> SocialEvent.Kind.UsernameNotAllowed
                        is SocialException.UsernameTaken -> SocialEvent.Kind.UsernameTaken
                        is SocialException.Offline -> SocialEvent.Kind.Offline
                        else -> SocialEvent.Kind.Unavailable
                    },
                )
            }
            publish()
        }
    }

    /** A friend's invite code, typed or pasted by the player. Friends at once. */
    fun addFriendByCode(code: String) {
        val trimmed = code.trim().uppercase()
        if (trimmed.isEmpty()) return
        friendAction(codeNotFound = true) {
            val name = api.addFriendByCode(trimmed)
            emit(SocialEvent.Kind.FriendAdded, name)
        }
    }

    /** Asks a player by username. Becomes friendship at once if they had already asked me. */
    fun sendRequest(username: String) {
        val trimmed = username.trim()
        if (trimmed.isEmpty()) return
        friendAction {
            val result = api.sendRequest(trimmed)
            emit(if (result.status == RequestStatus.Friends) SocialEvent.Kind.NowFriends else SocialEvent.Kind.RequestSent, result.username)
        }
    }

    fun respond(username: String, accept: Boolean) = friendAction {
        api.respond(username, accept)
        if (accept) emit(SocialEvent.Kind.NowFriends, username)
    }

    fun removeFriend(username: String) = friendAction { api.removeFriend(username) }

    fun cancelRequest(username: String) = friendAction { api.cancelRequest(username) }

    // --- Internals ---

    private fun isReady() = isMember && profile?.username != null

    private fun onAccount(state: AccountState) {
        val newUser = (state as? AccountState.SignedIn)?.userId?.takeIf { it.isNotEmpty() }
        val member = state is AccountState.SignedIn &&
            (state.kind == AccountState.SignedIn.Kind.Google || state.kind == AccountState.SignedIn.Kind.Apple)
        val changed = newUser != userId || member != isMember
        userId = newUser
        isMember = member
        if (!changed) return

        // Another account: nothing of the old one may stay on screen.
        profile = null
        profileChecked = false
        speed = null
        streak = null
        friends = null
        friendsOnly = friendsOnly && member
        failed = false
        stale = false
        if (newUser == null) {
            job?.cancel()
            loading = false
            publish()
            return
        }
        if (member) {
            profile = cached<MyProfile>("profile")
            refreshProfile()
        } else {
            reload()
        }
    }

    private fun refreshProfile() {
        viewModelScope.launch {
            try {
                profile = api.profile().also { cache("profile", socialJson.encodeToString(it)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SocialException) {
                // Keep the cached profile, if any: the username does not change by being offline.
            }
            profileChecked = true
            publish()
            reload()
            applyPendingInvite()
        }
    }

    private fun onInvite(code: String) {
        pendingInvite = code
        when {
            userId == null || !isMember -> emit(SocialEvent.Kind.InviteNeedsSignIn)
            profile?.username == null -> {
                if (profileChecked) emit(SocialEvent.Kind.InviteNeedsUsername)
            }
            else -> applyPendingInvite()
        }
    }

    private fun applyPendingInvite() {
        val code = pendingInvite ?: return
        if (!isReady()) return
        pendingInvite = null
        friendAction(codeNotFound = true, retryCode = code) {
            emit(SocialEvent.Kind.FriendAdded, api.addFriendByCode(code))
        }
    }

    /** Runs a friend change, tells the player how it went and refreshes what it changed. */
    private fun friendAction(codeNotFound: Boolean = false, retryCode: String? = null, action: suspend () -> Unit) {
        if (!isReady()) return
        viewModelScope.launch {
            try {
                action()
                reload(silent = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SocialException) {
                when (e) {
                    is SocialException.NotFound -> emit(if (codeNotFound) SocialEvent.Kind.CodeNotFound else SocialEvent.Kind.PlayerNotFound)
                    is SocialException.IsSelf -> emit(SocialEvent.Kind.IsSelf)
                    is SocialException.Offline -> {
                        pendingInvite = retryCode ?: pendingInvite // an invite that could not be used yet is not lost
                        emit(SocialEvent.Kind.Offline)
                    }
                    else -> emit(SocialEvent.Kind.Unavailable)
                }
            }
        }
    }

    /** Loads what the current selection shows: from the cache at once, then from the server. */
    private fun reload(silent: Boolean = false) {
        job?.cancel()
        val user = userId ?: return publish()
        val key = cacheKey()
        if (!silent) {
            when (section) {
                Section.Speed -> speed = cached(key)
                Section.Streak -> streak = cached(key)
                Section.Friends -> friends = cached(key)
            }
            loading = true
            stale = false
            failed = false
            publish()
        }
        // Friends need a friends account; for anyone else the screen asks to sign in instead of loading.
        if (section == Section.Friends && !isReady()) {
            loading = false
            publish()
            return
        }
        job = viewModelScope.launch {
            try {
                when (section) {
                    Section.Speed -> speed = api.dailyBoard(language, scope, friendsOnly).also { cache(key, socialJson.encodeToString(it)) }
                    Section.Streak -> streak = api.streakBoard(kind, friendsOnly).also { cache(key, socialJson.encodeToString(it)) }
                    Section.Friends -> friends = api.friends().also { cache(key, socialJson.encodeToString(it)) }
                }
                stale = false
                failed = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: SocialException) {
                val have = when (section) {
                    Section.Speed -> speed != null
                    Section.Streak -> streak != null
                    Section.Friends -> friends != null
                }
                if (have) stale = true else failed = true
                if (user != userId) return@launch
            }
            loading = false
            publish()
        }
    }

    private fun cacheKey() = when (section) {
        Section.Speed -> "board:speed:${language.name}:${scope.name}:$friendsOnly"
        Section.Streak -> "board:streak:${kind.name}:$friendsOnly"
        Section.Friends -> "friends"
    }

    private fun cache(key: String, json: String) {
        val user = userId ?: return
        repository.cacheText("social:$user:$key", json)
    }

    private inline fun <reified T> cached(key: String): T? {
        val user = userId ?: return null
        val json = repository.cachedText("social:$user:$key") ?: return null
        return try {
            socialJson.decodeFromString<T>(json)
        } catch (e: Exception) {
            null // an old cache in a format we no longer read: act as if there were none
        }
    }

    private fun emit(kind: SocialEvent.Kind, name: String? = null) {
        if (kind == SocialEvent.Kind.FriendAdded || kind == SocialEvent.Kind.NowFriends) telemetry.event(TelemetryEvent.FriendAdded)
        _events.tryEmit(SocialEvent(kind, name))
    }

    private fun publish() {
        _ui.value = snapshot()
    }

    private fun snapshot() = SocialUiState(
        access = when {
            userId == null -> Access.Offline
            !isMember -> Access.Anonymous
            profile?.username != null -> Access.Ready
            profileChecked -> Access.NeedsUsername
            else -> Access.Checking
        },
        username = profile?.username,
        inviteCode = profile?.inviteCode,
        section = section,
        language = language,
        scope = scope,
        kind = kind,
        friendsOnly = friendsOnly,
        speed = speed,
        streak = streak,
        friends = friends,
        loading = loading,
        stale = stale,
        failed = failed,
    )
}
