package com.mehmtcan.brainscroll.game.crosssum

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.CrossGuide
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a cross sums page for the UI. */
data class CrossSnapshot(
    override val id: String,
    override val index: Int,
    val state: CrossState,
    /** Time spent so far, as the round counts it (see [CrossRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == CrossStatus.Won
    override val kind: GameKind get() = GameKind.CrossSum
}

/**
 * One board of cross sums in the feed: a [CrossGame] plus the time. The time is the time the page is on show (see [onShown]): it runs
 * from the moment the page comes to the screen until the board is solved, and stops while the page is left. A result is always a win
 * and shows only the time. The board is saved as it was given (the `answer`), with the digits the player put and the moves made.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class CrossRound(
    override val index: Int,
    override val id: String,
    private val puzzle: CrossPuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = CrossGame(puzzle)
    private var elapsedMs = 0L
    private var shownSince: Long? = null
    private var touched = false

    override val kind: GameKind get() = GameKind.CrossSum

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        return { CrossGuide.step(state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = !touched

    /** Puts [digit] (0 empties the cell) in the cell number [cell]. */
    fun put(cell: Int, digit: Int): CrossResult = game.put(cell, digit).also {
        if (it != CrossResult.Ignored) touched = true
        if (it == CrossResult.Solved) onHidden()
    }

    /** Takes every digit the player put out. Returns false when there were none. */
    fun restart(): Boolean = game.restart()

    /** The page came to the screen: the clock starts (or goes on), so thinking before the first digit counts. */
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

    override fun snapshot() = CrossSnapshot(id, index, game.state, total())

    /** `n` and the number of moves, then `m` and a digit for every cell the player fills (0 empty). */
    private fun savedState(): List<String> {
        val state = game.state
        return listOf("n${state.moves}", "m" + puzzle.openCells.joinToString("") { state.values[it].toString() })
    }

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = savedState(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.CrossSum,
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
        game = GameKind.CrossSum,
    )

    companion object {
        /** Rebuilds a round from storage. Null if what was saved cannot be read: a new one is started then. */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): CrossRound? {
            val puzzle = CrossPuzzle.decode(stored.answer) ?: return null
            val round = CrossRound(index, stored.id, puzzle, stored.language, now)
            var moves = 0
            var digits: List<Int>? = null
            for (entry in stored.guesses) {
                when {
                    entry.length >= 2 && entry[0] == 'n' -> moves = entry.substring(1).toIntOrNull() ?: return null
                    entry.length >= 2 && entry[0] == 'm' -> digits = entry.substring(1).map { it.digitToIntOrNull() ?: return null }
                    else -> return null
                }
            }
            val saved = digits ?: List(puzzle.openCells.size) { 0 }
            if (!round.game.restore(saved, maxOf(moves, saved.count { it > 0 }))) return null
            round.touched = moves > 0 || saved.any { it > 0 }
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.assisted = stored.assisted
            return round
        }
    }
}
