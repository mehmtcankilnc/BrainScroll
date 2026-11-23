package com.mehmtcan.brainscroll.ui.fences

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_fences_board
import brainscroll.shared.generated.resources.a11y_fences_clue
import brainscroll.shared.generated.resources.a11y_fences_cross
import brainscroll.shared.generated.resources.a11y_fences_edge_h
import brainscroll.shared.generated.resources.a11y_fences_edge_v
import brainscroll.shared.generated.resources.a11y_fences_empty
import brainscroll.shared.generated.resources.a11y_fences_line
import brainscroll.shared.generated.resources.a11y_fences_restart
import brainscroll.shared.generated.resources.fences_hint
import brainscroll.shared.generated.resources.fences_lines
import brainscroll.shared.generated.resources.fences_result
import brainscroll.shared.generated.resources.fences_won
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.fences.FencePuzzle
import com.mehmtcan.brainscroll.game.fences.FenceResult
import com.mehmtcan.brainscroll.game.fences.FenceSnapshot
import com.mehmtcan.brainscroll.game.fences.FenceSolver
import com.mehmtcan.brainscroll.game.fences.FenceStatus
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import com.mehmtcan.brainscroll.ui.components.ResultStats
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.feed.GuideMark
import com.mehmtcan.brainscroll.ui.feed.isGuideEvidence
import com.mehmtcan.brainscroll.ui.feed.isGuided
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
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

/** At least this high, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val MaxBoard = 380.dp

/** The room round the outer dots, so the lines at the border are not cut and can be tapped. */
private val BoardPad = 16.dp

/** The mark of an edge, for tests: 0 none, 1 line, 2 cross. */
internal val FenceMarkKey = SemanticsPropertyKey<Int>("FenceMark")
private var SemanticsPropertyReceiver.fenceMark by FenceMarkKey

/** The board as it is saved (see [FencePuzzle.encode]), for tests: the board is random, so a test reads it the way a screen reader would. */
internal val FencePuzzleKey = SemanticsPropertyKey<String>("FencePuzzle")
private var SemanticsPropertyReceiver.fencePuzzle by FencePuzzleKey

/**
 * One full-screen page of the feed: how many lines are drawn, the board (the dots, the numbers, the lines) and the restart button.
 * Tapping an edge between two dots draws a line, again puts a cross (no line here), again takes the mark off. The result takes the place
 * of the hint and the button when the loop is closed and right. A dot where lines branch and a square with too many lines are red.
 */
@Composable
fun FencesPage(
    page: FenceSnapshot,
    onTap: (Int) -> FenceResult,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val puzzle = state.puzzle
    val n = puzzle.size
    val g = puzzle.geometry
    val won = state.status == FenceStatus.Won

    // The result and the buzz wait a moment so the last line can be seen.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 350L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    val lines = state.lines
    val world = GameKind.Fences.world
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val branching = state.branching
    val overfull = state.overfull
    val satisfied = state.satisfied

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Chip(text = stringResource(Res.string.fences_lines, state.lineCount), textColor = if (won) colors.correctFill else colors.textSecondary)
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side: Dp = minOf(MaxBoard, maxWidth, maxHeight)
            val cell: Dp = (side - BoardPad * 2) / n
            val description = stringResource(Res.string.a11y_fences_board, n, n, state.lineCount)
            val saved = puzzle.encode()
            val emptyText = stringResource(Res.string.a11y_fences_empty)
            val lineText = stringResource(Res.string.a11y_fences_line)
            val crossText = stringResource(Res.string.a11y_fences_cross)
            Box(
                modifier = Modifier.size(side).testTag("fenceBoard").semantics {
                    contentDescription = description
                    fencePuzzle = saved
                },
            ) {
                Canvas(Modifier.size(side)) {
                    val pad = BoardPad.toPx()
                    val step = cell.toPx()
                    fun dot(index: Int) = Offset(pad + (index % (n + 1)) * step, pad + (index / (n + 1)) * step)
                    // The inside of the loop, when it is closed: a square is inside when an odd number of vertical lines are to its left.
                    if (won) {
                        for (c in 0 until n * n) {
                            val row = c / n
                            val column = c % n
                            var crossings = 0
                            for (k in 0..column) if (lines[g.horizontal + row * (n + 1) + k]) crossings++
                            if (crossings % 2 == 1) {
                                drawRect(world.copy(alpha = 0.22f), Offset(pad + column * step, pad + row * step), Size(step, step))
                            }
                        }
                    }
                    // The edges: a faint guide for the free ones, a cross for "no line here", a thick line for a line.
                    val stroke = 5.dp.toPx()
                    for (e in 0 until g.edges) {
                        val a = dot(g.edgeDots[e][0])
                        val b = dot(g.edgeDots[e][1])
                        when (state.marks[e]) {
                            FenceSolver.LINE -> {
                                val wrong = !won && g.edgeDots[e].any { it in branching }
                                drawLine(if (wrong) colors.error else if (won) colors.correctFill else world, a, b, stroke, StrokeCap.Round)
                            }
                            FenceSolver.CROSS -> {
                                val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                                val r = step * 0.12f
                                val ink = colors.textSecondary.copy(alpha = 0.8f)
                                drawLine(ink, Offset(mid.x - r, mid.y - r), Offset(mid.x + r, mid.y + r), 2.dp.toPx(), StrokeCap.Round)
                                drawLine(ink, Offset(mid.x - r, mid.y + r), Offset(mid.x + r, mid.y - r), 2.dp.toPx(), StrokeCap.Round)
                            }
                            else -> drawLine(colors.borderStrong.copy(alpha = 0.28f), a, b, 1.5.dp.toPx(), StrokeCap.Round)
                        }
                    }
                    // The dots on top of the lines.
                    for (d in 0 until g.dots) {
                        drawCircle(if (d in branching && !won) colors.error else colors.textSecondary, 3.dp.toPx(), dot(d))
                    }
                    // The numbers.
                    for (c in 0 until n * n) {
                        val clue = puzzle.clues[c]
                        if (clue < 0) continue
                        val color = when {
                            c in overfull -> colors.error
                            c in satisfied && clue > 0 -> colors.correctFill
                            else -> colors.textPrimary
                        }
                        val layout = measurer.measure(clue.toString(), TextStyle(fontSize = with(density) { (step * 0.46f).toSp() }, fontWeight = FontWeight.Bold, color = color))
                        drawText(layout, topLeft = Offset(pad + (c % n) * step + (step - layout.size.width) / 2f, pad + (c / n) * step + (step - layout.size.height) / 2f))
                    }
                }
                // For a screen reader: the numbers say how many lines their square has now.
                for (c in 0 until n * n) {
                    val clue = puzzle.clues[c]
                    if (clue < 0) continue
                    val have = g.cellEdges[c].count { lines[it] }
                    val text = stringResource(Res.string.a11y_fences_clue, c / n + 1, c % n + 1, clue, have)
                    // The square that makes a line the answer (the guide rings it softly): its number in the guide is the edges plus its place.
                    Box(Modifier.offset(BoardPad + cell * (c % n) + cell * 0.1f, BoardPad + cell * (c / n) + cell * 0.1f)) {
                        GuideMark(false, Radius.tile, evidence = isGuideEvidence(g.edges + c) && !won) {
                            Box(Modifier.size(cell * 0.8f).semantics { contentDescription = text })
                        }
                    }
                }
                // The edges to tap, in the order of their numbers. Each one is a wide invisible target along its edge.
                val along = cell * 0.7f
                val across = cell * 0.5f
                for (e in 0 until g.edges) {
                    val horizontal = g.isHorizontal(e)
                    val first = g.edgeDots[e][0]
                    val row = first / (n + 1)
                    val column = first % (n + 1)
                    val width = if (horizontal) along else across
                    val height = if (horizontal) across else along
                    val centerX = BoardPad + cell * column + if (horizontal) cell / 2f else 0.dp
                    val centerY = BoardPad + cell * row + if (horizontal) 0.dp else cell / 2f
                    val mark = state.marks[e]
                    val markText = when (mark) {
                        FenceSolver.LINE -> lineText
                        FenceSolver.CROSS -> crossText
                        else -> emptyText
                    }
                    val edgeText = if (horizontal) {
                        stringResource(Res.string.a11y_fences_edge_h, row + 1, column + 1, column + 2)
                    } else {
                        stringResource(Res.string.a11y_fences_edge_v, column + 1, row + 1, row + 2)
                    }
                    Box(Modifier.offset(centerX - width / 2f, centerY - height / 2f)) {
                        GuideMark(isGuided(e) && !won, Radius.tile, evidence = isGuideEvidence(e) && !won) {
                            Box(
                                Modifier
                                    .size(width, height)
                                    .testTag("fenceEdge")
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = edgeText
                                        stateDescription = markText
                                        fenceMark = mark
                                        role = Role.Button
                                    }
                                    .then(
                                        if (!won) {
                                            Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button) {
                                                when (onTap(e)) {
                                                    FenceResult.Line -> haptics.perform(HapticKind.Medium)
                                                    FenceResult.Cross, FenceResult.Cleared -> haptics.perform(HapticKind.Light)
                                                    FenceResult.Solved, FenceResult.Ignored -> Unit
                                                }
                                            }
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                FencesResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.fences_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_fences_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.marks.any { it != FenceSolver.UNKNOWN },
                        tag = "fenceRestart",
                    ) { drawRestart(ink) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun FencesResultCard(page: FenceSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().testTag("fenceResultCard").resultSurface(colors.bgSurface, GameKind.Fences.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.fences_won),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.fences_result, formatDuration(page.durationMs)),
                stats = listOf(
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.Fences.world),
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

/** A small picture for the thumbnails: a 3 by 3 board with a loop round four of its squares and the numbers that say it. */
@Composable
internal fun FencesThumbArt(side: Dp) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.Fences.world
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // The loop goes (0,0) (2,0) (2,3) (1,3) (1,1) (0,1) and back, in dot columns and rows.
    val loop = listOf(0 to 0, 2 to 0, 2 to 3, 1 to 3, 1 to 1, 0 to 1, 0 to 0)
    val clues = mapOf(0 to 3, 1 to 2, 4 to 2, 7 to 3, 3 to 1, 8 to 1)
    Canvas(Modifier.size(side * 0.9f)) {
        val pad = size.width * 0.1f
        val step = (size.width - 2 * pad) / 3f
        fun at(x: Int, y: Int) = Offset(pad + x * step, pad + y * step)
        for (y in 0..3) for (x in 0..3) drawCircle(colors.textSecondary, size.width * 0.022f, at(x, y))
        for (i in 0 until loop.size - 1) {
            drawLine(world, at(loop[i].first, loop[i].second), at(loop[i + 1].first, loop[i + 1].second), size.width * 0.045f, StrokeCap.Round)
        }
        for ((c, clue) in clues) {
            val layout = measurer.measure(clue.toString(), TextStyle(fontSize = with(density) { (step * 0.42f).toSp() }, fontWeight = FontWeight.Bold, color = colors.textPrimary))
            drawText(layout, topLeft = Offset(pad + (c % 3) * step + (step - layout.size.width) / 2f, pad + (c / 3) * step + (step - layout.size.height) / 2f))
        }
    }
}
