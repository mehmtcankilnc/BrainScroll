package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
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
import brainscroll.shared.generated.resources.howto_button
import brainscroll.shared.generated.resources.howto_close_hint
import brainscroll.shared.generated.resources.howto_done
import brainscroll.shared.generated.resources.howto_slide_1
import brainscroll.shared.generated.resources.howto_slide_2
import brainscroll.shared.generated.resources.howto_slide_3
import brainscroll.shared.generated.resources.howto_slide_4
import brainscroll.shared.generated.resources.howto_title_slide
import brainscroll.shared.generated.resources.howto_lights_1
import brainscroll.shared.generated.resources.howto_lights_2
import brainscroll.shared.generated.resources.howto_lights_3
import brainscroll.shared.generated.resources.howto_lights_4
import brainscroll.shared.generated.resources.howto_title_lights
import brainscroll.shared.generated.resources.howto_memory_1
import brainscroll.shared.generated.resources.howto_memory_2
import brainscroll.shared.generated.resources.howto_memory_3
import brainscroll.shared.generated.resources.howto_memory_4
import brainscroll.shared.generated.resources.howto_mines_1
import brainscroll.shared.generated.resources.howto_mines_2
import brainscroll.shared.generated.resources.howto_mines_3
import brainscroll.shared.generated.resources.howto_mines_4
import brainscroll.shared.generated.resources.howto_mines_5
import brainscroll.shared.generated.resources.howto_sudoku_1
import brainscroll.shared.generated.resources.howto_sudoku_2
import brainscroll.shared.generated.resources.howto_sudoku_3
import brainscroll.shared.generated.resources.howto_sudoku_4
import brainscroll.shared.generated.resources.howto_sudoku_5
import brainscroll.shared.generated.resources.howto_title_sudoku
import brainscroll.shared.generated.resources.howto_title_memory
import brainscroll.shared.generated.resources.howto_title_mines
import brainscroll.shared.generated.resources.howto_title_tubes
import brainscroll.shared.generated.resources.howto_title_word
import brainscroll.shared.generated.resources.howto_tubes_1
import brainscroll.shared.generated.resources.howto_tubes_2
import brainscroll.shared.generated.resources.howto_tubes_3
import brainscroll.shared.generated.resources.howto_tubes_4
import brainscroll.shared.generated.resources.howto_word_1
import brainscroll.shared.generated.resources.howto_word_2
import brainscroll.shared.generated.resources.howto_word_3
import brainscroll.shared.generated.resources.howto_word_4
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The "How to play?" pill every game page has under its title. It opens [HowToPlaySheet]. */
@Composable
fun HowToPlayButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    Box(modifier.testTag("howToButton")) {
        Chip(
            text = stringResource(Res.string.howto_button),
            textColor = colors.textSecondary,
            onClick = onClick,
            icon = { QuestionMark(colors.textSecondary) },
        )
    }
}

/** A small circle with a question mark, drawn with text so it follows the text color. */
@Composable
private fun QuestionMark(color: Color) {
    Box(
        modifier = Modifier.size(18.dp).border(1.5.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText("?", style = BrainScrollTheme.typography.caption.copy(color = color))
    }
}

/** The title and the steps of each game. Kept as data so a new game only adds a line here. */
private class HowToContent(val title: StringResource, val steps: List<StringResource>)

private fun contentOf(kind: GameKind) = when (kind) {
    GameKind.Word -> HowToContent(
        Res.string.howto_title_word,
        listOf(Res.string.howto_word_1, Res.string.howto_word_2, Res.string.howto_word_3, Res.string.howto_word_4),
    )
    GameKind.Memory -> HowToContent(
        Res.string.howto_title_memory,
        listOf(Res.string.howto_memory_1, Res.string.howto_memory_2, Res.string.howto_memory_3, Res.string.howto_memory_4),
    )
    GameKind.Tubes -> HowToContent(
        Res.string.howto_title_tubes,
        listOf(Res.string.howto_tubes_1, Res.string.howto_tubes_2, Res.string.howto_tubes_3, Res.string.howto_tubes_4),
    )
    GameKind.Mines -> HowToContent(
        Res.string.howto_title_mines,
        listOf(Res.string.howto_mines_1, Res.string.howto_mines_2, Res.string.howto_mines_3, Res.string.howto_mines_4, Res.string.howto_mines_5),
    )
    GameKind.Slide -> HowToContent(
        Res.string.howto_title_slide,
        listOf(Res.string.howto_slide_1, Res.string.howto_slide_2, Res.string.howto_slide_3, Res.string.howto_slide_4),
    )
    GameKind.Lights -> HowToContent(
        Res.string.howto_title_lights,
        listOf(Res.string.howto_lights_1, Res.string.howto_lights_2, Res.string.howto_lights_3, Res.string.howto_lights_4),
    )
    GameKind.Sudoku -> HowToContent(
        Res.string.howto_title_sudoku,
        listOf(Res.string.howto_sudoku_1, Res.string.howto_sudoku_2, Res.string.howto_sudoku_3, Res.string.howto_sudoku_4, Res.string.howto_sudoku_5),
    )
}

/**
 * The rules of one game as a panel over the page, in the same style as the feed settings: the scrim behind it, the
 * "Got it" button or the system back button closes it. The steps scroll if the text size is large.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HowToPlaySheet(kind: GameKind, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val content = contentOf(kind)

    // Back closes the panel and nothing else (it is the innermost handler, so it goes first).
    BackHandler(enabled = true, onBack = onClose)

    Box(modifier = modifier.fillMaxSize().testTag("howToSheet")) {
        // The scrim: tapping outside the panel closes it. It also keeps touches and swipes away from the page below.
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
                text = stringResource(content.title),
                style = type.title.copy(color = colors.textPrimary),
                modifier = Modifier.semantics { heading() },
            )
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                content.steps.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        // The number sits in a small circle; the text is the same size as the rest of the app.
                        Box(
                            modifier = Modifier.size(24.dp).background(colors.bgRaised, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            BasicText((index + 1).toString(), style = type.label.copy(color = colors.textPrimary))
                        }
                        BasicText(
                            text = stringResource(step),
                            style = type.body.copy(color = colors.textPrimary),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            BasicText(stringResource(Res.string.howto_close_hint), style = type.caption.copy(color = colors.textSecondary))
            AppButton(text = stringResource(Res.string.howto_done), onClick = onClose, primary = true)
        }
    }
}
