package com.mehmtcan.brainscroll.ui.ballslide

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.drawscope.clipPath
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_ballslide_board
import brainscroll.shared.generated.resources.a11y_ballslide_count
import brainscroll.shared.generated.resources.a11y_ballslide_down
import brainscroll.shared.generated.resources.a11y_ballslide_left
import brainscroll.shared.generated.resources.a11y_ballslide_restart
import brainscroll.shared.generated.resources.a11y_ballslide_right
import brainscroll.shared.generated.resources.a11y_ballslide_undo
import brainscroll.shared.generated.resources.a11y_ballslide_up
import brainscroll.shared.generated.resources.ballslide_best
import brainscroll.shared.generated.resources.ballslide_hint
import brainscroll.shared.generated.resources.ballslide_moves
import brainscroll.shared.generated.resources.ballslide_painted
import brainscroll.shared.generated.resources.ballslide_result
import brainscroll.shared.generated.resources.ballslide_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.ballslide.BallDir
import com.mehmtcan.brainscroll.game.ballslide.BallSlideResult
import com.mehmtcan.brainscroll.game.ballslide.BallSlideSnapshot
import com.mehmtcan.brainscroll.game.ballslide.BallSlideStatus
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.feed.LocalGuide
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.sound.*
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.components.ResultStats
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import brainscroll.shared.generated.resources.result_stat_time
import brainscroll.shared.generated.resources.result_stat_slides
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import com.mehmtcan.brainscroll.ui.tubes.drawUndo
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val CellGap = 4.dp
private val MaxBoard = 380.dp

/** One cell of the way takes this long, and the whole slide at least and at most this long. */
private const val MILLIS_PER_CELL = 70
private const val MIN_SLIDE_MILLIS = 150
private const val MAX_SLIDE_MILLIS = 450

/** How far a finger has to move on the board (in dp) for a swipe to count as a slide. */
private const val SWIPE_DP = 24f

/** Where the ball is (a cell), how many cells are painted, and which cells are walls, for tests. */
internal val BallSlideWalls = SemanticsPropertyKey<List<Boolean>>("BallSlideWalls")
private var SemanticsPropertyReceiver.wallCells by BallSlideWalls
internal val BallSlideBallCell = SemanticsPropertyKey<Int>("BallSlideBallCell")
private var SemanticsPropertyReceiver.ballCell by BallSlideBallCell
internal val BallSlidePainted = SemanticsPropertyKey<Int>("BallSlidePainted")
private var SemanticsPropertyReceiver.paintedCells by BallSlidePainted

/**
 * One full-screen page of the feed: how many cells are painted, the slides made and the fewest there are, the board with its
 * walls and the ball, undo and restart, or the result. A swipe on the board slides the ball until a wall or the edge stops it; it
 * paints the cells it passes. The board is won when every cell is painted. (A screen reader gets the four slides as actions of
 * the board.)
 */
@Composable
fun BallSlidePage(
    page: BallSlideSnapshot,
    onSlide: (Int) -> BallSlideResult,
    onUndo: () -> Boolean,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val sounds = LocalSoundEffects.current
    val reduce = LocalReduceMotion.current
    val state = page.state
    val won = state.status == BallSlideStatus.Won
    val n = state.size

    // The result and the buzz wait until the ball has stopped and the last cells are painted.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, MAX_SLIDE_MILLIS.toLong() + 100)
    // The win has the sound of this game, so the buzz of the outcome is the quiet one.
    OutcomeHaptics(status, page.id, haptics.withoutSound())
    WinSound(status, listOf(WinStep(0, Sfx.BallSlideWinChord, BALLSLIDE_CHORD_VOLUME)))
    val justFinished = rememberJustFinished(status)

    // The slide (or the undo) that is shown: the ball goes along the cells, and a cell is painted (or cleared) when the ball has
    // passed it. [slideMoves] and [undoCount] say which slide and which undo the progress belongs to, so the very first frame of a
    // new one is drawn as it begins and never as it ends. The ball is drawn where it was until an animation ends; a restart puts it
    // back at once.
    // A page that is shown for the first time (scrolled back to) shows the board as it is, its last slide already over.
    val slideProgress = remember { Animatable(state.lastPath.size.toFloat()) }
    val undoProgress = remember { Animatable(state.undone.size.toFloat()) }
    var handledMoves by remember { mutableIntStateOf(state.moves) }
    var slideMoves by remember { mutableIntStateOf(state.moves) }
    var undoCount by remember { mutableIntStateOf(state.undos) }
    var drawnBall by remember { mutableIntStateOf(state.ball) }
    // The cells the ball goes over on its way back: the slide it took back, the other way round, ending on the cell it started from.
    val undoPath = remember(state.undone, state.undoneStart) { state.undone.asReversed().drop(1) + state.undoneStart }
    LaunchedEffect(state.moves, state.undos, state.ball) {
        val path = state.lastPath
        fun duration(cells: Int) = (cells * MILLIS_PER_CELL).coerceIn(MIN_SLIDE_MILLIS, MAX_SLIDE_MILLIS)
        when {
            path.isNotEmpty() -> {
                if (reduce || state.moves == handledMoves) {
                    handledMoves = state.moves
                    slideProgress.snapTo(path.size.toFloat())
                    slideMoves = state.moves
                } else {
                    handledMoves = state.moves
                    slideProgress.snapTo(0f)
                    slideMoves = state.moves
                    // The ball plays the same note whenever it comes to the middle of a cell.
                    var played = 0
                    fun playCells(upTo: Float) {
                        while (played < path.size && upTo >= played + BALLSLIDE_CELL_THRESHOLD) {
                            sounds.play(BALLSLIDE_CELL_SOUND, BALLSLIDE_CELL_VOLUME)
                            played++
                        }
                    }
                    slideProgress.animateTo(path.size.toFloat(), tween(duration(path.size), easing = FastOutSlowInEasing)) { playCells(value) }
                    playCells(path.size.toFloat())
                }
                drawnBall = state.ball
            }
            state.undone.isNotEmpty() && state.undos != undoCount -> {
                if (reduce) {
                    undoProgress.snapTo(undoPath.size.toFloat())
                    undoCount = state.undos
                } else {
                    undoProgress.snapTo(0f)
                    undoCount = state.undos
                    undoProgress.animateTo(undoPath.size.toFloat(), tween(duration(undoPath.size), easing = FastOutSlowInEasing))
                }
                drawnBall = state.ball
            }
            else -> {
                undoCount = state.undos
                drawnBall = state.ball
            }
        }
    }

    val latestSlide by rememberUpdatedState { dir: Int ->
        when (onSlide(dir)) {
            BallSlideResult.Moved, BallSlideResult.Solved -> {
                haptics.perform(HapticKind.Light)
                sounds.play(Sfx.BallSlideWhoosh, BALLSLIDE_WHOOSH_VOLUME)
            }
            BallSlideResult.Ignored -> Unit
        }
    }

    // The way the guide shows: arrows running along the cells of the slide it suggests.
    val guide = LocalGuide.current?.takeIf { it.reason == GuideReason.BallSlideMove && it.numbers.isNotEmpty() }
    val flow by rememberInfiniteTransition(label = "ballSlideGuide").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "flow",
    )

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Chip(
                text = stringResource(Res.string.ballslide_painted, state.paintedCount, state.openCount),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_ballslide_count, state.paintedCount, state.openCount),
            )
            Chip(text = stringResource(Res.string.ballslide_moves, state.moves), textColor = colors.textSecondary)
            Chip(text = stringResource(Res.string.ballslide_best, state.par), textColor = colors.textSecondary)
        }
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side: Dp = minOf(MaxBoard, maxWidth, maxHeight)
            val density = LocalDensity.current
            val cellPx = with(density) { (side / n).toPx() }
            val boardDescription = stringResource(Res.string.a11y_ballslide_board, state.ball / n + 1, state.ball % n + 1)
            val actionLabels = listOf(
                BallDir.UP to stringResource(Res.string.a11y_ballslide_up),
                BallDir.RIGHT to stringResource(Res.string.a11y_ballslide_right),
                BallDir.DOWN to stringResource(Res.string.a11y_ballslide_down),
                BallDir.LEFT to stringResource(Res.string.a11y_ballslide_left),
            )
            val swipePx = with(density) { SWIPE_DP.dp.toPx() }
            val extraBorder = if (LocalHighContrast.current) 1.5f else 0f
            Box(
                modifier = Modifier
                    .size(side)
                    .testTag("ballSlideBoard")
                    .semantics {
                        contentDescription = boardDescription
                        ballCell = state.ball
                        paintedCells = state.paintedCount
                        wallCells = state.walls
                        if (!won) customActions = actionLabels.map { (dir, label) -> CustomAccessibilityAction(label) { latestSlide(dir); true } }
                    }
                    .pointerInput(won) {
                        if (won) return@pointerInput
                        // One slide for each swipe: it fires as soon as the finger has moved far enough, and then waits for the next one.
                        var total = Offset.Zero
                        var fired = false
                        detectDragGestures(
                            onDragStart = { total = Offset.Zero; fired = false },
                            onDragEnd = { fired = true },
                            onDragCancel = { fired = true },
                        ) { change, amount ->
                            change.consume()
                            if (fired) return@detectDragGestures
                            total += amount
                            if (abs(total.x) >= swipePx || abs(total.y) >= swipePx) {
                                fired = true
                                val dir = if (abs(total.x) > abs(total.y)) {
                                    if (total.x > 0) BallDir.RIGHT else BallDir.LEFT
                                } else {
                                    if (total.y > 0) BallDir.DOWN else BallDir.UP
                                }
                                latestSlide(dir)
                            }
                        }
                    },
            ) {
                val tile = colors.bgSurface
                // A painted cell is the color of the squares the line has passed in One Line.
                val paint = colors.correctFill.copy(alpha = 0.16f)
                val edge = colors.borderStrong.copy(alpha = 0.35f)
                val wallBase = ballSlideWallColor(colors)
                val mortar = colors.bgPage.copy(alpha = 0.6f)
                val ballColor = colors.pendingFill
                val cue = colors.pendingFill
                Canvas(Modifier.fillMaxSize()) {
                    val gap = CellGap.toPx() / 2f
                    val corner = CornerRadius(cellPx * 0.18f)
                    fun center(c: Int) = Offset((c % n + 0.5f) * cellPx, (c / n + 0.5f) * cellPx)
                    val path = state.lastPath
                    // The last slide stays in the state after it is over; it is only on show while the ball is still going.
                    val passed = if (path.isNotEmpty() && slideMoves == state.moves) slideProgress.value else 0f
                    val sliding = path.isNotEmpty() && passed < path.size
                    val undoing = !sliding && state.undone.isNotEmpty() && (undoCount != state.undos || undoProgress.value < undoPath.size)
                    val cleared = if (undoing && undoCount == state.undos) undoProgress.value else 0f
                    for (c in 0 until n * n) {
                        val topLeft = Offset((c % n) * cellPx + gap, (c / n) * cellPx + gap)
                        val box = Size(cellPx - 2 * gap, cellPx - 2 * gap)
                        if (state.walls[c]) {
                            drawWall(topLeft, box.width, wallBase, mortar, corner)
                            continue
                        }
                        // Painted: the cells of the slide that is going on wait until the ball has passed them; the cells of the
                        // slide that is being taken back stay painted until the ball has left them.
                        var isPainted = state.painted[c]
                        if (sliding) {
                            val onPath = path.indexOf(c)
                            if (onPath >= 0 && passed <= onPath) isPainted = false
                        } else if (undoing) {
                            val back = state.undone.lastIndex - state.undone.indexOf(c)
                            if (state.undone.contains(c) && cleared <= back) isPainted = true
                        }
                        drawRoundRect(tile, topLeft, box, corner)
                        if (isPainted) drawRoundRect(paint, topLeft, box, corner)
                        drawRoundRect(edge, topLeft, box, corner, style = Stroke(width = 1.5.dp.toPx() + extraBorder))
                    }
                    // The guide: arrows run along the way the ball would go, one cell after the other.
                    if (guide != null && !sliding && !undoing) {
                        val dir = guide.numbers[0]
                        val dx = BallDir.dColumn[dir].toFloat()
                        val dy = BallDir.dRow[dir].toFloat()
                        guide.cells.forEachIndexed { k, c ->
                            val phase = flow * (guide.cells.size + 1.5f) - k
                            val a = if (reduce) 0.85f else (1f - abs(phase - 0.5f)).coerceIn(0f, 1f) * 0.85f + 0.15f
                            val m = center(c)
                            val reach = cellPx * 0.2f
                            val chevron = Path().apply {
                                moveTo(m.x + dx * reach - dy * reach - dx * reach * 1.6f, m.y + dy * reach + dx * reach - dy * reach * 1.6f)
                                lineTo(m.x + dx * reach, m.y + dy * reach)
                                lineTo(m.x + dx * reach + dy * reach - dx * reach * 1.6f, m.y + dy * reach - dx * reach - dy * reach * 1.6f)
                            }
                            drawPath(chevron, cue.copy(alpha = a), style = Stroke(width = cellPx * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                    // The ball: along the cells of the slide (or of the undo) that is going on, else where it stands.
                    val ballAt = when {
                        sliding -> {
                            val whole = passed.toInt().coerceIn(0, path.size - 1)
                            val from = if (whole == 0) center(drawnBall) else center(path[whole - 1])
                            val to = center(path[whole])
                            val f = passed - whole
                            Offset(from.x + (to.x - from.x) * f, from.y + (to.y - from.y) * f)
                        }
                        undoing && cleared < undoPath.size -> {
                            val whole = cleared.toInt().coerceIn(0, undoPath.size - 1)
                            val from = if (whole == 0) center(state.undone.last()) else center(undoPath[whole - 1])
                            val to = center(undoPath[whole])
                            val f = cleared - whole
                            Offset(from.x + (to.x - from.x) * f, from.y + (to.y - from.y) * f)
                        }
                        else -> center(state.ball)
                    }
                    drawCircle(ballColor, cellPx * 0.3f, ballAt)
                    drawCircle(Color.White.copy(alpha = 0.55f), cellPx * 0.09f, Offset(ballAt.x - cellPx * 0.1f, ballAt.y - cellPx * 0.1f))
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                BallSlideResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.ballslide_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        RoundIconButton(
                            description = stringResource(Res.string.a11y_ballslide_undo),
                            onClick = { if (onUndo()) haptics.perform(HapticKind.Light) },
                            enabled = state.canUndo,
                            tag = "ballSlideUndo",
                        ) { drawUndo(ink) }
                        RoundIconButton(
                            description = stringResource(Res.string.a11y_ballslide_restart),
                            onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                            enabled = state.canRestart,
                            tag = "ballSlideRestart",
                        ) { drawRestart(ink) }
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** The color of a wall: an orange that is on no cell of the board (the cells are dark or the green of the paint, the ball is yellow). */
internal fun ballSlideWallColor(@Suppress("UNUSED_PARAMETER") colors: com.mehmtcan.brainscroll.ui.theme.BrainScrollColors): Color = Color(0xFFC77A35)

/**
 * A wall: a block of bricks (three courses, the joints of the middle one shifted) with a light top edge and a dark bottom edge,
 * so it looks solid and raised, and nothing else on the board looks like it. [side] is the size of the cell's block.
 */
internal fun DrawScope.drawWall(topLeft: Offset, side: Float, base: Color, mortar: Color, corner: CornerRadius, alpha: Float = 1f) {
    val block = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(topLeft, Size(side, side)), corner)) }
    clipPath(block) {
        drawRect(base.copy(alpha = base.alpha * alpha), topLeft, Size(side, side))
        val course = side / 3f
        val joint = side * 0.045f
        val line = mortar.copy(alpha = mortar.alpha * alpha)
        for (row in 1..2) drawRect(line, Offset(topLeft.x, topLeft.y + row * course - joint / 2f), Size(side, joint))
        val joints = listOf(listOf(0.5f), listOf(0.25f, 0.75f), listOf(0.5f))
        joints.forEachIndexed { row, xs ->
            for (x in xs) drawRect(line, Offset(topLeft.x + side * x - joint / 2f, topLeft.y + row * course), Size(joint, course))
        }
        // The light edge on top and the dark edge at the bottom.
        drawRect(Color.White.copy(alpha = 0.22f * alpha), topLeft, Size(side, side * 0.07f))
        drawRect(Color.Black.copy(alpha = 0.3f * alpha), Offset(topLeft.x, topLeft.y + side * 0.9f), Size(side, side * 0.1f))
    }
}

@Composable
private fun BallSlideResultCard(page: BallSlideSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().resultSurface(colors.bgSurface, GameKind.BallSlide.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.ballslide_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
    summary = stringResource(Res.string.ballslide_result, page.state.moves, formatDuration(page.durationMs)),
    stats = listOf(
        ResultStatData(page.state.moves.toString(), stringResource(Res.string.result_stat_slides), GameKind.BallSlide.world),
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
