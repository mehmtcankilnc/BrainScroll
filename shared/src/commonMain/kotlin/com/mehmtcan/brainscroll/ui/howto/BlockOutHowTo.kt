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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_title_spoiler
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_blockout_start
import brainscroll.shared.generated.resources.howto_blockout_tech
import brainscroll.shared.generated.resources.howto_card_blockout_1
import brainscroll.shared.generated.resources.howto_card_blockout_2
import brainscroll.shared.generated.resources.howto_card_blockout_3
import com.mehmtcan.brainscroll.ui.blockout.drawTargetMark
import com.mehmtcan.brainscroll.ui.blockout.piecePalette
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the block out card: drag a block, or tap it and tap a cell; then slide the orange block out. */
internal val BlockOutHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_blockout_1, { t -> BlockOutDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_blockout_2, { t -> BlockOutDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_blockout_3, { t -> BlockOutDemo(step = 2, t = t) }),
        sceneStep(Res.string.howto_blockout_tech, BlockSlideScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_blockout_start, BlockStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

private const val SIZE = 4
private const val EXIT_ROW = 1

/** A block: the cell of its top left corner, its length, whether it lies along a row, and whether it is the orange target. */
private class DemoBlock(val row: Int, val column: Int, val length: Int, val horizontal: Boolean, val target: Boolean = false)

@Composable
internal fun BlockOutDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val handPainter = rememberHandPainter()
    val palette = piecePalette(colors)
    val frameColor = colors.borderStrong.copy(alpha = 0.22f)
    val cellColor = colors.bgSurface
    val selectedColor = colors.pendingFill
    val exitColor = colors.streakDay
    val bg = colors.bgPage

    // The target on the exit row, the blue block in front of it in column 2, and a green one in the corner.
    val target = DemoBlock(EXIT_ROW, 0, 2, true, target = true)
    val blocker = DemoBlock(0, 2, 2, false)
    val other = DemoBlock(2, 0, 2, false)

    // How far (in cells) the blocker has slid down, the target to the right, and whether the blocker is picked (the outline).
    var blockerSlide = 0f
    var targetSlide = 0f
    var picked = false
    var handFrom = Offset.Zero
    var handTo = Offset.Zero
    var handMove = 1f
    var press = 0f

    Canvas(Modifier.fillMaxSize()) {
        val fade = demoFade(t)
        val pad = size.width * 0.07f
        val exitWidth = size.width * 0.09f
        val framePad = size.width * 0.025f
        val boardSide = size.width - 2 * pad - exitWidth - 2 * framePad
        val gap = size.width * 0.012f
        val cell = (boardSide - gap * (SIZE - 1)) / SIZE
        val step1 = cell + gap
        val origin = Offset(pad + framePad, (size.height - boardSide) / 2f)
        fun cellCenter(row: Int, column: Int) = Offset(origin.x + column * step1 + cell / 2f, origin.y + row * step1 + cell / 2f)

        // What the hand does in this step.
        val offscreen = Offset(size.width, size.height)
        when (step) {
            0 -> {
                // Drag the blue block down two cells.
                val grab = cellCenter(0, 2) + Offset(0f, step1 * 0.5f)
                val drag = demoEase(demoSegment(t, 0.4f, 0.75f))
                blockerSlide = 2f * drag
                handFrom = offscreen
                val arrive = demoEase(demoSegment(t, 0.05f, 0.3f))
                handTo = grab + Offset(0f, step1 * 2f * drag)
                handMove = if (t < 0.4f) arrive else 1f
                if (t >= 0.4f) handFrom = grab
                press = demoSegment(t, 0.3f, 0.4f)
            }
            1 -> {
                // Tap the blue block (it gets an outline), then tap the empty cell at the end of its column.
                val first = cellCenter(1, 2)
                val second = cellCenter(3, 2)
                picked = t in 0.3f..0.62f
                blockerSlide = 2f * demoEase(demoSegment(t, 0.58f, 0.8f))
                val (pos, pr) = demoHand(t, listOf(DemoTap(0.3f, first), DemoTap(0.52f, second)), offscreen, travel = 0.12f)
                handFrom = pos
                handTo = pos
                press = pr
            }
            else -> {
                // The blue block is out of the way: drag the orange block right, out of the exit.
                blockerSlide = 2f
                val grab = cellCenter(EXIT_ROW, 0) + Offset(step1 * 0.5f, 0f)
                val drag = demoEase(demoSegment(t, 0.4f, 0.85f))
                targetSlide = 4f * drag
                val arrive = demoEase(demoSegment(t, 0.05f, 0.3f))
                handFrom = if (t < 0.4f) offscreen else grab
                handTo = grab + Offset(step1 * 4f * drag, 0f)
                handMove = if (t < 0.4f) arrive else 1f
                press = demoSegment(t, 0.3f, 0.4f)
            }
        }

        // The frame, the empty cells, and the opening of the frame where the target leaves.
        val frameTopLeft = Offset(origin.x - framePad, origin.y - framePad)
        val frameSize = Size(boardSide + 2 * framePad, boardSide + 2 * framePad)
        drawRoundRect(frameColor.copy(alpha = frameColor.alpha * fade), frameTopLeft, frameSize, CornerRadius(size.width * 0.04f))
        for (r in 0 until SIZE) for (c in 0 until SIZE) {
            val o = Offset(origin.x + c * step1, origin.y + r * step1)
            drawRoundRect(cellColor.copy(alpha = fade), o, Size(cell, cell), CornerRadius(cell * 0.18f))
        }
        drawRect(bg, Offset(origin.x + boardSide, origin.y + EXIT_ROW * step1), Size(framePad + 1f, cell))
        // The exit: an arrow on the right of the exit row.
        val exitX = origin.x + boardSide + framePad + exitWidth * 0.15f
        val exitY = origin.y + EXIT_ROW * step1 + cell * 0.2f
        val arrow = Path().apply {
            moveTo(exitX, exitY)
            lineTo(exitX + exitWidth * 0.8f, exitY + cell * 0.3f)
            lineTo(exitX, exitY + cell * 0.6f)
            close()
        }
        drawPath(arrow, exitColor.copy(alpha = fade))

        fun drawBlock(block: DemoBlock, color: Color, slide: Float, outline: Boolean) {
            val dx = if (block.horizontal) slide * step1 else 0f
            val dy = if (block.horizontal) 0f else slide * step1
            val o = Offset(origin.x + block.column * step1 + dx, origin.y + block.row * step1 + dy)
            val w = if (block.horizontal) cell * block.length + gap * (block.length - 1) else cell
            val h = if (block.horizontal) cell else cell * block.length + gap * (block.length - 1)
            drawRoundRect(color.copy(alpha = fade), o, Size(w, h), CornerRadius(cell * 0.18f))
            if (outline) drawRoundRect(selectedColor.copy(alpha = fade), o, Size(w, h), CornerRadius(cell * 0.18f), style = Stroke(width = 3.dp.toPx()))
            if (block.target) {
                val markSide = cell * 0.5f
                translateMark(Offset(o.x + (cell - markSide) / 2f, o.y + (cell - markSide) / 2f), markSide, bg.copy(alpha = fade))
            }
        }
        // The blocks leave the board through the opening: nothing is drawn right of it but the exit.
        clipRect(left = 0f, top = 0f, right = origin.x + boardSide + framePad, bottom = size.height) {
            drawBlock(other, palette[1 % palette.size], 0f, false)
            drawBlock(blocker, palette[0], blockerSlide, picked)
            drawBlock(target, exitColor, targetSlide, false)
        }

        val hand = Offset(handFrom.x + (handTo.x - handFrom.x) * handMove, handFrom.y + (handTo.y - handFrom.y) * handMove)
        drawHandCursor(handPainter, hand, cell * 0.95f, press, alpha = fade)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.translateMark(topLeft: Offset, side: Float, color: Color) {
    drawTargetMark(color, topLeft, side)
}
