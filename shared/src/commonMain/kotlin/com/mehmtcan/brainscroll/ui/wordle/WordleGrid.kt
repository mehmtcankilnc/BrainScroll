package com.mehmtcan.brainscroll.ui.wordle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.game.wordle.LetterResult
import com.mehmtcan.brainscroll.game.wordle.RoundSnapshot
import com.mehmtcan.brainscroll.ui.components.LetterTile
import com.mehmtcan.brainscroll.ui.components.TileState
import kotlinx.coroutines.delay

private val TileGap = 6.dp
private val MaxTile = 56.dp

/** docs/design.md section 6: motion.flip is 500 ms with a 100 ms stagger per tile. */
private const val FlipMillis = 500
private const val FlipStaggerMillis = 100
private val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * The guess grid: submitted rows, the row being typed, and empty rows.
 * Rows that are already submitted when this composable first appears are drawn instantly;
 * only rows submitted while it is on screen flip.
 */
@Composable
fun WordleGrid(round: RoundSnapshot, modifier: Modifier = Modifier) {
    // Rows present when we first appear (e.g. swiping back to a half-finished puzzle) must not replay the flip.
    val initialRows = remember { round.rows.size }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val byWidth = (maxWidth - TileGap * (round.wordLength - 1)) / round.wordLength
        val byHeight = (maxHeight - TileGap * (round.maxAttempts - 1)) / round.maxAttempts
        val tileSize = minOf(MaxTile, byWidth, byHeight)

        Column(verticalArrangement = Arrangement.spacedBy(TileGap)) {
            for (rowIndex in 0 until round.maxAttempts) {
                when {
                    rowIndex < round.rows.size -> {
                        val row = round.rows[rowIndex]
                        Row(horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                            row.word.forEachIndexed { i, letter ->
                                RevealTile(
                                    letter = letter.toString(),
                                    result = row.results[i],
                                    animate = rowIndex >= initialRows,
                                    delayMillis = i * FlipStaggerMillis,
                                    size = tileSize,
                                )
                            }
                        }
                    }
                    rowIndex == round.rows.size && !round.isFinished ->
                        InputRow(round, tileSize)
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                        repeat(round.wordLength) { LetterTile("", TileState.Empty, size = tileSize) }
                    }
                }
            }
        }
    }
}

/** The row being typed. It shakes when a guess is rejected. */
@Composable
private fun InputRow(round: RoundSnapshot, tileSize: Dp) {
    val shake = remember { Animatable(0f) }
    val density = LocalDensity.current

    // This row is created fresh after every accepted guess. The round may already have had rejected guesses
    // (errorTick > 0), so only a tick that changes while we are on screen means "shake".
    var seenTick by remember { mutableIntStateOf(round.errorTick) }
    LaunchedEffect(round.errorTick) {
        if (round.errorTick == seenTick) return@LaunchedEffect
        seenTick = round.errorTick
        for (target in listOf(-10f, 10f, -8f, 8f, 0f)) shake.animateTo(target, tween(50))
    }

    Row(
        // The tag comes after offset so the node's bounds include the shake (tests read them).
        modifier = Modifier
            .offset { IntOffset(with(density) { shake.value.dp.roundToPx() }, 0) }
            .testTag("inputRow"),
        horizontalArrangement = Arrangement.spacedBy(TileGap),
    ) {
        for (i in 0 until round.wordLength) {
            val letter = round.input.getOrNull(i)
            LetterTile(
                letter = letter?.toString() ?: "",
                state = if (letter != null) TileState.Filled else TileState.Empty,
                size = tileSize,
            )
        }
    }
}

private fun LetterResult.toTileState() = when (this) {
    LetterResult.Correct -> TileState.Correct
    LetterResult.Present -> TileState.Pending
    LetterResult.Absent -> TileState.Absent
}

/**
 * A tile showing a guess result. With [animate] it flips around its horizontal axis:
 * the first half turns the typed tile edge-on, the second half turns the result face up.
 */
@Composable
private fun RevealTile(letter: String, result: LetterResult, animate: Boolean, delayMillis: Int, size: Dp) {
    if (!animate) {
        LetterTile(letter, result.toTileState(), size = size)
        return
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        progress.animateTo(1f, tween(FlipMillis, easing = StandardEasing))
    }

    val p = progress.value
    LetterTile(
        letter = letter,
        state = if (p < 0.5f) TileState.Filled else result.toTileState(),
        modifier = Modifier.graphicsLayer {
            // 0 -> 90 degrees (edge-on) -> back to 0 on the other side.
            rotationX = if (p < 0.5f) p * 180f else (p - 1f) * 180f
            cameraDistance = 12f * density
        },
        size = size,
    )
}
