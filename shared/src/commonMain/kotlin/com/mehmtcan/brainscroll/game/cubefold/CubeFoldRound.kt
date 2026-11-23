package com.mehmtcan.brainscroll.game.cubefold

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.CubeGuide
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of an cube folding page for the UI. */
data class CubeFoldSnapshot(
    override val id: String,
    override val index: Int,
    val state: CubeState,
    /** Time spent so far, as the round counts it (see [CubeFoldRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same run with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status != CubeStatus.Playing
    override val isWon: Boolean get() = state.status == CubeStatus.Won
    override val kind: GameKind get() = GameKind.CubeFold
}

/**
 * One run of cube folding in the feed: an [CubeFoldGame] plus the time (the time the page is on show, see [onShown]). The result is the number
 * of questions answered right; answering all of them is a win, one wrong pick is a loss. A run is saved as it was given (the `answer`)
 * with the questions answered; the question being asked is shown again when the page comes back (it needs no memory, only a look).
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class CubeFoldRound(
    override val index: Int,
    override val id: String,
    private val puzzle: CubeFoldPuzzle,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = CubeFoldGame(puzzle)
    private var elapsedMs = 0L
    private var shownSince: Long? = null
    private var touched = false

    override val kind: GameKind get() = GameKind.CubeFold

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        return { CubeGuide.step(state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = !touched

    /** Picks the cube at [pick] of the question. */
    fun pick(pick: Int): CubeResult = game.pick(pick).also {
        if (it != CubeResult.Ignored) touched = true
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

    override fun snapshot() = CubeFoldSnapshot(id, index, game.state, total())

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = listOf("r${game.state.cleared}"),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.CubeFold,
    )

    /** The history row. Its `guesses` is only the number of questions answered right. */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = puzzle.encode(),
        guesses = listOf(game.state.cleared.toString()),
        outcome = if (game.state.status == CubeStatus.Won) Outcome.WON else Outcome.LOST,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = total(),
        game = GameKind.CubeFold,
    )

    companion object {
        /** Rebuilds a round from storage. Null if what was saved cannot be read: a new one is started then. */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): CubeFoldRound? {
            val puzzle = CubeFoldPuzzle.decode(stored.answer) ?: return null
            val round = CubeFoldRound(index, stored.id, puzzle, stored.language, now)
            var cleared = 0
            for (entry in stored.guesses) {
                when {
                    entry.length >= 2 && entry[0] == 'r' -> cleared = entry.substring(1).toIntOrNull() ?: return null
                    else -> return null
                }
            }
            if (!round.game.restore(cleared)) return null
            round.touched = cleared > 0
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.assisted = stored.assisted
            return round
        }
    }
}
