package com.mehmtcan.brainscroll.ui.crossword

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.lerp as lerpOffset
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_crossword_cell_empty
import brainscroll.shared.generated.resources.a11y_crossword_cell_letter
import brainscroll.shared.generated.resources.a11y_crossword_cell_wrong
import brainscroll.shared.generated.resources.a11y_crossword_clue_across
import brainscroll.shared.generated.resources.a11y_crossword_clue_down
import brainscroll.shared.generated.resources.a11y_crossword_next
import brainscroll.shared.generated.resources.a11y_crossword_zoom_out
import brainscroll.shared.generated.resources.a11y_sudoku_mistakes
import brainscroll.shared.generated.resources.crossword_clue_line
import brainscroll.shared.generated.resources.crossword_next
import brainscroll.shared.generated.resources.crossword_share
import brainscroll.shared.generated.resources.crossword_share_copied
import brainscroll.shared.generated.resources.crossword_result
import brainscroll.shared.generated.resources.crossword_won
import brainscroll.shared.generated.resources.crossword_zoom_hint
import brainscroll.shared.generated.resources.sudoku_mistakes
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.crossword.CrosswordClue
import com.mehmtcan.brainscroll.game.crossword.CrosswordResult
import com.mehmtcan.brainscroll.game.crossword.CrosswordSnapshot
import com.mehmtcan.brainscroll.game.crossword.CrosswordStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.AppTextButton
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.Stars
import com.mehmtcan.brainscroll.ui.components.StarsBar
import com.mehmtcan.brainscroll.ui.components.StarsInfoSheet
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.feed.GuideMark
import com.mehmtcan.brainscroll.ui.feed.LocalDailyPage
import com.mehmtcan.brainscroll.ui.feed.LocalGuide
import com.mehmtcan.brainscroll.ui.feed.isGuided
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.motion.popOnAppear
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.wordle.WordleKeyboard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor
import kotlin.math.min

private val CellGap = 2.dp

/** The size of a square when the board is zoomed in: big enough to read the clues. The board is drawn at this size and scaled down to fit. */
private val ZoomCell = 56.dp

/** Where the clue of the answer you are on is written, under the board: tall enough for two lines, so nothing below it moves. */
private val BannerHeight = 52.dp
private val CellShape = RoundedCornerShape(4.dp)

/**
 * What a square shows, for tests: "#" for a clue square, "." for an empty letter square, the capital letter for a letter, and "!"
 * before the letter if it is wrong.
 */
internal val CrosswordSquareInfo = SemanticsPropertyKey<String>("CrosswordSquareInfo")
private var SemanticsPropertyReceiver.squareInfo by CrosswordSquareInfo

/** The clues of a clue square, for tests: "A:" or "D:" (across or down) and then the text. */
internal val CrosswordClueTexts = SemanticsPropertyKey<List<String>>("CrosswordClueTexts")
private var SemanticsPropertyReceiver.clueTexts by CrosswordClueTexts

/** The squares of the answer that is picked, in order, and the square the cursor is in (the last of the list), for tests. */
internal val CrosswordPicked = SemanticsPropertyKey<List<Int>>("CrosswordPicked")
private var SemanticsPropertyReceiver.pickedCells by CrosswordPicked

/**
 * The line to copy for sharing the result (see [com.mehmtcan.brainscroll.game.crossword.CrosswordShare]); null where there is no
 * sharing (a board that is not the daily one). The daily screen provides it once the board is solved.
 */
val LocalCrosswordShare = compositionLocalOf<String?> { null }

/** Whether the board is zoomed in ("zoomed") or shows the whole board ("overview"), for tests. */
internal val CrosswordView = SemanticsPropertyKey<String>("CrosswordView")
private var SemanticsPropertyReceiver.view by CrosswordView

/**
 * One full-screen page of the feed: the mistake counter, the board with its clue squares, the clue of the answer you are on, and the
 * keyboard or the result. The board is 16 by 16 squares and starts as a far view of the whole board. A double tap zooms in on the
 * spot (to a fixed size, where the clues can be read); a finger drags the board about while it is zoomed in, a tap picks a letter
 * square (and its answer: tap again to swap across and down) and the keyboard types. A double tap, or the button at the corner,
 * zooms out again.
 */
@Composable
fun CrosswordPage(
    page: CrosswordSnapshot,
    onPlace: (cell: Int, letter: Char) -> CrosswordResult,
    onErase: (cell: Int) -> CrosswordResult,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val haptics = LocalHaptics.current
    val reduce = LocalReduceMotion.current
    val state = page.state
    val won = state.status == CrosswordStatus.Won
    // Only the daily puzzle gives stars; in the feed the board just counts mistakes.
    val daily = LocalDailyPage.current
    var starsInfo by remember { mutableStateOf(false) }

    // The result, the buzz and the card wait until the last letter has settled into its square.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, Motion.POP.toLong() + 50)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    // The square the cursor is in, and the way of the answer that is picked. They start at the first square that is still empty.
    val start = state.letters.indices.firstOrNull { !state.isClue[it] && state.letters[it] == ' ' }
        ?: state.letters.indices.first { !state.isClue[it] }
    var selected by remember(page.id) { mutableStateOf(start) }
    var across by remember(page.id) { mutableStateOf(state.clues.firstOrNull { start in it.cells }?.across ?: true) }

    val clueOf = { cell: Int, way: Boolean -> state.clues.firstOrNull { cell in it.cells && it.across == way } }
    val picked: CrosswordClue? = clueOf(selected, across) ?: state.clues.firstOrNull { selected in it.cells }

    // The keys of the keyboard keep the functions they were given, so the functions read the board as it is when a key is pressed,
    // and not as it was when they were made.
    val live by rememberUpdatedState(state)
    fun pickedNow(): CrosswordClue? = live.clues.firstOrNull { selected in it.cells && it.across == across } ?: live.clues.firstOrNull { selected in it.cells }

    fun select(cell: Int, way: Boolean) {
        selected = cell
        across = way
    }

    /** The first square of [clue] that is not right yet, leaving out [skip]. */
    fun open(clue: CrosswordClue, skip: Int? = null): Int? = clue.cells.firstOrNull { it !in live.correct && it != skip }

    fun nextAnswer(skip: Int? = null) {
        val from = live.clues.indexOf(pickedNow()).coerceAtLeast(0)
        for (step in 1..live.clues.size) {
            val clue = live.clues[(from + step) % live.clues.size]
            val cell = open(clue, skip) ?: continue
            select(cell, clue.across)
            return
        }
    }

    fun tapSquare(cell: Int) {
        val here = live.clues.filter { cell in it.cells }
        if (here.isEmpty()) return
        when {
            cell == selected && here.size == 2 -> across = !across
            else -> select(cell, if (here.any { it.across == across }) across else here.first().across)
        }
    }

    fun letter(letter: Char) {
        val cell = selected
        when (onPlace(cell, letter)) {
            CrosswordResult.Correct -> {
                haptics.perform(HapticKind.Medium)
                // On to the next square of the answer that is still open; when it is done, to the next answer.
                val here = pickedNow()
                val after = here?.cells?.let { it.drop(it.indexOf(cell) + 1) }.orEmpty().firstOrNull { it !in live.correct && it != cell }
                if (after != null) selected = after else if (here != null) {
                    val anyLeft = here.cells.firstOrNull { it !in live.correct && it != cell }
                    if (anyLeft != null) selected = anyLeft else nextAnswer(skip = cell)
                }
            }
            CrosswordResult.Mistake -> haptics.perform(HapticKind.Warning)
            else -> Unit
        }
    }

    fun backspace() {
        val cell = selected
        if (cell in live.wrong) {
            if (onErase(cell) == CrosswordResult.Erased) haptics.perform(HapticKind.Light)
        } else {
            // Nothing to take back here: go to the square before it, and take back a wrong letter there.
            val clue = pickedNow() ?: return
            val before = clue.cells.getOrNull(clue.cells.indexOf(cell) - 1) ?: return
            selected = before
            if (before in live.wrong) onErase(before)
        }
    }

    // ---- The view of the board: how far it is zoomed in and where it has been dragged to. All lengths here are in pixels.
    val n = state.size
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val cellPx = with(density) { ZoomCell.toPx() }
    val gapPx = with(density) { CellGap.toPx() }
    val stride = cellPx + gapPx
    val boardPx = n * cellPx + (n - 1) * gapPx
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var zoomed by remember(page.id) { mutableStateOf(false) }
    // 0 is the far view, 1 is zoomed in; the board is scaled between the two.
    val progress = remember(page.id) { Animatable(0f) }
    // Where the board is when zoomed in: the place of its top left corner in the view.
    val pan = remember(page.id) { Animatable(Offset.Zero, Offset.VectorConverter) }

    val vw = viewport.width.toFloat()
    val vh = viewport.height.toFloat()
    val fit = if (viewport.width == 0) 1f else min(vw / boardPx, vh / boardPx)

    fun overviewShift() = Offset((vw - boardPx * fit) / 2f, (vh - boardPx * fit) / 2f)
    fun scaleNow() = fit + (1f - fit) * progress.value
    fun shiftNow() = lerpOffset(overviewShift(), pan.value, progress.value)
    fun clampPan(o: Offset): Offset = Offset(
        if (boardPx <= vw) (vw - boardPx) / 2f else o.x.coerceIn(vw - boardPx, 0f),
        if (boardPx <= vh) (vh - boardPx) / 2f else o.y.coerceIn(vh - boardPx, 0f),
    )
    fun centered(point: Offset) = clampPan(Offset(vw / 2f - point.x, vh / 2f - point.y))
    fun centerOf(cell: Int) = Offset((cell % n) * stride + cellPx / 2f, (cell / n) * stride + cellPx / 2f)

    /** The letter square at a point of the board (in the board's own pixels), or null for a gap or a clue square. */
    fun squareAt(point: Offset): Int? {
        val column = floor(point.x / stride).toInt()
        val row = floor(point.y / stride).toInt()
        if (column !in 0 until n || row !in 0 until n) return null
        if (point.x - column * stride > cellPx || point.y - row * stride > cellPx) return null
        return (row * n + column).takeIf { !state.isClue[it] }
    }

    fun zoomIn(point: Offset) {
        scope.launch {
            if (!zoomed) {
                pan.snapTo(centered(point))
                zoomed = true
                if (reduce) progress.snapTo(1f) else progress.animateTo(1f, tween(Motion.SLOW, easing = Motion.Standard))
            } else if (reduce) {
                pan.snapTo(centered(point))
            } else {
                pan.animateTo(centered(point), tween(Motion.BASE, easing = Motion.Standard))
            }
        }
    }

    fun zoomOut() {
        scope.launch {
            zoomed = false
            if (reduce) progress.snapTo(0f) else progress.animateTo(0f, tween(Motion.SLOW, easing = Motion.Standard))
        }
    }

    // The cursor stays in view while it moves along an answer.
    LaunchedEffect(selected, zoomed, viewport) {
        if (!zoomed || viewport.width == 0) return@LaunchedEffect
        val c = centerOf(selected)
        val margin = stride * 0.9f
        val p = pan.value
        var x = p.x
        var y = p.y
        if (c.x - stride / 2f + p.x < margin) x = margin - (c.x - stride / 2f)
        if (c.x + stride / 2f + p.x > vw - margin) x = vw - margin - (c.x + stride / 2f)
        if (c.y - stride / 2f + p.y < margin) y = margin - (c.y - stride / 2f)
        if (c.y + stride / 2f + p.y > vh - margin) y = vh - margin - (c.y + stride / 2f)
        val target = clampPan(Offset(x, y))
        if (target != p) {
            if (reduce) pan.snapTo(target) else pan.animateTo(target, tween(Motion.BASE, easing = Motion.Standard))
        }
    }

    // The guide points at a square: show it.
    val guided = LocalGuide.current?.cells?.firstOrNull()
    LaunchedEffect(guided, viewport) {
        if (guided != null && viewport.width > 0 && guided in state.isClue.indices && !state.isClue[guided]) {
            tapSquare(guided)
            zoomIn(centerOf(guided))
        }
    }

    // A solved board shows itself whole.
    LaunchedEffect(status) {
        if (status == WordleStatus.Won && zoomed) zoomOut()
    }

    val zoomedNow by rememberUpdatedState(zoomed)
    val wonNow by rememberUpdatedState(won)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (daily) {
            // The stars the board is worth right now, and the way to find out how they are earned.
            Spacer(Modifier.height(Spacing.xs))
            StarsBar(stars = page.stars, onInfo = { starsInfo = true })
        } else {
            Spacer(Modifier.height(Spacing.sm))
            Spacer(Modifier.height(Spacing.sm))
        }
        Chip(
            text = stringResource(Res.string.sudoku_mistakes, state.mistakes),
            textColor = if (state.mistakes > 0) colors.error else colors.textSecondary,
            description = stringResource(Res.string.a11y_sudoku_mistakes, state.mistakes),
        )
        Spacer(Modifier.height(Spacing.md))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("crosswordBoard")
                .semantics { view = if (zoomed) "zoomed" else "overview" }
                .clipToBounds()
                .onSizeChanged { viewport = it }
                .pointerInput(page.id, boardPx, fit) {
                    detectTapGestures(
                        onDoubleTap = { at ->
                            if (zoomedNow) {
                                zoomOut()
                            } else {
                                val shift = overviewShift()
                                val point = Offset((at.x - shift.x) / fit, (at.y - shift.y) / fit)
                                squareAt(point)?.let { tapSquare(it) }
                                zoomIn(point)
                            }
                        },
                        onTap = { at ->
                            if (zoomedNow && !wonNow) {
                                val shift = shiftNow()
                                val scale = scaleNow()
                                squareAt(Offset((at.x - shift.x) / scale, (at.y - shift.y) / scale))?.let { tapSquare(it) }
                            }
                        },
                    )
                }
                .pointerInput(page.id, boardPx, fit) {
                    detectDragGestures { change, drag ->
                        if (zoomedNow) {
                            change.consume()
                            scope.launch { pan.snapTo(clampPan(pan.value + drag)) }
                        }
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0f)
                        val s = scaleNow()
                        val shift = shiftNow()
                        scaleX = s
                        scaleY = s
                        translationX = shift.x
                        translationY = shift.y
                    }
                    .semantics { pickedCells = (picked?.cells ?: emptyList()).filter { it != selected } + selected },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                    for (row in 0 until n) {
                        Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                            for (column in 0 until n) {
                                val cell = row * n + column
                                if (state.isClue[cell]) {
                                    ClueSquare(state.clues.filter { it.cell == cell }, ZoomCell)
                                } else {
                                    GuideMark(isGuided(cell), CellShape) {
                                        LetterSquare(
                                            row = row + 1,
                                            column = column + 1,
                                            letter = state.letters[cell],
                                            wrong = cell in state.wrong,
                                            isSelected = cell == selected,
                                            inPicked = picked?.let { cell in it.cells } == true,
                                            playing = !won,
                                            size = ZoomCell,
                                            onClick = {
                                                tapSquare(cell)
                                                zoomIn(centerOf(cell))
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (zoomed) ZoomOutButton(onClick = ::zoomOut)
        }

        Spacer(Modifier.height(Spacing.sm))
        Box(modifier = Modifier.fillMaxWidth().height(BannerHeight), contentAlignment = Alignment.Center) {
            if (status != WordleStatus.Won && zoomed) picked?.let { Banner(it) }
        }
        Spacer(Modifier.height(Spacing.sm))
        // The same height as the keyboard of the word puzzle, so the board does not jump between the two.
        Box(modifier = Modifier.fillMaxWidth().height(152.dp), contentAlignment = Alignment.Center) {
            when {
                status == WordleStatus.Won -> CrosswordResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
                zoomed -> WordleKeyboard(
                    language = page.language,
                    letterStates = emptyMap(),
                    enterLabel = stringResource(Res.string.crossword_next),
                    onLetter = ::letter,
                    onBackspace = ::backspace,
                    onEnter = { nextAnswer() },
                    enterDescriptionText = stringResource(Res.string.a11y_crossword_next),
                )
                else -> BasicText(
                    text = stringResource(Res.string.crossword_zoom_hint),
                    modifier = Modifier.testTag("crosswordZoomHint"),
                    style = BrainScrollTheme.typography.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
    }
    if (starsInfo) StarsInfoSheet(onClose = { starsInfo = false })
}

/** The small round button at the corner of the board that zooms out. */
@Composable
private fun BoxScope.ZoomOutButton(onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val description = stringResource(Res.string.a11y_crossword_zoom_out)
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(40.dp)
            .testTag("crosswordZoomOut")
            .background(colors.bgSurface.copy(alpha = 0.92f), RoundedCornerShape(50))
            .border(1.dp, colors.borderStrong, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp)) {
            // A minus: the board gets smaller.
            drawLine(colors.textPrimary, Offset(size.width * 0.1f, size.height / 2f), Offset(size.width * 0.9f, size.height / 2f), 2.5.dp.toPx())
        }
    }
}

/** The clue of the answer that is picked, with the arrow of its way and its length, under the board. */
@Composable
private fun Banner(clue: CrosswordClue) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val description = stringResource(if (clue.across) Res.string.a11y_crossword_clue_across else Res.string.a11y_crossword_clue_down, clue.cells.size, clue.text)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("crosswordBanner")
            .background(colors.bgSurface, Radius.control)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Canvas(Modifier.size(16.dp)) { drawArrow(colors.streakDay, clue.across) }
        BasicText(
            text = stringResource(Res.string.crossword_clue_line, clue.text, clue.cells.size),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = type.body.copy(color = colors.textPrimary),
        )
    }
}

/** A short line and then an arrowhead, pointing right ([across]) or down, the way an answer runs. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(color: Color, across: Boolean) {
    val w = size.width
    val h = size.height
    val stroke = 1.3.dp.toPx()
    val head = Path()
    if (across) {
        drawLine(color, Offset(0f, h * 0.5f), Offset(w * 0.55f, h * 0.5f), stroke)
        head.moveTo(w * 0.5f, h * 0.12f); head.lineTo(w, h * 0.5f); head.lineTo(w * 0.5f, h * 0.88f)
    } else {
        drawLine(color, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h * 0.55f), stroke)
        head.moveTo(w * 0.12f, h * 0.5f); head.lineTo(w * 0.5f, h); head.lineTo(w * 0.88f, h * 0.5f)
    }
    head.close()
    drawPath(head, color)
}

/** An arrow that turns once: down and then right ([across]), or right and then down. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawElbowArrow(color: Color, across: Boolean) {
    val w = size.width
    val h = size.height
    val stroke = 1.3.dp.toPx()
    val head = Path()
    if (across) {
        drawLine(color, Offset(w * 0.2f, 0f), Offset(w * 0.2f, h * 0.78f), stroke)
        drawLine(color, Offset(w * 0.2f, h * 0.78f), Offset(w * 0.6f, h * 0.78f), stroke)
        head.moveTo(w * 0.55f, h * 0.45f); head.lineTo(w, h * 0.78f); head.lineTo(w * 0.55f, h * 1.0f)
    } else {
        drawLine(color, Offset(0f, h * 0.2f), Offset(w * 0.78f, h * 0.2f), stroke)
        drawLine(color, Offset(w * 0.78f, h * 0.2f), Offset(w * 0.78f, h * 0.6f), stroke)
        head.moveTo(w * 0.45f, h * 0.55f); head.lineTo(w * 0.78f, h); head.lineTo(w * 1.0f, h * 0.55f)
    }
    head.close()
    drawPath(head, color)
}

/** True if the arrow of [clue] sits on the right edge of its square (the answer starts in the square to the right), false for the bottom edge. */
private val CrosswordClue.arrowOnRight: Boolean get() = across != bent

/**
 * A square that holds clues: one clue fills the square, two share it, one above the other. The arrow of a clue sits on the edge next to
 * the first letter of its answer (the right edge or the bottom edge) and points the way the answer runs. The text is sized by the
 * square and by its length, so it fits.
 */
@Composable
private fun ClueSquare(clues: List<CrosswordClue>, size: Dp) {
    val colors = BrainScrollTheme.colors
    val ordered = clues.sortedWith(compareByDescending<CrosswordClue> { it.across }.thenBy { it.bent })
    val texts = ordered.map { (if (it.across) "A:" else "D:") + it.text }
    val description = ordered.map { stringResource(if (it.across) Res.string.a11y_crossword_clue_across else Res.string.a11y_crossword_clue_down, it.cells.size, it.text) }.joinToString(". ")
    Column(
        modifier = Modifier
            .size(size)
            .testTag("crosswordSquare")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                squareInfo = "#"
                clueTexts = texts
            }
            .background(lerp(colors.bgSurface, colors.borderStrong, 0.2f), CellShape),
    ) {
        ordered.forEachIndexed { index, clue ->
            // A thin line between the two clues of a square.
            if (index > 0) Box(Modifier.padding(horizontal = 4.dp).fillMaxWidth().height(1.dp).background(colors.textSecondary.copy(alpha = 0.45f)))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                ClueText(clue, size, double = ordered.size == 2)
            }
        }
    }
}

@Composable
private fun ClueText(clue: CrosswordClue, size: Dp, double: Boolean) {
    val colors = BrainScrollTheme.colors
    val density = LocalDensity.current
    // The size follows the square and the length of the text, not the text size setting: it has to fit its square.
    // A word must not be broken in the middle: the longest word of the clue decides how small the text has to be.
    val longest = clue.text.split(' ').maxOf { it.length }
    val factor = (if (double) 0.14f else 0.17f) * when {
        clue.text.length > 22 -> 0.85f
        clue.text.length > 15 -> 0.92f
        else -> 1f
    } * when {
        longest > 10 -> 0.72f
        longest > 8 -> 0.85f
        else -> 1f
    }
    val fontSize = with(density) { (size * factor).toSp() }
    val onRight = clue.arrowOnRight
    Box(Modifier.fillMaxSize()) {
        // Room for the arrow: on the right edge or on the bottom edge.
        val end = if (onRight) size * 0.13f else 0.dp
        val bottom = if (onRight) 0.dp else size * 0.13f
        BasicText(
            text = clue.text,
            modifier = Modifier.align(Alignment.Center).padding(start = 2.dp, end = end + 2.dp, top = 1.dp, bottom = bottom + if (double) 0.dp else 1.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = BrainScrollTheme.typography.caption.copy(
                color = colors.textPrimary,
                fontSize = fontSize,
                lineHeight = fontSize * 1.08f,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
        )
        if (clue.bent) {
            // The answer starts in the square below (going across) or to the right (going down): the arrow goes down and then right, or right and then down.
            Canvas(Modifier.size(size * 0.17f).align(if (onRight) Alignment.CenterEnd else Alignment.BottomCenter)) { drawElbowArrow(colors.streakDay, clue.across) }
        } else {
            val arrowSize = size * 0.17f
            Canvas(Modifier.size(arrowSize).align(if (onRight) Alignment.CenterEnd else Alignment.BottomCenter)) { drawArrow(colors.streakDay, clue.across) }
        }
    }
}

/**
 * A square for one letter. The squares of the answer that is picked are a little lighter, the square of the cursor has a yellow outline.
 * It takes no touches of its own (the board tells which square a tap hit), but a screen reader can press it: that zooms in on it.
 */
@Composable
private fun LetterSquare(
    row: Int,
    column: Int,
    letter: Char,
    wrong: Boolean,
    isSelected: Boolean,
    inPicked: Boolean,
    playing: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val density = LocalDensity.current
    val has = letter != ' '
    val description = when {
        wrong -> stringResource(Res.string.a11y_crossword_cell_wrong, row, column, letter.toString())
        has -> stringResource(Res.string.a11y_crossword_cell_letter, row, column, letter.toString())
        else -> stringResource(Res.string.a11y_crossword_cell_empty, row, column)
    }
    val background = when {
        wrong -> colors.error.copy(alpha = 0.28f)
        inPicked -> lerp(colors.bgRaised, colors.borderStrong, 0.12f)
        else -> colors.bgSurface
    }
    Box(
        modifier = Modifier
            .size(size)
            .testTag("crosswordSquare")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                squareInfo = if (!has) "." else if (wrong) "!$letter" else letter.toString()
                role = Role.Button
                this.selected = isSelected
                if (playing) onClick { onClick(); true }
            }
            .background(background, CellShape)
            .then(if (isSelected) Modifier.border((2.5f + extraBorder).dp, colors.pendingFill, CellShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        // Always there, so a letter that appears can pop into its square.
        Box(Modifier.popOnAppear(has), contentAlignment = Alignment.Center) {
            if (has) {
                BasicText(
                    text = letter.toString(),
                    style = BrainScrollTheme.typography.title.copy(
                        color = if (wrong) colors.error else colors.textPrimary,
                        fontSize = with(density) { (size * 0.52f).toSp() },
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CrosswordResultCard(page: CrosswordSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.crossword_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            if (LocalDailyPage.current) Stars(page.stars, size = 24.dp)
            BasicText(
                text = stringResource(Res.string.crossword_result, formatDuration(page.durationMs)),
                style = type.body.copy(color = colors.textPrimary),
            )
            val share = LocalCrosswordShare.current
            if (share != null) {
                val clipboard = LocalClipboardManager.current
                var copied by remember { mutableStateOf(false) }
                AppTextButton(
                    text = stringResource(if (copied) Res.string.crossword_share_copied else Res.string.crossword_share),
                    onClick = { clipboard.setText(AnnotatedString(share)); copied = true },
                    modifier = Modifier.testTag("crosswordShare"),
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
