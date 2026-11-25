package com.mehmtcan.brainscroll.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * What the player sees right after the system's own start screen (the icon on the page color): the same icon-character comes
 * to life. It pops up, looks around, blinks, then the name appears and everything fades into the app, which has been starting
 * underneath. With reduced motion it is the still character for a moment. [onDone] is called when it is gone.
 */
@Composable
fun SplashOverlay(onDone: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val reduce = LocalReduceMotion.current
    val pop = remember { Animatable(if (reduce) 1f else 0.78f) }
    val look = remember { Animatable(0f) }
    val name = remember { Animatable(if (reduce) 1f else 0f) }
    val whole = remember { Animatable(1f) }
    var blinkNow by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        if (reduce) {
            delay(700)
            whole.animateTo(0f, tween(200))
            onDone()
            return@LaunchedEffect
        }
        coroutineScope {
            async { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
            async {
                delay(250)
                look.animateTo(-1f, tween(260))
                look.animateTo(1f, tween(420))
                look.animateTo(0f, tween(240))
            }
            async {
                delay(900)
                name.animateTo(1f, tween(320))
            }
        }
        blinkNow = 1f
        delay(260)
        whole.animateTo(0f, tween(260))
        onDone()
    }

    Column(
        Modifier.fillMaxSize().background(colors.bgPage).alpha(whole.value).testTag("splash").clearAndSetSemantics { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrainMascot(
            t = 0f,
            modifier = Modifier.size(132.dp).scale(pop.value),
            lookX = look.value,
            interactive = false,
        )
        Spacer(Modifier.height(20.dp))
        BasicText(
            "BrainScroll",
            style = type.display.copy(color = colors.textPrimary, textAlign = TextAlign.Center),
            modifier = Modifier.alpha(name.value),
        )
    }
}
