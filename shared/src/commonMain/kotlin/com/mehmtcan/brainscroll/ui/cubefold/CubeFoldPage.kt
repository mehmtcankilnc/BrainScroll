package com.mehmtcan.brainscroll.ui.cubefold

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_cubefold_net
import brainscroll.shared.generated.resources.a11y_cubefold_option
import brainscroll.shared.generated.resources.a11y_cubefold_right
import brainscroll.shared.generated.resources.a11y_cubefold_wrong
import brainscroll.shared.generated.resources.cubefold_hint
import brainscroll.shared.generated.resources.cubefold_lost
import brainscroll.shared.generated.resources.cubefold_result
import brainscroll.shared.generated.resources.cubefold_right
import brainscroll.shared.generated.resources.cubefold_round
import brainscroll.shared.generated.resources.cubefold_why_mirror
import brainscroll.shared.generated.resources.cubefold_why_opposite
import brainscroll.shared.generated.resources.cubefold_won
import brainscroll.shared.generated.resources.result_stat_rounds
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.cubefold.CubeFoldSnapshot
import com.mehmtcan.brainscroll.game.cubefold.CubeResult
import com.mehmtcan.brainscroll.game.cubefold.CubeReveal
import com.mehmtcan.brainscroll.game.cubefold.CubeStatus
import com.mehmtcan.brainscroll.game.cubefold.CubeWhy
import com.mehmtcan.brainscroll.game.guide.CubeGuide
import com.mehmtcan.brainscroll.game.memory.CardSymbol
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import com.mehmtcan.brainscroll.ui.components.ResultStats
import com.mehmtcan.brainscroll.ui.feed.GuideMark
import com.mehmtcan.brainscroll.ui.feed.isGuideEvidence
import com.mehmtcan.brainscroll.ui.feed.isGuided
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.memory.drawSymbol
import com.mehmtcan.brainscroll.ui.memory.symbolColor
import com.mehmtcan.brainscroll.ui.memory.symbolName
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.theme.BrainScrollColors
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** At least this high, the same as the other games, so the page does not jump between them. */
private val BottomAreaHeight = 112.dp
private val Gap = 8.dp
private val MaxNetCell = 56.dp
private val MaxOption = 150.dp

/** How long the answer is shown after a pick, before the next question. */
private const val REVEAL_MILLIS = 1_800L

/** Whether a cube option is the right one. Only tests read it: a screen reader is told after the pick, not before. */
internal val CubeCorrectKey = SemanticsPropertyKey<Boolean>("CubeCorrect")
private var SemanticsPropertyReceiver.cubeCorrect by CubeCorrectKey

/** The three symbols of a cube option (top, left, right), for tests. */
internal val CubeViewKey = SemanticsPropertyKey<List<Int>>("CubeView")
private var SemanticsPropertyReceiver.cubeView by CubeViewKey

private fun symbolOf(index: Int): CardSymbol = CardSymbol.entries[index % 6]

/**
 * One full-screen page of the feed: which question it is, the net (six squares with a symbol each), and the cubes to choose from, each seen
 * from a corner (three faces). The player taps the cube that the net folds to. A pick shows the right cube green (and a wrong pick red)
 * and says why a wrong cube is wrong, for a moment; a right pick then brings the next question, a wrong one ends the run.
 */
@Composable
fun CubeFoldPage(
    page: CubeFoldSnapshot,
    onPick: (Int) -> CubeResult,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val state = page.state
    val puzzle = state.puzzle
    val playing = state.status == CubeStatus.Playing

    // The result and the buzz wait for the answer to be seen.
    val status = rememberSettledStatus(
        when (state.status) {
            CubeStatus.Playing -> WordleStatus.Playing
            CubeStatus.Won -> WordleStatus.Won
            CubeStatus.Lost -> WordleStatus.Lost
        },
        REVEAL_MILLIS,
    )
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    // The pick that was just made stays on the screen with its answer for a moment (for a run that is over it stays).
    var reveal by remember(page.id) { mutableStateOf<CubeReveal?>(null) }
    LaunchedEffect(state.last) {
        val last = state.last ?: return@LaunchedEffect
        reveal = last
        if (state.status == CubeStatus.Playing) {
            delay(REVEAL_MILLIS)
            reveal = null
        }
    }

    val shown = reveal?.question ?: state.question
    val roundShown = (if (reveal?.correct == true) state.cleared - 1 else minOf(state.cleared, puzzle.rounds - 1)) + 1
    val settled = reveal == null
    val symbolNames = (0 until 6).map { stringResource(symbolName(symbolOf(it))) }
    val netDescription = stringResource(Res.string.a11y_cubefold_net, shown.net.joinToString(", ") { symbolNames[it.symbol] })

    val note = reveal?.let { r ->
        when (shown.whys[r.picked]) {
            CubeWhy.Right -> stringResource(Res.string.cubefold_right)
            CubeWhy.Opposite -> stringResource(Res.string.cubefold_why_opposite)
            CubeWhy.Mirror -> stringResource(Res.string.cubefold_why_mirror)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Chip(
            text = stringResource(Res.string.cubefold_round, roundShown, puzzle.rounds),
            textColor = if (state.status == CubeStatus.Won) colors.correctFill else colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.md))

        Column(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            // The net.
            BoxWithConstraints(modifier = Modifier.weight(0.42f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val rows = shown.net.maxOf { it.row } + 1
                val columns = shown.net.maxOf { it.column } + 1
                val cell: Dp = minOf(MaxNetCell, (maxWidth - Gap * (columns - 1)) / columns, (maxHeight - Gap * (rows - 1)) / rows).coerceAtLeast(0.dp)
                Column(
                    modifier = Modifier.testTag("cubeNet").semantics { contentDescription = netDescription },
                    verticalArrangement = Arrangement.spacedBy(Gap),
                ) {
                    for (row in 0 until rows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                            for (column in 0 until columns) {
                                val index = shown.net.indexOfFirst { it.row == row && it.column == column }
                                if (index < 0) {
                                    Spacer(Modifier.size(cell))
                                } else {
                                    GuideMark(false, Radius.tile, evidence = isGuideEvidence(CubeGuide.NET_BASE + index) && playing && settled) {
                                        NetSquare(shown.net[index].symbol, cell)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.md))
            // The cubes to choose from: one row of three, or two rows of two.
            BoxWithConstraints(modifier = Modifier.weight(0.58f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val count = shown.options.size
                val perRow = if (count <= 3) count else 2
                val rows = (count + perRow - 1) / perRow
                val size: Dp = minOf(MaxOption, (maxWidth - Gap * (perRow - 1)) / perRow, (maxHeight - Gap * (rows - 1)) / rows).coerceAtLeast(0.dp)
                Column(verticalArrangement = Arrangement.spacedBy(Gap), horizontalAlignment = Alignment.CenterHorizontally) {
                    for (row in 0 until rows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Gap)) {
                            for (k in 0 until perRow) {
                                val index = row * perRow + k
                                if (index >= count) continue
                                GuideMark(isGuided(index) && playing && settled, Radius.tile) {
                                    CubeOption(
                                        view = shown.options[index],
                                        number = index + 1,
                                        symbolNames = symbolNames,
                                        correct = index == shown.correct,
                                        right = reveal != null && index == shown.correct,
                                        wrong = reveal != null && !reveal!!.correct && index == reveal!!.picked,
                                        enabled = playing && settled,
                                        size = size,
                                        onClick = {
                                            when (onPick(index)) {
                                                CubeResult.Correct, CubeResult.Perfect -> haptics.perform(HapticKind.Medium)
                                                CubeResult.Wrong -> haptics.perform(HapticKind.Warning)
                                                CubeResult.Ignored -> Unit
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status != WordleStatus.Playing) {
                CubeFoldResultCard(page, note, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                BasicText(
                    text = note ?: stringResource(Res.string.cubefold_hint),
                    modifier = Modifier.padding(horizontal = Spacing.md).semantics { if (note != null) liveRegion = LiveRegionMode.Polite },
                    style = type.body.copy(
                        color = if (reveal == null) colors.textSecondary else if (reveal!!.correct) colors.correctFill else colors.error,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** One square of the net: the symbol of a face on a tile that has the colour of the symbol. */
@Composable
internal fun NetSquare(symbol: Int, size: Dp) {
    val colors = BrainScrollTheme.colors
    val color = symbolColor(symbolOf(symbol), colors)
    val shape = Radius.tile
    val safe = size.coerceAtLeast(0.dp)
    Box(
        modifier = Modifier.size(safe).background(colors.bgRaised, shape).background(color.copy(alpha = 0.22f), shape).border(1.5.dp, color.copy(alpha = 0.6f), shape),
    ) {
        Canvas(Modifier.fillMaxSize().padding(safe * 0.2f)) { drawSymbol(symbolOf(symbol), color) }
    }
}

/** One cube to choose from, seen from a corner. After a pick the right one is green and a wrong pick is red, so the answer is told by colour and by description. */
@Composable
private fun CubeOption(
    view: List<Int>,
    number: Int,
    symbolNames: List<String>,
    correct: Boolean,
    right: Boolean,
    wrong: Boolean,
    enabled: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val shape = Radius.tile
    val border = when {
        right -> colors.correctFill
        wrong -> colors.error
        else -> colors.borderStrong.copy(alpha = 0.28f)
    }
    val fill = when {
        right -> colors.correctFill.copy(alpha = 0.18f)
        wrong -> colors.error.copy(alpha = 0.18f)
        else -> colors.bgRaised
    }
    val description = stringResource(Res.string.a11y_cubefold_option, number, view.joinToString(", ") { symbolNames[it] })
    val state = when {
        right -> stringResource(Res.string.a11y_cubefold_right)
        wrong -> stringResource(Res.string.a11y_cubefold_wrong)
        else -> ""
    }
    Box(
        modifier = Modifier
            .size(size)
            .testTag("cubeOption")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                if (state.isNotEmpty()) stateDescription = state
                cubeCorrect = correct
                cubeView = view
                role = Role.Button
            }
            .background(fill, shape)
            .border((1.5f + extraBorder).dp, border, shape)
            .then(
                if (enabled) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCube(view, colors, Offset(this.size.width / 2f, this.size.height / 2f), this.size.minDimension * 0.44f)
        }
    }
}

/**
 * A cube seen from a corner: the top face, the face at the left and the face at the right, each with its symbol. [radius] is the distance
 * from the middle to every outer corner of the picture.
 */
internal fun DrawScope.drawCube(view: List<Int>, colors: BrainScrollColors, center: Offset, radius: Float, alpha: Float = 1f) {
    val c = 0.866f
    val top = Offset(center.x, center.y - radius)
    val upperLeft = Offset(center.x - c * radius, center.y - radius / 2f)
    val upperRight = Offset(center.x + c * radius, center.y - radius / 2f)
    val lowerLeft = Offset(center.x - c * radius, center.y + radius / 2f)
    val lowerRight = Offset(center.x + c * radius, center.y + radius / 2f)
    val bottom = Offset(center.x, center.y + radius)
    // (the polygon, the middle of the face, how bright the face is: the top catches the light)
    val faces = listOf(
        Triple(listOf(center, upperLeft, top, upperRight), Offset(center.x, center.y - radius / 2f), 0.42f),
        Triple(listOf(center, upperLeft, lowerLeft, bottom), Offset(center.x - c * radius / 2f, center.y + radius / 4f), 0.26f),
        Triple(listOf(center, upperRight, lowerRight, bottom), Offset(center.x + c * radius / 2f, center.y + radius / 4f), 0.34f),
    )
    faces.forEachIndexed { i, (polygon, middle, strength) ->
        val symbol = symbolOf(view[i])
        val color = symbolColor(symbol, colors)
        val path = Path().apply {
            moveTo(polygon[0].x, polygon[0].y)
            for (k in 1 until polygon.size) lineTo(polygon[k].x, polygon[k].y)
            close()
        }
        drawPath(path, colors.bgSurface.copy(alpha = alpha))
        drawPath(path, color.copy(alpha = strength * alpha))
        drawPath(path, colors.borderStrong.copy(alpha = 0.8f * alpha), style = Stroke(width = radius * 0.04f))
        val half = radius * 0.27f
        inset(middle.x - half, middle.y - half, size.width - (middle.x + half), size.height - (middle.y + half)) {
            drawSymbol(symbol, color.copy(alpha = alpha))
        }
    }
}

@Composable
private fun CubeFoldResultCard(page: CubeFoldSnapshot, note: String?, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val won = page.state.status == CubeStatus.Won
    val rounds = page.state.cleared

    Box(modifier = modifier.fillMaxWidth().testTag("cubeResultCard").resultSurface(colors.bgSurface, GameKind.CubeFold.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(if (won) Res.string.cubefold_won else Res.string.cubefold_lost),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = if (won) colors.correctFill else colors.error, textAlign = TextAlign.Center),
            )
            if (note != null && !won) {
                BasicText(text = note, style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center))
            }
            ResultStats(
                summary = stringResource(Res.string.cubefold_result, rounds),
                stats = listOf(
                    ResultStatData(rounds.toString(), stringResource(Res.string.result_stat_rounds), GameKind.CubeFold.world),
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.CubeFold.world),
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

/** A small picture for the thumbnails: a cube seen from a corner, with a circle, a triangle and a star on its faces. */
@Composable
internal fun CubeFoldThumbArt(side: Dp) {
    val colors = BrainScrollTheme.colors
    Canvas(Modifier.size(side * 0.9f)) {
        drawCube(listOf(0, 1, 2), colors, Offset(size.width / 2f, size.height / 2f), size.minDimension * 0.46f)
    }
}
