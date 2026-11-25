package com.mehmtcan.brainscroll.ui.untangle

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_untangle_board
import brainscroll.shared.generated.resources.a11y_untangle_restart
import brainscroll.shared.generated.resources.result_stat_time
import brainscroll.shared.generated.resources.a11y_untangle_down
import brainscroll.shared.generated.resources.a11y_untangle_left
import brainscroll.shared.generated.resources.a11y_untangle_next_dot
import brainscroll.shared.generated.resources.a11y_untangle_right
import brainscroll.shared.generated.resources.a11y_untangle_selected
import brainscroll.shared.generated.resources.a11y_untangle_up
import brainscroll.shared.generated.resources.untangle_crossings
import brainscroll.shared.generated.resources.untangle_hint
import brainscroll.shared.generated.resources.untangle_result
import brainscroll.shared.generated.resources.untangle_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.untangle.UNTANGLE_BOARD
import com.mehmtcan.brainscroll.game.untangle.UntangleResult
import com.mehmtcan.brainscroll.game.untangle.UntangleSnapshot
import com.mehmtcan.brainscroll.game.untangle.UntangleStatus
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
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.theme.GuideColor
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import org.jetbrains.compose.resources.stringResource
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
/* At least this high; a result card that needs more (big text) makes it higher and the board smaller. */
private val BottomAreaHeight = 152.dp
private val MaxBoard = 420.dp

/** How big a dot is, and how near a finger has to be to pick it up (a finger is much wider than a dot). */
private val NodeRadius = 11.dp
private val NodeReach = 32.dp
private val LineWidth = 3.5.dp

/** Where the dots are (x, y of dot 0, and so on), the lines (two numbers for each, the dots it joins) and how many pairs cross, for tests. */
internal val UntanglePositionsKey = SemanticsPropertyKey<List<Int>>("UntanglePositions")
private var SemanticsPropertyReceiver.positionList by UntanglePositionsKey
internal val UntangleEdgesKey = SemanticsPropertyKey<List<Int>>("UntangleEdges")
private var SemanticsPropertyReceiver.edgeList by UntangleEdgesKey
/** How far a TalkBack step moves a dot, in units of the board. */
private const val A11Y_STEP = 60

internal val UntangleCrossingKey = SemanticsPropertyKey<Int>("UntangleCrossing")
private var SemanticsPropertyReceiver.crossingCount by UntangleCrossingKey

/** The yellow the dots turn when the board is won. */
internal val UntangleWinColor = Color(0xFFFFC93C)

/** The colors of the board. */
internal class UntanglePaint(
    val line: Color,
    val crossing: Color,
    val node: Color,
    val nodeEdge: Color,
    val guide: Color,
)

/**
 * Draws a tangle the way the page does, for the page, the thumbnail and the demo: [edges] between [positions] (in units of the board,
 * x then y of every dot), the crossing ones in the color for it, dot [lifted] bigger, and a ring on [guideNode] with a dashed line to
 * ([guideX], [guideY]) if there is one. [side] is the side of the board in pixels.
 */
internal fun DrawScope.drawUntangle(
    positions: List<Int>,
    edges: List<Pair<Int, Int>>,
    crossing: Set<Int>,
    origin: Offset,
    side: Float,
    paint: UntanglePaint,
    nodeRadius: Float,
    lineWidth: Float,
    lifted: Int = -1,
    won: Boolean = false,
    guideNode: Int = -1,
    guideX: Int = 0,
    guideY: Int = 0,
    guidePulse: Float = 0f,
    alpha: Float = 1f,
) {
    val unit = side / UNTANGLE_BOARD
    fun at(i: Int) = Offset(origin.x + positions[2 * i] * unit, origin.y + positions[2 * i + 1] * unit)
    edges.forEachIndexed { i, (a, b) ->
        drawLine((if (i in crossing) paint.crossing else paint.line).copy(alpha = alpha), at(a), at(b), strokeWidth = lineWidth, cap = StrokeCap.Round)
    }
    if (guideNode >= 0) {
        val target = Offset(origin.x + guideX * unit, origin.y + guideY * unit)
        val pulse = (0.35f + 0.6f * guidePulse) * alpha
        drawLine(
            paint.guide.copy(alpha = pulse * 0.8f),
            at(guideNode),
            target,
            strokeWidth = lineWidth,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(lineWidth * 2.2f, lineWidth * 2.2f)),
        )
        drawCircle(paint.guide.copy(alpha = pulse), nodeRadius * 1.5f, target, style = Stroke(width = lineWidth * 1.2f))
        drawCircle(paint.guide.copy(alpha = pulse), nodeRadius * 1.9f, at(guideNode), style = Stroke(width = lineWidth * 1.2f))
    }
    for (i in 0 until positions.size / 2) {
        val big = if (i == lifted) 1.35f else 1f
        val fill = if (won) UntangleWinColor else paint.node
        drawCircle(fill.copy(alpha = alpha), nodeRadius * big, at(i))
        drawCircle(paint.nodeEdge.copy(alpha = alpha), nodeRadius * big, at(i), style = Stroke(width = lineWidth * 0.8f))
    }
}

/**
 * One full-screen page of the feed: how many pairs of lines cross, the board with its dots and lines, and a restart button, or the
 * result. A dot is picked up with a finger and dragged; the lines follow it and the ones that cross another are red. The board is won when
 * a dot is let go of and no line crosses another.
 */
@Composable
fun UntanglePage(
    page: UntangleSnapshot,
    onMove: (node: Int, x: Int, y: Int) -> Boolean,
    onDrop: () -> UntangleResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current.withoutSound()
    val sounds = com.mehmtcan.brainscroll.ui.sound.LocalSoundEffects.current
    val state = page.state
    val won = state.status == UntangleStatus.Won

    // The result and the buzz wait a moment so the last dot can be seen where it landed.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 350L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    // A chime when a drop leaves fewer crossings than before (a page restored with its crossings stays quiet).
    var crossingsBefore by remember(page.id) { mutableStateOf(state.crossingPairs) }
    LaunchedEffect(state.crossingPairs) {
        val sound = com.mehmtcan.brainscroll.ui.sound.untangleFewerSound(crossingsBefore, state.crossingPairs)
        crossingsBefore = state.crossingPairs
        if (sound != null) sounds.play(sound, com.mehmtcan.brainscroll.ui.sound.UNTANGLE_FEWER_VOLUME)
    }

    val latestState by rememberUpdatedState(state)
    val latestMove by rememberUpdatedState(onMove)
    val latestDrop by rememberUpdatedState(onDrop)
    var lifted by remember(page.id) { mutableIntStateOf(-1) }
    var picked by remember(page.id) { mutableIntStateOf(0) }

    val guide = LocalGuide.current?.takeIf { it.reason == GuideReason.UntangleMove && it.cells.size == 1 && it.numbers.size == 2 }
    val pulse by rememberInfiniteTransition(label = "untangleGuide").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    val paint = UntanglePaint(
        line = GameKind.Untangle.world,
        crossing = colors.error,
        node = colors.bgRaised,
        nodeEdge = colors.textPrimary,
        guide = GuideColor,
    )
    val wonPaint = UntanglePaint(colors.correctFill, colors.error, colors.bgRaised, colors.textPrimary, colors.pendingFill)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Chip(
                text = stringResource(Res.string.untangle_crossings, state.crossingPairs),
                textColor = if (state.crossingPairs == 0) colors.correctFill else colors.textSecondary,
            )
        }
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side: Dp = minOf(MaxBoard, maxWidth, maxHeight)
            val density = LocalDensity.current
            val sidePx = with(density) { side.toPx() }
            val description = stringResource(Res.string.a11y_untangle_board, state.crossingPairs)
            // TalkBack: pick a dot, then move it a step at a time; every step is a drop, so a move counts like it does with a finger.
            val pickedText = stringResource(Res.string.a11y_untangle_selected, picked + 1, state.nodeCount)
            val nextLabel = stringResource(Res.string.a11y_untangle_next_dot)
            val moveLabels = listOf(
                stringResource(Res.string.a11y_untangle_left),
                stringResource(Res.string.a11y_untangle_right),
                stringResource(Res.string.a11y_untangle_up),
                stringResource(Res.string.a11y_untangle_down),
            )
            val stepBy: (Int, Int) -> Boolean = { dx, dy ->
                val s = latestState
                val node = picked.coerceIn(0, s.nodeCount - 1)
                latestMove(node, s.positions[2 * node] + dx, s.positions[2 * node + 1] + dy)
                if (latestDrop() == UntangleResult.Moved) {
                    haptics.perform(HapticKind.Light)
                    sounds.play(com.mehmtcan.brainscroll.ui.sound.Sfx.UntangleDrop, com.mehmtcan.brainscroll.ui.sound.UNTANGLE_DROP_VOLUME)
                }
                true
            }
            Canvas(
                modifier = Modifier
                    .size(side)
                    .testTag("untangleBoard")
                    .semantics {
                        contentDescription = description
                        liveRegion = LiveRegionMode.Polite
                        stateDescription = pickedText
                        if (!won) {
                            customActions = listOf(
                                CustomAccessibilityAction(nextLabel) { picked = (picked + 1) % latestState.nodeCount; true },
                                CustomAccessibilityAction(moveLabels[0]) { stepBy(-A11Y_STEP, 0) },
                                CustomAccessibilityAction(moveLabels[1]) { stepBy(A11Y_STEP, 0) },
                                CustomAccessibilityAction(moveLabels[2]) { stepBy(0, -A11Y_STEP) },
                                CustomAccessibilityAction(moveLabels[3]) { stepBy(0, A11Y_STEP) },
                            )
                        }
                        positionList = state.positions
                        edgeList = state.edges.flatMap { listOf(it.first, it.second) }
                        crossingCount = state.crossingPairs
                    }
                    // Not keyed on the state: the board is only won when the finger is lifted, and a drag must never be cut short.
                    .pointerInput(state.nodeCount, sidePx) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val s = latestState
                            if (s.status == UntangleStatus.Won) return@awaitEachGesture
                            val unit = sidePx / UNTANGLE_BOARD
                            val reach = NodeReach.toPx()
                            // The dot nearest to the finger, if it is near enough.
                            var node = -1
                            var nearest = reach
                            for (i in 0 until s.nodeCount) {
                                val d = hypot(s.positions[2 * i] * unit - down.position.x, s.positions[2 * i + 1] * unit - down.position.y)
                                if (d < nearest) {
                                    nearest = d
                                    node = i
                                }
                            }
                            if (node < 0) return@awaitEachGesture
                            down.consume()
                            // The dot keeps its place under the finger: it does not jump to it.
                            val grabX = s.positions[2 * node] * unit - down.position.x
                            val grabY = s.positions[2 * node + 1] * unit - down.position.y
                            lifted = node
                            haptics.perform(HapticKind.Light)
                            sounds.play(com.mehmtcan.brainscroll.ui.sound.Sfx.MatchsticksPick, com.mehmtcan.brainscroll.ui.sound.MATCHSTICKS_PICK_VOLUME)
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                latestMove(node, ((change.position.x + grabX) / unit).roundToInt(), ((change.position.y + grabY) / unit).roundToInt())
                                change.consume()
                            } while (change.pressed)
                            lifted = -1
                            if (latestDrop() == UntangleResult.Moved) {
                                haptics.perform(HapticKind.Light)
                                sounds.play(com.mehmtcan.brainscroll.ui.sound.Sfx.UntangleDrop, com.mehmtcan.brainscroll.ui.sound.UNTANGLE_DROP_VOLUME)
                            }
                        }
                    },
            ) {
                val corner = CornerRadius(20.dp.toPx())
                drawRoundRect(colors.bgSurface, cornerRadius = corner)
                drawRoundRect(colors.borderStrong.copy(alpha = 0.35f), cornerRadius = corner, style = Stroke(width = 1.5.dp.toPx()))
                drawUntangle(
                    positions = state.positions,
                    edges = state.edges,
                    crossing = state.crossing,
                    origin = Offset.Zero,
                    side = size.width,
                    paint = if (won) wonPaint else paint,
                    nodeRadius = NodeRadius.toPx(),
                    lineWidth = LineWidth.toPx(),
                    lifted = lifted,
                    won = won,
                    guideNode = if (guide != null && !won && lifted < 0) guide.cells[0] else -1,
                    guideX = guide?.numbers?.get(0) ?: 0,
                    guideY = guide?.numbers?.get(1) ?: 0,
                    guidePulse = pulse,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                UntangleResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.untangle_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_untangle_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.canRestart,
                        tag = "untangleRestart",
                    ) { drawRestart(ink) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun UntangleResultCard(page: UntangleSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().testTag("untangleResultCard").resultSurface(colors.bgSurface, GameKind.Untangle.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.untangle_won),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.untangle_result, formatDuration(page.durationMs)),
                stats = listOf(
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.Untangle.world),
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
