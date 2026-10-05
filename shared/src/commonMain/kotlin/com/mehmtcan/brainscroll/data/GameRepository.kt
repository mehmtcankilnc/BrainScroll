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

    /** Moves a round from "unfinished" to the history in one transaction, so it can never be in both or neither. */
    override fun finish(result: FinishedRound) {
        db.transaction {
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
                day_index = IstanbulDay.dayIndex(result.finishedAt),
            )
            progress.deleteById(result.id)
        }
    }

    fun unfinishedRounds(): List<StoredRound> = progress.selectAll().executeAsList().map { it.toStored() }

    // --- History ---

    /** Every finished puzzle, oldest first. */
    fun history(): List<FinishedRound> = results.selectAll().executeAsList().map { it.toFinished() }

    // --- Favorites ---

    fun setFavorite(resultId: String, favorite: Boolean, now: Long) {
        favorites.setFavorite(resultId, if (favorite) 1 else 0, now)
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
            )
        }

    // --- Settings ---

    fun savedLanguage(): Language? =
        settings.get(SETTING_LANGUAGE).executeAsOneOrNull()?.let { name -> Language.entries.firstOrNull { it.name == name } }

    fun saveLanguage(language: Language) {
        settings.put(SETTING_LANGUAGE, language.name)
    }
}

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
