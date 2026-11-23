package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.ui.graphics.Color
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.mines.numberColor
import com.mehmtcan.brainscroll.ui.theme.BrainScrollColors
import com.mehmtcan.brainscroll.ui.theme.world

private fun mineInk(text: String, colors: BrainScrollColors): Color = numberColor(text.toIntOrNull() ?: 0, colors)

/** How many of the cells next to ([r], [c]) are mines. */
private fun count(n: Int, mines: Set<Pair<Int, Int>>, r: Int, c: Int) =
    (-1..1).sumOf { dr -> (-1..1).count { dc -> (dr != 0 || dc != 0) && (r + dr to c + dc) in mines && r + dr in 0 until n && c + dc in 0 until n } }

/** A square mine board: [open] cells show their number (nothing for a zero), the others stay hidden, [flags] have a flag on them. */
private fun mineScene(n: Int, mines: Set<Pair<Int, Int>>, open: Set<Pair<Int, Int>>, flags: Set<Pair<Int, Int>> = emptySet(), script: DemoSceneBuilder.() -> Unit) =
    demoScene(n, n, numberInk = ::mineInk, focus = GameKind.Mines.world) {
        for (r in 0 until n) for (c in 0 until n) {
            val at = r to c
            when {
                at in open -> set(r, c, SceneFill.Open, count(n, mines, r, c).let { if (it == 0) "" else it.toString() })
                at in flags -> set(r, c, SceneFill.Hidden, mark = SceneMark.Flag)
                else -> set(r, c, SceneFill.Hidden)
            }
        }
        script()
    }

private fun everythingBut(n: Int, vararg except: Pair<Int, Int>) = (0 until n).flatMap { r -> (0 until n).map { c -> r to c } }.filter { it !in except }.toSet()

private val Layout = setOf(1 to 3, 3 to 1)

/** Goal: a number says how many mines touch it, and the mines show up. */
internal val MinesGoalScene = mineScene(5, Layout, everythingBut(5, 1 to 3, 3 to 1)) {
    tint(0.08f, 0.5f, SceneRole.Focus, (1..3).flatMap { r -> (1..3).map { c -> cell(r, c) } }.filter { it != cell(2, 2) })
    ring(0.1f, 0.5f, SceneRole.Target, 2 to 2)
    mark(0.55f, 1, 3, SceneMark.Mine)
    mark(0.62f, 3, 1, SceneMark.Mine)
}

/** Technique 1: a 1 with only one hidden cell around it. */
internal val MinesSingleScene = mineScene(5, setOf(1 to 3), everythingBut(5, 1 to 3)) {
    ring(0.08f, 0.55f, SceneRole.Evidence, 0 to 2)
    ring(0.4f, 0.94f, SceneRole.Target, 1 to 3)
    mark(0.7f, 1, 3, SceneMark.Flag)
}

/** Technique 2: a number that has all its flags: the hand taps it twice and the cells around it open. */
internal val MinesChordScene: DemoScene = run {
    val mines = setOf(1 to 3)
    val open = (0..2).flatMap { r -> (0..2).map { c -> r to c } }.toSet() + setOf(0 to 3, 0 to 4)
    mineScene(5, mines, open, flags = setOf(1 to 3)) {
        ring(0.08f, 0.5f, SceneRole.Evidence, 2 to 2, 1 to 3)
        tap(0.3f, 2, 2)
        tap(0.4f, 2, 2)
        for ((k, at) in listOf(2 to 3, 3 to 3, 3 to 2, 3 to 1).withIndex()) {
            open(0.52f + k * 0.04f, at.first, at.second, count(5, mines, at.first, at.second).let { if (it == 0) "" else it.toString() })
        }
    }
}

/** Technique 3: the 1-2-1 row. */
internal val MinesPatternScene = demoScene(3, 3, numberInk = ::mineInk, focus = GameKind.Mines.world) {
    row(0, "h h h")
    row(1, "1 2 1", given = SceneFill.Open)
    row(2, "_ _ _")
    ring(0.06f, 0.36f, SceneRole.Evidence, 1 to 0, 1 to 2)
    ring(0.3f, 0.56f, SceneRole.Evidence, 1 to 1)
    ring(0.5f, 0.94f, SceneRole.Target, 0 to 1)
    open(0.68f, 0, 1, "2")
    mark(0.8f, 0, 0, SceneMark.Flag)
    mark(0.86f, 0, 2, SceneMark.Flag)
}

/** First step: the first tap is in the middle and a blank cell opens a whole area. */
internal val MinesStartScene: DemoScene = run {
    val n = 6
    val mines = setOf(0 to 5, 3 to 0, 5 to 3)
    val queue = ArrayDeque<Triple<Int, Int, Int>>()
    val seen = HashSet<Pair<Int, Int>>()
    val order = ArrayList<Triple<Int, Int, Int>>()
    queue += Triple(2, 3, 0)
    seen += 2 to 3
    while (queue.isNotEmpty()) {
        val (r, c, d) = queue.removeFirst()
        order += Triple(r, c, d)
        if (count(n, mines, r, c) != 0) continue
        for (dr in -1..1) for (dc in -1..1) {
            val next = r + dr to c + dc
            if (next.first !in 0 until n || next.second !in 0 until n || next in seen || next in mines) continue
            seen += next
            queue += Triple(next.first, next.second, d + 1)
        }
    }
    mineScene(n, mines, emptySet()) {
        tap(0.12f, 2, 3)
        for ((r, c, d) in order) open(0.22f + d * 0.05f, r, c, count(n, mines, r, c).let { if (it == 0) "" else it.toString() })
    }
}
