package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import kotlin.test.Test
import kotlin.test.assertTrue

/** Every game on a short, narrow screen with big text: the board must still be there and be usable, nothing squeezed to a sliver. */
@OptIn(ExperimentalTestApi::class)
class SmallScreenUiTest {

    private fun shot(kind: GameKind, name: String) = runComposeUiTest {
        start(games = listOf(kind), openFeed = false, fontScale = 1.3f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("300_small_$name")
    }

    @Test fun memory() = shot(GameKind.Memory, "memory")
    @Test fun tubes() = shot(GameKind.Tubes, "tubes")
    @Test fun mines() = shot(GameKind.Mines, "mines")
    @Test fun sudoku() = shot(GameKind.Sudoku, "sudoku")
    @Test fun lights() = shot(GameKind.Lights, "lights")
    @Test fun slide() = shot(GameKind.Slide, "slide")
    @Test fun target() = shot(GameKind.Target, "target")
    @Test fun pipes() = shot(GameKind.Pipes, "pipes")
    @Test fun hanoi() = shot(GameKind.Hanoi, "hanoi")
    @Test fun queens() = shot(GameKind.Queens, "queens")
    @Test fun tents() = shot(GameKind.Tents, "tents")
    @Test fun jigsaw() = shot(GameKind.Jigsaw, "jigsaw")
    @Test fun blockout() = shot(GameKind.BlockOut, "blockout")
    @Test fun arrows() = shot(GameKind.Arrows, "arrows")
    @Test fun nonogram() = shot(GameKind.Nonogram, "nonogram")
    @Test fun topicgroups() = shot(GameKind.TopicGroups, "topicgroups")

    /** The towers and the restart button are both on the short screen with big text, and nothing runs off its edge. */
    @Test
    fun hanoiFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Hanoi), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("300_small_hanoi_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("hanoiTower", "hanoiRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }

    /** The lone number board and the restart button are both on the short screen with big text, and no cell runs off the screen. */
    @Test
    fun loneNumberFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.LoneNumber), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("302_small_lonenumber_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        val cells = onAllNodesWithTag("loneCell").fetchSemanticsNodes()
        assertTrue(cells.isNotEmpty(), "the board is there")
        for (tag in listOf("loneCell", "loneRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        // A cell is big enough to touch: at least 24 pixels on this short screen with big text.
        assertTrue(cells.first().boundsInRoot.width >= 24f, "cell width ${cells.first().boundsInRoot.width}")
    }

    /** The zeros and ones board and the restart button are both on the short screen with big text, and no cell runs off the screen. */
    @Test
    fun zerosOnesFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.ZerosOnes), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("303_small_zerosones_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        val cells = onAllNodesWithTag("binCell").fetchSemanticsNodes()
        assertTrue(cells.isNotEmpty(), "the board is there")
        for (tag in listOf("binCell", "binRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        assertTrue(cells.first().boundsInRoot.width >= 20f, "cell width ${cells.first().boundsInRoot.width}")
    }

    /** The lamps board and the restart button are both on the short screen with big text, and no cell runs off the screen. */
    @Test
    fun lampsFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Lamps), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("304_small_lamps_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        val cells = onAllNodesWithTag("lampCell").fetchSemanticsNodes()
        assertTrue(cells.isNotEmpty(), "the board is there")
        for (tag in listOf("lampCell", "lampRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        assertTrue(cells.first().boundsInRoot.width >= 20f, "cell width ${cells.first().boundsInRoot.width}")
    }

    /** The greater and less board (the biggest one has 6 cells a side and signs between them) and the restart button fit the short screen with big text. */
    @Test
    fun greaterLessFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.GreaterLess), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("305_small_greaterless_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        val cells = onAllNodesWithTag("glCell").fetchSemanticsNodes()
        assertTrue(cells.isNotEmpty(), "the board is there")
        for (tag in listOf("glCell", "glKey", "glRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        assertTrue(cells.first().boundsInRoot.width >= 20f, "cell width ${cells.first().boundsInRoot.width}")
    }

    /** The fences board (the biggest one has 8 by 8 squares, with the edges to tap) and the restart button fit the short screen with big text. */
    @Test
    fun fencesFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Fences), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("306_small_fences_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        val edges = onAllNodesWithTag("fenceEdge").fetchSemanticsNodes()
        assertTrue(edges.isNotEmpty(), "the board is there")
        for (tag in listOf("fenceEdge", "fenceRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        assertTrue(edges.maxOf { maxOf(it.boundsInRoot.width, it.boundsInRoot.height) } >= 20f, "an edge target is long enough to hit")
    }

    /** The mirror board and the restart button fit the short screen with big text. */
    @Test
    fun mirrorFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Mirror), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("307_small_mirror_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        val cells = onAllNodesWithTag("mirrorCell").fetchSemanticsNodes()
        assertTrue(cells.isNotEmpty(), "the board is there")
        for (tag in listOf("mirrorCell", "mirrorRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        assertTrue(cells.first().boundsInRoot.width >= 28f, "cell width ${cells.first().boundsInRoot.width}")
    }

    /** The cross sums board (the biggest has 6 by 6 cells and a frame), the nine keys and the restart button fit the short screen with big text. */
    @Test
    fun crossSumFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.CrossSum), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("309_small_crosssum_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        val cells = onAllNodesWithTag("crossCell").fetchSemanticsNodes()
        assertTrue(cells.isNotEmpty(), "the board is there")
        for (tag in listOf("crossCell", "crossKey", "crossRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        assertTrue(cells.first().boundsInRoot.width >= 24f, "cell width ${cells.first().boundsInRoot.width}")
    }

    /** The coloured buttons of the color sequence, the message and the result card fit the short screen with big text, each button big enough to hit. */
    @Test
    fun colorSeqFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.ColorSeq), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("310_small_colorseq_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        fun inside(tag: String) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        inside("colorBoard")
        inside("colorStart")
        inside("colorMessage")
        inside("colorRestart")
        val first = onAllNodesWithTag("colorButton0").fetchSemanticsNodes().first().boundsInRoot
        assertTrue(first.width >= 56f && first.height >= 56f, "button size ${first.width} x ${first.height}")
        onAllNodesWithTag("colorStart")[0].performClick()
        mainClock.advanceTimeBy(100)
        waitForIdle()
        repeat(60) { mainClock.advanceTimeBy(100); waitForIdle() }
        val sequence = onAllNodesWithTag("colorBoard").fetchSemanticsNodes().first().config[com.mehmtcan.brainscroll.ui.colorseq.ColorSeqSequenceKey]
        val wrong = (0 until 4).first { it != sequence[0] }
        onAllNodesWithTag("colorButton$wrong")[0].performClick()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("311_small_colorseq_lost")
        inside("colorResultCard")
        val boardBottom = onAllNodesWithTag("colorBoard").fetchSemanticsNodes().first().boundsInRoot.bottom
        val cardTop = onAllNodesWithTag("colorResultCard").fetchSemanticsNodes().first().boundsInRoot.top
        assertTrue(boardBottom <= cardTop + 1f, "the buttons and the result card do not overlap: $boardBottom, $cardTop")
    }

    /** The net and the four cubes of cube folding, and the result card, fit the short screen with big text, each cube big enough to hit. */
    @Test
    fun cubeFoldFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.CubeFold), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("312_small_cubefold_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        fun inside(tag: String) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        inside("cubeNet")
        inside("cubeOption")
        val cubes = onAllNodesWithTag("cubeOption").fetchSemanticsNodes().take(4).map { it.boundsInRoot }
        assertTrue(cubes.all { it.width >= 64f && it.height >= 64f }, "cube size ${cubes.first().width} x ${cubes.first().height}")
        val net = onAllNodesWithTag("cubeNet").fetchSemanticsNodes().first().boundsInRoot
        assertTrue(cubes.none { it.overlaps(net) }, "the net and the cubes do not overlap")
        val wrong = onAllNodesWithTag("cubeOption").fetchSemanticsNodes().indexOfFirst { !it.config[com.mehmtcan.brainscroll.ui.cubefold.CubeCorrectKey] }
        onAllNodesWithTag("cubeOption")[wrong].performClick()
        mainClock.advanceTimeBy(2_500)
        waitForIdle()
        snap("313_small_cubefold_lost")
        inside("cubeResultCard")
        val cardTop = onAllNodesWithTag("cubeResultCard").fetchSemanticsNodes().first().boundsInRoot.top
        assertTrue(onAllNodesWithTag("cubeOption").fetchSemanticsNodes().first().boundsInRoot.bottom <= cardTop + 1f, "the cubes and the result card do not overlap")
    }

    /** The five numbers of odd one out fit the short screen with big text, each big enough to hit. */
    @Test
    fun oddOneFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.OddOne), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("308_small_oddone_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        val numbers = onAllNodesWithTag("oddNumber").fetchSemanticsNodes()
        assertTrue(numbers.size >= 5, "the numbers are there")
        for (node in numbers) {
            val b = node.boundsInRoot
            assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "a number is inside the screen: $b against $root")
        }
        assertTrue(numbers.first().boundsInRoot.width >= 40f, "number width ${numbers.first().boundsInRoot.width}")
    }

    /** The queens board and the restart button are both on the short screen with big text, and nothing runs off its edge. */
    @Test
    fun queensFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Queens), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("301_small_queens_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("queensCell", "queensRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }

    /** The block out board and the restart button are both on the short screen with big text, and nothing runs off its edge. */
    @Test
    fun blockOutFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.BlockOut), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("303_small_blockout_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("blockoutPiece", "blockoutCell", "blockoutRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }

    /** The tents board and the restart button are both on the short screen with big text, and nothing runs off its edge. */
    @Test
    fun tentsFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Tents), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("302_small_tents_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("tentsCell", "tentsRestart", "tentsRowClue", "tentsColumnClue")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }

    /** The arrow board and the restart button are both on the short screen with big text, and nothing runs off its edge. */
    @Test
    fun arrowsFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Arrows), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("303_small_arrows_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("arrowsCell", "arrowsRestart")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }


    /** The nonogram board, its numbers and the restart button are all on the short screen with big text, and nothing runs off its edge. */
    @Test
    fun nonogramFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Nonogram), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("304_small_nonogram_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("nonogramCell", "nonogramRestart", "nonogramRowClue", "nonogramColumnClue")) {
            for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
    }

    @Test fun word() = shot(GameKind.Word, "word")

    @Test
    fun theRulesCardFitsAShortScreenWithBigText() = runComposeUiTest {
        start(fontScale = 1.5f, screen = 360 to 600)
        onAllNodesWithTag("howToButton")[0].performClick()
        waitForIdle()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        snap("301_small_howto")
        // The card and its OK button are on screen, whatever the text size.
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        val button = onNodeWithTag("howToOk").fetchSemanticsNode().boundsInRoot
        assertTrue(button.bottom <= root.bottom + 1f && button.top >= 0f, "the OK button of the card is in view")
    }

    /** The sixteen words and the two buttons are all on the short screen with big text, and nothing runs off its edge. */
    @Test
    fun topicGroupsFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.TopicGroups), openFeed = false, fontScale = 1.5f, screen = 360 to 600)
        openTab("Akış", "Feed")
        waitForIdle()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        snap("304_small_topicgroups_big_text")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("topicTile", "topicShuffle", "topicClear")) {
            val nodes = onAllNodesWithTag(tag).fetchSemanticsNodes().filter { it.boundsInRoot.width > 0f && it.boundsInRoot.top >= 0f }
            assertTrue(nodes.isNotEmpty(), tag)
            for (node in nodes) {
                val b = node.boundsInRoot
                assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
            }
        }
        // A tile is still big enough to hit.
        val tile = onAllNodesWithTag("topicTile").fetchSemanticsNodes().first { it.boundsInRoot.width > 0f && it.boundsInRoot.top >= 0f }.boundsInRoot
        assertTrue(tile.height >= 40f && tile.width >= 40f, "a tile is ${tile.width} x ${tile.height}")
    }
}
