package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.ui.graphics.Color
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.world

// The pictures of the search and move games: a small board and what happens on it, one picture for a way of playing and one for the first move.

private val Blue = Color(0xFF64B5F6)
private val Gold = Color(0xFFFFD54F)
private val Leaf = Color(0xFF81C784)

private fun DemoSceneBuilder.hold(r: Int, c: Int, mark: SceneMark) = set(r, c, SceneFill.Empty, mark = mark)

// ---------------------------------------------------------------------------------------------------------------------------------
// Lights Out: a 3 by 3 board; a tap flips the cell and the four beside it. A lit light has a mark.
private fun lightsScene(lit: List<Pair<Int, Int>>, script: DemoSceneBuilder.() -> Unit) = demoScene(3, 3, focus = GameKind.Lights.world) {
    for ((r, c) in lit) hold(r, c, SceneMark.Fill)
    script()
}

/** Technique: one tap flips the lights round it, so a tap on the right cell puts out several lit lights at once. */
internal val LightsFlipScene = lightsScene(listOf(0 to 0, 0 to 1, 1 to 0)) {
    ring(0.06f, 0.4f, SceneRole.Evidence, 0 to 1, 1 to 0)
    ring(0.2f, 0.6f, SceneRole.Target, 0 to 0)
    tap(0.38f, 0, 0)
    mark(0.46f, 0, 0, SceneMark.None)
    mark(0.46f, 0, 1, SceneMark.None)
    mark(0.46f, 1, 0, SceneMark.None)
}

/** First step: a lit light in the top row goes out with a tap on the cell right under it, then the next row down. */
internal val LightsStartScene = lightsScene(listOf(0 to 1, 1 to 1)) {
    ring(0.06f, 0.4f, SceneRole.Evidence, 0 to 1)
    ring(0.3f, 0.94f, SceneRole.Target, 1 to 1)
    tap(0.45f, 1, 1)
    mark(0.52f, 0, 1, SceneMark.None)
    mark(0.52f, 1, 1, SceneMark.None)
    mark(0.52f, 1, 0, SceneMark.Fill)
    mark(0.52f, 1, 2, SceneMark.Fill)
    mark(0.52f, 2, 1, SceneMark.Fill)
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Sliding tiles: a 3 by 3 board with a gap.
private fun slideScene(vararg rows: String, script: DemoSceneBuilder.() -> Unit) = demoScene(3, 3, focus = GameKind.Slide.world) {
    rows.forEachIndexed { r, text -> row(r, text) }
    script()
}

/** Technique: put the tiles in order row by row, and leave the ones that are in place alone. */
internal val SlideRowScene = slideScene("1.3", "426", "758") {
    ring(0.06f, 0.4f, SceneRole.Evidence, 0 to 0, 0 to 2)
    ring(0.3f, 0.6f, SceneRole.Target, 0 to 1)
    tap(0.42f, 1, 1)
    place(0.5f, 0, 1, "2", fill = SceneFill.Given)
    place(0.5f, 1, 1, "")
    tint(0.58f, 0.94f, SceneRole.Good, rowCells(0))
}

/** First step: bring the 1 to the top left corner, then the 2 and the 3 beside it. */
internal val SlideStartScene = slideScene(".13", "246", "758") {
    ring(0.06f, 0.4f, SceneRole.Target, 0 to 0)
    tap(0.32f, 0, 1)
    place(0.4f, 0, 0, "1", fill = SceneFill.Given)
    place(0.4f, 0, 1, "")
    tint(0.5f, 0.94f, SceneRole.Good, listOf(cell(0, 0)))
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Block Out: a 4 by 4 board, the orange block has to leave through the right side of the second row; a blue block stands in the way.
private fun blockScene(script: DemoSceneBuilder.() -> Unit) = demoScene(4, 4, focus = GameKind.BlockOut.world) {
    regions("8888", "6618", "8818", "8888")
    hold(1, 3, SceneMark.ArrowRight)
    script()
}

/** Technique: a block slides only along its own line; a block in the way is moved along its line, out of the way. */
internal val BlockSlideScene = blockScene {
    ring(0.06f, 0.4f, SceneRole.Evidence, 1 to 2, 2 to 2)
    ring(0.4f, 0.7f, SceneRole.Target, 3 to 2)
    mark(0.46f, 2, 2, SceneMark.ArrowDown)
    ring(0.7f, 0.94f, SceneRole.Good, 1 to 0, 1 to 1)
    mark(0.74f, 1, 1, SceneMark.ArrowRight)
}

/** First step: look at the way out of the orange block and play the block that stands in it. */
internal val BlockStartScene = blockScene {
    tint(0.06f, 0.5f, SceneRole.Focus, listOf(cell(1, 2), cell(1, 3)))
    ring(0.12f, 0.5f, SceneRole.Evidence, 1 to 2)
    ring(0.5f, 0.94f, SceneRole.Target, 1 to 2, 2 to 2)
    tap(0.6f, 2, 2)
    mark(0.66f, 2, 2, SceneMark.ArrowDown)
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Target number: the target and three numbers in a row; two numbers are joined and the result takes their place.
private fun targetScene(target: String, a: String, b: String, c: String, script: DemoSceneBuilder.() -> Unit) = demoScene(1, 4, focus = GameKind.Target.world) {
    set(0, 0, SceneFill.Clue, target)
    set(0, 1, SceneFill.Given, a)
    set(0, 2, SceneFill.Given, b)
    set(0, 3, SceneFill.Given, c)
    script()
}

/** Technique: build a product that is a little too big, then take the extra away. */
internal val TargetOvershootScene = targetScene("28", "6", "5", "2") {
    ring(0.06f, 0.3f, SceneRole.Evidence, 0 to 1, 0 to 2)
    place(0.34f, 0, 2, "30", fill = SceneFill.Given); place(0.34f, 0, 1, "")
    ring(0.45f, 0.7f, SceneRole.Evidence, 0 to 2, 0 to 3)
    place(0.74f, 0, 3, "28", fill = SceneFill.Given); place(0.74f, 0, 2, "")
    ring(0.76f, 0.94f, SceneRole.Target, 0 to 0)
}

/** First step: start with the product that comes nearest to the target. */
internal val TargetStartScene = targetScene("14", "3", "4", "2") {
    ring(0.06f, 0.3f, SceneRole.Evidence, 0 to 1, 0 to 2)
    place(0.34f, 0, 2, "12", fill = SceneFill.Given); place(0.34f, 0, 1, "")
    ring(0.45f, 0.7f, SceneRole.Evidence, 0 to 2, 0 to 3)
    place(0.74f, 0, 3, "14", fill = SceneFill.Given); place(0.74f, 0, 2, "")
    ring(0.76f, 0.94f, SceneRole.Target, 0 to 0)
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Lasers: the laser is the arrow on the left, the yellow disk is the target, a mirror is a slash.
private fun laserScene(script: DemoSceneBuilder.() -> Unit) = demoScene(4, 4, focus = GameKind.Lasers.world) {
    hold(3, 0, SceneMark.ArrowRight)
    hold(0, 2, SceneMark.Lamp)
    script()
}

/** Technique: a beam that meets a mirror turns a right angle: here it goes up to the target. */
internal val LaserBounceScene = laserScene {
    set(3, 2, SceneFill.Given, "/")
    ring(0.1f, 0.5f, SceneRole.Evidence, 3 to 2)
    val beam = listOf(3 to 1, 3 to 2, 2 to 2, 1 to 2, 0 to 2)
    for ((k, at) in beam.withIndex()) tint(0.2f + k * 0.1f, 0.94f, SceneRole.Focus, listOf(cell(at.first, at.second)))
    ring(0.7f, 0.94f, SceneRole.Target, 0 to 2)
}

/** First step: think back from the target: which way must the beam come, and put a mirror where that line meets the beam. */
internal val LaserStartScene = laserScene {
    ring(0.06f, 0.4f, SceneRole.Target, 0 to 2)
    ring(0.2f, 0.6f, SceneRole.Evidence, 1 to 2, 2 to 2)
    ring(0.5f, 0.94f, SceneRole.Target, 3 to 2)
    place(0.7f, 3, 2, "/", fill = SceneFill.Given)
    tint(0.78f, 0.94f, SceneRole.Focus, listOf(cell(3, 1)))
}


/** Every picture of this file, with a name for the screenshots of the tests. */
internal val SearchScenes: List<Pair<String, DemoScene>> by lazy {
    listOf(
        "lights_flip" to LightsFlipScene, "lights_start" to LightsStartScene,
        "slide_row" to SlideRowScene, "slide_start" to SlideStartScene,
        "block_slide" to BlockSlideScene, "block_start" to BlockStartScene,
        "target_over" to TargetOvershootScene, "target_start" to TargetStartScene,
        "laser_bounce" to LaserBounceScene, "laser_start" to LaserStartScene,
    )
}
