package com.mehmtcan.brainscroll.game

/**
 * Every game in the feed (word puzzle now, Sudoku, memory match... later) implements this.
 * The feed only needs to know "what is the current state" and "did the player finish".
 *
 * [S] is the game's immutable state snapshot, [A] is what the player can do (an action).
 * The UI observes [state] and calls [perform]; the game never touches UI or platform code.
 */
interface Game<S, A> {
    val state: S

    /** True once the game cannot accept more actions (won, lost, or otherwise done). */
    val isFinished: Boolean

    /** Applies a player action and returns the new state. */
    fun perform(action: A): S
}
