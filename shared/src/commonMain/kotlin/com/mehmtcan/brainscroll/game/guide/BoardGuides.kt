package com.mehmtcan.brainscroll.game.guide

import com.mehmtcan.brainscroll.game.arrows.ArrowsState
import com.mehmtcan.brainscroll.game.fruitmath.FruitPuzzle
import com.mehmtcan.brainscroll.game.codebreaker.CodePuzzle
import com.mehmtcan.brainscroll.game.rectangles.RectPuzzle
import com.mehmtcan.brainscroll.game.colorseq.ColorSeqState
import com.mehmtcan.brainscroll.game.cubefold.CubeState
import com.mehmtcan.brainscroll.game.cubefold.CubeStatus
import com.mehmtcan.brainscroll.game.cubefold.CubeWhy
import com.mehmtcan.brainscroll.game.colorseq.ColorSeqStatus
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
import com.mehmtcan.brainscroll.game.fences.FencePuzzle
import com.mehmtcan.brainscroll.game.fences.FenceSolver
import com.mehmtcan.brainscroll.game.fences.FenceState
import com.mehmtcan.brainscroll.game.fences.FenceStatus
import com.mehmtcan.brainscroll.game.greaterless.GlPuzzle
import com.mehmtcan.brainscroll.game.greaterless.GlRules
import com.mehmtcan.brainscroll.game.greaterless.GlSolver
import com.mehmtcan.brainscroll.game.greaterless.GlState
import com.mehmtcan.brainscroll.game.greaterless.GlStatus
import com.mehmtcan.brainscroll.game.lamps.LampMark
import com.mehmtcan.brainscroll.game.lamps.LampPuzzle
import com.mehmtcan.brainscroll.game.lamps.LampSolver
import com.mehmtcan.brainscroll.game.lamps.LampState
import com.mehmtcan.brainscroll.game.lamps.LampStatus
import com.mehmtcan.brainscroll.game.lasers.LaserPuzzle
import com.mehmtcan.brainscroll.game.zerosones.BinPuzzle
import com.mehmtcan.brainscroll.game.zerosones.BinSolver
import com.mehmtcan.brainscroll.game.zerosones.BinState
import com.mehmtcan.brainscroll.game.zerosones.BinStatus
import com.mehmtcan.brainscroll.game.lonenumber.LoneMark
import com.mehmtcan.brainscroll.game.crosssum.CrossSolver
import com.mehmtcan.brainscroll.game.crosssum.CrossState
import com.mehmtcan.brainscroll.game.crosssum.CrossStatus
import com.mehmtcan.brainscroll.game.mirror.MirrorState
import com.mehmtcan.brainscroll.game.mirror.MirrorStatus
import com.mehmtcan.brainscroll.game.oddone.OddState
import com.mehmtcan.brainscroll.game.oddone.OddStatus
import com.mehmtcan.brainscroll.game.lonenumber.LonePuzzle
import com.mehmtcan.brainscroll.game.lonenumber.LoneSolver
import com.mehmtcan.brainscroll.game.lonenumber.LoneState
import com.mehmtcan.brainscroll.game.lonenumber.LoneStatus
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
import kotlin.math.roundToInt

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
        if (safe != null) {
            val why = MinesSolver.why(state.rows, state.cols, state.numbers.toIntArray(), found, safe)
            return GuideStep(GuideReason.MinesSafe, listOf(safe), listOf(why.number), evidence = why.evidence, rule = why.rule)
        }
        val mine = found.mines.filter { state.numbers[it] < 0 && it !in state.flagged }.minOrNull()
        if (mine != null) {
            val why = MinesSolver.why(state.rows, state.cols, state.numbers.toIntArray(), found, mine)
            return GuideStep(GuideReason.MinesMine, listOf(mine), listOf(why.number), evidence = why.evidence, rule = why.rule)
        }
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
                // One peer for each of the eight other digits: the cells that take them away.
                val digit = candidates[cell].countTrailingZeroBits() + 1
                val holders = (1..SIZE).filter { it != digit }.mapNotNull { other -> layout.peers[cell].firstOrNull { values[it] == other } }
                return GuideStep(if (jigsaw) GuideReason.JigsawOnlyDigit else GuideReason.SudokuOnlyDigit, listOf(cell), listOf(digit), evidence = holders)
            }
        }
        // A digit that has one place left in a row, a column or a box (a region).
        for ((at, unit) in layout.units.withIndex()) for (digit in 1..SIZE) {
            if (unit.any { values[it] == digit }) continue
            val places = unit.filter { values[it] == 0 && candidates[it] and (1 shl (digit - 1)) != 0 }
            if (places.size == 1) {
                // The cells that already hold the digit and keep it out of the other empty cells of the unit.
                val blockers = unit.filter { values[it] == 0 && it != places[0] }
                    .mapNotNull { open -> layout.peers[open].firstOrNull { values[it] == digit } }.distinct()
                // 1 a row, 2 a column, 3 a box (a region on the jigsaw board).
                return GuideStep(if (jigsaw) GuideReason.JigsawOnlyPlace else GuideReason.SudokuOnlyPlace, places, listOf(digit), evidence = blockers, rule = at / SIZE + 1)
            }
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
        // The tap switches the cell and its neighbours: the lit ones among them go out, the dark ones come on.
        val size = state.size
        val row = cell / size
        val column = cell % size
        val footprint = buildList {
            add(cell)
            if (row > 0) add(cell - size)
            if (row < size - 1) add(cell + size)
            if (column > 0) add(cell - 1)
            if (column < size - 1) add(cell + 1)
        }
        val lit = footprint.filter { state.lit[it] }
        // A tap that puts out more lights than it lights is the plain case; the others are needed for what comes later.
        if (2 * lit.size > footprint.size) {
            return GuideStep(GuideReason.LightsTap, listOf(cell), listOf(lit.size, footprint.size - lit.size), evidence = lit, rule = 1)
        }
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
        // The disk that moves is the smallest one on the tower it leaves. The smallest disk of all moves every other turn, always the same
        // way round; the turns in between are the only other legal move.
        val disk = state.pegs.indexOfFirst { it == move.from }
        return GuideStep(GuideReason.HanoiMove, listOf(move.from, move.to), listOf(move.from, move.to, disk + 1), rule = if (disk == 0) 1 else 2)
    }
}

/**
 * The guide of the queens puzzle: of the full placements that hold the locked queens, the one that has most of the queens
 * the player already put down. It says to put one of the missing queens of that placement, or, if every queen of it is there
 * already, to take away one that is not part of it.
 */
object QueensGuide {
    /**
     * Whether [cell] is the only cell of its row (rule 1) or of its column (rule 2) that no queen of [placed] attacks, with the queens that
     * attack the other cells of that line; null when it is not.
     */
    private fun queenWhy(size: Int, placed: Set<Int>, cell: Int): Triple<Int, List<Int>, Int>? {
        fun attacks(queen: Int, other: Int): Boolean {
            val rows = queen / size - other / size
            val columns = queen % size - other % size
            return rows == 0 || columns == 0 || abs(rows) == abs(columns)
        }
        val row = cell / size
        val column = cell % size
        for ((rule, line) in listOf(1 to (0 until size).map { row * size + it }, 2 to (0 until size).map { it * size + column })) {
            val others = line.filter { it != cell }
            val attackers = others.map { o -> placed.firstOrNull { attacks(it, o) } }
            if (attackers.all { it != null } && placed.none { attacks(it, cell) }) return Triple(rule, attackers.filterNotNull().distinct(), 0)
        }
        return null
    }

    fun step(state: QueensState): GuideStep? {
        if (state.status != QueensStatus.Playing) return null
        val locked = state.locked.indices.filter { state.locked[it] }.toSet()
        val placed = state.queens.indices.filter { state.queens[it] }.toSet()
        val best = QueensSolver.solutions(state.size, locked).maxByOrNull { solution -> solution.count { it in placed } } ?: return null
        val missing = best.filter { it !in placed }
        if (missing.isNotEmpty()) {
            // A queen that the others leave the only free cell of its row or column says why it is the one: the queens that attack the rest.
            val withReason = missing.firstNotNullOfOrNull { cell -> queenWhy(state.size, placed, cell)?.let { cell to it } }
            val (cell, why) = withReason ?: (missing.first() to Triple(0, emptyList<Int>(), 0))
            return GuideStep(GuideReason.QueensPlace, listOf(cell), listOf(cell / state.size + 1, cell % state.size + 1), evidence = why.second, rule = why.first)
        }
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
        val why = HashMap<Int, TentsSolver.Why>()
        val derived = TentsSolver.deduce(state.size, trees, state.rowTargets, state.columnTargets, placed, why)
        // A tree that has only one place left is the plainest reason: it comes first, then a line that is full of tents.
        val cell = derived.firstOrNull { it in missing && why[it]?.rule == TentsSolver.TREE_ONLY }
            ?: derived.firstOrNull { it in missing }
            ?: missing.minOrNull() ?: return null
        val reason = why[cell]
        if (reason != null && cell in derived) {
            return GuideStep(GuideReason.TentsPlace, listOf(cell), listOf(cell / state.size + 1, cell % state.size + 1), evidence = reason.evidence, rule = reason.rule)
        }
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
        // The block that has to reach the exit (1), or a block that is in its way or in the way of the one that clears it (2).
        return GuideStep(GuideReason.BlockOutSlide, listOf(move.piece), listOf(direction, steps), rule = if (piece.isTarget) 1 else 2)
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
        val missing = picture.filter { state.marks[it] != NonogramMark.FILLED }
        // A cell its own row or column fills by itself comes first: the line is ringed and the block that covers the cell is named.
        for (next in missing) {
            val size = state.size
            val inRow = lineBlock(state.rowClues[next / size], size, next % size)
            if (inRow != null) return step(GuideReason.NonogramFill, next, size, inRow.first, inRow.second, (0 until size).map { next / size * size + it }.filter { it != next })
            val inColumn = lineBlock(state.columnClues[next % size], size, next / size)
            if (inColumn != null) return step(GuideReason.NonogramFill, next, size, inColumn.first + 2, inColumn.second, (0 until size).map { it * size + next % size }.filter { it != next })
        }
        val next = missing.firstOrNull() ?: return null
        return step(GuideReason.NonogramFill, next, state.size)
    }

    /**
     * Whether the numbers of one line fill the cell at [at] whatever way their blocks slide: the first number is the rule (1 when the numbers
     * leave no room to slide, 2 when a block is longer than the room), the second the length of the block that covers the cell. Null if not.
     */
    private fun lineBlock(clue: List<Int>, size: Int, at: Int): Pair<Int, Int>? {
        if (clue.isEmpty() || clue.all { it == 0 }) return null
        val slack = size - (clue.sum() + clue.size - 1)
        var start = 0
        for (length in clue) {
            if (length > slack && at >= start + slack && at < start + length) return (if (slack == 0) 1 else 2) to length
            start += length + 1
        }
        return null
    }

    private fun step(reason: GuideReason, cell: Int, size: Int, rule: Int, block: Int, evidence: List<Int>) =
        GuideStep(reason, listOf(cell), listOf(cell / size + 1, cell % size + 1, block), evidence = evidence, rule = rule)

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
        // A number that is exactly what its row (or column) has too much: erasing it makes that line right.
        val size = state.size
        val value = state.digits[missing]
        val row = missing / size
        val column = missing % size
        val rowExcess = state.rowSums[row] - state.rowTargets[row]
        val columnExcess = state.columnSums[column] - state.columnTargets[column]
        val numbers = listOf(row + 1, column + 1, value)
        return when {
            rowExcess == value -> GuideStep(GuideReason.AxisSumErase, listOf(missing), numbers, evidence = (0 until size).map { row * size + it }, rule = 1)
            columnExcess == value -> GuideStep(GuideReason.AxisSumErase, listOf(missing), numbers, evidence = (0 until size).map { it * size + column }, rule = 2)
            else -> step(GuideReason.AxisSumErase, missing, size)
        }
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
        return GuideStep(GuideReason.UntangleMove, listOf(node), listOf(tx[node].roundToInt(), ty[node].roundToInt()))
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

/**
 * The guide of the flash grid: the tiles of the round that are not found yet light up again. [GuideStep.cells] are those tiles.
 * Nothing before the run is started: the tiles have not been shown yet, so there is nothing to show again (like the color sequence).
 */
object FlashGuide {
    fun step(state: FlashState): GuideStep? {
        if (state.status != FlashStatus.Playing || !state.started) return null
        val left = state.pattern.filter { it !in state.found }
        if (left.isEmpty()) return null
        return GuideStep(GuideReason.FlashPattern, left)
    }
}

/**
 * The guide of the lone numbers: a cell that is marked wrong (shaded or circled when the answer has it the other way) is the first thing
 * to put right; otherwise it points at a cell of the answer that is not shaded yet, the one whose number repeats most in its row and
 * column (the one that is easiest to see). [GuideStep.cells] is the cell (row * size + column).
 */
object LoneGuide {
    fun step(puzzle: LonePuzzle, state: LoneState): GuideStep? {
        if (state.status != LoneStatus.Playing) return null
        val answer = LoneSolver.uniqueSolution(puzzle) ?: return null
        val n = puzzle.size
        val marks = state.marks
        val wrong = marks.indices.firstOrNull { cell ->
            val mark = marks[cell]
            (mark == LoneMark.Shaded && cell !in answer) ||
                (mark == LoneMark.Circled && cell in answer)
        }
        if (wrong != null) return GuideStep(GuideReason.LoneClear, listOf(wrong))
        val next = answer.filter { marks[it] != LoneMark.Shaded }.maxByOrNull { cell ->
            val number = puzzle.numbers[cell]
            (0 until n).count { k ->
                (k != cell % n && puzzle.numbers[cell / n * n + k] == number) || (k != cell / n && puzzle.numbers[k * n + cell % n] == number)
            }
        } ?: return null
        // The same number in the row or column of the cell: the cells that say why this one is the one to go.
        val number = puzzle.numbers[next]
        val same = (0 until n).flatMap { k -> listOf(next / n * n + k, k * n + next % n) }
            .filter { it != next && puzzle.numbers[it] == number && marks[it] != LoneMark.Shaded }
            .distinct()
        return GuideStep(GuideReason.LoneShade, listOf(next), listOf(number), evidence = same, rule = if (same.isEmpty()) 0 else 1)
    }
}

/**
 * The guide of the zeros and ones: a digit the player put that is not the answer's is the first thing to put right. Otherwise it points
 * at a cell that the rules force right now (a pair of the same digit, a line that has all its 0s or 1s); if the rules force nothing
 * yet it takes the first cell where the other digit would lead to a broken rule once the rules are followed through; if there is no
 * such cell it takes the first empty one. [GuideStep.cells] is the cell (row * size + column) and [GuideStep.numbers] its digit.
 */
object BinGuide {
    fun step(puzzle: BinPuzzle, state: BinState): GuideStep? {
        if (state.status != BinStatus.Playing) return null
        val answer = BinSolver.uniqueSolution(puzzle) ?: return null
        val n = puzzle.size
        val cells = state.cells.toIntArray()
        val wrong = cells.indices.firstOrNull { !puzzle.isGiven(it) && cells[it] >= 0 && cells[it] != answer[it] }
        if (wrong != null) return GuideStep(GuideReason.BinClear, listOf(wrong))
        BinSolver.forcedWhy(n, cells).firstOrNull { cells[it.cell] < 0 }?.let {
            return GuideStep(GuideReason.BinFill, listOf(it.cell), listOf(it.digit, 1 - it.digit), evidence = it.evidence, rule = it.rule)
        }
        val cornered = cells.indices.firstOrNull { cell ->
            cells[cell] < 0 && cells.copyOf().also { it[cell] = 1 - answer[cell] }.let { !BinSolver.propagate(n, it) }
        }
        val pick = cornered ?: cells.indices.firstOrNull { cells[it] < 0 } ?: return null
        return GuideStep(GuideReason.BinFill, listOf(pick), listOf(answer[pick], 1 - answer[pick]))
    }
}

/**
 * The guide of the lamps: a mark the player put that is not the answer's (a lamp where the answer has none, a dot where the answer has a
 * lamp) is the first thing to put right. Otherwise it points at a cell that the rules force to hold a lamp right now (a cell that only one
 * place can light, a number that needs all its free cells); if the rules force none yet it takes a cell where "no lamp" would break a rule
 * once the rules are followed through; if there is none it takes the first lamp of the answer that is not there. [GuideStep.cells] is the
 * cell (row * size + column).
 */
object LampGuide {
    fun step(puzzle: LampPuzzle, state: LampState): GuideStep? {
        if (state.status != LampStatus.Playing) return null
        val answer = LampSolver.uniqueSolution(puzzle) ?: return null
        val marks = state.marks
        val wrong = marks.indices.firstOrNull { cell ->
            (marks[cell] == LampMark.Lamp && !answer[cell]) || (marks[cell] == LampMark.Dot && answer[cell])
        }
        if (wrong != null) return GuideStep(GuideReason.LampClear, listOf(wrong))
        // What the player has put, as the solver sees it: lamps are lamps, everything else is still open.
        val cells = IntArray(marks.size) { if (marks[it] == LampMark.Lamp) LampSolver.LAMP else LampSolver.UNKNOWN }
        // What the board shows a player: the cells a lamp lights and the cells around a full number cannot hold a lamp.
        while (true) {
            val none = LampSolver.forcedWhy(puzzle, cells).filter { it.value == LampSolver.NONE && cells[it.cell] == LampSolver.UNKNOWN }
            if (none.isEmpty()) break
            for (f in none) cells[f.cell] = LampSolver.NONE
        }
        LampSolver.forcedWhy(puzzle, cells).firstOrNull { it.value == LampSolver.LAMP && marks[it.cell] != LampMark.Lamp }?.let { f ->
            // A wall number that needs every free cell says its number; a dark cell that only this cell can light is ringed.
            val numbers = if (f.rule == LampSolver.WALL_NEEDS) listOf(puzzle.numberAt(f.evidence)) else emptyList()
            return GuideStep(GuideReason.LampPlace, listOf(f.cell), numbers, evidence = listOf(f.evidence), rule = f.rule)
        }
        val missing = puzzle.whiteCells.filter { answer[it] && marks[it] != LampMark.Lamp }
        val cornered = missing.firstOrNull { cell -> cells.copyOf().also { it[cell] = LampSolver.NONE }.let { !LampSolver.propagate(puzzle, it) } }
        val pick = cornered ?: missing.firstOrNull() ?: return null
        return GuideStep(GuideReason.LampPlace, listOf(pick))
    }
}

/**
 * The guide of the greater and less: a number the player put that is not the answer's is the first thing to put right. Otherwise it
 * points at a cell that only one number can fill by the rules right now (a row or column that has the others, a sign next to a number);
 * if the rules alone leave none it takes one that trying a number shows; if there is none it takes the first empty cell.
 * [GuideStep.cells] is the cell (row * size + column) and [GuideStep.numbers] its number.
 */
object GlGuide {
    fun step(puzzle: GlPuzzle, state: GlState): GuideStep? {
        if (state.status != GlStatus.Playing) return null
        val answer = GlSolver.uniqueSolution(puzzle) ?: return null
        val cells = state.cells.toIntArray()
        val wrong = cells.indices.firstOrNull { !puzzle.isGiven(it) && cells[it] > 0 && cells[it] != answer[it] }
        if (wrong != null) return GuideStep(GuideReason.GlClear, listOf(wrong))
        for (lookahead in listOf(false, true)) {
            val sets = GlSolver.sets(puzzle, cells)
            if (!GlSolver.refine(puzzle, sets, lookahead)) continue
            val forced = cells.indices.firstOrNull { cells[it] == 0 && GlSolver.valueOf(sets[it]) > 0 }
            if (forced != null) {
                val value = GlSolver.valueOf(sets[forced])
                val (rule, evidence) = glWhy(puzzle, cells, forced, value)
                return GuideStep(GuideReason.GlFill, listOf(forced), listOf(value), evidence = evidence, rule = rule)
            }
        }
        val pick = cells.indices.firstOrNull { cells[it] == 0 } ?: return null
        return GuideStep(GuideReason.GlFill, listOf(pick), listOf(answer[pick]))
    }

    /**
     * Which of the plain reasons puts [value] in [cell]: the row or column already holds every other number (1), the signs next to filled
     * cells and the numbers of the row and column leave only this one (2), the number fits no other cell of a line (3); 0 when it takes
     * more steps than that. With the cells that show it.
     */
    private fun glWhy(puzzle: GlPuzzle, cells: IntArray, cell: Int, value: Int): Pair<Int, List<Int>> {
        val n = puzzle.size
        val row = cell / n
        val column = cell % n
        val rowCells = (0 until n).map { row * n + it }.filter { it != cell }
        val columnCells = (0 until n).map { it * n + column }.filter { it != cell }
        for (line in listOf(rowCells, columnCells)) {
            val filled = line.filter { cells[it] > 0 }
            if (filled.size == n - 1) return 1 to filled
        }
        // What the numbers of the row and column leave, then what the signs to filled neighbours take away.
        var left = (1..n).filter { v -> (rowCells + columnCells).none { cells[it] == v } }
        val signed = ArrayList<Int>()
        for ((a, b, sign) in GlRules.links(puzzle)) {
            val other = when (cell) {
                a -> b
                b -> a
                else -> continue
            }
            if (cells[other] <= 0) continue
            val smaller = if (sign == GlPuzzle.LESS) a else b
            val keep = if (cell == smaller) left.filter { it < cells[other] } else left.filter { it > cells[other] }
            if (keep.size < left.size) signed += other
            left = keep
        }
        if (left == listOf(value) && signed.isNotEmpty()) return 2 to signed
        // The number fits no other cell of a line: the cells that already hold it block the others.
        for (line in listOf(rowCells, columnCells)) {
            val others = line.filter { cells[it] == 0 }
            if (others.isEmpty()) continue
            val blockers = others.map { o ->
                val oRow = o / n
                val oColumn = o % n
                (0 until n).flatMap { listOf(oRow * n + it, it * n + oColumn) }.firstOrNull { it != o && cells[it] == value }
            }
            if (blockers.all { it != null }) return 3 to blockers.filterNotNull().distinct()
        }
        return 0 to emptyList()
    }
}

/**
 * The guide of the fences: a mark the player put that is not the answer's (a line where the answer has none, a cross where it has a
 * line) is the first thing to put right. Otherwise it always points at a line still to draw, never at a cross: an edge that the rules
 * force to be a line right now, the one that goes on from the lines drawn so far first (a number that needs all its free sides, a dot
 * that has one line and one way to go); if the rules alone force none, one that a try and see step forces; if there is none, the
 * line of the answer that goes on from the drawn ones. [GuideStep.cells] is the edge number (see FenceGeometry).
 */
object FenceGuide {
    fun step(puzzle: FencePuzzle, state: FenceState): GuideStep? {
        if (state.status != FenceStatus.Playing) return null
        val answer = FenceSolver.solveByLogic(puzzle, true) ?: return null
        val g = puzzle.geometry
        val marks = state.marks
        val wrong = marks.indices.firstOrNull { e ->
            (marks[e] == FenceSolver.LINE && answer[e] != FenceSolver.LINE) || (marks[e] == FenceSolver.CROSS && answer[e] == FenceSolver.LINE)
        }
        if (wrong != null) return GuideStep(GuideReason.FenceClear, listOf(wrong))
        // The dots the drawn lines reach: a line that starts from one of them goes on from what the player has.
        val reached = HashSet<Int>()
        for (e in marks.indices) if (marks[e] == FenceSolver.LINE) for (d in g.edgeDots[e]) reached += d
        fun best(edges: List<Int>): Int? = edges.firstOrNull { e -> g.edgeDots[e].any { it in reached } } ?: edges.firstOrNull()
        for (lookahead in listOf(false, true)) {
            val refined = marks.toIntArray()
            if (!FenceSolver.refine(puzzle, refined, lookahead)) continue
            val pick = best(marks.indices.filter { marks[it] == FenceSolver.UNKNOWN && refined[it] == FenceSolver.LINE })
            if (pick != null) return fenceStep(puzzle, pick, refined, marks.toIntArray())
        }
        val pick = best(marks.indices.filter { answer[it] == FenceSolver.LINE && marks[it] != FenceSolver.LINE }) ?: return null
        return GuideStep(GuideReason.FenceLine, listOf(pick))
    }

    /**
     * The step for the line on edge [pick], with its reason as the rules see the board [state] (the crosses they found included): a numbered
     * square whose free sides are as many as its number needs all of them (rule 1, the square is ringed: in the numbering of the page it is
     * the number of edges plus the square), or a dot where a line the player drew has only this way left to go on (rule 2, that line is
     * ringed); 0 when it takes more.
     */
    private fun fenceStep(puzzle: FencePuzzle, pick: Int, state: IntArray, drawn: IntArray): GuideStep {
        val g = puzzle.geometry
        val probe = state.copyOf().also { it[pick] = FenceSolver.UNKNOWN }
        for (cell in g.edgeCells[pick]) {
            val clue = puzzle.clues[cell]
            if (clue < 0) continue
            val lines = g.cellEdges[cell].count { probe[it] == FenceSolver.LINE }
            val free = g.cellEdges[cell].count { probe[it] == FenceSolver.UNKNOWN }
            if (lines + free == clue) return GuideStep(GuideReason.FenceLine, listOf(pick), listOf(clue), evidence = listOf(g.edges + cell), rule = 1)
        }
        for (dot in g.edgeDots[pick]) {
            val lines = g.dotEdges[dot].filter { probe[it] == FenceSolver.LINE }
            val free = g.dotEdges[dot].count { probe[it] == FenceSolver.UNKNOWN }
            // The line it goes on from has to be one the player drew: a line the rules only found cannot be ringed.
            if (lines.size == 1 && free == 1 && drawn[lines[0]] == FenceSolver.LINE) return GuideStep(GuideReason.FenceLine, listOf(pick), evidence = lines, rule = 2)
        }
        return GuideStep(GuideReason.FenceLine, listOf(pick))
    }
}

/**
 * The guide of the mirror: the first cell of the half to fill that is not like its partner. A cell that has to be painted comes first
 * (the board is finished by painting), then one that has to be emptied. [GuideStep.cells] is the cell, [GuideStep.evidence] its partner.
 */
object MirrorGuide {
    fun step(state: MirrorState): GuideStep? {
        if (state.status != MirrorStatus.Playing) return null
        val puzzle = state.puzzle
        val wrong = state.mismatched
        val cell = wrong.firstOrNull { puzzle.target(it) } ?: wrong.firstOrNull() ?: return null
        val reason = if (puzzle.target(cell)) GuideReason.MirrorPaint else GuideReason.MirrorEmpty
        return GuideStep(reason, listOf(cell), evidence = listOf(puzzle.partner(cell)))
    }
}

/**
 * The guide of odd one out: it names the rule that four of the five numbers follow and rings two of those numbers, so the player still
 * has to find which of the other three does not follow it. [GuideStep.cells] is the first of the two numbers, [GuideStep.evidence] the
 * second, [GuideStep.rule] the index of the rule in the list of the rules.
 */
object OddGuide {
    fun step(state: OddState): GuideStep? {
        if (state.status != OddStatus.Playing) return null
        val question = state.question
        val followers = question.numbers.indices.filter { it != question.oddIndex }
        return GuideStep(GuideReason.OddRule, listOf(followers[0]), evidence = listOf(followers[1]), rule = question.rule.ordinal)
    }
}

/**
 * The guide of the cross sums: a digit the player put that is not the answer's is the first thing to put right. Otherwise it points at
 * the cell the sums decide most easily (see [CrossSolver.hint]): [GuideStep.cells] is the cell, [GuideStep.numbers] its digit,
 * [GuideStep.evidence] the other cells of the block or blocks that say so, [GuideStep.rule] how it follows.
 */
object CrossGuide {
    fun step(state: CrossState): GuideStep? {
        if (state.status != CrossStatus.Playing) return null
        val puzzle = state.puzzle
        val wrong = puzzle.openCells.firstOrNull { state.values[it] > 0 && state.values[it] != puzzle.solution[it] }
        if (wrong != null) return GuideStep(GuideReason.CrossClear, listOf(wrong))
        val hint = CrossSolver.hint(puzzle, state.values.toIntArray()) ?: return null
        return GuideStep(GuideReason.CrossDigit, listOf(hint.cell), listOf(hint.digit), evidence = hint.evidence, rule = hint.rule)
    }
}

/** The guide of the color sequence: once the run is started, the lights of the round are played again ([GuideStep.cells] are the buttons, in order). */
object ColorSeqGuide {
    fun step(state: ColorSeqState): GuideStep? {
        if (state.status != ColorSeqStatus.Playing || !state.started) return null
        return GuideStep(GuideReason.SeqReplay, state.sequence)
    }
}

/**
 * The guide of cube folding: it rules out the first wrong cube and says why. [GuideStep.cells] is the option, [GuideStep.evidence] the
 * squares of the net that show it (the net squares are numbered from [NET_BASE] so they do not mix with the options).
 */
object CubeGuide {
    const val NET_BASE = 10

    fun step(state: CubeState): GuideStep? {
        if (state.status != CubeStatus.Playing) return null
        val question = state.question
        val wrong = question.whys.indexOfFirst { it != CubeWhy.Right }
        if (wrong < 0) return null
        val reason = if (question.whys[wrong] == CubeWhy.Opposite) GuideReason.CubeOpposite else GuideReason.CubeMirror
        return GuideStep(reason, listOf(wrong), evidence = question.evidence(wrong).map { it + NET_BASE })
    }
}
