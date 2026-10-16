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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_button
import brainscroll.shared.generated.resources.howto_done
import brainscroll.shared.generated.resources.howto_target_1
import brainscroll.shared.generated.resources.howto_target_2
import brainscroll.shared.generated.resources.howto_target_3
import brainscroll.shared.generated.resources.howto_target_4
import brainscroll.shared.generated.resources.howto_target_5
import brainscroll.shared.generated.resources.howto_title_target
import brainscroll.shared.generated.resources.howto_pipes_1
import brainscroll.shared.generated.resources.howto_pipes_2
import brainscroll.shared.generated.resources.howto_pipes_3
import brainscroll.shared.generated.resources.howto_hanoi_1
import brainscroll.shared.generated.resources.howto_hanoi_2
import brainscroll.shared.generated.resources.howto_hanoi_3
import brainscroll.shared.generated.resources.howto_hanoi_4
import brainscroll.shared.generated.resources.howto_line_1
import brainscroll.shared.generated.resources.howto_line_2
import brainscroll.shared.generated.resources.howto_line_3
import brainscroll.shared.generated.resources.howto_line_4
import brainscroll.shared.generated.resources.howto_pipes_4
import brainscroll.shared.generated.resources.howto_title_hanoi
import brainscroll.shared.generated.resources.howto_title_queens
import brainscroll.shared.generated.resources.howto_title_tents
import brainscroll.shared.generated.resources.howto_title_crossword
import brainscroll.shared.generated.resources.howto_crossword_1
import brainscroll.shared.generated.resources.howto_crossword_2
import brainscroll.shared.generated.resources.howto_crossword_3
import brainscroll.shared.generated.resources.howto_crossword_4
import brainscroll.shared.generated.resources.howto_title_blockout
import brainscroll.shared.generated.resources.howto_blockout_1
import brainscroll.shared.generated.resources.howto_blockout_2
import brainscroll.shared.generated.resources.howto_blockout_3
import brainscroll.shared.generated.resources.howto_blockout_4
import brainscroll.shared.generated.resources.howto_title_jigsaw
import brainscroll.shared.generated.resources.howto_jigsaw_1
import brainscroll.shared.generated.resources.howto_jigsaw_2
import brainscroll.shared.generated.resources.howto_jigsaw_3
import brainscroll.shared.generated.resources.howto_jigsaw_4
import brainscroll.shared.generated.resources.howto_jigsaw_5
import brainscroll.shared.generated.resources.howto_tents_1
import brainscroll.shared.generated.resources.howto_tents_2
import brainscroll.shared.generated.resources.howto_tents_3
import brainscroll.shared.generated.resources.howto_tents_4
import brainscroll.shared.generated.resources.howto_queens_1
import brainscroll.shared.generated.resources.howto_queens_2
import brainscroll.shared.generated.resources.howto_queens_3
import brainscroll.shared.generated.resources.howto_queens_4
import brainscroll.shared.generated.resources.howto_title_line
import brainscroll.shared.generated.resources.howto_title_pipes
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
import com.mehmtcan.brainscroll.ui.components.ModalSheet
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The round "?" next to the name of the game. It opens [HowToPlaySheet]; the touch area is 40 dp wide, so the bar has room. */
@Composable
fun HowToIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val description = stringResource(Res.string.howto_button)
    Box(
        modifier = modifier
            .testTag("howToButton")
            .size(40.dp)
            .semantics { contentDescription = description }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { QuestionMark(colors.textSecondary) }
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
    GameKind.Pipes -> HowToContent(
        Res.string.howto_title_pipes,
        listOf(Res.string.howto_pipes_1, Res.string.howto_pipes_2, Res.string.howto_pipes_3, Res.string.howto_pipes_4),
    )
    GameKind.Target -> HowToContent(
        Res.string.howto_title_target,
        listOf(Res.string.howto_target_1, Res.string.howto_target_2, Res.string.howto_target_3, Res.string.howto_target_4, Res.string.howto_target_5),
    )
    GameKind.Slide -> HowToContent(
        Res.string.howto_title_slide,
        listOf(Res.string.howto_slide_1, Res.string.howto_slide_2, Res.string.howto_slide_3, Res.string.howto_slide_4),
    )
    GameKind.Lights -> HowToContent(
        Res.string.howto_title_lights,
        listOf(Res.string.howto_lights_1, Res.string.howto_lights_2, Res.string.howto_lights_3, Res.string.howto_lights_4),
    )
    GameKind.Hanoi -> HowToContent(
        Res.string.howto_title_hanoi,
        listOf(Res.string.howto_hanoi_1, Res.string.howto_hanoi_2, Res.string.howto_hanoi_3, Res.string.howto_hanoi_4),
    )
    GameKind.Queens -> HowToContent(
        Res.string.howto_title_queens,
        listOf(Res.string.howto_queens_1, Res.string.howto_queens_2, Res.string.howto_queens_3, Res.string.howto_queens_4),
    )
    GameKind.Tents -> HowToContent(
        Res.string.howto_title_tents,
        listOf(Res.string.howto_tents_1, Res.string.howto_tents_2, Res.string.howto_tents_3, Res.string.howto_tents_4),
    )
    GameKind.Jigsaw -> HowToContent(
        Res.string.howto_title_jigsaw,
        listOf(Res.string.howto_jigsaw_1, Res.string.howto_jigsaw_2, Res.string.howto_jigsaw_3, Res.string.howto_jigsaw_4, Res.string.howto_jigsaw_5),
    )
    GameKind.BlockOut -> HowToContent(
        Res.string.howto_title_blockout,
        listOf(Res.string.howto_blockout_1, Res.string.howto_blockout_2, Res.string.howto_blockout_3, Res.string.howto_blockout_4),
    )
    GameKind.Crossword -> HowToContent(
        Res.string.howto_title_crossword,
        listOf(Res.string.howto_crossword_1, Res.string.howto_crossword_2, Res.string.howto_crossword_3, Res.string.howto_crossword_4),
    )
    GameKind.Line -> HowToContent(
        Res.string.howto_title_line,
        listOf(Res.string.howto_line_1, Res.string.howto_line_2, Res.string.howto_line_3, Res.string.howto_line_4),
    )
    GameKind.Sudoku -> HowToContent(
        Res.string.howto_title_sudoku,
        listOf(Res.string.howto_sudoku_1, Res.string.howto_sudoku_2, Res.string.howto_sudoku_3, Res.string.howto_sudoku_4, Res.string.howto_sudoku_5),
    )
}

/**
 * The rules of one game as a sheet sliding up over the whole screen. The dark area behind it, the "Got it" button or the
 * system back button closes it. The steps scroll if the text size is large.
 */
@Composable
fun HowToPlaySheet(kind: GameKind, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val content = contentOf(kind)

    ModalSheet(onDismiss = onClose, modifier = modifier, tag = "howToSheet") { close ->
        BasicText(
            text = stringResource(content.title),
            style = type.title.copy(color = colors.textPrimary),
            modifier = Modifier.semantics { heading() },
        )
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
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
        AppButton(text = stringResource(Res.string.howto_done), onClick = close, primary = true)
    }
}
