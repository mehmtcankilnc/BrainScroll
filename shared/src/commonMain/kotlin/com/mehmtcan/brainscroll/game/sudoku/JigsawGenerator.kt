package com.mehmtcan.brainscroll.game.sudoku

import kotlin.random.Random

/**
 * Makes the boards of the jigsaw sudoku (docs/yeni-oyunlar-plan.md): nine connected regions of nine cells instead of the 3 by 3
 * boxes, a solved grid for those regions, and givens taken away at random for as long as naked and hidden singles can still
 * finish the board, down to [DEFAULT_CLUES] givens at the fewest. The same promise as the classic sudoku: one solution, no
 * guessing. The board only depends on the seed and the number of givens, so a saved game can be rebuilt exactly.
 */
object JigsawGenerator {
    /** The fewest givens a board is cut down to; one that stops being solvable by singles earlier keeps a few more. */
    const val DEFAULT_CLUES = 28

    private const val SIZE = SudokuLayout.SIZE
    private const val CELLS = SudokuLayout.CELLS

    /** How many shuffles of the regions are tried (a few swaps each) and how many layouts before giving up on a seed. */
    private const val SWAPS = 3_000
    private const val MAX_LAYOUTS = 500

    fun generate(seed: Int, clues: Int = DEFAULT_CLUES): SudokuPuzzle {
        val random = Random(seed)
        repeat(MAX_LAYOUTS) {
            val layout = randomLayout(random)
            val solution = SudokuSolver.randomSolution(layout, random) ?: return@repeat
            val givens = solution.copyOf()
            var left = CELLS
            for (cell in (0 until CELLS).shuffled(random)) {
                if (left <= clues) break
                val digit = givens[cell]
                givens[cell] = 0
                if (SudokuGenerator.solveBySingles(givens, layout) != null) left-- else givens[cell] = digit
            }
            return SudokuPuzzle(givens, solution, layout)
        }
        error("no jigsaw layout with a solution found")
    }

    /**
     * Nine connected regions of nine cells. They start as the nine rows and are changed by exchanging a cell of one region that
     * touches another region for a cell of that other region that touches the first, as long as both regions stay in one piece.
     */
    fun randomLayout(random: Random): SudokuLayout {
        val region = IntArray(CELLS) { it / SIZE }
        repeat(SWAPS) {
            val x = random.nextInt(CELLS)
            val y = straightNeighbors(x).random(random)
            val regionA = region[x]
            val regionB = region[y]
            if (regionA == regionB) return@repeat
            val a = (0 until CELLS).filter { region[it] == regionA && straightNeighbors(it).any { n -> region[n] == regionB } }.random(random)
            val b = (0 until CELLS).filter { region[it] == regionB && straightNeighbors(it).any { n -> region[n] == regionA } }.random(random)
            region[a] = regionB
            region[b] = regionA
            if (!connected(region, regionA) || !connected(region, regionB)) {
                region[a] = regionA
                region[b] = regionB
            }
        }
        return SudokuLayout(region)
    }

    private fun straightNeighbors(cell: Int): List<Int> {
        val row = cell / SIZE
        val column = cell % SIZE
        val result = ArrayList<Int>(4)
        if (row > 0) result += cell - SIZE
        if (row < SIZE - 1) result += cell + SIZE
        if (column > 0) result += cell - 1
        if (column < SIZE - 1) result += cell + 1
        return result
    }

    /** True if the cells of [target] are all in one piece (each reachable from the others along rows and columns). */
    internal fun connected(region: IntArray, target: Int): Boolean {
        val start = region.indexOf(target)
        if (start < 0) return false
        val seen = BooleanArray(CELLS)
        val queue = ArrayDeque<Int>()
        queue.add(start)
        seen[start] = true
        var reached = 0
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            reached++
            for (next in straightNeighbors(cell)) if (!seen[next] && region[next] == target) {
                seen[next] = true
                queue.add(next)
            }
        }
        return reached == region.count { it == target }
    }
}

/** Finds a full grid for a layout. */
internal object SudokuSolver {
    private const val ALL = 0x1FF

    /** How many cells are tried before the search gives the layout up (some layouts have no solution at all). */
    private const val NODE_LIMIT = 100_000

    /** A random full grid for [layout] (every row, column and region holds 1 to 9 once), or null if none was found. */
    fun randomSolution(layout: SudokuLayout, random: Random): IntArray? {
        val grid = IntArray(SudokuLayout.CELLS)
        val rows = IntArray(SudokuLayout.SIZE)
        val columns = IntArray(SudokuLayout.SIZE)
        val regions = IntArray(SudokuLayout.SIZE)
        var nodes = 0

        fun candidates(cell: Int) = ALL and (rows[cell / SudokuLayout.SIZE] or columns[cell % SudokuLayout.SIZE] or regions[layout.regionOf[cell]]).inv()

        fun search(): Boolean {
            // The empty cell with the fewest digits left to try.
            var best = -1
            var bestCount = 10
            for (cell in grid.indices) {
                if (grid[cell] != 0) continue
                val count = candidates(cell).countOneBits()
                if (count == 0) return false
                if (count < bestCount) {
                    best = cell
                    bestCount = count
                }
            }
            if (best < 0) return true
            if (++nodes > NODE_LIMIT) return false
            val digits = (1..SudokuLayout.SIZE).filter { candidates(best) and (1 shl (it - 1)) != 0 }.shuffled(random)
            for (digit in digits) {
                val bit = 1 shl (digit - 1)
                grid[best] = digit
                rows[best / SudokuLayout.SIZE] = rows[best / SudokuLayout.SIZE] or bit
                columns[best % SudokuLayout.SIZE] = columns[best % SudokuLayout.SIZE] or bit
                regions[layout.regionOf[best]] = regions[layout.regionOf[best]] or bit
                if (search()) return true
                grid[best] = 0
                rows[best / SudokuLayout.SIZE] = rows[best / SudokuLayout.SIZE] and bit.inv()
                columns[best % SudokuLayout.SIZE] = columns[best % SudokuLayout.SIZE] and bit.inv()
                regions[layout.regionOf[best]] = regions[layout.regionOf[best]] and bit.inv()
            }
            return false
        }
        return if (search()) grid else null
    }
}
