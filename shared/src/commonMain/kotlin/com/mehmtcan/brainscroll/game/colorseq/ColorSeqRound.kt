package com.mehmtcan.brainscroll.game.colorseq

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.ColorSeqGuide
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/**
 * Immutable picture of a color sequence page for the UI. [onShow] is true while the page is on the screen. The lights are played when
 * [ColorSeqState.showToken] goes up (start pressed, round cleared), never because the page came back to the screen: a run that was left in
 * the middle goes on without seeing the lights again (it can be started over).
 */
data class ColorSeqSnapshot(
    override val id: String,
    override val index: Int,
    val state: ColorSeqState,
    val onShow: Boolean,
    /** Time spent so far, as the round counts it (see [ColorSeqRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same run with no time and no showing on it, to tell whether a page is still as it was (they change by themselves). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L, onShow = false)

    override val isFinished: Boolean get() = state.status != ColorSeqStatus.Playing
    override val isWon: Boolean get() = state.status == ColorSeqStatus.Won
    override val kind: GameKind get() = GameKind.ColorSeq
}

/**
 * One run of the color sequence in the feed: a [ColorSeqGame] plus the time (the time the page is on show, see [onShown]). The result is the
 * number of rounds cleared; clearing every round is a win, one wrong button is a loss. A run is saved as it was given (the `answer`) with
 * the rounds cleared; the buttons touched in the round being played are not (the round is shown again when the page comes back).
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class ColorSeqRound(
    override val index: Int,
    override val id: String,
    puzzle: ColorSeqPuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = ColorSeqGame(puzzle)
    private var elapsedMs = 0L
    private var shownSince: Long? = null
    private var touched = false

    override val kind: GameKind get() = GameKind.ColorSeq

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        return { ColorSeqGuide.step(state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = !touched

    /** The player pressed start: the first round is lit. Returns false if it was started already. */
    fun start(): Boolean = game.start().also { if (it) touched = true }

    /** Starts the run over with other lights. Returns false if there is nothing to start over (not started yet, or the run is over). */
    fun restart(): Boolean = game.restart(game.freshPuzzle())

    /** Touches button [button]. */
    fun tap(button: Int): ColorSeqResult = game.tap(button).also {
        if (it != ColorSeqResult.Ignored) touched = true
        if (isFinished) onHidden()
    }

    /** The page came to the screen: the clock starts (or goes on). */
    override fun onShown() {
        if (!isFinished && shownSince == null) shownSince = now()
    }

    /** The page left the screen (swiped away, another tab, the app in the background) or the run is over: the clock stops. */
    override fun onHidden() {
        shownSince?.let { elapsedMs += (now() - it).coerceAtLeast(0) }
        shownSince = null
    }

    /** The time so far: what was counted before, and the time since the page came to the screen if it is on show now. */
    private fun total(): Long = elapsedMs + (shownSince?.let { (now() - it).coerceAtLeast(0) } ?: 0L)

    override fun snapshot() = ColorSeqSnapshot(id, index, game.state, shownSince != null, total())

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = game.puzzle.encode(),
        guesses = listOf("r${game.state.roundsCleared}") + (if (game.state.started) listOf("s1") else emptyList()),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.ColorSeq,
    )

    /** The history row. Its `guesses` is only the number of rounds cleared. */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = game.puzzle.encode(),
        guesses = listOf(game.state.roundsCleared.toString()),
        outcome = if (game.state.status == ColorSeqStatus.Won) Outcome.WON else Outcome.LOST,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = total(),
        game = GameKind.ColorSeq,
    )

    companion object {
        /** Rebuilds a round from storage. Null if what was saved cannot be read: a new one is started then. */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): ColorSeqRound? {
            val puzzle = ColorSeqPuzzle.decode(stored.answer) ?: return null
            val round = ColorSeqRound(index, stored.id, puzzle, stored.language, now)
            var cleared = 0
            var started = false
            for (entry in stored.guesses) {
                when {
                    entry.length >= 2 && entry[0] == 'r' -> cleared = entry.substring(1).toIntOrNull() ?: return null
                    entry == "s1" -> started = true
                    else -> return null
                }
            }
            if (!round.game.restore(cleared, started)) return null
            round.touched = started
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.assisted = stored.assisted
            return round
        }
    }
}
