package com.mehmtcan.brainscroll.game.crossword

enum class CrosswordStatus { Playing, Won }

/** What a move did. */
enum class CrosswordResult {
    /** Nothing happened: a clue square, a letter that is right already, the same wrong letter again, or the game is over. Not counted. */
    Ignored,

    /** A letter was put and it is the right one. */
    Correct,

    /** A letter was put and it is wrong: it counts as a mistake. */
    Mistake,

    /** A wrong letter was taken back. */
    Erased,
}

/**
 * One clue as the UI shows it: the square it is in, which way its answer runs, the text, and the cells of the answer. When [bent] is
 * true the arrow turns once: the answer starts below the clue square (going across) or to its right (going down).
 */
data class CrosswordClue(val cell: Int, val across: Boolean, val text: String, val cells: List<Int>, val bent: Boolean = false)

/** The picture of the board for the UI. Cells are numbered row by row from the top left. */
data class CrosswordState(
    val size: Int,
    /** True for the squares that hold clues. */
    val isClue: List<Boolean>,
    /** The letter in every square (a space if there is none, and for a clue square). */
    val letters: List<Char>,
    /** The letter squares that hold a wrong letter. */
    val wrong: Set<Int>,
    /** The squares whose letter is right (and so final). */
    val correct: Set<Int>,
    val clues: List<CrosswordClue>,
    /** Letters put so far (erasing is not a move). */
    val moves: Int,
    /** Wrong letters put so far. There is no limit: they only count, and the puzzle goes on. */
    val mistakes: Int,
    val status: CrosswordStatus,
)

/**
 * The rules of the crossword, free of UI and time (docs/yeni-oyunlar-plan.md). It works as the sudoku does: a letter that is not
 * the one of the answer is a mistake, counted and shown, and the puzzle goes on; a right letter is final, so only wrong letters can be
 * taken back. The game is won when every letter square holds its right letter. There is no losing.
 */
class CrosswordGame(private val puzzle: CrosswordPuzzle) {
    private val size = CrosswordLayout.SIZE
    private val letters = CharArray(CrosswordLayout.CELLS) { ' ' }
    private var moves = 0
    private var mistakes = 0

    private fun solution(cell: Int): Char = puzzle.squares[cell]

    val isFinished: Boolean get() = (0 until CrosswordLayout.CELLS).all { puzzle.isClue[it] || letters[it] == solution(it) }

    /** The letter that belongs in each square, a space for a clue square. Only the guide may use it. */
    internal val solution: List<Char> get() = List(CrosswordLayout.CELLS) { if (puzzle.isClue[it]) ' ' else solution(it) }

    val state: CrosswordState
        get() = CrosswordState(
            size = size,
            isClue = puzzle.isClue.toList(),
            letters = letters.toList(),
            wrong = (0 until CrosswordLayout.CELLS).filterTo(HashSet()) { letters[it] != ' ' && letters[it] != solution(it) },
            correct = (0 until CrosswordLayout.CELLS).filterTo(HashSet()) { !puzzle.isClue[it] && letters[it] == solution(it) },
            clues = puzzle.slots.indices.map { CrosswordClue(puzzle.slots[it].clueCell, puzzle.slots[it].across, puzzle.clues[it], puzzle.slots[it].cells, puzzle.slots[it].bent) },
            moves = moves,
            mistakes = mistakes,
            status = if (isFinished) CrosswordStatus.Won else CrosswordStatus.Playing,
        )

    /** True if [cell] can be changed: it is a letter square that does not hold its right letter yet, and the game goes on. */
    fun isEditable(cell: Int): Boolean =
        !isFinished && cell in 0 until CrosswordLayout.CELLS && !puzzle.isClue[cell] && letters[cell] != solution(cell)

    /** Puts [letter] (a capital of the language) in [cell]. A wrong one counts as a mistake. */
    fun place(cell: Int, letter: Char): CrosswordResult {
        if (!isEditable(cell) || letter !in puzzle.language.alphabet || letters[cell] == letter) return CrosswordResult.Ignored
        letters[cell] = letter
        moves++
        if (letter == solution(cell)) return CrosswordResult.Correct
        mistakes++
        return CrosswordResult.Mistake
    }

    /** Takes back a wrong letter. */
    fun erase(cell: Int): CrosswordResult {
        if (!isEditable(cell) || letters[cell] == ' ') return CrosswordResult.Ignored
        letters[cell] = ' '
        return CrosswordResult.Erased
    }
}
