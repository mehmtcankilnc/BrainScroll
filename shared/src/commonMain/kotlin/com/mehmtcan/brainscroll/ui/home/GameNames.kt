package com.mehmtcan.brainscroll.ui.home

import androidx.compose.runtime.Composable
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.game_hanoi
import brainscroll.shared.generated.resources.game_jigsaw
import brainscroll.shared.generated.resources.game_blockout
import brainscroll.shared.generated.resources.game_crossword
import brainscroll.shared.generated.resources.game_lights
import brainscroll.shared.generated.resources.game_line
import brainscroll.shared.generated.resources.game_memory
import brainscroll.shared.generated.resources.game_mines
import brainscroll.shared.generated.resources.game_pipes
import brainscroll.shared.generated.resources.game_queens
import brainscroll.shared.generated.resources.game_tents
import brainscroll.shared.generated.resources.game_slide
import brainscroll.shared.generated.resources.game_sudoku
import brainscroll.shared.generated.resources.game_target
import brainscroll.shared.generated.resources.game_tubes
import brainscroll.shared.generated.resources.game_word_puzzle
import com.mehmtcan.brainscroll.game.GameKind
import org.jetbrains.compose.resources.stringResource

/** The name of a game as the player sees it (the home tab and the feed editor). */
@Composable
fun GameKind.displayName(): String = stringResource(
    when (this) {
        GameKind.Word -> Res.string.game_word_puzzle
        GameKind.Memory -> Res.string.game_memory
        GameKind.Tubes -> Res.string.game_tubes
        GameKind.Mines -> Res.string.game_mines
        GameKind.Sudoku -> Res.string.game_sudoku
        GameKind.Lights -> Res.string.game_lights
        GameKind.Slide -> Res.string.game_slide
        GameKind.Target -> Res.string.game_target
        GameKind.Pipes -> Res.string.game_pipes
        GameKind.Hanoi -> Res.string.game_hanoi
        GameKind.Queens -> Res.string.game_queens
        GameKind.Tents -> Res.string.game_tents
        GameKind.Jigsaw -> Res.string.game_jigsaw
        GameKind.BlockOut -> Res.string.game_blockout
        GameKind.Crossword -> Res.string.game_crossword
        GameKind.Line -> Res.string.game_line
    },
)
