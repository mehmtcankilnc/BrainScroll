package com.mehmtcan.brainscroll.data

import com.mehmtcan.brainscroll.account.MergeTicket
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.db.Game_result
import com.mehmtcan.brainscroll.db.In_progress_round
import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideUse
import com.mehmtcan.brainscroll.ui.pro.HintCache
import com.mehmtcan.brainscroll.ui.pro.ProCache
import com.mehmtcan.brainscroll.ui.theme.Accent
import com.mehmtcan.brainscroll.game.wordle.FeedStore
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound
import com.mehmtcan.brainscroll.game.daily.DAILY_GAME_ID_PREFIX
import com.mehmtcan.brainscroll.time.IstanbulDay
import com.mehmtcan.brainscroll.ui.i18n.AppLanguage

internal const val DATABASE_FILE = "brainscroll.db"

private const val SETTING_LANGUAGE = "word_language"
private const val SETTING_WORD_LANGUAGES = "word_languages"
private const val SETTING_APP_LANGUAGE = "app_language"
private const val SETTING_SYNCED_USER = "synced_user_id"
private const val SETTING_TELEMETRY = "telemetry_enabled"
private const val SETTING_SOUND = "sound_enabled"
private const val SETTING_GAMES = "feed_games"
private const val SETTING_DIFFICULTY_PREFIX = "feed_difficulty_"
private const val SETTING_PENDING_CRASH = "pending_crash"
private const val SETTING_MERGE_TICKET = "merge_ticket"
private const val SETTING_DAILY_SENT = "daily_sent:"
private const val SETTING_BEST_FLOW = "best_flow"
private const val SETTING_GUIDE_QUOTA_DAY = "guide_quota_day"
private const val SETTING_GUIDE_QUOTA_USED = "guide_quota_used"
private const val SETTING_PRO_CACHED = "pro_cached"
private const val SETTING_HOME_TOUR_SEEN = "home_tour_seen"
private const val SETTING_HINT_BALANCE = "hint_balance_cache"
private const val SETTING_RESCUE_SHOWN = "streak_rescue_shown"
private const val SETTING_AUTO_PAYWALL_DAY = "auto_paywall_day"
private const val SETTING_THEME_ACCENT = "theme_accent"
private const val SETTING_THEME_AMOLED = "theme_amoled"

/**
 * Reads and writes everything the app keeps on the device. It is the only place that knows the tables;
 * the rest of the app works with plain Kotlin types ([FinishedRound], [StoredRound]).
 */
class GameRepository(database: BrainScrollDatabase) : FeedStore, ProCache, HintCache {
    private val db = database
    private val results = database.gameResultQueries
    private val progress = database.inProgressRoundQueries
    private val favorites = database.favoriteQueries
    private val settings = database.settingQueries
    private val queue = database.syncQueueQueries

    // --- Feed progress (FeedStore) ---

    override fun saveProgress(round: StoredRound) {
        progress.upsert(
            id = round.id,
            game = round.game.id,
            mode = Mode.ENDLESS.name,
            language = round.language.name,
            answer = round.answer,
            guesses = round.guesses.joinToString(","),
            input = round.input,
            was_skipped = if (round.assisted) 1 else 0, // the column was "skipped" once; it now says the guide was used
            position = round.position.toLong(),
            difficulty = round.difficulty.id.toLong(),
        )
    }

    override fun deleteProgress(id: String) {
        progress.deleteById(id)
    }

    /**
     * Moves a round from "unfinished" to the history in one transaction, so it can never be in both or neither.
     * The result is also put in the upload queue in that same step, so it cannot be saved and forgotten.
     */
    override fun finish(result: FinishedRound) {
        db.transaction {
            insertResultRow(result)
            progress.deleteById(result.id)
            if (result.mode == Mode.ENDLESS) queue.enqueue(KIND_RESULT, result.id)
        }
        onFinished?.invoke(result)
    }

    /** Told about every puzzle that ends, endless or daily, after it was saved. Used for the anonymous counts. */
    var onFinished: ((FinishedRound) -> Unit)? = null

    private fun insertResultRow(result: FinishedRound) {
        results.insertResult(
            id = result.id,
            game = result.game.id,
            mode = result.mode.name,
            language = result.language.name,
            answer = result.answer,
            guesses = result.guesses.joinToString(","),
            outcome = result.outcome.name,
            was_skipped = if (result.assisted) 1 else 0,
            finished_at = result.finishedAt,
            day_index = result.dayIndex,
            duration_ms = result.durationMs,
            difficulty = result.difficulty.id.toLong(),
        )
    }

    /** Rows of a game this version of the app does not know (written by a newer one) are left out. */
    fun unfinishedRounds(): List<StoredRound> =
        progress.selectAll().executeAsList().mapNotNull { it.toStoredOrNull() }.filterNot { it.id.startsWith(DAILY_GAME_ID_PREFIX) }

    /** Forgets every unfinished puzzle of the feed (the daily puzzle of the day is not part of it). */
    fun clearFeedProgress() {
        db.transaction {
            progress.selectAll().executeAsList().filterNot { it.id.startsWith(DAILY_GAME_ID_PREFIX) }.forEach { progress.deleteById(it.id) }
        }
    }

    /** The unfinished daily puzzle with this id (see [dailyGameId]), if there is one. The feed does not see these. */
    fun unfinishedDailyRound(id: String): StoredRound? =
        progress.selectAll().executeAsList().firstOrNull { it.id == id }?.toStoredOrNull()

    /** True once the server was told the result of a daily puzzle of another game than the word puzzle. */
    fun dailyResultSent(id: String): Boolean = settings.get(SETTING_DAILY_SENT + id).executeAsOneOrNull() == "1"

    fun markDailyResultSent(id: String) {
        settings.put(SETTING_DAILY_SENT + id, "1")
    }

    // --- History ---

    /** Every finished puzzle, oldest first. */
    fun history(): List<FinishedRound> = results.selectAll().executeAsList().mapNotNull { it.toFinishedOrNull() }

    // --- Favorites ---

    fun setFavorite(resultId: String, favorite: Boolean, now: Long) {
        db.transaction {
            favorites.setFavorite(resultId, if (favorite) 1 else 0, now)
            queue.enqueue(KIND_FAVORITE, resultId)
        }
    }

    fun isFavorite(resultId: String): Boolean =
        favorites.isFavorite(resultId).executeAsOneOrNull() == 1L

    /** Favorite puzzles, newest first. */
    fun favoriteResults(): List<FinishedRound> =
        favorites.selectFavoriteResults().executeAsList().mapNotNull {
            FinishedRound(
                game = GameKind.fromId(it.game) ?: return@mapNotNull null,
                id = it.id,
                mode = Mode.valueOf(it.mode),
                language = Language.valueOf(it.language),
                answer = it.answer,
                guesses = it.guesses.toGuessList(),
                outcome = Outcome.valueOf(it.outcome),
                assisted = it.was_skipped == 1L,
                finishedAt = it.finished_at,
                durationMs = it.duration_ms,
                puzzleDay = it.day_index.takeIf { day -> day != IstanbulDay.dayIndex(it.finished_at) },
                difficulty = Difficulty.fromId(it.difficulty.toInt()),
            )
        }

    // --- Settings ---

    /** The first-run tour of the home page was shown (or skipped). */
    fun homeTourSeen(): Boolean = settings.get(SETTING_HOME_TOUR_SEEN).executeAsOneOrNull() == "1"

    fun markHomeTourSeen() {
        settings.put(SETTING_HOME_TOUR_SEEN, "1")
    }

    /**
     * The languages of the word puzzles in the feed (one or more), or null if the player never chose (then the app
     * language is used). Also reads the single language an older version saved.
     */
    fun savedWordLanguages(): List<Language>? {
        val stored = settings.get(SETTING_WORD_LANGUAGES).executeAsOneOrNull() ?: settings.get(SETTING_LANGUAGE).executeAsOneOrNull()
        val names = stored?.split(",") ?: return null
        return Language.entries.filter { it.name in names }.takeIf { it.isNotEmpty() }
    }

    fun saveWordLanguages(languages: Collection<Language>) {
        settings.put(SETTING_WORD_LANGUAGES, languages.joinToString(",") { it.name })
    }

    /** The language of the app's texts the player chose, or null to follow the device. */
    fun savedAppLanguage(): AppLanguage? = AppLanguage.fromTag(settings.get(SETTING_APP_LANGUAGE).executeAsOneOrNull())

    fun saveAppLanguage(language: AppLanguage?) {
        if (language == null) settings.deleteKey(SETTING_APP_LANGUAGE) else settings.put(SETTING_APP_LANGUAGE, language.tag)
    }

    /** The games the player wants in the feed, or null if they never chose (then all of them). Unknown ids are ignored. */
    fun savedGames(): Set<GameKind>? =
        settings.get(SETTING_GAMES).executeAsOneOrNull()?.split(",")?.mapNotNull { GameKind.fromId(it) }?.toSet()?.takeIf { it.isNotEmpty() }

    fun saveGames(games: Set<GameKind>) {
        settings.put(SETTING_GAMES, games.joinToString(",") { it.id })
    }

    /** The level the player chose for [game] in the feed. Medium until they choose. */
    fun savedDifficulty(game: GameKind): Difficulty =
        Difficulty.fromId(settings.get(SETTING_DIFFICULTY_PREFIX + game.id).executeAsOneOrNull()?.toIntOrNull())

    /** The chosen level of every game that has one saved. */
    fun savedDifficulties(): Map<GameKind, Difficulty> =
        GameKind.entries.filter { settings.get(SETTING_DIFFICULTY_PREFIX + it.id).executeAsOneOrNull() != null }.associateWith { savedDifficulty(it) }

    fun saveDifficulty(game: GameKind, difficulty: Difficulty) {
        settings.put(SETTING_DIFFICULTY_PREFIX + game.id, difficulty.id.toString())
    }

    /** The most puzzles solved in one feed so far. Kept on this device only. */
    fun bestFlow(): Int = settings.get(SETTING_BEST_FLOW).executeAsOneOrNull()?.toIntOrNull() ?: 0

    fun saveBestFlow(count: Int) {
        settings.put(SETTING_BEST_FLOW, count.toString())
    }

    // --- Pro: free guide steps, the cached Pro answer, the paywall that comes by itself, the look (docs/pro-plan.md) ---

    fun guideUse(): GuideUse {
        val day = settings.get(SETTING_GUIDE_QUOTA_DAY).executeAsOneOrNull()?.toLongOrNull() ?: return GuideUse.None
        return GuideUse(day, settings.get(SETTING_GUIDE_QUOTA_USED).executeAsOneOrNull()?.toIntOrNull() ?: 0)
    }

    fun setGuideUse(use: GuideUse) {
        db.transaction {
            settings.put(SETTING_GUIDE_QUOTA_DAY, use.day.toString())
            settings.put(SETTING_GUIDE_QUOTA_USED, use.used.toString())
        }
    }

    override fun cached(): Boolean = settings.get(SETTING_PRO_CACHED).executeAsOneOrNull() == "1"

    override fun setCached(pro: Boolean) {
        settings.put(SETTING_PRO_CACHED, if (pro) "1" else "0")
    }

    /**
     * The last bought-hint balance the server told, only for showing a number. Not kept when the account is deleted: the hints
     * went with it. Spending never trusts this, the server decides (docs/pro-plan.md section 11).
     */
    override fun cachedHints(): Int = settings.get(SETTING_HINT_BALANCE).executeAsOneOrNull()?.toIntOrNull() ?: 0

    override fun setCachedHints(count: Int) {
        settings.put(SETTING_HINT_BALANCE, count.toString())
    }

    /** The break (the last day finished before it) the streak rescue paywall was shown for. */
    fun rescueShownFor(): Long? = settings.get(SETTING_RESCUE_SHOWN).executeAsOneOrNull()?.toLongOrNull()

    fun setRescueShownFor(breakId: Long) {
        settings.put(SETTING_RESCUE_SHOWN, breakId.toString())
    }

    /** The local day a paywall last came up by itself, so it is at most one a day. */
    fun autoPaywallDay(): Long? = settings.get(SETTING_AUTO_PAYWALL_DAY).executeAsOneOrNull()?.toLongOrNull()

    fun setAutoPaywallDay(day: Long) {
        settings.put(SETTING_AUTO_PAYWALL_DAY, day.toString())
    }

    /** The accent color and the pure-black choice. Kept even when Pro ends, so they return with it. */
    fun savedAccent(): Accent = Accent.fromId(settings.get(SETTING_THEME_ACCENT).executeAsOneOrNull())

    fun saveAccent(accent: Accent) {
        settings.put(SETTING_THEME_ACCENT, accent.id)
    }

    fun savedAmoled(): Boolean = settings.get(SETTING_THEME_AMOLED).executeAsOneOrNull() == "1"

    fun saveAmoled(on: Boolean) {
        settings.put(SETTING_THEME_AMOLED, if (on) "1" else "0")
    }

    // --- Account deletion ---

    /**
     * Removes everything about the player from the device: results, favorites, unfinished puzzles, the upload
     * queue and the cached tables. Only the chosen languages and the privacy choice stay. Done after the account was deleted on
     * the server, so a new anonymous account does not receive the old history.
     */
    fun wipeLocalData() {
        db.transaction {
            favorites.deleteAll() // first: it refers to the results
            results.deleteAll()
            progress.deleteAll()
            queue.deleteAll()
            // The free guide steps and the Pro answer stay: deleting the account must not hand out free steps or take a purchase away
            // (it belongs to the store account). The look stays with the purchase too.
            settings.deleteAllExcept(
                listOf(
                    SETTING_LANGUAGE, SETTING_WORD_LANGUAGES, SETTING_APP_LANGUAGE, SETTING_TELEMETRY, SETTING_SOUND,
                    SETTING_GUIDE_QUOTA_DAY, SETTING_GUIDE_QUOTA_USED, SETTING_PRO_CACHED, SETTING_THEME_ACCENT, SETTING_THEME_AMOLED,
                ),
            )
        }
    }

    // --- Privacy choice and crash waiting to be sent (see telemetry/) ---

    /** Whether anonymous usage counts and crash reports may be sent. On until the player turns it off. */
    fun telemetryEnabled(): Boolean = settings.get(SETTING_TELEMETRY).executeAsOneOrNull() != "0"

    /** Whether the games play their sound effects. On until the player turns it off. */
    fun soundEnabled(): Boolean = settings.get(SETTING_SOUND).executeAsOneOrNull() != "0"

    fun setSoundEnabled(enabled: Boolean) {
        settings.put(SETTING_SOUND, if (enabled) "1" else "0")
    }

    fun setTelemetryEnabled(enabled: Boolean) {
        settings.put(SETTING_TELEMETRY, if (enabled) "1" else "0")
    }

    fun pendingCrash(): String? = settings.get(SETTING_PENDING_CRASH).executeAsOneOrNull()

    fun putPendingCrash(json: String) {
        settings.put(SETTING_PENDING_CRASH, json)
    }

    fun clearPendingCrash() {
        settings.deleteKey(SETTING_PENDING_CRASH)
    }

    // --- Account merge: the ticket kept while the player signs in (see account/AccountMerge) ---

    fun mergeTicket(): MergeTicket? =
        settings.get(SETTING_MERGE_TICKET).executeAsOneOrNull()?.split('|', limit = 2)
            ?.takeIf { it.size == 2 && it[0].isNotEmpty() && it[1].isNotEmpty() }
            ?.let { MergeTicket(fromUser = it[0], token = it[1]) }

    fun putMergeTicket(ticket: MergeTicket) {
        settings.put(SETTING_MERGE_TICKET, ticket.fromUser + "|" + ticket.token)
    }

    fun clearMergeTicket() {
        settings.deleteKey(SETTING_MERGE_TICKET)
    }

    // --- Cache of the leaderboards and friends, so the last view is still there offline ---

    fun cachedText(key: String): String? = settings.get(key).executeAsOneOrNull()

    fun cacheText(key: String, value: String) {
        settings.put(key, value)
    }

    // --- Cloud sync (see sync/SyncEngine) ---

    /** The oldest queued changes, at most [limit]. */
    fun pendingItems(limit: Int): List<QueueItem> =
        queue.selectBatch(limit.toLong()).executeAsList().map { QueueItem(it.kind, it.key, it.seq) }

    fun pendingCount(): Int = queue.countPending().executeAsOne().toInt()

    /** Removes an uploaded item from the queue, unless it changed again after it was read. */
    fun removeFromQueue(item: QueueItem) {
        queue.removeIfUnchanged(item.kind, item.key, item.seq)
    }

    fun resultsByIds(ids: List<String>): List<FinishedRound> =
        if (ids.isEmpty()) emptyList() else results.selectByIds(ids).executeAsList().mapNotNull { it.toFinishedOrNull() }

    fun favoriteRecord(resultId: String): FavoriteRecord? =
        favorites.selectFavorite(resultId).executeAsOneOrNull()
            ?.let { FavoriteRecord(it.result_id, it.is_favorite == 1L, it.updated_at) }

    /**
     * Queues everything that can be uploaded. Used when the signed-in account changes (first sign-in, or a switch
     * to another account): the new account has none of this device's history yet.
     * Results are additive and have unique ids, so offering them again is always safe.
     */
    fun enqueueEverything() {
        db.transaction {
            results.selectAll().executeAsList()
                .filter { it.mode == Mode.ENDLESS.name } // daily results are written by the server only
                .forEach { queue.enqueue(KIND_RESULT, it.id) }
            favorites.selectAllFavorites().executeAsList().forEach { queue.enqueue(KIND_FAVORITE, it.result_id) }
        }
    }

    /**
     * Adds results that exist in the cloud but not here. They are not queued, they come from the cloud. Known ids stay
     * untouched, with one exception: a daily result has the same id on every device (`daily-<day>-<game>`), and the cloud
     * keeps only one per account and day. If this device made another one while it was anonymous (and the account the
     * player then signed in to already had its own), the cloud's one is the result of that day, so it replaces the local
     * one. Otherwise the result card would show moves and a time that the tables do not.
     */
    fun mergeRemoteResults(remote: List<FinishedRound>) {
        db.transaction {
            for (result in remote) {
                val local = results.selectById(result.id).executeAsOneOrNull()?.toFinishedOrNull()
                if (local != null && result.mode == Mode.DAILY && local.differsFrom(result)) replaceResultRow(result)
                else insertResultRow(result) // INSERT OR IGNORE
            }
        }
    }

    private fun FinishedRound.differsFrom(other: FinishedRound) =
        outcome != other.outcome || durationMs != other.durationMs || guesses != other.guesses

    private fun replaceResultRow(result: FinishedRound) {
        results.replaceResult(
            id = result.id,
            game = result.game.id,
            mode = result.mode.name,
            language = result.language.name,
            answer = result.answer,
            guesses = result.guesses.joinToString(","),
            outcome = result.outcome.name,
            was_skipped = if (result.assisted) 1 else 0,
            finished_at = result.finishedAt,
            day_index = result.dayIndex,
            duration_ms = result.durationMs,
            difficulty = result.difficulty.id.toLong(),
        )
    }

    /** Applies cloud favorites, newest write wins. They are not queued, they come from the cloud. */
    fun mergeRemoteFavorites(remote: List<FavoriteRecord>) {
        db.transaction {
            for (record in remote) {
                val local = favoriteRecord(record.resultId)
                if (local == null || record.updatedAt > local.updatedAt) {
                    favorites.setFavorite(record.resultId, if (record.isFavorite) 1 else 0, record.updatedAt)
                }
            }
        }
    }

    /** The account this device's queue was last prepared for, or null if it never was. */
    fun syncedUserId(): String? = settings.get(SETTING_SYNCED_USER).executeAsOneOrNull()

    fun setSyncedUserId(userId: String) {
        settings.put(SETTING_SYNCED_USER, userId)
    }
}

/** One entry of the upload queue. [seq] is how the queue notices that an item changed after it was read. */
data class QueueItem(val kind: String, val key: String, val seq: Long)

/** A favorite as it is stored and synced: the heart state of one finished puzzle and when it last changed. */
data class FavoriteRecord(val resultId: String, val isFavorite: Boolean, val updatedAt: Long)

const val KIND_RESULT = "result"
const val KIND_FAVORITE = "favorite"

private fun String.toGuessList(): List<String> = if (isEmpty()) emptyList() else split(",")

private fun Game_result.toFinishedOrNull(): FinishedRound? = FinishedRound(
    game = GameKind.fromId(game) ?: return null,
    id = id,
    mode = Mode.valueOf(mode),
    language = Language.valueOf(language),
    answer = answer,
    guesses = guesses.toGuessList(),
    outcome = Outcome.valueOf(outcome),
    assisted = was_skipped == 1L,
    finishedAt = finished_at,
    durationMs = duration_ms,
    puzzleDay = day_index.takeIf { it != IstanbulDay.dayIndex(finished_at) },
    difficulty = Difficulty.fromId(difficulty.toInt()),
)

private fun In_progress_round.toStoredOrNull(): StoredRound? = StoredRound(
    game = GameKind.fromId(game) ?: return null,
    id = id,
    language = Language.valueOf(language),
    answer = answer,
    guesses = guesses.toGuessList(),
    input = input,
    assisted = was_skipped == 1L,
    position = position.toInt(),
    difficulty = Difficulty.fromId(difficulty.toInt()),
)
