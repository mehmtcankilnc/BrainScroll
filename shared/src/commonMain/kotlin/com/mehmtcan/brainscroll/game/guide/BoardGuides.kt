package com.mehmtcan.brainscroll.game.guide

import com.mehmtcan.brainscroll.game.hanoi.HanoiSolver
import com.mehmtcan.brainscroll.game.hanoi.HanoiState
import com.mehmtcan.brainscroll.game.hanoi.HanoiStatus
import com.mehmtcan.brainscroll.game.lights.LightsState
import com.mehmtcan.brainscroll.game.lights.LightsStatus
import com.mehmtcan.brainscroll.game.memory.MemoryState
import com.mehmtcan.brainscroll.game.memory.MemoryStatus
import com.mehmtcan.brainscroll.game.mines.MinesSolver
import com.mehmtcan.brainscroll.game.mines.MinesState
import com.mehmtcan.brainscroll.game.mines.MinesStatus
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator
import com.mehmtcan.brainscroll.game.pipes.PipesState
import com.mehmtcan.brainscroll.game.pipes.PipesStatus
import com.mehmtcan.brainscroll.game.sudoku.SudokuState
import com.mehmtcan.brainscroll.game.sudoku.SudokuStatus
import com.mehmtcan.brainscroll.game.wordle.LetterResult
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import kotlin.math.abs

/** The guide of minesweeper: what the numbers on the board prove (see [MinesSolver]). */
object MinesGuide {
    fun step(state: MinesState): GuideStep? {
        if (state.status != MinesStatus.Playing) return null
        if (state.numbers.all { it < 0 }) {
            // The first open is always safe: the middle of the board gives the most room.
            return GuideStep(GuideReason.MinesStart, listOf((state.rows / 2 - 1).coerceAtLeast(0) * state.cols + state.cols / 2 - 1))
        }
        val found = MinesSolver.deduce(state.rows, state.cols, state.numbers.toIntArray())
        val safe = found.safe.filter { state.numbers[it] < 0 && it !in state.flagged }.minOrNull()
        if (safe != null) return GuideStep(GuideReason.MinesSafe, listOf(safe))
        val mine = found.mines.filter { state.numbers[it] < 0 && it !in state.flagged }.minOrNull()
        if (mine != null) return GuideStep(GuideReason.MinesMine, listOf(mine))
        return GuideStep(GuideReason.MinesNone)
    }
}

/** The guide of sudoku: a wrong digit first, then a cell with one candidate, then a digit with one place. */
object SudokuGuide {
    private const val SIZE = 9
    private const val CELLS = 81

    fun step(state: SudokuState, solution: List<Int>): GuideStep? {
        if (state.status != SudokuStatus.Playing) return null
        state.wrong.minOrNull()?.let { return GuideStep(GuideReason.SudokuErase, listOf(it)) }

        val values = state.values
        val candidates = IntArray(CELLS) { cell ->
            if (values[cell] != 0) 0 else (1..SIZE).fold(0) { mask, digit -> if (allows(values, cell, digit)) mask or (1 shl (digit - 1)) else mask }
        }
        // A cell that can only hold one digit.
        for (cell in 0 until CELLS) {
            if (values[cell] == 0 && candidates[cell].countOneBits() == 1) {
                return GuideStep(GuideReason.SudokuOnlyDigit, listOf(cell), listOf(candidates[cell].countTrailingZeroBits() + 1))
            }
        }
        // A digit that has one place left in a row, a column or a box.
        for (unit in units) for (digit in 1..SIZE) {
            if (unit.any { values[it] == digit }) continue
            val places = unit.filter { values[it] == 0 && candidates[it] and (1 shl (digit - 1)) != 0 }
            if (places.size == 1) return GuideStep(GuideReason.SudokuOnlyPlace, places, listOf(digit))
        }
        // Not needed for the boards of the game (singles are enough), but never leave the player without a step.
        val cell = (0 until CELLS).firstOrNull { values[it] == 0 } ?: return null
        return GuideStep(GuideReason.SudokuSolution, listOf(cell), listOf(solution[cell]))
    }

    private fun allows(values: List<Int>, cell: Int, digit: Int): Boolean = peers[cell].none { values[it] == digit }

    private val units: List<List<Int>> = buildList {
        for (i in 0 until SIZE) {
            add((0 until SIZE).map { i * SIZE + it })
            add((0 until SIZE).map { it * SIZE + i })
            add((0 until SIZE).map { k -> (i / 3 * 3 + k / 3) * SIZE + (i % 3 * 3 + k % 3) })
        }
    }

    private val peers: List<Set<Int>> = List(CELLS) { cell ->
        units.filter { cell in it }.flatten().filter { it != cell }.toSet()
    }
}

/** The guide of the pipes: the first tile that is not turned the way the cheapest solution needs. */
object PipesGuide {
    fun step(state: PipesState): GuideStep? {
        if (state.status != PipesStatus.Playing) return null
        val turns = PipesGenerator.solve(state.tiles.toIntArray(), state.size) ?: return null
        val cell = turns.indexOfFirst { it > 0 }
        if (cell < 0) return null
        return GuideStep(GuideReason.PipesTurn, listOf(cell), listOf(turns[cell]))
    }
}

/**
 * The guide of Lights Out. Tapping is arithmetic modulo 2 (two taps cancel), so the cells that put every light out from
 * the board as it is now come from solving a system of equations over {0, 1}; of the solutions the one with the fewest
 * taps is taken.
 */
object LightsGuide {
    fun step(state: LightsState): GuideStep? {
        if (state.status != LightsStatus.Playing) return null
        val taps = solve(state.size, state.lit) ?: return null
        val cell = taps.firstOrNull() ?: return null
        return GuideStep(GuideReason.LightsTap, listOf(cell))
    }

    /** The cells to tap (the fewest), or null if the board cannot be put out (it always can, with these rules). */
    internal fun solve(size: Int, lit: List<Boolean>): List<Int>? {
        val n = size * size
        // Row i of the system: the cells whose tap switches cell i, and on the right whether it is lit.
        val rows = Array(n) { i ->
            BooleanArray(n + 1) { j ->
                if (j == n) lit[i] else {
                    val ri = i / size
                    val ci = i % size
                    val rj = j / size
                    val cj = j % size
                    (ri == rj && abs(ci - cj) <= 1) || (ci == cj && abs(ri - rj) == 1)
                }
            }
        }
        val pivotOf = IntArray(n) { -1 } // the row that decides each variable
        var row = 0
        for (col in 0 until n) {
            val pivot = (row until n).firstOrNull { rows[it][col] } ?: continue
            val swap = rows[row]
            rows[row] = rows[pivot]
            rows[pivot] = swap
            for (other in 0 until n) {
                if (other != row && rows[other][col]) for (k in 0..n) rows[other][k] = rows[other][k] xor rows[row][k]
            }
            pivotOf[col] = row
            row++
        }
        if ((row until n).any { rows[it][n] }) return null // no way to put them all out
        val free = (0 until n).filter { pivotOf[it] < 0 }
        var best: List<Int>? = null
        // Every choice of the free cells gives a solution; the board has only a few free cells (2 for 5x5).
        for (mask in 0 until (1 shl free.size)) {
            val x = BooleanArray(n)
            free.forEachIndexed { index, cell -> x[cell] = mask and (1 shl index) != 0 }
            for (col in 0 until n) {
                val r = pivotOf[col]
                if (r < 0) continue
                var value = rows[r][n]
                for (f in free) if (rows[r][f] && x[f]) value = !value
                x[col] = value
            }
            val taps = (0 until n).filter { x[it] }
            if (best == null || taps.size < best.size) best = taps
        }
        return best
    }
}

/** The guide of memory match: the game knows where every pair is, so it points at one. */
object MemoryGuide {
    fun step(state: MemoryState): GuideStep? {
        if (state.status != MemoryStatus.Playing) return null
        val open = state.faceUp.filter { it !in state.matched }
        // One card face up: show where its partner is.
        if (open.size == 1 && !state.mismatchShown) {
            val card = open.single()
            val partner = state.layout.indices.first { it != card && state.layout[it] == state.layout[card] }
            return GuideStep(GuideReason.MemoryPartner, listOf(partner))
        }
        val unmatched = state.layout.indices.filter { it !in state.matched }
        val first = unmatched.firstOrNull() ?: return null
        val second = unmatched.first { it != first && state.layout[it] == state.layout[first] }
        return GuideStep(GuideReason.MemoryPair, listOf(first, second))
    }
}

/** The guide of the word puzzle: no move can be proved, so it gives one letter the player does not have yet. */
object WordGuide {
    fun step(answer: String, rows: List<List<LetterResult>>, status: WordleStatus): GuideStep? {
        if (status != WordleStatus.Playing) return null
        val place = answer.indices.firstOrNull { at -> rows.none { it.getOrNull(at) == LetterResult.Correct } } ?: return null
        return GuideStep(GuideReason.WordLetter, numbers = listOf(place + 1), text = answer[place].toString())
    }
}

/** The guide of the tower of Hanoi: the first move of the shortest way from the position as it is now. */
object HanoiGuide {
    fun step(state: HanoiState): GuideStep? {
        if (state.status != HanoiStatus.Playing) return null
        val move = HanoiSolver.shortest(state.pegs.toIntArray(), state.goal).firstOrNull() ?: return null
        return GuideStep(GuideReason.HanoiMove, listOf(move.from, move.to), listOf(move.from, move.to))
    }
}
