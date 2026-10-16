package com.mehmtcan.brainscroll.ui.queens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_queens_attacked
import brainscroll.shared.generated.resources.a11y_queens_empty
import brainscroll.shared.generated.resources.a11y_queens_locked
import brainscroll.shared.generated.resources.a11y_queens_moves
import brainscroll.shared.generated.resources.a11y_queens_par
import brainscroll.shared.generated.resources.a11y_queens_queen
import brainscroll.shared.generated.resources.a11y_queens_restart
import brainscroll.shared.generated.resources.queens_hint
import brainscroll.shared.generated.resources.queens_moves
import brainscroll.shared.generated.resources.queens_par
import brainscroll.shared.generated.resources.queens_result
import brainscroll.shared.generated.resources.queens_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.queens.QueensResult
import com.mehmtcan.brainscroll.game.queens.QueensSnapshot
import com.mehmtcan.brainscroll.game.queens.QueensStatus
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
import com.mehmtcan.brainscroll.ui.motion.popOnAppear
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import org.jetbrains.compose.resources.stringResource

/** Height reserved for the bottom area, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val CellGap = 4.dp
private val MaxCell = 68.dp

/** What a cell holds, for tests: 0 empty, 1 queen, 2 locked queen, 3 queen that is attacked. */
internal val QueensCellValue = SemanticsPropertyKey<Int>("QueensCellValue")
private var SemanticsPropertyReceiver.cellValue by QueensCellValue

/**
 * One full-screen page of the feed: the move counters, the board, and the restart button or the result. A tap puts a queen on
 * an empty cell or takes away one the player put there; the queens that attack each other turn red.
 */
@Composable
fun QueensPage(
    page: QueensSnapshot,
    onTap: (Int) -> QueensResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val won = state.status == QueensStatus.Won

    // The result, the buzz and the card wait until the last queen has settled into its cell.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, Motion.POP.toLong() + 50)
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
                text = stringResource(Res.string.queens_moves, state.moves),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_queens_moves, state.moves),
            )
            Chip(
                text = stringResource(Res.string.queens_par, state.par),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_queens_par, state.par),
            )
        }
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().testTag("queensBoard"), contentAlignment = Alignment.Center) {
            val cellSize: Dp = minOf(MaxCell, (maxWidth - CellGap * (state.size - 1)) / state.size, (maxHeight - CellGap * (state.size - 1)) / state.size)
            Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
                for (row in 0 until state.size) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                        for (column in 0 until state.size) {
                            val index = row * state.size + column
                            GuideMark(isGuided(index), Radius.card) {
                                Cell(
                                    row = row + 1,
                                    column = column + 1,
                                    dark = (row + column) % 2 == 1,
                                    queen = state.queens[index],
                                    locked = state.locked[index],
                                    attacked = state.attacked[index],
                                    playing = !won,
                                    size = cellSize,
                                    onClick = {
                                        when (onTap(index)) {
                                            QueensResult.Placed -> haptics.perform(HapticKind.Light)
                                            QueensResult.Removed -> haptics.perform(HapticKind.Light)
                                            QueensResult.Solved -> haptics.perform(HapticKind.Light)
                                            // A locked queen cannot be taken away.
                                            QueensResult.Ignored -> if (state.locked[index]) haptics.perform(HapticKind.Warning)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                QueensResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.queens_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val iconColor = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_queens_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.canRestart,
                        tag = "queensRestart",
                    ) { drawRestart(iconColor) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/**
 * One cell of the board, light or dark like a chessboard. A queen is a crown; one that attacks another is red and has a cross
 * on it, so it is told apart by more than its color; a locked queen has a small padlock.
 */
@Composable
private fun Cell(row: Int, column: Int, dark: Boolean, queen: Boolean, locked: Boolean, attacked: Boolean, playing: Boolean, size: Dp, onClick: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val shape = Radius.card
    val duration = if (LocalReduceMotion.current) 0 else Motion.BASE
    val fill by animateColorAsState(
        when {
            attacked -> colors.error.copy(alpha = 0.22f)
            dark -> colors.borderStrong.copy(alpha = 0.22f)
            else -> colors.bgSurface
        },
        tween(duration, easing = Motion.Standard),
    )
    val pieceColor = when {
        attacked -> colors.error
        locked -> colors.textPrimary
        else -> colors.pendingFill
    }
    val description = stringResource(
        when {
            !queen -> Res.string.a11y_queens_empty
            attacked -> Res.string.a11y_queens_attacked
            locked -> Res.string.a11y_queens_locked
            else -> Res.string.a11y_queens_queen
        },
        row, column,
    )

    Box(
        modifier = Modifier
            .size(size)
            .testTag("queensCell")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                cellValue = when {
                    !queen -> 0
                    attacked -> 3
                    locked -> 2
                    else -> 1
                }
                role = Role.Button
            }
            .background(fill, shape)
            .border((1.5f + extraBorder).dp, if (attacked) colors.error else colors.borderStrong.copy(alpha = 0.35f), shape)
            .then(
                if (playing) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Always there, so a queen that appears can pop into its cell.
        Box(Modifier.size(size * 0.7f).popOnAppear(queen)) {
            if (queen) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCrown(pieceColor)
                    if (attacked) drawCross(colors.bgPage)
                }
            }
        }
        if (queen && locked) {
            Canvas(Modifier.size(size * 0.24f).align(Alignment.BottomEnd).padding(end = 3.dp, bottom = 3.dp)) { drawLock(colors.textPrimary) }
        }
    }
}

/** A crown: a band at the bottom and five points above it, each with a ball on its tip. */
internal fun DrawScope.drawCrown(color: Color) {
    val w = size.width
    val h = size.height
    val band = Path().apply {
        moveTo(w * 0.08f, h * 0.88f)
        lineTo(w * 0.92f, h * 0.88f)
        lineTo(w * 0.98f, h * 0.30f)
        lineTo(w * 0.72f, h * 0.55f)
        lineTo(w * 0.5f, h * 0.18f)
        lineTo(w * 0.28f, h * 0.55f)
        lineTo(w * 0.02f, h * 0.30f)
        close()
    }
    drawPath(band, color)
    val ball = w * 0.07f
    for ((x, y) in listOf(0.02f to 0.28f, 0.5f to 0.14f, 0.98f to 0.28f)) drawCircle(color, ball, Offset(w * x, h * y))
}

/** A cross over the crown, in the color of the cell behind it. */
private fun DrawScope.drawCross(color: Color) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = w * 0.1f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    drawLine(color, Offset(w * 0.32f, h * 0.4f), Offset(w * 0.68f, h * 0.76f), strokeWidth = stroke.width, cap = stroke.cap)
    drawLine(color, Offset(w * 0.68f, h * 0.4f), Offset(w * 0.32f, h * 0.76f), strokeWidth = stroke.width, cap = stroke.cap)
}

/** A small padlock: a body and a shackle. */
private fun DrawScope.drawLock(color: Color) {
    val w = size.width
    val h = size.height
    drawRoundRect(color, Offset(w * 0.1f, h * 0.45f), Size(w * 0.8f, h * 0.5f), CornerRadius(w * 0.12f))
    drawArc(
        color, startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(w * 0.26f, h * 0.05f), size = Size(w * 0.48f, h * 0.7f),
        style = Stroke(width = w * 0.14f),
    )
}

@Composable
private fun QueensResultCard(page: QueensSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.queens_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(Res.string.queens_result, page.state.moves, formatDuration(page.durationMs)),
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
