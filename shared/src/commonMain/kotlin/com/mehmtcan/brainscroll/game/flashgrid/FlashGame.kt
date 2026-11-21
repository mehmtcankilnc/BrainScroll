package com.mehmtcan.brainscroll.game.flashgrid

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/** Playing: tiles are still to be found. Won: every round was cleared. Lost: a tile that was not lit was touched. */
enum class FlashStatus { Playing, Won, Lost }

/** What a touch did. */
enum class FlashResult {
    /** Nothing changed (a tile already found, a tile that is not on the board, a run that is over). */
    Ignored,

    /** A lit tile was found; there are more to find in this round. */
    Found,

    /** The last tile of the round was found: the next round has one tile more. */
    RoundCleared,

    /** The last tile of the last round was found: the run is won. */
    Perfect,

    /** A tile that was not lit was touched: the run is over. */
    Missed,
}

/** The picture of the run for the UI. */
data class FlashState(
    val puzzle: FlashPuzzle,
    /** Whether the player pressed start: until then nothing is lit and nothing can be touched. */
    val started: Boolean,
    /** Goes up every time the screen has to light the tiles of the round: when the run is started, and when a round is cleared. */
    val showToken: Int,
    /** The rounds cleared so far (the number of the round being played, from 0). */
    val roundsCleared: Int,
    /** The tiles lit in the round being played (or, once the run is over, in the round it ended in). */
    val pattern: List<Int>,
    /** The tiles of this round found so far. */
    val found: Set<Int>,
    /** The tile that ended the run, or -1. */
    val missed: Int,
    val status: FlashStatus,
)

/**
 * The rules of the flash grid, free of UI and time. The player is shown a round (the UI lights its tiles for a while), then touches
 * the tiles from memory in any order. All found: the next round, with one more tile. One wrong tile: the run is over, and its result is
 * the number of rounds cleared. Clearing the last round wins. How long the tiles stay lit is the screen's business.
 */
class FlashGame(puzzleStart: FlashPuzzle) {
    var puzzle: FlashPuzzle = puzzleStart
        private set
    private var started = false
    private var token = 0
    private var cleared = 0
    private val found = LinkedHashSet<Int>()
    private var missed = -1
    private var status = FlashStatus.Playing

    val isFinished: Boolean get() = status != FlashStatus.Playing

    val state: FlashState
        get() = FlashState(puzzle, started, token, cleared, puzzle.pattern(minOf(cleared, puzzle.rounds - 1)), found.toSet(), missed, status)

    /** The player pressed start: the first round is lit (the screen does that when [FlashState.showToken] goes up). False if it was started already. */
    fun start(): Boolean {
        if (isFinished || started) return false
        started = true
        token++
        return true
    }

    /**
     * Starts the run over with the new tiles of [fresh]: no rounds cleared, nothing lit, start has to be pressed again. Returns false
     * if the run is over (a lost or won run stays as it is) or nothing was started yet.
     */
    fun restart(fresh: FlashPuzzle): Boolean {
        if (isFinished || !started) return false
        puzzle = fresh
        started = false
        cleared = 0
        found.clear()
        token++
        return true
    }

    fun tap(tile: Int): FlashResult {
        if (isFinished || !started || tile !in 0 until puzzle.size * puzzle.size || tile in found) return FlashResult.Ignored
        val pattern = puzzle.pattern(cleared)
        if (tile !in pattern) {
            missed = tile
            status = FlashStatus.Lost
            return FlashResult.Missed
        }
        found += tile
        if (found.size < pattern.size) return FlashResult.Found
        found.clear()
        cleared++
        token++
        if (cleared >= puzzle.rounds) {
            status = FlashStatus.Won
            return FlashResult.Perfect
        }
        return FlashResult.RoundCleared
    }

    /**
     * Puts back a saved run: [rounds] rounds cleared and whether it was [wasStarted]. The tiles are not shown again (the player may have
     * left to do something else; to see tiles again the run is started over). Returns false if that cannot be (below 0, or the run would
     * have been won, or rounds cleared with no start).
     */
    fun restore(rounds: Int, wasStarted: Boolean): Boolean {
        if (rounds < 0 || rounds >= puzzle.rounds || (rounds > 0 && !wasStarted)) return false
        cleared = rounds
        started = wasStarted
        found.clear()
        return true
    }

    /** New tiles for a run that is started over: the same size and rounds, another seed (a different one for every restart). */
    fun freshPuzzle(): FlashPuzzle =
        FlashPuzzle(puzzle.size, puzzle.startTiles, puzzle.maxTiles, ((puzzle.seed.toLong() * 1_103_515_245L + 12_345L) and 0x7FFFFFFFL).toInt())
}

/** Makes the runs: only the seed is chosen at random, the level says the size and the tiles of the first and last round. */
object FlashGenerator {
    fun generate(seed: Int, difficulty: Difficulty): FlashPuzzle = generate(seed, GameLevels.flashGrid(difficulty))

    fun generate(seed: Int, shape: GameLevels.FlashShape): FlashPuzzle =
        FlashPuzzle(shape.size, shape.startTiles, shape.maxTiles, Random(seed).nextInt(0, Int.MAX_VALUE))
}
