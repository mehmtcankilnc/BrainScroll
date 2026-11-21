package com.mehmtcan.brainscroll.game.rectangles

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.RectGuide
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a rectangles page for the UI. */
data class RectSnapshot(
    override val id: String,
    override val index: Int,
    val state: RectState,
    /** Time spent so far, as the round counts it (see [RectRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == RectStatus.Won
    override val kind: GameKind get() = GameKind.Rectangles
}

/**
 * One board of rectangles in the feed: a [RectGame] plus the time. The time is the time the page is on show (see [onShown]): it runs from
 * the moment the page comes to the screen until the board is covered, and stops while the page is left. A result is always a win and
 * shows only the time. The board is saved as it was given (the `answer`), with the rectangles laid and the number of them made.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class RectRound(
    override val index: Int,
    override val id: String,
    private val puzzle: RectPuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = RectGame(puzzle)
    private var elapsedMs = 0L
    private var shownSince: Long? = null
    private var touched = false

    override val kind: GameKind get() = GameKind.Rectangles

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        val p = puzzle
        return { RectGuide.step(p, state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = !touched

    /** Lays [rect]. */
    fun place(rect: Rect): RectResult = game.place(rect).also {
        if (it == RectResult.Placed || it == RectResult.Solved) touched = true
        if (it == RectResult.Solved) onHidden()
    }

    /** Takes off the rectangle that covers the cell ([column], [row]). Returns true if there was one. */
    fun remove(column: Int, row: Int): Boolean = game.remove(column, row)

    /** Takes every rectangle off. Returns false when there were none. */
    fun restart(): Boolean = game.restart()

    /** The page came to the screen: the clock starts (or goes on), so thinking before the first rectangle counts. */
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

    override fun snapshot() = RectSnapshot(id, index, game.state, total())

    /** `n` and the number of rectangles laid, then every rectangle on the board as four digits (x, y, width, height). */
    private fun savedState(): List<String> {
        val state = game.state
        return listOf("n${state.moves}") + state.rects.map { "${it.x}${it.y}${it.w}${it.h}" }
    }

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = savedState(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Rectangles,
    )

    /** The history row. Its `guesses` is only the number of rectangles laid. */
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
        game = GameKind.Rectangles,
    )

    companion object {
        /** Rebuilds a round from storage. Null if what was saved cannot be read: a new one is started then. */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): RectRound? {
            val puzzle = RectPuzzle.decode(stored.answer) ?: return null
            val round = RectRound(index, stored.id, puzzle, stored.language, now)
            var moves = 0
            val rects = ArrayList<Rect>()
            for (entry in stored.guesses) {
                when {
                    entry.length >= 2 && entry[0] == 'n' -> moves = entry.substring(1).toIntOrNull() ?: return null
                    entry.length == 4 -> {
                        val d = entry.map { it.digitToIntOrNull() ?: return null }
                        rects += Rect(d[0], d[1], d[2], d[3])
                    }
                    else -> return null
                }
            }
            if (!round.game.restore(rects, maxOf(moves, rects.size))) return null
            round.touched = moves > 0 || rects.isNotEmpty()
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.assisted = stored.assisted
            return round
        }
    }
}
