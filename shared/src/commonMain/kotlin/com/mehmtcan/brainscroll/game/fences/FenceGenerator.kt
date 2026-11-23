package com.mehmtcan.brainscroll.game.fences

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/**
 * Makes the boards: a loop is made at random by growing a patch of squares (with no hole and no place where two squares only touch at a
 * corner) and taking its edge, every square gets the number of its sides on the loop, and then the numbers are taken away one by one
 * in a random order. A number stays away only when the board can still be finished the way a player does it (by the rules, and on the
 * harder levels also by trying a mark and seeing the rules break), which also means it has one loop. It stops when the level's number
 * of numbers is reached (or when no more can go). The board only depends on the seed, but it is saved as the board itself, so a later
 * change of this does not change a saved game.
 */
object FenceGenerator {
    fun generate(seed: Int, difficulty: Difficulty): FencePuzzle = generate(seed, GameLevels.fences(difficulty))

    fun generate(seed: Int, shape: GameLevels.FenceShape): FencePuzzle {
        // A loop whose full board the rules cannot finish, or a board too rich to be saved, is rare; the next seed gives another one.
        for (attempt in 0 until 20) {
            val puzzle = make(Random(seed + attempt * 7919), shape) ?: continue
            if (puzzle.canBeSaved) return puzzle
        }
        error("no board that can be solved by logic and saved")
    }

    private fun make(random: Random, shape: GameLevels.FenceShape): FencePuzzle? {
        val n = shape.size
        val loop = randomLoop(random, n)
        val full = FencePuzzle(n, List(n * n) { cell -> FenceRules.sideLines(FencePuzzle(n, List(n * n) { -1 }), loop, cell) })
        if (FenceSolver.solveByLogic(full, shape.lookahead) == null) return null
        val clues = full.clues.toMutableList()
        var left = n * n
        for (cell in (0 until n * n).shuffled(random)) {
            if (left <= shape.clues) break
            val old = clues[cell]
            clues[cell] = -1
            if (FenceSolver.solveByLogic(FencePuzzle(n, clues.toList()), shape.lookahead) != null) left-- else clues[cell] = old
        }
        return FencePuzzle(n, clues.toList())
    }

    /** The edges of a random loop on a board of [n] by [n] squares: the border of a patch that covers about two thirds of the squares. */
    fun randomLoop(random: Random, n: Int): BooleanArray {
        val blank = FencePuzzle(n, List(n * n) { -1 })
        val g = blank.geometry
        val inside = BooleanArray(n * n)
        inside[random.nextInt(n * n)] = true
        var size = 1
        val target = (n * n * 0.62).toInt().coerceAtLeast(4)
        while (size < target) {
            val candidates = (0 until n * n).filter { c ->
                !inside[c] && neighbours(n, c).any { inside[it] }
            }.shuffled(random)
            val pick = candidates.firstOrNull { c ->
                inside[c] = true
                val ok = FenceRules.isLoop(blank, border(n, g, inside))
                inside[c] = false
                ok
            } ?: break
            inside[pick] = true
            size++
        }
        return border(n, g, inside)
    }

    /** The edges between a square of the patch and one that is not (or the edge of the board). */
    private fun border(n: Int, g: FenceGeometry, inside: BooleanArray): BooleanArray = BooleanArray(g.edges) { e ->
        val cells = g.edgeCells[e]
        if (cells.size == 1) inside[cells[0]] else inside[cells[0]] != inside[cells[1]]
    }

    private fun neighbours(n: Int, cell: Int): List<Int> = buildList {
        val row = cell / n
        val column = cell % n
        if (row > 0) add(cell - n)
        if (row < n - 1) add(cell + n)
        if (column > 0) add(cell - 1)
        if (column < n - 1) add(cell + 1)
    }
}
