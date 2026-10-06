package com.mehmtcan.brainscroll.game.memory

import com.mehmtcan.brainscroll.game.FeedRound
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
    override val skipped: Boolean,
) : PageSnapshot {
    override val isFinished: Boolean get() = state.status != MemoryStatus.Playing
    override val kind: GameKind get() = GameKind.Memory
}

/**
 * One memory board in the feed: a [MemoryGame] plus the time and the order of the flips, which is all that is
 * needed to save it and to rebuild it exactly as the player left it.
 *
 * The time is the sum of the pauses between two flips, each counted for at most [MAX_THINK_MILLIS]. So thinking
 * counts, but leaving the app open in the background (or coming back the next day) does not blow it up. The clock
 * starts with the first flip.
 *
 * [language] only fills the language of the saved result (the table needs one); memory has no language of its own.
 */
class MemoryRound(
    override val index: Int,
    override val id: String,
    private val layout: List<CardSymbol>,
    private val language: Language,
    private val now: () -> Long = { 0L },
    maxMistakes: Int = MemoryGame.DEFAULT_MAX_MISTAKES,
) : FeedRound {
    private val game = MemoryGame(layout, maxMistakes)
    private val flips = mutableListOf<Int>()
    private var elapsedMs = 0L
    private var lastFlipAt: Long? = null

    override val kind: GameKind get() = GameKind.Memory

    override var skipped = false
        private set

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = flips.isEmpty()

    override fun markSkipped() {
        skipped = true
    }

    /** Turns over the card at [card]. Returns what happened; [FlipResult.Ignored] changes nothing, not even the clock. */
    fun flip(card: Int): FlipResult {
        val result = game.flip(card)
        if (result != FlipResult.Ignored) {
            flips += card
            val time = now()
            lastFlipAt?.let { elapsedMs += minOf(time - it, MAX_THINK_MILLIS).coerceAtLeast(0) }
            lastFlipAt = time
        }
        return result
    }

    /** Hides the two cards of a mismatch (the UI calls this when its short pause is over). */
    fun closeMismatch() = game.closeMismatch()

    override fun snapshot() = MemorySnapshot(id, index, game.state, elapsedMs, skipped)

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = layout.joinToString(",") { it.ordinal.toString() },
        guesses = flips.map { it.toString() },
        input = elapsedMs.toString(),
        skipped = skipped,
        position = index,
        game = GameKind.Memory,
    )

    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = layout.joinToString(",") { it.ordinal.toString() },
        guesses = flips.map { it.toString() },
        outcome = if (game.state.status == MemoryStatus.Won) Outcome.WON else Outcome.LOST,
        wasSkipped = skipped,
        finishedAt = finishedAt,
        durationMs = elapsedMs,
        game = GameKind.Memory,
    )

    companion object {
        /** A pause between two flips counts for at most this long. */
        const val MAX_THINK_MILLIS = 10_000L

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
            round.skipped = stored.skipped
            return round
        }
    }
}
