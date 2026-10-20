package com.mehmtcan.brainscroll.ui.arrows

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_arrows_blocked
import brainscroll.shared.generated.resources.a11y_arrows_count
import brainscroll.shared.generated.resources.a11y_arrows_free
import brainscroll.shared.generated.resources.a11y_arrows_mistakes
import brainscroll.shared.generated.resources.a11y_arrows_restart
import brainscroll.shared.generated.resources.arrows_dir_down
import brainscroll.shared.generated.resources.arrows_dir_left
import brainscroll.shared.generated.resources.arrows_dir_right
import brainscroll.shared.generated.resources.arrows_dir_up
import brainscroll.shared.generated.resources.arrows_hint
import brainscroll.shared.generated.resources.arrows_left
import brainscroll.shared.generated.resources.arrows_mistakes
import brainscroll.shared.generated.resources.arrows_result
import brainscroll.shared.generated.resources.arrows_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.arrows.ArrowDir
import com.mehmtcan.brainscroll.game.arrows.ArrowState
import com.mehmtcan.brainscroll.game.arrows.ArrowsGame
import com.mehmtcan.brainscroll.game.arrows.ArrowsResult
import com.mehmtcan.brainscroll.game.arrows.ArrowsSnapshot
import com.mehmtcan.brainscroll.game.arrows.ArrowsStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.feed.GuideMark
import com.mehmtcan.brainscroll.ui.feed.isGuided
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollColors
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val MaxBoard = 400.dp

private val CellGap = 4.dp

/**
 * The colors of the arrows, one after the other by arrow. Red is not among them: it is for the arrow that is in the way.
 */
internal fun arrowPalette(colors: BrainScrollColors): List<Color> = listOf(
    colors.streakAnswer,
    colors.correctFill,
    colors.pendingFill,
    colors.streakDay,
    lerp(colors.streakAnswer, colors.error, 0.5f),
)

/** How long one cell of the way out takes, and the shortest and longest time an arrow takes to leave. */
private const val MILLIS_PER_CELL = 70
private const val MIN_LEAVE_MILLIS = 300
private const val MAX_LEAVE_MILLIS = 800
private const val FLASH_MILLIS = 700L

/** The direction of the arrow whose head is on a cell, for tests: only the head cell of an arrow has it. */
internal val ArrowsCellValue = SemanticsPropertyKey<Int>("ArrowsCellValue")
private var SemanticsPropertyReceiver.cellValue by ArrowsCellValue

/** The cell of the head of the arrow, for tests. */
internal val ArrowsHeadCell = SemanticsPropertyKey<Int>("ArrowsHeadCell")
private var SemanticsPropertyReceiver.headCell by ArrowsHeadCell

/**
 * One full-screen page of the feed: how many arrows are left and how many mistakes were made, the board, and the restart
 * button or the result. A tap on an arrow sends it along its own path and off the board if nothing is in front of its head; if
 * something is, the arrow bumps into it, the one in the way flashes red and it counts as a mistake.
 */
@Composable
fun ArrowsPage(
    page: ArrowsSnapshot,
    onTap: (Int) -> ArrowsResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val reduceMotion = LocalReduceMotion.current
    val state = page.state
    val won = state.status == ArrowsStatus.Won

    // The result, the buzz and the card wait until the last arrow has left the board.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, MAX_LEAVE_MILLIS.toLong() + 50)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    // A mistake shakes the board, bumps the arrow that was tapped into the one in the way, and turns that one red for a moment.
    // A page that comes back with mistakes does not.
    var seenMistakes by remember { mutableIntStateOf(state.mistakes) }
    var bumps by remember { mutableIntStateOf(0) }
    var flashArrow by remember { mutableIntStateOf(-1) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state.mistakes) {
        if (state.mistakes > seenMistakes) {
            seenMistakes = state.mistakes
            bumps++
            flashArrow = state.lastBlocker
            if (!reduceMotion) {
                for (x in listOf(-10f, 10f, -7f, 7f, 0f)) shake.animateTo(x, tween(45))
            }
            delay(FLASH_MILLIS)
            flashArrow = -1
        } else {
            seenMistakes = state.mistakes
        }
    }

    val palette = remember(colors) { arrowPalette(colors) }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Chip(
                text = stringResource(Res.string.arrows_left, state.remaining),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_arrows_count, state.remaining, state.total),
            )
            Chip(
                text = stringResource(Res.string.arrows_mistakes, state.mistakes),
                textColor = if (state.mistakes > 0) colors.error else colors.textSecondary,
                description = stringResource(Res.string.a11y_arrows_mistakes, state.mistakes),
            )
        }
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val n = state.size
            val side: Dp = minOf(MaxBoard, maxWidth, maxHeight)
            val cell: Dp = side / n
            val density = LocalDensity.current
            val cellPx = with(density) { cell.toPx() }
            val tap by rememberUpdatedState { c: Int ->
                when (onTap(c)) {
                    ArrowsResult.Blocked -> haptics.perform(HapticKind.Warning)
                    ArrowsResult.Escaped -> haptics.perform(HapticKind.Light)
                    else -> Unit
                }
            }
            Box(
                modifier = Modifier
                    .size(side)
                    .testTag("arrowsBoard")
                    .graphicsLayer { translationX = with(density) { shake.value.dp.toPx() } }
                    .clipToBounds(),
            ) {
                // The cells, as in the other boards: a quiet tile each.
                val tile = colors.bgSurface
                val edge = colors.borderStrong.copy(alpha = 0.35f)
                val extra = if (LocalHighContrast.current) 1f else 0f
                Canvas(Modifier.fillMaxSize()) {
                    val gap = CellGap.toPx() / 2f
                    val corner = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                    for (c in 0 until n * n) {
                        val topLeft = Offset((c % n) * cellPx + gap, (c / n) * cellPx + gap)
                        val tileSize = androidx.compose.ui.geometry.Size(cellPx - 2 * gap, cellPx - 2 * gap)
                        drawRoundRect(tile, topLeft, tileSize, corner)
                        drawRoundRect(edge, topLeft, tileSize, corner, style = Stroke(width = (1.5f + extra).dp.toPx()))
                    }
                }
                // The taps: every cell of an arrow that is on the board is a tap on that arrow; the head cell is the one a screen reader finds.
                state.arrows.forEachIndexed { index, arrow ->
                    if (!arrow.alive || won) return@forEachIndexed
                    arrow.cells.forEach { c ->
                        val isHead = c == arrow.head
                        Box(
                            Modifier
                                .offset { IntOffset((c % n) * cellPx.toInt(), (c / n) * cellPx.toInt()) }
                                .size(cell)
                                .then(if (isHead) Modifier.testTag("arrowsCell") else Modifier)
                                .then(if (isHead) Modifier.headSemantics(arrow, c, n) { tap(c) } else Modifier)
                                .pointerInput(c) { detectTapGestures { tap(c) } },
                        ) {
                            GuideMark(isGuided(c), Radius.card) { Spacer(Modifier.size(cell)) }
                        }
                    }
                }
                state.arrows.forEachIndexed { index, arrow ->
                    Snake(
                        arrow = arrow,
                        size = n,
                        cellPx = cellPx,
                        color = palette[index % palette.size],
                        flash = flashArrow == index,
                        bumpTick = if (state.lastBlocked == index) bumps else 0,
                        bumpCells = bumpCells(arrow, state.lastBlockCell, n),
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status != WordleStatus.Playing) {
                ArrowsResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.arrows_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val iconColor = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_arrows_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.canRestart,
                        tag = "arrowsRestart",
                    ) { drawRestart(iconColor) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** The head cell of an arrow for a screen reader: which way it points, where it is, and whether it can leave. */
@Composable
private fun Modifier.headSemantics(arrow: ArrowState, cell: Int, n: Int, onClick: () -> Unit): Modifier {
    val name = stringResource(
        when (arrow.dir) {
            ArrowDir.UP -> Res.string.arrows_dir_up
            ArrowDir.RIGHT -> Res.string.arrows_dir_right
            ArrowDir.DOWN -> Res.string.arrows_dir_down
            else -> Res.string.arrows_dir_left
        },
    )
    val description = stringResource(if (arrow.free) Res.string.a11y_arrows_free else Res.string.a11y_arrows_blocked, name, cell / n + 1, cell % n + 1)
    return semantics {
        contentDescription = description
        role = Role.Button
        cellValue = arrow.dir
        headCell = cell
        onClick { onClick(); true }
    }
}

/** How many cells the arrow can go before its head touches the arrow in the way (a little short of it), 0 when nothing is. */
private fun bumpCells(arrow: ArrowState, blockCell: Int, n: Int): Float {
    if (blockCell < 0) return 0f
    val steps = abs(blockCell / n - arrow.head / n) + abs(blockCell % n - arrow.head % n)
    return max(0.15f, steps - 0.55f)
}

/**
 * One arrow, drawn as a thick winding line with the head at its end. When it leaves, it slides like a snake: the whole body
 * follows its own path, then goes straight out the way the head points, until the tail has left the board (the board clips it).
 * A blocked arrow slides forward until its head nearly touches the arrow in the way and comes back. With reduced motion the
 * arrow only fades. An arrow that was already gone when the page came back is not drawn at all.
 */
@Composable
private fun Snake(arrow: ArrowState, size: Int, cellPx: Float, color: Color, flash: Boolean, bumpTick: Int, bumpCells: Float) {
    val colors = BrainScrollTheme.colors
    val reduce = LocalReduceMotion.current
    val points = remember(arrow.cells, arrow.dir, size, cellPx) { snakePoints(arrow, size, cellPx) }
    val bodyLength = remember(points) { polylineLength(points.body) }
    val cellsAhead = remember(arrow, size) { cellsToEdge(arrow, size) }
    // How far the head has gone along its way out: 0 on the board, `total` when the tail is out too.
    val total = bodyLength + (cellsAhead + 1f) * cellPx
    val leave = remember { Animatable(if (arrow.alive) 0f else total) }
    val bump = remember { Animatable(0f) }
    var wasAlive by remember { mutableStateOf(arrow.alive) }
    LaunchedEffect(arrow.alive) {
        when {
            arrow.alive -> {
                leave.snapTo(0f)
                wasAlive = true
            }
            wasAlive -> {
                wasAlive = false
                if (reduce) {
                    leave.animateTo(total, tween(Motion.FAST))
                } else {
                    val cells = total / cellPx
                    leave.animateTo(total, tween((cells * MILLIS_PER_CELL).toInt().coerceIn(MIN_LEAVE_MILLIS, MAX_LEAVE_MILLIS), easing = LinearEasing))
                }
            }
        }
    }
    LaunchedEffect(bumpTick) {
        if (bumpTick > 0 && !reduce) {
            bump.animateTo(bumpCells * cellPx, tween(110, easing = Motion.Standard))
            bump.animateTo(0f, tween(170, easing = Motion.Standard))
        }
    }
    val progress = leave.value
    if (!arrow.alive && progress >= total) return
    val drawColor = if (flash) colors.error else color
    val fade = if (reduce && !arrow.alive) 1f - progress / total else 1f
    Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = fade }) {
        val shift = if (reduce) 0f else progress + bump.value
        drawSnake(points, bodyLength, shift, arrow.dir, drawColor, cellPx)
    }
}

/** The line of an arrow: the centers of its cells (a short stub for an arrow of one cell), and the way on past its head. */
internal class SnakePoints(val body: List<Offset>, val ahead: Offset)

internal fun snakePoints(arrow: ArrowState, size: Int, cellPx: Float): SnakePoints {
    fun center(cell: Int) = Offset(((cell % size) + 0.5f) * cellPx, ((cell / size) + 0.5f) * cellPx)
    val dx = ArrowDir.dColumn[arrow.dir].toFloat()
    val dy = ArrowDir.dRow[arrow.dir].toFloat()
    val head = center(arrow.head)
    val body = if (arrow.cells.size == 1) listOf(Offset(head.x - dx * 0.33f * cellPx, head.y - dy * 0.33f * cellPx), head) else arrow.cells.map(::center)
    // Far enough to take the whole body out of any board.
    val far = (size + arrow.cells.size + 2f) * cellPx
    return SnakePoints(body, Offset(head.x + dx * far, head.y + dy * far))
}

/** The cells between the head and the edge of the board it points at. */
internal fun cellsToEdge(arrow: ArrowState, size: Int): Float {
    val row = arrow.head / size
    val column = arrow.head % size
    return when (arrow.dir) {
        ArrowDir.UP -> row
        ArrowDir.DOWN -> size - 1 - row
        ArrowDir.LEFT -> column
        else -> size - 1 - column
    }.toFloat()
}

internal fun polylineLength(points: List<Offset>): Float = (1 until points.size).sumOf { distance(points[it - 1], points[it]).toDouble() }.toFloat()

private fun distance(a: Offset, b: Offset): Float = sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))

/** Draws the arrow after its head has slid [shift] along the way out: the stretch of the whole line that is from [shift] to [shift] + the body. */
internal fun DrawScope.drawSnake(points: SnakePoints, bodyLength: Float, shift: Float, dir: Int, color: Color, cellPx: Float) {
    val line = points.body + points.ahead
    val from = shift
    val to = shift + bodyLength
    val path = Path()
    var travelled = 0f
    var started = false
    var end = line.first()
    for (i in 1 until line.size) {
        val a = line[i - 1]
        val b = line[i]
        val length = distance(a, b)
        val segFrom = max(from, travelled)
        val segTo = min(to, travelled + length)
        if (segTo > segFrom && length > 0f) {
            val p = Offset(a.x + (b.x - a.x) * (segFrom - travelled) / length, a.y + (b.y - a.y) * (segFrom - travelled) / length)
            val q = Offset(a.x + (b.x - a.x) * (segTo - travelled) / length, a.y + (b.y - a.y) * (segTo - travelled) / length)
            if (!started) {
                path.moveTo(p.x, p.y)
                started = true
            }
            path.lineTo(q.x, q.y)
            end = q
        }
        travelled += length
    }
    if (!started) return
    drawPath(path, color, style = Stroke(width = cellPx * 0.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawArrowHead(end, dir, color, cellPx)
}

/** The head: a filled triangle whose base is on [at] (the end of the line) and whose tip is beyond it. */
private fun DrawScope.drawArrowHead(at: Offset, dir: Int, color: Color, cellPx: Float) {
    val dx = ArrowDir.dColumn[dir].toFloat()
    val dy = ArrowDir.dRow[dir].toFloat()
    val tip = Offset(at.x + dx * cellPx * 0.42f, at.y + dy * cellPx * 0.42f)
    val back = Offset(at.x - dx * cellPx * 0.04f, at.y - dy * cellPx * 0.04f)
    val half = cellPx * 0.27f
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(back.x - dy * half, back.y + dx * half)
        lineTo(back.x + dy * half, back.y - dx * half)
        close()
    }
    drawPath(path, color)
    drawPath(path, color, style = Stroke(width = cellPx * 0.06f, join = StrokeJoin.Round))
}

@Composable
private fun ArrowsResultCard(page: ArrowsSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.arrows_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(Res.string.arrows_result, page.state.mistakes, formatDuration(page.durationMs)),
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
