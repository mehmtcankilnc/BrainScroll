package com.mehmtcan.brainscroll.ui.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.mehmtcan.brainscroll.game.guide.GuideStep
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.hanoi.HanoiSnapshot
import com.mehmtcan.brainscroll.game.queens.QueensSnapshot
import com.mehmtcan.brainscroll.game.crossword.CrosswordSnapshot
import com.mehmtcan.brainscroll.game.blockout.BlockOutSnapshot
import com.mehmtcan.brainscroll.game.tents.TentsSnapshot
import com.mehmtcan.brainscroll.game.lights.LightsSnapshot
import com.mehmtcan.brainscroll.game.line.LineSnapshot
import com.mehmtcan.brainscroll.game.memory.MemorySnapshot
import com.mehmtcan.brainscroll.game.mines.MinesSnapshot
import com.mehmtcan.brainscroll.game.pipes.PipesSnapshot
import com.mehmtcan.brainscroll.game.slide.SlideSnapshot
import com.mehmtcan.brainscroll.game.sudoku.SudokuSnapshot
import com.mehmtcan.brainscroll.game.target.TargetSnapshot
import com.mehmtcan.brainscroll.game.tubes.TubesSnapshot
import com.mehmtcan.brainscroll.game.wordle.RoundSnapshot
import com.mehmtcan.brainscroll.ui.hanoi.HanoiPage
import com.mehmtcan.brainscroll.ui.queens.QueensPage
import com.mehmtcan.brainscroll.ui.crossword.CrosswordPage
import com.mehmtcan.brainscroll.ui.blockout.BlockOutPage
import com.mehmtcan.brainscroll.ui.tents.TentsPage
import com.mehmtcan.brainscroll.ui.lights.LightsPage
import com.mehmtcan.brainscroll.ui.line.LinePage
import com.mehmtcan.brainscroll.ui.memory.MemoryPage
import com.mehmtcan.brainscroll.ui.mines.MinesPage
import com.mehmtcan.brainscroll.ui.pipes.PipesPage
import com.mehmtcan.brainscroll.ui.slide.SlidePage
import com.mehmtcan.brainscroll.ui.sudoku.SudokuPage
import com.mehmtcan.brainscroll.ui.target.TargetPage
import com.mehmtcan.brainscroll.ui.tubes.TubesPage
import com.mehmtcan.brainscroll.ui.wordle.WordlePage

/**
 * The page of one puzzle, whatever the game: picks the screen of the game and connects its callbacks to [actions].
 * The endless feed and the daily puzzle of the day both use it, each with its own [FeedActions].
 */
@Composable
fun GamePage(item: PageSnapshot, index: Int, actions: FeedActions, favoriteIds: Set<String>, guide: GuideStep? = null) {
    CompositionLocalProvider(LocalGuide provides guide) {
        when (item) {
            is RoundSnapshot -> WordlePage(
                round = item,
                onLetter = { actions.type(index, it) },
                onBackspace = { actions.backspace(index) },
                onEnter = { actions.submit(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is MemorySnapshot -> MemoryPage(
                page = item,
                onFlip = { card -> actions.flip(index, card) },
                onCloseMismatch = { actions.closeMismatch(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is TubesSnapshot -> TubesPage(
                page = item,
                onPour = { from, to -> actions.pour(index, from, to) },
                onUndo = { actions.undo(index) },
                onRestart = { actions.restart(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is MinesSnapshot -> MinesPage(
                page = item,
                onOpen = { cell -> actions.openCell(index, cell) },
                onFlag = { cell -> actions.flagCell(index, cell) },
                onChord = { cell -> actions.chordCell(index, cell) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is SudokuSnapshot -> SudokuPage(
                page = item,
                onPlace = { cell, digit -> actions.placeDigit(index, cell, digit) },
                onNote = { cell, digit -> actions.toggleNote(index, cell, digit) },
                onErase = { cell -> actions.eraseCell(index, cell) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is LightsSnapshot -> LightsPage(
                page = item,
                onTap = { cell -> actions.tapLight(index, cell) },
                onRestart = { actions.restartLights(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is SlideSnapshot -> SlidePage(
                page = item,
                onSlide = { cell -> actions.slideTile(index, cell) },
                onRestart = { actions.restartSlide(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is TargetSnapshot -> TargetPage(
                page = item,
                onCombine = { first, op, second -> actions.combineNumbers(index, first, op, second) },
                preview = { first, op, second -> actions.previewNumbers(index, first, op, second) },
                onUndo = { actions.undoNumbers(index) },
                onRestart = { actions.restartNumbers(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is PipesSnapshot -> PipesPage(
                page = item,
                onTurn = { cell -> actions.turnPipe(index, cell) },
                onRestart = { actions.restartPipes(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is HanoiSnapshot -> HanoiPage(
                page = item,
                onMove = { from, to -> actions.moveDisk(index, from, to) },
                onRestart = { actions.restartHanoi(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is QueensSnapshot -> QueensPage(
                page = item,
                onTap = { cell -> actions.toggleQueen(index, cell) },
                onRestart = { actions.restartQueens(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is TentsSnapshot -> TentsPage(
                page = item,
                onTap = { cell -> actions.tapTent(index, cell) },
                onRestart = { actions.restartTents(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is BlockOutSnapshot -> BlockOutPage(
                page = item,
                onSlide = { piece, to -> actions.slideBlock(index, piece, to) },
                onRestart = { actions.restartBlockOut(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is CrosswordSnapshot -> CrosswordPage(
                page = item,
                onPlace = { cell, letter -> actions.placeLetter(index, cell, letter) },
                onErase = { cell -> actions.eraseLetter(index, cell) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is LineSnapshot -> LinePage(
                page = item,
                onDrag = { cell -> actions.dragLine(index, cell) },
                onTap = { cell -> actions.tapLine(index, cell) },
                onUndo = { actions.undoLine(index) },
                onRestart = { actions.restartLine(index) },
                onHint = { actions.hintLine(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            else -> Unit
        }
    }
}
