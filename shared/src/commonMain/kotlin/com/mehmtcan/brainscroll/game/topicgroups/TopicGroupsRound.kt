package com.mehmtcan.brainscroll.game.topicgroups

import com.mehmtcan.brainscroll.game.FeedRound
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.guide.TopicGroupsGuide
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** Immutable picture of a word groups page for the UI. */
data class TopicGroupsSnapshot(
    override val id: String,
    override val index: Int,
    val state: TopicGroupsState,
    /** Time spent so far, as the round counts it (see [TopicGroupsRound]). */
    val durationMs: Long,
    /** The language of the words. */
    val language: Language,
) : PageSnapshot {
    /** The same board with no time on it, to tell whether a page is still as it was (the time of a page that is on show never stops). */
    override fun withoutClock(): PageSnapshot = copy(durationMs = 0L)

    override val isFinished: Boolean get() = state.status == TopicGroupsStatus.Won
    override val kind: GameKind get() = GameKind.TopicGroups
}

/**
 * One word groups board in the feed: a [TopicGroupsGame] plus the time and the list of actions, which is all that is needed to
 * save it and to rebuild it exactly as the player left it.
 *
 * The time is the time the page is on show (see [onShown]): it runs from the moment the page comes to the screen until the board
 * is won, and stops while the page is left. The game cannot be lost, so a result is always a win; its second number is the
 * mistakes.
 *
 * The words are in [language], which the board was made for. It is the one game besides the word puzzle that has one.
 */
class TopicGroupsRound(
    override val index: Int,
    override val id: String,
    private val puzzle: TopicGroupsPuzzle,
    private val now: () -> Long = { 0L },
) : FeedRound {
    private val game = TopicGroupsGame(puzzle)

    /** What the player did, in order: "t5" taps word 5, "s123" mixes the words with seed 123, "c" puts the picked words back. */
    private val actions = mutableListOf<String>()
    private var elapsedMs = 0L
    private var shownSince: Long? = null

    val language: Language get() = puzzle.language

    override val kind: GameKind get() = GameKind.TopicGroups

    override var assisted = false
        private set

    override fun markAssisted() {
        assisted = true
    }

    override fun guideTask(): GuideTask? {
        if (isFinished) return null
        val state = game.state
        return { TopicGroupsGuide.step(state) }
    }

    override val isFinished: Boolean get() = game.isFinished

    override val isUntouched: Boolean get() = actions.isEmpty()

    /** Taps [word]. */
    fun tap(word: Int): TopicGroupsResult = game.toggle(word).also { if (it != TopicGroupsResult.Ignored) record("t$word") }

    /** Mixes up the words that are still on the board. */
    fun shuffle(): Boolean {
        val seed = puzzle.seed * 31 + actions.size * 7919 + 1
        return game.shuffle(seed).also { if (it) record("s$seed") }
    }

    /** Puts the picked words back. */
    fun clearSelection(): Boolean = game.clearSelection().also { if (it) record(CLEAR) }

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

    override fun snapshot() = TopicGroupsSnapshot(id, index, game.state, total(), language)

    override fun toStored() = StoredRound(
        id = id,
        language = language,
        answer = puzzle.encode(),
        guesses = actions.toList(),
        input = total().toString(),
        position = index,
        assisted = assisted,
        game = GameKind.TopicGroups,
    )

    /**
     * The history row. Its `guesses` is only the number of mistakes, not the list of actions: the cloud table keeps at most
     * 256 characters there, and a long game would not fit. A finished game is never replayed.
     */
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
        game = GameKind.TopicGroups,
    )

    companion object {
        private const val CLEAR = "c"

        /**
         * Rebuilds a round from storage by replaying its actions. Returns null if what was saved cannot be read: the caller
         * then simply starts a new one.
         */
        fun restore(index: Int, stored: StoredRound, now: () -> Long): TopicGroupsRound? {
            val puzzle = TopicGroupsPuzzle.decode(stored.answer) ?: return null
            val round = TopicGroupsRound(index, stored.id, puzzle, now)
            for (action in stored.guesses) {
                when {
                    action == CLEAR -> round.game.clearSelection()
                    action.length > 1 && action[0] == 't' -> round.game.toggle(action.substring(1).toIntOrNull() ?: return null)
                    action.length > 1 && action[0] == 's' -> round.game.shuffle(action.substring(1).toIntOrNull() ?: return null)
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
