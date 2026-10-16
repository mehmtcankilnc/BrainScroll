package com.mehmtcan.brainscroll.game.blockout

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.BlockOutGuide
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a block out page for the UI. */
data class BlockOutSnapshot(
    override val id: String,
    override val index: Int,
    val state: BlockOutState,
    /** Time spent so far, as the round counts it (see [BlockOutRound]). */
    val durationMs: Long,
) : PageSnapshot {
    override val isFinished: Boolean get() = state.status == BlockOutStatus.Won
    override val kind: GameKind get() = GameKind.BlockOut
}

/**
 * One block out board in the feed: a [BlockOutGame] plus the time and the list of actions, which is all that is needed to save it
 * and to rebuild it exactly as the player left it.
 *
 * The time works as in the other games: the pauses between two actions, each counted for at most [MAX_THINK_MILLIS]. The clock
 * starts with the first move. The game cannot be lost, so a result is always a win.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class BlockOutRound(
    override val index: Int,
    override val id: String,
    private val puzzle: BlockOutPuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
    par: Int = BlockOutSolver.shortest(puzzle.size, puzzle.shapes, puzzle.start)?.size ?: 0,
) : FeedRound {
    private val game = BlockOutGame(puzzle, par)

    /** What the player did, in order: "m3:2" slides piece 3 to place 2, "r" restarts. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var lastActionAt: Long? = null

    override val kind: GameKind get() = GameKind.BlockOut

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        val puzzle = puzzle
        return { BlockOutGuide.step(state, puzzle) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()

    fun move(piece: Int, to: Int): BlockOutResult = game.move(piece, to).also { if (it != BlockOutResult.Ignored) record("m$piece:$to") }

    fun restart(): Boolean = game.restart().also { if (it) record(RESTART) }

    private fun record(action: String) {
        actions += action
        val time = now()
        lastActionAt?.let { elapsedMs += minOf(time - it, MAX_THINK_MILLIS).coerceAtLeast(0) }
        lastActionAt = time
    }

    override fun snapshot() = BlockOutSnapshot(id, index, game.state, elapsedMs)

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = actions.toList(),
        input = elapsedMs.toString(),
        position = index,
        assisted = assisted,
        game = GameKind.BlockOut,
    )

    /**
     * The history row. Its `guesses` is only the number of moves, not the list of actions: the cloud table keeps at most 256
     * characters there, and a long game would not fit. A finished game is never replayed.
     */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = puzzle.encode(),
        guesses = listOf(game.state.moves.toString()),
        outcome = Outcome.WON,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = elapsedMs,
        game = GameKind.BlockOut,
    )

    companion object {
        /** A pause between two actions counts for at most this long. */
        const val MAX_THINK_MILLIS = 15_000L
        private const val RESTART = "r"

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read: the caller
         * then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): BlockOutRound? {
            val puzzle = BlockOutPuzzle.decode(stored.answer) ?: return null
            // A board that cannot be solved is not one this game made; it would also leave the guide and the par without an answer.
            val way = BlockOutSolver.shortest(puzzle.size, puzzle.shapes, puzzle.start) ?: return null
            val round = BlockOutRound(index, stored.id, puzzle, stored.language, now, way.size)
            for (action in stored.guesses) {
                when {
                    action == RESTART -> round.game.restart()
                    action.startsWith("m") -> {
                        val parts = action.drop(1).split(':')
                        if (parts.size != 2) return null
                        val piece = parts[0].toIntOrNull() ?: return null
                        val to = parts[1].toIntOrNull() ?: return null
                        round.game.move(piece, to)
                    }
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
