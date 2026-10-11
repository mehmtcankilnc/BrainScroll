package com.mehmtcan.brainscroll.ui.streak

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.daily_freeze
import brainscroll.shared.generated.resources.howto_done
import brainscroll.shared.generated.resources.streak_best
import brainscroll.shared.generated.resources.streak_daily_1
import brainscroll.shared.generated.resources.streak_daily_2
import brainscroll.shared.generated.resources.streak_daily_3
import brainscroll.shared.generated.resources.streak_daily_title
import brainscroll.shared.generated.resources.streak_now
import brainscroll.shared.generated.resources.streak_win_1
import brainscroll.shared.generated.resources.streak_win_2
import brainscroll.shared.generated.resources.streak_win_3
import brainscroll.shared.generated.resources.streak_win_title
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** The two streaks of the app. They are different things, so each has its own explanation. */
enum class StreakKind { Daily, Win }

/**
 * What a streak means, as a panel over the page in the same style as the rules of a game: where the number comes from,
 * what breaks it, and how far it has come. The scrim behind it, the "Got it" button or the system back button closes it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StreakInfoSheet(kind: StreakKind, current: Int, best: Int, onClose: () -> Unit, modifier: Modifier = Modifier, freezes: Int = 0) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val accent = if (kind == StreakKind.Daily) colors.streakDay else colors.streakAnswer
    val title = stringResource(if (kind == StreakKind.Daily) Res.string.streak_daily_title else Res.string.streak_win_title)
    val lines = if (kind == StreakKind.Daily) {
        listOf(Res.string.streak_daily_1, Res.string.streak_daily_2, Res.string.streak_daily_3)
    } else {
        listOf(Res.string.streak_win_1, Res.string.streak_win_2, Res.string.streak_win_3)
    }

    // Back closes the panel and nothing else.
    BackHandler(enabled = true, onBack = onClose)

    Box(modifier = modifier.fillMaxSize().testTag("streakSheet")) {
        // The scrim: tapping outside the panel closes it, and keeps touches away from the page below.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.bgSurface, Radius.card)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            BasicText(title, style = type.title.copy(color = accent), modifier = Modifier.semantics { heading() })
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                BasicText(stringResource(Res.string.streak_now, current), style = type.heading.copy(color = colors.textPrimary), modifier = Modifier.testTag("streakNow"))
                BasicText(stringResource(Res.string.streak_best, best), style = type.heading.copy(color = colors.textSecondary))
            }
            lines.forEachIndexed { index, line ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Box(Modifier.size(24.dp).background(colors.bgRaised, CircleShape), contentAlignment = Alignment.Center) {
                        BasicText((index + 1).toString(), style = type.label.copy(color = colors.textPrimary))
                    }
                    BasicText(stringResource(line), style = type.body.copy(color = colors.textPrimary), modifier = Modifier.weight(1f))
                }
            }
            if (kind == StreakKind.Daily) {
                BasicText(stringResource(Res.string.daily_freeze, freezes), style = type.label.copy(color = colors.textSecondary))
            }
            AppButton(text = stringResource(Res.string.howto_done), onClick = onClose, primary = true, modifier = Modifier.padding(top = Spacing.xs))
        }
    }
}
