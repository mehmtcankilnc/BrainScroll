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
import brainscroll.shared.generated.resources.guide_matchsticks_move
import brainscroll.shared.generated.resources.guide_lasers_turn
import brainscroll.shared.generated.resources.guide_untangle_move
import brainscroll.shared.generated.resources.guide_codebreaker_next
import brainscroll.shared.generated.resources.guide_lonenumber_shade_same
import brainscroll.shared.generated.resources.guide_zerosones_fill_pair
import brainscroll.shared.generated.resources.guide_zerosones_fill_split
import brainscroll.shared.generated.resources.guide_zerosones_fill_half
import brainscroll.shared.generated.resources.guide_lamps_place_self
import brainscroll.shared.generated.resources.guide_lamps_place_other
import brainscroll.shared.generated.resources.guide_lamps_place_wall
import brainscroll.shared.generated.resources.guide_greaterless_fill_line
import brainscroll.shared.generated.resources.guide_greaterless_fill_sign
import brainscroll.shared.generated.resources.guide_greaterless_fill_hidden
import brainscroll.shared.generated.resources.guide_fences_line_clue
import brainscroll.shared.generated.resources.guide_fences_line_dot
import brainscroll.shared.generated.resources.guide_mines_safe_complete
import brainscroll.shared.generated.resources.guide_mines_mine_fills
import brainscroll.shared.generated.resources.guide_sudoku_only_place_row
import brainscroll.shared.generated.resources.guide_sudoku_only_place_col
import brainscroll.shared.generated.resources.guide_sudoku_only_place_box
import brainscroll.shared.generated.resources.guide_jigsaw_only_place_region
import brainscroll.shared.generated.resources.guide_tents_place_row
import brainscroll.shared.generated.resources.guide_tents_place_tree
import brainscroll.shared.generated.resources.guide_tents_place_column
import brainscroll.shared.generated.resources.guide_queens_place_row
import brainscroll.shared.generated.resources.guide_queens_place_column
import brainscroll.shared.generated.resources.guide_nonogram_fill_full
import brainscroll.shared.generated.resources.guide_nonogram_fill_overlap
import brainscroll.shared.generated.resources.guide_lights_tap_out
import brainscroll.shared.generated.resources.guide_slide_tile_home
import brainscroll.shared.generated.resources.guide_slide_tile_out
import brainscroll.shared.generated.resources.guide_tubes_pour_full
import brainscroll.shared.generated.resources.guide_tubes_pour_same
import brainscroll.shared.generated.resources.guide_tubes_pour_empty
import brainscroll.shared.generated.resources.guide_target_combine_last
import brainscroll.shared.generated.resources.guide_target_combine_first
import brainscroll.shared.generated.resources.guide_hanoi_move_small
import brainscroll.shared.generated.resources.guide_hanoi_move_other
import brainscroll.shared.generated.resources.guide_axissum_erase_row
import brainscroll.shared.generated.resources.guide_axissum_erase_column
import brainscroll.shared.generated.resources.guide_blockout_why_clear
import brainscroll.shared.generated.resources.guide_blockout_why_target
import brainscroll.shared.generated.resources.guide_fences_clear
import brainscroll.shared.generated.resources.guide_fences_line
import brainscroll.shared.generated.resources.guide_greaterless_clear
import brainscroll.shared.generated.resources.guide_greaterless_fill
import brainscroll.shared.generated.resources.guide_lamps_clear
import brainscroll.shared.generated.resources.guide_lamps_place
import brainscroll.shared.generated.resources.guide_lonenumber_clear
import brainscroll.shared.generated.resources.guide_crosssum_both
import brainscroll.shared.generated.resources.guide_crosssum_block
import brainscroll.shared.generated.resources.guide_crosssum_clear
import brainscroll.shared.generated.resources.guide_crosssum_last
import brainscroll.shared.generated.resources.guide_mirror_empty
import brainscroll.shared.generated.resources.guide_oddone_rule
import brainscroll.shared.generated.resources.guide_mirror_paint
import brainscroll.shared.generated.resources.guide_zerosones_clear
import brainscroll.shared.generated.resources.guide_zerosones_fill
import brainscroll.shared.generated.resources.guide_lonenumber_shade
import brainscroll.shared.generated.resources.guide_rectangles_place
import brainscroll.shared.generated.resources.guide_colorseq_replay
import brainscroll.shared.generated.resources.guide_cubefold_mirror
import brainscroll.shared.generated.resources.guide_cubefold_opposite
import brainscroll.shared.generated.resources.guide_flashgrid_pattern
import brainscroll.shared.generated.resources.guide_rectangles_remove
import com.mehmtcan.brainscroll.ui.codebreaker.codeSymbol
import brainscroll.shared.generated.resources.guide_fruitmath_values
import com.mehmtcan.brainscroll.ui.fruitmath.fruitEmoji
import brainscroll.shared.generated.resources.guide_matchsticks_undo
import brainscroll.shared.generated.resources.guide_hanoi_move
import brainscroll.shared.generated.resources.guide_jigsaw_only_digit
import brainscroll.shared.generated.resources.guide_jigsaw_only_place
import brainscroll.shared.generated.resources.guide_arrows_escape
import brainscroll.shared.generated.resources.arrows_dir_down
import brainscroll.shared.generated.resources.arrows_dir_left
import brainscroll.shared.generated.resources.arrows_dir_right
import brainscroll.shared.generated.resources.arrows_dir_up
import brainscroll.shared.generated.resources.guide_ballslide_move
import brainscroll.shared.generated.resources.guide_colorflow_route
import brainscroll.shared.generated.resources.guide_topicgroups_group
import com.mehmtcan.brainscroll.game.memory.CardSymbol
import com.mehmtcan.brainscroll.ui.memory.symbolName
import brainscroll.shared.generated.resources.guide_ballslide_stuck
import brainscroll.shared.generated.resources.guide_axissum_erase
import brainscroll.shared.generated.resources.guide_axissum_keep
import brainscroll.shared.generated.resources.guide_nonogram_empty
import brainscroll.shared.generated.resources.guide_nonogram_fill
import brainscroll.shared.generated.resources.guide_blockout_down
import brainscroll.shared.generated.resources.guide_blockout_left
import brainscroll.shared.generated.resources.guide_blockout_right
import brainscroll.shared.generated.resources.guide_blockout_up
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
        GuideReason.MinesSafe -> if (step.rule == 1) stringResource(Res.string.guide_mines_safe_complete, n[0]) else stringResource(Res.string.guide_mines_safe)
        GuideReason.MinesMine -> if (step.rule == 2) stringResource(Res.string.guide_mines_mine_fills, n[0]) else stringResource(Res.string.guide_mines_mine)
        GuideReason.MinesNone -> stringResource(Res.string.guide_mines_none)
        GuideReason.SudokuErase -> stringResource(Res.string.guide_sudoku_erase)
        GuideReason.SudokuOnlyDigit -> stringResource(Res.string.guide_sudoku_only_digit, n[0])
        GuideReason.SudokuOnlyPlace -> when (step.rule) {
            1 -> stringResource(Res.string.guide_sudoku_only_place_row, n[0])
            2 -> stringResource(Res.string.guide_sudoku_only_place_col, n[0])
            else -> stringResource(Res.string.guide_sudoku_only_place_box, n[0])
        }
        GuideReason.SudokuSolution -> stringResource(Res.string.guide_sudoku_solution, n[0])
        GuideReason.PipesTurn -> stringResource(Res.string.guide_pipes_turn, n[0])
        GuideReason.HanoiMove -> when (step.rule) {
            1 -> stringResource(Res.string.guide_hanoi_move_small, towerName(n[0]), towerName(n[1]))
            2 -> stringResource(Res.string.guide_hanoi_move_other, towerName(n[0]), towerName(n[1]), n[2])
            else -> stringResource(Res.string.guide_hanoi_move, towerName(n[0]), towerName(n[1]))
        }
        GuideReason.QueensPlace -> when (step.rule) {
            1 -> stringResource(Res.string.guide_queens_place_row, n[0], n[1])
            2 -> stringResource(Res.string.guide_queens_place_column, n[0], n[1])
            else -> stringResource(Res.string.guide_queens_place, n[0], n[1])
        }
        GuideReason.QueensRemove -> stringResource(Res.string.guide_queens_remove, n[0], n[1])
        GuideReason.TentsPlace -> when (step.rule) {
            1 -> stringResource(Res.string.guide_tents_place_row, n[0], n[1])
            2 -> stringResource(Res.string.guide_tents_place_tree, n[0], n[1])
            3 -> stringResource(Res.string.guide_tents_place_column, n[0], n[1])
            else -> stringResource(Res.string.guide_tents_place, n[0], n[1])
        }
        GuideReason.TentsRemove -> stringResource(Res.string.guide_tents_remove, n[0], n[1])
        GuideReason.JigsawOnlyDigit -> stringResource(Res.string.guide_jigsaw_only_digit, n[0])
        GuideReason.JigsawOnlyPlace -> when (step.rule) {
            1, 2 -> stringResource(if (step.rule == 1) Res.string.guide_sudoku_only_place_row else Res.string.guide_sudoku_only_place_col, n[0])
            else -> stringResource(Res.string.guide_jigsaw_only_place_region, n[0])
        }
        GuideReason.BlockOutSlide -> stringResource(
            when (n[0]) {
                0 -> Res.string.guide_blockout_up
                1 -> Res.string.guide_blockout_down
                2 -> Res.string.guide_blockout_left
                else -> Res.string.guide_blockout_right
            },
            n[1],
        ) + " " + stringResource(if (step.rule == 1) Res.string.guide_blockout_why_target else Res.string.guide_blockout_why_clear)
        GuideReason.ArrowsEscape -> stringResource(Res.string.guide_arrows_escape, n[0], n[1])
        GuideReason.BallSlideMove -> stringResource(
            Res.string.guide_ballslide_move,
            stringResource(
                when (n[0]) {
                    0 -> Res.string.arrows_dir_up
                    1 -> Res.string.arrows_dir_right
                    2 -> Res.string.arrows_dir_down
                    else -> Res.string.arrows_dir_left
                },
            ),
        )
        GuideReason.FlowRoute -> stringResource(Res.string.guide_colorflow_route, stringResource(symbolName(CardSymbol.entries[n[0] % CardSymbol.entries.size])))
        GuideReason.MatchstickMove -> stringResource(Res.string.guide_matchsticks_move)
        GuideReason.FruitValues -> {
            val half = n.size / 2
            val values = (0 until half).joinToString("  ") { "${fruitEmoji(n[it])} = ${n[half + it]}" }
            stringResource(Res.string.guide_fruitmath_values, values)
        }
        GuideReason.UntangleMove -> stringResource(Res.string.guide_untangle_move)
        GuideReason.FlashPattern -> stringResource(Res.string.guide_flashgrid_pattern)
        GuideReason.SeqReplay -> stringResource(Res.string.guide_colorseq_replay)
        GuideReason.CubeOpposite -> stringResource(Res.string.guide_cubefold_opposite)
        GuideReason.CubeMirror -> stringResource(Res.string.guide_cubefold_mirror)
        GuideReason.RectPlace -> stringResource(Res.string.guide_rectangles_place, n[0])
        GuideReason.RectRemove -> stringResource(Res.string.guide_rectangles_remove)
        GuideReason.LoneShade -> if (step.rule == 1) stringResource(Res.string.guide_lonenumber_shade_same) else stringResource(Res.string.guide_lonenumber_shade)
        GuideReason.LoneClear -> stringResource(Res.string.guide_lonenumber_clear)
        GuideReason.MirrorPaint -> stringResource(Res.string.guide_mirror_paint)
        GuideReason.MirrorEmpty -> stringResource(Res.string.guide_mirror_empty)
        GuideReason.CrossDigit -> stringResource(
            when (step.rule) {
                com.mehmtcan.brainscroll.game.crosssum.CrossSolver.LAST -> Res.string.guide_crosssum_last
                com.mehmtcan.brainscroll.game.crosssum.CrossSolver.ONE_BLOCK -> Res.string.guide_crosssum_block
                else -> Res.string.guide_crosssum_both
            },
            step.numbers.first(),
        )
        GuideReason.CrossClear -> stringResource(Res.string.guide_crosssum_clear)
        GuideReason.OddRule -> stringResource(Res.string.guide_oddone_rule, stringResource(com.mehmtcan.brainscroll.ui.oddone.oddRuleName(com.mehmtcan.brainscroll.game.oddone.OddRule.entries[step.rule])))
        GuideReason.BinFill -> when (step.rule) {
            1 -> stringResource(Res.string.guide_zerosones_fill_pair, n[0], n[1])
            2 -> stringResource(Res.string.guide_zerosones_fill_split, n[0], n[1])
            3 -> stringResource(Res.string.guide_zerosones_fill_half, n[0], n[1])
            else -> stringResource(Res.string.guide_zerosones_fill, n[0], n[1])
        }
        GuideReason.BinClear -> stringResource(Res.string.guide_zerosones_clear)
        GuideReason.LampPlace -> when (step.rule) {
            1 -> stringResource(Res.string.guide_lamps_place_self)
            2 -> stringResource(Res.string.guide_lamps_place_other)
            4 -> stringResource(Res.string.guide_lamps_place_wall, n[0])
            else -> stringResource(Res.string.guide_lamps_place)
        }
        GuideReason.LampClear -> stringResource(Res.string.guide_lamps_clear)
        GuideReason.GlFill -> when (step.rule) {
            1 -> stringResource(Res.string.guide_greaterless_fill_line, n[0])
            2 -> stringResource(Res.string.guide_greaterless_fill_sign, n[0])
            3 -> stringResource(Res.string.guide_greaterless_fill_hidden, n[0])
            else -> stringResource(Res.string.guide_greaterless_fill, n[0])
        }
        GuideReason.GlClear -> stringResource(Res.string.guide_greaterless_clear)
        GuideReason.FenceLine -> when (step.rule) {
            1 -> stringResource(Res.string.guide_fences_line_clue, n[0])
            2 -> stringResource(Res.string.guide_fences_line_dot)
            else -> stringResource(Res.string.guide_fences_line)
        }
        GuideReason.FenceClear -> stringResource(Res.string.guide_fences_clear)
        GuideReason.CodeNext -> stringResource(Res.string.guide_codebreaker_next, n.joinToString(" ") { codeSymbol(it).toString() })
        GuideReason.LaserTurn -> stringResource(Res.string.guide_lasers_turn)
        GuideReason.MatchstickUndo -> stringResource(Res.string.guide_matchsticks_undo)
        GuideReason.TopicGroupsGroup -> stringResource(Res.string.guide_topicgroups_group, step.text)
        GuideReason.BallSlideStuck -> stringResource(Res.string.guide_ballslide_stuck)
        GuideReason.AxisSumErase -> when (step.rule) {
            1 -> stringResource(Res.string.guide_axissum_erase_row, n[0], n[1], n[2])
            2 -> stringResource(Res.string.guide_axissum_erase_column, n[0], n[1], n[2])
            else -> stringResource(Res.string.guide_axissum_erase, n[0], n[1])
        }
        GuideReason.AxisSumKeep -> stringResource(Res.string.guide_axissum_keep, n[0], n[1])
        GuideReason.NonogramFill -> when (step.rule) {
            1, 3 -> stringResource(Res.string.guide_nonogram_fill_full, n[0], n[1], n[2])
            2, 4 -> stringResource(Res.string.guide_nonogram_fill_overlap, n[0], n[1], n[2])
            else -> stringResource(Res.string.guide_nonogram_fill, n[0], n[1])
        }
        GuideReason.NonogramEmpty -> stringResource(Res.string.guide_nonogram_empty, n[0], n[1])
        GuideReason.LightsTap -> if (step.rule == 1) stringResource(Res.string.guide_lights_tap_out, n[0], n[1]) else stringResource(Res.string.guide_lights_tap)
        GuideReason.SlideTile -> when (step.rule) {
            1 -> stringResource(Res.string.guide_slide_tile_home, n[0])
            2 -> stringResource(Res.string.guide_slide_tile_out, n[0])
            else -> stringResource(Res.string.guide_slide_tile)
        }
        GuideReason.TubesPour -> when (step.rule) {
            1 -> stringResource(Res.string.guide_tubes_pour_full)
            2 -> stringResource(Res.string.guide_tubes_pour_same)
            3 -> stringResource(Res.string.guide_tubes_pour_empty)
            else -> stringResource(Res.string.guide_tubes_pour)
        }
        GuideReason.TubesStuck -> stringResource(Res.string.guide_tubes_stuck)
        GuideReason.TargetStuck -> stringResource(Res.string.guide_target_stuck)
        GuideReason.TargetCombine -> when {
            step.rule == 1 -> stringResource(Res.string.guide_target_combine_last, n[0], n[1], operationSymbol(step.text))
            n.size >= 4 && n[3] > 1 -> stringResource(Res.string.guide_target_combine_first, n[0], n[1], operationSymbol(step.text), n[3] - 1)
            else -> stringResource(Res.string.guide_target_combine, n[0], n[1], operationSymbol(step.text))
        }
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
            BasicText(guideText(step), style = type.body.copy(color = colors.textPrimary), maxLines = 6, overflow = TextOverflow.Ellipsis)
            if (onRestart != null && step.needsRestart()) {
                AppTextButton(stringResource(Res.string.guide_restart), onRestart, Modifier.testTag("guideRestart"))
            }
        }
        CloseButton(onClose, tag = "guideClose")
    }
}
