package com.mehmtcan.brainscroll.game.pipes

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a pipes page for the UI. */
data class PipesSnapshot(
    override val id: String,
    override val index: Int,
    val state: PipesState,
    /** Time spent so far, as the round counts it (see [PipesRound]). */
    val durationMs: Long,
    override val skipped: Boolean,
) : PageSnapshot {
    override val isFinished: Boolean get() = state.status == PipesStatus.Won
    override val kind: GameKind get() = GameKind.Pipes
}

/**
 * One pipes board in the feed: a [PipesGame] plus the time and the list of actions, which is all that is needed to save
 * it and to rebuild it exactly as the player left it.
 *
 * The time works as in the other games: the pauses between two actions, each counted for at most [MAX_THINK_MILLIS].
 * The clock starts with the first tap. The game cannot be lost, so a result is always a win.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class PipesRound(
    override val index: Int,
    override val id: String,
    private val start: List<Int>,
    private val language: Language,
    private val now: () -> Long = { 0L },
    par: Int = PipesGenerator.minTaps(start.toIntArray(), sizeOf(start)) ?: throw IllegalArgumentException("the board cannot be solved"),
) : FeedRound {
    private val game = PipesGame(start.toIntArray(), par)

    /** What the player did, in order: "t12" turns the tile in cell 12, "r" restarts. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var lastActionAt: Long? = null

    override val kind: GameKind get() = GameKind.Pipes

    override var skipped = false
        private set

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()

    override fun markSkipped() {
        skipped = true
    }

    fun tap(cell: Int): PipesResult = game.tap(cell).also { if (it != PipesResult.Ignored) record("t$cell") }

    fun restart(): Boolean = game.restart().also { if (it) record(RESTART) }

    private fun record(action: String) {
        actions += action
        val time = now()
        lastActionAt?.let { elapsedMs += minOf(time - it, MAX_THINK_MILLIS).coerceAtLeast(0) }
        lastActionAt = time
    }

    override fun snapshot() = PipesSnapshot(id, index, game.state, elapsedMs, skipped)

    /** The starting tiles, one hexadecimal digit each: 25 characters for the 5x5 board. */
    private fun encodedBoard() = start.joinToString("") { it.toString(16) }

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = encodedBoard(),
        guesses = actions.toList(),
        input = elapsedMs.toString(),
        skipped = skipped,
        position = index,
        game = GameKind.Pipes,
    )

    /**
     * The history row. Its `guesses` is only the number of taps, not the list of actions: the cloud table keeps at most
     * 256 characters there, and a long game would not fit. A finished game is never replayed.
     */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = encodedBoard(),
        guesses = listOf(game.state.moves.toString()),
        outcome = Outcome.WON,
        wasSkipped = skipped,
        finishedAt = finishedAt,
        durationMs = elapsedMs,
        game = GameKind.Pipes,
    )

    companion object {
        /** A pause between two actions counts for at most this long. */
        const val MAX_THINK_MILLIS = 15_000L
        private const val RESTART = "r"

        private fun sizeOf(tiles: List<Int>) = kotlin.math.sqrt(tiles.size.toDouble()).toInt()

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read (or the
         * board cannot be solved): the caller then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): PipesRound? {
            val tiles = stored.answer.map { it.digitToIntOrNull(16) ?: return null }
            if (sizeOf(tiles) < 3 || sizeOf(tiles) * sizeOf(tiles) != tiles.size) return null
            val round = try {
                PipesRound(index, stored.id, tiles, stored.language, now)
            } catch (e: IllegalArgumentException) {
                return null
            }
            for (action in stored.guesses) {
                when {
                    action == RESTART -> round.game.restart()
                    action.startsWith("t") -> round.game.tap(action.drop(1).toIntOrNull() ?: return null)
                    else -> return null
                }
                round.actions += action
            }
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.skipped = stored.skipped
            return round
        }
    }
}
