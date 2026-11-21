package com.mehmtcan.brainscroll.game.nonogram

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.NonogramGuide
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a nonogram page for the UI. */
data class NonogramSnapshot(
    override val id: String,
    override val index: Int,
    val state: NonogramState,
    /** Time spent so far, as the round counts it (see [NonogramRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == NonogramStatus.Won
    override val kind: GameKind get() = GameKind.Nonogram
}

/**
 * One nonogram board in the feed: a [NonogramGame] plus the time and the list of actions, which is all that is needed to save it
 * and to rebuild it exactly as the player left it.
 *
 * The time is the time the page is on show (see [onShown]): it runs from the moment the page comes to the screen until the board
 * is won, and stops while the page is left. The game cannot be lost, so a result is always a win.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class NonogramRound(
    override val index: Int,
    override val id: String,
    private val puzzle: NonogramPuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = NonogramGame(puzzle)

    /** What the player did, in order: "p12-15:1" fills cells 12 to 15, "r" clears the board. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var shownSince: Long? = null

    override val kind: GameKind get() = GameKind.Nonogram

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        return { NonogramGuide.step(state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()

    /** Sets the cells from [from] to [to] (on one row or column) to [value] (see [NonogramMark]). A drag is one stroke. */
    fun paint(from: Int, to: Int, value: Int): NonogramResult =
        game.paint(from, to, value).also { if (it != NonogramResult.Ignored) record("p$from-$to:$value") }

    fun restart(): Boolean = game.restart().also { if (it) record(RESTART) }

    private fun record(action: String) {
        actions += action
    }

    /**
     * The page came to the screen: the clock starts (or goes on). The time of this round is the time its page was on show, so
     * thinking before the first move counts, and a puzzle solved with one move is not solved "in no time".
     */
    override fun onShown() {
        if (!isFinished && shownSince == null) shownSince = now()
    }

    /** The page left the screen (swiped away, another tab, the app in the background) or the round is over: the clock stops. */
    override fun onHidden() {
        shownSince?.let { elapsedMs += (now() - it).coerceAtLeast(0) }
        shownSince = null
    }

    /** The time so far: what was counted before, and the time since the page came to the screen if it is on show now. */
    private fun total(): Long = elapsedMs + (shownSince?.let { (now() - it).coerceAtLeast(0) } ?: 0L)

    override fun snapshot() = NonogramSnapshot(id, index, game.state, total())

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = actions.toList(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Nonogram,
    )

    /**
     * The history row. Its `guesses` is only the number of strokes, not the list of actions: the cloud table keeps at most 256
     * characters there, and a long game would not fit. A finished game is never replayed.
     */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = puzzle.encode(),
        guesses = listOf(actions.size.toString()),
        outcome = Outcome.WON,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = total(),
        game = GameKind.Nonogram,
    )

    companion object {
        private const val RESTART = "r"

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read: the caller
         * then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): NonogramRound? {
            val puzzle = NonogramPuzzle.decode(stored.answer) ?: return null
            val round = NonogramRound(index, stored.id, puzzle, stored.language, now)
            for (action in stored.guesses) {
                when {
                    action == RESTART -> round.game.restart()
                    action.length > 1 && action[0] == 'p' -> {
                        val (cells, value) = action.substring(1).split(':').takeIf { it.size == 2 } ?: return null
                        val (from, to) = cells.split('-').takeIf { it.size == 2 } ?: return null
                        round.game.paint(from.toIntOrNull() ?: return null, to.toIntOrNull() ?: return null, value.toIntOrNull() ?: return null)
                    }
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
