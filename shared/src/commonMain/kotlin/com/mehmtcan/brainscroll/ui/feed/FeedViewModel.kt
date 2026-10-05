package com.mehmtcan.brainscroll.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmtcan.brainscroll.account.AccountEvent
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.account.DeepLinkInbox
import com.mehmtcan.brainscroll.cloud.CloudServicesFactory
import com.mehmtcan.brainscroll.daily.DailyApi
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.FeedSnapshot
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.loadWordList
import com.mehmtcan.brainscroll.stats.Stats
import com.mehmtcan.brainscroll.stats.Streaks
import com.mehmtcan.brainscroll.stats.computeStats
import com.mehmtcan.brainscroll.sync.SyncCoordinator
import com.mehmtcan.brainscroll.sync.SyncEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Ready(
        val feed: FeedSnapshot,
        /** Ids of the finished puzzles the player marked with the heart. */
        val favoriteIds: Set<String>,
    ) : FeedUiState
}

/** What the profile tab shows. */
data class ProfileState(
    val stats: Stats,
    val favorites: List<FinishedRound>,
    /** How many results and favorites still wait to be uploaded to the cloud. */
    val backupPending: Int = 0,
)

/**
 * Owns the [EndlessFeed] so it survives screen rotation and recomposition (a ViewModel lives longer
 * than the composables). The screen sends actions and observes [state]; every action re-publishes a snapshot.
 *
 * It also connects the feed to the database: at start it restores unfinished puzzles, the answer streak
 * and the chosen language, and from then on the feed saves its progress through the [repository].
 * The account and the cloud backup run in the background; the game never waits for them.
 */
class FeedViewModel(
    /** Shared with the daily puzzle screen, so both write to the same database. */
    val repository: GameRepository,
    deviceLanguage: Language,
    private val now: () -> Long,
    cloudServices: CloudServicesFactory,
) : ViewModel() {
    private var feed: EndlessFeed? = null
    private var favoriteIds: Set<String> = emptySet()

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state

    private val _profile = MutableStateFlow(ProfileState(computeStats(emptyList()), emptyList()))
    val profile: StateFlow<ProfileState> = _profile

    /** The page the feed was on, so switching tabs and coming back does not jump to the first puzzle. */
    var page: Int = 0
        private set

    // --- Account and cloud backup ---

    private val services = cloudServices(viewModelScope)
    private val account = services.account

    val accountState: StateFlow<AccountState> = account.state
    val accountEvents: SharedFlow<AccountEvent> = account.events
    val canSignInWithApple: Boolean get() = account.canSignInWithApple

    private val signedInUser: StateFlow<String?> = account.state
        .map { (it as? AccountState.SignedIn)?.userId?.takeIf { id -> id.isNotEmpty() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** The daily puzzle screen needs an account to know who is playing. */
    val signedIn: StateFlow<Boolean> = signedInUser
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** The daily puzzle on the server, created together with the account so both use one Supabase client. */
    val dailyApi: DailyApi = services.daily

    private val sync = SyncCoordinator(
        scope = viewModelScope,
        engine = SyncEngine(repository, services.cloud),
        signedInUser = signedInUser,
        onSynced = ::afterSync,
    )

    init {
        viewModelScope.launch {
            val lists = Language.entries.associateWith { loadWordList(it) }
            val restored = repository.unfinishedRounds()
            feed = EndlessFeed(
                wordLists = lists,
                startLanguage = repository.savedLanguage() ?: deviceLanguage,
                store = repository,
                restored = restored,
                startStreak = Streaks.current(repository.history()),
                now = now,
            ).also { it.ensureSize(maxOf(AHEAD, restored.size + 1)) }
            favoriteIds = repository.favoriteResults().map { it.id }.toSet()
            publish()
            refreshProfile()
        }
        viewModelScope.launch { account.start() }
        viewModelScope.launch {
            // The browser (Google) or the system hands the app a login link; the account service reads it.
            DeepLinkInbox.urls.collect { url ->
                account.handleCallbackUrl(url)
                DeepLinkInbox.consumed()
            }
        }
        sync.start()
    }

    fun type(page: Int, letter: Char) = act { it.type(page, letter) }

    fun backspace(page: Int) = act { it.backspace(page) }

    fun submit(page: Int) {
        act { it.submit(page) }
        refreshProfile()
        requestSyncIfNeeded()
    }

    /** Called when the pager settles on [page]: keeps the current and the next puzzle ready. */
    fun onPageSettled(page: Int) {
        this.page = page
        act { it.ensureSize(page + AHEAD) }
    }

    /** Asks to leave [page] forward. Returns false when that is not allowed (the screen blocks the swipe). */
    fun leave(page: Int): Boolean {
        val f = feed ?: return false
        val allowed = f.leave(page)
        publish()
        return allowed
    }

    fun setLanguage(language: Language) {
        act { if (it.setLanguage(language)) repository.saveLanguage(language) }
    }

    /** Hearts or un-hearts a finished puzzle. Its result is saved the moment it ends, so the id always exists. */
    fun toggleFavorite(resultId: String) {
        val nowFavorite = resultId !in favoriteIds
        repository.setFavorite(resultId, nowFavorite, now())
        favoriteIds = if (nowFavorite) favoriteIds + resultId else favoriteIds - resultId
        publish()
        refreshProfile()
        requestSyncIfNeeded()
    }

    fun refreshProfile() {
        _profile.value = ProfileState(
            stats = computeStats(repository.history()),
            favorites = repository.favoriteResults(),
            backupPending = repository.pendingCount(),
        )
    }

    fun signInWithGoogle() {
        viewModelScope.launch { account.signInWithGoogle() }
    }

    fun signInWithApple() {
        viewModelScope.launch { account.signInWithApple() }
    }

    fun signOut() {
        viewModelScope.launch { account.signOut() }
    }

    private fun requestSyncIfNeeded() {
        if (repository.pendingCount() > 0) sync.request()
    }

    /** The cloud may have brought in results and favorites from another device: reload what depends on them. */
    private fun afterSync() {
        favoriteIds = repository.favoriteResults().map { it.id }.toSet()
        feed?.setAnswerStreak(Streaks.current(repository.history()))
        if (feed != null) publish()
        refreshProfile()
    }

    private fun act(action: (EndlessFeed) -> Unit) {
        val f = feed ?: return
        action(f)
        publish()
    }

    private fun publish() {
        _state.value = FeedUiState.Ready(feed!!.snapshot(), favoriteIds)
    }

    private companion object {
        /** Current puzzle plus the next one (docs/design.md section 8). */
        const val AHEAD = 2
    }
}
