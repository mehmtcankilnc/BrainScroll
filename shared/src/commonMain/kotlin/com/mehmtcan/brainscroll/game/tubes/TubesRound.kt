package com.mehmtcan.brainscroll.game.tubes

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a tubes page for the UI. */
data class TubesSnapshot(
    override val id: String,
    override val index: Int,
    val state: TubesState,
    /** Time spent so far, as the round counts it (see [TubesRound]). */
    val durationMs: Long,
    override val skipped: Boolean,
) : PageSnapshot {
    override val isFinished: Boolean get() = state.status == TubesStatus.Won
    override val kind: GameKind get() = GameKind.Tubes
}

/**
 * One board of color tubes in the feed: a [TubesGame] plus the time and the list of actions, which is all that is
 * needed to save it and to rebuild it exactly as the player left it.
 *
 * The time works as in memory match: the pauses between two actions, each counted for at most [MAX_THINK_MILLIS], so
 * leaving the app open in the background does not blow it up. The clock starts with the first pour.
 *
 * [language] only fills the language of the saved result (the table needs one); tubes have no language of their own.
 */
class TubesRound(
    override val index: Int,
    override val id: String,
    private val layout: List<List<Int>>,
    private val language: Language,
    private val now: () -> Long = { 0L },
    capacity: Int = TubesGame.DEFAULT_CAPACITY,
) : FeedRound {
    private val game = TubesGame(layout, capacity)

    /** What the player did, in order: "3>5" is a pour, "u" an undo, "r" a restart. Saved on the device only. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var lastActionAt: Long? = null

    override val kind: GameKind get() = GameKind.Tubes

    override var skipped = false
        private set

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()

    override fun markSkipped() {
        skipped = true
    }

    fun pour(from: Int, to: Int): PourResult {
        val result = game.pour(from, to)
        if (result == PourResult.Poured) record("$from>$to")
        return result
    }

    fun undo(): Boolean = game.undo().also { if (it) record(UNDO) }

    fun restart(): Boolean = game.restart().also { if (it) record(RESTART) }

    private fun record(action: String) {
        actions += action
        val time = now()
        lastActionAt?.let { elapsedMs += minOf(time - it, MAX_THINK_MILLIS).coerceAtLeast(0) }
        lastActionAt = time
    }

    override fun snapshot() = TubesSnapshot(id, index, game.state, elapsedMs, skipped)

    private fun encodedLayout() = layout.joinToString("|") { tube -> tube.joinToString("") }

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = encodedLayout(),
        guesses = actions.toList(),
        input = elapsedMs.toString(),
        skipped = skipped,
        position = index,
        game = GameKind.Tubes,
    )

    /**
     * The history row. Its `guesses` is only the number of pours, not the list of actions: the cloud table keeps at
     * most 256 characters there, and a long game with many undos would not fit. A finished game is never replayed.
     */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = encodedLayout(),
        guesses = listOf(game.state.moves.toString()),
        outcome = Outcome.WON,
        wasSkipped = skipped,
        finishedAt = finishedAt,
        durationMs = elapsedMs,
        game = GameKind.Tubes,
    )

    companion object {
        /** A pause between two actions counts for at most this long. */
        const val MAX_THINK_MILLIS = 15_000L
        private const val UNDO = "u"
        private const val RESTART = "r"

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read: the
         * caller then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): TubesRound? {
            val layout = stored.answer.split('|').map { tube ->
                tube.map { ch -> ch.digitToIntOrNull() ?: return null }
            }
            val round = try {
                TubesRound(index, stored.id, layout, stored.language, now)
            } catch (e: IllegalArgumentException) {
                return null
            }
            for (action in stored.guesses) {
                when (action) {
                    UNDO -> round.game.undo()
                    RESTART -> round.game.restart()
                    else -> {
                        val (from, to) = action.split('>').map { it.toIntOrNull() ?: return null }.takeIf { it.size == 2 } ?: return null
                        round.game.pour(from, to)
                    }
                }
                round.actions += action
            }
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.skipped = stored.skipped
            return round
        }
    }
}
