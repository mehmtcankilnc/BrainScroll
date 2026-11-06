package com.mehmtcan.brainscroll.ui.colorflow

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_colorflow_count
import brainscroll.shared.generated.resources.a11y_colorflow_restart
import brainscroll.shared.generated.resources.colorflow_filled
import brainscroll.shared.generated.resources.colorflow_hint
import brainscroll.shared.generated.resources.colorflow_joined
import brainscroll.shared.generated.resources.colorflow_result
import brainscroll.shared.generated.resources.colorflow_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.colorflow.FlowResult
import com.mehmtcan.brainscroll.game.colorflow.FlowSnapshot
import com.mehmtcan.brainscroll.game.colorflow.FlowStatus
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.memory.CardSymbol
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.feed.LocalGuide
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.memory.drawSymbol
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.components.ResultStats
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import brainscroll.shared.generated.resources.result_stat_time
import brainscroll.shared.generated.resources.result_stat_strokes
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.sign

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val CellGap = 4.dp
private val MaxBoard = 380.dp

/** How many cells have a path and how many colors are joined, for tests. */
internal val FlowCovered = SemanticsPropertyKey<Int>("FlowCovered")
private var SemanticsPropertyReceiver.coveredCells by FlowCovered
internal val FlowJoinedCount = SemanticsPropertyKey<Int>("FlowJoinedCount")
private var SemanticsPropertyReceiver.joinedColors by FlowJoinedCount

/** The size of the board and the cells of the dots (two for each color, one after the other), for tests. */
internal val FlowBoardSize = SemanticsPropertyKey<Int>("FlowBoardSize")
private var SemanticsPropertyReceiver.boardSize by FlowBoardSize
internal val FlowDotCells = SemanticsPropertyKey<List<Int>>("FlowDotCells")
private var SemanticsPropertyReceiver.dotCells by FlowDotCells

private val Palette = listOf(
    Color(0xFFE5484D),
    Color(0xFF3E8EF7),
    Color(0xFF30A46C),
    Color(0xFFF5C542),
    Color(0xFF9B6BDF),
    Color(0xFFF08A3C),
    Color(0xFF2CC4D6),
    Color(0xFFEE6FB0),
)

/** The color of color [index]. Each also has its own shape on the dots (see [flowSymbol]), so the colors are not the only clue. */
internal fun flowColor(index: Int): Color = Palette[index % Palette.size]

internal fun flowSymbol(index: Int): CardSymbol = CardSymbol.entries[index % CardSymbol.entries.size]

/**
 * One full-screen page of the feed: how many colors are joined and how many cells are filled, the board with its dots, and a restart
 * button, or the result. A finger put down on a dot draws that color's path cell by cell; a path cuts the other paths it crosses.
 * The board is won when every color is joined and every cell is filled.
 */
@Composable
fun ColorFlowPage(
    page: FlowSnapshot,
    onStart: (Int) -> Boolean,
    onExtend: (Int) -> Boolean,
    onEnd: () -> FlowResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val reduce = LocalReduceMotion.current
    val state = page.state
    val won = state.status == FlowStatus.Won
    val n = state.size

    // The result and the buzz wait a moment so the last stroke can be seen.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 350L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    val latestStart by rememberUpdatedState(onStart)
    val latestExtend by rememberUpdatedState(onExtend)
    val latestEnd by rememberUpdatedState(onEnd)

    val guide = LocalGuide.current?.takeIf { it.reason == GuideReason.FlowRoute && it.cells.isNotEmpty() }
    val pulse by rememberInfiniteTransition(label = "flowGuide").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        val joinedCount = state.joined.count { it }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Chip(
                text = stringResource(Res.string.colorflow_joined, joinedCount, state.pairs.size),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_colorflow_count, joinedCount, state.pairs.size, state.covered, state.cellCount),
            )
            Chip(text = stringResource(Res.string.colorflow_filled, state.covered, state.cellCount), textColor = colors.textSecondary)
        }
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side: Dp = minOf(MaxBoard, maxWidth, maxHeight)
            val density = LocalDensity.current
            val cellPx = with(density) { (side / n).toPx() }
            val description = stringResource(Res.string.a11y_colorflow_count, joinedCount, state.pairs.size, state.covered, state.cellCount)
            val extraBorder = if (LocalHighContrast.current) 1.5f else 0f
            Box(
                modifier = Modifier
                    .size(side)
                    .testTag("colorFlowBoard")
                    .semantics {
                        contentDescription = description
                        coveredCells = state.covered
                        joinedColors = joinedCount
                        boardSize = n
                        dotCells = state.pairs.flatMap { listOf(it.first, it.second) }
                    }
                    .pointerInput(won, n, cellPx) {
                        if (won) return@pointerInput
                        awaitEachGesture {
                            fun cellOf(p: Offset): Int? {
                                val c = (p.x / cellPx).toInt()
                                val r = (p.y / cellPx).toInt()
                                return if (p.x >= 0f && p.y >= 0f && c in 0 until n && r in 0 until n) r * n + c else null
                            }
                            val down = awaitFirstDown()
                            val first = cellOf(down.position)
                            if (first == null || !latestStart(first)) return@awaitEachGesture
                            down.consume()
                            var last: Int = first
                            var moved = false
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                val target = cellOf(change.position)
                                if (target != null && target != last) {
                                    // A quick finger can skip cells: walk to the cell it is on, one neighbor at a time.
                                    var guard = 0
                                    while (last != target && guard++ < 2 * n) {
                                        val dr = target / n - last / n
                                        val dc = target % n - last % n
                                        val next = if (abs(dr) >= abs(dc)) last + sign(dr.toFloat()).toInt() * n else last + sign(dc.toFloat()).toInt()
                                        if (!latestExtend(next)) break
                                        moved = true
                                        last = next
                                    }
                                }
                                change.consume()
                            } while (change.pressed)
                            val result = latestEnd()
                            if (result != FlowResult.Ignored || moved) haptics.perform(HapticKind.Light)
                        }
                    },
            ) {
                val tile = colors.bgSurface
                val edge = colors.borderStrong.copy(alpha = 0.35f)
                Canvas(Modifier.fillMaxSize()) {
                    val gap = CellGap.toPx() / 2f
                    val corner = CornerRadius(cellPx * 0.18f)
                    fun center(c: Int) = Offset((c % n + 0.5f) * cellPx, (c / n + 0.5f) * cellPx)
                    val owner = IntArray(n * n) { -1 }
                    state.paths.forEachIndexed { color, path -> path.forEach { owner[it] = color } }
                    for (c in 0 until n * n) {
                        val topLeft = Offset((c % n) * cellPx + gap, (c / n) * cellPx + gap)
                        val box = Size(cellPx - 2 * gap, cellPx - 2 * gap)
                        drawRoundRect(tile, topLeft, box, corner)
                        if (owner[c] >= 0) drawRoundRect(flowColor(owner[c]).copy(alpha = 0.2f), topLeft, box, corner)
                        drawRoundRect(edge, topLeft, box, corner, style = Stroke(width = 1.5.dp.toPx() + extraBorder))
                    }
                    // The paths: a thick round line through the centers of the cells.
                    state.paths.forEachIndexed { color, path ->
                        if (path.size < 2) return@forEachIndexed
                        val line = Path().apply {
                            moveTo(center(path[0]).x, center(path[0]).y)
                            for (i in 1 until path.size) lineTo(center(path[i]).x, center(path[i]).y)
                        }
                        drawPath(line, flowColor(color), style = Stroke(width = cellPx * 0.34f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    // The guide: the cells to draw next light up in the color of the path.
                    if (guide != null) {
                        val a = if (reduce) 0.7f else 0.3f + 0.5f * pulse
                        val tint = flowColor(guide.numbers.firstOrNull() ?: 0)
                        guide.cells.forEach { c ->
                            drawCircle(tint.copy(alpha = a), cellPx * 0.2f, center(c))
                            drawCircle(Color.White.copy(alpha = a * 0.6f), cellPx * 0.2f, center(c), style = Stroke(width = cellPx * 0.04f))
                        }
                    }
                    // The dots, with the shape of their color inside.
                    state.pairs.forEachIndexed { color, (a, b) ->
                        for (cell in listOf(a, b)) {
                            val m = center(cell)
                            val active = state.active == color
                            drawCircle(flowColor(color), cellPx * (if (active) 0.4f else 0.34f), m)
                            drawCircle(Color.Black.copy(alpha = 0.25f), cellPx * (if (active) 0.4f else 0.34f), m, style = Stroke(width = cellPx * 0.03f))
                            val r = cellPx * 0.17f
                            inset(m.x - r, m.y - r, size.width - m.x - r, size.height - m.y - r) { drawSymbol(flowSymbol(color), Color.White.copy(alpha = 0.9f)) }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                ColorFlowResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.colorflow_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_colorflow_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.canRestart,
                        tag = "colorFlowRestart",
                    ) { drawRestart(ink) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun ColorFlowResultCard(page: FlowSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().resultSurface(colors.bgSurface, GameKind.ColorFlow.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.colorflow_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
    summary = stringResource(Res.string.colorflow_result, page.state.moves, formatDuration(page.durationMs)),
    stats = listOf(
        ResultStatData(page.state.moves.toString(), stringResource(Res.string.result_stat_strokes), GameKind.ColorFlow.world),
        ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), colors.streakAnswer),
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

/** Draws a board of dots and paths the way the page does, for the thumbnail and the demo: [paths] by color, [dots] by color. */
internal fun DrawScope.drawFlowBoard(
    n: Int,
    left: Float,
    top: Float,
    cell: Float,
    paths: List<List<Int>>,
    dots: List<Pair<Int, Int>>,
    tile: Color,
    edge: Color,
    alpha: Float = 1f,
) {
    fun center(c: Int) = Offset(left + (c % n + 0.5f) * cell, top + (c / n + 0.5f) * cell)
    val inset = cell * 0.03f
    val corner = CornerRadius(cell * 0.18f)
    val owner = IntArray(n * n) { -1 }
    paths.forEachIndexed { color, path -> path.forEach { owner[it] = color } }
    for (c in 0 until n * n) {
        val o = Offset(left + (c % n) * cell + inset, top + (c / n) * cell + inset)
        val box = Size(cell - 2 * inset, cell - 2 * inset)
        drawRoundRect(tile.copy(alpha = tile.alpha * alpha), o, box, corner)
        if (owner[c] >= 0) drawRoundRect(flowColor(owner[c]).copy(alpha = 0.2f * alpha), o, box, corner)
        drawRoundRect(edge.copy(alpha = edge.alpha * alpha), o, box, corner, style = Stroke(width = 1.dp.toPx()))
    }
    paths.forEachIndexed { color, path ->
        if (path.size < 2) return@forEachIndexed
        val line = Path().apply {
            moveTo(center(path[0]).x, center(path[0]).y)
            for (i in 1 until path.size) lineTo(center(path[i]).x, center(path[i]).y)
        }
        drawPath(line, flowColor(color).copy(alpha = alpha), style = Stroke(width = cell * 0.34f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    dots.forEachIndexed { color, (a, b) ->
        for (c in listOf(a, b)) {
            val m = center(c)
            drawCircle(flowColor(color).copy(alpha = alpha), cell * 0.34f, m)
            val r = cell * 0.17f
            inset(m.x - r, m.y - r, size.width - m.x - r, size.height - m.y - r) { drawSymbol(flowSymbol(color), Color.White.copy(alpha = 0.9f * alpha)) }
        }
    }
}
