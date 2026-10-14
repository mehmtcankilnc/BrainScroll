package com.mehmtcan.brainscroll.stats

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.line.LineScoring
import com.mehmtcan.brainscroll.game.line.lineStars
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

fun computeSudokuStats(history: List<FinishedRound>): SudokuStats {
    val results = history.filter { it.game == GameKind.Sudoku && it.mode == Mode.ENDLESS && !it.assisted }
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

/** What the statistics screen shows for the one-line game: only the daily puzzle, and every result is a win. */
data class LineStats(
    val played: Int,
    /** The fastest game (see LineRound for how the time is counted). Null until one was finished. */
    val bestTimeMs: Long?,
    /** How many boards were solved with three stars. */
    val threeStars: Int,
)

fun computeLineStats(history: List<FinishedRound>): LineStats {
    val results = history.filter { it.game == GameKind.Line && it.mode == Mode.DAILY }
    return LineStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        threeStars = results.count { it.lineStars == LineScoring.MAX_STARS },
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

fun computeTubesStats(history: List<FinishedRound>): TubesStats {
    val results = history.filter { it.game == GameKind.Tubes && it.mode == Mode.ENDLESS && !it.assisted }
    return TubesStats(
        played = results.size,
        bestTimeMs = results.mapNotNull { it.durationMs }.minOrNull(),
        fewestMoves = results.minOfOrNull { it.moveCount },
    )
}
