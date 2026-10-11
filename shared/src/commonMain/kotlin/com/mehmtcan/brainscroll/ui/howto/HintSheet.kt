package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.hint_lights
import brainscroll.shared.generated.resources.hint_memory
import brainscroll.shared.generated.resources.hint_mines
import brainscroll.shared.generated.resources.hint_pipes
import brainscroll.shared.generated.resources.hint_slide
import brainscroll.shared.generated.resources.hint_sudoku
import brainscroll.shared.generated.resources.hint_target
import brainscroll.shared.generated.resources.hint_title
import brainscroll.shared.generated.resources.hint_tubes
import brainscroll.shared.generated.resources.hint_word
import brainscroll.shared.generated.resources.howto_done
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.home.displayName
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/**
 * A general tip for a game, as a panel over the page like the rules. It says nothing about the puzzle on screen, only
 * how to go about a puzzle of this kind, so it costs nothing: the puzzle is not marked and the streak is not touched.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HintSheet(kind: GameKind, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val tip = stringResource(
        when (kind) {
            GameKind.Word -> Res.string.hint_word
            GameKind.Memory -> Res.string.hint_memory
            GameKind.Tubes -> Res.string.hint_tubes
            GameKind.Mines -> Res.string.hint_mines
            GameKind.Sudoku -> Res.string.hint_sudoku
            GameKind.Lights -> Res.string.hint_lights
            GameKind.Slide -> Res.string.hint_slide
            GameKind.Target -> Res.string.hint_target
            GameKind.Pipes -> Res.string.hint_pipes
        },
    )
    BackHandler(enabled = true, onBack = onClose)

    Box(modifier = modifier.fillMaxSize().testTag("hintSheet")) {
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
            BasicText(
                text = stringResource(Res.string.hint_title, kind.displayName()),
                style = type.title.copy(color = colors.textPrimary),
                modifier = Modifier.semantics { heading() },
            )
            BasicText(tip, style = type.body.copy(color = colors.textPrimary))
            AppButton(text = stringResource(Res.string.howto_done), onClick = onClose, primary = true)
        }
    }
}
