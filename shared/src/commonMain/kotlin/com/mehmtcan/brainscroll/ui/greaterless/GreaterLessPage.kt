package com.mehmtcan.brainscroll.ui.greaterless

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_greaterless_board
import brainscroll.shared.generated.resources.a11y_greaterless_broken
import brainscroll.shared.generated.resources.a11y_greaterless_cell
import brainscroll.shared.generated.resources.a11y_greaterless_empty
import brainscroll.shared.generated.resources.a11y_greaterless_given
import brainscroll.shared.generated.resources.a11y_greaterless_key
import brainscroll.shared.generated.resources.a11y_greaterless_selected
import brainscroll.shared.generated.resources.a11y_greaterless_less_below
import brainscroll.shared.generated.resources.a11y_greaterless_less_right
import brainscroll.shared.generated.resources.a11y_greaterless_more_below
import brainscroll.shared.generated.resources.a11y_greaterless_more_right
import brainscroll.shared.generated.resources.a11y_greaterless_number
import brainscroll.shared.generated.resources.a11y_greaterless_restart
import brainscroll.shared.generated.resources.greaterless_filled
import brainscroll.shared.generated.resources.greaterless_hint
import brainscroll.shared.generated.resources.greaterless_result
import brainscroll.shared.generated.resources.greaterless_won
import brainscroll.shared.generated.resources.result_stat_time
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.greaterless.GlPuzzle
import com.mehmtcan.brainscroll.game.greaterless.GlResult
import com.mehmtcan.brainscroll.game.greaterless.GlRules
import com.mehmtcan.brainscroll.game.greaterless.GlSnapshot
import com.mehmtcan.brainscroll.game.greaterless.GlStatus
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
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.Motion
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.LocalHighContrast
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import org.jetbrains.compose.resources.stringResource

/** At least this high, the same as the other games, so the board does not jump between them. */
private val BottomAreaHeight = 152.dp
private val MaxCell = 60.dp
private val KeyHeight = 46.dp
private val KeyGap = 6.dp

/** The place between two cells holds a sign: half a cell (the cells of a 6 by 6 board still fit a small phone). */
private const val GapShare = 0.5f

/** What a cell holds, for tests: 0 empty, else its number. */
internal val GlValueKey = SemanticsPropertyKey<Int>("GlValue")
private var SemanticsPropertyReceiver.glValue by GlValueKey

/** Whether a cell is one of the given ones, for tests. */
internal val GlGivenKey = SemanticsPropertyKey<Boolean>("GlGiven")
private var SemanticsPropertyReceiver.glGiven by GlGivenKey

/** Whether a cell breaks a rule (it is red), for tests. */
internal val GlBrokenKey = SemanticsPropertyKey<Boolean>("GlBroken")
private var SemanticsPropertyReceiver.glBroken by GlBrokenKey

/** The number a key puts, for tests. */
internal val GlKeyValueKey = SemanticsPropertyKey<Int>("GlKeyValue")
private var SemanticsPropertyReceiver.glKeyValue by GlKeyValueKey

/** The board as it is saved (see [GlPuzzle.encode]), for tests: the board is random, so a test reads it the way a screen reader would. */
internal val GlPuzzleKey = SemanticsPropertyKey<String>("GlPuzzle")
private var SemanticsPropertyReceiver.glPuzzle by GlPuzzleKey

/**
 * One full-screen page of the feed: how many cells are filled, the board with its signs between the cells, and under it the number keys
 * (1 to the size) and the restart button. The player taps a cell to select it and then a key to put that number in it; the same key
 * again empties the cell. The result takes the place of the hint and the button when the board is solved.
 * A cell that breaks a rule (a number twice in its line, a sign that is not true) is red, and so is the sign.
 */
@Composable
fun GreaterLessPage(
    page: GlSnapshot,
    onPut: (Int, Int) -> GlResult,
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
    val won = state.status == GlStatus.Won

    // The result and the buzz wait a moment so the last number can be seen.
    val status = rememberSettledStatus(if (won) WordleStatus.Won else WordleStatus.Playing, 350L)
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    val broken = state.conflicts
    val grid = state.cells.toIntArray()
    // The cell the keys write to (-1: none). A new board starts with none, and so does a board that is won.
    var selected by remember(page.id) { mutableIntStateOf(-1) }
    if (won && selected >= 0) selected = -1

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Chip(text = stringResource(Res.string.greaterless_filled, state.filled, n * n), textColor = if (won) colors.correctFill else colors.textSecondary)
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val across = n + (n - 1) * GapShare
            val cellSize: Dp = minOf(MaxCell, maxWidth / across, maxHeight / across)
            val gap = cellSize * GapShare
            val description = stringResource(Res.string.a11y_greaterless_board, n, n, state.filled, n * n)
            val saved = puzzle.encode()
            Column(
                modifier = Modifier.testTag("glBoard").semantics {
                    contentDescription = description
                    glPuzzle = saved
                },
            ) {
                for (row in 0 until n) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (column in 0 until n) {
                            val cell = row * n + column
                            GuideMark(isGuided(cell) && !won, Radius.tile, evidence = isGuideEvidence(cell) && !won) {
                                GlCell(
                                    puzzle = puzzle,
                                    cell = cell,
                                    value = state.cells[cell],
                                    broken = cell in broken,
                                    playing = !won,
                                    selected = cell == selected,
                                    size = cellSize,
                                    onClick = {
                                        selected = if (selected == cell) -1 else cell
                                        haptics.perform(HapticKind.Light)
                                    },
                                )
                            }
                            if (column < n - 1) {
                                SignSpot(puzzle.right[cell], horizontal = true, wrong = GlRules.breaks(puzzle.right[cell], grid[cell], grid[cell + 1]), width = gap, height = cellSize)
                            }
                        }
                    }
                    if (row < n - 1) {
                        Row {
                            for (column in 0 until n) {
                                val cell = row * n + column
                                SignSpot(puzzle.down[cell], horizontal = false, wrong = GlRules.breaks(puzzle.down[cell], grid[cell], grid[cell + n]), width = cellSize, height = gap)
                                if (column < n - 1) Spacer(Modifier.size(gap))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status == WordleStatus.Won) {
                GreaterLessResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = stringResource(Res.string.greaterless_hint),
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    Row(horizontalArrangement = Arrangement.spacedBy(KeyGap), verticalAlignment = Alignment.CenterVertically) {
                        for (value in 1..n) {
                            NumberKey(value, active = selected >= 0, onClick = {
                                // The same number again empties the cell.
                                when (onPut(selected, if (state.cells[selected] == value) 0 else value)) {
                                    GlResult.Placed -> haptics.perform(HapticKind.Medium)
                                    GlResult.Cleared -> haptics.perform(HapticKind.Light)
                                    GlResult.Solved, GlResult.Ignored -> Unit
                                }
                            }, modifier = Modifier.weight(1f))
                        }
                        RoundIconButton(
                            description = stringResource(Res.string.a11y_greaterless_restart),
                            onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                            enabled = (0 until n * n).any { !puzzle.isGiven(it) && state.cells[it] > 0 },
                            tag = "glRestart",
                        ) { drawRestart(ink) }
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** One number key under the board. It does nothing while no cell is selected. */
@Composable
private fun NumberKey(value: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.GreaterLess.world
    val density = LocalDensity.current
    val description = stringResource(Res.string.a11y_greaterless_key, value)
    Box(
        modifier = modifier
            .height(KeyHeight)
            .testTag("glKey")
            .alpha(if (active) 1f else 0.45f)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                glKeyValue = value
                role = Role.Button
            }
            .background(colors.bgSurface, Radius.tile)
            .border(1.5.dp, if (active) world else colors.borderStrong.copy(alpha = 0.5f), Radius.tile)
            .clickable(enabled = active, interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = value.toString(),
            style = TextStyle(fontSize = with(density) { (KeyHeight.toPx() * 0.45f).toSp() }, fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = TextAlign.Center),
        )
    }
}

/** The place between two cells: the sign of the board if there is one (the cells say it too, for a screen reader), else nothing. */
@Composable
private fun SignSpot(sign: Int, horizontal: Boolean, wrong: Boolean, width: Dp, height: Dp) {
    val colors = BrainScrollTheme.colors
    Box(modifier = Modifier.width(width).height(height).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        if (sign > 0) SignMark(sign, horizontal, if (wrong) colors.error else colors.textSecondary, Modifier.size(minOf(width, height) * 0.8f))
    }
}

/**
 * The sign of a place between two cells, drawn as a chevron whose point shows the smaller cell: `<` or `>` between two cells side by
 * side, an arrowhead up or down between two cells one above the other. [sign] is [GlPuzzle.LESS] when the first cell (the left or the
 * upper one) is the smaller.
 */
@Composable
internal fun SignMark(sign: Int, horizontal: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val a = size.minDimension * 0.42f
        val back = a * 0.6f
        // The point goes to the side of the smaller cell: left or up for LESS, right or down for MORE.
        val toward = if (sign == GlPuzzle.LESS) -1f else 1f
        val path = Path().apply {
            if (horizontal) {
                moveTo(c.x - toward * back, c.y - a)
                lineTo(c.x + toward * back, c.y)
                lineTo(c.x - toward * back, c.y + a)
            } else {
                moveTo(c.x - a, c.y - toward * back)
                lineTo(c.x, c.y + toward * back)
                lineTo(c.x + a, c.y - toward * back)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = (size.minDimension * 0.16f).coerceAtLeast(1.5f), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * One cell. A given cell is a raised tile with a solid border; a number the player put is a tile filled in the colour of the game,
 * so the two are told apart by the fill. A cell that breaks a rule is red.
 */
@Composable
private fun GlCell(
    puzzle: GlPuzzle,
    cell: Int,
    value: Int,
    broken: Boolean,
    playing: Boolean,
    selected: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.GreaterLess.world
    val extraBorder = if (LocalHighContrast.current) 1f else 0f
    val shape = Radius.tile
    val duration = if (LocalReduceMotion.current) 0 else Motion.BASE
    val n = puzzle.size
    val given = puzzle.isGiven(cell)
    val put = value > 0 && !given && !broken
    val fill by animateColorAsState(
        when {
            put -> world
            given -> colors.bgRaised
            selected -> world.copy(alpha = 0.22f).compositeOver(colors.bgSurface)
            else -> colors.bgSurface
        },
        tween(duration, easing = Motion.Standard),
    )
    val digitColor = when {
        broken -> colors.error
        put -> colors.bgPage
        else -> colors.textPrimary
    }
    val border = when {
        selected -> colors.textPrimary
        broken -> colors.error
        given -> colors.borderStrong
        put -> world
        else -> colors.borderStrong.copy(alpha = 0.5f)
    }
    val row = cell / n + 1
    val column = cell % n + 1
    val stateText = if (value <= 0) stringResource(Res.string.a11y_greaterless_empty) else stringResource(Res.string.a11y_greaterless_number, value)
    val givenText = stringResource(Res.string.a11y_greaterless_given)
    val brokenText = stringResource(Res.string.a11y_greaterless_broken)
    val selectedText = stringResource(Res.string.a11y_greaterless_selected)
    // What the signs around the cell say about it, for a screen reader.
    val lessRight = stringResource(Res.string.a11y_greaterless_less_right)
    val moreRight = stringResource(Res.string.a11y_greaterless_more_right)
    val lessBelow = stringResource(Res.string.a11y_greaterless_less_below)
    val moreBelow = stringResource(Res.string.a11y_greaterless_more_below)
    val signTexts = listOfNotNull(
        when (puzzle.right[cell]) {
            GlPuzzle.LESS -> lessRight
            GlPuzzle.MORE -> moreRight
            else -> null
        },
        when (puzzle.down[cell]) {
            GlPuzzle.LESS -> lessBelow
            GlPuzzle.MORE -> moreBelow
            else -> null
        },
    )
    val description = stringResource(Res.string.a11y_greaterless_cell, row, column)
    val density = LocalDensity.current
    val digitSize = with(density) { (size.toPx() * 0.5f).toSp() }

    Box(
        modifier = Modifier
            .size(size)
            .testTag("glCell")
            .semantics(mergeDescendants = true) {
                contentDescription = description
                stateDescription = listOfNotNull(stateText, givenText.takeIf { given }, selectedText.takeIf { selected }, brokenText.takeIf { broken }).plus(signTexts).joinToString(", ")
                glValue = value
                glGiven = given
                glBroken = broken
                role = Role.Button
            }
            .background(fill, shape)
            .border((1.5f + extraBorder).dp, border, shape)
            .then(
                if (playing && !given) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (value > 0) {
            BasicText(
                text = value.toString(),
                style = TextStyle(fontSize = digitSize, fontWeight = FontWeight.Bold, color = digitColor, textAlign = TextAlign.Center),
            )
        }
    }
}

@Composable
private fun GreaterLessResultCard(page: GlSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Box(modifier = modifier.fillMaxWidth().testTag("glResultCard").resultSurface(colors.bgSurface, GameKind.GreaterLess.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(Res.string.greaterless_won),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = colors.correctFill, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.greaterless_result, formatDuration(page.durationMs)),
                stats = listOf(
                    ResultStatData(formatDuration(page.durationMs), stringResource(Res.string.result_stat_time), GameKind.GreaterLess.world),
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

/** A small picture for the thumbnails: a 3 by 3 board with a few numbers and signs. */
@Composable
internal fun GreaterLessThumbArt(side: Dp) {
    val colors = BrainScrollTheme.colors
    val world = GameKind.GreaterLess.world
    val density = LocalDensity.current
    val numbers = listOf(
        1, 0, 3,
        0, 3, 0,
        3, 1, 0,
    )
    val given = setOf(0, 2, 6)
    val cell = side * 0.9f / (3 + 2 * GapShare)
    val gap = cell * GapShare
    Column(Modifier.size(side * 0.9f)) {
        for (row in 0 until 3) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (column in 0 until 3) {
                    val index = row * 3 + column
                    val isGiven = index in given
                    val digit = numbers[index]
                    Box(
                        Modifier.size(cell)
                            .background(if (isGiven) colors.bgRaised else if (digit > 0) world else colors.bgSurface, Radius.tile)
                            .border(1.dp, if (isGiven) colors.borderStrong else if (digit > 0) world else colors.borderStrong.copy(alpha = 0.5f), Radius.tile),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (digit > 0) {
                            BasicText(
                                text = digit.toString(),
                                style = TextStyle(fontSize = with(density) { (cell.toPx() * 0.5f).toSp() }, fontWeight = FontWeight.Bold, color = if (isGiven) colors.textPrimary else colors.bgPage),
                            )
                        }
                    }
                    if (column < 2) {
                        Box(Modifier.width(gap).height(cell), contentAlignment = Alignment.Center) {
                            // The first row says 1 < _ < 3, the last one 3 > 1.
                            if (row == 0) SignMark(GlPuzzle.LESS, true, world, Modifier.size(gap * 0.8f))
                            if (row == 2 && column == 0) SignMark(GlPuzzle.MORE, true, world, Modifier.size(gap * 0.8f))
                        }
                    }
                }
            }
            if (row < 2) {
                Row {
                    for (column in 0 until 3) {
                        Box(Modifier.width(cell).height(gap), contentAlignment = Alignment.Center) {
                            if (row == 0 && column == 1) SignMark(GlPuzzle.LESS, false, world, Modifier.size(gap * 0.8f))
                        }
                        if (column < 2) Spacer(Modifier.size(gap))
                    }
                }
            }
        }
    }
}
