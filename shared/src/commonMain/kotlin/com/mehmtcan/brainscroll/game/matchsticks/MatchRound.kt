package com.mehmtcan.brainscroll.game.matchsticks

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.MatchGuide
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a matchstick page for the UI. */
data class MatchSnapshot(
    override val id: String,
    override val index: Int,
    val state: MatchState,
    /** Time spent so far, as the round counts it (see [MatchRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == MatchStatus.Won
    override val kind: GameKind get() = GameKind.Matchsticks
}

/**
 * One matchstick sum in the feed: a [MatchGame] plus the time. The time is the time the page is on show (see [onShown]): it runs from the moment the page comes to the screen until the board is won, and stops while the page is left. A result is always a win, its number the moves
 * made (1 is the best). The sum is saved as it stands: the moves so far and the sticks of every symbol.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class MatchRound(
    override val index: Int,
    override val id: String,
    private val puzzle: MatchPuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = MatchGame(puzzle)
    private var elapsedMs = 0L
    private var shownSince: Long? = null
    private var touched = false

    override val kind: GameKind get() = GameKind.Matchsticks

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        val p = puzzle
        return { MatchGuide.step(p, state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = !touched

    /** Takes the stick on [move]`.from` and lays it on `.to`. */
    fun move(move: MatchMove): MatchResult = game.move(move).also {
        if (it != MatchResult.Ignored) record()
        if (it == MatchResult.Solved) onHidden()
    }

    /** Puts the stick back after a wrong move. */
    fun undo(): Boolean = game.undo().also { if (it) record() }

    private fun record() {
        touched = true
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

    override fun snapshot() = MatchSnapshot(id, index, game.state, total())

    /** The moves made, then the sticks of each symbol (a hex number each, apart by dots). */
    private fun savedState(): List<String> {
        val state = game.state
        return listOf("n${state.moves}", "s" + state.cells.joinToString(".") { it.toString(16) })
    }

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = savedState(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Matchsticks,
    )

    /** The history row. Its `guesses` is only the number of moves. */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = puzzle.encode(),
        guesses = listOf(game.state.moves.toString()),
        outcome = Outcome.WON,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = total(),
        game = GameKind.Matchsticks,
    )

    companion object {
        /** Rebuilds a round from storage. Null if what was saved cannot be read: a new one is started then. */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): MatchRound? {
            val puzzle = MatchPuzzle.decode(stored.answer) ?: return null
            val round = MatchRound(index, stored.id, puzzle, stored.language, now)
            var moves = 0
            var cells = puzzle.cells
            for (entry in stored.guesses) {
                when {
                    entry.length >= 2 && entry[0] == 'n' -> moves = entry.substring(1).toIntOrNull() ?: return null
                    entry.length >= 2 && entry[0] == 's' -> cells = entry.substring(1).split('.').map { it.toIntOrNull(16) ?: return null }
                    else -> return null
                }
            }
            if (!round.game.restore(cells, moves)) return null
            round.touched = moves > 0
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.assisted = stored.assisted
            return round
        }
    }
}
