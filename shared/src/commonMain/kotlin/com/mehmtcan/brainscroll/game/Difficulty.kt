package com.mehmtcan.brainscroll.game

/**
 * How hard the boards of a game are. [Medium] is the game as it was before levels existed: every saved
 * round and result without a level is a medium one. [id] is what is stored (`difficulty` column).
 */
enum class Difficulty(val id: Int) {
    Easy(0),
    Medium(1),
    Hard(2),
    ;

    companion object {
        val DEFAULT = Medium

        /** A level this version of the app does not know (written by a newer one) reads as [Medium]. */
        fun fromId(id: Int?): Difficulty = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * The games whose boards come in levels. The level choice only shows for these; a game joins the set when
 * its generator takes the level, so the screen never offers a choice that does nothing.
 */
val LEVELLED_GAMES: Set<GameKind> = setOf(
    GameKind.Word,
    GameKind.Memory, GameKind.Hanoi, GameKind.Pipes, GameKind.Queens, GameKind.Arrows,
    GameKind.Slide, GameKind.Lights, GameKind.Target, GameKind.Mines, GameKind.Tubes,
    GameKind.Sudoku, GameKind.Jigsaw, GameKind.Tents, GameKind.BlockOut, GameKind.Nonogram, GameKind.AxisSum, GameKind.BallSlide, GameKind.ColorFlow,
)
