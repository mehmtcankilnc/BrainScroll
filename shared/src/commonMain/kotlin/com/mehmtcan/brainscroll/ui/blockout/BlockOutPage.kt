package com.mehmtcan.brainscroll.ui.blockout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_blockout_exit
import brainscroll.shared.generated.resources.a11y_blockout_horizontal
import brainscroll.shared.generated.resources.a11y_blockout_moves
import brainscroll.shared.generated.resources.a11y_blockout_par
import brainscroll.shared.generated.resources.a11y_blockout_piece
import brainscroll.shared.generated.resources.a11y_blockout_piece_target
import brainscroll.shared.generated.resources.a11y_blockout_restart
import brainscroll.shared.generated.resources.a11y_blockout_selected
import brainscroll.shared.generated.resources.a11y_blockout_slide_down
import brainscroll.shared.generated.resources.a11y_blockout_slide_left
import brainscroll.shared.generated.resources.a11y_blockout_slide_right
import brainscroll.shared.generated.resources.a11y_blockout_slide_up
import brainscroll.shared.generated.resources.a11y_blockout_vertical
import brainscroll.shared.generated.resources.blockout_hint
import brainscroll.shared.generated.resources.blockout_moves
import brainscroll.shared.generated.resources.blockout_par
import brainscroll.shared.generated.resources.blockout_result
import brainscroll.shared.generated.resources.blockout_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.blockout.BlockOutResult
import com.mehmtcan.brainscroll.game.blockout.BlockOutSnapshot
import com.mehmtcan.brainscroll.game.blockout.BlockOutStatus
import com.mehmtcan.brainscroll.game.blockout.BlockPiece
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.feed.LocalGuide
import com.mehmtcan.brainscroll.ui.feed.guideHighlight
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollColors
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val CellGap = 4.dp
private val MaxCell = 64.dp
private val FramePad = 6.dp

/** The room to the right of the board for the arrow that marks the exit. */
private val ExitWidth = 26.dp

/** A slide of a block takes this long. */
private const val SLIDE_MILLIS = 180

/** What a block is, for tests: its number, its place, its length, 1 if it lies (0 if it stands), its row or column, and 1 if it is the target. */
internal val BlockOutPieceInfo = SemanticsPropertyKey<List<Int>>("BlockOutPieceInfo")
private var SemanticsPropertyReceiver.pieceInfo by BlockOutPieceInfo

/**
 * One full-screen page of the feed: the move counters, the board with its exit, and the restart button or the result. A block is
 * moved by dragging it along its length, or by tapping it and then the cell it should go to.
 */
@Composable
fun BlockOutPage(
    page: BlockOutSnapshot,
    onSlide: (piece: Int, to: Int) -> BlockOutResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val won = state.status == BlockOutStatus.Won

    // The block that was tapped and waits for the cell to go to. Only the screen knows about it.
    var selected by remember(page.id) { mutableStateOf<Int?>(null) }

    // The result, the buzz and the card wait until the target has slid out to the exit.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, SLIDE_MILLIS + 120L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    fun slide(piece: Int, to: Int): BlockOutResult {
        val result = onSlide(piece, to)
        if (result != BlockOutResult.Ignored) haptics.perform(HapticKind.Medium)
        return result
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Chip(
                text = stringResource(Res.string.blockout_moves, state.moves),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_blockout_moves, state.moves),
            )
            Chip(
                text = stringResource(Res.string.blockout_par, state.par),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_blockout_par, state.par),
            )
        }
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().testTag("blockoutBoard"), contentAlignment = Alignment.Center) {
            val n = state.size
            val framePad = FramePad
            val cellSize: Dp = minOf(
                MaxCell,
                (maxWidth - ExitWidth - framePad * 2 - CellGap * (n - 1)) / n,
                (maxHeight - framePad * 2 - CellGap * (n - 1)) / n,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Board(
                    state = state,
                    cell = cellSize,
                    selected = selected,
                    playing = !won,
                    onSelect = { selected = it },
                    onSlide = ::slide,
                    onCellTap = { cellRow, cellColumn ->
                        val piece = state.pieces.firstOrNull { it.id == selected } ?: return@Board
                        // Tapping a cell in the line of the picked block: the near end of the block goes there.
                        val to = target(piece, cellRow, cellColumn)
                        if (to == null || to !in piece.minPos..piece.maxPos || to == piece.pos) {
                            haptics.perform(HapticKind.Warning)
                        } else {
                            selected = null
                            slide(piece.id, to)
                        }
                    },
                )
                // The exit: an arrow on the right of the middle row.
                Box(Modifier.width(ExitWidth).height(cellSize * n + CellGap * (n - 1) + framePad * 2)) {
                    val exitDescription = stringResource(Res.string.a11y_blockout_exit)
                    val arrow = colors.streakDay
                    Canvas(
                        Modifier
                            .offset(y = framePad + (cellSize + CellGap) * state.exitRow + cellSize * 0.2f)
                            .size(ExitWidth, cellSize * 0.6f)
                            .semantics { contentDescription = exitDescription },
                    ) { drawExitArrow(arrow) }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                BlockOutResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.blockout_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val iconColor = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_blockout_restart),
                        onClick = { selected = null; if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.canRestart,
                        tag = "blockoutRestart",
                    ) { drawRestart(iconColor) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** Where the picked block goes when the cell at ([row], [column]) is tapped: null if that cell is not in the block's line. */
private fun target(piece: BlockPiece, row: Int, column: Int): Int? {
    val inLine = if (piece.horizontal) row == piece.line else column == piece.line
    if (!inLine) return null
    val at = if (piece.horizontal) column else row
    return when {
        at > piece.pos + piece.length - 1 -> at - piece.length + 1
        at < piece.pos -> at
        else -> null
    }
}

/** The board: the frame, the empty cells, the blocks on top of them and the opening in the frame at the exit. */
@Composable
private fun Board(
    state: com.mehmtcan.brainscroll.game.blockout.BlockOutState,
    cell: Dp,
    selected: Int?,
    playing: Boolean,
    onSelect: (Int?) -> Unit,
    onSlide: (piece: Int, to: Int) -> BlockOutResult,
    onCellTap: (row: Int, column: Int) -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val n = state.size
    val inner = cell * n + CellGap * (n - 1)
    val frame = RoundedCornerShape(12.dp)
    val palette = remember(colors) { piecePalette(colors) }
    var nextColor = 0
    // The non-target pieces take the colors of the palette one after the other.
    val colorOf = state.pieces.associate { piece -> piece.id to if (piece.isTarget) colors.streakDay else palette[nextColor++ % palette.size] }

    Box(
        modifier = Modifier
            .background(colors.borderStrong.copy(alpha = 0.22f), frame)
            .padding(FramePad),
    ) {
        Box(Modifier.size(inner)) {
            // The empty cells, under the blocks. A tap on one is where a picked block goes.
            for (row in 0 until n) for (column in 0 until n) {
                Box(
                    Modifier
                        .offset(x = (cell + CellGap) * column, y = (cell + CellGap) * row)
                        .size(cell)
                        .testTag("blockoutCell")
                        .background(colors.bgSurface, RoundedCornerShape(8.dp))
                        .then(
                            if (playing && selected != null) {
                                Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCellTap(row, column) }
                            } else Modifier,
                        ),
                )
            }
            for (piece in state.pieces) {
                Piece(
                    piece = piece,
                    color = colorOf.getValue(piece.id),
                    cell = cell,
                    isSelected = selected == piece.id,
                    playing = playing,
                    onTap = { onSelect(if (selected == piece.id) null else piece.id) },
                    onSlide = onSlide,
                )
            }
        }
        // The opening in the frame, where the target leaves the board.
        Box(
            Modifier
                .offset(x = inner, y = (cell + CellGap) * state.exitRow)
                .size(FramePad, cell)
                .background(colors.bgPage),
        )
    }
}

/**
 * The colors of the blocks that are not the target, one after the other (the orange is the target's alone, the yellow the picked
 * block's). Two of them are the blue and the green of the theme; the others are mixes of the theme's colors (purple, teal, pink),
 * so every block has a vivid color of its own and none is a gray.
 */
internal fun piecePalette(colors: BrainScrollColors): List<Color> = listOf(
    colors.streakAnswer,
    colors.correctFill,
    lerp(colors.streakAnswer, colors.error, 0.5f),
    lerp(colors.correctFill, colors.streakAnswer, 0.5f),
    lerp(colors.error, colors.textPrimary, 0.35f),
)

/**
 * One block. It is drawn where [BlockPiece.pos] says, and slides there when that changes; while a finger drags it, it follows the
 * finger along its length (as far as the other blocks and the edge let it), and on release it settles on the nearest cell.
 */
@Composable
private fun Piece(
    piece: BlockPiece,
    color: Color,
    cell: Dp,
    isSelected: Boolean,
    playing: Boolean,
    onTap: () -> Unit,
    onSlide: (piece: Int, to: Int) -> BlockOutResult,
) {
    val colors = BrainScrollTheme.colors
    val reduce = LocalReduceMotion.current
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(piece)
    val stepPx = with(density) { (cell + CellGap).toPx() }

    // The place the block is drawn at, in cells (a fraction while it moves). It starts at the place the game has.
    val shown = remember(piece.id) { Animatable(piece.pos.toFloat()) }
    var dragging by remember(piece.id) { mutableStateOf<Float?>(null) }
    LaunchedEffect(piece.pos, reduce) {
        // From where it is now (also mid-drag, after a release) to the place of the game; the first frame is where it was.
        if (reduce) shown.snapTo(piece.pos.toFloat()) else shown.animateTo(piece.pos.toFloat(), tween(SLIDE_MILLIS, easing = FastOutSlowInEasing))
    }
    val at = dragging ?: shown.value

    val width = if (piece.horizontal) cell * piece.length + CellGap * (piece.length - 1) else cell
    val height = if (piece.horizontal) cell else cell * piece.length + CellGap * (piece.length - 1)
    val shape = RoundedCornerShape(8.dp)
    val guided = LocalGuide.current?.cells?.contains(piece.id) == true

    val orientation = stringResource(if (piece.horizontal) Res.string.a11y_blockout_horizontal else Res.string.a11y_blockout_vertical)
    val row = if (piece.horizontal) piece.line else piece.pos
    val column = if (piece.horizontal) piece.pos else piece.line
    var description = if (piece.isTarget) {
        stringResource(Res.string.a11y_blockout_piece_target, piece.length, row + 1, column + 1)
    } else {
        stringResource(Res.string.a11y_blockout_piece, piece.id + 1, orientation, piece.length, row + 1, column + 1)
    }
    if (isSelected) description += ", " + stringResource(Res.string.a11y_blockout_selected)
    val backName = stringResource(if (piece.horizontal) Res.string.a11y_blockout_slide_left else Res.string.a11y_blockout_slide_up)
    val forwardName = stringResource(if (piece.horizontal) Res.string.a11y_blockout_slide_right else Res.string.a11y_blockout_slide_down)

    fun release() {
        val drag = dragging ?: return
        val now = current
        val to = drag.roundToInt().coerceIn(now.minPos, now.maxPos)
        scope.launch {
            // Hand the place over to the animation at the very value the finger left it, so nothing jumps.
            shown.snapTo(drag)
            dragging = null
            val result = if (to != now.pos) onSlide(now.id, to) else BlockOutResult.Ignored
            if (result == BlockOutResult.Ignored) shown.animateTo(now.pos.toFloat(), tween(SLIDE_MILLIS, easing = FastOutSlowInEasing))
        }
    }

    fun drag(amountPx: Float) {
        val now = current
        val from = dragging ?: shown.value
        dragging = (from + amountPx / stepPx).coerceIn(now.minPos.toFloat(), now.maxPos.toFloat())
    }

    val x = if (piece.horizontal) (cell + CellGap) * at else (cell + CellGap) * piece.line
    val y = if (piece.horizontal) (cell + CellGap) * piece.line else (cell + CellGap) * at

    // The ring of the guide is on a box of its own around the block, so its tag is not hidden by the tag of the block.
    Box(Modifier.offset(x = x, y = y).size(width, height).guideHighlight(guided, shape)) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("blockoutPiece")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                pieceInfo = listOf(piece.id, piece.pos, piece.length, if (piece.horizontal) 1 else 0, piece.line, if (piece.isTarget) 1 else 0)
                role = Role.Button
                this.selected = isSelected
                customActions = if (playing) listOf(
                    CustomAccessibilityAction(backName) { onSlide(piece.id, piece.pos - 1) != BlockOutResult.Ignored },
                    CustomAccessibilityAction(forwardName) { onSlide(piece.id, piece.pos + 1) != BlockOutResult.Ignored },
                ) else emptyList()
            }
            .background(color, shape)
            // Flat, solid blocks: the gaps between them tell them apart. Only the picked block gets an outline.
            .then(if (isSelected) Modifier.border((3f + extraBorder).dp, colors.pendingFill, shape) else Modifier)
            .then(
                if (!playing) Modifier
                else Modifier
                    .pointerInput(piece.id, stepPx, piece.horizontal) {
                        if (piece.horizontal) {
                            detectHorizontalDragGestures(
                                onDragEnd = { release() },
                                onDragCancel = { release() },
                                onHorizontalDrag = { change, amount -> change.consume(); drag(amount) },
                            )
                        } else {
                            detectVerticalDragGestures(
                                onDragEnd = { release() },
                                onDragCancel = { release() },
                                onVerticalDrag = { change, amount -> change.consume(); drag(amount) },
                            )
                        }
                    }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onTap),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (piece.isTarget) Canvas(Modifier.size(cell * 0.5f)) { drawTargetMark(colors.bgPage, Offset.Zero, size.width) }
    }
    }
}

/**
 * The mark of the target block: two arrows pointing right, towards the exit, in a square of [side] with its top left corner at
 * [topLeft]. It says where the block has to go, and it tells the target from the other blocks by more than its color.
 */
internal fun DrawScope.drawTargetMark(color: Color, topLeft: Offset, side: Float) {
    val stroke = side * 0.17f
    fun chevron(shift: Float) {
        val path = Path().apply {
            moveTo(topLeft.x + side * (0.12f + shift), topLeft.y + side * 0.16f)
            lineTo(topLeft.x + side * (0.48f + shift), topLeft.y + side * 0.5f)
            lineTo(topLeft.x + side * (0.12f + shift), topLeft.y + side * 0.84f)
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
    chevron(0f)
    chevron(0.36f)
}

/** An arrow pointing right, the exit. */
private fun DrawScope.drawExitArrow(color: Color) {
    val w = size.width
    val h = size.height
    val arrow = Path().apply {
        moveTo(w * 0.1f, h * 0.2f)
        lineTo(w * 0.9f, h * 0.5f)
        lineTo(w * 0.1f, h * 0.8f)
        close()
    }
    drawPath(arrow, color)
}

@Composable
private fun BlockOutResultCard(page: BlockOutSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.blockout_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(Res.string.blockout_result, page.state.moves, formatDuration(page.durationMs)),
                style = type.body.copy(color = colors.textPrimary),
            )
            BasicText(
                text = swipeHintText(),
                style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
        }
        HeartButton(filled = isFavorite, onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd))
    }
}
