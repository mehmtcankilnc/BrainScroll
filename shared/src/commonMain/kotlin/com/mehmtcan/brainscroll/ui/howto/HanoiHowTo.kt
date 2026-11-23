package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_hanoi_start
import brainscroll.shared.generated.resources.howto_hanoi_tech
import brainscroll.shared.generated.resources.howto_card_hanoi_1
import brainscroll.shared.generated.resources.howto_card_hanoi_2
import brainscroll.shared.generated.resources.howto_card_hanoi_3
import com.mehmtcan.brainscroll.game.memory.CardSymbol
import com.mehmtcan.brainscroll.ui.memory.symbolColor
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.math.sin

/** The three steps of the Hanoi card: carry a disk, a big disk never goes on a small one, bring them all to the flag. */
internal val HanoiHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_hanoi_1, { t -> HanoiDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_hanoi_2, { t -> HanoiDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_hanoi_3, { t -> HanoiDemo(step = 2, t = t) }, cycleMillis = 6400),
        HowToStep(Res.string.howto_hanoi_tech, { t -> HanoiDemo(step = 3, t = t) }, cycleMillis = 8000, title = Res.string.howto_title_solve),
        HowToStep(Res.string.howto_hanoi_start, { t -> HanoiDemo(step = 4, t = t) }, cycleMillis = 4800, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

/** One move of a picture: the tower it is picked up from (tapped at [pickAt]), the tower it goes to (tapped at [dropAt]), and whether the tower refuses it. */
private class DemoMove(val from: Int, val to: Int, val pickAt: Float, val dropAt: Float, val refused: Boolean = false)

private const val CARRY = 0.12f

/**
 * The picture of a step: three towers with their disks, as on the page. The hand taps a tower, the top disk lifts, the hand taps
 * the next tower and the disk is carried there (or, when the disk is bigger than the one on top, it is turned back).
 */
@Composable
internal fun HanoiDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val measurer = rememberTextMeasurer()
    val handPainter = rememberHandPainter()
    val tile = colors.bgSurface
    val edge = colors.borderStrong.copy(alpha = 0.3f)
    val peg = colors.borderStrong.copy(alpha = 0.6f)
    val flag = colors.correctFill
    val picked = colors.pendingFill
    val error = colors.error
    // Disks are numbers from 1 (the smallest); a tower lists its disks from the bottom up.
    val start: List<List<Int>>
    val moves: List<DemoMove>
    val total: Int
    when (step) {
        0 -> {
            start = listOf(listOf(3, 2, 1), emptyList(), emptyList())
            total = 3
            moves = listOf(DemoMove(0, 1, 0.12f, 0.4f))
        }
        1 -> {
            start = listOf(listOf(3, 2), listOf(1), emptyList())
            total = 3
            moves = listOf(DemoMove(0, 1, 0.12f, 0.4f, refused = true))
        }
        2 -> {
            start = listOf(listOf(2, 1), emptyList(), emptyList())
            total = 2
            moves = listOf(
                DemoMove(0, 1, 0.04f, 0.15f),
                DemoMove(0, 2, 0.34f, 0.45f),
                DemoMove(1, 2, 0.64f, 0.75f),
            )
        }
        // The big disk: the two small ones go to the middle tower first, then the big one crosses to the empty goal.
        3 -> {
            start = listOf(listOf(3, 2, 1), emptyList(), emptyList())
            total = 3
            moves = listOf(
                DemoMove(0, 2, 0.03f, 0.1f),
                DemoMove(0, 1, 0.24f, 0.31f),
                DemoMove(2, 1, 0.45f, 0.52f),
                DemoMove(0, 2, 0.66f, 0.73f),
            )
        }
        // The first move: with three disks (an odd number) the smallest goes straight to the goal tower.
        else -> {
            start = listOf(listOf(3, 2, 1), emptyList(), emptyList())
            total = 3
            moves = listOf(DemoMove(0, 2, 0.15f, 0.4f))
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val pad = size.width * 0.07f
        val gap = size.width * 0.04f
        val towerWidth = (size.width - 2 * pad - 2 * gap) / 3f
        val towerHeight = size.height - 2 * pad
        val diskHeight = minOf(towerHeight * 0.1f, (towerHeight - towerHeight * 0.25f) / (total + 1))
        val diskGap = size.width * 0.006f
        val bottomPad = size.width * 0.03f
        fun left(tower: Int) = pad + tower * (towerWidth + gap)
        fun centerX(tower: Int) = left(tower) + towerWidth / 2f
        fun diskTop(level: Int) = pad + towerHeight - bottomPad - diskHeight * (level + 1) - diskGap * level
        val headRoom = pad + towerHeight * 0.06f
        val corner = CornerRadius(size.width * 0.045f)

        // The towers as they are at time t: every move that has finished is done.
        val towers = start.map { it.toMutableList() }
        var active: DemoMove? = null
        moves.forEach { m ->
            val end = m.dropAt + CARRY
            if (t >= end) {
                if (!m.refused) towers[m.to].add(towers[m.from].removeAt(towers[m.from].lastIndex))
            } else if (t >= m.pickAt) {
                active = m
            }
        }
        // Which tower is picked up now, and the disk that is carried.
        val move = active
        val liftedTower = if (move != null && t >= move.pickAt && t < move.dropAt) move.from else -1
        val flash = move != null && move.refused && t >= move.dropAt + 0.03f

        fun drawDisk(number: Int, centerX: Float, top: Float) {
            val fraction = if (total <= 1) 0.9f else 0.64f + 0.3f * (number - 1) / (total - 1)
            val width = (towerWidth - size.width * 0.04f) * fraction
            val color = symbolColor(CardSymbol.entries[(number - 1) % CardSymbol.entries.size], colors).copy(alpha = fade)
            drawRoundRect(color, Offset(centerX - width / 2f, top), Size(width, diskHeight), CornerRadius(diskHeight / 2.5f))
            val label = measurer.measure(number.toString(), TextStyle(color = colors.bgPage.copy(alpha = fade), fontSize = (diskHeight * 0.55f / (density * fontScale)).sp, fontWeight = FontWeight.Bold))
            drawText(label, topLeft = Offset(centerX - label.size.width / 2f, top + (diskHeight - label.size.height) / 2f))
        }

        for (tower in 0 until 3) {
            val topLeft = Offset(left(tower), pad)
            val isGoal = tower == 2
            drawRoundRect(tile.copy(alpha = fade), topLeft, Size(towerWidth, towerHeight), corner)
            if (isGoal) drawRoundRect(flag.copy(alpha = 0.13f * fade), topLeft, Size(towerWidth, towerHeight), corner)
            val selected = tower == liftedTower
            val refusing = flash && tower == move?.to
            drawRoundRect(
                color = if (refusing) error.copy(alpha = fade) else if (selected) picked.copy(alpha = fade) else edge.copy(alpha = edge.alpha * fade),
                topLeft = topLeft,
                size = Size(towerWidth, towerHeight),
                cornerRadius = corner,
                style = Stroke(width = (if (selected || refusing) 2.5.dp else 1.5.dp).toPx()),
            )
            val pegWidth = size.width * 0.02f
            val pegHeight = towerHeight * 0.9f
            val pegTop = pad + towerHeight - size.width * 0.02f - pegHeight
            drawRoundRect(peg.copy(alpha = peg.alpha * fade), Offset(centerX(tower) - pegWidth / 2f, pegTop), Size(pegWidth, pegHeight), CornerRadius(pegWidth / 2f))
            if (isGoal) {
                val x = centerX(tower) + pegWidth / 2f
                val path = Path().apply {
                    moveTo(x, pegTop)
                    lineTo(x + size.width * 0.075f, pegTop + size.width * 0.025f)
                    lineTo(x, pegTop + size.width * 0.05f)
                    close()
                }
                drawPath(path, flag.copy(alpha = fade))
            }
            // The disks of the tower, but the one that is being carried is drawn on its own.
            towers[tower].forEachIndexed { level, number ->
                if (move != null && tower == move.from && level == towers[tower].lastIndex) return@forEachIndexed
                drawDisk(number, centerX(tower), diskTop(level))
            }
        }

        if (move != null) {
            val disk = towers[move.from].last()
            val fromLevel = towers[move.from].lastIndex
            val lift = headRoom
            val fromTop = diskTop(fromLevel)
            val toLevel = towers[move.to].size
            val toTop = diskTop(toLevel)
            val x: Float
            val y: Float
            if (t < move.dropAt) {
                // Picked up: the disk rises over its tower.
                val rise = demoEase(demoSegment(t, move.pickAt, move.pickAt + 0.05f))
                x = centerX(move.from)
                y = fromTop + (lift - fromTop) * rise
            } else {
                val p = demoSegment(t, move.dropAt, move.dropAt + CARRY)
                // Over to the other tower and down; a refused disk goes half way and turns back.
                val across = if (move.refused) demoEase(sin(p * 3.1416f)) * 0.55f else demoEase(demoSegment(p, 0f, 0.6f))
                val down = if (move.refused) 1f - demoEase(demoSegment(p, 0.6f, 1f)) else demoEase(demoSegment(p, 0.6f, 1f))
                x = centerX(move.from) + (centerX(move.to) - centerX(move.from)) * across
                val restTop = if (move.refused) lift else toTop
                y = if (move.refused) lift + (fromTop - lift) * demoEase(demoSegment(p, 0.6f, 1f)) else lift + (restTop - lift) * down
            }
            drawDisk(disk, x, y)
        }

        // The hand taps the tower it picks from, then the tower it drops on.
        val taps = moves.flatMap { listOf(DemoTap(it.pickAt, Offset(centerX(it.from), pad + towerHeight * 0.42f)), DemoTap(it.dropAt, Offset(centerX(it.to), pad + towerHeight * 0.42f))) }
        val (hand, press) = demoHand(t, taps, Offset(size.width * 1.0f, size.height * 1.0f))
        drawHandCursor(handPainter, hand, size.width * 0.22f, press, alpha = fade)
    }
}
