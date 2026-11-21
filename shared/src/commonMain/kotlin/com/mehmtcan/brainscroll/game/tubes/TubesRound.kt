package com.mehmtcan.brainscroll.game.tubes

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.TubesGuide
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a tubes page for the UI. */
data class TubesSnapshot(
    override val id: String,
    override val index: Int,
    val state: TubesState,
    /** Time spent so far, as the round counts it (see [TubesRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == TubesStatus.Won
    override val kind: GameKind get() = GameKind.Tubes
}

/**
 * One board of color tubes in the feed: a [TubesGame] plus the time and the list of actions, which is all that is needed to save
 * it and to rebuild it exactly as the player left it.
 *
 * The time is the time the page is on show (see [onShown]): it runs from the moment the page comes to the screen until the board
 * is won, and stops while the page is left.
 *
 * [language] only fills the language of the saved result (the table needs one); tubes have no language of their own.
 */
class TubesRound(
    override val index: Int,
    override val id: String,
    private val layout: List<List<Int>>,
    private val language: Language,
    private val now: () -> Long = { 0L },
    capacity: Int = TubesGame.DEFAULT_CAPACITY,
) : FeedRound {
    private val game = TubesGame(layout, capacity)

    /** What the player did, in order: "3>5" is a pour, "u" an undo, "r" a restart. Saved on the device only. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var shownSince: Long? = null

    override val kind: GameKind get() = GameKind.Tubes

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        return { TubesGuide.step(state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()


    fun pour(from: Int, to: Int): PourResult {
        val result = game.pour(from, to)
        if (result == PourResult.Poured) record("$from>$to")
        return result
    }

    fun undo(): Boolean = game.undo().also { if (it) record(UNDO) }

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

    override fun snapshot() = TubesSnapshot(id, index, game.state, total())

    private fun encodedLayout() = layout.joinToString("|") { tube -> tube.joinToString("") }

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = encodedLayout(),
        guesses = actions.toList(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Tubes,
    )

    /**
     * The history row. Its `guesses` is only the number of pours, not the list of actions: the cloud table keeps at
     * most 256 characters there, and a long game with many undos would not fit. A finished game is never replayed.
     */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = encodedLayout(),
        guesses = listOf(game.state.moves.toString()),
        outcome = Outcome.WON,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = total(),
        game = GameKind.Tubes,
    )

    companion object {
        private const val UNDO = "u"
        private const val RESTART = "r"

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read: the
         * caller then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): TubesRound? {
            val layout = stored.answer.split('|').map { tube ->
                tube.map { ch -> ch.digitToIntOrNull() ?: return null }
            }
            val round = try {
                TubesRound(index, stored.id, layout, stored.language, now)
            } catch (e: IllegalArgumentException) {
                return null
            }
            for (action in stored.guesses) {
                when (action) {
                    UNDO -> round.game.undo()
                    RESTART -> round.game.restart()
                    else -> {
                        val (from, to) = action.split('>').map { it.toIntOrNull() ?: return null }.takeIf { it.size == 2 } ?: return null
                        round.game.pour(from, to)
                    }
                }
                round.actions += action
            }
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.assisted = stored.assisted
            return round
        }
    }
}
