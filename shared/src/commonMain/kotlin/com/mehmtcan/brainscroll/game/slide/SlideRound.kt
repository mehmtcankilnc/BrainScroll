package com.mehmtcan.brainscroll.game.slide

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.SlideGuide
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a sliding puzzle page for the UI. */
data class SlideSnapshot(
    override val id: String,
    override val index: Int,
    val state: SlideState,
    /** Time spent so far, as the round counts it (see [SlideRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == SlideStatus.Won
    override val kind: GameKind get() = GameKind.Slide
}

/**
 * One sliding puzzle board in the feed: a [SlideGame] plus the time and the list of actions, which is all that is needed to save
 * it and to rebuild it exactly as the player left it (the board itself comes from [seed]).
 *
 * The time is the time the page is on show (see [onShown]): it runs from the moment the page comes to the screen until the board
 * is won, and stops while the page is left. The game cannot be lost, so a result is always a win.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class SlideRound(
    override val index: Int,
    override val id: String,
    private val seed: Int,
    private val language: Language,
    private val now: () -> Long = { 0L },
    private val cols: Int = SlideGame.DEFAULT_COLS,
    private val rows: Int = SlideGame.DEFAULT_ROWS,
    private val scramble: Int = SlideGame.DEFAULT_SCRAMBLE,
) : FeedRound {
    private val game = SlideGame(seed, cols, rows, scramble)

    /** What the player did, in order: "m7" slides from cell 7, "r" restarts. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var shownSince: Long? = null

    override val kind: GameKind get() = GameKind.Slide

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        return { SlideGuide.step(state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()


    fun slide(cell: Int): SlideResult = game.tap(cell).also { if (it != SlideResult.Ignored) record("m$cell") }

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

    override fun snapshot() = SlideSnapshot(id, index, game.state, total())

    /** "3x4x80:seed": the columns, the rows, the number of random slides the board was made with, and the seed. */
    private fun encodedBoard() = "${cols}x${rows}x$scramble:$seed"

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = encodedBoard(),
        guesses = actions.toList(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Slide,
    )

    /**
     * The history row. Its `guesses` is only the number of moves, not the list of actions: the cloud table keeps at most
     * 256 characters there, and a long game would not fit. A finished game is never replayed.
     */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = encodedBoard(),
        guesses = listOf(game.state.moves.toString()),
        outcome = Outcome.WON,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = total(),
        game = GameKind.Slide,
    )

    companion object {
        private const val RESTART = "r"

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read: the
         * caller then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): SlideRound? {
            val parts = stored.answer.split(':')
            if (parts.size != 2) return null
            val shape = parts[0].split('x').map { it.toIntOrNull() ?: return null }
            if (shape.size != 3) return null
            val seed = parts[1].toIntOrNull() ?: return null
            val round = try {
                SlideRound(index, stored.id, seed, stored.language, now, shape[0], shape[1], shape[2])
            } catch (e: IllegalArgumentException) {
                return null
            }
            for (action in stored.guesses) {
                when {
                    action == RESTART -> round.game.restart()
                    action.startsWith("m") -> round.game.tap(action.drop(1).toIntOrNull() ?: return null)
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
