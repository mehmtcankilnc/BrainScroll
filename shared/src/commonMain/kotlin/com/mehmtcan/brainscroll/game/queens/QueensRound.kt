package com.mehmtcan.brainscroll.game.queens

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.QueensGuide
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a queens page for the UI. */
data class QueensSnapshot(
    override val id: String,
    override val index: Int,
    val state: QueensState,
    /** Time spent so far, as the round counts it (see [QueensRound]). */
    val durationMs: Long,
) : PageSnapshot {
    override val isFinished: Boolean get() = state.status == QueensStatus.Won
    override val kind: GameKind get() = GameKind.Queens
}

/**
 * One queens board in the feed: a [QueensGame] plus the time and the list of actions, which is all that is needed to save it
 * and to rebuild it exactly as the player left it.
 *
 * The time works as in the other games: the pauses between two actions, each counted for at most [MAX_THINK_MILLIS]. The
 * clock starts with the first tap. The game cannot be lost, so a result is always a win.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class QueensRound(
    override val index: Int,
    override val id: String,
    private val size: Int,
    private val locked: Set<Int>,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = QueensGame(size, locked)

    /** What the player did, in order: "t12" taps cell 12, "r" restarts. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var lastActionAt: Long? = null

    override val kind: GameKind get() = GameKind.Queens

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        return { QueensGuide.step(state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()

    fun toggle(cell: Int): QueensResult = game.toggle(cell).also { if (it != QueensResult.Ignored) record("t$cell") }

    fun restart(): Boolean = game.restart().also { if (it) record(RESTART) }

    private fun record(action: String) {
        actions += action
        val time = now()
        lastActionAt?.let { elapsedMs += minOf(time - it, MAX_THINK_MILLIS).coerceAtLeast(0) }
        lastActionAt = time
    }

    override fun snapshot() = QueensSnapshot(id, index, game.state, elapsedMs)

    /** The board: its size, a colon and the locked cells, e.g. `5:3,12`. */
    private fun encoded() = "$size:" + locked.sorted().joinToString(",")

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = encoded(),
        guesses = actions.toList(),
        input = elapsedMs.toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Queens,
    )

    /**
     * The history row. Its `guesses` is only the number of moves, not the list of actions: the cloud table keeps at most 256
     * characters there, and a long game would not fit. A finished game is never replayed.
     */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = encoded(),
        guesses = listOf(game.state.moves.toString()),
        outcome = Outcome.WON,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = elapsedMs,
        game = GameKind.Queens,
    )

    companion object {
        /** A pause between two actions counts for at most this long. */
        const val MAX_THINK_MILLIS = 15_000L
        private const val RESTART = "r"

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read: the caller
         * then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): QueensRound? {
            val parts = stored.answer.split(':')
            if (parts.size != 2) return null
            val size = parts[0].toIntOrNull() ?: return null
            val locked = if (parts[1].isEmpty()) emptySet() else parts[1].split(',').map { it.toIntOrNull() ?: return null }.toSet()
            val round = try {
                QueensRound(index, stored.id, size, locked, stored.language, now)
            } catch (e: IllegalArgumentException) {
                return null
            }
            for (action in stored.guesses) {
                when {
                    action == RESTART -> round.game.restart()
                    action.length > 1 && action[0] == 't' -> round.game.toggle(action.substring(1).toIntOrNull() ?: return null)
                    else -> return null
                }
                round.actions += action
            }
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.assisted = stored.assisted
            return round
        }
    }
}
