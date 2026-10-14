package com.mehmtcan.brainscroll.ui.hanoi

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_hanoi_disks
import brainscroll.shared.generated.resources.a11y_hanoi_empty
import brainscroll.shared.generated.resources.a11y_hanoi_moves
import brainscroll.shared.generated.resources.a11y_hanoi_par
import brainscroll.shared.generated.resources.a11y_hanoi_restart
import brainscroll.shared.generated.resources.a11y_hanoi_tower
import brainscroll.shared.generated.resources.a11y_hanoi_tower_goal
import brainscroll.shared.generated.resources.a11y_hanoi_tower_selected
import brainscroll.shared.generated.resources.hanoi_goal
import brainscroll.shared.generated.resources.hanoi_hint
import brainscroll.shared.generated.resources.hanoi_moves
import brainscroll.shared.generated.resources.hanoi_par
import brainscroll.shared.generated.resources.hanoi_result
import brainscroll.shared.generated.resources.hanoi_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.hanoi.HanoiResult
import com.mehmtcan.brainscroll.game.hanoi.HanoiSnapshot
import com.mehmtcan.brainscroll.game.hanoi.HanoiStatus
import com.mehmtcan.brainscroll.game.memory.CardSymbol
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.feed.LocalGuide
import com.mehmtcan.brainscroll.ui.feed.guideHighlight
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.memory.symbolColor
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import org.jetbrains.compose.resources.stringResource

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val TowerGap = Spacing.sm

/** A disk is this high, and sits this far from the one below and from the bottom of its tower. */
private val DiskHeight = 28.dp
private val DiskGap = 2.dp
private val BottomPad = 10.dp

/** The room above the biggest pile for the disk that is carried over the towers, and the space below the last disk. */
private val HeadRoom = 26.dp

/** The whole carrying of a disk (up, across, down) takes this long. */
private const val MOVE_MILLIS = 420

/** How strongly the goal tower is tinted green. */
private const val GOAL_TINT = 0.13f

/** A disk being carried: from tower [from] to tower [to]. [before] are the towers as they were, the disk still on its own. */
private class DiskMove(val from: Int, val to: Int, val disk: Int, val before: List<List<Int>>)

/** The disks of a tower, from the bottom up, as the numbers written on them (the smallest is 1). For tests. */
internal val HanoiTowerDisks = SemanticsPropertyKey<List<Int>>("HanoiTowerDisks")
private var SemanticsPropertyReceiver.towerDisks by HanoiTowerDisks

/** Which tower is picked up (-1 for none), for tests. */
internal val HanoiPicked = SemanticsPropertyKey<Int>("HanoiPicked")
private var SemanticsPropertyReceiver.picked by HanoiPicked

/** The letter of a tower: the first is A. */
private fun towerName(tower: Int): String = ('A' + tower).toString()

/**
 * One full-screen page of the feed: the move counters, the three towers with their disks, and the restart button or the
 * result. A tap picks up the top disk of a tower, the next tap on another tower puts it there (if the rules allow it).
 */
@Composable
fun HanoiPage(
    page: HanoiSnapshot,
    onMove: (from: Int, to: Int) -> HanoiResult,
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
    val finished = state.status == HanoiStatus.Won

    // The tower whose top disk is lifted, waiting for the tower to go to. Only the screen knows about it.
    var selected by remember(page.id) { mutableStateOf<Int?>(null) }

    // The disk on its way. The game has the result the moment the player taps; the screen takes its time to show it and
    // meanwhile draws the towers as they were, with the disk in the air. Nothing else is accepted while it moves.
    var moving by remember(page.id) { mutableStateOf<DiskMove?>(null) }
    val progress = remember(page.id) { Animatable(0f) }
    LaunchedEffect(moving) {
        if (moving == null) return@LaunchedEffect
        progress.animateTo(1f, tween(MOVE_MILLIS, easing = LinearEasing))
        moving = null
        // Back to the start for the next move. It has to be done here: if the next move began from the old value, its first
        // frame would be drawn at the end of the way (the disk already on the other tower), and only then jump back.
        progress.snapTo(0f)
    }
    val shown = moving?.before ?: state.towers

    // The result, the buzz and the card wait until the last disk has landed.
    val won = finished && moving == null
    val status = if (won) WordleStatus.Won else WordleStatus.Playing
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Chip(
                text = stringResource(Res.string.hanoi_moves, state.moves),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_hanoi_moves, state.moves),
            )
            Chip(
                text = stringResource(Res.string.hanoi_par, state.par),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_hanoi_par, state.par),
            )
        }
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("hanoiBoard").semantics { picked = selected ?: -1 },
            contentAlignment = Alignment.Center,
        ) {
            val towerWidth = (maxWidth - TowerGap * 2) / 3
            val total = state.pegs.size
            // As high as the biggest pile needs, with room above it for the disk that is carried over the others.
            val areaHeight = minOf((DiskHeight + DiskGap) * (total + 1) + HeadRoom, maxHeight - 40.dp).coerceAtLeast(120.dp)
            Box {
                Row(horizontalArrangement = Arrangement.spacedBy(TowerGap)) {
                    for (tower in 0 until 3) {
                        // While a disk is carried, the tower it left is drawn without it (the disk is drawn in the air).
                        val inTower = if (moving?.from == tower) shown[tower].dropLast(1) else shown[tower]
                        Tower(
                            tower = tower,
                            disks = inTower,
                            totalDisks = total,
                            isGoal = tower == state.goal,
                            isSelected = selected == tower,
                            guided = guidedTower(tower, selected),
                            playing = !finished,
                            width = towerWidth,
                            height = areaHeight,
                            onTap = {
                                val from = selected
                                when {
                                    finished || moving != null -> Unit
                                    from == null -> if (state.towers[tower].isNotEmpty()) { selected = tower; haptics.perform(HapticKind.Light) } else haptics.perform(HapticKind.Warning)
                                    from == tower -> selected = null
                                    else -> {
                                        val before = state.towers
                                        when (onMove(from, tower)) {
                                            HanoiResult.Moved, HanoiResult.Solved -> {
                                                selected = null
                                                haptics.perform(HapticKind.Medium)
                                                if (!reduce) moving = DiskMove(from, tower, before[from].last(), before)
                                            }
                                            // Not allowed (a bigger disk on a smaller one): the disk stays lifted, the phone buzzes.
                                            HanoiResult.Ignored -> haptics.perform(HapticKind.Warning)
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
                moving?.let { FlyingDisk(it, progress.value, total, towerWidth, areaHeight) }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (won) {
                HanoiResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.hanoi_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val iconColor = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_hanoi_restart),
                        onClick = { selected = null; if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.canRestart,
                        tag = "hanoiRestart",
                    ) { drawRestart(iconColor) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** One tower: the frame with its peg and disks, and under it the letter (with the word Goal for the goal tower). */
@Composable
private fun Tower(
    tower: Int,
    disks: List<Int>,
    totalDisks: Int,
    isGoal: Boolean,
    isSelected: Boolean,
    guided: Boolean,
    playing: Boolean,
    width: Dp,
    height: Dp,
    onTap: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val reduce = LocalReduceMotion.current
    val diskHeight = minOf(DiskHeight, (height - HeadRoom) / (totalDisks + 1))
    val names = disks.map { it + 1 }
    val description = buildString {
        append(stringResource(if (isSelected) Res.string.a11y_hanoi_tower_selected else Res.string.a11y_hanoi_tower, towerName(tower), if (disks.isEmpty()) stringResource(Res.string.a11y_hanoi_empty) else stringResource(Res.string.a11y_hanoi_disks, names.joinToString(", "))))
        if (isGoal) append(", ").append(stringResource(Res.string.a11y_hanoi_tower_goal))
    }
    val shape = Radius.card

    Column(Modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        // The ring of the guide is on a box of its own around the tower, so its tag is not hidden by the tag of the tower.
        Box(Modifier.fillMaxWidth().height(height).guideHighlight(guided, shape)) {
        Box(
            modifier = Modifier
                .testTag("hanoiTower")
                .fillMaxSize()
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    towerDisks = names
                    role = Role.Button
                    // A screen reader says "selected" for the tower whose top disk is picked up.
                    this.selected = isSelected
                }
                .background(colors.bgSurface, shape)
                // The goal is told apart by a green tint and a flag on its peg, so the border is left to the choice of the
                // player: a thick yellow one on the tower that is picked up, a faint one on every other.
                .then(if (isGoal) Modifier.background(colors.correctFill.copy(alpha = GOAL_TINT), shape) else Modifier)
                .border(
                    width = (if (isSelected) 2.5f else 1.5f).dp + extraBorder.dp,
                    color = if (isSelected) colors.pendingFill else colors.borderStrong.copy(alpha = 0.3f),
                    shape = shape,
                )
                .then(
                    if (playing) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onTap)
                    else Modifier,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // The peg, and on the goal tower a flag at its top.
            val pegColor = colors.borderStrong.copy(alpha = 0.6f)
            val flagColor = colors.correctFill
            Canvas(Modifier.fillMaxSize()) {
                val pegWidth = 6.dp.toPx()
                val pegHeight = size.height * 0.9f
                val x = size.width / 2f
                val top = size.height - 6.dp.toPx() - pegHeight
                drawRoundRect(pegColor, Offset(x - pegWidth / 2f, top), Size(pegWidth, pegHeight), CornerRadius(pegWidth / 2f))
                if (isGoal) {
                    val flag = Path().apply {
                        moveTo(x + pegWidth / 2f, top)
                        lineTo(x + pegWidth / 2f + 24.dp.toPx(), top + 8.dp.toPx())
                        lineTo(x + pegWidth / 2f, top + 16.dp.toPx())
                        close()
                    }
                    drawPath(flag, flagColor)
                }
            }
            // The disks, the biggest at the bottom. The top one of a picked-up tower is lifted.
            Column(
                modifier = Modifier.padding(bottom = BottomPad),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DiskGap),
            ) {
                disks.asReversed().forEachIndexed { fromTop, disk ->
                    val lift by animateDpAsState(
                        targetValue = if (isSelected && fromTop == 0) (-14).dp else 0.dp,
                        animationSpec = if (reduce) snap() else tween(Motion.BASE, easing = Motion.Standard),
                    )
                    Disk(disk = disk, totalDisks = totalDisks, towerWidth = width, height = diskHeight, modifier = Modifier.offset(y = lift))
                }
            }
        }
        }
        BasicText(
            text = if (isGoal) "${towerName(tower)} · ${stringResource(Res.string.hanoi_goal)}" else towerName(tower),
            style = type.body.copy(color = if (isGoal) colors.correctFill else colors.textSecondary, fontWeight = if (isGoal) FontWeight.Bold else FontWeight.Normal),
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

/**
 * The disk that is being carried: up out of its tower to just under the top, across to the other tower, and down onto its pile.
 * [progress] runs from 0 to 1 over the whole way (a third up, a third across, a third down, each easing in and out).
 */
@Composable
private fun FlyingDisk(move: DiskMove, progress: Float, totalDisks: Int, towerWidth: Dp, areaHeight: Dp) {
    val diskHeight = minOf(DiskHeight, (areaHeight - HeadRoom) / (totalDisks + 1))
    fun centerX(tower: Int): Dp = (towerWidth + TowerGap) * tower + towerWidth / 2
    fun topY(level: Int): Dp = areaHeight - BottomPad - diskHeight * (level + 1) - DiskGap * level
    val startX = centerX(move.from)
    val endX = centerX(move.to)
    val startY = topY(move.before[move.from].size - 1)
    val endY = topY(move.before[move.to].size)
    val travelY = minOf(2.dp, startY, endY)
    fun ease(t: Float) = FastOutSlowInEasing.transform(t.coerceIn(0f, 1f))
    val x: Dp
    val y: Dp
    when {
        progress < UP_END -> { x = startX; y = lerp(startY, travelY, ease(progress / UP_END)) }
        progress < ACROSS_END -> { x = lerp(startX, endX, ease((progress - UP_END) / (ACROSS_END - UP_END))); y = travelY }
        else -> { x = endX; y = lerp(travelY, endY, ease((progress - ACROSS_END) / (1f - ACROSS_END))) }
    }
    val fraction = if (totalDisks <= 1) 0.9f else 0.42f + 0.5f * move.disk / (totalDisks - 1)
    val width = (towerWidth - 12.dp) * fraction
    Disk(
        disk = move.disk, totalDisks = totalDisks, towerWidth = towerWidth, height = diskHeight,
        modifier = Modifier.offset(x - width / 2, y), tag = "hanoiFlyingDisk",
    )
}

/** Where the carrying of a disk turns from going up to going across, and from going across to coming down. */
private const val UP_END = 0.3f
private const val ACROSS_END = 0.7f

/** A disk: a rounded bar as wide as its size says, in its own color, with its number on it (so it is not only a color). */
@Composable
private fun Disk(disk: Int, totalDisks: Int, towerWidth: Dp, height: Dp, modifier: Modifier = Modifier, tag: String = "hanoiDisk") {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val fraction = if (totalDisks <= 1) 0.9f else 0.42f + 0.5f * disk / (totalDisks - 1)
    Box(
        modifier = modifier
            .testTag(tag)
            .width((towerWidth - 12.dp) * fraction)
            .height(height)
            .background(symbolColor(CardSymbol.entries[disk % CardSymbol.entries.size], colors), RoundedCornerShape(height / 2.5f)),
        contentAlignment = Alignment.Center,
    ) {
        BasicText((disk + 1).toString(), style = type.label.copy(color = colors.bgPage, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun HanoiResultCard(page: HanoiSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.hanoi_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(Res.string.hanoi_result, page.state.moves, formatDuration(page.durationMs)),
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
 * The guide says "move the top disk of the first tower onto the second": the first tower is ringed while nothing is picked
 * up, and once it is picked up the second one is ringed.
 */
@Composable
private fun guidedTower(tower: Int, selected: Int?): Boolean {
    val cells = LocalGuide.current?.cells ?: return false
    if (cells.size != 2) return false
    return if (selected == null) tower == cells[0] else selected == cells[0] && tower == cells[1]
}
