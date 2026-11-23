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

    /** The cell can only hold one digit: its row, column and region already have all the others (the jigsaw board). */
    JigsawOnlyDigit,

    /** The digit can only go in this cell of its row, column or region (the jigsaw board). */
    JigsawOnlyPlace,

    /** The digit that belongs in the cell. */
    SudokuSolution,

    /** Turn the pipe this many times. */
    PipesTurn,

    /** Move the top disk of the first tower onto the second: it is the first step of the shortest way. */
    HanoiMove,

    /** Put a queen on this cell: it is part of a way to finish the board. */
    QueensPlace,

    /** Take away the queen on this cell: it is not part of any way to finish the board. */
    QueensRemove,

    /** Put a tent on this cell: the trees and the numbers leave no other place for it. */
    TentsPlace,

    /** Take away the tent on this cell: it cannot be part of a finished board. */
    TentsRemove,

    /** Slide the piece this many cells in this direction: it is the first move of the shortest way. */
    BlockOutSlide,

    /** This arrow can leave the board now. */
    ArrowsEscape,

    /** Draw the path of this color (the number, counted from 0) along these cells: they are the next ones of a way that wins. */
    FlowRoute,

    /** Slide the ball this way (the number is the direction, see BallDir): it is the first slide of the shortest way. */
    BallSlideMove,

    /** No slide paints the rest of the board from here: undo or start again. */
    BallSlideStuck,

    /** Erase the number on this cell: it is not part of the sums. */
    AxisSumErase,

    /** Bring back the number on this cell: it is part of the sums. */
    AxisSumKeep,

    /** These four words are a group: the text is its topic. */
    TopicGroupsGroup,

    /** Fill this cell: the numbers leave no other possibility for it. */
    NonogramFill,

    /** Empty this filled cell: it is not part of the picture. */
    NonogramEmpty,

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

    /** Take the stick from the first place (the first number is its slot) and lay it on the second: it is the way to put the sum right. */
    MatchstickMove,

    /** The stick is on a wrong place: put it back first. */
    MatchstickUndo,

    /** Turn the mirror on this cell: it is on the way to the fewest turns that light every target. */
    LaserTurn,

    /** Drag this dot (the first number is the dot) to the place the numbers say (x then y, in units of the board): it is on the way to a drawing with no crossing. */
    UntangleMove,

    /** The values of the fruits: the first numbers are the fruits and the next ones what each is worth. */
    FruitValues,

    /** A code that fits every clue so far: the numbers are its colours, the first to the last place. */
    CodeNext,

    /** Draw this rectangle (the cells are its column, row, width and height; the first number is the number it holds). */
    RectPlace,

    /** This rectangle is not part of the answer: take it off (the cells are its column, row, width and height). */
    RectRemove,

    /** Shade this cell (the cells hold its number: row * size + column): the number repeats and the answer has it shaded. */
    LoneShade,

    /** The mark on this cell (the cells hold its number: row * size + column) is not right: tap it until it is empty. */
    LoneClear,

    /** Put the digit (the number) in this cell (the cells hold its number: row * size + column): the rules leave no other digit for it. */
    BinFill,

    /** The digit in this cell (the cells hold its number: row * size + column) is not right: tap it until the cell is empty. */
    BinClear,

    /** Put a lamp on this cell (the cells hold its number: row * size + column): the rules leave no other way to light what it lights. */
    LampPlace,

    /** The mark on this cell (the cells hold its number: row * size + column) is not right: tap it until it is empty. */
    LampClear,

    /** Put the number (the number) in this cell (the cells hold its number: row * size + column): the row, the column and the signs leave no other number for it. */
    GlFill,

    /** The number in this cell (the cells hold its number: row * size + column) is not right: tap it until the cell is empty. */
    GlClear,

    /** Draw a line on this edge (the cells hold its number, see FenceGeometry): the numbers and the dots leave no other way. */
    FenceLine,

    /** The mark on this edge (the cells hold its number) is not right: tap it until it is empty. */
    FenceClear,

    /** Paint this cell: its partner on the other side of the axis is painted (the partner is in [GuideStep.evidence]). */
    MirrorPaint,

    /** Empty this cell: its partner on the other side of the axis is empty (the partner is in [GuideStep.evidence]). */
    MirrorEmpty,

    /** Two of the numbers follow the same rule (the rule is [GuideStep.rule], the index of the rule): the odd one is not one of them. */
    OddRule,

    /** Put this digit ([GuideStep.numbers]) in this cell: the sums leave no other (the cells of the blocks that say so are in [GuideStep.evidence], [GuideStep.rule] says which way). */
    CrossDigit,

    /** The digit in this cell is not right: take it out. */
    CrossClear,

    /** The tiles to find in this round (the cells) light up again. */
    FlashPattern,

    /** The lights of this round (the cells are the buttons, in order) are played again. */
    SeqReplay,

    /** This cube (the cell is the option) cannot be the answer: it shows two faces that are opposite faces of the net (they are in [GuideStep.evidence]). */
    CubeOpposite,

    /** This cube (the cell is the option) cannot be the answer: its three faces are right but turn the wrong way round (the three are in [GuideStep.evidence]). */
    CubeMirror,
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
    /**
     * The things that make [cells] the answer (the numbers around a mine, the digits in a row, the sign next to a cell), in the same
     * numbering as [cells]. The page rings them softly, so "because of these" can be seen and not only read.
     */
    val evidence: List<Int> = emptyList(),
    /** Which way the same [reason] was reached (which rule of the game fired), so the sentence can say it. 0 is the plain one. */
    val rule: Int = 0,
)

/** The work of finding a step, with everything it needs copied, so it can run anywhere. Null: there is nothing to say. */
typealias GuideTask = () -> GuideStep?
