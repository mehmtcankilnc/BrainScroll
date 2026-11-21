package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_fruitmath_1
import brainscroll.shared.generated.resources.howto_card_fruitmath_2
import brainscroll.shared.generated.resources.howto_card_fruitmath_3
import com.mehmtcan.brainscroll.game.fruitmath.FruitLine
import com.mehmtcan.brainscroll.game.fruitmath.FruitToken
import com.mehmtcan.brainscroll.ui.fruitmath.FruitRow
import com.mehmtcan.brainscroll.ui.fruitmath.fruitSizes
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The three steps of the fruit equations card: the first line, the next line holds the fruit before it, the last line is asked. */
internal val FruitMathHowTo = HowToCardContent(
    listOf(
        HowToStep(Res.string.howto_card_fruitmath_1, { t -> FruitMathDemo(step = 0, t = t) }),
        HowToStep(Res.string.howto_card_fruitmath_2, { t -> FruitMathDemo(step = 1, t = t) }),
        HowToStep(Res.string.howto_card_fruitmath_3, { t -> FruitMathDemo(step = 2, t = t) }),
    ),
)

// A banana is worth 2 and an apple 3: 3 bananas = 6; a banana times an apple = 6; the last line, an apple and 2 bananas, comes to 7.
private fun fruit(i: Int) = FruitToken.Fruit(i)
private val plus = FruitToken.Op('+')
private val First = FruitLine(listOf(fruit(1), plus, fruit(1), plus, fruit(1)), 6)
private val Second = FruitLine(listOf(fruit(1), FruitToken.Op('×'), fruit(0)), 6)
private val Last = FruitLine(listOf(fruit(0), plus, fruit(1), plus, fruit(1)), 7)

@Composable
internal fun FruitMathDemo(step: Int, t: Float) {
    val colors = BrainScrollTheme.colors
    val fade = demoFade(t)
    // The lines come one after the other; the one the step is about is lit.
    val second = if (step >= 1) demoEase(demoSegment(t, 0.1f, 0.3f)) else 0f
    val last = if (step >= 2) demoEase(demoSegment(t, 0.1f, 0.3f)) else 0f
    val solved = step == 2 && t > 0.6f
    Box(modifier = Modifier.fillMaxSize().alpha(fade), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp).background(colors.bgSurface, RoundedCornerShape(16.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val sizes = fruitSizes(listOf(First, Second, Last), 38.dp, 220.dp)
            FruitRow(First, First.total.toString(), colors.textPrimary, sizes, 38.dp)
            Box(Modifier.alpha(if (step == 0) 0.25f else second)) { FruitRow(Second, Second.total.toString(), colors.textPrimary, sizes, 38.dp) }
            Box(Modifier.alpha(if (step < 2) 0.25f else last)) {
                FruitRow(Last, if (solved) Last.total.toString() else "?", if (solved) colors.correctFill else colors.streakAnswer, sizes, 38.dp)
            }
        }
    }
}
