package com.mehmtcan.brainscroll.game.guide

import com.mehmtcan.brainscroll.game.arrows.ArrowsState
import com.mehmtcan.brainscroll.game.fruitmath.FruitPuzzle
import com.mehmtcan.brainscroll.game.codebreaker.CodePuzzle
import com.mehmtcan.brainscroll.game.rectangles.RectPuzzle
import com.mehmtcan.brainscroll.game.flashgrid.FlashState
import com.mehmtcan.brainscroll.game.flashgrid.FlashStatus
import com.mehmtcan.brainscroll.game.rectangles.RectSolver
import com.mehmtcan.brainscroll.game.rectangles.RectState
import com.mehmtcan.brainscroll.game.rectangles.RectStatus
import com.mehmtcan.brainscroll.game.codebreaker.CodeSolver
import com.mehmtcan.brainscroll.game.codebreaker.CodeState
import com.mehmtcan.brainscroll.game.codebreaker.CodeStatus
import com.mehmtcan.brainscroll.game.fruitmath.FruitState
import com.mehmtcan.brainscroll.game.fruitmath.FruitStatus
import com.mehmtcan.brainscroll.game.lasers.LaserPuzzle
import com.mehmtcan.brainscroll.game.untangle.UNTANGLE_BOARD
import com.mehmtcan.brainscroll.game.untangle.UNTANGLE_MARGIN
import com.mehmtcan.brainscroll.game.untangle.UntangleEmbedder
import com.mehmtcan.brainscroll.game.untangle.UntanglePuzzle
import com.mehmtcan.brainscroll.game.untangle.UntangleState
import com.mehmtcan.brainscroll.game.untangle.UntangleStatus
import com.mehmtcan.brainscroll.game.lasers.LaserRules
import com.mehmtcan.brainscroll.game.lasers.LaserState
import com.mehmtcan.brainscroll.game.lasers.LaserStatus
import com.mehmtcan.brainscroll.game.matchsticks.MatchPuzzle
import com.mehmtcan.brainscroll.game.matchsticks.MatchRules
import com.mehmtcan.brainscroll.game.matchsticks.MatchState
import com.mehmtcan.brainscroll.game.matchsticks.MatchStatus
import com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsState
import com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsStatus
import com.mehmtcan.brainscroll.game.axissum.AxisSumState
import com.mehmtcan.brainscroll.game.ballslide.BallSlidePuzzle
import com.mehmtcan.brainscroll.game.colorflow.FlowPuzzle
import com.mehmtcan.brainscroll.game.colorflow.FlowSolver
import com.mehmtcan.brainscroll.game.colorflow.FlowState
import com.mehmtcan.brainscroll.game.colorflow.FlowStatus
import com.mehmtcan.brainscroll.game.ballslide.BallSlideSolver
import com.mehmtcan.brainscroll.game.ballslide.BallSlideState
import com.mehmtcan.brainscroll.game.ballslide.BallSlideStatus
import com.mehmtcan.brainscroll.game.axissum.AxisSumStatus
import com.mehmtcan.brainscroll.game.nonogram.NonogramMark
import com.mehmtcan.brainscroll.game.nonogram.NonogramSolver
import com.mehmtcan.brainscroll.game.nonogram.NonogramState
import com.mehmtcan.brainscroll.game.nonogram.NonogramStatus
import com.mehmtcan.brainscroll.game.arrows.ArrowsStatus
import com.mehmtcan.brainscroll.game.hanoi.HanoiSolver
import com.mehmtcan.brainscroll.game.hanoi.HanoiState
import com.mehmtcan.brainscroll.game.hanoi.HanoiStatus
import com.mehmtcan.brainscroll.game.blockout.BlockOutPuzzle
import com.mehmtcan.brainscroll.game.blockout.BlockOutSolver
import com.mehmtcan.brainscroll.game.blockout.BlockOutState
import com.mehmtcan.brainscroll.game.blockout.BlockOutStatus
import com.mehmtcan.brainscroll.game.queens.QueensSolver
import com.mehmtcan.brainscroll.game.tents.TentsMark
import com.mehmtcan.brainscroll.game.tents.TentsSolver
import com.mehmtcan.brainscroll.game.tents.TentsState
import com.mehmtcan.brainscroll.game.tents.TentsStatus
import com.mehmtcan.brainscroll.game.queens.QueensState
import com.mehmtcan.brainscroll.game.queens.QueensStatus
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
import com.mehmtcan.brainscroll.game.sudoku.SudokuLayout
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

/**
 * The guide of sudoku (the classic board and the jigsaw one): a wrong digit first, then a cell with one candidate, then a digit
 * with one place. On the jigsaw board the third unit is a region instead of a box, and the sentences say so.
 */
object SudokuGuide {
    private const val SIZE = 9
    private const val CELLS = 81

    fun step(state: SudokuState, solution: List<Int>): GuideStep? {
        if (state.status != SudokuStatus.Playing) return null
        state.wrong.minOrNull()?.let { return GuideStep(GuideReason.SudokuErase, listOf(it)) }

        val layout = SudokuLayout.of(state.regions)
        val jigsaw = !layout.isStandard
        val values = state.values
        val candidates = IntArray(CELLS) { cell ->
            if (values[cell] != 0) 0 else (1..SIZE).fold(0) { mask, digit -> if (layout.peers[cell].none { values[it] == digit }) mask or (1 shl (digit - 1)) else mask }
        }
        // A cell that can only hold one digit.
        for (cell in 0 until CELLS) {
            if (values[cell] == 0 && candidates[cell].countOneBits() == 1) {
                return GuideStep(if (jigsaw) GuideReason.JigsawOnlyDigit else GuideReason.SudokuOnlyDigit, listOf(cell), listOf(candidates[cell].countTrailingZeroBits() + 1))
            }
        }
        // A digit that has one place left in a row, a column or a box (a region).
        for (unit in layout.units) for (digit in 1..SIZE) {
            if (unit.any { values[it] == digit }) continue
            val places = unit.filter { values[it] == 0 && candidates[it] and (1 shl (digit - 1)) != 0 }
            if (places.size == 1) return GuideStep(if (jigsaw) GuideReason.JigsawOnlyPlace else GuideReason.SudokuOnlyPlace, places, listOf(digit))
        }
        // Not needed for the boards of the game (singles are enough), but never leave the player without a step.
        val cell = (0 until CELLS).firstOrNull { values[it] == 0 } ?: return null
        return GuideStep(GuideReason.SudokuSolution, listOf(cell), listOf(solution[cell]))
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

/**
 * The guide of the queens puzzle: of the full placements that hold the locked queens, the one that has most of the queens
 * the player already put down. It says to put one of the missing queens of that placement, or, if every queen of it is there
 * already, to take away one that is not part of it.
 */
object QueensGuide {
    fun step(state: QueensState): GuideStep? {
        if (state.status != QueensStatus.Playing) return null
        val locked = state.locked.indices.filter { state.locked[it] }.toSet()
        val placed = state.queens.indices.filter { state.queens[it] }.toSet()
        val best = QueensSolver.solutions(state.size, locked).maxByOrNull { solution -> solution.count { it in placed } } ?: return null
        best.firstOrNull { it !in placed }?.let { return GuideStep(GuideReason.QueensPlace, listOf(it), listOf(it / state.size + 1, it % state.size + 1)) }
        val extra = placed.filter { it !in best }.minOrNull() ?: return null
        return GuideStep(GuideReason.QueensRemove, listOf(extra), listOf(extra / state.size + 1, extra % state.size + 1))
    }
}

/**
 * The guide of the tents puzzle (the board has one solution). A tent the player put that is not part of it is to be taken away
 * first. Otherwise it points at a missing tent, one that plain reasoning gives from the tents already there if there is one.
 */
object TentsGuide {
    fun step(state: TentsState): GuideStep? {
        if (state.status != TentsStatus.Playing) return null
        val trees = state.trees.indices.filter { state.trees[it] }.toSet()
        val solution = TentsSolver.solutions(state.size, trees, state.rowTargets, state.columnTargets, limit = 1).firstOrNull() ?: return null
        val placed = state.marks.indices.filter { state.marks[it] == TentsMark.TENT }.toSet()
        placed.filter { it !in solution }.minOrNull()?.let { return step(GuideReason.TentsRemove, it, state.size) }
        val missing = solution - placed
        val derived = TentsSolver.deduce(state.size, trees, state.rowTargets, state.columnTargets, placed)
        val cell = derived.firstOrNull { it in missing } ?: missing.minOrNull() ?: return null
        return step(GuideReason.TentsPlace, cell, state.size)
    }

    private fun step(reason: GuideReason, cell: Int, size: Int) = GuideStep(reason, listOf(cell), listOf(cell / size + 1, cell % size + 1))
}

/**
 * The guide of block out: the first move of the shortest way from the board as it is now. It points at the piece (its number in
 * the board) and says in which direction it slides (0 up, 1 down, 2 left, 3 right) and how many cells.
 */
object BlockOutGuide {
    fun step(state: BlockOutState, puzzle: BlockOutPuzzle): GuideStep? {
        if (state.status != BlockOutStatus.Playing) return null
        val pos = IntArray(state.pieces.size) { state.pieces[it].pos }
        val move = BlockOutSolver.shortest(state.size, puzzle.shapes, pos)?.firstOrNull() ?: return null
        val piece = state.pieces[move.piece]
        val steps = kotlin.math.abs(move.to - piece.pos)
        val direction = when {
            piece.horizontal -> if (move.to < piece.pos) 2 else 3
            else -> if (move.to < piece.pos) 0 else 1
        }
        return GuideStep(GuideReason.BlockOutSlide, listOf(move.piece), listOf(direction, steps))
    }
}


/** The guide of arrow escape: an arrow that can leave now (there is no wrong move to take back). It points at every cell of the arrow; the numbers are its head. */
object ArrowsGuide {
    fun step(state: ArrowsState): GuideStep? {
        if (state.status != ArrowsStatus.Playing) return null
        val arrow = state.arrows.firstOrNull { it.free } ?: return null
        val cell = arrow.head
        return GuideStep(GuideReason.ArrowsEscape, arrow.cells.toList(), listOf(cell / state.size + 1, cell % state.size + 1))
    }
}

/**
 * The guide of the word groups game: the four words of a group that is not found yet, and its topic. With words already picked it
 * points at the group most of them belong to (the first one on a tie), otherwise at the first group that is left.
 */
object TopicGroupsGuide {
    fun step(state: TopicGroupsState): GuideStep? {
        if (state.status != TopicGroupsStatus.Playing) return null
        val left = state.titles.indices.filter { it !in state.solved }
        val group = left.maxByOrNull { g -> state.selected.count { it / 4 == g } } ?: return null
        return GuideStep(GuideReason.TopicGroupsGroup, state.wordsOf(group), text = state.titles[group])
    }
}

/**
 * The guide of the mini nonogram (every picture can be worked out from its numbers). A filled cell that is not part of the picture
 * is to be emptied first. Otherwise it points at a cell of the picture that is not filled yet, the one the numbers give soonest.
 */
object NonogramGuide {
    fun step(state: NonogramState): GuideStep? {
        if (state.status != NonogramStatus.Playing) return null
        val (order, _) = NonogramSolver.deduce(state.size, state.rowClues, state.columnClues)
        val picture = order.filter { it.second == NonogramSolver.FILLED }.map { it.first }
        val wrong = state.marks.indices.filter { state.marks[it] == NonogramMark.FILLED && it !in picture }.minOrNull()
        if (wrong != null) return step(GuideReason.NonogramEmpty, wrong, state.size)
        val next = picture.firstOrNull { state.marks[it] != NonogramMark.FILLED } ?: return null
        return step(GuideReason.NonogramFill, next, state.size)
    }

    private fun step(reason: GuideReason, cell: Int, size: Int) = GuideStep(reason, listOf(cell), listOf(cell / size + 1, cell % size + 1))
}

/**
 * The guide of axis sum: the board has one way to erase numbers, [solution] (a mark per cell, true for a number that stays). A
 * number that is erased but stays comes back first, then a number that stays but has to go.
 */
object AxisSumGuide {
    fun step(state: AxisSumState, solution: List<Boolean>?): GuideStep? {
        if (state.status != AxisSumStatus.Playing || solution == null) return null
        val wrong = state.erased.indices.firstOrNull { state.erased[it] && solution[it] }
        if (wrong != null) return step(GuideReason.AxisSumKeep, wrong, state.size)
        val missing = state.erased.indices.firstOrNull { !state.erased[it] && !solution[it] } ?: return null
        return step(GuideReason.AxisSumErase, missing, state.size)
    }

    private fun step(reason: GuideReason, cell: Int, size: Int) = GuideStep(reason, listOf(cell), listOf(cell / size + 1, cell % size + 1))
}

/**
 * The guide of ball slide: the first slide of a shortest way from where the board is now (the cells it goes over are the cells of
 * the step, and the direction is in the numbers). When the painted cells can no longer be completed it says so.
 */
object BallSlideGuide {
    fun step(puzzle: BallSlidePuzzle, state: BallSlideState): GuideStep? {
        if (state.status != BallSlideStatus.Playing) return null
        val painted = state.painted.indices.fold(0L) { acc, i -> if (state.painted[i]) acc or (1L shl i) else acc }
        val way = BallSlideSolver.shortest(puzzle, state.ball, painted) ?: return GuideStep(GuideReason.BallSlideStuck)
        val dir = way.firstOrNull() ?: return null
        // The cells the slide goes over: the page shows the way with arrows running along them, not a ring to tap.
        val path = puzzle.slide(state.ball, dir)
        if (path.isEmpty()) return null
        return GuideStep(GuideReason.BallSlideMove, path, listOf(dir))
    }
}

/**
 * The guide of color flow: a way that joins every color and covers the board (found by the search); for the first color whose path is
 * not that way's yet, the next few cells it goes through after what the player drew rightly so far. The number is the color.
 */
object FlowGuide {
    /** How many cells the guide points at. */
    private const val CELLS = 4

    fun step(puzzle: FlowPuzzle, state: FlowState): GuideStep? {
        if (state.status != FlowStatus.Playing) return null
        val solution = FlowSolver.solve(puzzle) ?: return null
        for (color in solution.indices) {
            val way = solution[color]
            val drawn = state.paths[color]
            // The path may have been drawn from either dot.
            val target = if (drawn.isNotEmpty() && drawn.first() == way.last()) way.asReversed() else way
            if (drawn == target) continue
            var common = 0
            while (common < drawn.size && common < target.size && drawn[common] == target[common]) common++
            val cells = target.subList(common, minOf(target.size, common + CELLS))
            if (cells.isEmpty()) continue
            return GuideStep(GuideReason.FlowRoute, cells, listOf(color))
        }
        return null
    }
}

/**
 * The guide of the matchstick sum: the one stick that puts it right. [GuideStep.cells] are the place the stick comes from and the place
 * it goes to, as numbers in [MatchPuzzle.slots]. When a stick is off its place after a wrong try, the guide says to put it back first.
 */
object MatchGuide {
    fun step(puzzle: MatchPuzzle, state: MatchState): GuideStep? {
        if (state.status != MatchStatus.Playing) return null
        if (state.moved) return GuideStep(GuideReason.MatchstickUndo)
        val move = MatchRules.winningMoves(puzzle.cells).firstOrNull() ?: return null
        return GuideStep(GuideReason.MatchstickMove, listOf(puzzle.slots.indexOf(move.from), puzzle.slots.indexOf(move.to)))
    }
}

/**
 * The guide of the laser mirrors: a mirror that has to be turned on the way to the fewest turns that light every target (the search
 * tries every way the mirrors can stand). [GuideStep.cells] is the cell of that mirror.
 */
object LaserGuide {
    fun step(puzzle: LaserPuzzle, state: LaserState): GuideStep? {
        if (state.status != LaserStatus.Playing) return null
        val turns = LaserRules.fewestTurns(state.size, state.row, state.mirrors, state.targets) ?: return null
        val cells = state.mirrors.indices.filter { state.mirrors[it] != 0 }
        val index = turns.indexOfFirst { it }
        if (index < 0) return null
        return GuideStep(GuideReason.LaserTurn, listOf(cells[index]))
    }
}

/**
 * The guide of the tangle: one dot to move and where to. A drawing with no crossing is found (see [UntangleEmbedder]) and turned,
 * scaled and moved (also mirrored if that fits better) to lie as near as it can to where the dots are now; the dot that is furthest from its
 * place in that drawing is the one to move. [GuideStep.cells] is the dot, [GuideStep.numbers] the place (x then y, in units of the board).
 */
object UntangleGuide {
    /** A dot this near its place (in units) is where it should be: the guide moves a dot that is at least this far, however near the rest are. */
    private const val NEAR = 2.0

    fun step(puzzle: UntanglePuzzle, state: UntangleState): GuideStep? {
        if (state.status != UntangleStatus.Playing) return null
        val drawing = UntangleEmbedder.embed(puzzle) ?: return null
        val n = puzzle.nodeCount
        val px = DoubleArray(n) { state.positions[2 * it].toDouble() }
        val py = DoubleArray(n) { state.positions[2 * it + 1].toDouble() }
        val cpx = px.average()
        val cpy = py.average()
        var best: Pair<DoubleArray, DoubleArray>? = null
        var bestError = Double.MAX_VALUE
        for (mirror in listOf(false, true)) {
            val qx = DoubleArray(n) { drawing[2 * it].toDouble().let { x -> if (mirror) UNTANGLE_BOARD - x else x } }
            val qy = DoubleArray(n) { drawing[2 * it + 1].toDouble() }
            val cqx = qx.average()
            val cqy = qy.average()
            var qq = 0.0
            var pp = 0.0
            var dot = 0.0
            var cross = 0.0
            for (i in 0 until n) {
                val ax = qx[i] - cqx
                val ay = qy[i] - cqy
                val bx = px[i] - cpx
                val by = py[i] - cpy
                qq += ax * ax + ay * ay
                pp += bx * bx + by * by
                dot += ax * bx + ay * by
                cross += ax * by - ay * bx
            }
            if (qq < 1e-9) continue
            val angle = kotlin.math.atan2(cross, dot)
            val scale = kotlin.math.sqrt(pp / qq).coerceAtLeast(0.25)
            val c = kotlin.math.cos(angle)
            val s = kotlin.math.sin(angle)
            val tx = DoubleArray(n)
            val ty = DoubleArray(n)
            var error = 0.0
            for (i in 0 until n) {
                val ax = qx[i] - cqx
                val ay = qy[i] - cqy
                tx[i] = (cpx + scale * (c * ax - s * ay)).coerceIn(UNTANGLE_MARGIN.toDouble(), (UNTANGLE_BOARD - UNTANGLE_MARGIN).toDouble())
                ty[i] = (cpy + scale * (s * ax + c * ay)).coerceIn(UNTANGLE_MARGIN.toDouble(), (UNTANGLE_BOARD - UNTANGLE_MARGIN).toDouble())
                error += (tx[i] - px[i]) * (tx[i] - px[i]) + (ty[i] - py[i]) * (ty[i] - py[i])
            }
            if (error < bestError) {
                bestError = error
                best = tx to ty
            }
        }
        val (tx, ty) = best ?: return null
        var node = -1
        var far = NEAR
        for (i in 0 until n) {
            val d = kotlin.math.hypot(tx[i] - px[i], ty[i] - py[i])
            if (d > far) {
                far = d
                node = i
            }
        }
        if (node < 0) return null
        return GuideStep(GuideReason.UntangleMove, listOf(node), listOf(Math.round(tx[node]).toInt(), Math.round(ty[node]).toInt()))
    }
}

/**
 * The guide of the fruit equations: what every fruit is worth. [GuideStep.numbers] are the fruits (numbers from 0 to 7) and then their
 * values; the player works the last line out from them.
 */
object FruitGuide {
    fun step(puzzle: FruitPuzzle, state: FruitState): GuideStep? {
        if (state.status != FruitStatus.Playing) return null
        return GuideStep(GuideReason.FruitValues, emptyList(), puzzle.fruits + puzzle.values)
    }
}

/**
 * The guide of the code breaker: a code that still fits everything the guesses told (the first one there is). It is a good next guess,
 * and it shows how to think: only a code that would have scored the same can be the secret. [GuideStep.numbers] are its colours.
 */
object CodeGuide {
    fun step(puzzle: CodePuzzle, state: CodeState): GuideStep? {
        if (state.status != CodeStatus.Playing) return null
        // A game saved with a long list holds only its last guesses; the codes that fit those are still a fair thing to try.
        val next = CodeSolver.nextGuess(puzzle, state.guesses) ?: return null
        return GuideStep(GuideReason.CodeNext, emptyList(), next)
    }
}

/**
 * The guide of the rectangles: if a rectangle that was laid is not part of the (one) answer, it says to take that off. Otherwise it points
 * at a rectangle of the answer that is not there yet, the one whose number had the fewest places to go to begin with (the one that can be
 * worked out first). [GuideStep.cells] are the rectangle's column, row, width and height, and [GuideStep.numbers] its number.
 */
object RectGuide {
    fun step(puzzle: RectPuzzle, state: RectState): GuideStep? {
        if (state.status != RectStatus.Playing) return null
        val answer = RectSolver.uniqueSolution(puzzle) ?: return null
        val wrong = state.rects.firstOrNull { it !in answer }
        if (wrong != null) return GuideStep(GuideReason.RectRemove, listOf(wrong.x, wrong.y, wrong.w, wrong.h))
        val clues = puzzle.clueCells
        val next = answer.filter { it !in state.rects }.minByOrNull { rect ->
            val cell = clues[answer.indexOf(rect)]
            RectSolver.candidates(puzzle, cell).size
        } ?: return null
        return GuideStep(GuideReason.RectPlace, listOf(next.x, next.y, next.w, next.h), listOf(next.area))
    }
}

/** The guide of the flash grid: the tiles of the round that are not found yet light up again. [GuideStep.cells] are those tiles. */
object FlashGuide {
    fun step(state: FlashState): GuideStep? {
        if (state.status != FlashStatus.Playing) return null
        val left = state.pattern.filter { it !in state.found }
        if (left.isEmpty()) return null
        return GuideStep(GuideReason.FlashPattern, left)
    }
}
