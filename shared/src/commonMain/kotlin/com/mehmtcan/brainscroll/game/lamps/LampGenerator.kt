package com.mehmtcan.brainscroll.game.lamps

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/**
 * Makes the boards: walls are put at random, lamps are put on random white cells that nothing lights yet (so no two lamps see each
 * other) until every white cell is lit, and every wall gets the number of lamps next to it. The board is kept only when it can be lit
 * the way a player does it (by the rules, and on the harder levels also by trying a lamp and seeing the rules break), which also means it
 * has one way to be lit. Then the numbers are taken off the walls one by one in a random order, and one stays off only when the board can
 * still be lit that way, until the level's number of numbers is reached (or no more can go). The board only depends on the seed, but it is
 * saved as the board itself, so a later change of this does not change a saved game.
 */
object LampGenerator {
    private const val TRIES = 5_000

    fun generate(seed: Int, difficulty: Difficulty): LampPuzzle = generate(seed, GameLevels.lamps(difficulty))

    fun generate(seed: Int, shape: GameLevels.LampShape): LampPuzzle {
        val random = Random(seed)
        val n = shape.size
        repeat(TRIES) {
            val cells = IntArray(n * n) { LampPuzzle.WHITE }
            for (wall in (0 until n * n).shuffled(random).take(shape.walls)) cells[wall] = LampPuzzle.BLANK_WALL
            val bare = LampPuzzle(n, cells.toList())
            if (bare.whiteCells.isEmpty()) return@repeat

            val lamps = BooleanArray(n * n)
            val lit = BooleanArray(n * n)
            for (cell in bare.whiteCells.shuffled(random)) {
                if (lit[cell]) continue
                lamps[cell] = true
                lit[cell] = true
                for (seen in bare.seenFrom(cell)) lit[seen] = true
            }

            // Every wall shows how many lamps are next to it.
            for (wall in cells.indices) if (cells[wall] != LampPuzzle.WHITE) cells[wall] = bare.neighbours(wall).count { lamps[it] }
            var puzzle = LampPuzzle(n, cells.toList())
            if (LampSolver.solveByLogic(puzzle, shape.lookahead) == null) return@repeat

            // Take numbers off while the board can still be lit that way.
            var numbered = cells.indices.filter { cells[it] in 0..LampPuzzle.MAX_NUMBER }
            for (wall in numbered.shuffled(random)) {
                if (numbered.size <= shape.numbers) break
                val number = cells[wall]
                cells[wall] = LampPuzzle.BLANK_WALL
                val fewer = LampPuzzle(n, cells.toList())
                if (LampSolver.solveByLogic(fewer, shape.lookahead) != null) {
                    puzzle = fewer
                    numbered = numbered - wall
                } else {
                    cells[wall] = number
                }
            }
            return puzzle
        }
        error("no board found")
    }
}
