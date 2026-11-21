package com.mehmtcan.brainscroll.ui.lasers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_lasers_board
import brainscroll.shared.generated.resources.a11y_lasers_restart
import brainscroll.shared.generated.resources.lasers_hint
import brainscroll.shared.generated.resources.a11y_lasers_turn_mirror
import brainscroll.shared.generated.resources.lasers_lit
import brainscroll.shared.generated.resources.lasers_result
import brainscroll.shared.generated.resources.lasers_won
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.lasers.Beam
import com.mehmtcan.brainscroll.game.lasers.BeamPoint
import com.mehmtcan.brainscroll.game.lasers.LaserResult
import com.mehmtcan.brainscroll.game.lasers.LaserRules
import com.mehmtcan.brainscroll.game.lasers.LaserSnapshot
import com.mehmtcan.brainscroll.game.lasers.LaserStatus
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
import com.mehmtcan.brainscroll.ui.sound.withoutSound
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.hypot

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
/* At least this high; a result card that needs more (big text) makes it higher and the board smaller. */
private val BottomAreaHeight = 152.dp
private val MaxBoard = 420.dp

/** How long the beam takes to run its new way when a mirror is turned. */
private const val BEAM_MILLIS = 380

// The board is drawn in cells: the same room on the left (where the laser is) and on the right, so the grid sits in the middle.
private const val PAD_LEFT = 0.85f
private const val PAD_RIGHT = 0.85f
private const val PAD_Y = 0.35f

/** The mirror of each cell (0, 1 for `/`, 2 for `\`), the cells of the targets and the row of the laser, for tests. */
internal val LaserMirrorsKey = SemanticsPropertyKey<List<Int>>("LaserMirrors")
private var SemanticsPropertyReceiver.mirrorList by LaserMirrorsKey
internal val LaserTargetsKey = SemanticsPropertyKey<List<Int>>("LaserTargets")
private var SemanticsPropertyReceiver.targetList by LaserTargetsKey
internal val LaserRowKey = SemanticsPropertyKey<Int>("LaserRow")
private var SemanticsPropertyReceiver.laserRow by LaserRowKey
internal val LaserLitKey = SemanticsPropertyKey<Int>("LaserLit")
private var SemanticsPropertyReceiver.litTargets by LaserLitKey

/** The yellow of the targets (it stands out from the blue mirrors and the red beam). */
internal val LaserTargetColor = Color(0xFFFFC93C)

/** The colors of the board. */
internal class LaserPaint(
    val tile: Color,
    val mirrorTile: Color,
    val edge: Color,
    val mirror: Color,
    val beam: Color,
    val beamCore: Color,
    val target: Color,
    val emitter: Color,
    val guide: Color,
)

/** The middle of [cell] in pixels on a board [widthPx] wide, for tests. */
internal fun laserCellCenter(cell: Int, size: Int, widthPx: Float): Offset {
    val unit = widthPx / (size + PAD_LEFT + PAD_RIGHT)
    return Offset((PAD_LEFT + cell % size + 0.5f) * unit, (PAD_Y + cell / size + 0.5f) * unit)
}

/** How far along the beam (in cells) it reaches the middle of [cell], or null if it never does. */
internal fun beamReach(beam: Beam, size: Int, cell: Int): Float? {
    val cx = cell % size + 0.5f
    val cy = cell / size + 0.5f
    var travelled = 0f
    for (i in 1 until beam.points.size) {
        val a = beam.points[i - 1]
        val b = beam.points[i]
        val length = hypot(b.x - a.x, b.y - a.y)
        val onLine = abs((b.x - a.x) * (cy - a.y) - (b.y - a.y) * (cx - a.x)) < 0.001f
        val within = cx >= minOf(a.x, b.x) - 0.001f && cx <= maxOf(a.x, b.x) + 0.001f && cy >= minOf(a.y, b.y) - 0.001f && cy <= maxOf(a.y, b.y) + 0.001f
        if (onLine && within) return travelled + hypot(cx - a.x, cy - a.y)
        travelled += length
    }
    return null
}

private fun beamLength(beam: Beam): Float = (1 until beam.points.size).sumOf {
    hypot(beam.points[it].x - beam.points[it - 1].x, beam.points[it].y - beam.points[it - 1].y).toDouble()
}.toFloat()

private fun DrawScope.drawBeam(points: List<BeamPoint>, origin: Offset, cell: Float, progress: Float, color: Color, core: Color, alpha: Float) {
    if (points.size < 2 || progress <= 0f) return
    val path = Path().apply {
        moveTo(origin.x + points[0].x * cell, origin.y + points[0].y * cell)
        for (i in 1 until points.size) lineTo(origin.x + points[i].x * cell, origin.y + points[i].y * cell)
    }
    val shown = if (progress >= 1f) path else Path().also { part ->
        val measure = PathMeasure()
        measure.setPath(path, false)
        measure.getSegment(0f, measure.length * progress, part, true)
    }
    // A wide faint line, a narrower one and a thin bright core: a glow without a blur.
    drawPath(shown, color.copy(alpha = 0.16f * alpha), style = Stroke(cell * 0.28f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(shown, color.copy(alpha = 0.4f * alpha), style = Stroke(cell * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(shown, core.copy(alpha = alpha), style = Stroke(cell * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/**
 * Draws a board of mirrors the way the page does, for the page, the thumbnail and the demo: the cells, the laser, the beam up to
 * [beamProgress] of its length (a target is lit once the beam has reached it), the mirrors, and a ring on [guideCell] if there is one.
 */
internal fun DrawScope.drawLaserBoard(
    size: Int,
    row: Int,
    mirrors: List<Int>,
    targets: Set<Int>,
    beam: Beam,
    origin: Offset,
    cell: Float,
    paint: LaserPaint,
    beamProgress: Float = 1f,
    guideCell: Int = -1,
    guidePulse: Float = 0f,
    alpha: Float = 1f,
) {
    val gap = cell * 0.04f
    val corner = CornerRadius(cell * 0.14f)
    val reached = beamLength(beam) * beamProgress
    for (r in 0 until size) for (c in 0 until size) {
        val k = r * size + c
        val topLeft = Offset(origin.x + c * cell + gap, origin.y + r * cell + gap)
        val box = Size(cell - 2 * gap, cell - 2 * gap)
        drawRoundRect((if (mirrors[k] != 0) paint.mirrorTile else paint.tile).copy(alpha = alpha), topLeft, box, corner)
        drawRoundRect(paint.edge.copy(alpha = paint.edge.alpha * alpha), topLeft, box, corner, style = Stroke(width = cell * 0.03f))
    }
    // The laser.
    drawRoundRect(
        paint.emitter.copy(alpha = alpha),
        Offset(origin.x - 0.8f * cell, origin.y + (row + 0.29f) * cell),
        Size(0.44f * cell, 0.42f * cell),
        CornerRadius(cell * 0.1f),
    )
    drawBeam(beam.points, origin, cell, beamProgress, paint.beam, paint.beamCore, alpha)
    for (t in targets) {
        val center = Offset(origin.x + (t % size + 0.5f) * cell, origin.y + (t / size + 0.5f) * cell)
        val lit = beamReach(beam, size, t)?.let { it <= reached + 0.001f } == true
        if (lit) {
            drawCircle(paint.target.copy(alpha = 0.22f * alpha), cell * 0.36f, center)
            drawCircle(paint.target.copy(alpha = alpha), cell * 0.2f, center)
        } else {
            drawCircle(paint.target.copy(alpha = 0.9f * alpha), cell * 0.2f, center, style = Stroke(width = cell * 0.065f))
        }
    }
    for (k in mirrors.indices) {
        val m = mirrors[k]
        if (m == 0) continue
        val c = k % size
        val r = k / size
        val (a, b) = if (m == LaserRules.SLASH) {
            Offset(origin.x + (c + 0.24f) * cell, origin.y + (r + 0.76f) * cell) to Offset(origin.x + (c + 0.76f) * cell, origin.y + (r + 0.24f) * cell)
        } else {
            Offset(origin.x + (c + 0.24f) * cell, origin.y + (r + 0.24f) * cell) to Offset(origin.x + (c + 0.76f) * cell, origin.y + (r + 0.76f) * cell)
        }
        drawLine(paint.mirror.copy(alpha = 0.25f * alpha), a, b, strokeWidth = cell * 0.17f, cap = StrokeCap.Round)
        drawLine(paint.mirror.copy(alpha = alpha), a, b, strokeWidth = cell * 0.095f, cap = StrokeCap.Round)
    }
    if (guideCell >= 0) {
        val topLeft = Offset(origin.x + (guideCell % size) * cell + gap, origin.y + (guideCell / size) * cell + gap)
        drawRoundRect(
            paint.guide.copy(alpha = (0.35f + 0.6f * guidePulse) * alpha),
            topLeft,
            Size(cell - 2 * gap, cell - 2 * gap),
            corner,
            style = Stroke(width = cell * 0.07f),
        )
    }
}

/**
 * One full-screen page of the feed: how many targets are lit, the board with its laser and mirrors, and a restart button, or the
 * result. A tap on a mirror turns it. The beam runs its new way when a mirror turns, and the targets light as it reaches them.
 */
@Composable
fun LasersPage(
    page: LaserSnapshot,
    onTurn: (Int) -> LaserResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current.withoutSound()
    val sounds = com.mehmtcan.brainscroll.ui.sound.LocalSoundEffects.current
    val reduce = LocalReduceMotion.current
    val state = page.state
    val won = state.status == LaserStatus.Won
    val n = state.size

    // The result and the buzz wait until the beam has run its way and the last target is lit.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, BEAM_MILLIS + 120L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    // Each target that gets lit chimes once the beam has reached it; a board that is restored lit stays quiet.
    var litBefore by remember(page.id) { mutableStateOf(state.litCount) }
    LaunchedEffect(state.litCount) {
        val sound = com.mehmtcan.brainscroll.ui.sound.lasersLitSound(litBefore, state.litCount)
        litBefore = state.litCount
        if (sound != null) {
            kotlinx.coroutines.delay(BEAM_MILLIS.toLong() / 2)
            sounds.play(sound, com.mehmtcan.brainscroll.ui.sound.LASERS_LIT_VOLUME)
        }
    }

    val latestState by rememberUpdatedState(state)
    val latestTurn by rememberUpdatedState(onTurn)

    // The mirrors the beam on show belongs to: the beam runs its new way, then this is the board of the state. A page that is shown for
    // the first time (scrolled back to) shows the beam as it is. [runFor] says which board the progress belongs to, so the very first
    // frame of a new turn is drawn as it begins and never as it ends.
    var shown by remember(page.id) { mutableStateOf(state.mirrors) }
    var runFor by remember(page.id) { mutableStateOf<List<Int>?>(null) }
    val run = remember(page.id) { Animatable(1f) }
    // A mirror the beam does not meet (or one that sends it the way it already went) changes nothing the player can see: the beam is
    // not drawn again, it stays as it is.
    val beamUnchanged = shown != state.mirrors && LaserRules.trace(n, state.row, shown) == state.beam
    LaunchedEffect(state.mirrors) {
        if (shown == state.mirrors) return@LaunchedEffect
        if (reduce || LaserRules.trace(n, state.row, shown) == state.beam) {
            shown = state.mirrors
            return@LaunchedEffect
        }
        run.snapTo(0f)
        runFor = state.mirrors
        run.animateTo(1f, tween(BEAM_MILLIS, easing = LinearOutSlowInEasing))
        shown = state.mirrors
    }

    val guide = LocalGuide.current?.takeIf { it.reason == GuideReason.LaserTurn && it.cells.size == 1 }
    val pulse by rememberInfiniteTransition(label = "laserGuide").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    val paint = LaserPaint(
        tile = colors.bgPage.copy(alpha = 0.85f),
        mirrorTile = colors.bgRaised,
        edge = colors.borderStrong.copy(alpha = 0.35f),
        mirror = Color(0xFFA8DCFF),
        beam = GameKind.Lasers.world,
        beamCore = Color(0xFFFFE9EC),
        target = LaserTargetColor,
        emitter = GameKind.Lasers.world,
        guide = colors.pendingFill,
    )
    val wonPaint = LaserPaint(paint.tile, paint.mirrorTile, paint.edge, paint.mirror, colors.correctFill, Color(0xFFEFFFE6), paint.target, paint.emitter, paint.guide)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Chip(
                text = stringResource(Res.string.lasers_lit, state.litCount, state.targets.size),
                textColor = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val density = LocalDensity.current
            // The biggest cell that lets the whole board fit the room there is, across and down (a short screen with big text has little height).
            val byWidth = with(density) { minOf(MaxBoard, maxWidth).toPx() } / (n + PAD_LEFT + PAD_RIGHT)
            val byHeight = with(density) { maxHeight.toPx() } / (n + 2 * PAD_Y)
            val cellPx = minOf(byWidth, byHeight)
            val boardWidth = with(density) { (cellPx * (n + PAD_LEFT + PAD_RIGHT)).toDp() }
            val boardHeight = with(density) { (cellPx * (n + 2 * PAD_Y)).toDp() }
            val description = stringResource(Res.string.a11y_lasers_board, state.litCount, state.targets.size)
            // TalkBack: one action for every mirror, so each one can be turned without touching the board.
            val turnActions = state.mirrors.indices.filter { state.mirrors[it] != 0 }.map { cell ->
                val label = stringResource(Res.string.a11y_lasers_turn_mirror, cell / n + 1, cell % n + 1)
                CustomAccessibilityAction(label) {
                    if (latestState.status != LaserStatus.Won && latestTurn(cell) != LaserResult.Ignored) {
                        haptics.perform(HapticKind.Light)
                        sounds.play(com.mehmtcan.brainscroll.ui.sound.Sfx.LasersTurn, com.mehmtcan.brainscroll.ui.sound.LASERS_TURN_VOLUME)
                    }
                    true
                }
            }
            Canvas(
                modifier = Modifier
                    .size(boardWidth, boardHeight)
                    .testTag("lasersBoard")
                    .semantics {
                        contentDescription = description
                        liveRegion = LiveRegionMode.Polite
                        if (!won) customActions = turnActions
                        mirrorList = state.mirrors
                        targetList = state.targets.sorted()
                        laserRow = state.row
                        litTargets = state.litCount
                    }
                    .pointerInput(n, cellPx) {
                        detectTapGestures { tap ->
                            val col = ((tap.x / cellPx) - PAD_LEFT).let { if (it < 0f) -1 else it.toInt() }
                            val row = ((tap.y / cellPx) - PAD_Y).let { if (it < 0f) -1 else it.toInt() }
                            if (col !in 0 until n || row !in 0 until n) return@detectTapGestures
                            if (latestState.status == LaserStatus.Won) return@detectTapGestures
                            if (latestTurn(row * n + col) != LaserResult.Ignored) {
                                haptics.perform(HapticKind.Light)
                                sounds.play(com.mehmtcan.brainscroll.ui.sound.Sfx.LasersTurn, com.mehmtcan.brainscroll.ui.sound.LASERS_TURN_VOLUME)
                            }
                        }
                    },
            ) {
                val progress = if (shown != state.mirrors && !beamUnchanged) (if (runFor == state.mirrors) run.value else 0f) else 1f
                // While the beam is still running the old board's mirrors are not what lit the targets, so the targets follow the beam.
                val settled = shown == state.mirrors || beamUnchanged
                drawLaserBoard(
                    size = n,
                    row = state.row,
                    mirrors = state.mirrors,
                    targets = state.targets,
                    beam = state.beam,
                    origin = Offset(PAD_LEFT * cellPx, PAD_Y * cellPx),
                    cell = cellPx,
                    paint = if (won && settled) wonPaint else paint,
                    beamProgress = progress,
                    guideCell = if (guide != null && !won) guide.cells[0] else -1,
                    guidePulse = pulse,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                LasersResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.lasers_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_lasers_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.canRestart,
                        tag = "lasersRestart",
                    ) { drawRestart(ink) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun LasersResultCard(page: LaserSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().testTag("lasersResultCard").resultSurface(colors.bgSurface, GameKind.Lasers.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.lasers_won),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.lasers_result, formatDuration(page.durationMs)),
                stats = listOf(
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.Lasers.world),
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
