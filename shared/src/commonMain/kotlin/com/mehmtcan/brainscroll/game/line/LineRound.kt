package com.mehmtcan.brainscroll.game.line

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a one-line page for the UI. */
data class LineSnapshot(
    override val id: String,
    override val index: Int,
    val puzzle: LinePuzzle,
    val state: LineState,
    /** Time spent so far, as the round counts it (see [LineRound]). */
    val durationMs: Long,
    /** The cells the last hint showed. Empty until a hint is asked for, and again after the next change of the line. */
    val hint: List<Int>,
) : PageSnapshot {
    override val isFinished: Boolean get() = state.status == LineStatus.Won
    override val kind: GameKind get() = GameKind.Line

    /** The stars this board is worth now (final once it is won). Only the daily board shows them. */
    val stars: Int get() = LineScoring.stars(state.undos, state.hintsUsed)
}

/** The stars of a finished daily one-line result: its only guess is the score code (see [LineScoring.encode]). */
val FinishedRound.lineStars: Int
    get() = LineScoring.starsOf(guesses.firstOrNull()?.toIntOrNull() ?: 1)

/**
 * One one-line board: a [LineGame] plus the time and the list of actions, which is all that is needed to save it and
 * to rebuild it exactly as the player left it.
 *
 * The time is the time the page was on show (as in the other timed games): it starts when the board comes to the screen, not with the
 * first touch, so looking at the board and working the line out in the head counts, and a board solved with a few quick drags is not
 * solved "in no time". It stops when the page leaves the screen (another tab, the app in the background) and when the board is done.
 * The game cannot be lost, so a result is always a win.
 *
 * The hint button is the game's own help, and a round in which it was used counts as helped ([assisted]) like a round of
 * another game that the guide helped with: it is left out of the statistics. There is no guide step. The page keeps two
 * counts: the mistakes (the undos) and the moves; the result keeps the moves.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class LineRound(
    override val index: Int,
    override val id: String,
    private val puzzle: LinePuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = LineGame(puzzle)

    /** What the player did, in order: "d12" drag over cell 12, "t12" tap on it, "u" undo, "r" restart, "h" hint. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var shownSince: Long? = null
    private var hint: List<Int> = emptyList()

    override val kind: GameKind get() = GameKind.Line

    override val assisted: Boolean get() = game.state.hintsUsed > 0

    override fun markAssisted() = Unit

    override fun guideTask(): GuideTask? = null

    override val isFinished: Boolean get() = game.isFinished

    // A board that was only looked at is still saved once the page has been left: its time must not start again from nothing.
    override val isUntouched: Boolean get() = actions.isEmpty() && elapsedMs == 0L

    fun drag(cell: Int): LineResult = game.drag(cell).also { if (changes(it)) record("d$cell") }

    fun tap(cell: Int): LineResult = game.tap(cell).also { if (changes(it)) record("t$cell") }

    fun undo(): Boolean = game.undo().also { if (it) record("u") }

    fun restart(): Boolean = game.restart().also { if (it) record("r") }

    /** The next cells of the line, or null when there is nothing to show (the board is done, or the line leads nowhere). */
    fun showHint(): List<Int>? = game.hint().also {
        if (it != null) {
            record("h")
            hint = it
        }
    }

    private fun changes(result: LineResult) = when (result) {
        LineResult.Started, LineResult.Extended, LineResult.Retracted, LineResult.Cut, LineResult.Solved -> true
        LineResult.Ignored, LineResult.WrongOrder, LineResult.EndTooEarly -> false
    }

    private fun record(action: String) {
        actions += action
        if (action != "h") hint = emptyList()
        // The board is done: the clock stops there.
        if (game.isFinished) onHidden()
    }

    /** The page came to the screen: the clock starts (or goes on). */
    override fun onShown() {
        if (!isFinished && shownSince == null) shownSince = now()
    }

    /** The page left the screen or the board is done: the clock stops. */
    override fun onHidden() {
        shownSince?.let { elapsedMs += (now() - it).coerceAtLeast(0) }
        shownSince = null
    }

    /** The time so far: what was counted before, and the time since the page came to the screen if it is on show now. */
    private fun total(): Long = elapsedMs + (shownSince?.let { (now() - it).coerceAtLeast(0) } ?: 0L)

    override fun snapshot() = LineSnapshot(id, index, puzzle, game.state, total(), hint)

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = actions.toList(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Line,
    )

    /**
     * The history row. Its `answer` is only the size (the table keeps at most 32 characters there); its guesses are the
     * number of moves, then the undos and the hints (the daily puzzle works its score code out of those two). A finished
     * board is never replayed.
     */
    override fun toFinished(finishedAt: Long): FinishedRound {
        val state = game.state
        return FinishedRound(
            id = id,
            mode = Mode.ENDLESS,
            language = language,
            answer = "${puzzle.size}x${puzzle.size}",
            guesses = listOf(maxOf(1, state.moves).toString(), state.undos.toString(), state.hintsUsed.toString()),
            outcome = Outcome.WON,
            assisted = assisted,
            finishedAt = finishedAt,
            durationMs = total(),
            game = GameKind.Line,
        )
    }

    companion object {
        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read (or the
         * board has no line): the caller then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): LineRound? {
            val puzzle = LinePuzzle.decode(stored.answer) ?: return null
            val round = LineRound(index, stored.id, puzzle, stored.language, now)
            for (action in stored.guesses) {
                when {
                    action == "u" -> round.game.undo()
                    action == "r" -> round.game.restart()
                    action == "h" -> round.game.hint()
                    action.startsWith("d") -> round.game.drag(action.drop(1).toIntOrNull() ?: return null)
                    action.startsWith("t") -> round.game.tap(action.drop(1).toIntOrNull() ?: return null)
                    else -> return null
                }
                round.actions += action
            }
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            return round
        }
    }
}
