package com.mehmtcan.brainscroll.ui.feed

import com.mehmtcan.brainscroll.game.lights.LightsResult
import com.mehmtcan.brainscroll.game.line.LineResult
import com.mehmtcan.brainscroll.game.memory.FlipResult
import com.mehmtcan.brainscroll.game.mines.MinesResult
import com.mehmtcan.brainscroll.game.pipes.PipesResult
import com.mehmtcan.brainscroll.game.slide.SlideResult
import com.mehmtcan.brainscroll.game.sudoku.SudokuResult
import com.mehmtcan.brainscroll.game.target.Op
import com.mehmtcan.brainscroll.game.target.TargetResult
import com.mehmtcan.brainscroll.game.tubes.PourResult

/**
 * What a puzzle page can ask for, in every game. [page] is the index of the page the action is for. The endless feed
 * ([FeedViewModel]) and the daily puzzle of the day ([com.mehmtcan.brainscroll.ui.daily.DailyGameViewModel]) implement it,
 * so the same screens of the games work in both.
 */
interface FeedActions {
    fun type(page: Int, letter: Char)
    fun backspace(page: Int)
    fun submit(page: Int)
    fun flip(page: Int, card: Int): FlipResult
    fun closeMismatch(page: Int)
    fun pour(page: Int, from: Int, to: Int): PourResult
    fun undo(page: Int): Boolean
    fun restart(page: Int): Boolean
    fun openCell(page: Int, cell: Int): MinesResult
    fun flagCell(page: Int, cell: Int): Boolean
    fun chordCell(page: Int, cell: Int): MinesResult
    fun tapLight(page: Int, cell: Int): LightsResult
    fun restartLights(page: Int): Boolean
    fun slideTile(page: Int, cell: Int): SlideResult
    fun restartSlide(page: Int): Boolean
    fun combineNumbers(page: Int, firstId: Int, op: Op, secondId: Int): TargetResult
    fun previewNumbers(page: Int, firstId: Int, op: Op, secondId: Int): Int?
    fun undoNumbers(page: Int): Boolean
    fun restartNumbers(page: Int): Boolean
    fun turnPipe(page: Int, cell: Int): PipesResult
    fun restartPipes(page: Int): Boolean
    fun dragLine(page: Int, cell: Int): LineResult
    fun tapLine(page: Int, cell: Int): LineResult
    fun undoLine(page: Int): Boolean
    fun restartLine(page: Int): Boolean
    fun hintLine(page: Int): List<Int>?
    fun placeDigit(page: Int, cell: Int, digit: Int): SudokuResult
    fun toggleNote(page: Int, cell: Int, digit: Int): SudokuResult
    fun eraseCell(page: Int, cell: Int): SudokuResult
    fun toggleFavorite(resultId: String)
}
