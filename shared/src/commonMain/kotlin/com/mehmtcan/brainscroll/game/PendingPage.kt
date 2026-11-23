package com.mehmtcan.brainscroll.game

import com.mehmtcan.brainscroll.game.guide.GuideTask
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/**
 * Everything the feed decides about a new page before its board is made: which game, which level, the seed, the id, and for a word
 * puzzle its language and answer. Making the board from a plan touches nothing else of the feed (see `EndlessFeed.build`), so it can run on
 * any thread, while the choices that use the feed's shared random numbers are all made on the main thread when the plan is made.
 */
data class PagePlan(
    val index: Int,
    val id: String,
    val kind: GameKind,
    val seed: Int,
    val level: Difficulty,
    /** The language of everything that is not a word puzzle (the feed's first word language when the plan was made). */
    val language: Language,
    /** The language of a word puzzle or a topic puzzle; null for the other games. */
    val wordLanguage: Language?,
    /** The answer of a word puzzle; null for the other games. */
    val answer: String?,
)

/** A page whose board is still being made in the background. Nothing can be done on it, and it is not worth saving. */
class PendingRound(val plan: PagePlan) : FeedRound {
    override val kind: GameKind get() = plan.kind
    override val index: Int get() = plan.index
    override val id: String get() = plan.id
    override val isFinished: Boolean get() = false
    override val isUntouched: Boolean get() = true
    override val assisted: Boolean get() = false

    override fun markAssisted() = Unit

    override fun snapshot(): PageSnapshot = PendingSnapshot(id, index, kind)

    override fun guideTask(): GuideTask? = null

    override fun toStored(): StoredRound = error("a page that is still being made is not saved")

    override fun toFinished(finishedAt: Long): FinishedRound = error("a page that is still being made has no result")
}

/** What the screen draws for a page that is still being made: the game it will be, as a skeleton. */
data class PendingSnapshot(
    override val id: String,
    override val index: Int,
    override val kind: GameKind,
) : PageSnapshot {
    override val isFinished: Boolean get() = false
}
