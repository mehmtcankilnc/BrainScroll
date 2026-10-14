package com.mehmtcan.brainscroll.ui.line

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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_line_board
import brainscroll.shared.generated.resources.a11y_line_cell_filled
import brainscroll.shared.generated.resources.a11y_line_cell_free
import brainscroll.shared.generated.resources.a11y_line_cell_head
import brainscroll.shared.generated.resources.a11y_line_cell_number
import brainscroll.shared.generated.resources.a11y_line_cell_number_done
import brainscroll.shared.generated.resources.a11y_line_hint
import brainscroll.shared.generated.resources.a11y_line_restart
import brainscroll.shared.generated.resources.a11y_line_stars
import brainscroll.shared.generated.resources.a11y_line_undo
import brainscroll.shared.generated.resources.line_end_too_early
import brainscroll.shared.generated.resources.line_hint_text
import brainscroll.shared.generated.resources.line_hints
import brainscroll.shared.generated.resources.line_no_hint
import brainscroll.shared.generated.resources.line_result
import brainscroll.shared.generated.resources.line_share
import brainscroll.shared.generated.resources.line_share_copied
import brainscroll.shared.generated.resources.line_squares
import brainscroll.shared.generated.resources.line_undos
import brainscroll.shared.generated.resources.line_won
import brainscroll.shared.generated.resources.line_wrong_order
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.line.LineResult
import com.mehmtcan.brainscroll.game.line.LineScoring
import com.mehmtcan.brainscroll.game.line.LineSnapshot
import com.mehmtcan.brainscroll.game.line.LineStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.AppTextButton
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import com.mehmtcan.brainscroll.ui.tubes.drawUndo
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val MaxBoard = 420.dp

/** How many squares are filled, for tests (the board node also carries the number of the next number). */
internal val LineFilled = SemanticsPropertyKey<Int>("LineFilled")
internal val LineReached = SemanticsPropertyKey<Int>("LineReached")
private var SemanticsPropertyReceiver.lineFilled by LineFilled
private var SemanticsPropertyReceiver.lineReached by LineReached

/**
     * The line to copy for sharing the result (see [com.mehmtcan.brainscroll.game.line.LineShare]); null where there is no
     * sharing (a board that is not the daily one). The daily screen provides it once the board is solved.
     */
val LocalLineShare = compositionLocalOf<String?> { null }

/** What the line of text under the board says instead of the rules, after something that did not work. */
private enum class LineNotice { WrongOrder, EndTooEarly, NoHint }

/**
 * One full-screen page of the daily puzzle: the counters, the board, and the buttons or the result.
 *
 * The line is drawn with a finger: a touch on a square next to the end of the line (or on number 1 to begin) puts it
 * on, moving the finger over the next squares goes on, and moving back over the line takes squares off. A touch on an
 * earlier square of the line cuts the line back to it. The same works with taps one at a time, and for a screen
 * reader every square is a button.
 */
@Composable
fun LinePage(
    page: LineSnapshot,
    onDrag: (Int) -> LineResult,
    onTap: (Int) -> LineResult,
    onUndo: () -> Boolean,
    onRestart: () -> Boolean,
    onHint: () -> List<Int>?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val won = state.status == LineStatus.Won
    val cells = page.puzzle.cells

    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 0)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    var notice by remember { mutableStateOf<LineNotice?>(null) }
    var starsInfo by remember { mutableStateOf(false) }

    /** Buzzes for what a touch did and keeps the line of text under the board in step. */
    fun react(result: LineResult) {
        when (result) {
            LineResult.Started, LineResult.Extended, LineResult.Retracted, LineResult.Cut -> {
                notice = null
                haptics.perform(HapticKind.Light)
            }
            LineResult.Solved -> notice = null
            LineResult.WrongOrder -> { notice = LineNotice.WrongOrder; haptics.perform(HapticKind.Warning) }
            LineResult.EndTooEarly -> { notice = LineNotice.EndTooEarly; haptics.perform(HapticKind.Warning) }
            LineResult.Ignored -> Unit
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.xs))
        // The stars the board is worth right now, and the way to find out how they are earned.
        LineStarsBar(stars = page.stars, onInfo = { starsInfo = true })
        Spacer(Modifier.height(Spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Chip(text = stringResource(Res.string.line_squares, state.path.size, cells), textColor = colors.textSecondary)
            Chip(text = stringResource(Res.string.line_undos, state.undos), textColor = colors.textSecondary)
            if (state.hintsUsed > 0) Chip(text = stringResource(Res.string.line_hints, state.hintsUsed), textColor = colors.textSecondary)
        }
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side: Dp = minOf(MaxBoard, maxWidth, maxHeight)
            val boardDescription = stringResource(Res.string.a11y_line_board, state.path.size, cells, minOf(state.reached + 1, page.puzzle.lastNumber))
            Box(
                modifier = Modifier
                    .size(side)
                    .testTag("lineBoard")
                    .semantics {
                        contentDescription = boardDescription
                        lineFilled = state.path.size
                        lineReached = state.reached
                    },
            ) {
                LineBoard(page, side, playing = !won, onDrag = { react(onDrag(it)) }, onTap = { react(onTap(it)) })
                // For a screen reader and for tests: every square is a button that does what a tap does.
                if (!won) {
                    val cell = side / state.size
                    Column(Modifier.size(side)) {
                        for (row in 0 until state.size) {
                            Row {
                                for (column in 0 until state.size) {
                                    val index = row * state.size + column
                                    CellButton(page, index, row + 1, column + 1, cell) { react(onTap(index)) }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                LineResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = when (notice) {
                            LineNotice.WrongOrder -> stringResource(Res.string.line_wrong_order, state.reached + 1)
                            LineNotice.EndTooEarly -> stringResource(Res.string.line_end_too_early)
                            LineNotice.NoHint -> stringResource(Res.string.line_no_hint)
                            null -> stringResource(Res.string.line_hint_text)
                        },
                        modifier = Modifier.testTag("lineMessage").semantics { liveRegion = LiveRegionMode.Polite },
                        style = type.body.copy(color = if (notice == null) colors.textSecondary else colors.textPrimary, textAlign = TextAlign.Center),
                    )
                    val iconColor = colors.textPrimary
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        RoundIconButton(
                            description = stringResource(Res.string.a11y_line_undo),
                            onClick = { if (onUndo()) { notice = null; haptics.perform(HapticKind.Light) } },
                            enabled = state.canUndo,
                            tag = "lineUndo",
                        ) { drawUndo(iconColor) }
                        RoundIconButton(
                            description = stringResource(Res.string.a11y_line_restart),
                            onClick = { if (onRestart()) { notice = null; haptics.perform(HapticKind.Light) } },
                            enabled = state.canRestart,
                            tag = "lineRestart",
                        ) { drawRestart(iconColor) }
                        RoundIconButton(
                            description = stringResource(Res.string.a11y_line_hint),
                            onClick = {
                                if (onHint() != null) { notice = null; haptics.perform(HapticKind.Light) } else notice = LineNotice.NoHint
                            },
                            tag = "lineHint",
                        ) { drawHint(iconColor) }
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
    if (starsInfo) LineStarsInfoSheet(onClose = { starsInfo = false })
}

/** The board and the finger on it. [side] is its width and height; the touch work is done in pixels of this node. */
@Composable
private fun LineBoard(page: LineSnapshot, side: Dp, playing: Boolean, onDrag: (Int) -> Unit, onTap: (Int) -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val highContrast = LocalHighContrast.current
    val measurer = rememberTextMeasurer()
    val state = page.state
    val n = state.size
    val tap by rememberUpdatedState(onTap)
    val drag by rememberUpdatedState(onDrag)
    val numberStyle: TextStyle = type.label.copy(fontWeight = FontWeight.Bold)

    Canvas(
        modifier = Modifier
            .size(side)
            .then(
                if (playing) {
                    Modifier.pointerInput(n) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val width = size.width.toFloat()
                            /** The square under [at]; while moving, only its middle counts, so cutting a corner does not skip or flicker. */
                            fun squareAt(at: Offset, strict: Boolean): Int? {
                                val c = (at.x / width * n).toInt()
                                val r = (at.y / width * n).toInt()
                                if (c !in 0 until n || r !in 0 until n) return null
                                if (strict) {
                                    val fx = at.x / width * n - c
                                    val fy = at.y / width * n - r
                                    if (fx < DEAD_ZONE || fx > 1f - DEAD_ZONE || fy < DEAD_ZONE || fy > 1f - DEAD_ZONE) return null
                                }
                                return r * n + c
                            }
                            var last = squareAt(down.position, strict = false)
                            last?.let { tap(it) }
                            down.consume()
                            do {
                                val event = awaitPointerEvent()
                                for (change in event.changes) {
                                    if (change.positionChanged()) {
                                        val square = squareAt(change.position, strict = true)
                                        if (square != null && square != last) {
                                            last = square
                                            drag(square)
                                        }
                                        change.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                } else Modifier,
            ),
    ) {
        val cell = size.width / n
        val gap = cell * 0.04f
        val filled = state.path.toSet()
        val won = state.status == LineStatus.Won

        for (row in 0 until n) for (column in 0 until n) {
            val index = row * n + column
            val topLeft = Offset(column * cell + gap, row * cell + gap)
            val box = Size(cell - 2 * gap, cell - 2 * gap)
            drawRoundRect(if (index in filled) colors.correctFill.copy(alpha = 0.16f) else colors.bgSurface, topLeft, box, CornerRadius(cell * 0.16f))
            drawRoundRect(colors.borderStrong.copy(alpha = if (highContrast) 0.9f else 0.3f), topLeft, box, CornerRadius(cell * 0.16f), style = Stroke(if (highContrast) 2f else 1f))
        }

        fun center(index: Int) = Offset((index % n + 0.5f) * cell, (index / n + 0.5f) * cell)

        if (state.path.size > 1) {
            val line = Path().apply {
                moveTo(center(state.path[0]).x, center(state.path[0]).y)
                for (i in 1 until state.path.size) lineTo(center(state.path[i]).x, center(state.path[i]).y)
            }
            drawPath(line, colors.correctFill.copy(alpha = if (won) 1f else 0.6f), style = Stroke(cell * 0.34f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (state.path.isNotEmpty() && !won) drawCircle(colors.correctFill, cell * 0.2f, center(state.path.last()))

        page.puzzle.checkpoints.forEachIndexed { i, index ->
            val number = i + 1
            val passed = number <= state.reached
            drawCircle(if (passed) colors.correctFill else colors.textPrimary, cell * 0.3f, center(index))
            val layout = measurer.measure(number.toString(), numberStyle.copy(fontSize = (cell * 0.34f).toSp()))
            val textColor = if (passed) colors.correctOn else colors.bgPage
            drawText(layout, textColor, center(index) - Offset(layout.size.width / 2f, layout.size.height / 2f))
        }

        // The hint is drawn over the numbers: a dot on a plain square, a ring round a number (a dot would be hidden by it).
        for (index in page.hint) {
            if (page.puzzle.numbers[index] > 0) drawCircle(colors.streakDay, cell * 0.38f, center(index), style = Stroke(cell * 0.07f))
            else drawCircle(colors.streakDay, cell * 0.14f, center(index))
        }
    }
}

/** How much of the edge of a square does not count while a finger moves over the board. */
private const val DEAD_ZONE = 0.15f

/** One square as a button for a screen reader. It draws nothing and passes touches to the board under it. */
@Composable
private fun CellButton(page: LineSnapshot, index: Int, row: Int, column: Int, size: Dp, onClick: () -> Unit) {
    val state = page.state
    val number = page.puzzle.numbers[index]
    val description = when {
        number > 0 && number <= state.reached -> stringResource(Res.string.a11y_line_cell_number_done, row, column, number)
        number > 0 -> stringResource(Res.string.a11y_line_cell_number, row, column, number)
        state.path.lastOrNull() == index -> stringResource(Res.string.a11y_line_cell_head, row, column)
        index in state.path -> stringResource(Res.string.a11y_line_cell_filled, row, column)
        else -> stringResource(Res.string.a11y_line_cell_free, row, column)
    }
    Box(
        modifier = Modifier
            .size(size)
            .testTag("lineCell")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
                onClick { onClick(); true }
            },
    )
}

/** The three stars of the result, [earned] of them filled. */
@Composable
internal fun LineStars(earned: Int, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val colors = BrainScrollTheme.colors
    val description = stringResource(Res.string.a11y_line_stars, earned)
    Row(
        modifier = modifier.testTag("lineStars").semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        repeat(LineScoring.MAX_STARS) { i ->
            Canvas(Modifier.size(size)) { drawStar(if (i < earned) colors.streakDay else colors.borderStrong, filled = i < earned) }
        }
    }
}

@Composable
private fun LineResultCard(page: LineSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.line_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            LineStars(page.stars)
            BasicText(
                text = stringResource(Res.string.line_result, formatDuration(page.durationMs)),
                style = type.body.copy(color = colors.textPrimary),
            )
            val share = LocalLineShare.current
            if (share != null) {
                val clipboard = LocalClipboardManager.current
                var copied by remember { mutableStateOf(false) }
                AppTextButton(
                    text = stringResource(if (copied) Res.string.line_share_copied else Res.string.line_share),
                    onClick = { clipboard.setText(AnnotatedString(share)); copied = true },
                    modifier = Modifier.testTag("lineShare"),
                )
            } else {
                BasicText(
                    text = swipeHintText(),
                    style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                )
            }
        }
        HeartButton(filled = isFavorite, onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd))
    }
}

/** A five-pointed star filling the drawing area, solid or only an outline. */
internal fun DrawScope.drawStar(color: Color, filled: Boolean) {
    val s = size.minDimension
    val center = Offset(s / 2, s / 2 + s * 0.04f)
    val outer = s * 0.48f
    val inner = outer * 0.42f
    val star = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * PI / 5
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()
        if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
    }
    star.close()
    if (filled) drawPath(star, color) else drawPath(star, color, style = Stroke(s * 0.06f, join = StrokeJoin.Round))
}

/** A light bulb: a round glass over a small base. */
internal fun DrawScope.drawHint(color: Color) {
    val s = size.minDimension
    val stroke = Stroke(width = s * 0.1f, cap = StrokeCap.Round)
    drawCircle(color, radius = s * 0.24f, center = Offset(s * 0.5f, s * 0.40f), style = stroke)
    drawLine(color, Offset(s * 0.38f, s * 0.74f), Offset(s * 0.62f, s * 0.74f), strokeWidth = s * 0.1f, cap = StrokeCap.Round)
    drawLine(color, Offset(s * 0.42f, s * 0.88f), Offset(s * 0.58f, s * 0.88f), strokeWidth = s * 0.1f, cap = StrokeCap.Round)
}
