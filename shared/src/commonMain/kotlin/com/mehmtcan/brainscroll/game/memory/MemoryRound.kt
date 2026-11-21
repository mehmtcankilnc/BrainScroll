package com.mehmtcan.brainscroll.game.memory

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.MemoryGuide
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a memory page for the UI. */
data class MemorySnapshot(
    override val id: String,
    override val index: Int,
    val state: MemoryState,
    /** Time spent so far, as the round counts it (see [MemoryRound]). */
    val durationMs: Long,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status != MemoryStatus.Playing
    override val isWon: Boolean get() = state.status == MemoryStatus.Won
    override val kind: GameKind get() = GameKind.Memory
}

/**
 * One memory board in the feed: a [MemoryGame] plus the time and the order of the flips, which is all that is needed to save it
 * and to rebuild it exactly as the player left it.
 *
 * The time is the time the page is on show (see [onShown]): it runs from the moment the page comes to the screen until the board
 * is won, and stops while the page is left.
 *
 * [language] only fills the language of the saved result (the table needs one); memory has no language of its own.
 */
class MemoryRound(
    override val index: Int,
    override val id: String,
    private val layout: List<CardSymbol>,
    private val language: Language,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = MemoryGame(layout)
    private val flips = mutableListOf<Int>()
    private var elapsedMs = 0L
    private var shownSince: Long? = null

    override val kind: GameKind get() = GameKind.Memory

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        return { MemoryGuide.step(state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = flips.isEmpty()


    /** Turns over the card at [card]. Returns what happened; [FlipResult.Ignored] changes nothing, not even the clock. */
    fun flip(card: Int): FlipResult {
        val result = game.flip(card)
        if (result != FlipResult.Ignored) {
            flips += card
        }
        return result
    }

    /** Hides the two cards of a mismatch (the UI calls this when its short pause is over). */
    fun closeMismatch() = game.closeMismatch()

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

    override fun snapshot() = MemorySnapshot(id, index, game.state, total())

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = layout.joinToString(",") { it.ordinal.toString() },
        guesses = flips.map { it.toString() },
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Memory,
    )

    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = layout.joinToString(",") { it.ordinal.toString() },
        guesses = flips.map { it.toString() },
        outcome = if (game.state.status == MemoryStatus.Won) Outcome.WON else Outcome.LOST,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = total(),
        game = GameKind.Memory,
    )

    companion object {
        /**
         * Rebuilds a round from storage by replaying its flips. Returns null if the saved board cannot be read (it was
         * written by something else): the caller then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): MemoryRound? {
            val layout = stored.answer.split(',').map { text ->
                CardSymbol.entries.getOrNull(text.toIntOrNull() ?: return null) ?: return null
            }
            val round = try {
                MemoryRound(index, stored.id, layout, stored.language, now)
            } catch (e: IllegalArgumentException) {
                return null
            }
            stored.guesses.forEach { round.game.flip(it.toIntOrNull() ?: return null).also { _ -> round.flips += it.toInt() } }
            round.game.closeMismatch() // a mismatch that was on screen when the app closed is hidden again
            round.elapsedMs = stored.input.toLongOrNull() ?: 0L
            round.assisted = stored.assisted
            return round
        }
    }
}
