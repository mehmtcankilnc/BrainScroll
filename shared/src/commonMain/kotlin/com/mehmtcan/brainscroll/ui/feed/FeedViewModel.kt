package com.mehmtcan.brainscroll.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmtcan.brainscroll.account.AccountEvent
import com.mehmtcan.brainscroll.account.AccountState
import com.mehmtcan.brainscroll.account.DeepLinkInbox
import com.mehmtcan.brainscroll.account.MergeResult
import com.mehmtcan.brainscroll.account.MergeTicket
import com.mehmtcan.brainscroll.social.SocialApi
import com.mehmtcan.brainscroll.telemetry.CrashHandler
import com.mehmtcan.brainscroll.telemetry.PlatformInfo
import com.mehmtcan.brainscroll.telemetry.Telemetry
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.telemetry.TelemetryRecorder
import com.mehmtcan.brainscroll.social.parseInviteCode
import com.mehmtcan.brainscroll.cloud.CloudServicesFactory
import com.mehmtcan.brainscroll.daily.DailyApi
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.memory.FlipResult
import com.mehmtcan.brainscroll.game.mines.MinesResult
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.tubes.PourResult
import com.mehmtcan.brainscroll.game.wordle.FeedSnapshot
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.loadWordList
import com.mehmtcan.brainscroll.stats.DayStreak
import com.mehmtcan.brainscroll.stats.MemoryStats
import com.mehmtcan.brainscroll.stats.MinesStats
import com.mehmtcan.brainscroll.stats.TubesStats
import com.mehmtcan.brainscroll.stats.computeMinesStats
import com.mehmtcan.brainscroll.stats.computeMemoryStats
import com.mehmtcan.brainscroll.stats.computeTubesStats
import com.mehmtcan.brainscroll.stats.DayStreaks
import com.mehmtcan.brainscroll.stats.Stats
import com.mehmtcan.brainscroll.stats.Streaks
import com.mehmtcan.brainscroll.stats.computeStats
import com.mehmtcan.brainscroll.sync.SyncCoordinator
import com.mehmtcan.brainscroll.time.IstanbulDay
import com.mehmtcan.brainscroll.sync.SyncEngine
import kotlinx.coroutines.flow.MutableSharedFlow
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
    /** Statistics of the daily puzzle (both languages together). */
    val daily: Stats = computeStats(emptyList(), mode = Mode.DAILY),
    /** Memory match, from the endless feed. */
    val memory: MemoryStats = computeMemoryStats(emptyList()),
    /** Color tubes, from the endless feed. */
    val tubes: TubesStats = computeTubesStats(emptyList()),
    /** Minesweeper, from the endless feed. */
    val mines: MinesStats = computeMinesStats(emptyList()),
    /** The same, for each language alone. */
    val dailyByLanguage: Map<Language, Stats> = emptyMap(),
    val dayStreak: DayStreak = DayStreak(current = 0, best = 0, freezesAvailable = 0),
    /** A daily puzzle (either language) was finished today. */
    val dailyDoneToday: Boolean = false,
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
    /** Platform and app version for the anonymous reports. */
    platformInfo: PlatformInfo = PlatformInfo("desktop", "dev"),
    /** The games of the feed, in the order they come (docs/decisions.md: one after the other). */
    private val games: List<GameKind> = listOf(GameKind.Word, GameKind.Memory, GameKind.Tubes, GameKind.Mines),
) : ViewModel() {
    /** Every game of this build, in feed order. The player can switch some of them off ([enabledGames]). */
    val availableGames: List<GameKind> = games

    private var feed: EndlessFeed? = null
    private var favoriteIds: Set<String> = emptySet()

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state

    private val _profile = MutableStateFlow(ProfileState(computeStats(emptyList()), emptyList()))
    val profile: StateFlow<ProfileState> = _profile

    private val _enabledGames = MutableStateFlow(games.toSet())

    /** The games the player wants in the feed (docs/decisions.md, Ana Sayfa). Always at least one. */
    val enabledGames: StateFlow<Set<GameKind>> = _enabledGames

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

    /** The leaderboards and friends, on the same Supabase client. */
    val socialApi: SocialApi = services.social

    private val _inviteCodes = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 4)

    /** Friend invite codes that arrived through an invite link. Kept until the friends screen listens. */
    val inviteCodes: SharedFlow<String> = _inviteCodes

    /** Anonymous usage counts and crash reports. The daily, social and shell code use this same one. */
    val telemetry: Telemetry = TelemetryRecorder(
        repository = repository,
        api = services.telemetry,
        info = platformInfo,
        scope = viewModelScope,
        signedIn = signedIn,
    ).also { it.start() }

    private val sync = SyncCoordinator(
        scope = viewModelScope,
        engine = SyncEngine(repository, services.cloud),
        signedInUser = signedInUser,
        onSynced = ::afterSync,
    )

    private val startLanguage = deviceLanguage

    /** Builds the feed from what the device remembers: unfinished puzzles, the streak, the favorites. */
    private suspend fun loadFeed() {
        val lists = Language.entries.associateWith { loadWordList(it) }
        val restored = repository.unfinishedRounds()
        val chosen = repository.savedGames()?.filter { it in games }?.takeIf { it.isNotEmpty() } ?: games
        _enabledGames.value = chosen.toSet()
        feed = EndlessFeed(
            wordLists = lists,
            startLanguage = repository.savedLanguage() ?: startLanguage,
            store = repository,
            restored = restored,
            startStreak = Streaks.current(repository.history()),
            now = now,
            games = games.filter { it in chosen },
        ).also { it.ensureSize(maxOf(AHEAD, restored.size + 1)) }
        favoriteIds = repository.favoriteResults().map { it.id }.toSet()
        page = 0
        publish()
        refreshProfile()
    }

    init {
        telemetry.event(TelemetryEvent.AppOpen)
        // Errors nobody caught are kept on the device and sent at the next start.
        CrashHandler.install { crash -> telemetry.recordCrash(crash) }
        repository.onFinished = { result ->
            val mode = if (result.mode == Mode.DAILY) TelemetryEvent.DailyFinished else TelemetryEvent.EndlessFinished
            val props = when (result.game) {
                GameKind.Word -> mapOf("outcome" to result.outcome.name, "guesses" to result.guesses.size.toString(), "language" to result.language.name)
                // Memory has no language; its guesses are card flips, two to a move.
                GameKind.Memory -> mapOf("outcome" to result.outcome.name, "moves" to result.moveCount.toString())
                GameKind.Tubes, GameKind.Mines -> mapOf("outcome" to result.outcome.name, "moves" to result.moveCount.toString())
            }
            telemetry.event(mode, props + ("game" to result.game.id))
        }
        viewModelScope.launch {
            account.events.collect { event ->
                when (event) {
                    AccountEvent.SignedIn -> telemetry.event(TelemetryEvent.SignedIn, mapOf("provider" to providerName()))
                    AccountEvent.AccountDeleted -> telemetry.event(TelemetryEvent.AccountDeleted)
                    else -> Unit
                }
            }
        }
        viewModelScope.launch { loadFeed() }
        viewModelScope.launch { account.state.collect { handleMerge(it) } }
        viewModelScope.launch { account.start() }
        viewModelScope.launch {
            // The browser (Google) or the system hands the app a login link; the account service reads it.
            DeepLinkInbox.urls.collect { url ->
                val invite = parseInviteCode(url)
                if (invite != null) _inviteCodes.tryEmit(invite) else account.handleCallbackUrl(url)
                DeepLinkInbox.consumed()
            }
        }
        sync.start()
    }

    fun type(page: Int, letter: Char) = act { it.type(page, letter) }

    fun backspace(page: Int) = act { it.backspace(page) }

    /** Turns over a card of the memory board on [page]. Returns what happened, so the screen can buzz accordingly. */
    fun flip(page: Int, card: Int): FlipResult {
        val f = feed ?: return FlipResult.Ignored
        val result = f.flip(page, card)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result != FlipResult.Ignored && (_state.value as FeedUiState.Ready).feed.pages[page].isFinished) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** The two wrong cards of the memory board on [page] turn back. */
    fun closeMismatch(page: Int) {
        feed?.closeMismatch(page)
        publish()
    }

    /** Pours tube [from] into tube [to] on the tubes board of [page]. Returns what happened, so the screen can buzz. */
    fun pour(page: Int, from: Int, to: Int): PourResult {
        val f = feed ?: return PourResult.Ignored
        val result = f.pour(page, from, to)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result != PourResult.Ignored && (_state.value as FeedUiState.Ready).feed.pages[page].isFinished) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Opens a cell of the minesweeper board on [page]. Returns what happened, so the screen can buzz accordingly. */
    fun openCell(page: Int, cell: Int): MinesResult {
        val f = feed ?: return MinesResult.Ignored
        return f.openCell(page, cell).also { afterMinesMove(it, page) }
    }

    /** Flags or unflags a cell of the minesweeper board on [page]. */
    fun flagCell(page: Int, cell: Int): Boolean {
        val f = feed ?: return false
        return f.flagCell(page, cell).also { publish() }
    }

    /** Chords on an open number of the minesweeper board on [page]. */
    fun chordCell(page: Int, cell: Int): MinesResult {
        val f = feed ?: return MinesResult.Ignored
        return f.chordCell(page, cell).also { afterMinesMove(it, page) }
    }

    private fun afterMinesMove(result: MinesResult, page: Int) {
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result != MinesResult.Ignored && (_state.value as FeedUiState.Ready).feed.pages[page].isFinished) {
            refreshProfile()
            requestSyncIfNeeded()
        }
    }

    /** Takes back the last pour of the tubes board on [page]. */
    fun undo(page: Int): Boolean {
        val f = feed ?: return false
        return f.undo(page).also { publish() }
    }

    /** Puts the tubes board on [page] back as it was at the start. */
    fun restart(page: Int): Boolean {
        val f = feed ?: return false
        return f.restart(page).also { publish() }
    }

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

    /**
     * Switches a game on or off in the feed. The last game that is on cannot be switched off. Applies to pages that are
     * still to come; puzzles that were started are left alone.
     */
    fun setGameEnabled(game: GameKind, enabled: Boolean) {
        val next = if (enabled) _enabledGames.value + game else _enabledGames.value - game
        if (next.isEmpty() || game !in availableGames || next == _enabledGames.value) return
        _enabledGames.value = next
        repository.saveGames(next)
        act { it.setEnabledGames(next, fromIndex = page) }
    }

    /** The language of the word puzzles from now on. Puzzles already started keep theirs. */
    fun setWordLanguage(language: Language) {
        repository.saveLanguage(language)
        act { it.setWordLanguage(language, fromIndex = page) }
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
        val history = repository.history()
        val today = IstanbulDay.dayIndex(now())
        _profile.value = ProfileState(
            stats = computeStats(history),
            favorites = repository.favoriteResults(),
            backupPending = repository.pendingCount(),
            daily = computeStats(history, mode = Mode.DAILY),
            memory = computeMemoryStats(history),
            tubes = computeTubesStats(history),
            mines = computeMinesStats(history),
            dailyByLanguage = Language.entries.associateWith { computeStats(history, language = it, mode = Mode.DAILY) },
            dayStreak = DayStreaks.compute(
                finishedDays = history.filter { it.mode == Mode.DAILY }.map { it.dayIndex },
                today = today,
            ),
            dailyDoneToday = history.any { it.mode == Mode.DAILY && it.dayIndex == today },
        )
    }

    /** Called by the daily puzzle screen after it changed something that should be backed up (a favorite). */
    fun requestSync() = requestSyncIfNeeded()

    fun signInWithGoogle() {
        viewModelScope.launch {
            prepareMerge()
            account.signInWithGoogle()
        }
    }

    fun signInWithApple() {
        viewModelScope.launch {
            prepareMerge()
            account.signInWithApple()
        }
    }

    /**
     * Signing in from an anonymous account may end in an account that already exists (it was used on another phone).
     * Then the anonymous account is left behind with its daily results, which only the server can move. So a ticket
     * is asked for now, while still anonymous, and kept until the sign-in is done ([handleMerge]).
     */
    private suspend fun prepareMerge() {
        val state = account.state.value as? AccountState.SignedIn ?: return
        if (state.kind != AccountState.SignedIn.Kind.Anonymous) return
        val token = services.merge.start() ?: return // offline: signing in cannot work either, nothing is lost
        repository.putMergeTicket(MergeTicket(fromUser = state.userId, token = token))
    }

    /** After a sign-in: if a ticket is waiting and the account is another one than the anonymous one, hand it over. */
    private suspend fun handleMerge(state: AccountState) {
        val signedIn = state as? AccountState.SignedIn ?: return
        if (signedIn.kind == AccountState.SignedIn.Kind.Anonymous) return
        val ticket = repository.mergeTicket() ?: return
        if (ticket.fromUser == signedIn.userId) {
            repository.clearMergeTicket() // the identity was linked to the same account: nothing to move
            return
        }
        when (services.merge.complete(ticket.token)) {
            MergeResult.Done -> {
                repository.clearMergeTicket()
                requestSyncIfNeeded()
            }
            MergeResult.Invalid -> repository.clearMergeTicket()
            MergeResult.Retry -> Unit // kept: tried again at the next start or sign-in
        }
    }

    /** The player came back, or has been looking at the feed for a while: a long pause means a new session. */
    fun refreshSession() {
        val f = feed ?: return
        if (f.startNewSessionIfIdle()) publish()
    }

    private fun providerName(): String = when ((account.state.value as? AccountState.SignedIn)?.kind) {
        AccountState.SignedIn.Kind.Google -> "google"
        AccountState.SignedIn.Kind.Apple -> "apple"
        else -> "unknown"
    }

    fun signOut() {
        viewModelScope.launch { account.signOut() }
    }

    /**
     * Deletes the account on the server, then everything on the device, then starts over with a fresh feed and a new
     * anonymous account. If the server cannot be reached nothing is touched.
     */
    fun deleteAccount() {
        viewModelScope.launch {
            if (!account.deleteAccount()) return@launch
            repository.wipeLocalData()
            loadFeed()
            account.start()
        }
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
