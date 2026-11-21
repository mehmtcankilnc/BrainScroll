package com.mehmtcan.brainscroll.ui.nonogram

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as StrokeStyle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_nonogram_column_clue
import brainscroll.shared.generated.resources.a11y_nonogram_count
import brainscroll.shared.generated.resources.a11y_nonogram_cross
import brainscroll.shared.generated.resources.a11y_nonogram_empty
import brainscroll.shared.generated.resources.a11y_nonogram_filled
import brainscroll.shared.generated.resources.a11y_nonogram_line_done
import brainscroll.shared.generated.resources.a11y_nonogram_restart
import brainscroll.shared.generated.resources.a11y_nonogram_row_clue
import brainscroll.shared.generated.resources.nonogram_column_label
import brainscroll.shared.generated.resources.nonogram_count
import brainscroll.shared.generated.resources.nonogram_hint
import brainscroll.shared.generated.resources.nonogram_language
import brainscroll.shared.generated.resources.nonogram_mode_cross
import brainscroll.shared.generated.resources.nonogram_result
import brainscroll.shared.generated.resources.nonogram_row_label
import brainscroll.shared.generated.resources.nonogram_won
import brainscroll.shared.generated.resources.nonogram_won_plain
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.nonogram.NonogramMark
import com.mehmtcan.brainscroll.game.nonogram.NonogramPicture
import com.mehmtcan.brainscroll.game.nonogram.NonogramResult
import com.mehmtcan.brainscroll.game.nonogram.NonogramSnapshot
import com.mehmtcan.brainscroll.game.nonogram.NonogramStatus
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
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.sound.*
import com.mehmtcan.brainscroll.ui.theme.BrainScrollColors
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.components.ResultStats
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import org.jetbrains.compose.resources.stringResource

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val CellGap = 2.dp
private val MaxCell = 40.dp

/** How far the numbers may reach into the padding at the side of the page (the page pads 16 dp, 4 dp stay free). */
private val SideRoom = 12.dp

/** How long the board waits for the picture to take its colors before the result shows. */
private const val COLOR_MILLIS = 400

/** The whole reveal runs on one clock of this length; the parts below are cut out of it. */
private const val REVEAL_MILLIS = 1000
private const val REVEAL_DELAY = 100L
private const val CLUES_MILLIS = 500
private const val MORPH_MILLIS = 600
private const val ZOOM_MILLIS = 900
private const val ZOOM_TARGET = 1.2f

private val CellRadius = 16.dp
private val PictureRadius = 3.dp

private fun lerpFloat(a: Float, b: Float, t: Float) = a + (b - a) * t

/** One part of the reveal: [clock] (0..1 over [REVEAL_MILLIS]) turned into 0..1 over [millis] with the standard curve. */
private fun part(clock: Float, millis: Int): Float = Motion.Standard.transform((clock * REVEAL_MILLIS / millis).coerceIn(0f, 1f))

/** What a cell holds, for tests: see [NonogramMark]. */
internal val NonogramCellValue = SemanticsPropertyKey<Int>("NonogramCellValue")
private var SemanticsPropertyReceiver.cellValue by NonogramCellValue

/** One stroke being drawn: the cells from [from] to [to] get [value]. */
private data class Stroke(val from: Int, val to: Int, val value: Int)

/** The colors a solved picture can take, by picture (every color is a mix of the theme's, none is red). */
private fun accentColors(colors: BrainScrollColors): List<Color> = listOf(
    colors.streakAnswer,
    colors.streakDay,
    colors.pendingFill,
    lerp(colors.streakAnswer, colors.error, 0.5f),
    lerp(colors.correctFill, colors.streakAnswer, 0.5f),
)

private fun accentOf(picture: NonogramPicture?, colors: BrainScrollColors): Color {
    val palette = accentColors(colors)
    return palette[((picture?.id?.hashCode() ?: 0) and Int.MAX_VALUE) % palette.size]
}

/**
 * One full-screen page of the feed: how many cells are filled, the mode (fill or mark), the board with the numbers of its rows
 * and columns, and the restart button or the result. A tap sets one cell, a drag along a row or column sets the whole stretch.
 */
@Composable
fun NonogramPage(
    page: NonogramSnapshot,
    onPaint: (from: Int, to: Int, value: Int) -> NonogramResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current.withoutSound()
    val state = page.state
    val won = state.status == NonogramStatus.Won
    val n = state.size

    // The result, the buzz and the card wait until the picture has taken its colors.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, COLOR_MILLIS.toLong() + 50)
    OutcomeHaptics(status, page.id, haptics)
    // The sound starts with the picture taking its colors.
    WinSound(if (won) WordleStatus.Won else WordleStatus.Playing, listOf(WinStep(0, Sfx.NonogramWinChord, NONOGRAM_CHORD_VOLUME)))
    val sounds = LocalSoundEffects.current
    val soundScope = rememberCoroutineScope()
    // Counters of the sounds (not of the game): the strokes made and the rattles played, from the start of the puzzle.
    var strokeNo by remember { mutableIntStateOf(0) }
    var rattles by remember { mutableIntStateOf(0) }
    var lastRattle by remember { mutableStateOf<TimeMark?>(null) }
    LaunchedEffect(page.id, state.canRestart) {
        if (!state.canRestart) {
            strokeNo = 0
            rattles = 0
            lastRattle = null
        }
    }
    /** One note for each cell the stroke fills, in the order of the stroke, [NONOGRAM_FILL_GAP_MILLIS] apart. */
    fun playFill(from: Int, to: Int, before: List<Int>) {
        val step = if (from / n == to / n) 1 else n
        val dir = if (to >= from) step else -step
        val cells = generateSequence(from) { if (it == to) null else it + dir }.toList()
            .filter { before[it] != NonogramMark.FILLED }
        val stroke = strokeNo++
        soundScope.launch {
            cells.forEachIndexed { k, _ ->
                if (k > 0) delay(NONOGRAM_FILL_GAP_MILLIS)
                sounds.play(nonogramFillSound(stroke, k), NONOGRAM_FILL_VOLUME)
            }
        }
    }
    val justFinished = rememberJustFinished(status)
    val reduceMotion = LocalReduceMotion.current

    // The reveal after the win: numbers and empty cells step back, the picture closes up, grows and glows. 0 while playing.
    // A puzzle that is already solved when it is first shown starts at the end.
    val reveal = remember { Animatable(if (status == WordleStatus.Won) 1f else 0f) }
    LaunchedEffect(status, reduceMotion) {
        when {
            status != WordleStatus.Won -> reveal.snapTo(0f)
            reduceMotion || !justFinished -> reveal.snapTo(1f)
            else -> {
                delay(REVEAL_DELAY)
                reveal.animateTo(1f, tween(REVEAL_MILLIS, easing = LinearEasing))
            }
        }
    }

    // Which mark the finger puts down: filling or crossing.
    var mode by remember { mutableIntStateOf(NonogramMark.FILLED) }
    var stroke by remember { mutableStateOf<Stroke?>(null) }
    val marks by rememberUpdatedState(state.marks)
    val modeNow by rememberUpdatedState(mode)
    val playing by rememberUpdatedState(!won)

    // Said aloud when a line becomes complete: (is a row, number from 0).
    var completed by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
    var seenRows by remember { mutableStateOf(state.rowDone) }
    var seenColumns by remember { mutableStateOf(state.columnDone) }
    LaunchedEffect(state.rowDone, state.columnDone) {
        val newRow = state.rowDone.indices.firstOrNull { state.rowDone[it] && !seenRows[it] }
        val newColumn = state.columnDone.indices.firstOrNull { state.columnDone[it] && !seenColumns[it] }
        if (newRow != null || newColumn != null) {
            // Lines that finish together (a cell closes a row and a column) rattle once.
            val last = lastRattle
            if (last == null || last.elapsedNow() >= NONOGRAM_LINE_MIN_GAP_MILLIS.milliseconds) {
                sounds.play(nonogramLineSound(rattles++), NONOGRAM_LINE_VOLUME)
                lastRattle = TimeSource.Monotonic.markNow()
            }
        }
        completed = when {
            newRow != null -> true to newRow
            newColumn != null -> false to newColumn
            else -> null
        }
        seenRows = state.rowDone
        seenColumns = state.columnDone
    }
    val completedText = completed?.let { (isRow, line) ->
        stringResource(
            Res.string.a11y_nonogram_line_done,
            stringResource(if (isRow) Res.string.nonogram_row_label else Res.string.nonogram_column_label, line + 1),
        )
    }.orEmpty()

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Chip(
            text = stringResource(Res.string.nonogram_count, state.filledCount),
            textColor = colors.textSecondary,
            description = stringResource(Res.string.a11y_nonogram_count, state.filledCount),
        )
        Spacer(Modifier.height(Spacing.md))

        val accent = remember(state.picture, colors) { accentOf(state.picture, colors) }
        val language = stringResource(Res.string.nonogram_language)

        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("nonogramBoard").drawBehind {
                val glow = part(reveal.value, REVEAL_MILLIS)
                if (glow > 0f) {
                    // The glow ends a little before half the width, around the middle of the area.
                    val radius = size.width * 1.05f
                    drawCircle(
                        brush = Brush.radialGradient(
                            0f to accent.copy(alpha = 0x55 / 255f * glow),
                            0.46f to accent.copy(alpha = 0f),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            val density = LocalDensity.current
            val maxRowClues = state.rowClues.maxOf { it.size.coerceAtLeast(1) }
            val maxColumnClues = state.columnClues.maxOf { it.size.coerceAtLeast(1) }
            // A number takes half a cell along its line, so the clue area grows with the picture's busiest line.
            val unit = 0.55f
            // The row numbers sit on the left, so the cell size is worked out as if the same room were on the right too.
            val widthUnits = n + maxRowClues * unit * 2
            val heightUnits = n + maxColumnClues * unit
            val cell: Dp = minOf(
                MaxCell,
                (maxWidth + SideRoom * 2 - CellGap * (n + 1)) / widthUnits,
                (maxHeight - CellGap * (n - 1)) / heightUnits,
            )
            val clueWidth = cell * unit * maxRowClues
            val clueHeight = cell * unit * maxColumnClues
            val step = with(density) { (cell + CellGap).toPx() }
            val boardSide = cell * n + CellGap * (n - 1)
            // Grows to 1.2 unless the board would then leave the area it has (the page keeps 4 dp at the sides free).
            val zoomTarget = minOf(ZOOM_TARGET, maxHeight / boardSide, (maxWidth + SideRoom * 2) / boardSide).coerceAtLeast(1f)

            fun cellAt(position: Offset): Int {
                val column = (position.x / step).toInt().coerceIn(0, n - 1)
                val row = (position.y / step).toInt().coerceIn(0, n - 1)
                return row * n + column
            }

            /** The end of a stroke from [start] to the cell under the finger: kept on one row or column, the one the finger moved along most. */
            fun project(start: Int, position: Offset): Int {
                val target = cellAt(position)
                val dRow = kotlin.math.abs(target / n - start / n)
                val dColumn = kotlin.math.abs(target % n - start % n)
                return if (dColumn >= dRow) (start / n) * n + target % n else (target / n) * n + start % n
            }

            // Shifted left by half the clue area, so the board itself is in the middle; the numbers may reach into the side padding.
            Column(
                modifier = Modifier
                    .wrapContentWidth(Alignment.CenterHorizontally, unbounded = true)
                    .offset(x = -(clueWidth + CellGap) / 2)
                    .graphicsLayer {
                        // Grows around the middle of the board, which also moves to the middle of the area once the numbers are gone.
                        if (reduceMotion) return@graphicsLayer
                        val zoom = part(reveal.value, ZOOM_MILLIS)
                        val boardLeft = (clueWidth + CellGap).toPx()
                        val boardTop = (clueHeight + CellGap).toPx()
                        val side = boardSide.toPx()
                        transformOrigin = TransformOrigin((boardLeft + side / 2) / size.width, (boardTop + side / 2) / size.height)
                        scaleX = 1f + (zoomTarget - 1f) * zoom
                        scaleY = scaleX
                        translationY = (size.height / 2 - boardTop - side / 2) * zoom
                    },
                horizontalAlignment = Alignment.Start,
            ) {
                Row {
                    Spacer(Modifier.width(clueWidth + CellGap))
                    Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                        for (column in 0 until n) {
                            ClueBox(
                                numbers = state.columnClues[column],
                                done = state.columnDone[column],
                                vertical = true,
                                cell = cell,
                                length = clueHeight,
                                description = stringResource(Res.string.a11y_nonogram_column_clue, column + 1, clueText(state.columnClues[column])),
                                tag = "nonogramColumnClue",
                                fade = { part(reveal.value, CLUES_MILLIS) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(CellGap))
                Row {
                    Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                        for (row in 0 until n) {
                            ClueBox(
                                numbers = state.rowClues[row],
                                done = state.rowDone[row],
                                vertical = false,
                                cell = cell,
                                length = clueWidth,
                                description = stringResource(Res.string.a11y_nonogram_row_clue, row + 1, clueText(state.rowClues[row])),
                                tag = "nonogramRowClue",
                                fade = { part(reveal.value, CLUES_MILLIS) },
                            )
                        }
                    }
                    Spacer(Modifier.width(CellGap))
                    Box(
                        modifier = Modifier
                            .size(boardSide)
                            .pointerInput(n, step) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    if (!playing) return@awaitEachGesture
                                    val start = cellAt(down.position)
                                    // The first cell decides: one that already holds this mark is cleared, any other takes it.
                                    val value = if (marks[start] == modeNow) NonogramMark.EMPTY else modeNow
                                    var end = start
                                    stroke = Stroke(start, end, value)
                                    down.consume()
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.first()
                                        change.consume()
                                        if (!change.pressed) break
                                        end = project(start, change.position)
                                        stroke = Stroke(start, end, value)
                                    }
                                    stroke = null
                                    val before = marks
                                    if (onPaint(start, end, value) != NonogramResult.Ignored) {
                                        haptics.perform(HapticKind.Light)
                                        if (value == NonogramMark.FILLED) playFill(start, end, before)
                                    }
                                }
                            },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                            for (row in 0 until n) {
                                Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                                    for (column in 0 until n) {
                                        val index = row * n + column
                                        val live = stroke?.takeIf { s -> inStroke(s, index, n) }?.value
                                        GuideMark(isGuided(index), Radius.card) {
                                            Cell(
                                                row = row + 1,
                                                column = column + 1,
                                                mark = live ?: state.marks[index],
                                                solved = won,
                                                accent = accent,
                                                size = cell,
                                                morph = { part(reveal.value, MORPH_MILLIS) },
                                                onTap = {
                                                    val value = if (state.marks[index] == mode) NonogramMark.EMPTY else mode
                                                    val before = state.marks
                                                    if (onPaint(index, index, value) != NonogramResult.Ignored && value == NonogramMark.FILLED) playFill(index, index, before)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Read out when a line becomes complete.
            Box(Modifier.size(1.dp).semantics { liveRegion = LiveRegionMode.Polite; contentDescription = completedText })
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                NonogramResultCard(page, language, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.nonogram_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val iconColor = colors.textPrimary
                    val crossMode = mode == NonogramMark.CROSS
                    val crossColor = if (crossMode) colors.pendingFill else colors.textPrimary
                    // Like the pencil of Sudoku: one round button that switches between filling and marking empty.
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl), verticalAlignment = Alignment.CenterVertically) {
                        RoundIconButton(
                            description = stringResource(Res.string.nonogram_mode_cross),
                            onClick = { mode = if (crossMode) NonogramMark.FILLED else NonogramMark.CROSS },
                            selected = crossMode,
                            tag = "nonogramMode",
                        ) { drawCrossMode(crossColor) }
                        RoundIconButton(
                            description = stringResource(Res.string.a11y_nonogram_restart),
                            onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                            enabled = state.canRestart,
                            tag = "nonogramRestart",
                        ) { drawRestart(iconColor) }
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** The cross of a marked cell, as the icon of the mode button. */
private fun DrawScope.drawCrossMode(color: Color) {
    val inset = size.width * 0.3f
    val far = size.width - inset
    val width = size.width * 0.09f
    drawLine(color, Offset(inset, inset), Offset(far, far), strokeWidth = width, cap = StrokeCap.Round)
    drawLine(color, Offset(far, inset), Offset(inset, far), strokeWidth = width, cap = StrokeCap.Round)
}

private fun clueText(numbers: List<Int>): String = if (numbers.isEmpty()) "0" else numbers.joinToString(" ")

/** True if [cell] is on the stroke's row or column stretch (see [Stroke]). */
private fun inStroke(stroke: Stroke, cell: Int, n: Int): Boolean {
    val (r1, c1) = stroke.from / n to stroke.from % n
    val (r2, c2) = stroke.to / n to stroke.to % n
    val (r, c) = cell / n to cell % n
    return when {
        r1 == r2 -> r == r1 && c in minOf(c1, c2)..maxOf(c1, c2)
        else -> c == c1 && r in minOf(r1, r2)..maxOf(r1, r2)
    }
}

/**
 * The numbers of one line, at the end of the row or the top of the column, right next to the cells. They fade when the line matches.
 * Their size follows the cell, not the text size setting: they have to fit.
 */
@Composable
private fun ClueBox(numbers: List<Int>, done: Boolean, vertical: Boolean, cell: Dp, length: Dp, description: String, tag: String, fade: () -> Float) {
    val colors = BrainScrollTheme.colors
    val density = LocalDensity.current
    val color by animateColorAsState(
        if (done) colors.textSecondary.copy(alpha = 0.4f) else colors.textPrimary,
        tween(if (LocalReduceMotion.current) 0 else Motion.BASE, easing = Motion.Standard),
    )
    val style = BrainScrollTheme.typography.label.copy(
        color = color,
        fontSize = with(density) { (cell * 0.4f).toSp() },
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    val shown = numbers.ifEmpty { listOf(0) }
    val size = if (vertical) Modifier.width(cell).height(length) else Modifier.width(length).height(cell)
    val modifier = size.graphicsLayer { alpha = 1f - fade() }
    val semantic = Modifier.testTag(tag).semantics(mergeDescendants = true) { contentDescription = description }
    if (vertical) {
        Column(modifier.then(semantic), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
            shown.forEach { BasicText(it.toString(), style = style) }
        }
    } else {
        Row(modifier.then(semantic), horizontalArrangement = Arrangement.spacedBy(cell * 0.12f, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            shown.forEach { BasicText(it.toString(), style = style) }
        }
    }
}

/**
 * One cell: dark when empty, filled with the green of the game (the solved picture's own color once it is solved), a cross for a
 * cell marked empty. Fill and cross differ in shape, not only in color. A tap from a screen reader works through the click action
 * (the finger itself is handled by the board).
 */
@Composable
private fun Cell(row: Int, column: Int, mark: Int, solved: Boolean, accent: Color, size: Dp, morph: () -> Float, onTap: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val reduce = LocalReduceMotion.current
    val filled = mark == NonogramMark.FILLED
    val fill by animateColorAsState(
        when {
            filled && solved -> accent
            filled -> colors.correctFill
            else -> colors.bgSurface
        },
        tween(if (reduce) 0 else if (solved) COLOR_MILLIS else Motion.FAST, easing = Motion.Standard),
    )
    val description = stringResource(
        when (mark) {
            NonogramMark.FILLED -> Res.string.a11y_nonogram_filled
            NonogramMark.CROSS -> Res.string.a11y_nonogram_cross
            else -> Res.string.a11y_nonogram_empty
        },
        row, column,
    )
    Box(
        modifier = Modifier
            .size(size)
            .testTag("nonogramCell")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                cellValue = mark
                role = Role.Button
                onClick { onTap(); true }
            }
            .graphicsLayer {
                // Once solved, the picture's cells grow by the gap and touch; the others (empty, crossed) fade out.
                val m = if (solved) morph() else 0f
                if (filled) {
                    val grown = (size.toPx() + CellGap.toPx() * m) / size.toPx()
                    scaleX = grown
                    scaleY = grown
                } else {
                    alpha = 1f - m
                }
            }
            .drawBehind {
                val m = if (solved && filled) morph() else 0f
                val corner = lerpFloat(CellRadius.toPx(), PictureRadius.toPx(), m)
                drawRoundRect(fill, cornerRadius = CornerRadius(corner))
                val borderAlpha = 0.3f * (1f - m)
                if (borderAlpha > 0f) {
                    val width = (1f + extraBorder).dp.toPx()
                    drawRoundRect(
                        colors.borderStrong.copy(alpha = borderAlpha),
                        topLeft = Offset(width / 2, width / 2),
                        size = Size(this.size.width - width, this.size.height - width),
                        cornerRadius = CornerRadius((corner - width / 2).coerceAtLeast(0f)),
                        style = StrokeStyle(width),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (mark == NonogramMark.CROSS) {
            val crossColor = colors.textSecondary.copy(alpha = 0.7f)
            Canvas(Modifier.size(size * 0.45f)) {
                val w = this.size.width
                drawLine(crossColor, Offset(0f, 0f), Offset(w, w), strokeWidth = w * 0.18f, cap = StrokeCap.Round)
                drawLine(crossColor, Offset(w, 0f), Offset(0f, w), strokeWidth = w * 0.18f, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun NonogramResultCard(page: NonogramSnapshot, language: String, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val picture = page.state.picture
    val name = picture?.let { if (language == "tr") it.nameTr else it.nameEn }

    Box(modifier = modifier.fillMaxWidth().resultSurface(colors.bgSurface, GameKind.Nonogram.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = if (name != null) stringResource(Res.string.nonogram_won, name.lowercase()) else stringResource(Res.string.nonogram_won_plain),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = accentOf(picture, colors), textAlign = TextAlign.Center),
            )
            ResultStats(
    summary = stringResource(Res.string.nonogram_result, formatDuration(page.durationMs)),
    stats = listOf(
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
