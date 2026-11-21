package com.mehmtcan.brainscroll.game.lasers

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/** A board of laser mirrors, and the fewest mirrors to turn to light every target. */
class LaserBoard(val puzzle: LaserPuzzle, val par: Int)

/**
 * Makes the boards: the way of the beam is drawn first (it goes along a row, turns at a mirror, goes on, and so on, and the last
 * stretch runs to the edge), the targets are put on that way and a few more mirrors that the beam never meets are added. That
 * is a board that can be won. Then every mirror is turned at random, and the result is kept when it takes at least the level's
 * number of turns to win it (so the board is not nearly done at the start). The board only depends on the seed, but it is saved as
 * the board itself, so a later change of this does not change a saved game.
 */
object LaserGenerator {
    private const val LAYOUTS = 3_000
    private const val SCRAMBLES = 30

    fun generate(seed: Int, difficulty: Difficulty): LaserBoard = generate(seed, GameLevels.lasers(difficulty))

    fun generate(seed: Int, shape: GameLevels.LaserShape): LaserBoard {
        val random = Random(seed)
        repeat(LAYOUTS) { layout(random, shape)?.let { return it } }
        error("no board found")
    }

    private fun layout(random: Random, shape: GameLevels.LaserShape): LaserBoard? {
        val n = shape.size
        fun inside(r: Int, c: Int) = r in 0 until n && c in 0 until n
        val solution = IntArray(n * n)
        val way = ArrayList<Int>()
        val row = random.nextInt(n)
        var r = row
        var c = 0
        var dr = 0
        var dc = 1
        way += r * n + c
        for (m in 0 until shape.pathMirrors) {
            val length = if (m == 0) random.nextInt(n - 1) else 1 + random.nextInt(maxOf(1, n - 2))
            for (step in 0 until length) {
                val nr = r + dr
                val nc = c + dc
                if (!inside(nr, nc) || solution[nr * n + nc] != 0) break
                r = nr
                c = nc
                way += r * n + c
            }
            // A mirror cannot stand on a cell the beam has already crossed.
            if (way.count { it == r * n + c } > 1) return null
            val turns = listOf(dc to dr, -dc to -dr).shuffled(random).filter { (a, b) -> inside(r + a, c + b) && solution[(r + a) * n + c + b] == 0 }
            if (turns.isEmpty()) return null
            val (ndr, ndc) = turns.first()
            solution[r * n + c] = if (LaserRules.reflect(dr, dc, LaserRules.SLASH) == (ndr to ndc)) LaserRules.SLASH else LaserRules.BACKSLASH
            r += ndr
            c += ndc
            dr = ndr
            dc = ndc
            way += r * n + c
        }
        while (true) {
            val nr = r + dr
            val nc = c + dc
            if (!inside(nr, nc) || solution[nr * n + nc] != 0) break
            r = nr
            c = nc
            way += r * n + c
        }
        val onWay = way.toSet()
        val candidates = onWay.filter { solution[it] == 0 && it != row * n }
        val free = (0 until n * n).filter { it !in onWay }
        if (candidates.size < shape.targets || free.size < shape.decoys) return null
        val targets = candidates.shuffled(random).take(shape.targets).toSet()
        // Mirrors the beam never meets: any way they stand wins.
        free.shuffled(random).take(shape.decoys).forEach { solution[it] = if (random.nextBoolean()) LaserRules.SLASH else LaserRules.BACKSLASH }
        if (!LaserRules.lightsAll(n, row, solution.toList(), targets)) return null
        repeat(SCRAMBLES) {
            val start = IntArray(n * n) { if (solution[it] == 0) 0 else if (random.nextBoolean()) LaserRules.SLASH else LaserRules.BACKSLASH }
            val turns = LaserRules.fewestTurns(n, row, start.toList(), targets)?.count { it } ?: return@repeat
            if (turns >= shape.minTurns) return LaserBoard(LaserPuzzle(n, row, start.toList(), targets), turns)
        }
        return null
    }
}
