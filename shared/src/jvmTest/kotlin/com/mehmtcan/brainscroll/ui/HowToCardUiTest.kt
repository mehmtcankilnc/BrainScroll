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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The How to Play card, through the real screen, on the arrow escape page. */
@OptIn(ExperimentalTestApi::class)
class HowToCardUiTest {

    private fun ComposeUiTest.openCard(reduceMotion: Boolean? = null) {
        start(reduceMotion = reduceMotion, games = listOf(GameKind.Word, GameKind.Arrows))
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("arrowsCell").fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithTag("howToButton")[0].performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.cardShown() = onAllNodesWithTag("howToCard").fetchSemanticsNodes().isNotEmpty()

    private fun ComposeUiTest.dots() = onNodeWithTag("howToDots").fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString().orEmpty()

    private fun ComposeUiTest.disabled(tag: String) = onNodeWithTag(tag).fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)

    @Test
    fun theCardOpensOnTheFirstStepWithThePictureAndTheBackButtonOff() = runComposeUiTest {
        openCard()
        assertTrue(cardShown())
        assertTrue(hasAnyText("NASIL OYNANIR", "HOW TO PLAY"))
        assertTrue(hasAnyText("Bir oka dokun, tahtadan dışarı kaysın.", "Tap an arrow to slide it off the board."))
        assertTrue(dots().contains("1") && dots().contains("3"), dots())
        assertTrue(disabled("howToPrevious"))
        assertFalse(disabled("howToNext"))
        snap("350_howto_card_step1_start")
        mainClock.advanceTimeBy(1_500)
        snap("351_howto_card_step1_hand")
        mainClock.advanceTimeBy(900)
        snap("352_howto_card_step1_sliding")
    }

    @Test
    fun theButtonsGoThroughTheStepsAndTheLastOneHasNoNext() = runComposeUiTest {
        openCard()
        onNodeWithTag("howToNext").performClick()
        mainClock.advanceTimeBy(1_600)
        waitForIdle()
        assertTrue(dots().contains("2"), dots())
        assertTrue(hasAnyText("Önünün boş olduğundan emin ol. Başka bir oka çarparsa hata sayılır.", "Make sure its way is clear. If it hits another arrow, that is a mistake."))
        snap("353_howto_card_step2")
        onNodeWithTag("howToNext").performClick()
        mainClock.advanceTimeBy(2_500)
        waitForIdle()
        assertTrue(dots().contains("3"), dots())
        assertTrue(disabled("howToNext"))
        assertFalse(disabled("howToPrevious"))
        snap("354_howto_card_step3")
        mainClock.advanceTimeBy(2_500)
        snap("355_howto_card_step3_later")
        onNodeWithTag("howToPrevious").performClick()
        waitForIdle()
        assertTrue(dots().contains("2"), dots())
    }

    @Test
    fun okClosesTheCardAndTheBoardIsThereAgain() = runComposeUiTest {
        openCard()
        onNodeWithTag("howToOk").performClick()
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertFalse(cardShown())
        assertTrue(onAllNodesWithTag("arrowsCell").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun withReducedMotionTheCardShowsAStillFrame() = runComposeUiTest {
        openCard(reduceMotion = true)
        assertTrue(cardShown())
        snap("356_howto_card_reduced")
        assertTrue(dots().contains("3"), dots())
    }

    /** The pictures at chosen times of their loops, to look at (the loop itself runs on the clock of the screen). */
    @Test
    fun thePicturesAtChosenTimes() = runComposeUiTest {
        val game = androidx.compose.runtime.mutableIntStateOf(19)
        val step = androidx.compose.runtime.mutableIntStateOf(0)
        val time = androidx.compose.runtime.mutableFloatStateOf(0f)
        setContent {
            com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme {
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier.size(320.dp).background(com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme.colors.bgPage),
                ) {
                    when (game.intValue) {
                        0 -> com.mehmtcan.brainscroll.ui.howto.ArrowsDemo(step.intValue, time.floatValue)
                        1 -> com.mehmtcan.brainscroll.ui.howto.HanoiDemo(step.intValue, time.floatValue)
                        2 -> com.mehmtcan.brainscroll.ui.howto.TentsDemo(step.intValue, time.floatValue)
                        3 -> com.mehmtcan.brainscroll.ui.howto.QueensDemo(step.intValue, time.floatValue)
                        4 -> com.mehmtcan.brainscroll.ui.howto.BlockOutDemo(step.intValue, time.floatValue)
                        5 -> com.mehmtcan.brainscroll.ui.howto.NonogramDemo(step.intValue, time.floatValue)
                        6 -> com.mehmtcan.brainscroll.ui.howto.SudokuDemo(false, step.intValue, time.floatValue)
                        7 -> com.mehmtcan.brainscroll.ui.howto.SudokuDemo(true, step.intValue, time.floatValue)
                        8 -> com.mehmtcan.brainscroll.ui.howto.PipesDemo(step.intValue, time.floatValue)
                        9 -> com.mehmtcan.brainscroll.ui.howto.LightsDemo(step.intValue, time.floatValue)
                        10 -> com.mehmtcan.brainscroll.ui.howto.SlideDemo(step.intValue, time.floatValue)
                        11 -> com.mehmtcan.brainscroll.ui.howto.TargetDemo(step.intValue, time.floatValue)
                        12 -> com.mehmtcan.brainscroll.ui.howto.TubesDemo(step.intValue, time.floatValue)
                        13 -> com.mehmtcan.brainscroll.ui.howto.MinesDemo(step.intValue, time.floatValue)
                        14 -> com.mehmtcan.brainscroll.ui.howto.MemoryDemo(step.intValue, time.floatValue)
                        15 -> androidx.compose.runtime.CompositionLocalProvider(
                            com.mehmtcan.brainscroll.ui.howto.LocalHowToWordLanguage provides com.mehmtcan.brainscroll.game.wordle.Language.TR,
                        ) { com.mehmtcan.brainscroll.ui.howto.WordDemo(step.intValue, time.floatValue) }
                        16 -> com.mehmtcan.brainscroll.ui.howto.LineDemo(step.intValue, time.floatValue)
                        17 -> com.mehmtcan.brainscroll.ui.howto.AxisSumDemo(step.intValue, time.floatValue)
                        18 -> com.mehmtcan.brainscroll.ui.howto.BallSlideDemo(step.intValue, time.floatValue)
                        else -> com.mehmtcan.brainscroll.ui.howto.ColorFlowDemo(step.intValue, time.floatValue)
                    }
                }
            }
        }
        val frames = listOf(0 to listOf(0f, 0.3f, 0.5f, 0.7f), 1 to listOf(0.3f, 0.5f, 0.6f, 0.8f), 2 to listOf(0.1f, 0.3f, 0.55f, 0.85f))
        for (g in 19 until 20) for ((s, times) in frames) {
            game.intValue = g
            step.intValue = s
            for (t in times) {
                time.floatValue = t
                waitForIdle()
                snap("demo_${g}_${s}_${(t * 100).toInt()}")
            }
        }
    }

    @Test
    fun theCardFitsAShortScreenWithBigText() = runComposeUiTest {
        start(games = listOf(GameKind.Word, GameKind.Arrows), fontScale = 1.5f, screen = 360 to 600)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("arrowsCell").fetchSemanticsNodes().isNotEmpty() }
        onAllNodesWithTag("howToButton")[0].performClick()
        waitForIdle()
        snap("357_howto_card_small")
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        for (tag in listOf("howToDemo", "howToOk", "howToPrevious", "howToNext", "howToDots")) {
            val b = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertTrue(b.left >= root.left - 1f && b.right <= root.right + 1f && b.top >= root.top - 1f && b.bottom <= root.bottom + 1f, "$tag is inside the screen: $b against $root")
        }
    }
}
