package com.mehmtcan.brainscroll.game.ballslide

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.BallSlideGuide
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a ball slide page for the UI. */
data class BallSlideSnapshot(
    override val id: String,
    override val index: Int,
    val state: BallSlideState,
    /** Time spent so far, as the round counts it (see [BallSlideRound]). */
    val durationMs: Long,
) : PageSnapshot {
    override val isFinished: Boolean get() = state.status == BallSlideStatus.Won
    override val kind: GameKind get() = GameKind.BallSlide
}

/**
 * One ball slide board in the feed: a [BallSlideGame] plus the time and the list of actions, which is all that is needed to save it
 * and to rebuild it exactly as the player left it. The time works as in the other games: the pauses between two actions, each
 * counted for at most [MAX_THINK_MILLIS]; the clock starts with the first action. A result is always a win, its number the slides.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class BallSlideRound(
    override val index: Int,
    override val id: String,
    private val puzzle: BallSlidePuzzle,
    par: Int,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = BallSlideGame(puzzle, par)

    /** What the player did, in order: "m1" slides the ball right (see [BallDir]), "u" takes the last slide back, "r" starts again. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var lastActionAt: Long? = null

    override val kind: GameKind get() = GameKind.BallSlide

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        val p = puzzle
        return { BallSlideGuide.step(p, state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()

    fun slide(dir: Int): BallSlideResult = game.slide(dir).also { if (it != BallSlideResult.Ignored) record("m$dir") }

    fun undo(): Boolean = game.undo().also { if (it) record(UNDO) }

    fun restart(): Boolean = game.restart().also { if (it) record(RESTART) }

    private fun record(action: String) {
        actions += action
        val time = now()
        lastActionAt?.let { elapsedMs += minOf(time - it, MAX_THINK_MILLIS).coerceAtLeast(0) }
        lastActionAt = time
    }

    override fun snapshot() = BallSlideSnapshot(id, index, game.state, elapsedMs)

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = actions.toList(),
        input = elapsedMs.toString(),
        position = index,
        assisted = assisted,
        game = GameKind.BallSlide,
    )

    /** The history row. Its `guesses` is only the number of slides (the table keeps at most 256 characters there). */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = puzzle.encode(),
        guesses = listOf(game.state.moves.toString()),
        outcome = Outcome.WON,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = elapsedMs,
        game = GameKind.BallSlide,
    )

    companion object {
        /** A pause between two actions counts for at most this long. */
        const val MAX_THINK_MILLIS = 15_000L
        private const val UNDO = "u"
        private const val RESTART = "r"

        /** Rebuilds a round from storage by replaying its actions. Null if what was saved cannot be read: a new one is started then. */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): BallSlideRound? {
            val puzzle = BallSlidePuzzle.decode(stored.answer) ?: return null
            val par = BallSlideSolver.par(puzzle) ?: return null
            val round = BallSlideRound(index, stored.id, puzzle, par, stored.language, now)
            for (action in stored.guesses) {
                when {
                    action == UNDO -> round.game.undo()
                    action == RESTART -> round.game.restart()
                    action.length == 2 && action[0] == 'm' -> round.game.slide(action[1].digitToIntOrNull() ?: return null)
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
