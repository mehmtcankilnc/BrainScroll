package com.mehmtcan.brainscroll.game.target

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.TargetGuide
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a target number page for the UI. */
data class TargetSnapshot(
    override val id: String,
    override val index: Int,
    val state: TargetState,
    /** Time spent so far, as the round counts it (see [TargetRound]). */
    val durationMs: Long,
) : PageSnapshot {
    override val isFinished: Boolean get() = state.status == TargetStatus.Won
    override val kind: GameKind get() = GameKind.Target
}

/**
 * One target number puzzle in the feed: a [TargetGame] plus the time and the list of actions, which is all that is
 * needed to save it and to rebuild it exactly as the player left it.
 *
 * The time works as in the other games: the pauses between two actions, each counted for at most [MAX_THINK_MILLIS].
 * The clock starts with the first operation. The game cannot be lost, so a result is always a win.
 *
 * [language] only fills the language of the saved result (the table needs one); this game has no language.
 */
class TargetRound(
    override val index: Int,
    override val id: String,
    private val numbers: List<Int>,
    private val target: Int,
    private val language: Language,
    private val now: () -> Long = { 0L },
    par: Int = TargetGenerator.minSteps(numbers, target) ?: throw IllegalArgumentException("the target cannot be made"),
) : FeedRound {
    private val game = TargetGame(numbers, target, par)

    /** What the player did, in order: "0+1" combines the numbers 0 and 1 with a plus, "u" undoes, "r" restarts. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var lastActionAt: Long? = null

    override val kind: GameKind get() = GameKind.Target

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        return { TargetGuide.step(state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()


    fun preview(firstId: Int, op: Op, secondId: Int): Int? = game.preview(firstId, op, secondId)

    fun combine(firstId: Int, op: Op, secondId: Int): TargetResult =
        game.combine(firstId, op, secondId).also { if (it != TargetResult.Ignored) record("$firstId${op.symbol}$secondId") }

    fun undo(): Boolean = game.undo().also { if (it) record(UNDO) }

    fun restart(): Boolean = game.restart().also { if (it) record(RESTART) }

    private fun record(action: String) {
        actions += action
        val time = now()
        lastActionAt?.let { elapsedMs += minOf(time - it, MAX_THINK_MILLIS).coerceAtLeast(0) }
        lastActionAt = time
    }

    override fun snapshot() = TargetSnapshot(id, index, game.state, elapsedMs)

    /** "2367>48": the starting numbers, then the target. */
    private fun encodedPuzzle() = numbers.joinToString("") + ">" + target

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = encodedPuzzle(),
        guesses = actions.toList(),
        input = elapsedMs.toString(),
        position = index,
        assisted = assisted,
        game = GameKind.Target,
    )

    /**
     * The history row. Its `guesses` is only the number of operations, not the list of actions: the cloud table keeps at
     * most 256 characters there, and a long game with many undos would not fit. A finished game is never replayed.
     */
    override fun toFinished(finishedAt: Long) = FinishedRound(
        id = id,
        mode = Mode.ENDLESS,
        language = language,
        answer = encodedPuzzle(),
        guesses = listOf(game.state.moves.toString()),
        outcome = Outcome.WON,
        assisted = assisted,
        finishedAt = finishedAt,
        durationMs = elapsedMs,
        game = GameKind.Target,
    )

    companion object {
        /** A pause between two actions counts for at most this long. */
        const val MAX_THINK_MILLIS = 15_000L
        private const val UNDO = "u"
        private const val RESTART = "r"

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read (or the
         * target cannot be made from the numbers): the caller then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): TargetRound? {
            val parts = stored.answer.split('>')
            if (parts.size != 2) return null
            val numbers = parts[0].map { it.digitToIntOrNull()?.takeIf { digit -> digit in 1..9 } ?: return null }
            val target = parts[1].toIntOrNull() ?: return null
            val round = try {
                TargetRound(index, stored.id, numbers, target, stored.language, now)
            } catch (e: IllegalArgumentException) {
                return null
            }
            for (action in stored.guesses) {
                when (action) {
                    UNDO -> round.game.undo()
                    RESTART -> round.game.restart()
                    else -> {
                        val at = action.indexOfFirst { !it.isDigit() }
                        if (at <= 0) return null
                        val first = action.substring(0, at).toIntOrNull() ?: return null
                        val op = Op.fromSymbol(action[at]) ?: return null
                        val second = action.substring(at + 1).toIntOrNull() ?: return null
                        round.game.combine(first, op, second)
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
