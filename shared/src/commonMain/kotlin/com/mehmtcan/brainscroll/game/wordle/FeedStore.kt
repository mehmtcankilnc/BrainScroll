package com.mehmtcan.brainscroll.game.wordle

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.time.IstanbulDay

enum class Outcome { WON, LOST }

enum class Mode { ENDLESS, DAILY }

/** A puzzle that was started but not finished. Enough to rebuild the round exactly as the player left it. */
data class StoredRound(
    val id: String,
    val language: Language,
    val answer: String,
    val guesses: List<String>,
    val input: String,
    val position: Int,
    /** Which game this round belongs to. For memory, [answer] is the board and [guesses] the order of the flips. */
    val game: GameKind = GameKind.Word,
    /** True once the player asked the guide for a step: the puzzle no longer counts for streaks and statistics. */
    val assisted: Boolean = false,
)

/** A puzzle that is over. These rows are append-only history; streaks and statistics are derived from them. */
data class FinishedRound(
    val id: String,
    val mode: Mode,
    val language: Language,
    val answer: String,
    val guesses: List<String>,
    val outcome: Outcome,
    val finishedAt: Long,
    /** Daily puzzles only: how long it took, from the server's clock. Null for endless puzzles. */
    val durationMs: Long? = null,
    /**
     * The day of the puzzle itself, when it differs from the day it was finished on: a daily puzzle started just
     * before midnight and finished just after belongs to the day it was started. Null means "the day it ended".
     */
    val puzzleDay: Long? = null,
    val game: GameKind = GameKind.Word,
    /** True if the player used the guide: the result stays in the history but counts for no streak and no statistic. */
    val assisted: Boolean = false,
) {
    /** The Istanbul day this result counts for (docs/decisions.md). */
    val dayIndex: Long get() = puzzleDay ?: IstanbulDay.dayIndex(finishedAt)
}

/**
 * Where the feed saves its progress. The feed only talks to this interface, so the game logic stays free of
 * database code and tests can use a fake.
 */
interface FeedStore {
    fun saveProgress(round: StoredRound)
    fun deleteProgress(id: String)

    /** Records a finished puzzle and removes it from the unfinished ones, as one step. */
    fun finish(result: FinishedRound)
}

/** Used when nothing should be saved (tests, previews). */
object NoFeedStore : FeedStore {
    override fun saveProgress(round: StoredRound) = Unit
    override fun deleteProgress(id: String) = Unit
    override fun finish(result: FinishedRound) = Unit
}
