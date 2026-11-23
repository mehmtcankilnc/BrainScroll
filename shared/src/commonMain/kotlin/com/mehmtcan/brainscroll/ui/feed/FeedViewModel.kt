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
import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PagePlan
import com.mehmtcan.brainscroll.game.GameLevels
import com.mehmtcan.brainscroll.game.memory.FlipResult
import com.mehmtcan.brainscroll.game.mines.MinesResult
import com.mehmtcan.brainscroll.game.hanoi.HanoiResult
import com.mehmtcan.brainscroll.game.queens.QueensResult
import com.mehmtcan.brainscroll.game.LEVELLED_GAMES
import com.mehmtcan.brainscroll.game.arrows.ArrowsResult
import com.mehmtcan.brainscroll.game.axissum.AxisSumResult
import com.mehmtcan.brainscroll.game.ballslide.BallSlideResult
import com.mehmtcan.brainscroll.game.colorflow.FlowResult
import com.mehmtcan.brainscroll.game.fruitmath.FruitResult
import com.mehmtcan.brainscroll.game.codebreaker.CodeResult
import com.mehmtcan.brainscroll.game.fences.FenceResult
import com.mehmtcan.brainscroll.game.crosssum.CrossResult
import com.mehmtcan.brainscroll.game.mirror.MirrorResult
import com.mehmtcan.brainscroll.game.oddone.OddResult
import com.mehmtcan.brainscroll.game.greaterless.GlResult
import com.mehmtcan.brainscroll.game.lamps.LampResult
import com.mehmtcan.brainscroll.game.lonenumber.LoneResult
import com.mehmtcan.brainscroll.game.zerosones.BinResult
import com.mehmtcan.brainscroll.game.rectangles.Rect
import com.mehmtcan.brainscroll.game.colorseq.ColorSeqResult
import com.mehmtcan.brainscroll.game.cubefold.CubeResult
import com.mehmtcan.brainscroll.game.flashgrid.FlashResult
import com.mehmtcan.brainscroll.stats.ColorSeqStats
import com.mehmtcan.brainscroll.stats.CubeStats
import com.mehmtcan.brainscroll.stats.FlashStats
import com.mehmtcan.brainscroll.stats.computeColorSeqStats
import com.mehmtcan.brainscroll.stats.computeCubeStats
import com.mehmtcan.brainscroll.stats.computeFlashStats
import com.mehmtcan.brainscroll.game.rectangles.RectResult
import com.mehmtcan.brainscroll.stats.BinStats
import com.mehmtcan.brainscroll.stats.FenceStats
import com.mehmtcan.brainscroll.stats.CrossStats
import com.mehmtcan.brainscroll.stats.MirrorStats
import com.mehmtcan.brainscroll.stats.OddStats
import com.mehmtcan.brainscroll.stats.GlStats
import com.mehmtcan.brainscroll.stats.LampStats
import com.mehmtcan.brainscroll.stats.LoneStats
import com.mehmtcan.brainscroll.stats.RectStats
import com.mehmtcan.brainscroll.stats.computeBinStats
import com.mehmtcan.brainscroll.stats.computeFenceStats
import com.mehmtcan.brainscroll.stats.computeCrossStats
import com.mehmtcan.brainscroll.stats.computeMirrorStats
import com.mehmtcan.brainscroll.stats.computeOddStats
import com.mehmtcan.brainscroll.stats.computeGlStats
import com.mehmtcan.brainscroll.stats.computeLampStats
import com.mehmtcan.brainscroll.stats.computeLoneStats
import com.mehmtcan.brainscroll.stats.computeRectStats
import com.mehmtcan.brainscroll.stats.CodeStats
import com.mehmtcan.brainscroll.stats.computeCodeStats
import com.mehmtcan.brainscroll.game.lasers.LaserResult
import com.mehmtcan.brainscroll.game.untangle.UntangleResult
import com.mehmtcan.brainscroll.game.matchsticks.MatchMove
import com.mehmtcan.brainscroll.game.matchsticks.MatchResult
import com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsResult
import com.mehmtcan.brainscroll.game.nonogram.NonogramResult
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
import com.mehmtcan.brainscroll.stats.ArrowsStats
import com.mehmtcan.brainscroll.stats.AxisSumStats
import com.mehmtcan.brainscroll.stats.BallSlideStats
import com.mehmtcan.brainscroll.stats.FlowStats
import com.mehmtcan.brainscroll.stats.FruitStats
import com.mehmtcan.brainscroll.stats.computeFruitStats
import com.mehmtcan.brainscroll.stats.LaserStats
import com.mehmtcan.brainscroll.stats.UntangleStats
import com.mehmtcan.brainscroll.stats.computeUntangleStats
import com.mehmtcan.brainscroll.stats.computeLaserStats
import com.mehmtcan.brainscroll.stats.MatchStats
import com.mehmtcan.brainscroll.stats.computeMatchStats
import com.mehmtcan.brainscroll.stats.TopicGroupsStats
import com.mehmtcan.brainscroll.stats.computeTopicGroupsStats
import com.mehmtcan.brainscroll.stats.computeFlowStats
import com.mehmtcan.brainscroll.stats.computeBallSlideStats
import com.mehmtcan.brainscroll.stats.computeAxisSumStats
import com.mehmtcan.brainscroll.stats.NonogramStats
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
import com.mehmtcan.brainscroll.stats.computeArrowsStats
import com.mehmtcan.brainscroll.stats.computeNonogramStats
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
import com.mehmtcan.brainscroll.stats.Rescue
import com.mehmtcan.brainscroll.stats.RescuePolicy
import com.mehmtcan.brainscroll.game.guide.GuideQuota
import com.mehmtcan.brainscroll.game.guide.QuotaState
import com.mehmtcan.brainscroll.time.localDayIndex
import com.mehmtcan.brainscroll.ui.theme.Accent
import com.mehmtcan.brainscroll.ui.theme.Look
import com.mehmtcan.brainscroll.ui.pro.PaywallSource
import com.mehmtcan.brainscroll.ui.pro.ProController
import com.mehmtcan.brainscroll.ui.pro.StoreBilling
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import com.mehmtcan.brainscroll.stats.Stats
import com.mehmtcan.brainscroll.stats.atLevel
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
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
    /** The level the per-game statistics below are for (the daily puzzle and the totals are not about a level). */
    val statsLevel: Difficulty = Difficulty.DEFAULT,
    /** The endless word numbers at [statsLevel] (the guess bars follow the attempts of that level). */
    val endless: Stats = computeStats(emptyList()),
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
    /** Block out, from the endless feed. */
    val blockOut: BlockOutStats = computeBlockOutStats(emptyList()),
    /** Arrow escape, from the endless feed. */
    val arrows: ArrowsStats = computeArrowsStats(emptyList()),
    /** Mini nonogram, from the endless feed. */
    val nonogram: NonogramStats = computeNonogramStats(emptyList()),
    /** Word groups, from the endless feed. */
    val topicGroups: TopicGroupsStats = computeTopicGroupsStats(emptyList()),
    /** Axis sum, from the endless feed. */
    val axisSum: AxisSumStats = computeAxisSumStats(emptyList()),
    /** Ball slide, from the endless feed. */
    val ballSlide: BallSlideStats = computeBallSlideStats(emptyList()),
    /** Color flow, from the endless feed. */
    val colorFlow: FlowStats = computeFlowStats(emptyList()),
    val matchsticks: MatchStats = computeMatchStats(emptyList()),
    val lasers: LaserStats = computeLaserStats(emptyList()),
    val untangle: UntangleStats = computeUntangleStats(emptyList()),
    val fruitMath: FruitStats = computeFruitStats(emptyList()),
    val codeBreaker: CodeStats = computeCodeStats(emptyList()),
    val rectangles: RectStats = computeRectStats(emptyList()),
    val flashGrid: FlashStats = computeFlashStats(emptyList()),
    val colorSeq: ColorSeqStats = computeColorSeqStats(emptyList()),
    val cubeFold: CubeStats = computeCubeStats(emptyList()),
    val loneNumber: LoneStats = computeLoneStats(emptyList()),
    val zerosOnes: BinStats = computeBinStats(emptyList()),
    val lamps: LampStats = computeLampStats(emptyList()),
    val greaterLess: GlStats = computeGlStats(emptyList()),
    val fences: FenceStats = computeFenceStats(emptyList()),
    val mirror: MirrorStats = computeMirrorStats(emptyList()),
    val oddOne: OddStats = computeOddStats(emptyList()),
    val crossSum: CrossStats = computeCrossStats(emptyList()),
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
    private val games: List<GameKind> = listOf(GameKind.Word, GameKind.Memory, GameKind.Tubes, GameKind.Mines, GameKind.Sudoku, GameKind.Lights, GameKind.Slide, GameKind.Target, GameKind.Pipes, GameKind.Hanoi, GameKind.Queens, GameKind.Tents, GameKind.Jigsaw, GameKind.BlockOut, GameKind.Arrows, GameKind.Nonogram, GameKind.AxisSum, GameKind.BallSlide, GameKind.ColorFlow, GameKind.TopicGroups, GameKind.Matchsticks, GameKind.Lasers, GameKind.Untangle, GameKind.FruitMath, GameKind.CodeBreaker, GameKind.Rectangles, GameKind.FlashGrid, GameKind.LoneNumber, GameKind.ZerosOnes, GameKind.Lamps, GameKind.GreaterLess, GameKind.Fences, GameKind.Mirror, GameKind.OddOne, GameKind.CrossSum, GameKind.ColorSeq, GameKind.CubeFold),
    /** The games that offer a level choice. */
    val levelledGames: Set<GameKind> = LEVELLED_GAMES,
    /** The store behind Pro; null means free with nothing to buy (desktop, tests, no keys yet). */
    billing: StoreBilling? = null,
    /**
     * Whether the boards of new pages are made in the background (the app) or on the spot (tests, which look at the screen the moment
     * something is asked). In the background a page is a skeleton until its board is there, so the screen never waits for a slow board.
     */
    private val buildInBackground: Boolean = true,
) : ViewModel(), FeedActions {
    /** BrainScroll Pro. The quota, the streak rule and the look all ask it. */
    val pro = ProController(billing, repository, viewModelScope)

    /** Every game of this build, in feed order. The player can switch some of them off ([enabledGames]). */
    val availableGames: List<GameKind> = games

    private var feed: EndlessFeed? = null

    /**
     * The boards that wait to be made, one at a time and in order, on a background thread; the feed they are for goes with each.
     * Declared here, above the `init` blocks: the builder starts in one of them and must find the queue there.
     */
    private val buildQueue = Channel<Pair<EndlessFeed, PagePlan>>(Channel.UNLIMITED)
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

    private val _difficulties = MutableStateFlow(repository.savedDifficulties())

    /** The level the player chose for each game; a game that is not in the map is medium. */
    val difficulties: StateFlow<Map<GameKind, Difficulty>> = _difficulties

    /** The games the player wants in the feed (docs/decisions.md, Ana Sayfa). Always at least one. */
    val enabledGames: StateFlow<Set<GameKind>> = _enabledGames

    private val _statsLevel = MutableStateFlow(Difficulty.DEFAULT)

    /** The level the per-game statistics are shown for. */
    val statsLevel: StateFlow<Difficulty> = _statsLevel

    /** Shows the per-game statistics of [level]. */
    fun setStatsLevel(level: Difficulty) {
        if (_statsLevel.value == level) return
        _statsLevel.value = level
        refreshProfile()
    }

    private val _guide = MutableStateFlow<GuideState?>(null)

    /** The step the guide gave, with the page it is for: it only counts while that page looks the same. */
    val guide: StateFlow<GuideState?> = _guide

    private var guideSearching = false

    /** The last step given, kept after the player closes it: asking again on the same puzzle position costs nothing. */
    private var lastGuided: GuideState? = null

    /** No limit for Pro, and none where nothing can be bought (desktop, no store keys): a limit with no way out would be a wall. */
    private fun unlimitedGuide(): Boolean = pro.isPro.value || !pro.available

    private fun readQuota(): QuotaState = GuideQuota.state(localDayIndex(now()), repository.guideUse(), unlimitedGuide())

    private val _quota = MutableStateFlow(readQuota())

    /** The free guide steps left today (docs/pro-plan.md section 5), for the badge on the light bulb. */
    val quota: StateFlow<QuotaState> = _quota

    /** Looks at the clock again: a new day gives the free steps back. Called whenever the feed is shown. */
    fun refreshQuota() {
        _quota.value = readQuota()
    }

    private val _paywallRequests = MutableSharedFlow<PaywallSource>(extraBufferCapacity = 1)

    /** Something wants the purchase screen open (no free guide step left). The shell shows it. */
    val paywallRequests: SharedFlow<PaywallSource> = _paywallRequests

    /**
     * The player asks the guide for a step on page [index]. The puzzle is neutral from then on.
     *
     * The free steps are counted here and not in the feed: with none left the purchase screen is asked for and nothing is
     * searched; a step is spent only once one was found (no step found costs nothing); and a second tap while a search runs
     * is ignored, so one search is one step.
     */
    fun requestGuide(index: Int) {
        if (guideSearching) return
        val f = feed ?: return
        val page = f.snapshot().pages.getOrNull(index) ?: return
        // The same step asked for again on a puzzle that has not changed since is the step already paid for: shown again,
        // for nothing, and even with no free steps left.
        lastGuided?.takeIf { it.page.withoutClock() == page.withoutClock() }?.let {
            _guide.value = it
            return
        }
        _quota.value = readQuota()
        if (!_quota.value.canGuide) {
            _paywallRequests.tryEmit(PaywallSource.Guide)
            return
        }
        val task = f.guideTask(index) ?: return
        guideSearching = true
        viewModelScope.launch {
            try {
                // The search can take a moment on a hard position: not on the thread of the screen.
                val step = withContext(Dispatchers.Default) { task() }
                if (step == null) {
                    _guide.value = null
                    _guideNothing.tryEmit(Unit)
                    return@launch
                }
                repository.setGuideUse(GuideQuota.spend(localDayIndex(now()), repository.guideUse(), unlimitedGuide()))
                _quota.value = readQuota()
                f.markGuided(index)
                publish()
                _guide.value = GuideState(page, step).also { lastGuided = it }
            } finally {
                guideSearching = false
            }
        }
    }

    /**
     * The "bring your streak back" offer, if the player should see it now (docs/pro-plan.md section 7.3). Asking does not count
     * as showing it: call [markRescueShown] when the screen really opens.
     */
    fun rescueOffer(): Rescue? = if (!pro.available) null else RescuePolicy.offer(
        finishedDays = dailyFinishedDays(),
        today = IstanbulDay.dayIndex(now()),
        pro = pro.isPro.value,
        shownBreak = repository.rescueShownFor(),
        autoPaywallDay = repository.autoPaywallDay(),
        localToday = localDayIndex(now()),
    )

    /** The rescue screen was shown: not again for this break, and no other paywall by itself today. */
    fun markRescueShown() {
        RescuePolicy.breakOf(dailyFinishedDays())?.let(repository::setRescueShownFor)
        repository.setAutoPaywallDay(localDayIndex(now()))
    }

    private fun dailyFinishedDays(): List<Long> = repository.history().filter { it.mode == Mode.DAILY }.map { it.dayIndex }

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

    private val _look = MutableStateFlow(Look(repository.savedAccent(), repository.savedAmoled()))

    /** The accent color and pure black the player picked. Whether they apply depends on Pro: see [Look.forPro]. */
    val look: StateFlow<Look> = _look

    fun setAccent(accent: Accent) {
        repository.saveAccent(accent)
        _look.value = _look.value.copy(accent = accent)
    }

    fun setAmoled(on: Boolean) {
        repository.saveAmoled(on)
        _look.value = _look.value.copy(amoled = on)
    }

    private val _soundEnabled = MutableStateFlow(repository.soundEnabled())
    val soundEnabled: StateFlow<Boolean> = _soundEnabled

    fun setSoundEnabled(enabled: Boolean) {
        repository.setSoundEnabled(enabled)
        _soundEnabled.value = enabled
    }

    fun setAppLanguage(language: AppLanguage?) {
        repository.saveAppLanguage(language)
        _appLanguage.value = language
    }

    private val _wordLanguages = MutableStateFlow(listOf((repository.savedWordLanguages() ?: listOf(resolvedAppLanguage)).first()))

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
            difficulties = _difficulties.value.filterKeys { it in levelledGames },
            buildInBackground = buildInBackground,
        ).also { it.ensureSize(maxOf(AHEAD, restored.size + 1)) }
    }

    /** Builds the feed from what the device remembers: unfinished puzzles, the streak, the favorites. */
    private suspend fun loadFeed() {
        wordLists = Language.entries.associateWith { loadWordList(it) }
        // A page of a game that is no longer in the feed (One Line is the daily puzzle only) is left behind.
        val restored = repository.unfinishedRounds().filter { it.game in games }
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
            // The same level as the favorite: a hard board is played, and counted, as a hard one.
            difficulty = round.difficulty,
        )
        // A puzzle played again is not a new answer of the flow: its win does not raise the number in the bar.
        feed = buildFeed(listOf(copy)).also { it.markAsReplay(copy.id) }
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
                GameKind.Tubes, GameKind.Mines, GameKind.Sudoku, GameKind.Lights, GameKind.Slide, GameKind.Target, GameKind.Pipes, GameKind.Hanoi, GameKind.Queens, GameKind.Tents, GameKind.Jigsaw, GameKind.BlockOut, GameKind.Arrows, GameKind.Nonogram, GameKind.AxisSum, GameKind.BallSlide, GameKind.ColorFlow, GameKind.TopicGroups, GameKind.Matchsticks, GameKind.Lasers, GameKind.Untangle, GameKind.FruitMath, GameKind.CodeBreaker, GameKind.Rectangles, GameKind.FlashGrid, GameKind.LoneNumber, GameKind.ZerosOnes, GameKind.Lamps, GameKind.GreaterLess, GameKind.Fences, GameKind.Mirror, GameKind.OddOne, GameKind.CrossSum, GameKind.ColorSeq, GameKind.CubeFold, GameKind.Line -> mapOf("outcome" to result.outcome.name, "moves" to result.moveCount.toString())
            }
            // The time in seconds (anonymous, like the rest) is what lets the levels be balanced by how long boards really take.
            val seconds = result.durationMs?.let { mapOf("seconds" to (it / 1000).toString()) }.orEmpty()
            telemetry.event(mode, props + ("game" to result.game.id) + ("difficulty" to result.difficulty.id.toString()) + seconds)
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
        // Pro changed (bought, restored, refunded): the free steps, the streak and the look follow.
        viewModelScope.launch { pro.isPro.drop(1).collect { refreshQuota(); refreshProfile() } }
        // The purchase follows the player when the account changes (guest to Apple sign-in, a new guest after deleting).
        viewModelScope.launch { signedInUser.filterNotNull().distinctUntilChanged().collect { pro.identify(it) } }
        startBuilder()
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

    /** Taps an arrow of the arrow escape board on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun tapArrow(page: Int, cell: Int): ArrowsResult {
        val f = feed ?: return ArrowsResult.Ignored
        val result = f.tapArrow(page, cell)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == ArrowsResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Taps a number of the axis sum board on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun tapAxisSum(page: Int, cell: Int): AxisSumResult {
        val f = feed ?: return AxisSumResult.Ignored
        val result = f.tapAxisSum(page, cell)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == AxisSumResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Slides the ball of the ball slide board on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun slideBall(page: Int, dir: Int): BallSlideResult {
        val f = feed ?: return BallSlideResult.Ignored
        val result = f.slideBall(page, dir)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == BallSlideResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** A stroke begins on [cell] of the color flow board on [page]. */
    override fun startFlow(page: Int, cell: Int): Boolean {
        val f = feed ?: return false
        return f.startFlow(page, cell).also { publish() }
    }

    /** The finger of the stroke on [page] moves on to [cell]: the paths are drawn as it goes. */
    override fun extendFlow(page: Int, cell: Int): Boolean {
        val f = feed ?: return false
        return f.extendFlow(page, cell).also { if (it) publish() }
    }

    /** The finger of the stroke on [page] is lifted. Returns what the stroke did, so the screen can buzz accordingly. */
    override fun endFlow(page: Int): FlowResult {
        val f = feed ?: return FlowResult.Ignored
        val result = f.endFlow(page)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == FlowResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** The answer [answer] is given for the last line of the fruit equations board on [page]. */
    override fun submitFruit(page: Int, answer: Int): FruitResult {
        val f = feed ?: return FruitResult.Ignored
        val result = f.submitFruit(page, answer)
        publish()
        // A board that was won is a new result: the statistics and the cloud backup follow.
        if (result == FruitResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Start is pressed on the flash grid on [page]. */
    override fun startFlash(page: Int): Boolean {
        val f = feed ?: return false
        return f.startFlash(page).also { if (it) publish() }
    }

    /** The flash grid on [page] is started over with other tiles. */
    override fun restartFlash(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartFlash(page).also { if (it) publish() }
    }

    /** Tile [tile] of the flash grid on [page] is touched. */
    override fun tapFlashTile(page: Int, tile: Int): FlashResult {
        val f = feed ?: return FlashResult.Ignored
        val result = f.tapFlashTile(page, tile)
        publish()
        // A run that is over (cleared to the end, or lost on a wrong tile) is a new result: the statistics and the cloud backup follow.
        if (result == FlashResult.Perfect || result == FlashResult.Missed) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** The cube at [pick] of the cube folding question on [page] is picked. */
    override fun pickCube(page: Int, pick: Int): CubeResult {
        val f = feed ?: return CubeResult.Ignored
        val result = f.pickCube(page, pick)
        publish()
        // A run that is over (answered to the end, or lost on a wrong pick) is a new result: the statistics and the cloud backup follow.
        if (result == CubeResult.Perfect || result == CubeResult.Wrong) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Start is pressed on the color sequence on [page]. */
    override fun startColorSeq(page: Int): Boolean {
        val f = feed ?: return false
        return f.startColorSeq(page).also { if (it) publish() }
    }

    /** The color sequence on [page] is started over with other lights. */
    override fun restartColorSeq(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartColorSeq(page).also { if (it) publish() }
    }

    /** Button [button] of the color sequence on [page] is touched. */
    override fun tapColorButton(page: Int, button: Int): ColorSeqResult {
        val f = feed ?: return ColorSeqResult.Ignored
        val result = f.tapColorButton(page, button)
        publish()
        // A run that is over (cleared to the end, or lost on a wrong button) is a new result: the statistics and the cloud backup follow.
        if (result == ColorSeqResult.Perfect || result == ColorSeqResult.Missed) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** [rect] is laid on the rectangles board on [page]. */
    override fun placeRect(page: Int, rect: Rect): RectResult {
        val f = feed ?: return RectResult.Ignored
        val result = f.placeRect(page, rect)
        publish()
        // A board that was covered is a new result: the statistics and the cloud backup follow.
        if (result == RectResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** The edge number [edge] of the fences board on [page] is tapped. */
    override fun tapFenceEdge(page: Int, edge: Int): FenceResult {
        val f = feed ?: return FenceResult.Ignored
        val result = f.tapFence(page, edge)
        if (result != FenceResult.Ignored) publish()
        // A board that was solved is a new result: the statistics and the cloud backup follow.
        if (result == FenceResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Every mark of the fences board on [page] is taken off. */
    override fun restartFences(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartFences(page).also { if (it) publish() }
    }

    /** The cell number [cell] of the mirror board on [page] is tapped. */
    override fun tapMirrorCell(page: Int, cell: Int): MirrorResult {
        val f = feed ?: return MirrorResult.Ignored
        val result = f.tapMirror(page, cell)
        if (result != MirrorResult.Ignored) publish()
        // A board that was solved is a new result: the statistics and the cloud backup follow.
        if (result == MirrorResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** The half to fill of the mirror board on [page] is emptied. */
    override fun restartMirror(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartMirror(page).also { if (it) publish() }
    }

    /** The digit [digit] (0 empties the cell) is put in the cell number [cell] of the cross sums board on [page]. */
    override fun putCrossDigit(page: Int, cell: Int, digit: Int): CrossResult {
        val f = feed ?: return CrossResult.Ignored
        val result = f.putCross(page, cell, digit)
        if (result != CrossResult.Ignored) publish()
        // A board that was solved is a new result: the statistics and the cloud backup follow.
        if (result == CrossResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Every digit the player put on the cross sums board on [page] is taken out. */
    override fun restartCross(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartCross(page).also { if (it) publish() }
    }

    /** The number at [pick] of the odd one out question on [page] is picked. */
    override fun pickOdd(page: Int, pick: Int): OddResult {
        val f = feed ?: return OddResult.Ignored
        val result = f.pickOdd(page, pick)
        publish()
        // A run that is over (answered to the end, or lost on a wrong pick) is a new result: the statistics and the cloud backup follow.
        if (result == OddResult.Perfect || result == OddResult.Wrong) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** The number [value] (0 empties the cell) is put in the cell number [cell] of the greater and less board on [page]. */
    override fun putGlNumber(page: Int, cell: Int, value: Int): GlResult {
        val f = feed ?: return GlResult.Ignored
        val result = f.putGl(page, cell, value)
        if (result != GlResult.Ignored) publish()
        // A board that was solved is a new result: the statistics and the cloud backup follow.
        if (result == GlResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Every number the player put on the greater and less board on [page] is taken off. */
    override fun restartGl(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartGl(page).also { if (it) publish() }
    }

    /** The cell number [cell] of the lamps board on [page] is tapped. */
    override fun tapLampCell(page: Int, cell: Int): LampResult {
        val f = feed ?: return LampResult.Ignored
        val result = f.tapLamp(page, cell)
        if (result != LampResult.Ignored) publish()
        // A board that was solved is a new result: the statistics and the cloud backup follow.
        if (result == LampResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Every mark of the lamps board on [page] is taken off. */
    override fun restartLamps(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartLamps(page).also { if (it) publish() }
    }

    /** The cell number [cell] of the zeros and ones board on [page] is tapped. */
    override fun tapBinCell(page: Int, cell: Int): BinResult {
        val f = feed ?: return BinResult.Ignored
        val result = f.tapBin(page, cell)
        if (result != BinResult.Ignored) publish()
        // A board that was solved is a new result: the statistics and the cloud backup follow.
        if (result == BinResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Every digit the player put on the zeros and ones board on [page] is taken off. */
    override fun restartBin(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartBin(page).also { if (it) publish() }
    }

    /** The cell number [cell] of the lone number board on [page] is tapped. */
    override fun tapLoneCell(page: Int, cell: Int): LoneResult {
        val f = feed ?: return LoneResult.Ignored
        val result = f.tapLone(page, cell)
        if (result != LoneResult.Ignored && result != LoneResult.Rejected) publish()
        // A board that was solved is a new result: the statistics and the cloud backup follow.
        if (result == LoneResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Every mark of the lone number board on [page] is taken off. */
    override fun restartLone(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartLone(page).also { if (it) publish() }
    }

    /** The rectangle over the cell ([column], [row]) of the board on [page] is taken off. */
    override fun removeRect(page: Int, column: Int, row: Int): Boolean {
        val f = feed ?: return false
        return f.removeRect(page, column, row).also { if (it) publish() }
    }

    /** Every rectangle of the board on [page] is taken off. */
    override fun restartRects(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartRects(page).also { if (it) publish() }
    }

    /** [guess] is given on the code breaker board on [page]. */
    override fun submitCode(page: Int, guess: List<Int>): CodeResult {
        val f = feed ?: return CodeResult.Ignored
        val result = f.submitCode(page, guess)
        publish()
        // A code that was cracked is a new result: the statistics and the cloud backup follow.
        if (result == CodeResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Dot [node] of the tangle on [page] is dragged to ([x], [y]): the lines follow as it goes. */
    override fun moveNode(page: Int, node: Int, x: Int, y: Int): Boolean {
        val f = feed ?: return false
        return f.moveNode(page, node, x, y).also { if (it) publish() }
    }

    /** The finger of the drag on [page] is lifted. Returns what the drag did, so the screen can buzz accordingly. */
    override fun dropNode(page: Int): UntangleResult {
        val f = feed ?: return UntangleResult.Ignored
        val result = f.dropNode(page)
        publish()
        // A board that was won is a new result: the statistics and the cloud backup follow.
        if (result == UntangleResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Puts the dots of the tangle on [page] back where it was given. */
    override fun restartUntangle(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartUntangle(page).also { publish() }
    }

    /** The mirror on [cell] of the laser board on [page] is turned. */
    override fun turnMirror(page: Int, cell: Int): LaserResult {
        val f = feed ?: return LaserResult.Ignored
        val result = f.turnMirror(page, cell)
        publish()
        // A board that was won is a new result: the statistics and the cloud backup follow.
        if (result == LaserResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Puts every mirror of the laser board on [page] back as it was given. */
    override fun restartLasers(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartLasers(page).also { publish() }
    }

    /** A stick of the matchstick sum on [page] is taken and laid on another place. */
    override fun moveStick(page: Int, move: MatchMove): MatchResult {
        val f = feed ?: return MatchResult.Ignored
        val result = f.moveStick(page, move)
        publish()
        // A sum that was put right is a new result: the statistics and the cloud backup follow.
        if (result == MatchResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Puts the stick of the matchstick sum on [page] back. */
    override fun undoStick(page: Int): Boolean {
        val f = feed ?: return false
        return f.undoStick(page).also { publish() }
    }

    /** Clears the paths of the color flow board on [page]. */
    override fun restartFlow(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartFlow(page).also { publish() }
    }

    /** Takes the last slide of the ball slide board on [page] back. */
    override fun undoBall(page: Int): Boolean {
        val f = feed ?: return false
        return f.undoBall(page).also { publish() }
    }

    /** Puts the ball of the ball slide board on [page] on its start again. */
    override fun restartBall(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartBall(page).also { publish() }
    }

    /** Brings the numbers of the axis sum board on [page] back. */
    override fun restartAxisSum(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartAxisSum(page).also { publish() }
    }

    /** Puts the arrows of the arrow escape board on [page] back. */
    override fun restartArrows(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartArrows(page).also { publish() }
    }

    /** Taps a word of the word groups board on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun tapTopicWord(page: Int, word: Int): TopicGroupsResult {
        val f = feed ?: return TopicGroupsResult.Ignored
        val result = f.tapTopicWord(page, word)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == TopicGroupsResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Mixes up the words of the word groups board on [page]. */
    override fun shuffleTopicWords(page: Int): Boolean {
        val f = feed ?: return false
        return f.shuffleTopicWords(page).also { publish() }
    }

    /** Puts the picked words of the word groups board on [page] back. */
    override fun clearTopicSelection(page: Int): Boolean {
        val f = feed ?: return false
        return f.clearTopicSelection(page).also { publish() }
    }

    /** Paints a stroke of cells on the nonogram board on [page]. Returns what happened, so the screen can buzz accordingly. */
    override fun paintNonogram(page: Int, from: Int, to: Int, value: Int): NonogramResult {
        val f = feed ?: return NonogramResult.Ignored
        val result = f.paintNonogram(page, from, to, value)
        publish()
        // A board that just ended is a new result: the statistics and the cloud backup follow.
        if (result == NonogramResult.Solved) {
            refreshProfile()
            requestSyncIfNeeded()
        }
        return result
    }

    /** Clears the nonogram board on [page]. */
    override fun restartNonogram(page: Int): Boolean {
        val f = feed ?: return false
        return f.restartNonogram(page).also { publish() }
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
        act {
            it.ensureSize(page + AHEAD)
            it.pageShown(page)
        }
    }

    /** The feed came back to the screen (the tab was opened again, the app came back): the clock of the page that is on show goes on. */
    fun onFeedResumed() {
        act { it.pageShown(page) }
    }

    /** The feed left the screen (another tab, the app in the background): the clock of the page that was on show stops. */
    fun onFeedPaused() {
        act { it.pagesHidden() }
    }

    /**
     * Switches a game on or off in the feed. The last game that is on cannot be switched off. Applies to pages that are
     * still to come; puzzles that were started are left alone.
     */
    /** Switches a whole list of games on at once (the editor's "select all"): one save, one change of the feed. Games that were on stay on. */
    fun addGames(games: Collection<GameKind>) {
        val next = _enabledGames.value + games.filter { it in availableGames }
        if (next == _enabledGames.value) return
        _enabledGames.value = next
        repository.saveGames(next)
        act { it.setEnabledGames(next, fromIndex = page) }
    }

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

    /** The one level of the whole feed: the level every game has, or medium when the games were set apart (an older version let each have its own). */
    fun feedDifficulty(): Difficulty =
        levelledGames.map { _difficulties.value[it] ?: Difficulty.DEFAULT }.distinct().singleOrNull() ?: Difficulty.DEFAULT

    /** Sets the level of every game that has levels at once: the feed has one level. Applies to pages still to come. */
    fun setFeedDifficulty(difficulty: Difficulty) {
        val changed = levelledGames.filter { (_difficulties.value[it] ?: Difficulty.DEFAULT) != difficulty }
        if (changed.isEmpty()) return
        _difficulties.value = _difficulties.value + changed.associateWith { difficulty }
        changed.forEach { repository.saveDifficulty(it, difficulty) }
        // One change of the feed, not one for each game.
        act { feed -> changed.forEach { feed.setDifficulty(it, difficulty, fromIndex = page) } }
    }

    /**
     * Sets the level of the boards of [game] (only for a game that has levels). Applies to pages that are still to come;
     * puzzles that were started are left alone.
     */
    fun setDifficulty(game: GameKind, difficulty: Difficulty) {
        if (game !in levelledGames || (_difficulties.value[game] ?: Difficulty.DEFAULT) == difficulty) return
        _difficulties.value = _difficulties.value + (game to difficulty)
        repository.saveDifficulty(game, difficulty)
        act { it.setDifficulty(game, difficulty, fromIndex = page) }
    }

    /**
     * Sets the language of the word puzzles that come from now on. An untouched word page on show is drawn again in the new
     * language; a puzzle that was started goes on in the language it began in.
     */
    fun setWordLanguage(language: Language) {
        if (_wordLanguages.value == listOf(language)) return
        _wordLanguages.value = listOf(language)
        repository.saveWordLanguages(listOf(language))
        act { it.setWordLanguage(language, fromIndex = page) }
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
        val level = _statsLevel.value
        val atLevel = history.atLevel(level)
        _profile.value = ProfileState(
            statsLevel = level,
            stats = computeStats(history),
            endless = computeStats(atLevel, maxAttempts = GameLevels.word(level).attempts),
            favorites = repository.favoriteResults(),
            backupPending = repository.pendingCount(),
            daily = computeStats(history, mode = Mode.DAILY),
            memory = computeMemoryStats(atLevel),
            tubes = computeTubesStats(atLevel),
            mines = computeMinesStats(atLevel),
            sudoku = computeSudokuStats(atLevel),
            jigsaw = computeJigsawStats(atLevel),
            lights = computeLightsStats(atLevel),
            slide = computeSlideStats(atLevel),
            target = computeTargetStats(atLevel),
            pipes = computePipesStats(atLevel),
            line = computeLineStats(atLevel),
            hanoi = computeHanoiStats(atLevel),
            queens = computeQueensStats(atLevel),
            blockOut = computeBlockOutStats(atLevel),
            arrows = computeArrowsStats(atLevel),
            axisSum = computeAxisSumStats(atLevel),
            ballSlide = computeBallSlideStats(atLevel),
            colorFlow = computeFlowStats(atLevel),
            matchsticks = computeMatchStats(atLevel),
            lasers = computeLaserStats(atLevel),
            untangle = computeUntangleStats(atLevel),
            fruitMath = computeFruitStats(atLevel),
            codeBreaker = computeCodeStats(atLevel),
            rectangles = computeRectStats(atLevel),
            flashGrid = computeFlashStats(atLevel),
            colorSeq = computeColorSeqStats(atLevel),
            cubeFold = computeCubeStats(atLevel),
            loneNumber = computeLoneStats(atLevel),
            zerosOnes = computeBinStats(atLevel),
            lamps = computeLampStats(atLevel),
            greaterLess = computeGlStats(atLevel),
            fences = computeFenceStats(atLevel),
            mirror = computeMirrorStats(atLevel),
            oddOne = computeOddStats(atLevel),
            crossSum = computeCrossStats(atLevel),
            nonogram = computeNonogramStats(atLevel),
            topicGroups = computeTopicGroupsStats(atLevel),
            tents = computeTentsStats(atLevel),
            dailyByLanguage = Language.entries.associateWith { computeStats(history, language = it, mode = Mode.DAILY) },
            dayStreak = DayStreaks.compute(
                finishedDays = history.filter { it.mode == Mode.DAILY }.map { it.dayIndex },
                today = today,
                pro = pro.isPro.value,
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

    private fun startBuilder() {
        viewModelScope.launch { for ((f, plan) in buildQueue) build(f, plan) }
    }

    private suspend fun build(f: EndlessFeed, plan: PagePlan) {
        // The feed was left (a new one was made) while this waited: nobody wants the board.
        if (feed !== f) return
        var current = plan
        var round: FeedRound? = null
        // A generator that gives up (it has a limit) is asked again with the next seed, so a page does not wait for ever.
        for (attempt in 0 until BUILD_TRIES) {
            round = try {
                withContext(Dispatchers.Default) { f.build(current) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (round != null) break
            current = current.copy(seed = current.seed + 1)
        }
        if (round != null && feed === f && f.fill(plan, round)) publish()
    }

    /** Hands the boards that the feed has still to make to the background builder. */
    private fun scheduleBuilds() {
        val f = feed ?: return
        for (plan in f.takePlans()) buildQueue.trySend(f to plan)
    }

    private fun publish() {
        scheduleBuilds()
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
        const val AHEAD = 3

        /** How many seeds are tried for a page whose generator failed before it is given up. */
        const val BUILD_TRIES = 3

        /** How many pages are looked at, at most, for one of a game that is on (only started puzzles of a game that is off are passed). */
        const val MAX_SCAN = 100
    }
}

/** A step of the guide and the page it was asked for. When the page changes, the step is out of date. */
data class GuideState(val page: PageSnapshot, val step: GuideStep)
