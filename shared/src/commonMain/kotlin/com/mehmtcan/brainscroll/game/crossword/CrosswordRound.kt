package com.mehmtcan.brainscroll.game.crossword

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.CrosswordGuide
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a crossword page for the UI. */
data class CrosswordSnapshot(
    override val id: String,
    override val index: Int,
    val state: CrosswordState,
    /** The language of the clues and the letters (the keyboard follows it). */
    val language: Language,
    /** Time spent so far, as the round counts it (see [CrosswordRound]). */
    val durationMs: Long,
) : PageSnapshot {
    override val isFinished: Boolean get() = state.status == CrosswordStatus.Won
    override val kind: GameKind get() = GameKind.Crossword

    /** The stars this board is worth now (final once it is won): they only count in the daily puzzle. */
    val stars: Int get() = CrosswordScoring.stars(state.mistakes)
}

/**
 * One crossword board in the feed: a [CrosswordGame] plus the time and the list of actions, which is all that is needed to save it
 * and to rebuild it exactly as the player left it (the board is saved as its squares, the language is the one of the round).
 *
 * The time works as in the other games: the pauses between two actions, each counted for at most [MAX_THINK_MILLIS]. The clock
 * starts with the first letter. The game cannot be lost, so a result is always a win.
 */
class CrosswordRound(
    override val index: Int,
    override val id: String,
    private val puzzle: CrosswordPuzzle,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = CrosswordGame(puzzle)

    /** What the player did, in order: "p12:A" puts an A in square 12, "e12" takes a wrong letter back. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var lastActionAt: Long? = null

    val language: Language get() = puzzle.language

    override val kind: GameKind get() = GameKind.Crossword

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        val solution = game.solution
        return { CrosswordGuide.step(state, solution) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()

    fun place(cell: Int, letter: Char): CrosswordResult = game.place(cell, letter).also { if (it != CrosswordResult.Ignored) record("p$cell:$letter") }

    fun erase(cell: Int): CrosswordResult = game.erase(cell).also { if (it != CrosswordResult.Ignored) record("e$cell") }

    private fun record(action: String) {
        actions += action
        val time = now()
        lastActionAt?.let { elapsedMs += minOf(time - it, MAX_THINK_MILLIS).coerceAtLeast(0) }
        lastActionAt = time
    }

    override fun snapshot() = CrosswordSnapshot(id, index, game.state, puzzle.language, elapsedMs)

    override fun toStored() = StoredRound(
        id = id,
        language = puzzle.language,
        answer = puzzle.encode(),
        guesses = actions.toList(),
        input = elapsedMs.toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Crossword,
    )

    /**
     * The history row. Its `guesses` is only the number of letters put and the mistakes, not the list of actions: the cloud table
     * keeps at most 256 characters there, and a long game would not fit. A finished game is never replayed. (The daily puzzle
     * keeps the score code instead, see [CrosswordScoring.encode].)
     */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = puzzle.language,
        answer = puzzle.encode(),
        guesses = listOf(game.state.moves.toString(), game.state.mistakes.toString()),
        outcome = Outcome.WON,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = elapsedMs,
        game = GameKind.Crossword,
    )

    companion object {
        /** A pause between two actions counts for at most this long (a crossword needs thinking). */
        const val MAX_THINK_MILLIS = 30_000L

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read (or has a word the
         * bank no longer has): the caller then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): CrosswordRound? {
            val puzzle = CrosswordPuzzle.decode(stored.language, stored.answer) ?: return null
            val round = CrosswordRound(index, stored.id, puzzle, now)
            for (action in stored.guesses) {
                when (action.firstOrNull()) {
                    'p' -> {
                        val parts = action.drop(1).split(':')
                        if (parts.size != 2 || parts[1].length != 1) return null
                        round.game.place(parts[0].toIntOrNull() ?: return null, parts[1][0])
                    }
                    'e' -> round.game.erase(action.drop(1).toIntOrNull() ?: return null)
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
