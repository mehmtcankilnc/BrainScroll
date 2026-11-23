package com.mehmtcan.brainscroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.howto.BallSlideDemo
import com.mehmtcan.brainscroll.ui.howto.CodeBreakerDemo
import com.mehmtcan.brainscroll.ui.howto.ColorFlowDemo
import com.mehmtcan.brainscroll.ui.howto.ColorSeqDemo
import com.mehmtcan.brainscroll.ui.howto.CubeFoldDemo
import com.mehmtcan.brainscroll.ui.howto.FruitMathDemo
import com.mehmtcan.brainscroll.ui.howto.HanoiDemo
import com.mehmtcan.brainscroll.ui.howto.LampsDemo
import com.mehmtcan.brainscroll.ui.howto.LineDemo
import com.mehmtcan.brainscroll.ui.howto.MatchsticksDemo
import com.mehmtcan.brainscroll.ui.howto.TubesDemo
import com.mehmtcan.brainscroll.ui.howto.UntangleDemo
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.test.Test

/** The two last steps of the games whose pictures are the real drawings of the game (a how to step has its own picture): looked at in the screenshots at three moments of the loop. */
@OptIn(ExperimentalTestApi::class)
class HowToDemoStepsTest {

    private val demos: List<Pair<String, @Composable (Int, Float) -> Unit>> = listOf(
        "hanoi" to { s, t -> HanoiDemo(s, t) },
        "tubes" to { s, t -> TubesDemo(s, t) },
        "ballslide" to { s, t -> BallSlideDemo(s, t) },
        "colorflow" to { s, t -> ColorFlowDemo(s, t) },
        "matchsticks" to { s, t -> MatchsticksDemo(s, t) },
        "untangle" to { s, t -> UntangleDemo(s, t) },
        "codebreaker" to { s, t -> CodeBreakerDemo(s, t) },
        "fruitmath" to { s, t -> FruitMathDemo(s, t) },
        "line" to { s, t -> LineDemo(s, t) },
        "lamps" to { s, t -> LampsDemo(s, t) },
        "colorseq" to { s, t -> ColorSeqDemo(s, t) },
        "cubefold" to { s, t -> CubeFoldDemo(s, t) },
    )

    @Test
    fun theLastTwoStepsAreDrawnAtThreeMoments() = runComposeUiTest {
        val which = mutableIntStateOf(0)
        val step = mutableIntStateOf(3)
        val time = mutableFloatStateOf(0.3f)
        setContent {
            BrainScrollTheme {
                Box(Modifier.requiredSize(300.dp).background(androidx.compose.ui.graphics.Color(0xFF111111))) { demos[which.intValue].second(step.intValue, time.floatValue) }
            }
        }
        for ((i, demo) in demos.withIndex()) {
            which.intValue = i
            for (s in 3..4) {
                step.intValue = s
                for ((label, t) in listOf("a" to 0.25f, "b" to 0.55f, "c" to 0.9f)) {
                    time.floatValue = t
                    waitForIdle()
                    snap("50_${demo.first}_${s}_$label")
                }
            }
        }
    }
}
