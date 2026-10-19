package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.runtime.mutableStateOf
import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.hanoi.HanoiSolver
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.ui.feed.ProfileState
import com.mehmtcan.brainscroll.ui.hanoi.HanoiTowerDisks
import com.mehmtcan.brainscroll.ui.profile.FavoritesPage
import com.mehmtcan.brainscroll.ui.profile.StatsPage
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The level filter of the statistics and the level on a favorite. */
@OptIn(ExperimentalTestApi::class)
class DifficultyStatsUiTest {

    private fun profile(level: Difficulty = Difficulty.Medium, favorites: List<FinishedRound> = emptyList()) =
        ProfileState(stats = com.mehmtcan.brainscroll.stats.computeStats(emptyList()), favorites = favorites, statsLevel = level)

    private fun hardSudoku() = FinishedRound(
        id = "s", mode = Mode.ENDLESS, language = Language.EN, answer = "9x9x36:7", guesses = listOf("1"),
        outcome = Outcome.WON, finishedAt = 5_000L, durationMs = 61_000L, game = GameKind.Sudoku, difficulty = Difficulty.Hard,
    )

    @Test
    fun theLevelChoiceShowsOnTheStatisticsPageOnlyWhenAGameHasLevels() = runComposeUiTest {
        setContent { BrainScrollTheme { StatsPage(profile(), showLevels = false) } }
        assertEquals(0, onAllNodesWithTag("statsLevel").fetchSemanticsNodes().size)
    }

    @Test
    fun choosingALevelTellsTheViewModel() = runComposeUiTest {
        val chosen = mutableStateOf<Difficulty?>(null)
        setContent { BrainScrollTheme { StatsPage(profile(), showLevels = true, onLevel = { chosen.value = it }) } }
        assertEquals(1, onAllNodesWithTag("statsLevel").fetchSemanticsNodes().size)
        assertTrue(hasAnyText("Kolay", "Easy") && hasAnyText("Orta", "Medium") && hasAnyText("Zor", "Hard"))
        onAllNodesWithText(if (hasAnyText("Zor")) "Zor" else "Hard")[0].performClick()
        waitForIdle()
        assertEquals(Difficulty.Hard, chosen.value)
    }

    @Test
    fun aFavoriteOfAHardBoardNamesTheLevelAndAMediumOneDoesNot() = runComposeUiTest {
        val medium = hardSudoku().copy(id = "m", difficulty = Difficulty.Medium)
        setContent { BrainScrollTheme { FavoritesPage(profile(favorites = listOf(hardSudoku(), medium))) } }
        assertTrue(hasAnyText("Sudoku · Zor", "Sudoku · Hard"), "the hard one says so")
        assertEquals(1, onAllNodesWithText("Sudoku", substring = true).fetchSemanticsNodes().count { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.joinToString().orEmpty().let { it.contains("·") }
        })
    }

    // --- Through the app: a hard Hanoi win lands in the hard statistics ----------------------------------------------

    private fun ComposeUiTest.hanoiTowers(): List<List<Int>> =
        onAllNodesWithTag("hanoiTower").fetchSemanticsNodes().take(3).map { it.config[HanoiTowerDisks] }

    @Test
    fun aWinAtAHardLevelCountsOnlyInTheHardStatistics() = runComposeUiTest {
        start(games = listOf(GameKind.Hanoi), openFeed = false, levelledGames = setOf(GameKind.Hanoi))
        clickAny("Akışın", "Your feed")
        waitForIdle()
        onNodeWithTag("editorNext").performClick()
        waitForIdle()
        onNodeWithTag("level_Hard").performClick()
        waitForIdle()
        onNodeWithTag("startFeed").performClick()
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithTag("hanoiTower").fetchSemanticsNodes().size >= 3 }

        val pegs = IntArray(hanoiTowers().sumOf { it.size })
        hanoiTowers().forEachIndexed { tower, disks -> disks.forEach { pegs[it - 1] = tower } }
        val goal = (0 until 3).first { tower ->
            onAllNodesWithTag("hanoiTower").fetchSemanticsNodes()[tower].config[SemanticsProperties.ContentDescription].joinToString()
                .let { it.contains("goal", ignoreCase = true) || it.contains("hedef", ignoreCase = true) }
        }
        for (move in HanoiSolver.shortest(pegs, goal)) {
            onAllNodesWithTag("hanoiTower")[move.from].performClick()
            mainClock.advanceTimeBy(700)
            waitForIdle()
            onAllNodesWithTag("hanoiTower")[move.to].performClick()
            mainClock.advanceTimeBy(700)
            waitForIdle()
        }
        mainClock.advanceTimeBy(1_500)
        waitForIdle()

        openTab("Profil", "Profile")
        clickAny("İstatistikler", "Statistics")
        waitForIdle()
        // Medium is chosen first: the hard win is not in it.
        assertEquals(1, onAllNodesWithTag("statsLevel").fetchSemanticsNodes().size)
        assertEquals("0", playedOf("statsHanoi"), "nothing at the medium level")
        clickAny("Zor", "Hard")
        waitForIdle()
        assertEquals("1", playedOf("statsHanoi"), "the hard win is here")
    }

    /** How many were played, as a statistics group shows it next to its title. */
    private fun ComposeUiTest.playedOf(tag: String): String {
        val texts = mutableListOf<String>()
        fun collect(node: androidx.compose.ui.semantics.SemanticsNode) {
            node.config.getOrNull(SemanticsProperties.Text)?.let { texts += it.joinToString() }
            node.children.forEach(::collect)
        }
        collect(onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode())
        return texts[1]
    }
}
