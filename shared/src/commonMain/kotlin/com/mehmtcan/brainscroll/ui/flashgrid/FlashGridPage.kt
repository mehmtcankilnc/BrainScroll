package com.mehmtcan.brainscroll.ui.flashgrid

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.a11y_flashgrid_board
import brainscroll.shared.generated.resources.a11y_flashgrid_found
import brainscroll.shared.generated.resources.a11y_flashgrid_lit
import brainscroll.shared.generated.resources.a11y_flashgrid_restart
import brainscroll.shared.generated.resources.a11y_flashgrid_start
import brainscroll.shared.generated.resources.flashgrid_ready
import brainscroll.shared.generated.resources.flashgrid_resume
import brainscroll.shared.generated.resources.flashgrid_start
import brainscroll.shared.generated.resources.a11y_flashgrid_tile
import brainscroll.shared.generated.resources.flashgrid_go
import brainscroll.shared.generated.resources.flashgrid_lost
import brainscroll.shared.generated.resources.flashgrid_next
import brainscroll.shared.generated.resources.flashgrid_result
import brainscroll.shared.generated.resources.flashgrid_round
import brainscroll.shared.generated.resources.flashgrid_watch
import brainscroll.shared.generated.resources.flashgrid_won
import brainscroll.shared.generated.resources.result_stat_rounds
import brainscroll.shared.generated.resources.result_stat_tiles
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.flashgrid.FlashResult
import com.mehmtcan.brainscroll.game.flashgrid.FlashSnapshot
import com.mehmtcan.brainscroll.game.flashgrid.FlashStatus
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.ResultStatData
import com.mehmtcan.brainscroll.ui.components.ResultStats
import com.mehmtcan.brainscroll.ui.components.RoundIconButton
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import com.mehmtcan.brainscroll.ui.feed.LocalGuide
import com.mehmtcan.brainscroll.ui.feed.swipeHintText
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.OutcomeHaptics
import com.mehmtcan.brainscroll.ui.motion.bounceIn
import com.mehmtcan.brainscroll.ui.motion.rememberJustFinished
import com.mehmtcan.brainscroll.ui.motion.rememberSettledStatus
import com.mehmtcan.brainscroll.ui.sound.LocalSoundEffects
import com.mehmtcan.brainscroll.ui.sound.NONOGRAM_FILL_VOLUME
import com.mehmtcan.brainscroll.ui.sound.Sfx
import com.mehmtcan.brainscroll.ui.sound.withoutSound
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.theme.resultSurface
import com.mehmtcan.brainscroll.ui.theme.world
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** At least this high; a result card that needs more (big text) makes it higher and the grid smaller. */
private val BottomAreaHeight = 152.dp
private val MaxBoard = 360.dp
private val TileGap = 6.dp

/** The tiles are lit for this long: a moment, and a little more for every tile. */
internal fun flashShowMillis(tiles: Int): Long = 900L + 350L * tiles

/** How long the tiles that were just found all stay green before the next round is lit. */
private const val BETWEEN_MILLIS = 700L

private enum class Phase { Ready, Between, Show, Guess }

/** What the grid shows, for tests: the tiles of this round, the tiles found, the rounds cleared, and what it is doing (wait, between, show, guess). */
internal val FlashPatternKey = SemanticsPropertyKey<List<Int>>("FlashPattern")
private var SemanticsPropertyReceiver.patternList by FlashPatternKey
internal val FlashFoundKey = SemanticsPropertyKey<List<Int>>("FlashFound")
private var SemanticsPropertyReceiver.foundList by FlashFoundKey
internal val FlashRoundsKey = SemanticsPropertyKey<Int>("FlashRounds")
private var SemanticsPropertyReceiver.roundsCount by FlashRoundsKey
internal val FlashPhaseKey = SemanticsPropertyKey<String>("FlashPhase")
private var SemanticsPropertyReceiver.phaseName by FlashPhaseKey

/**
 * One full-screen page of the feed: a grid whose tiles light up for a moment, and then have to be touched from memory. There is no
 * button between the rounds: a cleared round shows its tiles green for a moment and the next one lights up by itself. The result takes
 * the place of the message under the grid when the run is over.
 */
@Composable
fun FlashGridPage(
    page: FlashSnapshot,
    onTap: (Int) -> FlashResult,
    onStart: () -> Boolean,
    onRestart: () -> Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val haptics = LocalHaptics.current
    val quiet = haptics.withoutSound()
    val sounds = LocalSoundEffects.current
    val state = page.state
    val puzzle = state.puzzle
    val n = puzzle.size
    val over = state.status != FlashStatus.Playing

    // The result and the buzz wait a moment so the last tile (or the wrong one) can be seen.
    val status = rememberSettledStatus(
        when (state.status) {
            FlashStatus.Playing -> WordleStatus.Playing
            FlashStatus.Won -> WordleStatus.Won
            FlashStatus.Lost -> WordleStatus.Lost
        },
        450L,
    )
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    // What the grid is doing. Until start is pressed nothing is lit. The tiles are lit when the run is started and when a round is cleared
    // (a cleared round stays green a moment first), and only if the page is on the screen: a page that comes back to the screen, or
    // that was left in the middle of a round, goes on with the tiles hidden (the run can be started over to see new ones).
    var phase by remember(page.id) { mutableStateOf(if (state.started) Phase.Guess else Phase.Ready) }
    var handled by remember(page.id) { mutableIntStateOf(state.showToken) }
    var seen by remember(page.id) { mutableIntStateOf(state.roundsCleared) }
    // True while the tiles of this round have not been seen on this screen (a run that came back from storage, or was due to light while the page was away).
    var hidden by remember(page.id) { mutableStateOf(state.started) }
    LaunchedEffect(page.onShow, state.showToken, state.status, state.started) {
        when {
            over -> phase = Phase.Guess
            !state.started -> {
                hidden = false
                phase = Phase.Ready
            }
            state.showToken == handled -> phase = Phase.Guess
            !page.onShow -> {
                // The tiles were due to light while nobody looked: they are not shown later.
                handled = state.showToken
                seen = state.roundsCleared
                hidden = true
                phase = Phase.Guess
            }
            else -> {
                handled = state.showToken
                if (state.roundsCleared > seen) {
                    seen = state.roundsCleared
                    phase = Phase.Between
                    delay(BETWEEN_MILLIS)
                }
                seen = state.roundsCleared
                hidden = false
                phase = Phase.Show
                delay(flashShowMillis(state.pattern.size))
                phase = Phase.Guess
            }
        }
    }

    val guide = LocalGuide.current?.takeIf { it.reason == GuideReason.FlashPattern }
    val pulse by rememberInfiniteTransition(label = "flashGuide").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    val lit = GameKind.FlashGrid.world
    val previous = if (state.roundsCleared > 0 && !over) puzzle.pattern(state.roundsCleared - 1) else emptyList()

    fun tap(tile: Int) {
        if (phase != Phase.Guess || over) return
        val before = state.found.size
        when (onTap(tile)) {
            FlashResult.Found -> {
                quiet.perform(HapticKind.Light)
                sounds.play(Sfx.nonogramFill(before.coerceIn(0, 9)), NONOGRAM_FILL_VOLUME)
            }
            FlashResult.RoundCleared -> {
                quiet.perform(HapticKind.Medium)
                sounds.play(Sfx.nonogramFill((before + 2).coerceIn(0, 9)), NONOGRAM_FILL_VOLUME)
            }
            FlashResult.Missed -> haptics.perform(HapticKind.Warning)
            FlashResult.Perfect, FlashResult.Ignored -> Unit
        }
    }

    val watch = stringResource(Res.string.flashgrid_watch)
    val go = stringResource(Res.string.flashgrid_go)
    val next = stringResource(Res.string.flashgrid_next, puzzle.tilesIn(minOf(state.roundsCleared, puzzle.rounds - 1)))
    val ready = stringResource(Res.string.flashgrid_ready)
    val resume = stringResource(Res.string.flashgrid_resume)
    // A run that was left in the middle comes back with the tiles hidden: it says so, and the restart button is there.
    val message = when (phase) {
        Phase.Ready -> ready
        Phase.Between -> next
        Phase.Show -> watch
        Phase.Guess -> if (hidden) resume else go
    }
    // A screen reader cannot see the tiles light up: while they are lit it is told which ones they are.
    val litTiles = state.pattern.map { tileLabel(it, n) }.joinToString(", ")
    val spokenMessage = if (phase == Phase.Show) stringResource(Res.string.a11y_flashgrid_lit, litTiles) else message
    val boardDescription = stringResource(Res.string.a11y_flashgrid_board, n, n)
    val foundText = stringResource(Res.string.a11y_flashgrid_found)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Chip(
            text = stringResource(Res.string.flashgrid_round, minOf(state.roundsCleared + 1, puzzle.rounds), state.pattern.size),
            textColor = colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val side: Dp = minOf(MaxBoard, maxWidth, maxHeight)
            val tile = (side - TileGap * (n - 1)) / n
            Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .size(side)
                    .testTag("flashBoard")
                    .semantics {
                        contentDescription = boardDescription
                        patternList = state.pattern
                        foundList = state.found.toList()
                        roundsCount = state.roundsCleared
                        phaseName = phase.name
                    },
                verticalArrangement = Arrangement.spacedBy(TileGap),
            ) {
                for (row in 0 until n) {
                    Row(horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                        for (col in 0 until n) {
                            val index = row * n + col
                            val inPattern = index in state.pattern
                            val isFound = index in state.found
                            val shape = RoundedCornerShape(10.dp)
                            val fill: Color = when {
                                over -> when {
                                    index == state.missed -> colors.error
                                    inPattern && (isFound || state.status == FlashStatus.Won) -> colors.correctFill
                                    else -> colors.bgRaised
                                }
                                phase == Phase.Between -> if (index in previous) colors.correctFill else colors.bgRaised
                                phase == Phase.Show -> if (inPattern) lit else colors.bgRaised
                                else -> if (isFound) colors.correctFill else colors.bgRaised
                            }
                            // The tiles that were lit and not found when the run ended are marked, so the player sees what was missed.
                            val missedOutline = state.status == FlashStatus.Lost && inPattern && !isFound
                            val guided = guide != null && !over && index in guide.cells
                            val label = tileLabel(index, n) + (if (isFound) ", $foundText" else "")
                            var modifierTile = Modifier.size(tile).background(fill, shape)
                            if (missedOutline) modifierTile = modifierTile.border(BorderStroke(3.dp, lit), shape)
                            if (guided) modifierTile = modifierTile.border(BorderStroke(3.dp, colors.pendingFill.copy(alpha = 0.35f + 0.6f * pulse)), shape)
                            Box(
                                modifier = modifierTile
                                    .testTag("flashTile$index")
                                    .clickable(enabled = phase == Phase.Guess && !over && !isFound, role = Role.Button) { tap(index) }
                                    .semantics { contentDescription = label },
                            )
                        }
                    }
                }
            }
            if (phase == Phase.Ready && !over) {
                val startLabel = stringResource(Res.string.flashgrid_start)
                val startDescription = stringResource(Res.string.a11y_flashgrid_start)
                Box(
                    modifier = Modifier
                        .testTag("flashStart")
                        .background(lit, RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button) {
                            if (onStart()) haptics.perform(HapticKind.Medium)
                        }
                        .padding(horizontal = 36.dp, vertical = 16.dp)
                        .semantics { contentDescription = startDescription },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(startLabel, style = type.title.copy(color = Color(0xFF1B1500), textAlign = TextAlign.Center))
                }
            }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status != WordleStatus.Playing) {
                FlashGridResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else if (!over) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = message,
                        modifier = Modifier
                            .padding(horizontal = Spacing.md)
                            .testTag("flashMessage")
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = spokenMessage
                            },
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_flashgrid_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.started,
                        tag = "flashRestart",
                    ) { drawRestart(ink) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun FlashGridResultCard(page: FlashSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val won = page.state.status == FlashStatus.Won
    val rounds = page.state.roundsCleared

    Box(modifier = modifier.fillMaxWidth().testTag("flashResultCard").resultSurface(colors.bgSurface, GameKind.FlashGrid.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(if (won) Res.string.flashgrid_won else Res.string.flashgrid_lost),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = if (won) colors.correctFill else colors.error, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.flashgrid_result, rounds),
                stats = listOf(
                    ResultStatData(rounds.toString(), stringResource(Res.string.result_stat_rounds), GameKind.FlashGrid.world),
                    // The most tiles the player held in one round: the first round's tiles plus one for every round cleared, less one.
                    ResultStatData((if (rounds == 0) 0 else page.state.puzzle.tilesIn(rounds - 1)).toString(), stringResource(Res.string.result_stat_tiles), GameKind.FlashGrid.world),
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

/** What a screen reader says for a tile: its row and column. */
@Composable
private fun tileLabel(tile: Int, n: Int): String = stringResource(Res.string.a11y_flashgrid_tile, tile / n + 1, tile % n + 1)

/** A small picture for the thumbnails: a 3 by 3 grid with a few tiles lit. */
@Composable
internal fun FlashGridThumbArt(side: Dp) {
    val colors = BrainScrollTheme.colors
    val lit = GameKind.FlashGrid.world
    val on = setOf(0, 4, 5, 6)
    val gap = side * 0.04f
    val tile = (side * 0.84f - gap * 2) / 3f
    Column(Modifier.size(side * 0.84f), verticalArrangement = Arrangement.spacedBy(gap)) {
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (col in 0 until 3) {
                    Box(Modifier.size(tile).background(if (row * 3 + col in on) lit else colors.bgRaised, RoundedCornerShape(tile * 0.22f)))
                }
            }
        }
    }
}
