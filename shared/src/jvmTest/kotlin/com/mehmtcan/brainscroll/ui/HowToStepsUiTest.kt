package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The cards that have more than three steps, with the pictures that are made from a script, and the last card that gives the first move away. */
@OptIn(ExperimentalTestApi::class)
class HowToStepsUiTest {

    private fun ComposeUiTest.openCard(game: GameKind, ready: String, fontScale: Float = 1f, screen: Pair<Int, Int> = 390 to 740) {
        start(games = listOf(GameKind.Word, game), fontScale = fontScale, screen = screen)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag(ready).fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithTag("howToButton")[0].performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.dots() = onNodeWithTag("howToDots").fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString().orEmpty()

    /** Goes through every step of the open card: a picture at three moments of its loop, the label of the step, and that the card stays on the screen. */
    private fun ComposeUiTest.walk(name: String, steps: Int, spoilerAt: Int) {
        assertTrue(dots().contains(steps.toString()), "the card has $steps steps: ${dots()}")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (step in 1..steps) {
            mainClock.advanceTimeBy(300)
            snap("48${steps}_${name}_${step}_a")
            mainClock.advanceTimeBy(1_800)
            snap("48${steps}_${name}_${step}_b")
            mainClock.advanceTimeBy(1_800)
            val card = onNodeWithTag("howToCard").fetchSemanticsNode().boundsInRoot
            assertTrue(card.left >= root.left - 1f && card.right <= root.right + 1f && card.bottom <= root.bottom + 1f, "the card is on the screen at step $step")
            val ok = onNodeWithTag("howToOk").fetchSemanticsNode().boundsInRoot
            assertTrue(ok.bottom <= root.bottom + 1f && ok.top >= root.top - 1f, "the button is on the screen at step $step")
            assertEquals(step == spoilerAt, onAllNodesWithTag("howToSpoilerTitle").fetchSemanticsNodes().isNotEmpty(), "the spoiler label is only on the card that gives the move away (step $step)")
            if (step < steps) {
                onNodeWithTag("howToOk").performClick()
                mainClock.advanceTimeBy(400)
                waitForIdle()
            }
        }
    }

    @Test
    fun sudokuHasSixStepsAndTheLastOneIsTheSpoiler() = runComposeUiTest {
        openCard(GameKind.Sudoku, "sudokuCell")
        walk("sudoku", steps = 6, spoilerAt = 6)
    }

    @Test
    fun minesHasSevenStepsAndTheLastOneIsTheSpoiler() = runComposeUiTest {
        openCard(GameKind.Mines, "mineCell")
        walk("mines", steps = 7, spoilerAt = 7)
    }

    /** The grid games of the second slice: three cards as before, a card about how it is solved and the last one that gives the first move away. */
    @Test
    fun theDeductionGamesHaveFiveStepsAndTheLastOneIsTheSpoiler() = fiveSteps(
        GameKind.Jigsaw, GameKind.Nonogram, GameKind.Tents, GameKind.Queens, GameKind.LoneNumber, GameKind.ZerosOnes, GameKind.Lamps,
        GameKind.GreaterLess, GameKind.Fences, GameKind.Rectangles, GameKind.AxisSum, GameKind.Arrows, GameKind.Pipes,
    )

    /** The search and move games of the third slice. */
    @Test
    fun theSearchGamesHaveFiveStepsAndTheLastOneIsTheSpoiler() = fiveSteps(
        GameKind.Lights, GameKind.Slide, GameKind.Hanoi, GameKind.BlockOut, GameKind.Tubes, GameKind.Target, GameKind.BallSlide,
        GameKind.ColorFlow, GameKind.Matchsticks, GameKind.Lasers, GameKind.Untangle, GameKind.Mirror, GameKind.OddOne,
    )

    /** The last slice: the word games, memory and the rest (the daily one-line puzzle has no page in the feed; the content test below covers it). */
    @Test
    fun theLastGamesHaveFiveStepsAndTheLastOneIsTheSpoiler() = fiveSteps(
        GameKind.Word, GameKind.Memory, GameKind.FlashGrid, GameKind.TopicGroups, GameKind.FruitMath, GameKind.CodeBreaker, GameKind.ColorSeq,
    )

    private fun fiveSteps(vararg games: GameKind) = stepsOf(5, *games)

    /** Opens the card of every game from its page in the feed and walks it: [steps] steps, the last one the spoiler. */
    private fun stepsOf(steps: Int, vararg games: GameKind) {
        for (game in games) runComposeUiTest {
            start(games = listOf(GameKind.Word, game))
            swipeFeedUp()
            mainClock.advanceTimeBy(1_500)
            waitForIdle()
            val buttons = onAllNodesWithTag("howToButton")
            val current = (0 until buttons.fetchSemanticsNodes().size).maxBy { buttons[it].fetchSemanticsNode().boundsInRoot.top.coerceAtLeast(-1f) }
            buttons[current].performClick()
            waitForIdle()
            walk(game.name.lowercase(), steps = steps, spoilerAt = steps)
        }
    }

    /** Cross sums has a card of six steps: the goal, the keys, the rule, two techniques and the first step. */
    @Test
    fun crossSumHasSixStepsAndTheLastOneIsTheSpoiler() = stepsOf(6, GameKind.CrossSum)

    /** Cube folding has a card of six steps: the goal, the faces, the tap, two techniques and the first step. */
    @Test
    fun cubeFoldHasSixStepsAndTheLastOneIsTheSpoiler() = stepsOf(6, GameKind.CubeFold)

    @Test
    fun theLongestCardsFitAShortScreenWithBigText() = runComposeUiTest {
        openCard(GameKind.Sudoku, "sudokuCell", fontScale = 1.5f, screen = 360 to 600)
        walk("sudokusmall", steps = 6, spoilerAt = 6)
    }

    @Test
    fun theLabelOfAStepIsThereOnTheCardsThatTeachHowToSolve() = runComposeUiTest {
        openCard(GameKind.Sudoku, "sudokuCell")
        // Steps 1 to 3 have no label (the rule and how to tap), the next two say how to solve.
        repeat(3) {
            assertTrue(onAllNodesWithTag("howToTitle").fetchSemanticsNodes().isEmpty())
            onNodeWithTag("howToOk").performClick()
            mainClock.advanceTimeBy(400)
            waitForIdle()
        }
        assertTrue(onAllNodesWithTag("howToTitle").fetchSemanticsNodes().isNotEmpty())
        assertTrue(hasAnyText("NASIL ÇÖZÜLÜR", "HOW TO SOLVE"))
    }
}
