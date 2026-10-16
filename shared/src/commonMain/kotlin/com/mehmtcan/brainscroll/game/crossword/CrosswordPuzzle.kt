package com.mehmtcan.brainscroll.game.crossword

import com.mehmtcan.brainscroll.game.wordle.Language

/**
 * One answer of the board: the cells of its letters in order, and the clue square that holds its clue. Cells are numbered row by row
 * from the top left. The clue square is the one to the left of the first cell ([across]) or above it, and the arrow goes straight; or,
 * when [bent] is true, it is the one above the first cell of an answer going across, or on the left of the first cell of an answer going
 * down, and the arrow turns once.
 */
class CrosswordSlot(val clueCell: Int, val across: Boolean, val cells: List<Int>, val bent: Boolean)

/** The arrow crossword's way of laying out a board: squares that hold clues, and runs of letters that begin next to them. */
object CrosswordLayout {
    const val SIZE = 16
    const val CELLS = SIZE * SIZE

    /** At most this many clues in a clue square. */
    const val MAX_CLUES_PER_SQUARE = 2

    /** A run of two or more letters: where it starts, which way it goes, how long it is. */
    class Run(val start: Int, val across: Boolean, val length: Int) {
        val cells: List<Int> get() = List(length) { if (across) start + it else start + it * SIZE }
    }

    /** Every run of two or more letters, the ones going across first (row by row), then the ones going down (column by column). */
    fun runs(isClue: BooleanArray): List<Run> {
        val result = ArrayList<Run>()
        for (row in 0 until SIZE) {
            var column = 0
            while (column < SIZE) {
                if (isClue[row * SIZE + column]) {
                    column++
                    continue
                }
                val first = column
                while (column < SIZE && !isClue[row * SIZE + column]) column++
                if (column - first >= 2) result += Run(row * SIZE + first, true, column - first)
            }
        }
        for (column in 0 until SIZE) {
            var row = 0
            while (row < SIZE) {
                if (isClue[row * SIZE + column]) {
                    row++
                    continue
                }
                val first = row
                while (row < SIZE && !isClue[row * SIZE + column]) row++
                if (row - first >= 2) result += Run(first * SIZE + column, false, row - first)
            }
        }
        return result
    }

    /** The clue squares that may hold the clue of [run]: the one before it in its direction, then the one that turns once; only clue squares. */
    private fun options(run: Run, isClue: BooleanArray): List<Int> {
        val row = run.start / SIZE
        val column = run.start % SIZE
        val straight = if (run.across) row to column - 1 else row - 1 to column
        val bent = if (run.across) row - 1 to column else row to column - 1
        return listOf(straight, bent).filter { (r, c) -> r >= 0 && c >= 0 && isClue[r * SIZE + c] }.map { (r, c) -> r * SIZE + c }
    }

    /**
     * The answers of a layout, each with its clue square, or null if the clues cannot be given: a run with no clue square next to its
     * start, a clue square with no run, or a clue square that would have more than [MAX_CLUES_PER_SQUARE]. A matching gives every clue
     * square a run of its own first; the other runs are then placed by augmenting paths, which move runs between clue squares but never
     * leave one empty. The result is always the same for the same layout.
     */
    fun slots(isClue: BooleanArray): List<CrosswordSlot>? {
        val runs = runs(isClue)
        val options = runs.map { options(it, isClue) }
        if (options.any { it.isEmpty() }) return null
        val clueCells = (0 until CELLS).filter { isClue[it] }
        val runsOf = clueCells.associateWith { cell -> runs.indices.filter { cell in options[it] } }

        val matchRun = arrayOfNulls<Int>(runs.size)
        fun augment(cell: Int, seen: BooleanArray): Boolean {
            for (k in runsOf.getValue(cell)) {
                if (seen[k]) continue
                seen[k] = true
                val other = matchRun[k]
                if (other == null || augment(other, seen)) {
                    matchRun[k] = cell
                    return true
                }
            }
            return false
        }
        for (cell in clueCells) if (!augment(cell, BooleanArray(runs.size))) return null

        val held = HashMap<Int, MutableList<Int>>()
        for (cell in clueCells) held[cell] = ArrayList()
        val at = IntArray(runs.size) { -1 }
        for (k in runs.indices) matchRun[k]?.let {
            held.getValue(it).add(k)
            at[k] = it
        }

        fun place(k: Int, seen: MutableSet<Int>, leaving: Int): Boolean {
            for (cell in options[k]) {
                if (cell == leaving || !seen.add(cell)) continue
                val list = held.getValue(cell)
                if (list.size < MAX_CLUES_PER_SQUARE) {
                    list.add(k)
                    at[k] = cell
                    return true
                }
                for (j in list.toList()) {
                    if (place(j, seen, cell)) {
                        list.remove(j)
                        list.add(k)
                        at[k] = cell
                        return true
                    }
                }
            }
            return false
        }
        for (k in runs.indices) if (matchRun[k] == null && !place(k, HashSet(), -1)) return null

        return runs.indices.map { k ->
            val run = runs[k]
            val row = run.start / SIZE
            val column = run.start % SIZE
            val straight = if (run.across) row * SIZE + column - 1 else (row - 1) * SIZE + column
            CrosswordSlot(at[k], run.across, run.cells, bent = at[k] != straight)
        }
    }

    /** True if every letter square is in an answer, the clues can be given (see [slots]) and no four clue squares make a block. */
    fun isUsable(isClue: BooleanArray): Boolean {
        val slots = slots(isClue) ?: return false
        val covered = BooleanArray(CELLS)
        for (slot in slots) for (cell in slot.cells) covered[cell] = true
        if ((0 until CELLS).any { !isClue[it] && !covered[it] }) return false
        for (row in 0 until SIZE - 1) for (column in 0 until SIZE - 1) {
            val cell = row * SIZE + column
            if (isClue[cell] && isClue[cell + 1] && isClue[cell + SIZE] && isClue[cell + SIZE + 1]) return false
        }
        return true
    }
}

/**
 * A finished crossword board: which squares are clues and the letters of the others. The boards are made offline and kept in
 * [CrosswordBoards]; the clue of every answer comes from [CrosswordBank], found again by the word. A board of the list is saved by
 * its number ([encode]: `b` and the number), which fits the 32 characters of the cloud table.
 */
class CrosswordPuzzle(val language: Language, val squares: String, private val number: Int = -1) {
    val isClue: BooleanArray
    val slots: List<CrosswordSlot>

    /** The answers in the order of [slots]. */
    val words: List<String>

    /** The clue of every answer, in the order of [slots]. */
    val clues: List<String>

    init {
        require(squares.length == CrosswordLayout.CELLS) { "256 squares" }
        isClue = BooleanArray(CrosswordLayout.CELLS) { squares[it] == CLUE }
        require(squares.all { it == CLUE || it in language.alphabet }) { "letters of the language" }
        require(CrosswordLayout.isUsable(isClue)) { "a layout every letter of which is in an answer" }
        slots = CrosswordLayout.slots(isClue)!!
        words = slots.map { slot -> slot.cells.map { squares[it] }.joinToString("") }
        require(words.toSet().size == words.size) { "no answer twice" }
        clues = words.map { CrosswordBank.clueOf(language, it) ?: throw IllegalArgumentException("$it is not in the bank") }
    }

    /** The text that is saved: the number of the board if it is one of the list, else the squares themselves. */
    fun encode(): String = if (number >= 0) "b$number" else squares

    companion object {
        const val CLUE = '#'

        /** The board [number] of the list of [language] (taken modulo the length of the list). */
        fun ofBoard(language: Language, number: Int): CrosswordPuzzle {
            val boards = CrosswordBoards.of(language)
            val index = ((number % boards.size) + boards.size) % boards.size
            return CrosswordPuzzle(language, boards[index], index)
        }

        /** Null if [text] is not something that [encode] wrote, or has an answer the bank does not know. */
        fun decode(language: Language, text: String): CrosswordPuzzle? = try {
            if (text.startsWith("b")) {
                val number = text.drop(1).toIntOrNull()
                val boards = CrosswordBoards.of(language)
                if (number == null || number !in boards.indices) null else CrosswordPuzzle(language, boards[number], number)
            } else {
                CrosswordPuzzle(language, text)
            }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
