package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.home.gameMatchesQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The feed editor's "select all" and its search by name and by what a game is about. */
@OptIn(ExperimentalTestApi::class)
class EditorSearchUiTest {

    private val feedGames = GameKind.entries - GameKind.Line

    private fun ComposeUiTest.openEditor() {
        start(games = feedGames, openFeed = false)
        clickAny("Akışın", "Your feed")
        waitForIdle()
    }

    private fun ComposeUiTest.cards() = onAllNodesWithTag("gameCard").fetchSemanticsNodes().size

    private fun ComposeUiTest.selectedText(n: Int) = hasAnyText("$n oyun seçili", "$n games selected", "$n game selected")

    @Test
    fun onlyTheRowsOnTheScreenAreBuiltAndTheListScrollsToTheLastGame() = runComposeUiTest {
        openEditor()
        assertTrue(cards() in 3 until feedGames.size, "not every card is built at once: ${cards()}")
        val rows = (feedGames.size + 2) / 3
        onNodeWithTag("editorList").performScrollToIndex(rows - 1)
        waitForIdle()
        snap("430_editor_end")
        assertTrue(cards() >= 1)
    }

    @Test
    fun selectAllTurnsOnEveryGameOnceAndThenItIsDimmed() = runComposeUiTest {
        openEditor()
        assertTrue(selectedText(feedGames.size))
        onNodeWithTag("editorSelectAll").assertIsNotEnabled()

        // Two games off (they are at the top of the list), then select all brings them back.
        val word = if (hasAnyText("Kelime bulmacası")) "Kelime bulmacası" else "Word puzzle"
        val memory = if (hasAnyText("Hafıza eşleştirme")) "Hafıza eşleştirme" else "Memory match"
        onAllNodesWithText(word).onLast().performClick()
        waitForIdle()
        onAllNodesWithText(memory).onLast().performClick()
        waitForIdle()
        assertTrue(selectedText(feedGames.size - 2))
        onNodeWithTag("editorSelectAll").performClick()
        waitForIdle()
        assertTrue(selectedText(feedGames.size))
        onNodeWithTag("editorSelectAll").assertIsNotEnabled()
        // Pressing it again does nothing: there is no way to turn everything off with it.
        onNodeWithTag("editorSelectAll").performClick()
        waitForIdle()
        assertTrue(selectedText(feedGames.size))
    }

    @Test
    fun searchingForColourShowsTheGamesAboutColoursAndNothingElse() = runComposeUiTest {
        openEditor()
        onNodeWithTag("editorSearchField").performTextInput("renk")
        waitForIdle()
        snap("431_editor_search_renk")
        val expected = feedGames.count { gameMatchesQuery(it, "", "renk") }
        assertEquals(3, expected, "tubes, colour flow and code breaker")
        assertEquals(expected, cards())
    }

    @Test
    fun aSearchThatFindsNothingSaysSo() = runComposeUiTest {
        openEditor()
        onNodeWithTag("editorSearchField").performTextInput("zzzzqq")
        waitForIdle()
        assertEquals(0, cards())
        assertTrue(onAllNodesWithTag("editorNoMatch").fetchSemanticsNodes().isNotEmpty())
        snap("432_editor_search_none")
    }

    @Test
    fun selectAllWhileSearchingTurnsOnTheFoundGamesAndClosingTheSearchBringsBackTheList() = runComposeUiTest {
        openEditor()
        // Switch the two games at the top off, then look for only those and select them.
        val word = if (hasAnyText("Kelime bulmacası")) "Kelime bulmacası" else "Word puzzle"
        onAllNodesWithText(word).onLast().performClick()
        waitForIdle()
        assertTrue(selectedText(feedGames.size - 1))
        onNodeWithTag("editorSearchField").performTextInput("wordle")
        waitForIdle()
        assertEquals(1, cards())
        onNodeWithTag("editorSelectAll").performClick()
        waitForIdle()
        assertTrue(selectedText(feedGames.size))
        // Clearing the search brings the whole list back.
        onNodeWithTag("editorSearchClear").performClick()
        waitForIdle()
        assertTrue(cards() in 3 until feedGames.size)
    }
}
