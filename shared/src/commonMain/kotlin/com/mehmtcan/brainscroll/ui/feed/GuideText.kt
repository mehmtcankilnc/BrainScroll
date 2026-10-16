package com.mehmtcan.brainscroll.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import com.mehmtcan.brainscroll.ui.components.AppTextButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.components.CloseButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.guide_hanoi_move
import brainscroll.shared.generated.resources.guide_jigsaw_only_digit
import brainscroll.shared.generated.resources.guide_jigsaw_only_place
import brainscroll.shared.generated.resources.guide_blockout_down
import brainscroll.shared.generated.resources.guide_blockout_left
import brainscroll.shared.generated.resources.guide_blockout_right
import brainscroll.shared.generated.resources.guide_blockout_up
import brainscroll.shared.generated.resources.guide_crossword_erase
import brainscroll.shared.generated.resources.guide_crossword_letter
import brainscroll.shared.generated.resources.guide_lights_tap
import brainscroll.shared.generated.resources.guide_queens_place
import brainscroll.shared.generated.resources.guide_queens_remove
import brainscroll.shared.generated.resources.guide_tents_place
import brainscroll.shared.generated.resources.guide_tents_remove
import brainscroll.shared.generated.resources.guide_memory_pair
import brainscroll.shared.generated.resources.guide_memory_partner
import brainscroll.shared.generated.resources.guide_mines_mine
import brainscroll.shared.generated.resources.guide_mines_none
import brainscroll.shared.generated.resources.guide_mines_safe
import brainscroll.shared.generated.resources.guide_mines_start
import brainscroll.shared.generated.resources.guide_pipes_turn
import brainscroll.shared.generated.resources.guide_slide_tile
import brainscroll.shared.generated.resources.guide_sudoku_erase
import brainscroll.shared.generated.resources.guide_sudoku_only_digit
import brainscroll.shared.generated.resources.guide_sudoku_only_place
import brainscroll.shared.generated.resources.guide_sudoku_solution
import brainscroll.shared.generated.resources.guide_target_combine
import brainscroll.shared.generated.resources.guide_target_stuck
import brainscroll.shared.generated.resources.guide_restart
import brainscroll.shared.generated.resources.guide_tubes_pour
import brainscroll.shared.generated.resources.guide_tubes_stuck
import brainscroll.shared.generated.resources.guide_word_letter
import com.mehmtcan.brainscroll.game.guide.GuideReason
import com.mehmtcan.brainscroll.game.guide.GuideStep
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** The sentence of a step, in the language of the app. */
@Composable
fun guideText(step: GuideStep): String {
    val n = step.numbers
    return when (step.reason) {
        GuideReason.MinesStart -> stringResource(Res.string.guide_mines_start)
        GuideReason.MinesSafe -> stringResource(Res.string.guide_mines_safe)
        GuideReason.MinesMine -> stringResource(Res.string.guide_mines_mine)
        GuideReason.MinesNone -> stringResource(Res.string.guide_mines_none)
        GuideReason.SudokuErase -> stringResource(Res.string.guide_sudoku_erase)
        GuideReason.SudokuOnlyDigit -> stringResource(Res.string.guide_sudoku_only_digit, n[0])
        GuideReason.SudokuOnlyPlace -> stringResource(Res.string.guide_sudoku_only_place, n[0])
        GuideReason.SudokuSolution -> stringResource(Res.string.guide_sudoku_solution, n[0])
        GuideReason.PipesTurn -> stringResource(Res.string.guide_pipes_turn, n[0])
        GuideReason.HanoiMove -> stringResource(Res.string.guide_hanoi_move, towerName(n[0]), towerName(n[1]))
        GuideReason.QueensPlace -> stringResource(Res.string.guide_queens_place, n[0], n[1])
        GuideReason.QueensRemove -> stringResource(Res.string.guide_queens_remove, n[0], n[1])
        GuideReason.TentsPlace -> stringResource(Res.string.guide_tents_place, n[0], n[1])
        GuideReason.TentsRemove -> stringResource(Res.string.guide_tents_remove, n[0], n[1])
        GuideReason.JigsawOnlyDigit -> stringResource(Res.string.guide_jigsaw_only_digit, n[0])
        GuideReason.JigsawOnlyPlace -> stringResource(Res.string.guide_jigsaw_only_place, n[0])
        GuideReason.BlockOutSlide -> stringResource(
            when (n[0]) {
                0 -> Res.string.guide_blockout_up
                1 -> Res.string.guide_blockout_down
                2 -> Res.string.guide_blockout_left
                else -> Res.string.guide_blockout_right
            },
            n[1],
        )
        GuideReason.CrosswordErase -> stringResource(Res.string.guide_crossword_erase, n[0], n[1])
        GuideReason.CrosswordLetter -> stringResource(Res.string.guide_crossword_letter, step.text, n[0], n[1])
        GuideReason.LightsTap -> stringResource(Res.string.guide_lights_tap)
        GuideReason.SlideTile -> stringResource(Res.string.guide_slide_tile)
        GuideReason.TubesPour -> stringResource(Res.string.guide_tubes_pour)
        GuideReason.TubesStuck -> stringResource(Res.string.guide_tubes_stuck)
        GuideReason.TargetStuck -> stringResource(Res.string.guide_target_stuck)
        GuideReason.TargetCombine -> stringResource(Res.string.guide_target_combine, n[0], n[1], operationSymbol(step.text))
        GuideReason.MemoryPartner -> stringResource(Res.string.guide_memory_partner)
        GuideReason.MemoryPair -> stringResource(Res.string.guide_memory_pair)
        GuideReason.WordLetter -> stringResource(Res.string.guide_word_letter, n[0], step.text)
    }
}

/** The letter of a tower (the first is A). */
private fun towerName(tower: Int): String = ('A' + tower).toString()

/** What the screen shows for an operation: the saved symbols are + - * /. */
private fun operationSymbol(saved: String): String = when (saved) {
    "*" -> "×"
    "/" -> "÷"
    "-" -> "−"
    else -> saved
}

/** Whether the step says the puzzle cannot be finished from here, so the banner offers to start it over. */
fun GuideStep.needsRestart(): Boolean = reason == GuideReason.TubesStuck || reason == GuideReason.TargetStuck

/**
 * The step on screen: its sentence (that the guide leaves the win streak alone is said in the explanation of the streak). It sits under the
 * board, in the room the board does not use, so it never covers what the guide points at. The x closes it. When the puzzle
 * cannot be finished from where it is, [onRestart] puts it back to its start so the guide can begin again.
 */
@Composable
fun GuideBanner(step: GuideStep, onClose: () -> Unit, modifier: Modifier = Modifier, onRestart: (() -> Unit)? = null) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Row(
        modifier = modifier
            .testTag("guideBanner")
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = 2.dp)
            .heightIn(min = 52.dp)
            .background(colors.bgRaised, Radius.card)
            // The bar under the card must not get the touches that land on the card.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(start = Spacing.md)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(vertical = Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            BasicText(guideText(step), style = type.body.copy(color = colors.textPrimary), maxLines = 4, overflow = TextOverflow.Ellipsis)
            if (onRestart != null && step.needsRestart()) {
                AppTextButton(stringResource(Res.string.guide_restart), onRestart, Modifier.testTag("guideRestart"))
            }
        }
        CloseButton(onClose, tag = "guideClose")
    }
}
