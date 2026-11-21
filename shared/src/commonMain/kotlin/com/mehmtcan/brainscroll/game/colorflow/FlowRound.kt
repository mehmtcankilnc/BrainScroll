package com.mehmtcan.brainscroll.game.colorflow

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.FlowGuide
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a color flow page for the UI. */
data class FlowSnapshot(
    override val id: String,
    override val index: Int,
    val state: FlowState,
    /** Time spent so far, as the round counts it (see [FlowRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == FlowStatus.Won
    override val kind: GameKind get() = GameKind.ColorFlow
}

/**
 * One color flow board in the feed: a [FlowGame] plus the time. A board is saved as it stands, not as the list of what the player
 * did (a stroke can be dozens of cells and the table keeps 256 characters): the strokes so far, and the path of every color with
 * one character for a cell. The time is the time the page is on show (see [onShown]): it runs from the moment the page comes to
 * the screen until the board is won, and stops while the page is left. A result is always a win, its number the strokes.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class FlowRound(
    override val index: Int,
    override val id: String,
    private val puzzle: FlowPuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = FlowGame(puzzle)
    private var elapsedMs = 0L
    private var shownSince: Long? = null
    private var touched = false

    override val kind: GameKind get() = GameKind.ColorFlow

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        val p = puzzle
        return { FlowGuide.step(p, state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = !touched

    /** A stroke begins on [cell]. */
    fun start(cell: Int): Boolean = game.start(cell)

    /** The finger moves on to [cell]. */
    fun extend(cell: Int): Boolean = game.extend(cell)

    /** The finger is lifted: the stroke counts if it changed something. */
    fun end(): FlowResult = game.end().also { if (it != FlowResult.Ignored) record() }

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

    override fun snapshot() = FlowSnapshot(id, index, game.state, total())

    /** The paths, as saved: `n<strokes>`, then `p<color>:<cells>` for each color that has one (a cell is one character, base 36). */
    private fun savedState(): List<String> {
        val state = game.state
        return listOf("n${state.moves}") + state.paths.mapIndexedNotNull { color, path ->
            if (path.isEmpty()) null else "p$color:" + path.joinToString("") { it.toString(36) }
        }
    }

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = savedState(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.ColorFlow,
    )

    /** The history row. Its `guesses` is only the number of strokes (the table keeps at most 256 characters there). */
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
        game = GameKind.ColorFlow,
    )

    companion object {
        /** Rebuilds a round from storage. Null if what was saved cannot be read: a new one is started then. */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): FlowRound? {
            val puzzle = FlowPuzzle.decode(stored.answer) ?: return null
            val round = FlowRound(index, stored.id, puzzle, stored.language, now)
            var moves = 0
            val paths = MutableList(puzzle.colorCount) { emptyList<Int>() }
            for (entry in stored.guesses) {
                when {
                    entry.length >= 2 && entry[0] == 'n' -> moves = entry.substring(1).toIntOrNull() ?: return null
                    entry.length >= 4 && entry[0] == 'p' -> {
                        val split = entry.indexOf(':')
                        val color = entry.substring(1, if (split < 0) return null else split).toIntOrNull() ?: return null
                        if (color !in paths.indices) return null
                        paths[color] = entry.substring(split + 1).map { it.digitToIntOrNull(36) ?: return null }
                    }
                    else -> return null
                }
            }
            if (!round.game.restore(paths, moves)) return null
            round.touched = moves > 0 || paths.any { it.isNotEmpty() }
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.assisted = stored.assisted
            return round
        }
    }
}
