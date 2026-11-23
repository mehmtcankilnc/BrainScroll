package com.mehmtcan.brainscroll.ui.feed

import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.sound.LocalSoundEffects
import com.mehmtcan.brainscroll.ui.sound.rememberSoundHaptics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.mehmtcan.brainscroll.game.guide.GuideStep
import com.mehmtcan.brainscroll.game.PageSnapshot
import com.mehmtcan.brainscroll.game.hanoi.HanoiSnapshot
import com.mehmtcan.brainscroll.game.queens.QueensSnapshot
import com.mehmtcan.brainscroll.game.arrows.ArrowsSnapshot
import com.mehmtcan.brainscroll.game.axissum.AxisSumSnapshot
import com.mehmtcan.brainscroll.game.ballslide.BallSlideSnapshot
import com.mehmtcan.brainscroll.game.colorflow.FlowSnapshot
import com.mehmtcan.brainscroll.game.lasers.LaserSnapshot
import com.mehmtcan.brainscroll.game.fruitmath.FruitSnapshot
import com.mehmtcan.brainscroll.ui.fruitmath.FruitMathPage
import com.mehmtcan.brainscroll.game.untangle.UntangleSnapshot
import com.mehmtcan.brainscroll.game.codebreaker.CodeSnapshot
import com.mehmtcan.brainscroll.ui.codebreaker.CodeBreakerPage
import com.mehmtcan.brainscroll.game.PendingSnapshot
import com.mehmtcan.brainscroll.game.fences.FenceSnapshot
import com.mehmtcan.brainscroll.game.crosssum.CrossSnapshot
import com.mehmtcan.brainscroll.game.mirror.MirrorSnapshot
import com.mehmtcan.brainscroll.ui.crosssum.CrossSumPage
import com.mehmtcan.brainscroll.game.oddone.OddSnapshot
import com.mehmtcan.brainscroll.ui.oddone.OddOnePage
import com.mehmtcan.brainscroll.ui.mirror.MirrorPage
import com.mehmtcan.brainscroll.game.greaterless.GlSnapshot
import com.mehmtcan.brainscroll.ui.fences.FencesPage
import com.mehmtcan.brainscroll.game.lamps.LampSnapshot
import com.mehmtcan.brainscroll.ui.greaterless.GreaterLessPage
import com.mehmtcan.brainscroll.game.lonenumber.LoneSnapshot
import com.mehmtcan.brainscroll.ui.lamps.LampsPage
import com.mehmtcan.brainscroll.game.zerosones.BinSnapshot
import com.mehmtcan.brainscroll.ui.zerosones.ZerosOnesPage
import com.mehmtcan.brainscroll.game.rectangles.RectSnapshot
import com.mehmtcan.brainscroll.ui.lonenumber.LoneNumberPage
import com.mehmtcan.brainscroll.ui.rectangles.RectanglesPage
import com.mehmtcan.brainscroll.game.flashgrid.FlashSnapshot
import com.mehmtcan.brainscroll.game.colorseq.ColorSeqSnapshot
import com.mehmtcan.brainscroll.game.cubefold.CubeFoldSnapshot
import com.mehmtcan.brainscroll.ui.cubefold.CubeFoldPage
import com.mehmtcan.brainscroll.ui.colorseq.ColorSeqPage
import com.mehmtcan.brainscroll.ui.flashgrid.FlashGridPage
import com.mehmtcan.brainscroll.ui.untangle.UntanglePage
import com.mehmtcan.brainscroll.ui.lasers.LasersPage
import com.mehmtcan.brainscroll.game.matchsticks.MatchSnapshot
import com.mehmtcan.brainscroll.ui.matchsticks.MatchsticksPage
import com.mehmtcan.brainscroll.game.topicgroups.TopicGroupsSnapshot
import com.mehmtcan.brainscroll.ui.topicgroups.TopicGroupsPage
import com.mehmtcan.brainscroll.ui.colorflow.ColorFlowPage
import com.mehmtcan.brainscroll.ui.ballslide.BallSlidePage
import com.mehmtcan.brainscroll.ui.axissum.AxisSumPage
import com.mehmtcan.brainscroll.game.nonogram.NonogramSnapshot
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
import com.mehmtcan.brainscroll.ui.arrows.ArrowsPage
import com.mehmtcan.brainscroll.ui.nonogram.NonogramPage
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
import com.mehmtcan.brainscroll.ui.wordle.LanguageSwitch
import com.mehmtcan.brainscroll.ui.wordle.WordlePage

/**
 * The page of one puzzle, whatever the game: picks the screen of the game and connects its callbacks to [actions].
 * The endless feed and the daily puzzle of the day both use it, each with its own [FeedActions].
 */
@Composable
fun GamePage(item: PageSnapshot, index: Int, actions: FeedActions, favoriteIds: Set<String>, guide: GuideStep? = null, languageSwitch: LanguageSwitch? = null) {
    // The sounds of the games come with their haptics: inside a game page only, never in the menus and the tabs.
    val haptics = rememberSoundHaptics(LocalHaptics.current, LocalSoundEffects.current)
    CompositionLocalProvider(LocalGuide provides guide, LocalHaptics provides haptics) {
        when (item) {
            // The board is still being made in the background: a skeleton takes its place until it is there.
            is PendingSnapshot -> GeneratingPage(item.kind)
            is RoundSnapshot -> WordlePage(
                round = item,
                onLetter = { actions.type(index, it) },
                onBackspace = { actions.backspace(index) },
                onEnter = { actions.submit(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
                languageSwitch = languageSwitch,
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
            is ArrowsSnapshot -> ArrowsPage(
                page = item,
                onTap = { cell -> actions.tapArrow(index, cell) },
                onRestart = { actions.restartArrows(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is FlowSnapshot -> ColorFlowPage(
                page = item,
                onStart = { cell -> actions.startFlow(index, cell) },
                onExtend = { cell -> actions.extendFlow(index, cell) },
                onEnd = { actions.endFlow(index) },
                onRestart = { actions.restartFlow(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is FruitSnapshot -> FruitMathPage(
                page = item,
                onSubmit = { answer -> actions.submitFruit(index, answer) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is CubeFoldSnapshot -> CubeFoldPage(
                page = item,
                onPick = { pick -> actions.pickCube(index, pick) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is ColorSeqSnapshot -> ColorSeqPage(
                page = item,
                onTap = { button -> actions.tapColorButton(index, button) },
                onStart = { actions.startColorSeq(index) },
                onRestart = { actions.restartColorSeq(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is FlashSnapshot -> FlashGridPage(
                page = item,
                onTap = { tile -> actions.tapFlashTile(index, tile) },
                onStart = { actions.startFlash(index) },
                onRestart = { actions.restartFlash(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is RectSnapshot -> RectanglesPage(
                page = item,
                onPlace = { rect -> actions.placeRect(index, rect) },
                onRemove = { column, row -> actions.removeRect(index, column, row) },
                onRestart = { actions.restartRects(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is FenceSnapshot -> FencesPage(
                page = item,
                onTap = { edge -> actions.tapFenceEdge(index, edge) },
                onRestart = { actions.restartFences(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is CrossSnapshot -> CrossSumPage(
                page = item,
                onPut = { cell, digit -> actions.putCrossDigit(index, cell, digit) },
                onRestart = { actions.restartCross(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is OddSnapshot -> OddOnePage(
                page = item,
                onPick = { pick -> actions.pickOdd(index, pick) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is MirrorSnapshot -> MirrorPage(
                page = item,
                onTap = { cell -> actions.tapMirrorCell(index, cell) },
                onRestart = { actions.restartMirror(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is GlSnapshot -> GreaterLessPage(
                page = item,
                onPut = { cell, value -> actions.putGlNumber(index, cell, value) },
                onRestart = { actions.restartGl(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is LampSnapshot -> LampsPage(
                page = item,
                onTap = { cell -> actions.tapLampCell(index, cell) },
                onRestart = { actions.restartLamps(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is BinSnapshot -> ZerosOnesPage(
                page = item,
                onTap = { cell -> actions.tapBinCell(index, cell) },
                onRestart = { actions.restartBin(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is LoneSnapshot -> LoneNumberPage(
                page = item,
                onTap = { cell -> actions.tapLoneCell(index, cell) },
                onRestart = { actions.restartLone(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is CodeSnapshot -> CodeBreakerPage(
                page = item,
                onSubmit = { guess -> actions.submitCode(index, guess) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is UntangleSnapshot -> UntanglePage(
                page = item,
                onMove = { node, x, y -> actions.moveNode(index, node, x, y) },
                onDrop = { actions.dropNode(index) },
                onRestart = { actions.restartUntangle(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is LaserSnapshot -> LasersPage(
                page = item,
                onTurn = { cell -> actions.turnMirror(index, cell) },
                onRestart = { actions.restartLasers(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is MatchSnapshot -> MatchsticksPage(
                page = item,
                onMove = { move -> actions.moveStick(index, move) },
                onUndo = { actions.undoStick(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is BallSlideSnapshot -> BallSlidePage(
                page = item,
                onSlide = { dir -> actions.slideBall(index, dir) },
                onUndo = { actions.undoBall(index) },
                onRestart = { actions.restartBall(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is AxisSumSnapshot -> AxisSumPage(
                page = item,
                onTap = { cell -> actions.tapAxisSum(index, cell) },
                onRestart = { actions.restartAxisSum(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
            )
            is TopicGroupsSnapshot -> TopicGroupsPage(
                page = item,
                onTap = { word -> actions.tapTopicWord(index, word) },
                onShuffle = { actions.shuffleTopicWords(index) },
                onClear = { actions.clearTopicSelection(index) },
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { actions.toggleFavorite(item.id) },
                languageSwitch = languageSwitch,
            )
            is NonogramSnapshot -> NonogramPage(
                page = item,
                onPaint = { from, to, value -> actions.paintNonogram(index, from, to, value) },
                onRestart = { actions.restartNonogram(index) },
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
