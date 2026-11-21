package com.mehmtcan.brainscroll.ui.rectangles

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_rectangles_board
import brainscroll.shared.generated.resources.a11y_rectangles_cell
import brainscroll.shared.generated.resources.a11y_rectangles_corner
import brainscroll.shared.generated.resources.a11y_rectangles_covered
import brainscroll.shared.generated.resources.a11y_rectangles_free
import brainscroll.shared.generated.resources.a11y_rectangles_next_cell
import brainscroll.shared.generated.resources.a11y_rectangles_next_number
import brainscroll.shared.generated.resources.a11y_rectangles_number
import brainscroll.shared.generated.resources.a11y_rectangles_prev_cell
import brainscroll.shared.generated.resources.a11y_rectangles_prev_number
import brainscroll.shared.generated.resources.a11y_rectangles_remove
import brainscroll.shared.generated.resources.a11y_rectangles_restart
import brainscroll.shared.generated.resources.a11y_rectangles_set_corner
import brainscroll.shared.generated.resources.rectangles_covered
import brainscroll.shared.generated.resources.rectangles_hint
import brainscroll.shared.generated.resources.rectangles_result
import brainscroll.shared.generated.resources.rectangles_won
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.rectangles.Rect
import com.mehmtcan.brainscroll.game.rectangles.RectResult
import com.mehmtcan.brainscroll.game.rectangles.RectSnapshot
import com.mehmtcan.brainscroll.game.rectangles.RectStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import com.mehmtcan.brainscroll.ui.components.ResultStats
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.feed.LocalGuide
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max
import kotlin.math.min

/** At least this high; a result card that needs more (big text) makes it higher and the board smaller. */
private val BottomAreaHeight = 152.dp
private val MaxBoard = 420.dp

/** What the board shows, for tests: the number of every cell (0 for none), the side, and the rectangles laid (x, y, width, height each). */
internal val RectNumbersKey = SemanticsPropertyKey<List<Int>>("RectNumbers")
private var SemanticsPropertyReceiver.numberList by RectNumbersKey
internal val RectPlacedKey = SemanticsPropertyKey<List<Int>>("RectPlaced")
private var SemanticsPropertyReceiver.placedList by RectPlacedKey
internal val RectSizeKey = SemanticsPropertyKey<Int>("RectSize")
private var SemanticsPropertyReceiver.sideSize by RectSizeKey

/** The colour of the rectangle of the number number [ordinal] (a rectangle keeps its colour whichever are laid). */
private val RectColors = listOf(
    Color(0xFF4CAF50), Color(0xFF29B6F6), Color(0xFFFFA726), Color(0xFFAB47BC), Color(0xFFEC407A), Color(0xFFFDD835),
)

/** The rectangle with corners at cells [a] and [b] (cell numbers, row * size + column). */
internal fun rectBetween(a: Int, b: Int, size: Int): Rect {
    val x0 = min(a % size, b % size)
    val x1 = max(a % size, b % size)
    val y0 = min(a / size, b / size)
    val y1 = max(a / size, b / size)
    return Rect(x0, y0, x1 - x0 + 1, y1 - y0 + 1)
}

/** Where the board is drawn: its cell size in pixels. */
internal class RectPaint(
    val cell: Color,
    val line: Color,
    val text: Color,
    val preview: Color,
    val rejected: Color,
    val won: Color,
    val guide: Color,
)

/**
 * Draws the board: the cells with their numbers, the rectangles laid (each tinted in its colour with a border), the one being drawn
 * ([preview], in the preview colour, or the rejected one while [rejected]) and a ring on [guideRect] if there is one.
 */
internal fun DrawScope.drawRectBoard(
    measurer: TextMeasurer,
    size: Int,
    numbers: List<Int>,
    clueCells: List<Int>,
    rects: List<Rect>,
    cellPx: Float,
    numberSize: androidx.compose.ui.unit.TextUnit,
    paint: RectPaint,
    won: Boolean,
    preview: Rect?,
    previewRejected: Boolean,
    guideRect: Rect?,
    guideRemove: Boolean,
    guidePulse: Float,
) {
    val inset = 1.5.dp.toPx()
    val corner = CornerRadius(5.dp.toPx())
    fun topLeft(x: Int, y: Int) = Offset(x * cellPx, y * cellPx)

    for (row in 0 until size) for (col in 0 until size) {
        drawRoundRect(paint.cell, topLeft(col, row) + Offset(inset / 2, inset / 2), Size(cellPx - inset, cellPx - inset), corner)
    }
    for (rect in rects) {
        // The rectangle is coloured as the number it holds is.
        val cell = rect.cells(size).first { numbers[it] > 0 }
        val tint = if (won) paint.won else RectColors[clueCells.indexOf(cell).mod(RectColors.size)]
        drawRoundRect(tint.copy(alpha = 0.38f), topLeft(rect.x, rect.y) + Offset(inset, inset), Size(rect.w * cellPx - 2 * inset, rect.h * cellPx - 2 * inset), corner)
        drawRoundRect(tint, topLeft(rect.x, rect.y) + Offset(inset, inset), Size(rect.w * cellPx - 2 * inset, rect.h * cellPx - 2 * inset), corner, style = Stroke(2.5.dp.toPx()))
    }
    if (preview != null) {
        val tint = if (previewRejected) paint.rejected else paint.preview
        drawRoundRect(tint.copy(alpha = 0.25f), topLeft(preview.x, preview.y) + Offset(inset, inset), Size(preview.w * cellPx - 2 * inset, preview.h * cellPx - 2 * inset), corner)
        drawRoundRect(tint, topLeft(preview.x, preview.y) + Offset(inset, inset), Size(preview.w * cellPx - 2 * inset, preview.h * cellPx - 2 * inset), corner, style = Stroke(3.dp.toPx()))
    }
    if (guideRect != null) {
        val tint = if (guideRemove) paint.rejected else paint.guide
        drawRoundRect(
            tint.copy(alpha = 0.35f + 0.6f * guidePulse),
            topLeft(guideRect.x, guideRect.y) + Offset(inset, inset),
            Size(guideRect.w * cellPx - 2 * inset, guideRect.h * cellPx - 2 * inset), corner, style = Stroke(4.dp.toPx()),
        )
    }
    for (cell in clueCells) {
        val layout = measurer.measure(numbers[cell].toString(), TextStyle(fontSize = numberSize, fontWeight = FontWeight.Bold, color = paint.text, textAlign = TextAlign.Center))
        val x = (cell % size) * cellPx + (cellPx - layout.size.width) / 2f
        val y = (cell / size) * cellPx + (cellPx - layout.size.height) / 2f
        drawText(layout, topLeft = Offset(x, y))
    }
}

/**
 * One full-screen page of the feed: the board with its numbers (drag across cells to draw a rectangle, tap one to take it off, tap a
 * free 1 to fill it), how much is covered, and a button that takes every rectangle off. The result takes the place of the hint and
 * the button when the board is covered.
 */
@Composable
fun RectanglesPage(
    page: RectSnapshot,
    onPlace: (Rect) -> RectResult,
    onRemove: (column: Int, row: Int) -> Boolean,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val puzzle = state.puzzle
    val n = puzzle.size
    val won = state.status == RectStatus.Won

    // The result and the buzz wait a moment so the last rectangle can be seen where it was laid.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 350L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    val latestState by rememberUpdatedState(state)
    val latestPlace by rememberUpdatedState(onPlace)
    val latestRemove by rememberUpdatedState(onRemove)

    // The rectangle being drawn (from the cell the finger went down on to the one it is on), and the one that was not allowed (it shows red a moment).
    var from by remember(page.id) { mutableIntStateOf(-1) }
    var to by remember(page.id) { mutableIntStateOf(-1) }
    var rejected by remember(page.id) { mutableStateOf<Rect?>(null) }
    LaunchedEffect(rejected) {
        if (rejected != null) {
            delay(450)
            rejected = null
        }
    }

    // Lets go of a rectangle drawn from cell [a] to cell [b] (or a tap, when they are the same): lay it, take it off, or fill a free 1.
    val commit: (Int, Int) -> Unit = commit@{ a, b ->
        val s = latestState
        if (s.status == RectStatus.Won) return@commit
        if (a == b) {
            val column = a % n
            val row = a / n
            if (s.rectAt(column, row) != null) {
                if (latestRemove(column, row)) haptics.perform(HapticKind.Light)
                return@commit
            }
            if (puzzle.numbers[a] != 1) return@commit
        }
        val rect = rectBetween(a, b, n)
        when (latestPlace(rect)) {
            RectResult.Placed -> haptics.perform(HapticKind.Medium)
            RectResult.Rejected -> {
                rejected = rect
                haptics.perform(HapticKind.Warning)
            }
            RectResult.Solved, RectResult.Ignored -> Unit
        }
    }

    // For TalkBack: a cursor that goes cell by cell or number by number, said out loud, and a corner that can be set to draw a rectangle.
    var cursor by remember(page.id) { mutableIntStateOf(0) }
    var corner by remember(page.id) { mutableIntStateOf(-1) }
    val cursorCell = cursor.coerceIn(0, n * n - 1)
    val freeText = stringResource(Res.string.a11y_rectangles_free)
    val coveredText = stringResource(Res.string.a11y_rectangles_covered)
    val numberText = stringResource(Res.string.a11y_rectangles_number, puzzle.numbers[cursorCell])
    val cornerText = stringResource(Res.string.a11y_rectangles_corner)
    val what = when {
        puzzle.numbers[cursorCell] > 0 -> numberText
        state.rectAt(cursorCell % n, cursorCell / n) != null -> coveredText
        else -> freeText
    }
    val spot = stringResource(Res.string.a11y_rectangles_cell, cursorCell / n + 1, cursorCell % n + 1, what) + (if (corner >= 0) ", $cornerText" else "")
    val nextCell = stringResource(Res.string.a11y_rectangles_next_cell)
    val prevCell = stringResource(Res.string.a11y_rectangles_prev_cell)
    val nextNumber = stringResource(Res.string.a11y_rectangles_next_number)
    val prevNumber = stringResource(Res.string.a11y_rectangles_prev_number)
    val setCorner = stringResource(Res.string.a11y_rectangles_set_corner)
    val removeHere = stringResource(Res.string.a11y_rectangles_remove)
    val clues = puzzle.clueCells

    val guide = LocalGuide.current?.takeIf { (it.reason == GuideReason.RectPlace || it.reason == GuideReason.RectRemove) && it.cells.size == 4 }
    val pulse by rememberInfiniteTransition(label = "rectGuide").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    val paint = RectPaint(
        cell = colors.bgSurface,
        line = colors.borderStrong,
        text = colors.textPrimary,
        preview = colors.textPrimary,
        rejected = colors.error,
        won = colors.correctFill,
        guide = colors.pendingFill,
    )
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Chip(text = stringResource(Res.string.rectangles_covered, state.covered, n * n), textColor = if (won) colors.correctFill else colors.textSecondary)
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // The biggest square that fits the room there is, across and down (a short screen with big text has little height).
            val side: Dp = minOf(MaxBoard, maxWidth, maxHeight)
            val cellPx = with(density) { side.toPx() } / n
            val numberSize = with(density) { (cellPx * 0.46f).toSp() }
            val description = stringResource(Res.string.a11y_rectangles_board, state.covered, n * n)
            val preview = when {
                rejected != null -> rejected
                from >= 0 && to >= 0 -> rectBetween(from, to, n)
                else -> null
            }
            Canvas(
                modifier = Modifier
                    .size(side)
                    .testTag("rectanglesBoard")
                    .semantics {
                        contentDescription = description
                        liveRegion = LiveRegionMode.Polite
                        numberList = puzzle.numbers
                        placedList = state.rects.flatMap { listOf(it.x, it.y, it.w, it.h) }
                        sideSize = n
                        stateDescription = spot
                        if (!won) {
                            customActions = listOf(
                                CustomAccessibilityAction(nextCell) { cursor = (cursorCell + 1) % (n * n); true },
                                CustomAccessibilityAction(prevCell) { cursor = (cursorCell + n * n - 1) % (n * n); true },
                                CustomAccessibilityAction(nextNumber) { cursor = clues.firstOrNull { it > cursorCell } ?: clues.first(); true },
                                CustomAccessibilityAction(prevNumber) { cursor = clues.lastOrNull { it < cursorCell } ?: clues.last(); true },
                                CustomAccessibilityAction(setCorner) {
                                    if (corner < 0) corner = cursorCell else {
                                        val a = corner
                                        corner = -1
                                        commit(a, cursorCell)
                                    }
                                    true
                                },
                                CustomAccessibilityAction(removeHere) { commit(cursorCell, cursorCell).let { true } },
                            )
                        }
                    }
                    .pointerInput(n, cellPx) {
                        fun cellAt(p: Offset): Int {
                            val col = (p.x / cellPx).toInt().coerceIn(0, n - 1)
                            val row = (p.y / cellPx).toInt().coerceIn(0, n - 1)
                            return row * n + col
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            if (latestState.status == RectStatus.Won) return@awaitEachGesture
                            down.consume()
                            val start = cellAt(down.position)
                            from = start
                            to = start
                            try {
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    to = cellAt(change.position)
                                    change.consume()
                                } while (change.pressed)
                            } catch (e: PointerEventTimeoutCancellationException) {
                                // The gesture was taken away: nothing is drawn.
                                from = -1
                                to = -1
                                return@awaitEachGesture
                            }
                            val end = to
                            from = -1
                            to = -1
                            commit(start, end)
                        }
                    },
            ) {
                drawRectBoard(
                    measurer = measurer,
                    size = n,
                    numbers = puzzle.numbers,
                    clueCells = clues,
                    rects = state.rects,
                    cellPx = cellPx,
                    numberSize = numberSize,
                    paint = paint,
                    won = won,
                    preview = preview,
                    previewRejected = rejected != null,
                    guideRect = if (guide != null && !won) Rect(guide.cells[0], guide.cells[1], guide.cells[2], guide.cells[3]) else null,
                    guideRemove = guide?.reason == GuideReason.RectRemove,
                    guidePulse = pulse,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                RectanglesResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.rectangles_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_rectangles_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.rects.isNotEmpty(),
                        tag = "rectanglesRestart",
                    ) { drawRestart(ink) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun RectanglesResultCard(page: RectSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().testTag("rectanglesResultCard").resultSurface(colors.bgSurface, GameKind.Rectangles.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.rectangles_won),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.rectangles_result, formatDuration(page.durationMs)),
                stats = listOf(
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.Rectangles.world),
                ),
            )
            BasicText(
                text = swipeHintText(),
                style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
        }
        HeartButton(filled = isFavorite, onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd))
    }
}

/** A small picture for the thumbnails: a 4 by 4 board cut into four coloured rectangles with their numbers. */
@Composable
internal fun RectanglesThumbArt(side: Dp) {
    val colors = BrainScrollTheme.colors
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // 4 + 2 + 4 + 4 + 2 = 16 cells.
    val rects = listOf(Rect(0, 0, 2, 2), Rect(2, 0, 2, 1), Rect(2, 1, 2, 2), Rect(0, 2, 2, 2), Rect(2, 3, 2, 1))
    val tints = listOf(RectColors[0], RectColors[1], RectColors[2], RectColors[3], RectColors[4])
    val labels = mapOf(0 to "4", 1 to "2", 2 to "4", 3 to "4", 4 to "2")
    Canvas(Modifier.size(side * 0.9f)) {
        val cell = size.width / 4f
        val numberSize = with(density) { (cell * 0.46f).toSp() }
        val inset = 1.5.dp.toPx()
        val corner = CornerRadius(4.dp.toPx())
        for ((i, r) in rects.withIndex()) {
            val topLeft = Offset(r.x * cell + inset, r.y * cell + inset)
            val box = Size(r.w * cell - 2 * inset, r.h * cell - 2 * inset)
            drawRoundRect(tints[i].copy(alpha = 0.38f), topLeft, box, corner)
            drawRoundRect(tints[i], topLeft, box, corner, style = Stroke(2.dp.toPx()))
            val layout = measurer.measure(labels.getValue(i), TextStyle(fontSize = numberSize, fontWeight = FontWeight.Bold, color = colors.textPrimary))
            drawText(layout, topLeft = Offset(r.x * cell + (r.w * cell - layout.size.width) / 2f, r.y * cell + (r.h * cell - layout.size.height) / 2f))
        }
    }
}
