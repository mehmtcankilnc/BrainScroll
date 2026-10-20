package com.mehmtcan.brainscroll.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.editor_difficulty_label
import brainscroll.shared.generated.resources.editor_difficulty_note
import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val TRACK_HEIGHT = 18.dp
private val THUMB_SIZE = 44.dp

/**
 * The second step of the feed editor: one level for the whole feed. The brain mascot shows it with its face, and the face
 * follows the thumb while it is dragged, so the choice is felt before it is made. Tapping the track or one of the names
 * under it chooses that level too.
 */
@Composable
fun FeedDifficultyStep(current: Difficulty, onSelect: (Difficulty) -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val reduce = LocalReduceMotion.current
    val haptics = LocalHaptics.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // The slider position, 0 to 1. It follows the finger while dragging and springs to the level when let go.
    val position = remember { Animatable(current.id / 2f) }
    var dragging by remember { mutableStateOf(false) }
    var widthPx by remember { mutableStateOf(1f) }
    // The level the slider shows now. The feed only learns of it once the thumb has settled: making new boards while the thumb
    // is still moving made it stutter.
    var target by remember { mutableStateOf(current) }
    suspend fun settle(level: Difficulty) {
        if (reduce) position.snapTo(level.id / 2f)
        else position.animateTo(level.id / 2f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
        if (level != current) onSelect(level)
    }
    LaunchedEffect(current) {
        if (!dragging && target != current) {
            target = current
            settle(current)
        }
    }

    fun levelAt(fraction: Float) = Difficulty.entries[(fraction * 2f).roundToInt().coerceIn(0, 2)]
    fun choose(level: Difficulty) {
        if (level != target) {
            haptics.perform(HapticKind.Light)
            target = level
            scope.launch { settle(level) }
        }
    }
    fun preview(level: Difficulty) {
        if (level != target) {
            haptics.perform(HapticKind.Light)
            target = level
        }
    }
    val t = position.value * 2f
    val tint = mascotTint(t)
    val name = difficultyName(target)
    val label = stringResource(Res.string.editor_difficulty_label)

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        BrainMascot(t = t, lookX = position.value * 2f - 1f, modifier = Modifier.size(200.dp))
        // The name is centered in the whole width from the first frame of the change: both the old and the new name sit in a box as wide
        // as the screen, so the crossfade never lays one of them out at the left first.
        Crossfade(targetState = name, modifier = Modifier.fillMaxWidth(), label = "difficultyName") { shown ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BasicText(
                    text = shown.uppercase(),
                    style = type.display.copy(color = tint, textAlign = TextAlign.Center),
                    modifier = Modifier.testTag("difficultyName"),
                )
            }
        }

        // The slider: a track that fills with the color of the face, and a round thumb.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(THUMB_SIZE)
                .testTag("difficultySlider")
                .semantics {
                    contentDescription = label
                    stateDescription = name
                    progressBarRangeInfo = ProgressBarRangeInfo(target.id.toFloat(), 0f..2f, steps = 1)
                    setProgress { value ->
                        choose(levelAt(value / 2f))
                        true
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val usable = (widthPx - with(density) { THUMB_SIZE.toPx() }).coerceAtLeast(1f)
                        val fraction = ((offset.x - with(density) { THUMB_SIZE.toPx() } / 2f) / usable).coerceIn(0f, 1f)
                        choose(levelAt(fraction))
                    }
                }
                .pointerInput(Unit) {
                    val thumb = with(density) { THUMB_SIZE.toPx() }
                    detectDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            dragging = false
                            scope.launch { settle(levelAt(position.value)) }
                        },
                        onDragCancel = { dragging = false },
                    ) { change, drag ->
                        change.consume()
                        val usable = (widthPx - thumb).coerceAtLeast(1f)
                        val next = (position.value + drag.x / usable).coerceIn(0f, 1f)
                        scope.launch { position.snapTo(next) }
                        preview(levelAt(next))
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            run {
                BoxWithConstraints(Modifier.fillMaxWidth().height(THUMB_SIZE)) {
                    widthPx = with(density) { maxWidth.toPx() }
                    val usable = maxWidth - THUMB_SIZE
                    Box(
                        Modifier.align(Alignment.CenterStart).fillMaxWidth().height(TRACK_HEIGHT).clip(CircleShape).background(colors.bgRaised),
                    )
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .width(THUMB_SIZE / 2 + usable * position.value)
                            .height(TRACK_HEIGHT)
                            .clip(CircleShape)
                            .background(tint),
                    )
                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = usable * position.value)
                            .size(THUMB_SIZE)
                            .background(Color.White, CircleShape)
                            .border(4.dp, tint, CircleShape),
                    )
                }
            }
        }

        // The names: each one chooses its level.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Difficulty.entries.forEach { level ->
                val chosen = level == target
                val levelName = difficultyName(level)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("level_${level.name}")
                        .semantics(mergeDescendants = true) {
                            selected = chosen
                            role = Role.RadioButton
                        }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) { choose(level) },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = levelName,
                        maxLines = 1,
                        style = type.label.copy(color = if (chosen) colors.textPrimary else colors.textSecondary, textAlign = TextAlign.Center),
                    )
                }
            }
        }

        BasicText(
            text = stringResource(Res.string.editor_difficulty_note),
            style = type.caption.copy(color = colors.textSecondary, textAlign = TextAlign.Center),
            modifier = Modifier.testTag("difficultyNote"),
        )
    }
}
