package com.mehmtcan.brainscroll.ui.colorseq

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import brainscroll.shared.generated.resources.a11y_colorseq_b0
import brainscroll.shared.generated.resources.a11y_colorseq_b1
import brainscroll.shared.generated.resources.a11y_colorseq_b2
import brainscroll.shared.generated.resources.a11y_colorseq_b3
import brainscroll.shared.generated.resources.a11y_colorseq_b4
import brainscroll.shared.generated.resources.a11y_colorseq_b5
import brainscroll.shared.generated.resources.a11y_colorseq_board
import brainscroll.shared.generated.resources.a11y_colorseq_lit
import brainscroll.shared.generated.resources.a11y_colorseq_restart
import brainscroll.shared.generated.resources.a11y_colorseq_start
import brainscroll.shared.generated.resources.colorseq_go
import brainscroll.shared.generated.resources.colorseq_lost
import brainscroll.shared.generated.resources.colorseq_next
import brainscroll.shared.generated.resources.colorseq_ready
import brainscroll.shared.generated.resources.colorseq_result
import brainscroll.shared.generated.resources.colorseq_resume
import brainscroll.shared.generated.resources.colorseq_round
import brainscroll.shared.generated.resources.colorseq_start
import brainscroll.shared.generated.resources.colorseq_watch
import brainscroll.shared.generated.resources.colorseq_won
import brainscroll.shared.generated.resources.result_stat_length
import brainscroll.shared.generated.resources.result_stat_rounds
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.colorseq.ColorSeqResult
import com.mehmtcan.brainscroll.game.colorseq.ColorSeqSnapshot
import com.mehmtcan.brainscroll.game.colorseq.ColorSeqStatus
import com.mehmtcan.brainscroll.game.guide.GuideReason
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
import com.mehmtcan.brainscroll.ui.tubes.drawRestart
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** At least this high; a result card that needs more (big text) makes it higher and the buttons smaller. */
private val BottomAreaHeight = 152.dp
private val MaxButton = 150.dp
private val ButtonGap = 12.dp

/** How long the board waits after the cleared round is shown (and before the first light) so the player can look up. */
private const val LEAD_IN_MILLIS = 600L

private enum class Phase { Ready, Between, Show, Guess }

/** What the board shows, for tests: the lights of this round, the lights repeated so far, the rounds cleared, the button lit now (or -1), what it is doing. */
internal val ColorSeqSequenceKey = SemanticsPropertyKey<List<Int>>("ColorSeqSequence")
private var SemanticsPropertyReceiver.sequenceList by ColorSeqSequenceKey
internal val ColorSeqEnteredKey = SemanticsPropertyKey<Int>("ColorSeqEntered")
private var SemanticsPropertyReceiver.enteredCount by ColorSeqEnteredKey
internal val ColorSeqRoundsKey = SemanticsPropertyKey<Int>("ColorSeqRounds")
private var SemanticsPropertyReceiver.roundsCount by ColorSeqRoundsKey
internal val ColorSeqLitKey = SemanticsPropertyKey<Int>("ColorSeqLit")
private var SemanticsPropertyReceiver.litButton by ColorSeqLitKey
internal val ColorSeqPhaseKey = SemanticsPropertyKey<String>("ColorSeqPhase")
private var SemanticsPropertyReceiver.phaseName by ColorSeqPhaseKey

/** The colour of every button; the shape on it says the same (a colour alone is not enough for everybody). */
internal val SeqColors = listOf(
    Color(0xFFFF6B6B), // red, triangle
    Color(0xFF5AA9FF), // blue, square
    Color(0xFF5ED38A), // green, circle
    Color(0xFFFFD84F), // yellow, star
    Color(0xFFC77DFF), // violet, diamond
    Color(0xFFFF9F43), // orange, hexagon
)

private val SeqInk = Color(0xFF14110B)

/** A note of the pentatonic ladder for button [button] of [buttons]: low to high from the first button to the last. */
private fun noteOf(button: Int, buttons: Int): Int = (button * 9 / (buttons - 1).coerceAtLeast(1)).coerceIn(0, 9)

/**
 * One full-screen page of the feed: coloured buttons light up one after the other (with a note each), and have to be touched in the same
 * order from memory. There is no button between the rounds: a cleared round waits a moment and the next one is played by itself, with one
 * light more. The result takes the place of the message under the buttons when the run is over.
 */
@Composable
fun ColorSeqPage(
    page: ColorSeqSnapshot,
    onTap: (Int) -> ColorSeqResult,
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
    val over = state.status != ColorSeqStatus.Playing

    // The result and the buzz wait a moment so the last button (or the wrong one) can be seen.
    val status = rememberSettledStatus(
        when (state.status) {
            ColorSeqStatus.Playing -> WordleStatus.Playing
            ColorSeqStatus.Won -> WordleStatus.Won
            ColorSeqStatus.Lost -> WordleStatus.Lost
        },
        450L,
    )
    OutcomeHaptics(status, page.id)
    val justFinished = rememberJustFinished(status)

    // What the board is doing. Until start is pressed nothing is played. The lights are played when the run is started and when a round is
    // cleared, and only if the page is on the screen: a page that comes back, or that was left in the middle of a round, goes on with the
    // lights hidden (the hint plays them again, or the run can be started over to see new ones).
    var phase by remember(page.id) { mutableStateOf(if (state.started) Phase.Guess else Phase.Ready) }
    var handled by remember(page.id) { mutableIntStateOf(state.showToken) }
    var seen by remember(page.id) { mutableIntStateOf(state.roundsCleared) }
    var hidden by remember(page.id) { mutableStateOf(state.started) }
    // The button that is lit now: a light of the sequence, or the one the player is touching.
    var lit by remember(page.id) { mutableIntStateOf(-1) }
    var pressStamp by remember(page.id) { mutableIntStateOf(0) }

    suspend fun play(steps: List<Int>) {
        delay(LEAD_IN_MILLIS)
        for (button in steps) {
            lit = button
            sounds.play(Sfx.nonogramFill(noteOf(button, puzzle.buttons)), NONOGRAM_FILL_VOLUME)
            delay(puzzle.stepMs * 7L / 10)
            lit = -1
            delay(puzzle.stepMs * 3L / 10)
        }
    }

    LaunchedEffect(page.onShow, state.showToken, state.status, state.started) {
        when {
            over -> phase = Phase.Guess
            !state.started -> {
                hidden = false
                phase = Phase.Ready
            }
            state.showToken == handled -> phase = Phase.Guess
            !page.onShow -> {
                // The lights were due to play while nobody looked: they are not played later.
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
                    delay(700L)
                }
                seen = state.roundsCleared
                hidden = false
                phase = Phase.Show
                play(state.sequence)
                phase = Phase.Guess
            }
        }
    }

    // The hint plays the lights of the round again.
    val guide = LocalGuide.current?.takeIf { it.reason == GuideReason.SeqReplay }
    LaunchedEffect(guide) {
        if (guide != null && !over && state.started && phase == Phase.Guess) {
            phase = Phase.Show
            hidden = false
            play(state.sequence)
            phase = Phase.Guess
        }
    }
    LaunchedEffect(pressStamp) {
        if (pressStamp > 0) {
            delay(180)
            if (phase != Phase.Show) lit = -1
        }
    }

    val world = GameKind.ColorSeq.world

    fun tap(button: Int) {
        if (phase != Phase.Guess || over) return
        when (onTap(button)) {
            ColorSeqResult.Right -> {
                quiet.perform(HapticKind.Light)
                sounds.play(Sfx.nonogramFill(noteOf(button, puzzle.buttons)), NONOGRAM_FILL_VOLUME)
            }
            ColorSeqResult.RoundCleared -> {
                quiet.perform(HapticKind.Medium)
                sounds.play(Sfx.nonogramFill(noteOf(button, puzzle.buttons)), NONOGRAM_FILL_VOLUME)
            }
            ColorSeqResult.Perfect -> sounds.play(Sfx.nonogramFill(noteOf(button, puzzle.buttons)), NONOGRAM_FILL_VOLUME)
            ColorSeqResult.Missed -> haptics.perform(HapticKind.Warning)
            ColorSeqResult.Ignored -> Unit
        }
        lit = button
        pressStamp++
    }

    val watch = stringResource(Res.string.colorseq_watch)
    val go = stringResource(Res.string.colorseq_go)
    val next = stringResource(Res.string.colorseq_next, puzzle.lengthIn(minOf(state.roundsCleared, puzzle.rounds - 1)))
    val ready = stringResource(Res.string.colorseq_ready)
    val resume = stringResource(Res.string.colorseq_resume)
    val message = when (phase) {
        Phase.Ready -> ready
        Phase.Between -> next
        Phase.Show -> watch
        Phase.Guess -> if (hidden) resume else go
    }
    val names = listOf(
        stringResource(Res.string.a11y_colorseq_b0), stringResource(Res.string.a11y_colorseq_b1), stringResource(Res.string.a11y_colorseq_b2),
        stringResource(Res.string.a11y_colorseq_b3), stringResource(Res.string.a11y_colorseq_b4), stringResource(Res.string.a11y_colorseq_b5),
    )
    // A screen reader cannot see the lights: while they play it is told which buttons they are.
    val spokenMessage = if (phase == Phase.Show) stringResource(Res.string.a11y_colorseq_lit, state.sequence.joinToString(", ") { names[it] }) else message
    val boardDescription = stringResource(Res.string.a11y_colorseq_board, puzzle.buttons)

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.lg))
        Chip(
            text = stringResource(Res.string.colorseq_round, minOf(state.roundsCleared + 1, puzzle.rounds), state.sequence.size),
            textColor = colors.textSecondary,
        )
        Spacer(Modifier.height(Spacing.md))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val columns = if (puzzle.buttons <= 4) 2 else 3
            val rows = (puzzle.buttons + columns - 1) / columns
            val button: Dp = minOf(MaxButton, (maxWidth - ButtonGap * (columns - 1)) / columns, (maxHeight - ButtonGap * (rows - 1)) / rows)
            Box(contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .testTag("colorBoard")
                        .semantics {
                            contentDescription = boardDescription
                            sequenceList = state.sequence
                            enteredCount = state.entered
                            roundsCount = state.roundsCleared
                            litButton = lit
                            phaseName = phase.name
                        },
                    verticalArrangement = Arrangement.spacedBy(ButtonGap),
                ) {
                    for (row in 0 until rows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGap), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            for (column in 0 until columns) {
                                val index = row * columns + column
                                if (index >= puzzle.buttons) continue
                                // When the run is over the button that was due shows, and the wrong one is marked.
                                val ring: Color? = when {
                                    over && index == state.missed -> colors.error
                                    over && index == state.expected -> world
                                    else -> null
                                }
                                SeqButton(
                                    index = index,
                                    lit = if (over) index == state.expected else index == lit,
                                    ring = ring,
                                    size = button,
                                    modifier = Modifier
                                        .testTag("colorButton$index")
                                        .clickable(enabled = phase == Phase.Guess && !over, role = Role.Button) { tap(index) }
                                        .semantics { contentDescription = names[index] },
                                )
                            }
                        }
                    }
                }
                if (phase == Phase.Ready && !over) {
                    val startLabel = stringResource(Res.string.colorseq_start)
                    val startDescription = stringResource(Res.string.a11y_colorseq_start)
                    Box(
                        modifier = Modifier
                            .testTag("colorStart")
                            .background(world, RoundedCornerShape(16.dp))
                            .clickable(role = Role.Button) {
                                if (onStart()) haptics.perform(HapticKind.Medium)
                            }
                            .padding(horizontal = 36.dp, vertical = 16.dp)
                            .semantics { contentDescription = startDescription },
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(startLabel, style = type.title.copy(color = SeqInk, textAlign = TextAlign.Center))
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (status != WordleStatus.Playing) {
                ColorSeqResultCard(page, isFavorite, onToggleFavorite, Modifier.bounceIn(justFinished))
            } else if (!over) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BasicText(
                        text = message,
                        modifier = Modifier
                            .padding(horizontal = Spacing.md)
                            .testTag("colorMessage")
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = spokenMessage
                            },
                        style = type.body.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
                    )
                    val ink = colors.textPrimary
                    RoundIconButton(
                        description = stringResource(Res.string.a11y_colorseq_restart),
                        onClick = { if (onRestart()) haptics.perform(HapticKind.Light) },
                        enabled = state.started,
                        tag = "colorRestart",
                    ) { drawRestart(ink) }
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** One coloured button: dim when it is off, bright with the shape in dark ink when it is lit; [ring] marks it (the wrong button, or the one that was due). */
@Composable
internal fun SeqButton(index: Int, lit: Boolean, ring: Color?, size: Dp, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val color = SeqColors[index % SeqColors.size]
    val shape = RoundedCornerShape(size * 0.2f)
    var look = Modifier.size(size).background(colors.bgRaised, shape).background(if (lit) color else color.copy(alpha = 0.22f), shape)
    if (ring != null) look = look.border(BorderStroke(3.dp, ring), shape)
    Box(modifier = modifier.then(look)) {
        Canvas(Modifier.fillMaxSize()) {
            drawSeqShape(index, Offset(this.size.width / 2f, this.size.height / 2f), this.size.minDimension * 0.26f, if (lit) SeqInk.copy(alpha = 0.85f) else color.copy(alpha = 0.6f))
        }
    }
}

/** The shape that goes with button [index]: a triangle, a square, a circle, a star, a diamond, a hexagon. */
internal fun DrawScope.drawSeqShape(index: Int, center: Offset, r: Float, color: Color) {
    val cx = center.x
    val cy = center.y
    when (index % SeqColors.size) {
        0 -> drawPath(polygon(listOf(Offset(cx, cy - r), Offset(cx + r * 0.95f, cy + r * 0.75f), Offset(cx - r * 0.95f, cy + r * 0.75f))), color)
        1 -> drawRect(color, Offset(cx - r * 0.8f, cy - r * 0.8f), Size(r * 1.6f, r * 1.6f))
        2 -> drawCircle(color, r * 0.9f, center)
        3 -> drawPath(polygon((0 until 10).map { k ->
            val angle = -PI / 2 + k * PI / 5
            val radius = if (k % 2 == 0) r * 1.05f else r * 0.45f
            Offset(cx + (cos(angle) * radius).toFloat(), cy + (sin(angle) * radius).toFloat())
        }), color)
        4 -> drawPath(polygon(listOf(Offset(cx, cy - r), Offset(cx + r * 0.8f, cy), Offset(cx, cy + r), Offset(cx - r * 0.8f, cy))), color)
        else -> drawPath(polygon((0 until 6).map { k ->
            val angle = k * PI / 3
            Offset(cx + (cos(angle) * r).toFloat(), cy + (sin(angle) * r).toFloat())
        }), color)
    }
}

private fun polygon(points: List<Offset>): Path = Path().apply {
    moveTo(points[0].x, points[0].y)
    for (k in 1 until points.size) lineTo(points[k].x, points[k].y)
    close()
}

@Composable
private fun ColorSeqResultCard(page: ColorSeqSnapshot, isFavorite: Boolean, onToggleFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val won = page.state.status == ColorSeqStatus.Won
    val rounds = page.state.roundsCleared

    Box(modifier = modifier.fillMaxWidth().testTag("colorResultCard").resultSurface(colors.bgSurface, GameKind.ColorSeq.world, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = stringResource(if (won) Res.string.colorseq_won else Res.string.colorseq_lost),
                modifier = Modifier.padding(horizontal = 36.dp).semantics { liveRegion = LiveRegionMode.Polite },
                style = type.title.copy(color = if (won) colors.correctFill else colors.error, textAlign = TextAlign.Center),
            )
            ResultStats(
                summary = stringResource(Res.string.colorseq_result, rounds),
                stats = listOf(
                    ResultStatData(rounds.toString(), stringResource(Res.string.result_stat_rounds), GameKind.ColorSeq.world),
                    // The longest sequence the player repeated: the lights of the last round cleared.
                    ResultStatData((if (rounds == 0) 0 else page.state.puzzle.lengthIn(rounds - 1)).toString(), stringResource(Res.string.result_stat_length), GameKind.ColorSeq.world),
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

/** A small picture for the thumbnails: four buttons with one of them lit. */
@Composable
internal fun ColorSeqThumbArt(side: Dp) {
    val gap = side * 0.05f
    val button = (side * 0.84f - gap) / 2f
    Column(Modifier.size(side * 0.84f), verticalArrangement = Arrangement.spacedBy(gap)) {
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (col in 0 until 2) {
                    val index = row * 2 + col
                    SeqButton(index = index, lit = index == 3, ring = null, size = button)
                }
            }
        }
    }
}
