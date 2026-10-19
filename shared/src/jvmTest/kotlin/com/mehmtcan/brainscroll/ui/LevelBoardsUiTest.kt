package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.hanoi.HanoiTowerDisks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The boards of the levels on the screen: the right size, and the biggest of them fits a short screen with big text. */
@OptIn(ExperimentalTestApi::class)
class LevelBoardsUiTest {

    private fun ComposeUiTest.openAt(game: GameKind, level: String, tag: String, fontScale: Float = 1f, screen: Pair<Int, Int> = 390 to 740) {
        start(games = listOf(game), openFeed = false, levelledGames = setOf(game), fontScale = fontScale, screen = screen)
        clickAny("Akışın", "Your feed")
        waitForIdle()
        onNodeWithTag("editorNext").performClick()
        waitForIdle()
        onNodeWithTag("level_$level").performClick()
        waitForIdle()
        onNodeWithTag("startFeed").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
    }

    private fun ComposeUiTest.insideScreen(vararg tags: String) {
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in tags) for (node in onAllNodesWithTag(tag).fetchSemanticsNodes()) {
            val b = node.boundsInRoot
            if (b.width <= 0f) continue // a page that is composed ahead, below the screen
            assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
        }
    }

    private fun ComposeUiTest.disksOnTheFirstPage(): Int =
        onAllNodesWithTag("hanoiTower").fetchSemanticsNodes().take(3).sumOf { it.config[HanoiTowerDisks].size }

    private fun ComposeUiTest.cardsOnTheFirstPage(): Int {
        // The page ahead is composed too: the first page's cards are the ones that are on the screen.
        val cards = onAllNodesWithTag("memoryCard").fetchSemanticsNodes().filter { it.boundsInRoot.width > 0f && it.boundsInRoot.top >= 0f }
        return cards.size
    }

    @Test
    fun hanoiHasThreeFourOrFiveDisksByLevel() {
        for ((level, disks) in listOf("Easy" to 3, "Medium" to 4, "Hard" to 5)) runComposeUiTest {
            openAt(GameKind.Hanoi, level, "hanoiTower")
            assertEquals(disks, disksOnTheFirstPage(), level)
        }
    }

    @Test
    fun memoryHasEightTwelveOrSixteenCardsByLevel() {
        for ((level, cards) in listOf("Easy" to 8, "Medium" to 12, "Hard" to 16)) runComposeUiTest {
            openAt(GameKind.Memory, level, "memoryCard")
            snap("340_memory_$level")
            assertEquals(cards, cardsOnTheFirstPage(), level)
        }
    }

    @Test
    fun theHardHanoiFitsAShortScreenWithBigText() = runComposeUiTest {
        openAt(GameKind.Hanoi, "Hard", "hanoiTower", fontScale = 1.5f, screen = 360 to 600)
        snap("341_hanoi_hard_small")
        insideScreen("hanoiTower", "hanoiRestart")
    }

    @Test
    fun theHardMemoryFitsAShortScreenWithBigText() = runComposeUiTest {
        openAt(GameKind.Memory, "Hard", "memoryCard", fontScale = 1.5f, screen = 360 to 600)
        snap("342_memory_hard_small")
        insideScreen("memoryCard")
    }

    /** The cells of the first page on the screen, by tag (the page ahead is composed too, below the screen). */
    private fun ComposeUiTest.cellsOnScreen(tag: String) =
        onAllNodesWithTag(tag).fetchSemanticsNodes().count { it.boundsInRoot.width > 0f && it.boundsInRoot.top >= 0f && it.boundsInRoot.top < 600f }

    @Test
    fun theHardBoardOfEveryOtherGameFitsAShortScreenWithBigText() {
        // game, the tag of a cell, how many cells the hard board has
        val cases = listOf(
            Triple(GameKind.Mines, "mineCell", 81),
            Triple(GameKind.Slide, "slideTile", 15), // the empty cell has no tile
            Triple(GameKind.Lights, "lightCell", 36),
            Triple(GameKind.Queens, "queensCell", 49),
            Triple(GameKind.Arrows, "arrowsBoard", 1),
            Triple(GameKind.Pipes, "pipeTile", 25),
            Triple(GameKind.Tubes, "tube", 8),
            Triple(GameKind.Target, "targetNumber", 6),
        )
        for ((game, tag, cells) in cases) runComposeUiTest {
            openAt(game, "Hard", tag, fontScale = 1.5f, screen = 360 to 600)
            snap("343_hard_${game.id}_small")
            assertEquals(cells, cellsOnScreen(tag), "$game: cells of the hard board on the screen")
            insideScreen(tag)
        }
    }

    @Test
    fun theEasyBoardOfEveryOtherGameHasFewerCellsThanTheHardOne() {
        val cases = listOf(
            Triple(GameKind.Mines, "mineCell", 64),
            Triple(GameKind.Slide, "slideTile", 8),
            Triple(GameKind.Lights, "lightCell", 16),
            Triple(GameKind.Queens, "queensCell", 25),
            Triple(GameKind.Arrows, "arrowsBoard", 1),
            Triple(GameKind.Pipes, "pipeTile", 16),
            Triple(GameKind.Tubes, "tube", 6),
        )
        for ((game, tag, cells) in cases) runComposeUiTest {
            openAt(game, "Easy", tag)
            assertEquals(cells, cellsOnScreen(tag), "$game: cells of the easy board")
        }
    }

    @Test
    fun theHardBoardsOfSudokuTentsAndBlockOutFitAShortScreenWithBigText() {
        val cases = listOf(
            Triple(GameKind.Sudoku, "sudokuCell", 81),
            Triple(GameKind.Jigsaw, "sudokuCell", 81),
            Triple(GameKind.Tents, "tentsCell", 49),
            Triple(GameKind.BlockOut, "blockoutCell", 25),
        )
        for ((game, tag, cells) in cases) runComposeUiTest {
            openAt(game, "Hard", tag, fontScale = 1.5f, screen = 360 to 600)
            snap("344_hard_${game.id}_small")
            assertEquals(cells, cellsOnScreen(tag), "$game: cells of the hard board on the screen")
            insideScreen(tag)
        }
    }

    @Test
    fun theEasyTentsBoardIsFiveByFive() = runComposeUiTest {
        openAt(GameKind.Tents, "Easy", "tentsCell")
        assertEquals(25, cellsOnScreen("tentsCell"))
    }

    @Test
    fun theNonogramHasFiveEightOrTenCellsAcrossByLevelAndTheHardOneFitsAShortScreen() {
        for ((level, cells) in listOf("Easy" to 25, "Medium" to 64, "Hard" to 100)) runComposeUiTest {
            val small = level == "Hard"
            openAt(GameKind.Nonogram, level, "nonogramCell", fontScale = if (small) 1.5f else 1f, screen = if (small) 360 to 600 else 390 to 740)
            snap("345_nonogram_$level")
            assertEquals(cells, cellsOnScreen("nonogramCell"), level)
            if (small) insideScreen("nonogramCell", "nonogramRowClue", "nonogramColumnClue", "nonogramRestart")
        }
    }
}
