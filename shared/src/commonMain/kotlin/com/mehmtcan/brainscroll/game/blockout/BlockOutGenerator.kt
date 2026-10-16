package com.mehmtcan.brainscroll.game.blockout

import kotlin.random.Random

/** A puzzle and the fewest moves that solve it. */
class BlockOutBoard(val puzzle: BlockOutPuzzle, val par: Int)

/**
 * Makes the boards of block out (docs/yeni-oyunlar-plan.md): the target piece lies in the middle row on the left, a number of
 * other pieces of two or three cells are put on the board at random, and the board is kept only if it can be solved and the
 * shortest way ([BlockOutSolver]) takes at least [DEFAULT_MIN_PAR] moves.
 *
 * Boards that hard are rare among random ones (a few in a thousand), so the search does not draw boards blindly: it takes a random
 * board and changes it a little at a time (one piece put somewhere else, a piece added or taken away), keeping a change that does
 * not make the board easier or unsolvable, until the board is hard enough; if it gets stuck it starts over. The board only depends
 * on the seed, but it is saved as the cells themselves, so a later change of this algorithm does not change a saved game.
 */
object BlockOutGenerator {
    const val DEFAULT_MIN_PAR = 8

    /** The most boards looked at before settling for the hardest one found (it does not come to that for 5 by 5). */
    private const val MAX_STEPS = 60_000

    /** How many changes are tried on one board before starting over with a new one. */
    private const val CLIMB_STEPS = 120

    private const val MIN_OTHER_PIECES = 6
    private const val MAX_OTHER_PIECES = 10

    /** The target and the other pieces, in no order, each with the place it starts at. */
    private class Draft(val shapes: List<PieceShape>, val places: List<Int>)

    fun generate(seed: Int, size: Int = BlockOutPuzzle.DEFAULT_SIZE, minPar: Int = DEFAULT_MIN_PAR): BlockOutBoard {
        val random = Random(seed)
        var best: BlockOutBoard? = null
        var steps = 0
        while (steps < MAX_STEPS) {
            var draft = randomDraft(random, size)
            var score = -1
            repeat(CLIMB_STEPS) {
                steps++
                val par = parOf(draft, size)
                if (par >= minPar) return BlockOutBoard(puzzleOf(draft, size), par)
                if (par > (best?.par ?: -1)) best = BlockOutBoard(puzzleOf(draft, size), par)
                if (par >= score) score = par else return@repeat
                // The board is no worse than the one before: it is the one to change from.
                val next = mutate(draft, random, size)
                val nextPar = parOf(next, size)
                if (nextPar >= score) {
                    draft = next
                    score = nextPar
                }
            }
        }
        return best ?: error("no solvable board found")
    }

    private fun puzzleOf(draft: Draft, size: Int) = BlockOutPuzzle.of(size, draft.shapes, draft.places.toIntArray())

    /** The fewest moves of a draft, or -1 if it cannot be solved. */
    private fun parOf(draft: Draft, size: Int): Int {
        val puzzle = puzzleOf(draft, size)
        return BlockOutSolver.shortest(size, puzzle.shapes, puzzle.start)?.size ?: -1
    }

    /** The target on the left of the middle row (so it has a way to go) and seven to nine other pieces where they fit. */
    private fun randomDraft(random: Random, size: Int): Draft {
        val target = PieceShape(horizontal = true, line = size / 2, length = 2, isTarget = true)
        var draft = Draft(listOf(target), listOf(random.nextInt(size - 2)))
        val wanted = random.nextInt(MIN_OTHER_PIECES + 1, MAX_OTHER_PIECES)
        var tries = 0
        while (draft.shapes.size < wanted + 1 && tries++ < 300) draft = withNewPiece(draft, random, size) ?: draft
        return draft
    }

    /** One small change: a piece put somewhere else, or one added, or one taken away, or the target started somewhere else. */
    private fun mutate(draft: Draft, random: Random, size: Int): Draft {
        val others = draft.shapes.size - 1
        return when (random.nextInt(10)) {
            0 -> if (others < MAX_OTHER_PIECES) withNewPiece(draft, random, size) ?: draft else draft
            1 -> if (others > MIN_OTHER_PIECES) without(draft, 1 + random.nextInt(others)) else draft
            2 -> Draft(draft.shapes, draft.places.toMutableList().also { it[0] = random.nextInt(size - 2) }).takeIf { fits(it, size) } ?: draft
            else -> withNewPiece(without(draft, 1 + random.nextInt(others)), random, size) ?: draft
        }
    }

    private fun without(draft: Draft, index: Int) =
        Draft(draft.shapes.filterIndexed { i, _ -> i != index }, draft.places.filterIndexed { i, _ -> i != index })

    /** [draft] with one more piece of random shape and place that does not overlap, or null if none was found in a few tries. */
    private fun withNewPiece(draft: Draft, random: Random, size: Int): Draft? {
        val taken = BooleanArray(size * size)
        for (i in draft.shapes.indices) for (cell in BlockOutPuzzle.cellsOf(size, draft.shapes[i], draft.places[i])) taken[cell] = true
        repeat(40) {
            val length = if (random.nextInt(4) == 0) 3 else 2
            val shape = PieceShape(horizontal = random.nextBoolean(), line = random.nextInt(size), length = length)
            val place = random.nextInt(size - length + 1)
            val cells = BlockOutPuzzle.cellsOf(size, shape, place)
            if (cells.none { taken[it] }) return Draft(draft.shapes + shape, draft.places + place)
        }
        return null
    }

    private fun fits(draft: Draft, size: Int): Boolean {
        val taken = BooleanArray(size * size)
        for (i in draft.shapes.indices) for (cell in BlockOutPuzzle.cellsOf(size, draft.shapes[i], draft.places[i])) {
            if (taken[cell]) return false
            taken[cell] = true
        }
        return true
    }
}
