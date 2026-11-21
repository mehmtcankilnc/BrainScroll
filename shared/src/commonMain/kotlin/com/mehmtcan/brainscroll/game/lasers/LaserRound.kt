package com.mehmtcan.brainscroll.game.lasers

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.LaserGuide
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a laser mirrors page for the UI. */
data class LaserSnapshot(
    override val id: String,
    override val index: Int,
    val state: LaserState,
    /** Time spent so far, as the round counts it (see [LaserRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == LaserStatus.Won
    override val kind: GameKind get() = GameKind.Lasers
}

/**
 * One laser mirrors board in the feed: a [LaserGame] plus the time. The time is the time the page is on show (see [onShown]): it runs from the moment the page comes to the screen until the board is won, and stops while the page is left. A result is always a win, its number the turns made.
 * The board is saved as it was given (the `answer`), and the way the mirrors stand now is saved with the turns.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class LaserRound(
    override val index: Int,
    override val id: String,
    private val puzzle: LaserPuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = LaserGame(puzzle)
    private var elapsedMs = 0L
    private var shownSince: Long? = null
    private var touched = false

    override val kind: GameKind get() = GameKind.Lasers

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        val p = puzzle
        return { LaserGuide.step(p, state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = !touched

    /** Turns the mirror on [cell]. */
    fun turn(cell: Int): LaserResult = game.turn(cell).also {
        if (it != LaserResult.Ignored) record()
        if (it == LaserResult.Solved) onHidden()
    }

    /** Puts every mirror back as the board was given. */
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

    override fun snapshot() = LaserSnapshot(id, index, game.state, total())

    /** The turns made, then the mirrors as they stand (a `1` for each `/` and a `2` for each `\`, in cell order). */
    private fun savedState(): List<String> {
        val state = game.state
        return listOf("n${state.turns}", "m" + puzzle.mirrorCells.joinToString("") { state.mirrors[it].toString() })
    }

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = savedState(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Lasers,
    )

    /** The history row. Its `guesses` is only the number of turns. */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = puzzle.encode(),
        guesses = listOf(game.state.turns.toString()),
        outcome = Outcome.WON,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = total(),
        game = GameKind.Lasers,
    )

    companion object {
        /** Rebuilds a round from storage. Null if what was saved cannot be read: a new one is started then. */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): LaserRound? {
            val puzzle = LaserPuzzle.decode(stored.answer) ?: return null
            val round = LaserRound(index, stored.id, puzzle, stored.language, now)
            var turns = 0
            var mirrors = puzzle.mirrors
            for (entry in stored.guesses) {
                when {
                    entry.length >= 2 && entry[0] == 'n' -> turns = entry.substring(1).toIntOrNull() ?: return null
                    entry.length >= 2 && entry[0] == 'm' -> {
                        val text = entry.substring(1)
                        if (text.length != puzzle.mirrorCells.size) return null
                        val next = puzzle.mirrors.toMutableList()
                        for ((i, cell) in puzzle.mirrorCells.withIndex()) next[cell] = text[i].digitToIntOrNull()?.takeIf { it in 1..2 } ?: return null
                        mirrors = next
                    }
                    else -> return null
                }
            }
            if (!round.game.restore(mirrors, turns)) return null
            round.touched = turns > 0
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.assisted = stored.assisted
            return round
        }
    }
}
