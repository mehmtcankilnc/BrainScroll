package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_codebreaker_1
import brainscroll.shared.generated.resources.howto_card_codebreaker_2
import brainscroll.shared.generated.resources.howto_card_codebreaker_3
import com.mehmtcan.brainscroll.game.codebreaker.CodeMark
import com.mehmtcan.brainscroll.game.codebreaker.CodePuzzle
import com.mehmtcan.brainscroll.ui.codebreaker.Peg
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the code breaker card: fill a row with colours, the dots tell how it scored, the next try uses them. */
internal val CodeBreakerHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_codebreaker_1, { t -> CodeBreakerDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_codebreaker_2, { t -> CodeBreakerDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_codebreaker_3, { t -> CodeBreakerDemo(step = 2, t = t) }),
    ),
)

// The code is 0 3 2 1. First try 0 2 4 3: the first colour is right in its place, 2 and 3 are in the code but elsewhere, 4 is not in it.
private val Code = listOf(0, 3, 2, 1)
private val First = listOf(0, 2, 4, 3)

@Composable
internal fun CodeBreakerDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val fade = demoFade(t)
    val marks = CodePuzzle.feedback(Code, First).marks
    // Step 0: the pegs of the first try come one after the other. Step 1: they get their rings. Step 2: the second try is the code.
    val second = if (step >= 2) demoEase(demoSegment(t, 0.1f, 0.3f)) else 0f
    Box(modifier = Modifier.fillMaxSize().alpha(fade), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp).background(colors.bgSurface, RoundedCornerShape(16.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                First.forEachIndexed { i, c ->
                    val shown = if (step == 0) demoEase(demoSegment(t, 0.05f + 0.15f * i, 0.2f + 0.15f * i)) else 1f
                    val ring = if (step == 0) null else if (step == 1 && demoSegment(t, 0.2f + 0.1f * i, 0.3f + 0.1f * i) < 1f) null else marks[i]
                    Box(Modifier.alpha(shown)) { Peg(c, 44.dp, mark = ring ?: CodeMark.Absent) }
                }
            }
            Row(modifier = Modifier.alpha(second), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Code.forEach { Peg(it, 44.dp, mark = CodeMark.Exact) }
            }
        }
    }
}
