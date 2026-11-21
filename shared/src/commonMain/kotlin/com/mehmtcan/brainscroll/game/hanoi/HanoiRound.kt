package com.mehmtcan.brainscroll.game.hanoi

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.HanoiGuide
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a Hanoi page for the UI. */
data class HanoiSnapshot(
    override val id: String,
    override val index: Int,
    val state: HanoiState,
    /** Time spent so far, as the round counts it (see [HanoiRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == HanoiStatus.Won
    override val kind: GameKind get() = GameKind.Hanoi
}

/**
 * One tower of Hanoi board in the feed: a [HanoiGame] plus the time and the list of actions, which is all that is needed to save
 * it and to rebuild it exactly as the player left it.
 *
 * The time is the time the page is on show (see [onShown]): it runs from the moment the page comes to the screen until the board
 * is won, and stops while the page is left. The game cannot be lost, so a result is always a win.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class HanoiRound(
    override val index: Int,
    override val id: String,
    private val start: List<Int>,
    private val goal: Int,
    private val language: Language,
    private val now: () -> Long = { 0L },
    par: Int = HanoiSolver.distance(start.toIntArray(), goal),
) : FeedRound {
    private val game = HanoiGame(start, goal, par)

    /** What the player did, in order: "m02" moves the top disk of tower 0 onto tower 2, "r" restarts. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var shownSince: Long? = null

    override val kind: GameKind get() = GameKind.Hanoi

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        return { HanoiGuide.step(state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()

    fun move(from: Int, to: Int): HanoiResult = game.move(from, to).also { if (it != HanoiResult.Ignored) record("m$from$to") }

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

    override fun snapshot() = HanoiSnapshot(id, index, game.state, total())

    /** The starting position: a digit (the tower) for every disk from the smallest, then `>` and the goal tower, e.g. `0120>2`. */
    private fun encoded() = start.joinToString("") + ">" + goal

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = encoded(),
        guesses = actions.toList(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Hanoi,
    )

    /**
     * The history row. Its `guesses` is only the number of moves, not the list of actions: the cloud table keeps at most 256
     * characters there, and a long game would not fit. A finished game is never replayed.
     */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = encoded(),
        guesses = listOf(game.state.moves.toString()),
        outcome = Outcome.WON,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = total(),
        game = GameKind.Hanoi,
    )

    companion object {
        private const val RESTART = "r"

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read: the caller
         * then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): HanoiRound? {
            val parts = stored.answer.split('>')
            if (parts.size != 2) return null
            val start = parts[0].map { it.digitToIntOrNull() ?: return null }
            val goal = parts[1].toIntOrNull() ?: return null
            // The par is worked out from the position, so a position that is not one must not get that far.
            if (goal !in 0 until HanoiGame.TOWERS || start.size !in 2..HanoiGame.MAX_DISKS || start.any { it !in 0 until HanoiGame.TOWERS }) return null
            val round = try {
                HanoiRound(index, stored.id, start, goal, stored.language, now)
            } catch (e: IllegalArgumentException) {
                return null
            }
            for (action in stored.guesses) {
                when {
                    action == RESTART -> round.game.restart()
                    action.length == 3 && action[0] == 'm' -> {
                        val from = action[1].digitToIntOrNull() ?: return null
                        val to = action[2].digitToIntOrNull() ?: return null
                        round.game.move(from, to)
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
