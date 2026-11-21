package com.mehmtcan.brainscroll.ui.matchsticks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_match_first
import brainscroll.shared.generated.resources.a11y_match_sign
import brainscroll.shared.generated.resources.a11y_match_second
import brainscroll.shared.generated.resources.a11y_match_result
import brainscroll.shared.generated.resources.a11y_match_result_tens
import brainscroll.shared.generated.resources.a11y_match_result_ones
import brainscroll.shared.generated.resources.a11y_match_top
import brainscroll.shared.generated.resources.a11y_match_top_right
import brainscroll.shared.generated.resources.a11y_match_bottom_right
import brainscroll.shared.generated.resources.a11y_match_bottom
import brainscroll.shared.generated.resources.a11y_match_bottom_left
import brainscroll.shared.generated.resources.a11y_match_top_left
import brainscroll.shared.generated.resources.a11y_match_middle
import brainscroll.shared.generated.resources.a11y_match_flat
import brainscroll.shared.generated.resources.a11y_match_upright
import brainscroll.shared.generated.resources.a11y_matchsticks_spot
import brainscroll.shared.generated.resources.a11y_matchsticks_holding
import brainscroll.shared.generated.resources.a11y_matchsticks_has_stick
import brainscroll.shared.generated.resources.a11y_matchsticks_empty
import brainscroll.shared.generated.resources.a11y_matchsticks_board
import brainscroll.shared.generated.resources.a11y_matchsticks_next
import brainscroll.shared.generated.resources.a11y_matchsticks_prev
import brainscroll.shared.generated.resources.a11y_matchsticks_touch
import brainscroll.shared.generated.resources.a11y_matchsticks_undo
import brainscroll.shared.generated.resources.matchsticks_hint
import brainscroll.shared.generated.resources.matchsticks_result
import brainscroll.shared.generated.resources.matchsticks_won
import brainscroll.shared.generated.resources.matchsticks_wrong
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.matchsticks.MatchMove
import com.mehmtcan.brainscroll.game.matchsticks.MatchResult
import com.mehmtcan.brainscroll.game.matchsticks.MatchRules
import com.mehmtcan.brainscroll.game.matchsticks.MatchSlot
import com.mehmtcan.brainscroll.game.matchsticks.MatchSnapshot
import com.mehmtcan.brainscroll.game.matchsticks.MatchStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
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
import com.mehmtcan.brainscroll.ui.tubes.drawUndo
import org.jetbrains.compose.resources.stringResource
import kotlin.math.hypot

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
/* At least this high; a result card that needs more (big text) makes it higher and the board smaller. */
private val BottomAreaHeight = 152.dp
private val MaxBoard = 420.dp

/** How long a stick takes to fly to its new place. */
private const val FLIGHT_MILLIS = 280

/** How far from a stick, in units, a tap still counts as on it (a stick is 8 wide: a finger is much wider). */
private const val TOUCH_REACH = 24f

/** What the player is looking for (a stick to pick up, or an empty place for the one in hand) counts as this much nearer. It also settles a tap on the middle of a plus sign, where two places cross. */
private const val WANTED_BONUS = 5f

/** The sticks of each symbol as they are now (see MatchPuzzle), for tests. */
internal val MatchSticksCells = SemanticsPropertyKey<List<Int>>("MatchSticksCells")
private var SemanticsPropertyReceiver.stickCells by MatchSticksCells

// The board is drawn in units: a digit is 56 wide and 96 high, symbols are 70 apart, the equals sign sits after the second number.
private const val PAD_X = 16f
private const val PAD_Y = 30f
private const val BOARD_HEIGHT = 96f
private const val STICK_WIDTH = 8f
private const val HEAD_RADIUS = 5f

// The seven places of a digit, a to g (see MatchRules): top, top right, bottom right, bottom, bottom left, top left, middle.
// Every stick stops short of the next one, so the sticks of a digit touch at the corners without lying on each other. The sign is
// drawn so that its two sticks are as long as each other and cross in the middle of the symbol (x 28, y 48).
private val DigitLines = arrayOf(
    floatArrayOf(14f, 8f, 42f, 8f),
    floatArrayOf(50f, 16f, 50f, 42f),
    floatArrayOf(50f, 54f, 50f, 80f),
    floatArrayOf(14f, 88f, 42f, 88f),
    floatArrayOf(6f, 54f, 6f, 80f),
    floatArrayOf(6f, 16f, 6f, 42f),
    floatArrayOf(14f, 48f, 42f, 48f),
)
private val SignLines = arrayOf(floatArrayOf(14f, 48f, 42f, 48f), floatArrayOf(28f, 34f, 28f, 62f))
private val EqualsLines = arrayOf(floatArrayOf(14f, 38f, 42f, 38f), floatArrayOf(14f, 58f, 42f, 58f))

/** Where symbol [cell] starts, in units: the equals sign takes the place between the second number and the result. */
private fun cellLeft(cell: Int): Float = if (cell < 3) cell * 70f else (cell + 1) * 70f

/** How wide the sum is, in units. */
internal fun matchWidthUnits(cellCount: Int): Float = (cellCount + 1) * 70f - 14f

internal fun matchSlotLine(slot: MatchSlot): FloatArray {
    val base = if (slot.cell == 1) SignLines[slot.bit] else DigitLines[slot.bit]
    val x = cellLeft(slot.cell)
    return floatArrayOf(base[0] + x, base[1], base[2] + x, base[3])
}

/**
 * A point on a place, a little way from its end, in pixels on a board [widthPx] wide, for tests. The very middle is no good: the two sticks
 * of a plus sign cross there, so a tap in the middle of one is also a tap on the other.
 */
internal fun matchSlotTapPoint(slot: MatchSlot, cellCount: Int, widthPx: Float): Offset {
    val unit = widthPx / (matchWidthUnits(cellCount) + 2 * PAD_X)
    val l = matchSlotLine(slot)
    val t = 0.25f
    return Offset((PAD_X + l[0] + (l[2] - l[0]) * t) * unit, (PAD_Y + l[1] + (l[3] - l[1]) * t) * unit)
}

/** The colors of the sticks. */
internal class MatchPaint(
    val stick: Color,
    val head: Color,
    val fixed: Color,
    val held: Color,
    val heldHead: Color,
    val ghost: Color,
    /** The stick that is picked up when it is not the one the guide points at. */
    val wrong: Color = Color(0xFFFF5A5A),
)

internal val WoodStick = Color(0xFFE2A869)
internal val MatchHead = Color(0xFFE8553D)

internal fun DrawScope.drawMatchStick(line: FloatArray, origin: Offset, unit: Float, color: Color, head: Color, scale: Float = 1f) {
    val a = Offset(origin.x + line[0] * unit, origin.y + line[1] * unit)
    val b = Offset(origin.x + line[2] * unit, origin.y + line[3] * unit)
    drawLine(color, a, b, strokeWidth = STICK_WIDTH * unit * scale, cap = StrokeCap.Round)
    drawCircle(head, HEAD_RADIUS * unit * scale, a)
}

/**
 * Draws the sum: every stick that is on [cells] except those on [hidden], the sticks of the equals sign, and, while a stick is
 * [lifted] (picked up), a dotted mark on every empty place. [origin] is where the board starts and [unit] how long one unit is.
 * Used by the page, the thumbnail and the demo.
 */
internal fun DrawScope.drawMatchBoard(
    cells: List<Int>,
    origin: Offset,
    unit: Float,
    paint: MatchPaint,
    hidden: Set<MatchSlot> = emptySet(),
    lifted: MatchSlot? = null,
    liftedWrong: Boolean = false,
    showGhosts: Boolean = false,
    alpha: Float = 1f,
) {
    for (line in EqualsLines) {
        val x = 210f
        drawMatchStick(floatArrayOf(line[0] + x, line[1], line[2] + x, line[3]), origin, unit, paint.fixed.copy(alpha = paint.fixed.alpha * alpha), Color.Transparent)
    }
    for (slot in MatchRules.slotsOf(cells.size)) {
        val has = (cells[slot.cell] shr slot.bit) and 1 == 1
        if (has && slot !in hidden) {
            val isLifted = slot == lifted
            val body = if (isLifted) (if (liftedWrong) paint.wrong else paint.held) else paint.stick
            val tip = if (isLifted) (if (liftedWrong) paint.wrong else paint.heldHead) else paint.head
            drawMatchStick(matchSlotLine(slot), origin, unit, body.copy(alpha = alpha), tip.copy(alpha = alpha))
        } else if (!has && showGhosts) {
            val l = matchSlotLine(slot)
            // An empty place is a faint stick, like a socket on the board.
            drawLine(
                paint.ghost.copy(alpha = paint.ghost.alpha * alpha),
                Offset(origin.x + l[0] * unit, origin.y + l[1] * unit),
                Offset(origin.x + l[2] * unit, origin.y + l[3] * unit),
                strokeWidth = STICK_WIDTH * unit,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** The stick that went from one sum to the next, or null if they differ by anything but one stick moved. */
internal fun stickMove(from: List<Int>, to: List<Int>): MatchMove? {
    var source: MatchSlot? = null
    var target: MatchSlot? = null
    for (c in from.indices) for (b in 0 until MatchRules.width(c)) {
        val was = (from[c] shr b) and 1
        val now = (to[c] shr b) and 1
        if (was == 1 && now == 0) { if (source != null) return null; source = MatchSlot(c, b) }
        if (was == 0 && now == 1) { if (target != null) return null; target = MatchSlot(c, b) }
    }
    return if (source != null && target != null) MatchMove(source, target) else null
}

/** The sum as a screen reader says it: `6 − 4 = 2`, with a ? for a symbol that is not a digit or a sign. */
internal fun equationText(cells: List<Int>): String {
    fun digit(mask: Int) = MatchRules.digitOf(mask).let { if (it < 0) "?" else it.toString() }
    val sign = when (cells[1]) {
        MatchRules.PLUS -> "+"
        MatchRules.MINUS -> "−"
        else -> "?"
    }
    val result = if (cells.size == 5) digit(cells[3]) + digit(cells[4]) else digit(cells[3])
    return "${digit(cells[0])} $sign ${digit(cells[2])} = $result"
}

private fun distanceToStick(x: Float, y: Float, line: FloatArray): Float {
    val dx = line[2] - line[0]
    val dy = line[3] - line[1]
    val lengthSquared = dx * dx + dy * dy
    val t = if (lengthSquared == 0f) 0f else (((x - line[0]) * dx + (y - line[1]) * dy) / lengthSquared).coerceIn(0f, 1f)
    return hypot(x - (line[0] + t * dx), y - (line[1] + t * dy))
}

/**
 * The stick or the empty place a tap at ([x], [y]) (in units) is meant for, or null if it is too far from all of them. A finger is wide:
 * what the player is looking for counts as nearer (a stick to pick up, or, while [holding] one, an empty place).
 */
internal fun pickMatchSlot(cells: List<Int>, x: Float, y: Float, holding: Boolean): MatchSlot? =
    MatchRules.slotsOf(cells.size)
        .map { slot ->
            val has = (cells[slot.cell] shr slot.bit) and 1 == 1
            val wanted = if (holding) !has else has
            slot to distanceToStick(x, y, matchSlotLine(slot)) - (if (wanted) WANTED_BONUS else 0f)
        }
        .minByOrNull { it.second }
        ?.takeIf { it.second <= TOUCH_REACH }
        ?.first

/**
 * One full-screen page of the feed: the sum made of sticks on a board, and an undo button, or the result. A tap on a stick picks
 * it up, a tap on an empty place lays it there. A wrong sum has to be put back (undo) before the next try.
 */
@Composable
fun MatchsticksPage(
    page: MatchSnapshot,
    onMove: (MatchMove) -> MatchResult,
    onUndo: () -> Boolean,
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
    val won = state.status == MatchStatus.Won
    val cellCount = state.cells.size

    // The result and the buzz wait a moment so the last stick can be seen landing.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 350L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    val latestState by rememberUpdatedState(state)
    val latestMove by rememberUpdatedState(onMove)
    var held by remember(page.id) { mutableStateOf<MatchSlot?>(null) }

    // The sum that is on show: the stick flies from its old place to its new one, then this is the sum of the state. A page that is
    // shown for the first time (scrolled back to) shows the sum as it is. [flightFor] says which sum the progress belongs to, so
    // the very first frame of a new move is drawn as it begins and never as it ends.
    var shown by remember(page.id) { mutableStateOf(state.cells) }
    var flightFor by remember(page.id) { mutableStateOf<List<Int>?>(null) }
    val flight = remember(page.id) { Animatable(1f) }
    LaunchedEffect(state.cells) {
        if (shown == state.cells) return@LaunchedEffect
        if (reduce || stickMove(shown, state.cells) == null) {
            shown = state.cells
            return@LaunchedEffect
        }
        flight.snapTo(0f)
        flightFor = state.cells
        flight.animateTo(1f, tween(FLIGHT_MILLIS, easing = FastOutSlowInEasing))
        shown = state.cells
    }
    // A stick that is picked up and a sum that changes (undo, a restored game) never stay together.
    LaunchedEffect(state.cells) { held = null }

    // Touching a place (with a finger, or with the place cursor of TalkBack): pick the stick up, put it down, or let it go.
    val pickSound = com.mehmtcan.brainscroll.ui.sound.Sfx.MatchsticksPick
    val placeSound = com.mehmtcan.brainscroll.ui.sound.Sfx.MatchsticksPlace
    val tapSlot: (MatchSlot?) -> Unit = tap@{ hit ->
        val s = latestState
        if (s.status == MatchStatus.Won || s.moved) return@tap
        val current = held
        if (hit == null) {
            held = null
            return@tap
        }
        val occupied = (s.cells[hit.cell] shr hit.bit) and 1 == 1
        when {
            current == null -> if (occupied) {
                held = hit
                haptics.perform(HapticKind.Light)
                sounds.play(pickSound, com.mehmtcan.brainscroll.ui.sound.MATCHSTICKS_PICK_VOLUME)
            }
            current == hit -> held = null
            occupied -> {
                held = hit
                haptics.perform(HapticKind.Light)
                sounds.play(pickSound, com.mehmtcan.brainscroll.ui.sound.MATCHSTICKS_PICK_VOLUME)
            }
            else -> {
                held = null
                when (latestMove(MatchMove(current, hit))) {
                    MatchResult.Moved -> {
                        haptics.perform(HapticKind.Warning)
                        sounds.play(placeSound, com.mehmtcan.brainscroll.ui.sound.MATCHSTICKS_PLACE_VOLUME)
                    }
                    MatchResult.Ignored -> Unit
                    else -> sounds.play(placeSound, com.mehmtcan.brainscroll.ui.sound.MATCHSTICKS_PLACE_VOLUME)
                }
            }
        }
    }

    // For TalkBack: a cursor that goes from place to place, said out loud, and one action that touches the place it is on.
    var cursor by remember(page.id) { androidx.compose.runtime.mutableIntStateOf(0) }
    val spotText = matchSpotText(state.cells, cursor, held)

    val guide = LocalGuide.current?.takeIf { it.reason == GuideReason.MatchstickMove && it.cells.size == 2 }
    val slots = remember(cellCount) { MatchRules.slotsOf(cellCount) }
    val guideFrom = guide?.let { slots.getOrNull(it.cells[0]) }
    val guideTo = guide?.let { slots.getOrNull(it.cells[1]) }
    val pulse by rememberInfiniteTransition(label = "matchGuide").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    val paint = MatchPaint(
        stick = WoodStick,
        head = MatchHead,
        fixed = WoodStick.copy(alpha = 0.45f),
        held = colors.streakAnswer,
        heldHead = Color(0xFFFF9F43),
        ghost = colors.textSecondary.copy(alpha = 0.3f),
    )
    val wonPaint = MatchPaint(
        stick = colors.correctFill,
        head = colors.correctFill.copy(alpha = 0.6f),
        fixed = colors.correctFill.copy(alpha = 0.45f),
        held = paint.held,
        heldHead = paint.heldHead,
        ghost = paint.ghost,
    )

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val density = LocalDensity.current
            val totalUnits = matchWidthUnits(cellCount) + 2 * PAD_X
            val boardWidth = minOf(MaxBoard, maxWidth)
            val unitPx = with(density) { boardWidth.toPx() } / totalUnits
            val boardHeight = with(density) { (unitPx * (BOARD_HEIGHT + 2 * PAD_Y)).toDp() }
            val description = stringResource(Res.string.a11y_matchsticks_board, equationText(state.cells))
            val nextLabel = stringResource(Res.string.a11y_matchsticks_next)
            val prevLabel = stringResource(Res.string.a11y_matchsticks_prev)
            val touchLabel = stringResource(Res.string.a11y_matchsticks_touch)
            Canvas(
                modifier = Modifier
                    .size(boardWidth, boardHeight)
                    .testTag("matchsticksBoard")
                    .semantics {
                        contentDescription = description
                        stickCells = state.cells
                        stateDescription = spotText
                        if (!won) {
                            customActions = listOf(
                                CustomAccessibilityAction(nextLabel) { cursor = (cursor + 1) % slots.size; true },
                                CustomAccessibilityAction(prevLabel) { cursor = (cursor + slots.size - 1) % slots.size; true },
                                CustomAccessibilityAction(touchLabel) { tapSlot(slots[cursor.coerceIn(0, slots.size - 1)]); true },
                            )
                        }
                    }
                    .pointerInput(cellCount, unitPx) {
                        detectTapGestures { tap ->
                            val x = tap.x / unitPx - PAD_X
                            val y = tap.y / unitPx - PAD_Y
                            val s = latestState
                            if (s.status == MatchStatus.Won || s.moved) return@detectTapGestures
                            // The stick or the empty place nearest to the finger, if it is near enough. A finger is wide: what the
                            // player is looking for counts as nearer (a stick to pick up, or, with one in hand, an empty place).
                            tapSlot(pickMatchSlot(s.cells, x, y, holding = held != null))
                        }
                    },
            ) {
                // The board the sticks lie on.
                val corner = CornerRadius(20.dp.toPx())
                drawRoundRect(colors.bgSurface, cornerRadius = corner)
                drawRoundRect(colors.borderStrong.copy(alpha = 0.35f), cornerRadius = corner, style = Stroke(width = 1.5.dp.toPx()))
                val origin = Offset(PAD_X * unitPx, PAD_Y * unitPx)
                val moving = if (shown != state.cells) stickMove(shown, state.cells) else null
                val progress = if (moving != null) (if (flightFor == state.cells) flight.value else 0f) else 1f
                val landed = moving == null
                // The guide only speaks while a sum is on the board that has not been tried.
                val guideOn = guideFrom != null && guideTo != null && moving == null && !won && !state.moved
                val palette = if (won && landed) wonPaint else paint
                // While a stick is on its way its new place waits empty; the stick itself is drawn on top.
                drawMatchBoard(
                    cells = state.cells,
                    origin = origin,
                    unit = unitPx,
                    paint = palette,
                    hidden = setOfNotNull(moving?.to),
                    lifted = held,
                    liftedWrong = guideOn && held != null && held != guideFrom,
                    showGhosts = held != null,
                )
                if (moving != null) {
                    val a = matchSlotLine(moving.from)
                    val b = matchSlotLine(moving.to)
                    val p = progress
                    val line = FloatArray(4) { a[it] + (b[it] - a[it]) * p }
                    // The stick rises a little on its way (it is bigger half way).
                    val rise = 1f + 0.25f * (1f - kotlin.math.abs(2f * p - 1f))
                    drawMatchStick(line, origin, unitPx, palette.held, palette.heldHead, rise)
                }
                // The guide: the stick to take pulses in the guide color (a stick in hand that is not that one shows red); once the right
                // stick is in hand, the place to lay it on pulses.
                if (guideOn && guideFrom != null && guideTo != null) {
                    val a = 0.35f + 0.6f * pulse
                    if (held != guideFrom) {
                        val line = matchSlotLine(guideFrom)
                        drawMatchStick(line, origin, unitPx, colors.pendingFill.copy(alpha = a), colors.pendingFill.copy(alpha = a))
                    } else {
                        val to = matchSlotLine(guideTo)
                        drawLine(
                            colors.pendingFill.copy(alpha = a),
                            Offset(origin.x + to[0] * unitPx, origin.y + to[1] * unitPx),
                            Offset(origin.x + to[2] * unitPx, origin.y + to[3] * unitPx),
                            strokeWidth = STICK_WIDTH * unitPx,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                MatchsticksResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(if (state.moved) Res.string.matchsticks_wrong else Res.string.matchsticks_hint),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = type.body.copy(color = if (state.moved) colors.error else colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_matchsticks_undo),
                        onClick = { if (onUndo()) haptics.perform(HapticKind.Light) },
                        enabled = state.canUndo,
                        tag = "matchsticksUndo",
                    ) { drawUndo(ink) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun MatchsticksResultCard(page: MatchSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().testTag("matchsticksResultCard").resultSurface(colors.bgSurface, GameKind.Matchsticks.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.matchsticks_won),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.matchsticks_result, formatDuration(page.durationMs)),
                stats = listOf(
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.Matchsticks.world),
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

/** What TalkBack says about the place the cursor is on: which symbol, which place of it, and whether a stick lies there. */
@Composable
internal fun matchSpotText(cells: List<Int>, cursor: Int, held: MatchSlot?): String {
    val slot = MatchRules.slotsOf(cells.size).let { it[cursor.coerceIn(0, it.size - 1)] }
    val symbol = stringResource(
        when {
            slot.cell == 0 -> Res.string.a11y_match_first
            slot.cell == 1 -> Res.string.a11y_match_sign
            slot.cell == 2 -> Res.string.a11y_match_second
            cells.size == 5 && slot.cell == 3 -> Res.string.a11y_match_result_tens
            cells.size == 5 -> Res.string.a11y_match_result_ones
            else -> Res.string.a11y_match_result
        },
    )
    // The places of a digit: bit 0 is the top stick, then clockwise, bit 6 the middle one. A sign: the flat stick, then the upright one.
    val place = stringResource(
        if (slot.cell == 1) {
            if (slot.bit == 0) Res.string.a11y_match_flat else Res.string.a11y_match_upright
        } else {
            when (slot.bit) {
                0 -> Res.string.a11y_match_top
                1 -> Res.string.a11y_match_top_right
                2 -> Res.string.a11y_match_bottom_right
                3 -> Res.string.a11y_match_bottom
                4 -> Res.string.a11y_match_bottom_left
                5 -> Res.string.a11y_match_top_left
                else -> Res.string.a11y_match_middle
            }
        },
    )
    val occupied = (cells[slot.cell] shr slot.bit) and 1 == 1
    val state = stringResource(if (occupied) Res.string.a11y_matchsticks_has_stick else Res.string.a11y_matchsticks_empty)
    val holding = if (held != null) " " + stringResource(Res.string.a11y_matchsticks_holding).trim() else ""
    return stringResource(Res.string.a11y_matchsticks_spot, symbol, place, state) + holding
}
