package com.mehmtcan.brainscroll.game.lights

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.LightsGuide
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a Lights Out page for the UI. */
data class LightsSnapshot(
    override val id: String,
    override val index: Int,
    val state: LightsState,
    /** Time spent so far, as the round counts it (see [LightsRound]). */
    val durationMs: Long,
) : PageSnapshot {
    override val isFinished: Boolean get() = state.status == LightsStatus.Won
    override val kind: GameKind get() = GameKind.Lights
}

/**
 * One Lights Out board in the feed: a [LightsGame] plus the time and the list of actions, which is all that is needed
 * to save it and to rebuild it exactly as the player left it (the board itself comes from [seed]).
 *
 * The time works as in the other games: the pauses between two actions, each counted for at most [MAX_THINK_MILLIS].
 * The clock starts with the first tap. The game cannot be lost, so a result is always a win.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class LightsRound(
    override val index: Int,
    override val id: String,
    private val seed: Int,
    private val language: Language,
    private val now: () -> Long = { 0L },
    private val size: Int = LightsGame.DEFAULT_SIZE,
    private val taps: Int = LightsGame.DEFAULT_TAPS,
) : FeedRound {
    private val game = LightsGame(seed, size, taps)

    /** What the player did, in order: "t12" taps cell 12, "r" restarts. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var lastActionAt: Long? = null

    override val kind: GameKind get() = GameKind.Lights

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        return { LightsGuide.step(state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()


    fun tap(cell: Int): LightsResult = game.tap(cell).also { if (it != LightsResult.Ignored) record("t$cell") }

    fun restart(): Boolean = game.restart().also { if (it) record(RESTART) }

    private fun record(action: String) {
        actions += action
        val time = now()
        lastActionAt?.let { elapsedMs += minOf(time - it, MAX_THINK_MILLIS).coerceAtLeast(0) }
        lastActionAt = time
    }

    override fun snapshot() = LightsSnapshot(id, index, game.state, elapsedMs)

    /** "5x5x7:seed": the size, the number of taps the board was made with, and the seed. */
    private fun encodedBoard() = "${size}x${size}x$taps:$seed"

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = encodedBoard(),
        guesses = actions.toList(),
        input = elapsedMs.toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Lights,
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
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = elapsedMs,
        game = GameKind.Lights,
    )

    companion object {
        /** A pause between two actions counts for at most this long. */
        const val MAX_THINK_MILLIS = 15_000L
        private const val RESTART = "r"

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read: the
         * caller then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): LightsRound? {
            val parts = stored.answer.split(':')
            if (parts.size != 2) return null
            val shape = parts[0].split('x').map { it.toIntOrNull() ?: return null }
            if (shape.size != 3 || shape[0] != shape[1]) return null
            val seed = parts[1].toIntOrNull() ?: return null
            val round = try {
                LightsRound(index, stored.id, seed, stored.language, now, shape[0], shape[2])
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
            round.assisted = stored.assisted
            return round
        }
    }
}
