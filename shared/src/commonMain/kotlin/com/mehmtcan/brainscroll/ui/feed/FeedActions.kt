package com.mehmtcan.brainscroll.ui.feed

import com.mehmtcan.brainscroll.game.hanoi.HanoiResult
import com.mehmtcan.brainscroll.game.queens.QueensResult
import com.mehmtcan.brainscroll.game.arrows.ArrowsResult
import com.mehmtcan.brainscroll.game.axissum.AxisSumResult
import com.mehmtcan.brainscroll.game.ballslide.BallSlideResult
import com.mehmtcan.brainscroll.game.colorflow.FlowResult
import com.mehmtcan.brainscroll.game.fruitmath.FruitResult
import com.mehmtcan.brainscroll.game.lasers.LaserResult
import com.mehmtcan.brainscroll.game.untangle.UntangleResult
import com.mehmtcan.brainscroll.game.codebreaker.CodeResult
import com.mehmtcan.brainscroll.game.fences.FenceResult
import com.mehmtcan.brainscroll.game.crosssum.CrossResult
import com.mehmtcan.brainscroll.game.mirror.MirrorResult
import com.mehmtcan.brainscroll.game.oddone.OddResult
import com.mehmtcan.brainscroll.game.greaterless.GlResult
import com.mehmtcan.brainscroll.game.lamps.LampResult
import com.mehmtcan.brainscroll.game.lonenumber.LoneResult
import com.mehmtcan.brainscroll.game.zerosones.BinResult
import com.mehmtcan.brainscroll.game.rectangles.Rect
import com.mehmtcan.brainscroll.game.colorseq.ColorSeqResult
import com.mehmtcan.brainscroll.game.cubefold.CubeResult
import com.mehmtcan.brainscroll.game.flashgrid.FlashResult
import com.mehmtcan.brainscroll.game.rectangles.RectResult
import com.mehmtcan.brainscroll.game.matchsticks.MatchMove
import com.mehmtcan.brainscroll.game.matchsticks.MatchResult
import com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsResult
import com.mehmtcan.brainscroll.game.nonogram.NonogramResult
import com.mehmtcan.brainscroll.game.blockout.BlockOutResult
import com.mehmtcan.brainscroll.game.tents.TentsResult
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
    fun moveDisk(page: Int, from: Int, to: Int): HanoiResult
    fun restartHanoi(page: Int): Boolean
    fun toggleQueen(page: Int, cell: Int): QueensResult
    fun restartQueens(page: Int): Boolean
    fun tapTent(page: Int, cell: Int): TentsResult
    fun restartTents(page: Int): Boolean
    fun slideBlock(page: Int, piece: Int, to: Int): BlockOutResult
    fun restartBlockOut(page: Int): Boolean
    fun tapArrow(page: Int, cell: Int): ArrowsResult
    fun restartArrows(page: Int): Boolean
    fun tapAxisSum(page: Int, cell: Int): AxisSumResult
    fun restartAxisSum(page: Int): Boolean
    fun slideBall(page: Int, dir: Int): BallSlideResult
    fun undoBall(page: Int): Boolean
    fun restartBall(page: Int): Boolean
    fun startFlow(page: Int, cell: Int): Boolean
    fun extendFlow(page: Int, cell: Int): Boolean
    fun endFlow(page: Int): FlowResult
    fun restartFlow(page: Int): Boolean
    fun moveStick(page: Int, move: MatchMove): MatchResult
    fun undoStick(page: Int): Boolean
    fun turnMirror(page: Int, cell: Int): LaserResult
    fun restartLasers(page: Int): Boolean
    fun moveNode(page: Int, node: Int, x: Int, y: Int): Boolean
    fun dropNode(page: Int): UntangleResult
    fun restartUntangle(page: Int): Boolean
    fun submitFruit(page: Int, answer: Int): FruitResult
    fun submitCode(page: Int, guess: List<Int>): CodeResult
    fun placeRect(page: Int, rect: Rect): RectResult
    fun tapFlashTile(page: Int, tile: Int): FlashResult
    fun pickCube(page: Int, pick: Int): CubeResult
    fun startColorSeq(page: Int): Boolean
    fun restartColorSeq(page: Int): Boolean
    fun tapColorButton(page: Int, button: Int): ColorSeqResult
    fun startFlash(page: Int): Boolean
    fun restartFlash(page: Int): Boolean
    fun removeRect(page: Int, column: Int, row: Int): Boolean
    fun restartRects(page: Int): Boolean
    fun tapLoneCell(page: Int, cell: Int): LoneResult
    fun restartLone(page: Int): Boolean
    fun tapBinCell(page: Int, cell: Int): BinResult
    fun restartBin(page: Int): Boolean
    fun tapLampCell(page: Int, cell: Int): LampResult
    fun restartLamps(page: Int): Boolean
    fun putGlNumber(page: Int, cell: Int, value: Int): GlResult
    fun restartGl(page: Int): Boolean
    fun tapFenceEdge(page: Int, edge: Int): FenceResult
    fun restartFences(page: Int): Boolean
    fun tapMirrorCell(page: Int, cell: Int): MirrorResult
    fun restartMirror(page: Int): Boolean
    fun pickOdd(page: Int, pick: Int): OddResult
    fun putCrossDigit(page: Int, cell: Int, digit: Int): CrossResult
    fun restartCross(page: Int): Boolean
    fun tapTopicWord(page: Int, word: Int): TopicGroupsResult
    fun shuffleTopicWords(page: Int): Boolean
    fun clearTopicSelection(page: Int): Boolean
    fun paintNonogram(page: Int, from: Int, to: Int, value: Int): NonogramResult
    fun restartNonogram(page: Int): Boolean
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
