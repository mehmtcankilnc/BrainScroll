package com.mehmtcan.brainscroll.data

import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.db.Game_result
import com.mehmtcan.brainscroll.db.In_progress_round
import com.mehmtcan.brainscroll.game.wordle.FeedStore
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound
import com.mehmtcan.brainscroll.time.IstanbulDay

internal const val DATABASE_FILE = "brainscroll.db"

private const val GAME_WORD = "word"
private const val SETTING_LANGUAGE = "word_language"
private const val SETTING_SYNCED_USER = "synced_user_id"

/**
 * Reads and writes everything the app keeps on the device. It is the only place that knows the tables;
 * the rest of the app works with plain Kotlin types ([FinishedRound], [StoredRound]).
 */
class GameRepository(database: BrainScrollDatabase) : FeedStore {
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
            game = GAME_WORD,
            mode = Mode.ENDLESS.name,
            language = round.language.name,
            answer = round.answer,
            guesses = round.guesses.joinToString(","),
            input = round.input,
            was_skipped = if (round.skipped) 1 else 0,
            position = round.position.toLong(),
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
    }

    private fun insertResultRow(result: FinishedRound) {
        results.insertResult(
            id = result.id,
            game = GAME_WORD,
            mode = result.mode.name,
            language = result.language.name,
            answer = result.answer,
            guesses = result.guesses.joinToString(","),
            outcome = result.outcome.name,
            was_skipped = if (result.wasSkipped) 1 else 0,
            finished_at = result.finishedAt,
            day_index = result.dayIndex,
            duration_ms = result.durationMs,
        )
    }

    fun unfinishedRounds(): List<StoredRound> = progress.selectAll().executeAsList().map { it.toStored() }

    // --- History ---

    /** Every finished puzzle, oldest first. */
    fun history(): List<FinishedRound> = results.selectAll().executeAsList().map { it.toFinished() }

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
        favorites.selectFavoriteResults().executeAsList().map {
            FinishedRound(
                id = it.id,
                mode = Mode.valueOf(it.mode),
                language = Language.valueOf(it.language),
                answer = it.answer,
                guesses = it.guesses.toGuessList(),
                outcome = Outcome.valueOf(it.outcome),
                wasSkipped = it.was_skipped == 1L,
                finishedAt = it.finished_at,
                durationMs = it.duration_ms,
                puzzleDay = it.day_index.takeIf { day -> day != IstanbulDay.dayIndex(it.finished_at) },
            )
        }

    // --- Settings ---

    fun savedLanguage(): Language? =
        settings.get(SETTING_LANGUAGE).executeAsOneOrNull()?.let { name -> Language.entries.firstOrNull { it.name == name } }

    fun saveLanguage(language: Language) {
        settings.put(SETTING_LANGUAGE, language.name)
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
        if (ids.isEmpty()) emptyList() else results.selectByIds(ids).executeAsList().map { it.toFinished() }

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

    /** Adds results that exist in the cloud but not here. They are not queued, they come from the cloud. */
    fun mergeRemoteResults(remote: List<FinishedRound>) {
        db.transaction { remote.forEach { insertResultRow(it) } } // INSERT OR IGNORE: known ids stay untouched
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

private fun Game_result.toFinished() = FinishedRound(
    id = id,
    mode = Mode.valueOf(mode),
    language = Language.valueOf(language),
    answer = answer,
    guesses = guesses.toGuessList(),
    outcome = Outcome.valueOf(outcome),
    wasSkipped = was_skipped == 1L,
    finishedAt = finished_at,
    durationMs = duration_ms,
    puzzleDay = day_index.takeIf { it != IstanbulDay.dayIndex(finished_at) },
)

private fun In_progress_round.toStored() = StoredRound(
    id = id,
    language = Language.valueOf(language),
    answer = answer,
    guesses = guesses.toGuessList(),
    input = input,
    skipped = was_skipped == 1L,
    position = position.toInt(),
)
