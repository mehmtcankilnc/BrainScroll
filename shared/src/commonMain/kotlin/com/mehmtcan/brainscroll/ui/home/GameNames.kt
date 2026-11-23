package com.mehmtcan.brainscroll.ui.home

import androidx.compose.runtime.Composable
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.game_matchsticks
import brainscroll.shared.generated.resources.game_lasers
import brainscroll.shared.generated.resources.game_untangle
import brainscroll.shared.generated.resources.game_codebreaker
import brainscroll.shared.generated.resources.game_fences
import brainscroll.shared.generated.resources.game_greaterless
import brainscroll.shared.generated.resources.game_lamps
import brainscroll.shared.generated.resources.game_lonenumber
import brainscroll.shared.generated.resources.game_crosssum
import brainscroll.shared.generated.resources.game_mirror
import brainscroll.shared.generated.resources.game_oddone
import brainscroll.shared.generated.resources.game_zerosones
import brainscroll.shared.generated.resources.game_rectangles
import brainscroll.shared.generated.resources.game_colorseq
import brainscroll.shared.generated.resources.game_cubefold
import brainscroll.shared.generated.resources.game_flashgrid
import brainscroll.shared.generated.resources.game_fruitmath
import brainscroll.shared.generated.resources.game_hanoi
import brainscroll.shared.generated.resources.game_jigsaw
import brainscroll.shared.generated.resources.difficulty_easy
import brainscroll.shared.generated.resources.difficulty_hard
import brainscroll.shared.generated.resources.difficulty_medium
import brainscroll.shared.generated.resources.game_arrows
import brainscroll.shared.generated.resources.game_axissum
import brainscroll.shared.generated.resources.game_ballslide
import brainscroll.shared.generated.resources.game_colorflow
import brainscroll.shared.generated.resources.game_topicgroups
import brainscroll.shared.generated.resources.game_nonogram
import brainscroll.shared.generated.resources.game_blockout
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
import com.mehmtcan.brainscroll.game.Difficulty
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
        GameKind.Arrows -> Res.string.game_arrows
        GameKind.Nonogram -> Res.string.game_nonogram
        GameKind.AxisSum -> Res.string.game_axissum
        GameKind.BallSlide -> Res.string.game_ballslide
        GameKind.ColorFlow -> Res.string.game_colorflow
        GameKind.TopicGroups -> Res.string.game_topicgroups
        GameKind.Matchsticks -> Res.string.game_matchsticks
        GameKind.Lasers -> Res.string.game_lasers
        GameKind.Untangle -> Res.string.game_untangle
        GameKind.FruitMath -> Res.string.game_fruitmath
        GameKind.CodeBreaker -> Res.string.game_codebreaker
        GameKind.Rectangles -> Res.string.game_rectangles
        GameKind.FlashGrid -> Res.string.game_flashgrid
        GameKind.ColorSeq -> Res.string.game_colorseq
        GameKind.CubeFold -> Res.string.game_cubefold
        GameKind.LoneNumber -> Res.string.game_lonenumber
        GameKind.ZerosOnes -> Res.string.game_zerosones
        GameKind.Lamps -> Res.string.game_lamps
        GameKind.GreaterLess -> Res.string.game_greaterless
        GameKind.Fences -> Res.string.game_fences
        GameKind.Mirror -> Res.string.game_mirror
        GameKind.OddOne -> Res.string.game_oddone
        GameKind.CrossSum -> Res.string.game_crosssum
        GameKind.Line -> Res.string.game_line
    },
)

/** The name of a level in the language of the app. */
@Composable
fun difficultyName(difficulty: Difficulty): String = stringResource(
    when (difficulty) {
        Difficulty.Easy -> Res.string.difficulty_easy
        Difficulty.Medium -> Res.string.difficulty_medium
        Difficulty.Hard -> Res.string.difficulty_hard
    },
)
