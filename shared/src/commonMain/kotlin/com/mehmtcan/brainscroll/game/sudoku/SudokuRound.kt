package com.mehmtcan.brainscroll.game.sudoku

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.SudokuGuide
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a sudoku page for the UI. */
data class SudokuSnapshot(
    override val id: String,
    override val index: Int,
    val state: SudokuState,
    /** Time spent so far, as the round counts it (see [SudokuRound]). */
    val durationMs: Long,
    override val kind: GameKind = GameKind.Sudoku,
) : PageSnapshot {
    override val isFinished: Boolean get() = state.status != SudokuStatus.Playing
    override val isWon: Boolean get() = state.status == SudokuStatus.Won
}

/**
 * One sudoku board in the feed (the classic one or the jigsaw one, see [SudokuVariant]): a [SudokuGame] plus the time and the list of actions, which is all that is needed to
 * save it and to rebuild it exactly as the player left it (the board itself comes from [seed] and [clues]).
 *
 * The time works as in the other games: the pauses between two actions, each counted for at most [MAX_THINK_MILLIS]
 * (longer than in the other games, a sudoku needs more thinking). The clock starts with the first action, notes included.
 *
 * [language] only fills the language of the saved result (the table needs one); sudoku has no language.
 */
class SudokuRound(
    override val index: Int,
    override val id: String,
    private val seed: Int,
    private val language: Language,
    private val now: () -> Long = { 0L },
    private val clues: Int = SudokuGenerator.DEFAULT_CLUES,
    private val variant: SudokuVariant = SudokuVariant.Classic,
) : FeedRound {
    private val game = SudokuGame(seed, clues, variant)

    /** What the player did, in order: "p12:5" places a 5 in cell 12, "n12:5" toggles the note 5, "e12" erases. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var lastActionAt: Long? = null

    override val kind: GameKind get() = variant.kind

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        val solution = game.solution
        return { SudokuGuide.step(state, solution) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()


    fun place(cell: Int, digit: Int): SudokuResult = game.place(cell, digit).also { if (it != SudokuResult.Ignored) record("p$cell:$digit") }

    fun toggleNote(cell: Int, digit: Int): SudokuResult = game.toggleNote(cell, digit).also { if (it != SudokuResult.Ignored) record("n$cell:$digit") }

    fun erase(cell: Int): SudokuResult = game.erase(cell).also { if (it != SudokuResult.Ignored) record("e$cell") }

    private fun record(action: String) {
        actions += action
        val time = now()
        lastActionAt?.let { elapsedMs += minOf(time - it, MAX_THINK_MILLIS).coerceAtLeast(0) }
        lastActionAt = time
    }

    override fun snapshot() = SudokuSnapshot(id, index, game.state, elapsedMs, variant.kind)

    /** "9x9x36:seed": the size, the number of givens and the seed. */
    private fun encodedBoard() = "9x9x$clues:$seed"

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = encodedBoard(),
        guesses = actions.toList(),
        input = elapsedMs.toString(),
        position = index,
        assisted = assisted,
        game = variant.kind,
    )

    /**
     * The history row. Its `guesses` is only the number of moves (digits placed), not the list of actions: the cloud
     * table keeps at most 256 characters there, and a long game would not fit. A finished game is not replayed.
     */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = encodedBoard(),
        guesses = listOf(game.state.moves.toString()),
        outcome = if (game.state.status == SudokuStatus.Won) Outcome.WON else Outcome.LOST,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = elapsedMs,
        game = variant.kind,
    )

    companion object {
        /** A pause between two actions counts for at most this long. */
        const val MAX_THINK_MILLIS = 30_000L

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read: the
         * caller then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): SudokuRound? {
            val variant = SudokuVariant.of(stored.game) ?: return null
            val parts = stored.answer.split(':')
            if (parts.size != 2) return null
            val size = parts[0].split('x').map { it.toIntOrNull() ?: return null }
            if (size.size != 3 || size[0] != 9 || size[1] != 9) return null
            val seed = parts[1].toIntOrNull() ?: return null
            val round = try {
                SudokuRound(index, stored.id, seed, stored.language, now, size[2], variant)
            } catch (e: IllegalArgumentException) {
                return null
            }
            for (action in stored.guesses) {
                val args = action.drop(1).split(':').map { it.toIntOrNull() ?: return null }
                when (action.firstOrNull()) {
                    'p' -> if (args.size == 2) round.game.place(args[0], args[1]) else return null
                    'n' -> if (args.size == 2) round.game.toggleNote(args[0], args[1]) else return null
                    'e' -> if (args.size == 1) round.game.erase(args[0]) else return null
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
