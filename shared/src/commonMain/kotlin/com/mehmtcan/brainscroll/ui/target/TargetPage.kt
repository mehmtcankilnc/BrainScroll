package com.mehmtcan.brainscroll.ui.target

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_target_goal
import brainscroll.shared.generated.resources.a11y_target_moves
import brainscroll.shared.generated.resources.a11y_target_number
import brainscroll.shared.generated.resources.a11y_target_number_selected
import brainscroll.shared.generated.resources.a11y_target_op_add
import brainscroll.shared.generated.resources.a11y_target_op_div
import brainscroll.shared.generated.resources.a11y_target_op_mul
import brainscroll.shared.generated.resources.a11y_target_op_selected
import brainscroll.shared.generated.resources.a11y_target_op_sub
import brainscroll.shared.generated.resources.a11y_target_par
import brainscroll.shared.generated.resources.a11y_target_restart
import brainscroll.shared.generated.resources.a11y_target_undo
import brainscroll.shared.generated.resources.game_target
import brainscroll.shared.generated.resources.swipe_hint
import brainscroll.shared.generated.resources.target_hint
import brainscroll.shared.generated.resources.target_label
import brainscroll.shared.generated.resources.target_moves
import brainscroll.shared.generated.resources.target_par
import brainscroll.shared.generated.resources.target_result
import brainscroll.shared.generated.resources.target_won
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.target.Op
import com.mehmtcan.brainscroll.game.target.Step
import com.mehmtcan.brainscroll.game.target.TargetResult
import com.mehmtcan.brainscroll.game.target.TargetSnapshot
import com.mehmtcan.brainscroll.game.target.TargetStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.howto.HowToPlayButton
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.slideFadeIn
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import com.mehmtcan.brainscroll.ui.tubes.drawUndo
import org.jetbrains.compose.resources.stringResource

/** Height reserved for the bottom area, the same as the other games, so the numbers do not jump between them. */
private val BottomAreaHeight = 152.dp
private val TileGap = 10.dp
private val MaxTile = 88.dp
private val KeyHeight = 56.dp

/** The value and the id of a number on the table, for tests. */
internal val TargetTokenValue = SemanticsPropertyKey<Int>("TargetTokenValue")
internal val TargetTokenId = SemanticsPropertyKey<Int>("TargetTokenId")
private var SemanticsPropertyReceiver.tokenValue by TargetTokenValue
private var SemanticsPropertyReceiver.tokenId by TargetTokenId

/** The sign the player sees for an operation (the saved symbol is the plain one). */
private fun Op.label(): String = when (this) {
    Op.Add -> "+"
    Op.Sub -> "−"
    Op.Mul -> "×"
    Op.Div -> "÷"
}

private fun Op.tag(): String = "targetOp" + name

/**
 * One full-screen page of the feed: the title, the counters, the target, the numbers on the table, the operations done,
 * and the keys. A move is a number, an operation and another number, tapped in that order.
 */
@Composable
fun TargetPage(
    page: TargetSnapshot,
    onCombine: (firstId: Int, op: Op, secondId: Int) -> TargetResult,
    preview: (firstId: Int, op: Op, secondId: Int) -> Int?,
    onUndo: () -> Boolean,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onHowToPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val won = state.status == TargetStatus.Won

    val status = if (won) WordleStatus.Won else WordleStatus.Playing
    OutcomeHaptics(status)
    val justFinished = rememberJustFinished(status)

    // The move being built: the first number and the operation. Gone after the move, or when the number is gone (undo).
    var pickedFirst by remember(page.id) { mutableStateOf<Int?>(null) }
    var pickedOp by remember(page.id) { mutableStateOf<Op?>(null) }
    val first = pickedFirst?.takeIf { id -> state.tokens.any { it.id == id } }
    val op = pickedOp.takeIf { first != null }

    fun clear() {
        pickedFirst = null
        pickedOp = null
    }

    fun tapNumber(id: Int) {
        when {
            first == null -> { pickedFirst = id; haptics.perform(HapticKind.Light) }
            id == first -> clear()
            op == null -> { pickedFirst = id; haptics.perform(HapticKind.Light) }
            else -> {
                val result = onCombine(first, op, id)
                clear()
                haptics.perform(if (result == TargetResult.Ignored) HapticKind.Warning else HapticKind.Medium)
            }
        }
    }

    fun tapOp(chosen: Op) {
        if (first == null) return
        pickedOp = if (op == chosen) null else chosen
        haptics.perform(HapticKind.Light)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        BasicText(stringResource(Res.string.game_target), style = type.heading.copy(color = colors.textPrimary))
        HowToPlayButton(onHowToPlay)
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Chip(
                text = stringResource(Res.string.target_moves, state.moves),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_target_moves, state.moves),
            )
            Chip(
                text = stringResource(Res.string.target_par, state.par),
                textColor = colors.textSecondary,
                description = stringResource(Res.string.a11y_target_par, state.par),
            )
        }
        Spacer(Modifier.height(Spacing.md))

        TargetCard(state.target, reached = won)

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val count = page.state.tokens.size.coerceAtLeast(4)
            val tile: Dp = minOf(MaxTile, (maxWidth - TileGap * (count - 1)) / count)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                Row(horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                    for (token in state.tokens) {
                        // With a number and an operation picked, the numbers that make a move not allowed are dimmed.
                        val fits = first == null || op == null || token.id == first || preview(first, op, token.id) != null
                        NumberTile(
                            value = token.value,
                            id = token.id,
                            size = tile,
                            selected = token.id == first,
                            dimmed = !fits,
                            reached = won && token.value == state.target,
                            playing = !won,
                            onClick = { tapNumber(token.id) },
                        )
                    }
                }
                Steps(state.steps)
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (won) {
                TargetResultCard(page, isFavorite, onToggleFavorite, Modifier.slideFadeIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        for (candidate in Op.entries) {
                            OpKey(candidate, selected = op == candidate, enabled = first != null, onClick = { tapOp(candidate) }, modifier = Modifier.weight(1f))
                        }
                    }
                    val iconColor = colors.textPrimary
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        RoundIconButton(
                            description = stringResource(Res.string.a11y_target_undo),
                            onClick = { clear(); if (onUndo()) haptics.perform(HapticKind.Light) },
                            enabled = state.canUndo,
                            tag = "targetUndo",
                        ) { drawUndo(iconColor) }
                        RoundIconButton(
                            description = stringResource(Res.string.a11y_target_restart),
                            onClick = { clear(); if (onRestart()) haptics.perform(HapticKind.Light) },
                            enabled = state.canRestart,
                            tag = "targetRestart",
                        ) { drawRestart(iconColor) }
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** The target, large, with its name above. It turns green once reached. */
@Composable
private fun TargetCard(target: Int, reached: Boolean) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val goal = stringResource(Res.string.a11y_target_goal, target)
    Column(
        modifier = Modifier
            .background(colors.bgSurface, Radius.card)
            .padding(horizontal = Spacing.xl, vertical = Spacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = goal }
            .testTag("targetGoal"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(stringResource(Res.string.target_label), style = type.caption.copy(color = colors.textSecondary))
        BasicText(
            text = target.toString(),
            style = type.display.copy(color = if (reached) colors.correctFill else colors.pendingFill, fontSize = 52.sp, fontWeight = FontWeight.Bold),
        )
    }
}

/** A number on the table. The text size follows the tile (and the digits), not the text size setting: it must fit. */
@Composable
private fun NumberTile(
    value: Int,
    id: Int,
    size: Dp,
    selected: Boolean,
    dimmed: Boolean,
    reached: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val density = LocalDensity.current
    val shape = Radius.card
    val digits = value.toString().length
    val fontSize = with(density) { (size * (if (digits <= 2) 0.46f else if (digits == 3) 0.36f else 0.29f)).toSp() }
    val description = stringResource(if (selected) Res.string.a11y_target_number_selected else Res.string.a11y_target_number, value)

    Box(
        modifier = Modifier
            .size(size)
            .alpha(if (dimmed) 0.35f else 1f)
            .testTag("targetNumber")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                tokenValue = value
                tokenId = id
                this.selected = selected
                role = Role.Button
            }
            .background(if (selected) colors.bgRaised else colors.bgSurface, shape)
            .border(
                (if (selected) 3f else 1.5f + extraBorder).dp,
                when {
                    reached -> colors.correctFill
                    selected -> colors.pendingFill
                    else -> colors.borderStrong.copy(alpha = 0.5f)
                },
                shape,
            )
            .then(
                if (playing) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = value.toString(),
            style = BrainScrollTheme.typography.display.copy(
                color = if (reached) colors.correctFill else colors.textPrimary,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/** The last three operations, newest at the bottom. The room is always three lines, so the page does not jump. */
@Composable
private fun Steps(steps: List<Step>) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(
        modifier = Modifier.height(66.dp).testTag("targetSteps"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        for (step in steps.takeLast(3)) {
            BasicText(
                text = "${step.a} ${step.op.label()} ${step.b} = ${step.result}",
                style = type.body.copy(color = colors.textSecondary),
            )
        }
    }
}

/** One operation key: at least 48 dp tall. It only works once a number is picked. */
@Composable
private fun OpKey(op: Op, selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val name = stringResource(
        when (op) {
            Op.Add -> Res.string.a11y_target_op_add
            Op.Sub -> Res.string.a11y_target_op_sub
            Op.Mul -> Res.string.a11y_target_op_mul
            Op.Div -> Res.string.a11y_target_op_div
        },
    )
    val description = if (selected) stringResource(Res.string.a11y_target_op_selected, name) else name

    Box(
        modifier = modifier
            .height(KeyHeight)
            .alpha(if (enabled) 1f else 0.35f)
            .testTag(op.tag())
            .background(if (selected) colors.bgRaised else colors.bgSurface, Radius.control)
            .border(
                (if (selected) 2.5f else 1.5f + extraBorder).dp,
                if (selected) colors.pendingFill else colors.borderStrong.copy(alpha = 0.6f),
                Radius.control,
            )
            .semantics {
                contentDescription = description
                this.selected = selected
                role = Role.Button
                if (!enabled) disabled()
            }
            .then(
                if (enabled) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(op.label(), style = BrainScrollTheme.typography.title.copy(color = colors.textPrimary))
    }
}

@Composable
private fun TargetResultCard(page: TargetSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.target_won),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            BasicText(
                text = stringResource(Res.string.target_result, page.state.moves, formatDuration(page.durationMs)),
                style = type.body.copy(color = colors.textPrimary),
            )
            BasicText(
                text = "↑  " + stringResource(Res.string.swipe_hint),
                style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            )
        }
        HeartButton(filled = isFavorite, onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd))
    }
}
