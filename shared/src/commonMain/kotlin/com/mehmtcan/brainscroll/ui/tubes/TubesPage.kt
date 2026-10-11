package com.mehmtcan.brainscroll.ui.tubes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import com.mehmtcan.brainscroll.ui.feed.GuideMark
import com.mehmtcan.brainscroll.ui.feed.LocalGuide
import com.mehmtcan.brainscroll.ui.feed.guideHighlight
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_tube_done
import brainscroll.shared.generated.resources.a11y_tube_empty
import brainscroll.shared.generated.resources.a11y_tube_filled
import brainscroll.shared.generated.resources.a11y_tube_selected
import brainscroll.shared.generated.resources.a11y_tube_selected_empty
import brainscroll.shared.generated.resources.a11y_tubes_moves
import brainscroll.shared.generated.resources.a11y_tubes_restart
import brainscroll.shared.generated.resources.a11y_tubes_undo
import brainscroll.shared.generated.resources.game_tubes
import brainscroll.shared.generated.resources.tubes_hint
import brainscroll.shared.generated.resources.tubes_moves
import brainscroll.shared.generated.resources.tubes_result
import brainscroll.shared.generated.resources.tubes_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.memory.CardSymbol
import com.mehmtcan.brainscroll.game.tubes.PourResult
import com.mehmtcan.brainscroll.game.tubes.TubesGame
import com.mehmtcan.brainscroll.game.tubes.TubesSnapshot
import com.mehmtcan.brainscroll.game.tubes.TubesStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.memory.CardShape
import com.mehmtcan.brainscroll.ui.memory.symbolColor
import com.mehmtcan.brainscroll.ui.memory.symbolName
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.motion.popOnAppear
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val TubeGap = 12.dp
private val MaxTubeWidth = 72.dp
private val LiftHeight = 14.dp
private val TubePadding = 3.dp
private const val COLUMNS = 4

/** The whole pour (go over the other tube, tip, drain, come back) takes this long. */
private const val POUR_MILLIS = 1000

/** A color of the tubes is a number; it is shown with the color and the shape of the card symbol of the same number. */
private fun symbolOf(color: Int): CardSymbol = CardSymbol.entries[color % CardSymbol.entries.size]

/** Some liquid of one color in a tube; [units] is below 1 while it is being poured in or out. */
private data class Segment(val color: Int, val units: Float)

/** A pour that is being shown. [before] is the board as it was; the game itself already has the result. */
private class PourAnimation(val from: Int, val to: Int, val before: List<List<Int>>, val color: Int, val amount: Int)

/** How much liquid is drawn in a tube right now, in units (fractions while pouring). Only tests read it. */
internal val DrawnUnits = SemanticsPropertyKey<Float>("DrawnUnits")
private var SemanticsPropertyReceiver.drawnUnits by DrawnUnits

private fun ease(t: Float): Float = FastOutSlowInEasing.transform(t.coerceIn(0f, 1f))

/** One full-screen page of the feed: the title, the move counter, the tubes, and the buttons or the result. */
@Composable
fun TubesPage(
    page: TubesSnapshot,
    /** Pours one tube into another and says what happened, so this page can buzz the right way. */
    onPour: (from: Int, to: Int) -> PourResult,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val reduce = LocalReduceMotion.current
    val state = page.state

    // The tube that is lifted, waiting for the tube to pour into. Only the screen knows about it.
    var selected by remember(page.id) { mutableStateOf<Int?>(null) }

    // The pour on show. The game has the result the moment the player taps, the screen takes its time to show it, and
    // meanwhile draws the board as it was. Nothing else is accepted while it runs.
    var pouring by remember(page.id) { mutableStateOf<PourAnimation?>(null) }
    val progress = remember(page.id) { Animatable(0f) }
    LaunchedEffect(pouring) {
        if (pouring == null) return@LaunchedEffect
        progress.animateTo(1f, tween(POUR_MILLIS, easing = LinearEasing))
        pouring = null
        // Back to the start for the next pour. It has to be done here: if the next pour began from the old value, its
        // first frame would be drawn at the end of the pour (the liquid already in the other tube).
        progress.snapTo(0f)
    }
    val shown = pouring?.before ?: state.tubes

    // The result, the buzz and the card wait until the last pour has been shown.
    val won = state.status == TubesStatus.Won && pouring == null
    val status = if (won) WordleStatus.Won else WordleStatus.Playing
    OutcomeHaptics(status)
    val justFinished = rememberJustFinished(status)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Chip(
            text = stringResource(Res.string.tubes_moves, state.moves),
            textColor = colors.textSecondary,
            description = stringResource(Res.string.a11y_tubes_moves, state.moves),
        )
        Spacer(Modifier.height(Spacing.md))

        Board(
            tubes = shown,
            capacity = state.capacity,
            selected = selected,
            pouring = pouring,
            progress = progress.value,
            onTap = { tube ->
                val from = selected
                val tubeIsEmpty = state.tubes[tube].isEmpty()
                when {
                    state.status == TubesStatus.Won || pouring != null -> Unit
                    from == null -> if (!tubeIsEmpty) { selected = tube; haptics.perform(HapticKind.Light) }
                    from == tube -> selected = null
                    else -> {
                        val before = state.tubes
                        when (onPour(from, tube)) {
                            PourResult.Poured -> {
                                selected = null
                                haptics.perform(HapticKind.Medium)
                                if (!reduce) {
                                    val after = TubesGame.poured(before, from, tube, state.capacity)
                                    if (after != null) {
                                        pouring = PourAnimation(from, tube, before, before[from].last(), before[from].size - after[from].size)
                                    }
                                }
                            }
                            // Not allowed: a full tube or another color is the new choice, nothing else just buzzes.
                            PourResult.Ignored -> if (tubeIsEmpty) haptics.perform(HapticKind.Warning) else { selected = tube; haptics.perform(HapticKind.Light) }
                        }
                    }
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (won) {
                TubesResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.tubes_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        IconButton(
                            icon = ButtonIcon.Undo,
                            description = stringResource(Res.string.a11y_tubes_undo),
                            enabled = state.canUndo && pouring == null,
                            tag = "tubesUndo",
                            onClick = { selected = null; onUndo() },
                        )
                        IconButton(
                            icon = ButtonIcon.Restart,
                            description = stringResource(Res.string.a11y_tubes_restart),
                            enabled = state.canRestart && pouring == null,
                            tag = "tubesRestart",
                            onClick = { selected = null; onRestart() },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/**
 * The tubes in rows of up to 4, as large as the room allows, each at a place worked out here (a short last row is
 * centered). They are placed one by one instead of in rows so that the tube being poured can travel over the others.
 */
@Composable
private fun Board(
    tubes: List<List<Int>>,
    capacity: Int,
    selected: Int?,
    pouring: PourAnimation?,
    progress: Float,
    onTap: (Int) -> Unit,
    modifier: Modifier,
) {
    val count = tubes.size
    val rows = (count + COLUMNS - 1) / COLUMNS
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val byWidth = (maxWidth - TubeGap * (COLUMNS - 1)) / COLUMNS
        // A tube is about 2.6 times as tall as wide; the lifted tube needs a little room above it.
        val byHeight = ((maxHeight - LiftHeight - TubeGap * (rows - 1)) / rows) / 2.6f
        val width: Dp = minOf(MaxTubeWidth, byWidth, byHeight)
        val height = width * 2.6f
        val boardHeight = LiftHeight + height * rows + TubeGap * (rows - 1)

        // Left and top of a tube, in dp.
        fun left(index: Int): Float {
            val inRow = minOf(COLUMNS, count - (index / COLUMNS) * COLUMNS)
            val rowWidth = width * inRow + TubeGap * (inRow - 1)
            return ((maxWidth - rowWidth) / 2 + (width + TubeGap) * (index % COLUMNS)).value
        }
        fun top(index: Int): Float = (LiftHeight + (height + TubeGap) * (index / COLUMNS)).value

        val w = width.value
        val h = height.value
        val pour = pouring?.let { PourGeometry(it, progress, w, h, capacity, left(it.from), top(it.from), left(it.to), top(it.to)) }

        Box(Modifier.size(maxWidth, boardHeight)) {
            tubes.forEachIndexed { index, contents ->
                val segments = when {
                    pour != null && index == pour.animation.from -> pour.sourceSegments(contents)
                    pour != null && index == pour.animation.to -> pour.targetSegments(contents)
                    else -> contents.map { Segment(it, 1f) }
                }
                Tube(
                    number = index + 1,
                    contents = contents,
                    segments = segments,
                    capacity = capacity,
                    selected = selected == index,
                    width = width,
                    height = height,
                    modifier = Modifier.offset(left(index).dp, top(index).dp).guideHighlight(guidedTube(index, selected), RoundedCornerShape(14.dp)),
                    pour = pour?.takeIf { index == it.animation.from },
                    onClick = { onTap(index) },
                )
            }
            if (pour != null) Stream(pour, Modifier.fillMaxSize().zIndex(1f))
            tubes.forEachIndexed { index, contents ->
                val sorted = contents.size == capacity && contents.all { it == contents.first() }
                CompletionConfetti(sorted, seed = index, centerX = left(index) + w / 2, topY = top(index), tubeWidth = w)
            }
        }
    }
}

/**
 * Where everything is while a pour is shown, for the moment [progress] (0..1). The tube goes over the other one and
 * tips towards it, the liquid runs out as a stream and fills the other tube, then the tube goes home. All lengths are in dp.
 */
private class PourGeometry(
    val animation: PourAnimation,
    progress: Float,
    private val w: Float,
    private val h: Float,
    private val capacity: Int,
    homeLeft: Float,
    homeTop: Float,
    targetLeft: Float,
    private val targetTop: Float,
) {
    /** How far the tube has travelled to the other one: 0 at home, 1 over it. */
    private val travel: Float = when {
        progress < 0.25f -> ease(progress / 0.25f)
        progress < 0.75f -> 1f
        else -> 1f - ease((progress - 0.75f) / 0.25f)
    }

    /** How much of the liquid has moved: 0 before the pour, 1 after. */
    val poured: Float = when {
        progress < 0.25f -> 0f
        progress < 0.75f -> ease((progress - 0.25f) / 0.5f)
        else -> 1f
    }

    /** The stream only runs while the tube is tipped over the other one. */
    val flowing: Boolean = progress in 0.25f..0.75f

    /** The tube tips towards the other one, further as it empties. */
    private val side = if (targetLeft >= homeLeft) 1f else -1f
    private val angleRadians = side * (30f + 28f * poured) * (PI.toFloat() / 180f)

    private val homeCenterX = homeLeft + w / 2
    private val homeCenterY = homeTop + h / 2

    private fun rotate(x: Float, y: Float, radians: Float) =
        Offset(x * cos(radians) - y * sin(radians), x * sin(radians) + y * cos(radians))

    // The lip (the corner of the mouth on the pouring side) rests just above the other tube's mouth.
    private val lipTargetX = targetLeft + w / 2 - side * w * 0.1f
    private val lipTargetY = targetTop - w * 0.25f
    private val lipFromCenter = rotate(side * w / 2, -h / 2, angleRadians)
    private val farCenterX = lipTargetX - lipFromCenter.x
    private val farCenterY = lipTargetY - lipFromCenter.y

    val centerX = homeCenterX + (farCenterX - homeCenterX) * travel
    val centerY = homeCenterY + (farCenterY - homeCenterY) * travel
    val angleDegrees = angleRadians * 180f / PI.toFloat() * travel
    val translationX = centerX - homeCenterX
    val translationY = centerY - homeCenterY

    /** The lip right now, where the stream starts. */
    val lip: Offset = rotate(side * w / 2, -h / 2, angleRadians * travel).let { Offset(centerX + it.x, centerY + it.y) }

    /** Where the surface of the filling tube is, so the stream ends in the liquid. */
    fun surfaceY(targetContents: List<Int>): Float {
        val inner = h - 2 * TubePadding.value
        val filled = (targetContents.size + animation.amount * poured) / capacity
        return targetTop + h - TubePadding.value - inner * filled
    }

    val streamWidth: Float get() = w * 0.14f

    fun sourceSegments(contents: List<Int>): List<Segment> {
        val segments = contents.map { Segment(it, 1f) }.toMutableList()
        var remove = animation.amount * poured
        while (remove > 0f && segments.isNotEmpty()) {
            val top = segments.last()
            val take = minOf(remove, top.units)
            remove -= take
            if (top.units - take <= 0.001f) segments.removeAt(segments.lastIndex) else segments[segments.lastIndex] = top.copy(units = top.units - take)
        }
        return segments
    }

    fun targetSegments(contents: List<Int>): List<Segment> {
        val segments = contents.map { Segment(it, 1f) }
        val added = animation.amount * poured
        return if (added > 0.001f) segments + Segment(animation.color, added) else segments
    }
}

private const val CONFETTI_MILLIS = 900
private const val CONFETTI_PIECES = 16

/**
 * A few pieces of confetti that fly up out of a tube the moment it is finished, then fall and fade. Nothing for a tube
 * that was already finished when it appeared (a restored board) and nothing when motion is reduced. Never takes touches.
 */
@Composable
private fun CompletionConfetti(sorted: Boolean, seed: Int, centerX: Float, topY: Float, tubeWidth: Float) {
    val reduce = LocalReduceMotion.current
    val colors = BrainScrollTheme.colors
    val palette = listOf(colors.correctFill, colors.pendingFill, colors.streakDay, colors.streakAnswer, colors.error)
    val progress = remember { Animatable(1f) }
    var wasSorted by remember { mutableStateOf(sorted) }
    var burst by remember { mutableStateOf(0) }
    LaunchedEffect(sorted) {
        if (sorted && !wasSorted && !reduce) {
            burst++
            progress.snapTo(0f)
            progress.animateTo(1f, tween(CONFETTI_MILLIS, easing = LinearEasing))
        }
        wasSorted = sorted
    }
    if (progress.value >= 1f) return

    // Fixed pieces per tube and burst, so a screenshot test is stable.
    val pieces = remember(burst) {
        val random = Random(seed * 31 + burst)
        List(CONFETTI_PIECES) {
            // Mostly upwards, fanning out to both sides.
            val angle = (-90f + (random.nextFloat() - 0.5f) * 140f) * (PI.toFloat() / 180f)
            val speed = tubeWidth * (1.6f + random.nextFloat() * 1.8f)
            ConfettiPiece(cos(angle) * speed, sin(angle) * speed, tubeWidth * (0.07f + random.nextFloat() * 0.06f), (random.nextFloat() - 0.5f) * 540f, random.nextInt(palette.size))
        }
    }
    Canvas(Modifier.fillMaxSize().zIndex(3f).testTag("tubeConfetti")) {
        val t = progress.value
        val seconds = t * CONFETTI_MILLIS / 1000f
        val gravity = tubeWidth * 5f
        val alpha = if (t > 0.6f) (1f - t) / 0.4f else 1f
        for (piece in pieces) {
            val x = (centerX + piece.vx * seconds).dp.toPx()
            val y = (topY + piece.vy * seconds + 0.5f * gravity * seconds * seconds).dp.toPx()
            val side = piece.size.dp.toPx()
            rotate(degrees = piece.spin * seconds, pivot = Offset(x, y)) {
                drawRect(palette[piece.color].copy(alpha = alpha), Offset(x - side / 2, y - side / 2), Size(side, side * 0.6f))
            }
        }
    }
}

private class ConfettiPiece(val vx: Float, val vy: Float, val size: Float, val spin: Float, val color: Int)

/** The thin line of liquid between the two tubes. */
@Composable
private fun Stream(pour: PourGeometry, modifier: Modifier) {
    if (!pour.flowing) return
    val color = symbolColor(symbolOf(pour.animation.color))
    // The target tube is the one drawn at the end of the pour, whose surface rises with the pour.
    val surface = pour.surfaceY(pour.animation.before[pour.animation.to])
    Canvas(modifier) {
        val x = pour.lip.x.dp.toPx()
        val top = pour.lip.y.dp.toPx()
        val bottom = surface.dp.toPx()
        if (bottom > top) {
            drawRoundRect(
                color = color,
                topLeft = Offset(x - pour.streamWidth.dp.toPx() / 2, top),
                size = Size(pour.streamWidth.dp.toPx(), bottom - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(pour.streamWidth.dp.toPx() / 2),
            )
        }
    }
}

/**
 * One tube: an outline with its colors stacked from the bottom. Every color is a fill and a shape, so the colors can
 * be told apart without seeing them (docs/design.md: content is told apart by shape, not by color). The chosen tube
 * is lifted and outlined in yellow; a finished one (full, one color) is outlined in green and pops once (the board adds a little confetti, see [CompletionConfetti]).
 *
 * [contents] is what the screen reader and the finished check go by; [segments] is what is drawn (they differ while
 * liquid is on its way). [pour] moves and tips this tube while it is the one being poured.
 */
@Composable
private fun Tube(
    number: Int,
    contents: List<Int>,
    segments: List<Segment>,
    capacity: Int,
    selected: Boolean,
    width: Dp,
    height: Dp,
    modifier: Modifier,
    pour: PourGeometry?,
    onClick: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val reduce = LocalReduceMotion.current
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val lift by animateDpAsState(if (selected) -LiftHeight else 0.dp, if (reduce) snap() else tween(Motion.BASE, easing = Motion.Standard))
    val shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = width / 2, bottomEnd = width / 2)
    val inner = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = width / 2 - TubePadding, bottomEnd = width / 2 - TubePadding)
    val sorted = contents.size == capacity && contents.all { it == contents.first() }

    val names = contents.map { stringResource(symbolName(symbolOf(it))) }.joinToString(", ")
    val description = when {
        contents.isEmpty() -> stringResource(if (selected) Res.string.a11y_tube_selected_empty else Res.string.a11y_tube_empty, number)
        sorted -> stringResource(Res.string.a11y_tube_done, number, names)
        selected -> stringResource(Res.string.a11y_tube_selected, number, names)
        else -> stringResource(Res.string.a11y_tube_filled, number, names)
    }

    val density = LocalDensity.current
    Box(
        modifier = modifier
            .zIndex(if (pour != null) 2f else 0f)
            .offset(y = lift)
            .size(width, height)
            .graphicsLayer {
                if (pour != null) {
                    translationX = with(density) { pour.translationX.dp.toPx() }
                    translationY = with(density) { pour.translationY.dp.toPx() }
                    rotationZ = pour.angleDegrees
                }
            }
            .popOnAppear(sorted)
            .testTag("tube")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
                drawnUnits = segments.sumOf { it.units.toDouble() }.toFloat()
            }
            .clip(shape)
            .background(colors.bgSurface)
            .border(
                width = (if (selected) 2.5f else 1.5f).dp + extraBorder.dp,
                color = when {
                    selected -> colors.pendingFill
                    sorted -> colors.correctFill
                    else -> colors.borderStrong
                },
                shape = shape,
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(TubePadding),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // The segments are listed from the bottom, so the stack is drawn bottom-up.
        Column(modifier = Modifier.fillMaxSize().clip(inner), verticalArrangement = Arrangement.Bottom) {
            val slotHeight = (height - TubePadding * 2) / capacity
            segments.asReversed().forEach { segment ->
                val symbol = symbolOf(segment.color)
                Box(
                    modifier = Modifier.fillMaxWidth().height(slotHeight * segment.units).background(symbolColor(symbol)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (segment.units >= 0.6f) {
                        CardShape(symbol, colors.correctOn.copy(alpha = 0.55f), Modifier.size(minOf(slotHeight, width) * 0.5f))
                    }
                }
            }
        }
    }
}

private enum class ButtonIcon { Undo, Restart }

@Composable
private fun IconButton(icon: ButtonIcon, description: String, enabled: Boolean, tag: String, onClick: () -> Unit) {
    val iconColor = BrainScrollTheme.colors.textPrimary
    RoundIconButton(description = description, onClick = onClick, enabled = enabled, tag = tag) {
        when (icon) {
            ButtonIcon.Undo -> drawUndo(iconColor)
            ButtonIcon.Restart -> drawRestart(iconColor)
        }
    }
}

/** A U-turn arrow pointing left. */
internal fun DrawScope.drawUndo(color: androidx.compose.ui.graphics.Color) {
    val s = size.minDimension
    val stroke = Stroke(width = s * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val head = Path().apply {
        moveTo(s * 0.10f, s * 0.36f)
        lineTo(s * 0.36f, s * 0.14f)
        lineTo(s * 0.36f, s * 0.58f)
        close()
    }
    drawPath(head, color)
    val tail = Path().apply {
        moveTo(s * 0.30f, s * 0.36f)
        lineTo(s * 0.58f, s * 0.36f)
        arcTo(Rect(s * 0.36f, s * 0.36f, s * 0.80f, s * 0.80f), -90f, 180f, false)
        lineTo(s * 0.22f, s * 0.80f)
    }
    drawPath(tail, color, style = stroke)
}

/** A circular arrow, nearly closed, pointing clockwise. */
internal fun DrawScope.drawRestart(color: androidx.compose.ui.graphics.Color) {
    val s = size.minDimension
    val stroke = Stroke(width = s * 0.1f, cap = StrokeCap.Round)
    val start = -50f
    val sweep = 285f
    val box = Rect(s * 0.14f, s * 0.14f, s * 0.86f, s * 0.86f)
    drawArc(color, start, sweep, useCenter = false, topLeft = box.topLeft, size = Size(box.width, box.height), style = stroke)
    // The arrow head sits at the end of the arc, along its direction of travel.
    val end = (start + sweep) * (PI.toFloat() / 180f)
    val r = box.width / 2
    val c = box.center
    val tip = Offset(c.x + r * cos(end), c.y + r * sin(end))
    val tangent = Offset(-sin(end), cos(end))
    val normal = Offset(cos(end), sin(end))
    val size = s * 0.2f
    val head = Path().apply {
        moveTo(tip.x + tangent.x * size, tip.y + tangent.y * size)
        lineTo(tip.x + normal.x * size, tip.y + normal.y * size)
        lineTo(tip.x - normal.x * size, tip.y - normal.y * size)
        close()
    }
    drawPath(head, color)
}

@Composable
private fun TubesResultCard(page: TubesSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.tubes_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(Res.string.tubes_result, page.state.moves, formatDuration(page.durationMs)),
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

/**
 * The guide says "pour the first tube into the second": the first is ringed while nothing is picked up, and once it is
 * picked up the second one is ringed.
 */
@Composable
private fun guidedTube(index: Int, selected: Int?): Boolean {
    val cells = LocalGuide.current?.cells ?: return false
    if (cells.size != 2) return false
    return if (selected == null) index == cells[0] else selected == cells[0] && index == cells[1]
}
