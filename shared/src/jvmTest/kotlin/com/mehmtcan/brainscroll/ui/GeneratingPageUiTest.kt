package com.mehmtcan.brainscroll.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.feed.GeneratingPage
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.test.Test
import kotlin.test.assertTrue
import androidx.compose.ui.test.onAllNodesWithTag

/** The skeleton of a page whose board is still being made, and a feed that makes its boards in the background. */
@OptIn(ExperimentalTestApi::class)
class GeneratingPageUiTest {

    @Test
    fun theSkeletonSaysThePuzzleIsBeingPreparedAndFitsAShortScreen() = runComposeUiTest {
        setContent {
            BrainScrollTheme {
                Box(Modifier.requiredSize(360.dp, 600.dp)) { GeneratingPage(GameKind.Fences) }
            }
        }
        mainClock.advanceTimeBy(400)
        waitForIdle()
        snap("470_generating_page")
        assertTrue(hasAnyText("Bulmaca hazırlanıyor…", "Preparing your puzzle…"))
        val page = onNodeWithTag("generatingPage").fetchSemanticsNode().boundsInRoot
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(page.left >= root.left - 1f && page.right <= root.right + 1f && page.bottom <= root.bottom + 1f, "inside the screen: $page against $root")
    }

    @Test
    fun theFeedShowsItsBoardsOnceTheyAreMadeInTheBackground() = runComposeUiTest {
        start(games = listOf(GameKind.Word, GameKind.Fences), buildInBackground = true)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        // The board comes from another thread: wait for it for real.
        waitUntil(timeoutMillis = 15_000) {
            mainClock.advanceTimeBy(100)
            onAllNodesWithTag("fenceEdge").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(onAllNodesWithTag("fenceEdge").fetchSemanticsNodes().isNotEmpty())
    }
}
