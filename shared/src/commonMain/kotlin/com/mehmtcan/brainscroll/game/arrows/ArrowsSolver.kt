package com.mehmtcan.brainscroll.game.arrows

/** Solving arrow escape is a loop: an arrow that leaves never blocks another, so any free arrow may go first. */
object ArrowsSolver {
    /** The arrows (by index) of a way to clear the board, in the order they leave. */
    fun order(puzzle: ArrowsPuzzle): List<Int> = ArrowsRules.clearOrder(puzzle, BooleanArray(puzzle.arrowCount) { true }).first
}
