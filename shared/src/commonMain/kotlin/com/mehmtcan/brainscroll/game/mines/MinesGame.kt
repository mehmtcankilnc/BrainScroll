package com.mehmtcan.brainscroll.game.mines

import kotlin.random.Random

enum class MinesStatus { Playing, Won, Lost }

/** What an open or a chord did. */
enum class MinesResult {
    /** Nothing happened: the cell is open, flagged or out of range, or the game is over. Not counted. */
    Ignored,

    /** Cells were opened. */
    Opened,

    /** A mine was opened: the game is lost. */
    Exploded,
}

/** The picture of a board for the UI. Cells are numbered row by row from 0. */
data class MinesState(
    val rows: Int,
    val cols: Int,
    val mines: Int,
    /** For every cell: the number of mines around it if it is open, -1 if it is not. */
    val numbers: List<Int>,
    val flagged: Set<Int>,
    /** All mines once the game is lost, so the player sees where they were. Empty before that. */
    val mineCells: Set<Int>,
    /** The mine that was opened, or -1. */
    val exploded: Int,
    /** Opens and chords so far (flags are not moves). */
    val moves: Int,
    val status: MinesStatus,
) {
    /** Mines minus flags: what the counter shows. Can go below zero when the player flags too much. */
    val minesLeft: Int get() = mines - flagged.size
}

/**
 * The rules of minesweeper, free of UI and time.
 *
 * The mines are placed at the first open, so the first open never loses (docs/decisions.md): the cell and its
 * neighbors are free of mines, and the board is one that logic alone can finish. The board only depends on [seed] and
 * the first cell, so a saved game can be rebuilt exactly.
 */
class MinesGame(
    val rows: Int = DEFAULT_ROWS,
    val cols: Int = DEFAULT_COLS,
    val mineCount: Int = DEFAULT_MINES,
    private val seed: Int,
) {
    private var mines: BooleanArray? = null
    private var counts = IntArray(rows * cols)
    private val open = BooleanArray(rows * cols)
    private var openCount = 0
    private val flags = mutableSetOf<Int>()
    private var moves = 0
    private var firstCell = -1
    private var exploded = -1
    private var status = MinesStatus.Playing

    init {
        require(rows >= 2 && cols >= 2) { "a board needs some room" }
        require(mineCount in 1..(rows * cols - 9)) { "the first open needs 9 free cells" }
    }

    val size: Int get() = rows * cols

    val isFinished: Boolean get() = status != MinesStatus.Playing

    /** The cell the first open was on, or -1 while nothing was opened. Together with the seed it fixes the board. */
    val firstOpen: Int get() = firstCell

    val state: MinesState
        get() = MinesState(
            rows = rows,
            cols = cols,
            mines = mineCount,
            numbers = List(size) { if (open[it]) counts[it] else -1 },
            flagged = flags.toSet(),
            mineCells = if (status == MinesStatus.Lost) mines!!.indices.filter { mines!![it] }.toSet() else emptySet(),
            exploded = exploded,
            moves = moves,
            status = status,
        )

    /** Opens [cell]. Flooding: a cell with no mines around opens its neighbors too. */
    fun open(cell: Int): MinesResult {
        if (isFinished || cell !in 0 until size || open[cell] || cell in flags) return MinesResult.Ignored
        if (mines == null) startBoard(cell)
        moves++
        return reveal(cell)
    }

    /**
     * Flags or unflags a closed cell. Returns false when that is not possible (open cell, game over). The number of
     * flags is not limited: the counter can show a negative number.
     */
    fun toggleFlag(cell: Int): Boolean {
        if (isFinished || cell !in 0 until size || open[cell]) return false
        if (!flags.remove(cell)) flags += cell
        return true
    }

    /**
     * On an open number with as many flags around it as the number says: opens all the other neighbors at once. A wrong
     * flag makes that lose. Ignored on anything else.
     */
    fun chord(cell: Int): MinesResult {
        if (isFinished || cell !in 0 until size || !open[cell] || counts[cell] == 0) return MinesResult.Ignored
        val around = neighbors(cell, rows, cols)
        if (around.count { it in flags } != counts[cell]) return MinesResult.Ignored
        val targets = around.filter { !open[it] && it !in flags }
        if (targets.isEmpty()) return MinesResult.Ignored
        moves++
        for (target in targets) {
            // An earlier target may have flooded this one open already: it must not be opened (and counted) twice.
            if (open[target]) continue
            if (reveal(target) == MinesResult.Exploded) return MinesResult.Exploded
        }
        return MinesResult.Opened
    }

    private fun startBoard(first: Int) {
        firstCell = first
        val board = generate(rows, cols, mineCount, seed, first)
        mines = board
        counts = IntArray(size) { cell -> neighbors(cell, rows, cols).count { board[it] } }
    }

    private fun reveal(start: Int): MinesResult {
        val board = mines!!
        if (open[start]) return MinesResult.Opened
        if (board[start]) {
            exploded = start
            status = MinesStatus.Lost
            return MinesResult.Exploded
        }
        val queue = ArrayDeque<Int>()
        queue += start
        open[start] = true
        openCount++
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            if (counts[cell] != 0) continue
            for (next in neighbors(cell, rows, cols)) {
                if (open[next] || next in flags) continue
                open[next] = true
                openCount++
                queue += next
            }
        }
        if (openCount == size - mineCount) {
            status = MinesStatus.Won
            // A cleared board shows every mine flagged.
            flags.clear()
            for (i in 0 until size) if (board[i]) flags += i
        }
        return MinesResult.Opened
    }

    companion object {
        const val DEFAULT_ROWS = 8
        const val DEFAULT_COLS = 8
        const val DEFAULT_MINES = 10

        /** How many boards are tried before giving up on one that needs no guess (it never comes to that in practice). */
        private const val MAX_ATTEMPTS = 3_000

        fun neighbors(cell: Int, rows: Int, cols: Int): List<Int> {
            val r = cell / cols
            val c = cell % cols
            val result = ArrayList<Int>(8)
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until rows && nc in 0 until cols) result += nr * cols + nc
            }
            return result
        }

        /**
         * A mine layout for a game whose first open is on [first]: that cell and its neighbors are free, and the board
         * can be finished by [MinesSolver] alone. The same arguments always give the same board.
         */
        fun generate(rows: Int, cols: Int, mineCount: Int, seed: Int, first: Int): BooleanArray {
            val random = Random(seed)
            val free = neighbors(first, rows, cols).toSet() + first
            val candidates = (0 until rows * cols).filter { it !in free }
            var board = BooleanArray(rows * cols)
            repeat(MAX_ATTEMPTS) {
                board = BooleanArray(rows * cols)
                candidates.shuffled(random).take(mineCount).forEach { board[it] = true }
                if (solvableByLogic(rows, cols, board, first)) return board
            }
            return board
        }

        /** True if opening [first] and then only following what is certain clears the whole board. */
        fun solvableByLogic(rows: Int, cols: Int, board: BooleanArray, first: Int): Boolean {
            val total = rows * cols
            val counts = IntArray(total) { cell -> neighbors(cell, rows, cols).count { board[it] } }
            val open = BooleanArray(total)
            var openCount = 0

            fun flood(start: Int) {
                if (open[start]) return
                val queue = ArrayDeque<Int>()
                open[start] = true; openCount++
                queue += start
                while (queue.isNotEmpty()) {
                    val cell = queue.removeFirst()
                    if (counts[cell] != 0) continue
                    for (next in neighbors(cell, rows, cols)) {
                        if (!open[next]) { open[next] = true; openCount++; queue += next }
                    }
                }
            }

            flood(first)
            val goal = total - board.count { it }
            while (openCount < goal) {
                val numbers = IntArray(total) { if (open[it]) counts[it] else -1 }
                val next = MinesSolver.deduce(rows, cols, numbers).safe.filter { !open[it] }
                if (next.isEmpty()) return false
                next.forEach { flood(it) }
            }
            return true
        }
    }
}
