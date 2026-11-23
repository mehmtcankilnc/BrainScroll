package com.mehmtcan.brainscroll.game.lamps

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.LampGuide
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a lamps page for the UI. */
data class LampSnapshot(
    override val id: String,
    override val index: Int,
    val state: LampState,
    /** Time spent so far, as the round counts it (see [LampRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == LampStatus.Won
    override val kind: GameKind get() = GameKind.Lamps
}

/**
 * One board of lamps in the feed: a [LampGame] plus the time. The time is the time the page is on show (see [onShown]): it runs from the
 * moment the page comes to the screen until the board is solved, and stops while the page is left. A result is always a win and shows
 * only the time. The board is saved as it was given (the `answer`), with the marks and the number of moves made.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class LampRound(
    override val index: Int,
    override val id: String,
    private val puzzle: LampPuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = LampGame(puzzle)
    private var elapsedMs = 0L
    private var shownSince: Long? = null
    private var touched = false

    override val kind: GameKind get() = GameKind.Lamps

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        val p = puzzle
        return { LampGuide.step(p, state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = !touched

    /** Taps the cell number [cell]. */
    fun tap(cell: Int): LampResult = game.tap(cell).also {
        if (it != LampResult.Ignored) touched = true
        if (it == LampResult.Solved) onHidden()
    }

    /** Takes every mark off. Returns false when there were none. */
    fun restart(): Boolean = game.restart()

    /** The page came to the screen: the clock starts (or goes on), so thinking before the first tap counts. */
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

    override fun snapshot() = LampSnapshot(id, index, game.state, total())

    /** `n` and the number of moves, then `m` and a digit for every cell (0 empty, 1 lamp, 2 dot; walls are 0). */
    private fun savedState(): List<String> {
        val state = game.state
        return listOf("n${state.moves}", "m" + state.marks.joinToString("") { it.ordinal.toString() })
    }

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = savedState(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Lamps,
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
        game = GameKind.Lamps,
    )

    companion object {
        /** Rebuilds a round from storage. Null if what was saved cannot be read: a new one is started then. */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): LampRound? {
            val puzzle = LampPuzzle.decode(stored.answer) ?: return null
            val round = LampRound(index, stored.id, puzzle, stored.language, now)
            var moves = 0
            var marks: List<LampMark>? = null
            for (entry in stored.guesses) {
                when {
                    entry.length >= 2 && entry[0] == 'n' -> moves = entry.substring(1).toIntOrNull() ?: return null
                    entry.length >= 2 && entry[0] == 'm' -> marks = entry.substring(1).map { ch ->
                        LampMark.entries.getOrNull(ch.digitToIntOrNull() ?: return null) ?: return null
                    }
                    else -> return null
                }
            }
            val saved = marks ?: List(puzzle.size * puzzle.size) { LampMark.Empty }
            if (!round.game.restore(saved, maxOf(moves, saved.count { it != LampMark.Empty }))) return null
            round.touched = moves > 0 || saved.any { it != LampMark.Empty }
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.assisted = stored.assisted
            return round
        }
    }
}
