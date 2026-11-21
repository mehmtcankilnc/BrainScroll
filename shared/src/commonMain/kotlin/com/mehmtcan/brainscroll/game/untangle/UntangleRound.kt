package com.mehmtcan.brainscroll.game.untangle

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.UntangleGuide
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of an untangle page for the UI. */
data class UntangleSnapshot(
    override val id: String,
    override val index: Int,
    val state: UntangleState,
    /** Time spent so far, as the round counts it (see [UntangleRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == UntangleStatus.Won
    override val kind: GameKind get() = GameKind.Untangle
}

/**
 * One tangle in the feed: an [UntangleGame] plus the time. The time is the time the page is on show (see [onShown]): it runs from the moment the page comes to the screen until the board is won, and stops while the page is left. A result is always a win, its number the dots let go of. The board is saved as it was given (the `answer`), and the
 * place of every dot is saved with the number of moves.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class UntangleRound(
    override val index: Int,
    override val id: String,
    private val puzzle: UntanglePuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = UntangleGame(puzzle)
    private var elapsedMs = 0L
    private var shownSince: Long? = null
    private var touched = false

    override val kind: GameKind get() = GameKind.Untangle

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        val p = puzzle
        return { UntangleGuide.step(p, state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = !touched

    /** Dot [node] is at ([x], [y]) while the finger is down. Returns true if the dot moved. */
    fun move(node: Int, x: Int, y: Int): Boolean {
        val dragging = game.isDragging
        return game.move(node, x, y).also { if (it && !dragging) record() }
    }

    /** The finger is lifted: the drag counts if a dot ended somewhere else. */
    fun drop(): UntangleResult = game.drop().also {
        if (it != UntangleResult.Ignored) record()
        if (it == UntangleResult.Solved) onHidden()
    }

    /** Puts the dots back where the board was given. */
    fun restart(): Boolean = game.restart().also { if (it) record() }

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

    override fun snapshot() = UntangleSnapshot(id, index, game.state, total())

    /** The moves made, then the place of every dot (x and y, three hex digits each, one dot after the other). */
    private fun savedState(): List<String> {
        val state = game.state
        return listOf("n${state.moves}", "p" + state.positions.joinToString("") { it.toString(16).padStart(3, '0') })
    }

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = savedState(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Untangle,
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
        game = GameKind.Untangle,
    )

    companion object {
        /** Rebuilds a round from storage. Null if what was saved cannot be read: a new one is started then. */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): UntangleRound? {
            val puzzle = UntanglePuzzle.decode(stored.answer) ?: return null
            val round = UntangleRound(index, stored.id, puzzle, stored.language, now)
            var moves = 0
            var positions = puzzle.start
            for (entry in stored.guesses) {
                when {
                    entry.length >= 2 && entry[0] == 'n' -> moves = entry.substring(1).toIntOrNull() ?: return null
                    entry.length >= 2 && entry[0] == 'p' -> {
                        val text = entry.substring(1)
                        if (text.length != puzzle.nodeCount * 6) return null
                        positions = text.chunked(3).map { it.toIntOrNull(16) ?: return null }
                    }
                    else -> return null
                }
            }
            if (!round.game.restore(positions, moves)) return null
            round.touched = moves > 0
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.assisted = stored.assisted
            return round
        }
    }
}
