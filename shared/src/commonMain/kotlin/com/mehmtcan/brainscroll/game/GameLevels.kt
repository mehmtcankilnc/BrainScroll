package com.mehmtcan.brainscroll.game

import com.mehmtcan.brainscroll.game.lights.LightsGame
import com.mehmtcan.brainscroll.game.mines.MinesGame
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator
import com.mehmtcan.brainscroll.game.queens.QueensGame
import com.mehmtcan.brainscroll.game.queens.QueensGenerator
import com.mehmtcan.brainscroll.game.slide.SlideGame
import com.mehmtcan.brainscroll.game.arrows.ArrowsGenerator
import com.mehmtcan.brainscroll.game.arrows.ArrowsPuzzle
import com.mehmtcan.brainscroll.game.blockout.BlockOutGenerator
import com.mehmtcan.brainscroll.game.blockout.BlockOutPuzzle
import com.mehmtcan.brainscroll.game.sudoku.JigsawGenerator
import com.mehmtcan.brainscroll.game.sudoku.SudokuGenerator
import com.mehmtcan.brainscroll.game.tents.TentsGenerator
import com.mehmtcan.brainscroll.game.tents.TentsPuzzle
import com.mehmtcan.brainscroll.game.tubes.TubesGame

/**
 * What each level means for the games whose boards are made from a few numbers. Medium is always the
 * value the game had before levels existed. The values are a first guess, tuned on a device (phase 7 of the plan): they are all
 * here so that tuning is one file.
 *
 * Every saved board is written out in full or with these numbers in its answer, so changing a value here does not change a game
 * that was saved earlier.
 */
object GameLevels {
    /** [attempts] guesses, and in [hardMode] every hint found has to be used (see [com.mehmtcan.brainscroll.game.wordle.WordleGame]). */
    data class WordShape(val attempts: Int, val hardMode: Boolean)

    fun word(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> WordShape(7, false)
        Difficulty.Medium -> WordShape(6, false)
        Difficulty.Hard -> WordShape(5, true)
    }

    data class MinesShape(val rows: Int, val cols: Int, val mines: Int)

    fun mines(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> MinesShape(8, 8, 8)
        Difficulty.Medium -> MinesShape(MinesGame.DEFAULT_ROWS, MinesGame.DEFAULT_COLS, MinesGame.DEFAULT_MINES)
        Difficulty.Hard -> MinesShape(9, 9, 18)
    }

    data class LightsShape(val size: Int, val taps: Int)

    fun lights(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> LightsShape(4, 4)
        Difficulty.Medium -> LightsShape(LightsGame.DEFAULT_SIZE, LightsGame.DEFAULT_TAPS)
        Difficulty.Hard -> LightsShape(6, 14)
    }

    data class SlideShape(val cols: Int, val rows: Int, val scramble: Int)

    fun slide(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> SlideShape(3, 3, 40)
        Difficulty.Medium -> SlideShape(SlideGame.DEFAULT_COLS, SlideGame.DEFAULT_ROWS, SlideGame.DEFAULT_SCRAMBLE)
        Difficulty.Hard -> SlideShape(4, 4, 120)
    }

    /**
     * The saved answer of a pipes board is its tiles as hex digits and the table keeps 32 characters, so 5 by 5 (25 digits) is the
     * biggest board there can be: the hard level asks for more taps instead of more tiles.
     */
    data class PipesShape(val size: Int, val minPar: Int)

    fun pipes(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> PipesShape(4, 8)
        Difficulty.Medium -> PipesShape(PipesGenerator.SIZE, PipesGenerator.DEFAULT_MIN_PAR)
        Difficulty.Hard -> PipesShape(PipesGenerator.SIZE, 40)
    }

    data class QueensShape(val size: Int, val locked: Int)

    fun queens(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> QueensShape(QueensGame.DEFAULT_SIZE, 3)
        Difficulty.Medium -> QueensShape(QueensGame.DEFAULT_SIZE, QueensGenerator.DEFAULT_LOCKED)
        Difficulty.Hard -> QueensShape(7, 1)
    }

    /** A board of axis sum: the size, the biggest number on it, and the share of the numbers that have to be erased. */
    data class AxisSumShape(val size: Int, val maxDigit: Int, val eraseShare: Double)

    fun axisSum(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> AxisSumShape(4, 6, 0.38)
        Difficulty.Medium -> AxisSumShape(5, 9, 0.4)
        Difficulty.Hard -> AxisSumShape(6, 9, 0.42)
    }

    /** A board of ball slide: the size, how many walls it has (at least, at most), and how many slides win it at the fewest. */
    data class BallSlideShape(val size: Int, val minWalls: Int, val maxWalls: Int, val minPar: Int, val maxPar: Int)

    fun ballSlide(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> BallSlideShape(4, 2, 4, 3, 5)
        Difficulty.Medium -> BallSlideShape(5, 4, 7, 5, 8)
        Difficulty.Hard -> BallSlideShape(6, 6, 10, 8, 12)
    }

    /** A board of color flow: the size and how many colors (at least, at most). */
    data class FlowShape(val size: Int, val minColors: Int, val maxColors: Int)

    fun colorFlow(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> FlowShape(5, 4, 5)
        Difficulty.Medium -> FlowShape(6, 5, 6)
        Difficulty.Hard -> FlowShape(7, 6, 7)
    }

    /** A board of winding arrows: the size, how many arrows (at least, at most), how long each is, and how many may leave at the start. */
    data class ArrowsShape(
        val size: Int,
        val minArrows: Int,
        val maxArrows: Int,
        val minLength: Int,
        val maxLength: Int,
        val minFree: Int,
        val maxFree: Int,
    )

    fun arrows(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> ArrowsShape(5, 5, 7, 2, 4, 2, 4)
        Difficulty.Medium -> ArrowsShape(ArrowsPuzzle.DEFAULT_SIZE, 7, 9, 3, 5, 2, 4)
        Difficulty.Hard -> ArrowsShape(6, 8, 10, 3, 6, 1, 3)
    }

    /** Given cells of a classic sudoku: every board is still finished by singles alone, so the guide and "no guessing" hold at every level. */
    fun sudokuClues(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> 44
        Difficulty.Medium -> SudokuGenerator.DEFAULT_CLUES
        Difficulty.Hard -> 24
    }

    fun jigsawClues(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> 36
        Difficulty.Medium -> JigsawGenerator.DEFAULT_CLUES
        Difficulty.Hard -> 22
    }

    /** [byLogic]: true is a board plain reasoning finishes, false one that needs more than that, null any (see [TentsGenerator.generate]). */
    data class TentsShape(val size: Int, val tents: Int, val byLogic: Boolean?)

    fun tents(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> TentsShape(5, 6, true)
        Difficulty.Medium -> TentsShape(TentsPuzzle.DEFAULT_SIZE, TentsGenerator.DEFAULT_TENTS, null)
        Difficulty.Hard -> TentsShape(7, 12, false)
    }

    /**
     * Block out stays 5 by 5 at every level: a 6 by 6 board takes the search three quarters of a second to make on a computer and
     * its saved text (36 cells) would not fit the 32 characters of the table. A higher fewest-moves limit makes it hard instead.
     */
    data class BlockOutShape(val size: Int, val minPar: Int)

    fun blockOut(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> BlockOutShape(BlockOutPuzzle.DEFAULT_SIZE, 5)
        Difficulty.Medium -> BlockOutShape(BlockOutPuzzle.DEFAULT_SIZE, BlockOutGenerator.DEFAULT_MIN_PAR)
        Difficulty.Hard -> BlockOutShape(BlockOutPuzzle.DEFAULT_SIZE, 13)
    }

    /**
     * The saved answer of a tubes board is its tubes written out and the table keeps 32 characters: six colors in four-unit tubes
     * (with the two empty ones) are 31, a seventh would not fit.
     */
    data class TubesShape(val colors: Int, val empty: Int)

    fun tubes(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> TubesShape(4, 2)
        Difficulty.Medium -> TubesShape(TubesGame.COLORS, TubesGame.EMPTY_TUBES)
        Difficulty.Hard -> TubesShape(6, 2)
    }

    /**
     * A matchstick sum: whether the result has two digits (`A + B = CD`), and whether the stick that puts it right only moves inside
     * one symbol (a 6 into a 9) instead of going to another one.
     */
    data class MatchShape(val twoDigitResult: Boolean, val sameSymbol: Boolean)

    fun matchsticks(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> MatchShape(twoDigitResult = false, sameSymbol = true)
        Difficulty.Medium -> MatchShape(twoDigitResult = false, sameSymbol = false)
        Difficulty.Hard -> MatchShape(twoDigitResult = true, sameSymbol = false)
    }

    /**
     * A board of laser mirrors: the size, how many mirrors stand on the beam's way, how many more mirrors are in the way of nothing,
     * how many targets there are, and how many mirrors at least have to be turned to win it from the start.
     */
    data class LaserShape(val size: Int, val pathMirrors: Int, val decoys: Int, val targets: Int, val minTurns: Int)

    fun lasers(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> LaserShape(5, 3, 1, 2, 2)
        Difficulty.Medium -> LaserShape(6, 5, 2, 3, 3)
        Difficulty.Hard -> LaserShape(7, 7, 3, 4, 4)
    }

    /** The flash grid: the side of the square, the tiles of the first round, and the tiles of the last (clearing it wins the run). */
    data class FlashShape(val size: Int, val startTiles: Int, val maxTiles: Int)

    fun flashGrid(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> FlashShape(3, 3, 6)
        Difficulty.Medium -> FlashShape(4, 3, 9)
        Difficulty.Hard -> FlashShape(5, 4, 13)
    }

    /** Rectangles: the side of the square, the biggest rectangle, and the fewest rectangles (so no board is cut into a few big ones). */
    data class RectShape(val size: Int, val maxArea: Int, val minPieces: Int)

    fun rectangles(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> RectShape(6, 8, 8)
        Difficulty.Medium -> RectShape(7, 10, 10)
        Difficulty.Hard -> RectShape(8, 12, 12)
    }

    /** A code: how many places, how many colours, and whether a colour may be in it twice. */
    data class CodeShape(val length: Int, val colors: Int, val repeats: Boolean)

    fun codeBreaker(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> CodeShape(4, 5, false)
        Difficulty.Medium -> CodeShape(4, 6, false)
        Difficulty.Hard -> CodeShape(5, 8, true)
    }

    /** A tangle: how many dots, how many lines (at least; more are added to join the dots), and how many pairs of lines cross at the start (at least). */
    data class UntangleShape(val nodes: Int, val edges: Int, val minCrossings: Int)

    fun untangle(difficulty: Difficulty) = when (difficulty) {
        Difficulty.Easy -> UntangleShape(7, 11, 4)
        Difficulty.Medium -> UntangleShape(10, 16, 7)
        Difficulty.Hard -> UntangleShape(14, 23, 11)
    }

    /**
     * A board of fruit equations: how many kinds of fruit, the biggest value of a fruit, which kinds of line there are, how many lines at least
     * are not plain sums, how many at least are powers, which forms the last line may have, the biggest total of any line (and answer), and the
     * smallest answer.
     */
    data class FruitShape(
        val fruits: Int,
        val maxValue: Int,
        val kinds: Set<com.mehmtcan.brainscroll.game.fruitmath.FruitLineKind>,
        val special: Int,
        val minPowers: Int,
        val questions: Set<com.mehmtcan.brainscroll.game.fruitmath.FruitQuestionKind>,
        val maxTotal: Int,
        val minAnswer: Int,
    )

    fun fruitMath(difficulty: Difficulty): FruitShape {
        val sum = com.mehmtcan.brainscroll.game.fruitmath.FruitLineKind.Sum
        val product = com.mehmtcan.brainscroll.game.fruitmath.FruitLineKind.Product
        val quotient = com.mehmtcan.brainscroll.game.fruitmath.FruitLineKind.Quotient
        val squarePlus = com.mehmtcan.brainscroll.game.fruitmath.FruitLineKind.SquarePlus
        val squareOnly = com.mehmtcan.brainscroll.game.fruitmath.FruitLineKind.SquareOnly
        val powerOf = com.mehmtcan.brainscroll.game.fruitmath.FruitLineKind.PowerOf
        val powerBase = com.mehmtcan.brainscroll.game.fruitmath.FruitLineKind.PowerBase
        val sumAll = com.mehmtcan.brainscroll.game.fruitmath.FruitQuestionKind.SumAll
        val productPlus = com.mehmtcan.brainscroll.game.fruitmath.FruitQuestionKind.ProductPlus
        val powerOnly = com.mehmtcan.brainscroll.game.fruitmath.FruitQuestionKind.PowerOnly
        return when (difficulty) {
            Difficulty.Easy -> FruitShape(3, 7, setOf(sum), 0, 0, setOf(sumAll), 40, 8)
            Difficulty.Medium -> FruitShape(4, 6, setOf(sum, product), 1, 0, setOf(sumAll), 40, 10)
            Difficulty.Hard -> FruitShape(4, 9, setOf(sum, product, quotient, squarePlus, squareOnly, powerOf, powerBase), 2, 1, setOf(sumAll, productPlus, powerOnly), 99, 12)
        }
    }
}
