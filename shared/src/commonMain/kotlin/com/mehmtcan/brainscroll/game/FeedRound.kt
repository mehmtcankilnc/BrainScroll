package com.mehmtcan.brainscroll.game

import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.StoredRound

/** The games that can appear in the feed. [id] is what is stored in the database (`game` column). */
enum class GameKind(val id: String) {
    Word("word"),
    Memory("memory"),
    Tubes("tubes"),
    Mines("mines"),
    Sudoku("sudoku"),
    Lights("lights"),
    Slide("slide"),
    ;

    companion object {
        /** Null for a game this version of the app does not know (a row written by a newer version). */
        fun fromId(id: String): GameKind? = entries.firstOrNull { it.id == id }
    }
}

/**
 * How many moves a finished result took, whatever the game: a memory move is two flipped cards (the flips are the
 * guesses), a tubes, minesweeper, sudoku, lights or slide result keeps its number of moves as its only guess. Word puzzles count guesses.
 */
val FinishedRound.moveCount: Int
    get() = when (game) {
        GameKind.Word -> guesses.size
        GameKind.Memory -> guesses.size / 2
        GameKind.Tubes, GameKind.Mines, GameKind.Sudoku, GameKind.Lights, GameKind.Slide -> guesses.firstOrNull()?.toIntOrNull() ?: 0
    }

/**
 * What every screen of the feed needs to know about a page, whatever the game: which one it is, whether it is
 * over, and whether the player left it once. The game specific parts are in the implementations
 * ([com.mehmtcan.brainscroll.game.wordle.RoundSnapshot], [com.mehmtcan.brainscroll.game.memory.MemorySnapshot]).
 */
interface PageSnapshot {
    /** Stable id of the puzzle; also the id of its saved result (favorites use it). */
    val id: String
    val index: Int
    val skipped: Boolean
    val isFinished: Boolean
    val kind: GameKind
}

/**
 * One page of the feed: a puzzle with its own state. The feed treats all games alike for skipping, saving and
 * the answer streak, and only the game itself knows its moves. Mutable on purpose (a round is the single owner of
 * its state); [snapshot] hands out immutable copies for the UI.
 */
interface FeedRound {
    val kind: GameKind
    val index: Int
    val id: String
    val skipped: Boolean
    val isFinished: Boolean

    /** True while nothing has been done yet, so the page can still be swapped for another puzzle. */
    val isUntouched: Boolean

    /** Called when the player left this puzzle forward unfinished. */
    fun markSkipped()

    fun snapshot(): PageSnapshot

    /** What has to be saved to rebuild this round later. */
    fun toStored(): StoredRound

    /** The history row of a finished round. */
    fun toFinished(finishedAt: Long): FinishedRound
}
