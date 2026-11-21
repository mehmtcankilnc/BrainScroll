package com.mehmtcan.brainscroll.game.axissum

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.AxisSumGuide
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of an axis sum page for the UI. */
data class AxisSumSnapshot(
    override val id: String,
    override val index: Int,
    val state: AxisSumState,
    /** Time spent so far, as the round counts it (see [AxisSumRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == AxisSumStatus.Won
    override val kind: GameKind get() = GameKind.AxisSum
}

/**
 * One axis sum board in the feed: an [AxisSumGame] plus the time and the list of actions, which is all that is needed to save it
 * and to rebuild it exactly as the player left it. The time is the time the page is on show (see [onShown]): it runs from the
 * moment the page comes to the screen until the board is won, and stops while the page is left. A result is always a win, its
 * number the taps.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class AxisSumRound(
    override val index: Int,
    override val id: String,
    private val puzzle: AxisSumPuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = AxisSumGame(puzzle)

    /** What the player did, in order: "t12" taps cell 12, "r" brings the numbers back. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var shownSince: Long? = null

    override val kind: GameKind get() = GameKind.AxisSum

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        val p = puzzle
        return { AxisSumGuide.step(state, AxisSumSolver.unique(p)) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()

    fun tap(cell: Int): AxisSumResult = game.tap(cell).also { if (it != AxisSumResult.Ignored) record("t$cell") }

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

    override fun snapshot() = AxisSumSnapshot(id, index, game.state, total())

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = actions.toList(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.AxisSum,
    )

    /** The history row. Its `guesses` is only the number of taps (the table keeps at most 256 characters there). */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = puzzle.encode(),
        guesses = listOf(game.state.taps.toString()),
        outcome = Outcome.WON,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = total(),
        game = GameKind.AxisSum,
    )

    companion object {
        private const val RESTART = "r"

        /** Rebuilds a round from storage by replaying its actions. Null if what was saved cannot be read: a new one is started then. */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): AxisSumRound? {
            val puzzle = AxisSumPuzzle.decode(stored.answer) ?: return null
            val round = AxisSumRound(index, stored.id, puzzle, stored.language, now)
            for (action in stored.guesses) {
                when {
                    action == RESTART -> round.game.restart()
                    action.length > 1 && action[0] == 't' -> round.game.tap(action.substring(1).toIntOrNull() ?: return null)
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
