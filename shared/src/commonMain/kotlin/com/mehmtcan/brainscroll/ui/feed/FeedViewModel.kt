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
import com.mehmtcan.brainscroll.game.hanoi.HanoiResult
import com.mehmtcan.brainscroll.game.queens.QueensResult
import com.mehmtcan.brainscroll.game.crossword.CrosswordResult
import com.mehmtcan.brainscroll.game.crossword.CrosswordStatus
import com.mehmtcan.brainscroll.game.blockout.BlockOutResult
import com.mehmtcan.brainscroll.game.tents.TentsResult
import com.mehmtcan.brainscroll.game.lights.LightsResult
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.line.LineResult
import com.mehmtcan.brainscroll.game.pipes.PipesResult
import com.mehmtcan.brainscroll.game.slide.SlideResult
import com.mehmtcan.brainscroll.game.sudoku.SudokuResult
import com.mehmtcan.brainscroll.game.target.Op
import com.mehmtcan.brainscroll.game.target.TargetResult
import com.mehmtcan.brainscroll.game.tubes.PourResult
import com.mehmtcan.brainscroll.game.wordle.FeedSnapshot
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import kotlin.random.Random
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.StoredRound
import com.mehmtcan.brainscroll.game.wordle.WordList
import com.mehmtcan.brainscroll.ui.i18n.AppLanguage
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.loadWordList
import com.mehmtcan.brainscroll.stats.DayStreak
import com.mehmtcan.brainscroll.stats.MemoryStats
import com.mehmtcan.brainscroll.stats.LightsStats
import com.mehmtcan.brainscroll.stats.MinesStats
import com.mehmtcan.brainscroll.stats.HanoiStats
import com.mehmtcan.brainscroll.stats.QueensStats
import com.mehmtcan.brainscroll.stats.CrosswordStats
import com.mehmtcan.brainscroll.stats.BlockOutStats
import com.mehmtcan.brainscroll.stats.TentsStats
import com.mehmtcan.brainscroll.stats.LineStats
import com.mehmtcan.brainscroll.stats.PipesStats
import com.mehmtcan.brainscroll.stats.SlideStats
import com.mehmtcan.brainscroll.stats.SudokuStats
import com.mehmtcan.brainscroll.stats.TargetStats
import com.mehmtcan.brainscroll.stats.TubesStats
import com.mehmtcan.brainscroll.stats.computeLightsStats
import com.mehmtcan.brainscroll.stats.computeMinesStats
import com.mehmtcan.brainscroll.stats.computeHanoiStats
import com.mehmtcan.brainscroll.stats.computeQueensStats
import com.mehmtcan.brainscroll.stats.computeCrosswordStats
import com.mehmtcan.brainscroll.stats.computeBlockOutStats
import com.mehmtcan.brainscroll.stats.computeTentsStats
import com.mehmtcan.brainscroll.stats.computeLineStats
import com.mehmtcan.brainscroll.stats.computePipesStats
import com.mehmtcan.brainscroll.stats.computeSlideStats
import com.mehmtcan.brainscroll.stats.computeSudokuStats
import com.mehmtcan.brainscroll.stats.computeJigsawStats
import com.mehmtcan.brainscroll.stats.computeTargetStats
import com.mehmtcan.brainscroll.stats.computeMemoryStats
import com.mehmtcan.brainscroll.stats.computeTubesStats
import com.mehmtcan.brainscroll.stats.DayStreaks
import com.mehmtcan.brainscroll.stats.Stats
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.GuideStep

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
    /** Sudoku, from the endless feed. */
    val sudoku: SudokuStats = computeSudokuStats(emptyList()),
    /** Jigsaw sudoku, from the endless feed. */
    val jigsaw: SudokuStats = computeJigsawStats(emptyList()),
    /** Lights Out, from the endless feed. */
    val lights: LightsStats = computeLightsStats(emptyList()),
    /** Sliding puzzle, from the endless feed. */
    val slide: SlideStats = computeSlideStats(emptyList()),
    /** Target number, from the endless feed. */
    val target: TargetStats = computeTargetStats(emptyList()),
    /** Pipes, from the endless feed. */
    val pipes: PipesStats = computePipesStats(emptyList()),
    /** One Line, from the daily puzzle. */
    val line: LineStats = computeLineStats(emptyList()),
    /** Tower of Hanoi, from the endless feed. */
    val hanoi: HanoiStats = computeHanoiStats(emptyList()),
    /** Queens, from the endless feed. */
    val queens: QueensStats = computeQueensStats(emptyList()),
    /** Crossword, from the endless feed. */
    val crossword: CrosswordStats = computeCrosswordStats(emptyList()),
    /** Block out, from the endless feed. */
    val blockOut: BlockOutStats = computeBlockOutStats(emptyList()),
    /** Tents, from the endless feed. */
    val tents: TentsStats = computeTentsStats(emptyList()),
    /** The same, for each language alone. */
    val dailyByLanguage: Map<Language, Stats> = emptyMap(),
    val dayStreak: DayStreak = DayStreak(current = 0, best = 0, freezesAvailable = 0),
    /** The most puzzles solved in one feed so far (kept on this device only). */
    val bestFlow: Int = 0,
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
    private val games: List<GameKind> = listOf(GameKind.Word, GameKind.Memory, GameKind.Tubes, GameKind.Mines, GameKind.Sudoku, GameKind.Lights, GameKind.Slide, GameKind.Target, GameKind.Pipes, GameKind.Hanoi, GameKind.Queens, GameKind.Tents, GameKind.Jigsaw, GameKind.BlockOut, GameKind.Line),
) : ViewModel(), FeedActions {
    /** Every game of this build, in feed order. The player can switch some of them off ([enabledGames]). */
    val availableGames: List<GameKind> = games

    private var feed: EndlessFeed? = null
    private var favoriteIds: Set<String> = emptySet()

    private val _syncTick = MutableStateFlow(0)

    /** Goes up every time a sync brought the cloud's state in, so screens that keep their own copy (the daily puzzle) look again. */
    val syncTick: StateFlow<Int> = _syncTick

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state

    /** The most puzzles solved in one feed so far. */
    private var bestFlow = repository.bestFlow()

    private val _profile = MutableStateFlow(ProfileState(computeStats(emptyList()), emptyList(), bestFlow = bestFlow))
    val profile: StateFlow<ProfileState> = _profile

    private val _enabledGames = MutableStateFlow(games.toSet())

    /** The games the player wants in the feed (docs/decisions.md, Ana Sayfa). Always at least one. */
    val enabledGames: StateFlow<Set<GameKind>> = _enabledGames

    private val _guide = MutableStateFlow<GuideState?>(null)

    /** The step the guide gave, with the page it is for: it only counts while that page looks the same. */
    val guide: StateFlow<GuideState?> = _guide

    /** The player asks the guide for a step on page [index]. The puzzle is neutral from then on. */
    fun requestGuide(index: Int) {
        val f = feed ?: return
        val page = f.snapshot().pages.getOrNull(index) ?: return
        val task = f.guideTask(index) ?: return
        viewModelScope.launch {
            // The search can take a moment on a hard position: not on the thread of the screen.
            val step = withContext(Dispatchers.Default) { task() }
            if (step == null) {
                _guide.value = null
                _guideNothing.tryEmit(Unit)
                return@launch
            }
            f.markGuided(index)
            publish()
            _guide.value = GuideState(page, step)
        }
    }

    fun clearGuide() {
        _guide.value = null
    }

    private val _guideNothing = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** The guide had nothing to say for the page the player asked about. */
    val guideNothing: SharedFlow<Unit> = _guideNothing

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

    private val _appLanguage = MutableStateFlow(repository.savedAppLanguage())

    /** The language of the app's texts the player chose, or null to follow the device. */
    val appLanguage: StateFlow<AppLanguage?> = _appLanguage

    /** The language the app's texts are in now: the chosen one, or the device's. */
    val resolvedAppLanguage: Language get() = _appLanguage.value?.wordLanguage ?: startLanguage

    fun setAppLanguage(language: AppLanguage?) {
        repository.saveAppLanguage(language)
        _appLanguage.value = language
    }

    private val _wordLanguages = MutableStateFlow(repository.savedWordLanguages() ?: listOf(resolvedAppLanguage))

    /** The languages of the word puzzles in the feed: at least one. Until the player chooses, the app language. */
    val wordLanguages: StateFlow<List<Language>> = _wordLanguages

    private var wordLists: Map<Language, WordList> = emptyMap()

    /** A feed of the games the player switched on, in the languages chosen, starting with [restored] puzzles. */
    private fun buildFeed(restored: List<StoredRound>, startSolved: Int = 0): EndlessFeed {
        val chosen = _enabledGames.value
        return EndlessFeed(
            wordLists = wordLists,
            startLanguage = _wordLanguages.value.first(),
            startLanguages = _wordLanguages.value,
            store = repository,
            restored = restored,
            startSolved = startSolved,
            now = now,
            games = games.filter { it in chosen },
        ).also { it.ensureSize(maxOf(AHEAD, restored.size + 1)) }
    }

    /** Builds the feed from what the device remembers: unfinished puzzles, the streak, the favorites. */
    private suspend fun loadFeed() {
        wordLists = Language.entries.associateWith { loadWordList(it) }
        val restored = repository.unfinishedRounds()
        val chosen = repository.savedGames()?.filter { it in games }?.takeIf { it.isNotEmpty() } ?: games
        _enabledGames.value = chosen.toSet()
        feed = buildFeed(restored)
        favoriteIds = repository.favoriteResults().map { it.id }.toSet()
        page = 0
        publish()
        refreshProfile()
    }

    /**
     * Starts a new feed from the games and languages the player chose: whatever was left unfinished in the old one is
     * forgotten, and the feed begins on its first page. The count of solved puzzles starts again from zero, unless this
     * is the same flow going on ([keepSolvedCount]).
     */
    fun startNewFeed(keepSolvedCount: Boolean = false) {
        val old = feed ?: return
        repository.clearFeedProgress()
        feed = buildFeed(emptyList(), startSolved = if (keepSolvedCount) old.solvedCount else 0)
        page = 0
        _guide.value = null
        publish()
    }

    /**
     * Plays a puzzle from the history again (a favorite): a new feed whose first page is that very puzzle, untouched and
     * with a fresh id, so its new result is a result of its own. Whatever was left unfinished in the old feed is forgotten.
     * Minesweeper keeps its seed, but the mines are placed after the first tap, so the board can differ.
     */
    fun playAgain(round: FinishedRound) {
        if (feed == null) return
        repository.clearFeedProgress()
        val copy = StoredRound(
            id = Random.nextLong().toULong().toString(16),
            language = round.language,
            answer = round.answer,
            guesses = emptyList(),
            input = "",
            position = 0,
            game = round.game,
        )
        feed = buildFeed(listOf(copy))
        page = 0
        _guide.value = null
        publish()
    }

    /** Goes on with the feed that was left: opens it on the first puzzle that was started and not finished. */
    fun continueFeed() {
        val f = feed ?: return
        prepareFeed()
        f.firstUnfinishedIndex()?.let {
            page = it
            publish()
        }
    }

    /**
     * The feed tab was opened. The feed is kept when the puzzle it was left on is not finished (started or not), and it
     * goes on at a puzzle that was started and left half done, wherever that is. When the last puzzle is finished and
     * nothing is half done, there is nothing to go on with: a new feed is made from the choices of the player.
     */
    fun openFeedTab() {
        val f = feed ?: return
        val leftOn = f.snapshot().pages.getOrNull(page)
        when {
            leftOn != null && !leftOn.isFinished && f.firstUnfinishedIndex() == null -> prepareFeed() // kept, on the same page
            f.firstUnfinishedIndex() != null -> continueFeed()
            // The last puzzle is done and nothing is half done: the player goes on playing, so the count goes on too.
            else -> startNewFeed(keepSolvedCount = true)
        }
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
                GameKind.Tubes, GameKind.Mines, GameKind.Sudoku, GameKind.Lights, GameKind.Slide, GameKind.Target, GameKind.Pipes, GameKind.Hanoi, GameKind.Queens, GameKind.Tents, GameKind.Jigsaw, GameKind.BlockOut, GameKind.Crossword, GameKind.Line -> mapOf("outcome" to result.outcome.name, "moves" to result.moveCount.toString())
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

    override fun type(page: Int, letter: Char) = act { it.type(page, letter) }

    override fun backspace(page: Int) = act { it.backspace(page) }

    /** Turns over a card of the memory board on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun flip(page: Int, card: Int): FlipResult {
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
    override fun closeMismatch(page: Int) {
        feed?.closeMismatch(page)
        publish()
    }

    /** Pours tube [from] into tube [to] on the tubes board of [page]. Returns what happened, so the screen can buzz. */
    override fun pour(page: Int, from: Int, to: Int): PourResult {
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
    override fun openCell(page: Int, cell: Int): MinesResult {
        val f = feed ?: return MinesResult.Ignored
        return f.openCell(page, cell).also { afterMinesMove(it, page) }
    }

    /** Flags or unflags a cell of the minesweeper board on [page]. */
    override fun flagCell(page: Int, cell: Int): Boolean {
        val f = feed ?: return false
        return f.flagCell(page, cell).also { publish() }
    }

    /** Chords on an open number of the minesweeper board on [page]. */
    override fun chordCell(page: Int, cell: Int): MinesResult {
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

    /** Taps a light of the Lights Out board on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun tapLight(page: Int, cell: Int): LightsResult {
        val f = feed ?: return LightsResult.Ignored
        val result = f.tapLight(page, cell)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == LightsResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Puts the Lights Out board on [page] back as it was at the start. */
    override fun restartLights(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartLights(page).also { publish() }
    }

    /** Slides the tile in [cell] of the sliding puzzle on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun slideTile(page: Int, cell: Int): SlideResult {
        val f = feed ?: return SlideResult.Ignored
        val result = f.slideTile(page, cell)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == SlideResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Puts the sliding puzzle on [page] back as it was at the start. */
    override fun restartSlide(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartSlide(page).also { publish() }
    }

    /** Combines two numbers of the target number game on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun combineNumbers(page: Int, firstId: Int, op: Op, secondId: Int): TargetResult {
        val f = feed ?: return TargetResult.Ignored
        val result = f.combineNumbers(page, firstId, op, secondId)
        publish()
        // A game that just ended is a new result: the statistics and the cloud backup follow.
        if (result == TargetResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** What the operation would give, or null if it is not allowed. Used to dim numbers that do not fit. */
    override fun previewNumbers(page: Int, firstId: Int, op: Op, secondId: Int): Int? = feed?.previewNumbers(page, firstId, op, secondId)

    /** Takes back the last operation of the target number game on [page]. */
    override fun undoNumbers(page: Int): Boolean {
        val f = feed ?: return false
        return f.undoNumbers(page).also { publish() }
    }

    /** Puts the target number game on [page] back as it was at the start. */
    override fun restartNumbers(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartNumbers(page).also { publish() }
    }

    /** Turns a tile of the pipes board on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun turnPipe(page: Int, cell: Int): PipesResult {
        val f = feed ?: return PipesResult.Ignored
        val result = f.turnPipe(page, cell)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == PipesResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Puts the pipes board on [page] back as it was at the start. */
    override fun restartPipes(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartPipes(page).also { publish() }
    }

    /** Moves a disk of the Hanoi board on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun moveDisk(page: Int, from: Int, to: Int): HanoiResult {
        val f = feed ?: return HanoiResult.Ignored
        val result = f.moveDisk(page, from, to)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == HanoiResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Puts the Hanoi disks on [page] back as they were at the start. */
    override fun restartHanoi(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartHanoi(page).also { publish() }
    }

    /** Taps a cell of the queens board on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun toggleQueen(page: Int, cell: Int): QueensResult {
        val f = feed ?: return QueensResult.Ignored
        val result = f.toggleQueen(page, cell)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == QueensResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Takes away the queens the player put on [page]. */
    override fun restartQueens(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartQueens(page).also { publish() }
    }

    /** Taps a cell of the tents board on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun tapTent(page: Int, cell: Int): TentsResult {
        val f = feed ?: return TentsResult.Ignored
        val result = f.tapTent(page, cell)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == TentsResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Clears the marks of the tents board on [page]. */
    override fun restartTents(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartTents(page).also { publish() }
    }

    /** Slides a piece of the block out board on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun slideBlock(page: Int, piece: Int, to: Int): BlockOutResult {
        val f = feed ?: return BlockOutResult.Ignored
        val result = f.slideBlock(page, piece, to)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == BlockOutResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Puts the pieces of the block out board on [page] back as they were at the start. */
    override fun restartBlockOut(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartBlockOut(page).also { publish() }
    }

    /** Puts a letter in a square of the crossword on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun placeLetter(page: Int, cell: Int, letter: Char): CrosswordResult {
        val f = feed ?: return CrosswordResult.Ignored
        val result = f.placeLetter(page, cell, letter)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == CrosswordResult.Correct && (f.snapshot().pages.getOrNull(page) as? com.mehmtcan.brainscroll.game.crossword.CrosswordSnapshot)?.state?.status == CrosswordStatus.Won) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Takes back a wrong letter of the crossword on [page]. */
    override fun eraseLetter(page: Int, cell: Int): CrosswordResult {
        val f = feed ?: return CrosswordResult.Ignored
        return f.eraseLetter(page, cell).also { publish() }
    }

    // The one-line game is the daily puzzle and not part of the feed; these only keep the feed able to play it.
    override fun dragLine(page: Int, cell: Int): LineResult = feed?.dragLine(page, cell).also { publish() } ?: LineResult.Ignored
    override fun tapLine(page: Int, cell: Int): LineResult = feed?.tapLine(page, cell).also { publish() } ?: LineResult.Ignored
    override fun undoLine(page: Int): Boolean = feed?.undoLine(page).also { publish() } ?: false
    override fun restartLine(page: Int): Boolean = feed?.restartLine(page).also { publish() } ?: false
    override fun hintLine(page: Int): List<Int>? = feed?.hintLine(page).also { publish() }

    /** Puts a digit in a cell of the sudoku board on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun placeDigit(page: Int, cell: Int, digit: Int): SudokuResult {
        val f = feed ?: return SudokuResult.Ignored
        return f.placeDigit(page, cell, digit).also { afterSudokuMove(it, page) }
    }

    /** Switches a pencil note on or off in a cell of the sudoku board on [page]. */
    override fun toggleNote(page: Int, cell: Int, digit: Int): SudokuResult {
        val f = feed ?: return SudokuResult.Ignored
        return f.toggleNote(page, cell, digit).also { publish() }
    }

    /** Takes back a wrong digit, or the notes, of a cell of the sudoku board on [page]. */
    override fun eraseCell(page: Int, cell: Int): SudokuResult {
        val f = feed ?: return SudokuResult.Ignored
        return f.eraseCell(page, cell).also { publish() }
    }

    private fun afterSudokuMove(result: SudokuResult, page: Int) {
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result != SudokuResult.Ignored && (_state.value as FeedUiState.Ready).feed.pages[page].isFinished) {
            refreshProfile()
            requestSyncIfNeeded()
        }
    }

    /** Takes back the last pour of the tubes board on [page]. */
    override fun undo(page: Int): Boolean {
        val f = feed ?: return false
        return f.undo(page).also { publish() }
    }

    /** Puts the tubes board on [page] back as it was at the start. */
    override fun restart(page: Int): Boolean {
        val f = feed ?: return false
        return f.restart(page).also { publish() }
    }

    override fun submit(page: Int) {
        act { it.submit(page) }
        refreshProfile()
        requestSyncIfNeeded()
    }

    /** Called when the pager settles on [page]: keeps the current and the next puzzle ready. */
    fun onPageSettled(page: Int) {
        this.page = page
        act { it.ensureSize(page + AHEAD) }
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

    /**
     * Called before the feed is shown. The feed opens on the page it was left on; if that page belongs to a game that is
     * switched off now (a puzzle that was started before, which is kept), it opens on the next page of a game that is on.
     * The page that was passed over stays where it is: the player can still swipe back to it.
     */
    fun prepareFeed() {
        val f = feed ?: return
        val on = _enabledGames.value
        var target = page
        var scanned = 0
        while (scanned++ < MAX_SCAN) {
            f.ensureSize(target + 1)
            if (f.snapshot().pages[target].kind in on) break
            target++
        }
        page = target
        f.ensureSize(target + AHEAD)
        publish()
    }

    /** Switches a language of the word puzzles on or off. The last language that is on stays. Applies to puzzles still to come. */
    fun setWordLanguageEnabled(language: Language, enabled: Boolean) {
        val current = _wordLanguages.value
        val next = Language.entries.filter { if (it == language) enabled else it in current }
        if (next.isEmpty() || next == current) return
        _wordLanguages.value = next
        repository.saveWordLanguages(next)
        act { it.setWordLanguages(next.toSet(), fromIndex = page) }
    }

    /** Hearts or un-hearts a finished puzzle. Its result is saved the moment it ends, so the id always exists. */
    override fun toggleFavorite(resultId: String) {
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
            sudoku = computeSudokuStats(history),
            jigsaw = computeJigsawStats(history),
            lights = computeLightsStats(history),
            slide = computeSlideStats(history),
            target = computeTargetStats(history),
            pipes = computePipesStats(history),
            line = computeLineStats(history),
            hanoi = computeHanoiStats(history),
            queens = computeQueensStats(history),
            crossword = computeCrosswordStats(history),
            blockOut = computeBlockOutStats(history),
            tents = computeTentsStats(history),
            dailyByLanguage = Language.entries.associateWith { computeStats(history, language = it, mode = Mode.DAILY) },
            dayStreak = DayStreaks.compute(
                finishedDays = history.filter { it.mode == Mode.DAILY }.map { it.dayIndex },
                today = today,
            ),
            dailyDoneToday = history.any { it.mode == Mode.DAILY && it.dayIndex == today },
            bestFlow = bestFlow,
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
        _syncTick.value += 1
        favoriteIds = repository.favoriteResults().map { it.id }.toSet()
        if (feed != null) publish()
        refreshProfile()
    }

    private fun act(action: (EndlessFeed) -> Unit) {
        val f = feed ?: return
        action(f)
        publish()
    }

    private fun publish() {
        val snapshot = feed!!.snapshot()
        // A new record is kept at once (on this device only).
        if (snapshot.solvedCount > bestFlow) {
            bestFlow = snapshot.solvedCount
            repository.saveBestFlow(bestFlow)
            _profile.value = _profile.value.copy(bestFlow = bestFlow)
        }
        _state.value = FeedUiState.Ready(snapshot, favoriteIds)
    }

    private companion object {
        /** Current puzzle plus the next one (docs/design.md section 8). */
        const val AHEAD = 2

        /** How many pages are looked at, at most, for one of a game that is on (only started puzzles of a game that is off are passed). */
        const val MAX_SCAN = 100
    }
}

/** A step of the guide and the page it was asked for. When the page changes, the step is out of date. */
data class GuideState(val page: PageSnapshot, val step: GuideStep)
