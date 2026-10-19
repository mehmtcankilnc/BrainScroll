package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.moveCount
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome

/** What the statistics screen shows. */
data class Stats(
    val played: Int,
    val won: Int,
    /** How many puzzles were won with 1, 2, ... guesses. Index 0 is "won in 1 guess". */
    val guessDistribution: List<Int>,
    /** The fastest win that has a time. Only daily puzzles are timed (by the server), so it is null for endless. */
    val bestTimeMs: Long? = null,
) {
    /** Share of puzzles that were won, from 0.0 to 1.0. Zero when nothing was played. */
    val winRate: Float get() = if (played == 0) 0f else won.toFloat() / played
}

/**
 * Statistics for one [mode], optionally for one [language]. Everything is computed from [history], so the numbers
 * can never drift away from the saved results.
 *
 * a day streak instead, see [DayStreaks].
 */
fun computeStats(
    history: List<FinishedRound>,
    language: Language? = null,
    maxAttempts: Int = 6,
    mode: Mode = Mode.ENDLESS,
    /** Null counts every game; a game counts only that one. */
    game: GameKind? = null,
): Stats {
    val results = history.filter {
        it.mode == mode && !it.assisted && (language == null || it.language == language) && (game == null || it.game == game)
    }
    val wins = results.filter { it.outcome == Outcome.WON }
    val distribution = MutableList(maxAttempts) { 0 }
    // Guesses per win only means something for word puzzles.
    for (win in wins.filter { it.game == GameKind.Word }) {
        val attempts = win.guesses.size
        if (attempts in 1..maxAttempts) distribution[attempts - 1]++
    }
    return Stats(
        played = results.size,
        won = wins.size,
        guessDistribution = distribution,
        bestTimeMs = wins.filter { it.game == GameKind.Word }.mapNotNull { it.durationMs }.minOrNull(),
    )
}

/** What the statistics screen shows for memory match. */
data class MemoryStats(
    val played: Int,
    val won: Int,
    /** The fastest win (see MemoryRound for how the time is counted). Null until one was won. */
    val bestTimeMs: Long?,
    /** The fewest moves (pairs turned over) in a win. Null until one was won. */
    val fewestMoves: Int?,
) {
    val winRate: Float get() = if (played == 0) 0f else won.toFloat() / played
}

/** Memory statistics from the saved history. A move is two flipped cards, and the flips are saved in the guesses. */
fun computeMemoryStats(history: List<FinishedRound>): MemoryStats {
    val results = history.filter { it.game == GameKind.Memory && it.mode == Mode.ENDLESS && !it.assisted }
    val wins = results.filter { it.outcome == Outcome.WON }
    return MemoryStats(
        played = results.size,
        won = wins.size,
        bestTimeMs = wins.mapNotNull { it.durationMs }.minOrNull(),
        fewestMoves = wins.minOfOrNull { it.moveCount },
    )
}

/** What the statistics screen shows for color tubes. Every tubes result is a win (the game cannot be lost). */
data class TubesStats(
    val played: Int,
    /** The fastest game (see TubesRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** The fewest pours in a game. Null until one was finished. */
    val fewestMoves: Int?,
)

/** What the statistics screen shows for minesweeper. */
data class MinesStats(
    val played: Int,
    val won: Int,
    /** The fastest win (see MinesRound for how the time is counted). Null until one was won. */
    val bestTimeMs: Long?,
) {
    val winRate: Float get() = if (played == 0) 0f else won.toFloat() / played
}

/** What the statistics screen shows for sudoku. */
data class SudokuStats(
    val played: Int,
    val won: Int,
    /** The fastest win (see SudokuRound for how the time is counted). Null until one was won. */
    val bestTimeMs: Long?,
) {
    val winRate: Float get() = if (played == 0) 0f else won.toFloat() / played
}

fun computeSudokuStats(history: List<FinishedRound>): SudokuStats = computeSudokuStats(history, GameKind.Sudoku)

/** The same numbers for the jigsaw sudoku, which plays by the same rules and is counted the same way. */
fun computeJigsawStats(history: List<FinishedRound>): SudokuStats = computeSudokuStats(history, GameKind.Jigsaw)

private fun computeSudokuStats(history: List<FinishedRound>, game: GameKind): SudokuStats {
    val results = history.filter { it.game == game && it.mode == Mode.ENDLESS && !it.assisted }
    val wins = results.filter { it.outcome == Outcome.WON }
    return SudokuStats(played = results.size, won = wins.size, bestTimeMs = wins.mapNotNull { it.durationMs }.minOrNull())
}

fun computeMinesStats(history: List<FinishedRound>): MinesStats {
    val results = history.filter { it.game == GameKind.Mines && it.mode == Mode.ENDLESS && !it.assisted }
    val wins = results.filter { it.outcome == Outcome.WON }
    return MinesStats(played = results.size, won = wins.size, bestTimeMs = wins.mapNotNull { it.durationMs }.minOrNull())
}

/** What the statistics screen shows for Lights Out. Every result is a win (the game cannot be lost). */
data class LightsStats(
    val played: Int,
    /** The fastest game (see LightsRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** The fewest taps in a game. Null until one was finished. */
    val fewestMoves: Int?,
)

fun computeLightsStats(history: List<FinishedRound>): LightsStats {
    val results = history.filter { it.game == GameKind.Lights && it.mode == Mode.ENDLESS && !it.assisted }
    return LightsStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        fewestMoves = results.minOfOrNull { it.moveCount },
    )
}

/** What the statistics screen shows for the sliding puzzle. Every result is a win (the game cannot be lost). */
data class SlideStats(
    val played: Int,
    /** The fastest game (see SlideRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** The fewest moves in a game. Null until one was finished. */
    val fewestMoves: Int?,
)

fun computeSlideStats(history: List<FinishedRound>): SlideStats {
    val results = history.filter { it.game == GameKind.Slide && it.mode == Mode.ENDLESS && !it.assisted }
    return SlideStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        fewestMoves = results.minOfOrNull { it.moveCount },
    )
}

/** What the statistics screen shows for the target number game. Every result is a win (the game cannot be lost). */
data class TargetStats(
    val played: Int,
    /** The fastest game (see TargetRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** The fewest operations in a game. Null until one was finished. */
    val fewestMoves: Int?,
)

fun computeTargetStats(history: List<FinishedRound>): TargetStats {
    val results = history.filter { it.game == GameKind.Target && it.mode == Mode.ENDLESS && !it.assisted }
    return TargetStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        fewestMoves = results.minOfOrNull { it.moveCount },
    )
}

/** What the statistics screen shows for the pipes game. Every result is a win (the game cannot be lost). */
data class PipesStats(
    val played: Int,
    /** The fastest game (see PipesRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** The fewest taps in a game. Null until one was finished. */
    val fewestMoves: Int?,
)

fun computePipesStats(history: List<FinishedRound>): PipesStats {
    val results = history.filter { it.game == GameKind.Pipes && it.mode == Mode.ENDLESS && !it.assisted }
    return PipesStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        fewestMoves = results.minOfOrNull { it.moveCount },
    )
}

/** What the statistics screen shows for the one-line game. Every result is a win (the game cannot be lost). */
data class LineStats(
    val played: Int,
    /** The fastest game (see LineRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** The fewest moves in a game. Null until one was finished. */
    val fewestMoves: Int?,
)

fun computeLineStats(history: List<FinishedRound>): LineStats {
    val results = history.filter { it.game == GameKind.Line && it.mode == Mode.ENDLESS && !it.assisted }
    return LineStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        fewestMoves = results.minOfOrNull { it.moveCount },
    )
}

/** What the statistics screen shows for the tower of Hanoi. Every result is a win (the game cannot be lost). */
data class HanoiStats(
    val played: Int,
    /** The fastest game (see HanoiRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** The fewest moves in a game. Null until one was finished. */
    val fewestMoves: Int?,
)

fun computeHanoiStats(history: List<FinishedRound>): HanoiStats {
    val results = history.filter { it.game == GameKind.Hanoi && it.mode == Mode.ENDLESS && !it.assisted }
    return HanoiStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        fewestMoves = results.minOfOrNull { it.moveCount },
    )
}

/** What the statistics screen shows for the queens puzzle. Every result is a win (the game cannot be lost). */
data class QueensStats(
    val played: Int,
    /** The fastest game (see QueensRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** The fewest moves in a game. Null until one was finished. */
    val fewestMoves: Int?,
)

fun computeQueensStats(history: List<FinishedRound>): QueensStats {
    val results = history.filter { it.game == GameKind.Queens && it.mode == Mode.ENDLESS && !it.assisted }
    return QueensStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        fewestMoves = results.minOfOrNull { it.moveCount },
    )
}

/** What the statistics screen shows for the tents puzzle: the games and the fastest one (the game counts time only). */
data class TentsStats(
    val played: Int,
    /** The fastest game (see TentsRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
)

fun computeTentsStats(history: List<FinishedRound>): TentsStats {
    val results = history.filter { it.game == GameKind.Tents && it.mode == Mode.ENDLESS && !it.assisted }
    return TentsStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
    )
}

/** What the statistics screen shows for block out. Every result is a win (the game cannot be lost). */
data class BlockOutStats(
    val played: Int,
    /** The fastest game (see BlockOutRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** The fewest moves in a game. Null until one was finished. */
    val fewestMoves: Int?,
)

fun computeBlockOutStats(history: List<FinishedRound>): BlockOutStats {
    val results = history.filter { it.game == GameKind.BlockOut && it.mode == Mode.ENDLESS && !it.assisted }
    return BlockOutStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        fewestMoves = results.minOfOrNull { it.moveCount },
    )
}

/** What the statistics screen shows for arrow escape. Every result is a win (the game cannot be lost). */
data class ArrowsStats(
    val played: Int,
    /** The fastest game (see ArrowsRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** Boards cleared without a single mistake. */
    val flawless: Int,
)

fun computeArrowsStats(history: List<FinishedRound>): ArrowsStats {
    val results = history.filter { it.game == GameKind.Arrows && it.mode == Mode.ENDLESS && !it.assisted }
    return ArrowsStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        flawless = results.count { it.moveCount == 0 },
    )
}

/** What the statistics screen shows for axis sum. Every result is a win (the game cannot be lost). */
data class AxisSumStats(
    val played: Int,
    /** The fastest game (see AxisSumRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** The fewest taps a finished board took. */
    val fewestTaps: Int?,
)

fun computeAxisSumStats(history: List<FinishedRound>): AxisSumStats {
    val results = history.filter { it.game == GameKind.AxisSum && it.mode == Mode.ENDLESS && !it.assisted }
    return AxisSumStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        fewestTaps = results.minOfOrNull { it.moveCount },
    )
}

/** What the statistics screen shows for ball slide. Every result is a win (the game cannot be lost). */
data class BallSlideStats(
    val played: Int,
    /** The fastest game (see BallSlideRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** The fewest slides a finished board took. */
    val fewestMoves: Int?,
)

fun computeBallSlideStats(history: List<FinishedRound>): BallSlideStats {
    val results = history.filter { it.game == GameKind.BallSlide && it.mode == Mode.ENDLESS && !it.assisted }
    return BallSlideStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        fewestMoves = results.minOfOrNull { it.moveCount },
    )
}

/** What the statistics screen shows for color flow. Every result is a win (the game cannot be lost). */
data class FlowStats(
    val played: Int,
    /** The fastest game (see FlowRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** The fewest strokes a finished board took. */
    val fewestMoves: Int?,
)

fun computeFlowStats(history: List<FinishedRound>): FlowStats {
    val results = history.filter { it.game == GameKind.ColorFlow && it.mode == Mode.ENDLESS && !it.assisted }
    return FlowStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        fewestMoves = results.minOfOrNull { it.moveCount },
    )
}

/** What the statistics screen shows for the mini nonogram: the games and the fastest one (the game counts time only). */
data class NonogramStats(
    val played: Int,
    /** The fastest game (see NonogramRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
)

fun computeNonogramStats(history: List<FinishedRound>): NonogramStats {
    val results = history.filter { it.game == GameKind.Nonogram && it.mode == Mode.ENDLESS && !it.assisted }
    return NonogramStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
    )
}

fun computeTubesStats(history: List<FinishedRound>): TubesStats {
    val results = history.filter { it.game == GameKind.Tubes && it.mode == Mode.ENDLESS && !it.assisted }
    return TubesStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        fewestMoves = results.minOfOrNull { it.moveCount },
    )
}

/**
 * The results made at [level], for the per-game statistics: the best time of an easy board says nothing about a hard one, so
 * each game's numbers are read for one level at a time. The daily puzzle and the totals use the whole history.
 */
fun List<FinishedRound>.atLevel(level: Difficulty): List<FinishedRound> = filter { it.difficulty == level }
