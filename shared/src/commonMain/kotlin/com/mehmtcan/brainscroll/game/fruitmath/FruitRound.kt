package com.mehmtcan.brainscroll.game.fruitmath

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.FruitGuide
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a fruit equations page for the UI. */
data class FruitSnapshot(
    override val id: String,
    override val index: Int,
    val state: FruitState,
    /** Time spent so far, as the round counts it (see [FruitRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == FruitStatus.Won
    override val kind: GameKind get() = GameKind.FruitMath
}

/**
 * One board of fruit equations in the feed: a [FruitGame] plus the time. The time is the time the page is on show (see [onShown]): it
 * runs from the moment the page comes to the screen until the board is won, and stops while the page is left. A result is always a win,
 * its number the wrong answers given before the right one. The board is saved as it was given (the `answer`), and the wrong answers so far.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class FruitRound(
    override val index: Int,
    override val id: String,
    private val puzzle: FruitPuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = FruitGame(puzzle)
    private var elapsedMs = 0L
    private var shownSince: Long? = null
    private var touched = false

    override val kind: GameKind get() = GameKind.FruitMath

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        val p = puzzle
        return { FruitGuide.step(p, state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = !touched

    /** Gives [answer] for the last line. */
    fun submit(answer: Int): FruitResult = game.submit(answer).also {
        if (it != FruitResult.Ignored) touched = true
        if (it == FruitResult.Solved) onHidden()
    }

    /**
     * The page came to the screen: the clock starts (or goes on). The time of this round is the time its page was on show, so
     * thinking before the first answer counts, and a puzzle solved with one answer is not solved "in no time".
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

    override fun snapshot() = FruitSnapshot(id, index, game.state, total())

    /** The number of wrong answers so far, after an `m`. */
    private fun savedState(): List<String> = listOf("m${game.state.mistakes}")

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = savedState(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.FruitMath,
    )

    /** The history row. Its `guesses` is only the number of wrong choices. */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = puzzle.encode(),
        guesses = listOf(game.state.mistakes.toString()),
        outcome = Outcome.WON,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = total(),
        game = GameKind.FruitMath,
    )

    companion object {
        /** Rebuilds a round from storage. Null if what was saved cannot be read (a board of an older kind too): a new one is started then. */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): FruitRound? {
            val puzzle = FruitPuzzle.decode(stored.answer) ?: return null
            val round = FruitRound(index, stored.id, puzzle, stored.language, now)
            var mistakes = 0
            for (entry in stored.guesses) {
                when {
                    entry.length >= 2 && entry[0] == 'm' -> mistakes = entry.substring(1).toIntOrNull() ?: return null
                    else -> return null
                }
            }
            if (!round.game.restore(mistakes)) return null
            round.touched = mistakes > 0
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.assisted = stored.assisted
            return round
        }
    }
}
