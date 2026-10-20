package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The level choice in the feed editor and the badge of the level on the page. */
@OptIn(ExperimentalTestApi::class)
class DifficultyUiTest {

    private val games = listOf(GameKind.Word, GameKind.Hanoi)

    private fun ComposeUiTest.openEditor(levelled: Set<GameKind>) {
        start(games = games, openFeed = false, levelledGames = levelled)
        clickAny("Akışın", "Your feed")
        waitForIdle()
    }

    private fun ComposeUiTest.isSelected(tag: String) =
        onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected) == true

    @Test
    fun theLevelStepShowsOnlyWhenAGameHasLevels() = runComposeUiTest {
        openEditor(levelled = emptySet())
        assertEquals(0, onAllNodesWithTag("editorNext").fetchSemanticsNodes().size)
        assertEquals(1, onAllNodesWithTag("startFeed").fetchSemanticsNodes().size)
    }

    @Test
    fun aGameWithLevelsShowsThreeChoicesWithMediumChosen() = runComposeUiTest {
        openEditor(levelled = setOf(GameKind.Hanoi))
        onNodeWithTag("editorNext").performClick()
        waitForIdle()
        snap("330_editor_levels")
        assertEquals(1, onAllNodesWithTag("difficultySlider").fetchSemanticsNodes().size)
        assertTrue(hasAnyText("Kolay", "Easy"))
        assertTrue(hasAnyText("Orta", "Medium"))
        assertTrue(hasAnyText("Zor", "Hard"))
        assertTrue(isSelected("level_Medium"))
        assertFalse(isSelected("level_Hard"))
    }

    @Test
    fun aShortNoteSaysTheLevelIsForEveryGameNotOne() = runComposeUiTest {
        openEditor(levelled = setOf(GameKind.Hanoi))
        onNodeWithTag("editorNext").performClick()
        waitForIdle()
        assertEquals(1, onAllNodesWithTag("difficultyNote").fetchSemanticsNodes().size)
        assertTrue(hasAnyText("tüm oyunlarını etkiler", "applies to all your games"))
    }

    @Test
    fun choosingALevelSelectsItAndTheChoiceIsRemembered() = runComposeUiTest {
        openEditor(levelled = setOf(GameKind.Hanoi))
        onNodeWithTag("editorNext").performClick()
        waitForIdle()
        onNodeWithTag("level_Hard").performClick()
        waitForIdle()
        assertTrue(isSelected("level_Hard"))
        assertFalse(isSelected("level_Medium"))
        onNodeWithTag("level_Easy").performClick()
        waitForIdle()
        assertTrue(isSelected("level_Easy"))
    }

    @Test
    fun theFeedPageShowsABadgeForEasyAndHardButNotMedium() = runComposeUiTest {
        start(games = listOf(GameKind.Hanoi), openFeed = false, levelledGames = setOf(GameKind.Hanoi))
        clickAny("Akışın", "Your feed")
        waitForIdle()
        onNodeWithTag("editorNext").performClick()
        waitForIdle()
        onNodeWithTag("level_Hard").performClick()
        waitForIdle()
        onNodeWithTag("startFeed").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("hanoiTower").fetchSemanticsNodes().size >= 3 }
        snap("331_hard_badge")
        assertTrue(hasAnyText("Zor", "Hard"), "the level shows next to the name of the game")
    }

    @Test
    fun aMediumPageHasNoBadge() = runComposeUiTest {
        start(games = listOf(GameKind.Hanoi), openFeed = false, levelledGames = setOf(GameKind.Hanoi))
        onNodeWithTag("startFeedHome").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("hanoiTower").fetchSemanticsNodes().size >= 3 }
        assertTrue(onAllNodesWithTag("languageBadge").fetchSemanticsNodes().isEmpty())
    }

}
