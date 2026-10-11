package com.mehmtcan.brainscroll.game.mines

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.MinesGuide
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a minesweeper page for the UI. */
data class MinesSnapshot(
    override val id: String,
    override val index: Int,
    val state: MinesState,
    /** Time spent so far, as the round counts it (see [MinesRound]). */
    val durationMs: Long,
) : PageSnapshot {
    override val isFinished: Boolean get() = state.status != MinesStatus.Playing
    override val isWon: Boolean get() = state.status == MinesStatus.Won
    override val kind: GameKind get() = GameKind.Mines
}

/**
 * One minesweeper board in the feed: a [MinesGame] plus the time and the list of actions, which is all that is needed
 * to save it and to rebuild it exactly as the player left it (the board itself comes from [seed] and the first open).
 *
 * The time works as in the other games: the pauses between two actions, each counted for at most [MAX_THINK_MILLIS].
 * The clock starts with the first action, flags included.
 *
 * [language] only fills the language of the saved result (the table needs one); minesweeper has no language.
 */
class MinesRound(
    override val index: Int,
    override val id: String,
    private val seed: Int,
    private val language: Language,
    private val now: () -> Long = { 0L },
    private val rows: Int = MinesGame.DEFAULT_ROWS,
    private val cols: Int = MinesGame.DEFAULT_COLS,
    private val mineCount: Int = MinesGame.DEFAULT_MINES,
) : FeedRound {
    private val game = MinesGame(rows, cols, mineCount, seed)

    /** What the player did, in order: "o12" opens cell 12, "f12" flags or unflags it, "c12" chords on it. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var lastActionAt: Long? = null

    override val kind: GameKind get() = GameKind.Mines

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        return { MinesGuide.step(state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()


    fun open(cell: Int): MinesResult = game.open(cell).also { if (it != MinesResult.Ignored) record("o$cell") }

    fun flag(cell: Int): Boolean = game.toggleFlag(cell).also { if (it) record("f$cell") }

    fun chord(cell: Int): MinesResult = game.chord(cell).also { if (it != MinesResult.Ignored) record("c$cell") }

    private fun record(action: String) {
        actions += action
        val time = now()
        lastActionAt?.let { elapsedMs += minOf(time - it, MAX_THINK_MILLIS).coerceAtLeast(0) }
        lastActionAt = time
    }

    override fun snapshot() = MinesSnapshot(id, index, game.state, elapsedMs)

    /** "8x8x10:seed:first": the size, the seed and the cell of the first open (-1 before that). */
    private fun encodedBoard() = "${rows}x${cols}x$mineCount:$seed:${game.firstOpen}"

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = encodedBoard(),
        guesses = actions.toList(),
        input = elapsedMs.toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Mines,
    )

    /**
     * The history row. Its `guesses` is only the number of moves (opens and chords), not the list of actions: the
     * cloud table keeps at most 256 characters there, and a long game would not fit. A finished game is not replayed.
     */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = encodedBoard(),
        guesses = listOf(game.state.moves.toString()),
        outcome = if (game.state.status == MinesStatus.Won) Outcome.WON else Outcome.LOST,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = elapsedMs,
        game = GameKind.Mines,
    )

    companion object {
        /** A pause between two actions counts for at most this long. */
        const val MAX_THINK_MILLIS = 15_000L

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read: the
         * caller then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): MinesRound? {
            val parts = stored.answer.split(':')
            if (parts.size != 3) return null
            val size = parts[0].split('x').map { it.toIntOrNull() ?: return null }
            if (size.size != 3) return null
            val seed = parts[1].toIntOrNull() ?: return null
            val round = try {
                MinesRound(index, stored.id, seed, stored.language, now, size[0], size[1], size[2])
            } catch (e: IllegalArgumentException) {
                return null
            }
            for (action in stored.guesses) {
                val cell = action.drop(1).toIntOrNull() ?: return null
                when (action.firstOrNull()) {
                    'o' -> round.game.open(cell)
                    'f' -> round.game.toggleFlag(cell)
                    'c' -> round.game.chord(cell)
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
