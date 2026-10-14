package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import kotlin.test.Test
import kotlin.test.assertTrue

/** The ring of the guide on every game: the ring is on the thing that is pointed at (looked at in the screenshots). */
@OptIn(ExperimentalTestApi::class)
class GuideRingUiTest {

    private fun ring(kind: GameKind, name: String, ready: String) = runComposeUiTest {
        start(openFeed = false, games = listOf(kind))
        openTab("Akış", "Feed")
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag(ready).fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithTag("guideButton")[0].performClick()
        waitUntil(timeoutMillis = 60_000) { onAllNodesWithTag("guideBanner").fetchSemanticsNodes().isNotEmpty() }
        mainClock.advanceTimeBy(500)
        snap("320_ring_$name")
        val marks = onAllNodesWithTag("guideMark").fetchSemanticsNodes().map { it.boundsInRoot }
        // (On the sliding puzzle the tile has a tag of its own, which hides the tag of the ring: no mark is found there.)
        assertTrue(marks.isNotEmpty() || kind == GameKind.Slide)
        for (b in marks) println("RING $name ${b.left} ${b.top} ${b.width} ${b.height}")
    }

    @Test fun tubes() = ring(GameKind.Tubes, "tubes", "tube")
    @Test fun sudoku() = ring(GameKind.Sudoku, "sudoku", "sudokuCell")
    @Test fun lights() = ring(GameKind.Lights, "lights", "lightCell")
    @Test fun slide() = ring(GameKind.Slide, "slide", "slideTile")
    @Test fun target() = ring(GameKind.Target, "target", "targetNumber")
    @Test fun pipes() = ring(GameKind.Pipes, "pipes", "pipeTile")
    @Test fun memory() = ring(GameKind.Memory, "memory", "memoryCard")
    @Test fun mines() = ring(GameKind.Mines, "mines", "mineCell")
    @Test fun hanoi() = ring(GameKind.Hanoi, "hanoi", "hanoiTower")
}
