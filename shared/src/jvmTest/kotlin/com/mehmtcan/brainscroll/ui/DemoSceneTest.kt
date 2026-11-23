package com.mehmtcan.brainscroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.howto.DemoScene
import com.mehmtcan.brainscroll.ui.howto.DemoSceneView
import com.mehmtcan.brainscroll.ui.howto.MinesChordScene
import com.mehmtcan.brainscroll.ui.howto.MinesGoalScene
import com.mehmtcan.brainscroll.ui.howto.MinesPatternScene
import com.mehmtcan.brainscroll.ui.howto.MinesSingleScene
import com.mehmtcan.brainscroll.ui.howto.MinesStartScene
import com.mehmtcan.brainscroll.ui.howto.SudokuGoalScene
import com.mehmtcan.brainscroll.ui.howto.SudokuPlaceScene
import com.mehmtcan.brainscroll.ui.howto.SudokuSingleScene
import com.mehmtcan.brainscroll.ui.howto.SudokuStartScene
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The pictures made from a script, drawn at fixed moments of their loop (the clock of the test does not run infinite animations, so the
 * time is given): they are looked at in the screenshots, and every event of a script has to happen inside the loop.
 */
@OptIn(ExperimentalTestApi::class)
class DemoSceneTest {

    private val own: List<Pair<String, DemoScene>> = listOf(
        "sudoku_goal" to SudokuGoalScene,
        "sudoku_single" to SudokuSingleScene,
        "sudoku_place" to SudokuPlaceScene,
        "sudoku_start" to SudokuStartScene,
        "mines_goal" to MinesGoalScene,
        "mines_single" to MinesSingleScene,
        "mines_chord" to MinesChordScene,
        "mines_pattern" to MinesPatternScene,
        "mines_start" to MinesStartScene,
    )

    private val scenes: List<Pair<String, DemoScene>> get() = own + com.mehmtcan.brainscroll.ui.howto.DeductionScenes + com.mehmtcan.brainscroll.ui.howto.SearchScenes + com.mehmtcan.brainscroll.ui.howto.RestScenes + com.mehmtcan.brainscroll.ui.howto.MirrorScenes + com.mehmtcan.brainscroll.ui.howto.OddOneScenes + com.mehmtcan.brainscroll.ui.howto.CrossSumScenes

    @Test
    fun everySceneHasSomethingToShow() {
        for ((name, scene) in scenes) assertTrue(scene.events.size >= 2, "$name has a script")
    }

    @Test
    fun everyEventHappensInsideTheLoop() {
        for ((name, scene) in scenes) {
            for (e in scene.events) {
                assertTrue(e.at in 0f..1f, "$name: an event at ${e.at}")
                if (e is com.mehmtcan.brainscroll.ui.howto.SceneRing) assertTrue(e.until <= 1f && e.until > e.at, "$name: a ring until ${e.until}")
                if (e is com.mehmtcan.brainscroll.ui.howto.SceneTint) assertTrue(e.until <= 1f && e.until > e.at, "$name: a tint until ${e.until}")
            }
        }
    }

    @Test
    fun theScenesAreDrawnAtThreeMomentsOfTheirLoop() = runComposeUiTest {
        val time = mutableFloatStateOf(0.1f)
        val current = androidx.compose.runtime.mutableStateOf(SudokuGoalScene)
        setContent {
            BrainScrollTheme {
                Box(Modifier.requiredSize(300.dp).background(androidx.compose.ui.graphics.Color(0xFF111111))) { DemoSceneView(current.value, time) }
            }
        }
        for ((name, scene) in scenes) {
            current.value = scene
            waitForIdle()
            for ((label, t) in listOf("a" to 0.3f, "b" to 0.55f, "c" to 0.88f)) {
                time.floatValue = t
                waitForIdle()
                snap("49_scene_${name}_$label")
            }
        }
    }

    /** The biggest picture (81 cells, rings and marks) over a whole loop: a frame has to stay far below the 16 ms of a screen that refreshes at 60 Hz, even in the slow software drawing of a test. */
    @Test
    fun aFrameOfTheBiggestPictureIsCheap() = runComposeUiTest {
        val time = mutableFloatStateOf(0f)
        setContent {
            BrainScrollTheme {
                Box(Modifier.requiredSize(300.dp)) { DemoSceneView(SudokuPlaceScene, time) }
            }
        }
        waitForIdle()
        val frames = 240
        val start = System.nanoTime()
        for (i in 0 until frames) {
            time.floatValue = i / frames.toFloat()
            waitForIdle()
        }
        val perFrameMs = (System.nanoTime() - start) / 1_000_000.0 / frames
        println("SCENE frame ms $perFrameMs")
        assertTrue(perFrameMs < 40.0, "a frame took $perFrameMs ms")
    }
}
