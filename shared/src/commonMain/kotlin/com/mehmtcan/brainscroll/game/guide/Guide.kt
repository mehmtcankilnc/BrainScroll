package com.mehmtcan.brainscroll.game.guide

/**
 * What the guide says about the next move. The screen turns the reason into a sentence in the language of the app; the
 * numbers and the text fill the blanks of that sentence.
 */
enum class GuideReason {
    /** Nothing is open yet: the middle is a good place to start. */
    MinesStart,

    /** The numbers around the cell prove it holds no mine: open it. */
    MinesSafe,

    /** The numbers around the cell prove it holds a mine: flag it. */
    MinesMine,

    /** No move is certain right now. The flags may be wrong. */
    MinesNone,

    /** A digit in the cell is wrong: take it out first. */
    SudokuErase,

    /** The cell can only hold one digit: the row, the column and the box already have all the others. */
    SudokuOnlyDigit,

    /** The digit can only go in this cell of its row, column or box. */
    SudokuOnlyPlace,

    /** The digit that belongs in the cell. */
    SudokuSolution,

    /** Turn the pipe this many times. */
    PipesTurn,

    /** Move the top disk of the first tower onto the second: it is the first step of the shortest way. */
    HanoiMove,

    /** Tap the light: it is part of the shortest way to put all of them out. */
    LightsTap,

    /** Slide this tile into the empty cell: it is the first step of the shortest way. */
    SlideTile,

    /** Pour the first tube into the second one. */
    TubesPour,

    /** The tubes are stuck: no pour finishes them from here. Undo or start again. */
    TubesStuck,

    /** The numbers on the table can no longer make the target: start the puzzle over. */
    TargetStuck,

    /** Combine the two numbers with the operation. */
    TargetCombine,

    /** One card is face up: this is the card it matches. */
    MemoryPartner,

    /** These two cards match. */
    MemoryPair,

    /** The letter at this place of the word. */
    WordLetter,
}

/**
 * One step of the guide. [cells] are the things to point at, in the numbering of the game's page: cells of the board,
 * cards, tubes, the ids of the numbers on the table. [numbers] and [text] fill the blanks of the sentence of [reason].
 */
data class GuideStep(
    val reason: GuideReason,
    val cells: List<Int> = emptyList(),
    val numbers: List<Int> = emptyList(),
    val text: String = "",
)

/** The work of finding a step, with everything it needs copied, so it can run anywhere. Null: there is nothing to say. */
typealias GuideTask = () -> GuideStep?
