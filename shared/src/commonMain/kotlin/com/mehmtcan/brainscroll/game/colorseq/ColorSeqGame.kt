package com.mehmtcan.brainscroll.game.colorseq

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/** Playing: lights are still to be repeated. Won: every round was cleared. Lost: a wrong button was touched. */
enum class ColorSeqStatus { Playing, Won, Lost }

/** What a touch did. */
enum class ColorSeqResult {
    /** Nothing changed (a run that is not started, or is over, or a button that is not on the board). */
    Ignored,

    /** The right button: there are more lights to repeat in this round. */
    Right,

    /** The last light of the round was repeated: the next round has one light more. */
    RoundCleared,

    /** The last light of the last round was repeated: the run is won. */
    Perfect,

    /** A wrong button was touched: the run is over. */
    Missed,
}

/** The picture of the run for the UI. */
data class ColorSeqState(
    val puzzle: ColorSeqPuzzle,
    /** Whether the player pressed start: until then nothing is played and nothing can be touched. */
    val started: Boolean,
    /** Goes up every time the screen has to play the lights of the round: when the run is started, and when a round is cleared. */
    val showToken: Int,
    /** The rounds cleared so far (the number of the round being played, from 0). */
    val roundsCleared: Int,
    /** The lights of the round being played (or, once the run is over, of the round it ended in). */
    val sequence: List<Int>,
    /** How many lights of this round were repeated right so far. */
    val entered: Int,
    /** The wrong button that ended the run, or -1. */
    val missed: Int,
    /** The button that was due when the run ended, or -1. */
    val expected: Int,
    val status: ColorSeqStatus,
)

/**
 * The rules of the color sequence, free of UI and time. The screen plays a round (its lights, one after the other), then the player
 * touches the buttons in the same order from memory. All right: the next round, with one light more. One wrong button: the run is over,
 * and its result is the number of rounds cleared. Clearing the last round wins. How long a light takes is the screen's business.
 */
class ColorSeqGame(puzzleStart: ColorSeqPuzzle) {
    var puzzle: ColorSeqPuzzle = puzzleStart
        private set
    private var started = false
    private var token = 0
    private var cleared = 0
    private var entered = 0
    private var missed = -1
    private var expected = -1
    private var status = ColorSeqStatus.Playing

    val isFinished: Boolean get() = status != ColorSeqStatus.Playing

    val state: ColorSeqState
        get() = ColorSeqState(puzzle, started, token, cleared, puzzle.steps(minOf(cleared, puzzle.rounds - 1)), entered, missed, expected, status)

    /** The player pressed start: the first round is played (the screen does that when [ColorSeqState.showToken] goes up). False if it was started already. */
    fun start(): Boolean {
        if (isFinished || started) return false
        started = true
        token++
        return true
    }

    /**
     * Starts the run over with the new lights of [fresh]: no rounds cleared, nothing played, start has to be pressed again. Returns false
     * if the run is over (a lost or won run stays as it is) or nothing was started yet.
     */
    fun restart(fresh: ColorSeqPuzzle): Boolean {
        if (isFinished || !started) return false
        puzzle = fresh
        started = false
        cleared = 0
        entered = 0
        token++
        return true
    }

    fun tap(button: Int): ColorSeqResult {
        if (isFinished || !started || button !in 0 until puzzle.buttons) return ColorSeqResult.Ignored
        val steps = puzzle.steps(cleared)
        if (button != steps[entered]) {
            missed = button
            expected = steps[entered]
            status = ColorSeqStatus.Lost
            return ColorSeqResult.Missed
        }
        entered++
        if (entered < steps.size) return ColorSeqResult.Right
        entered = 0
        cleared++
        token++
        if (cleared >= puzzle.rounds) {
            status = ColorSeqStatus.Won
            return ColorSeqResult.Perfect
        }
        return ColorSeqResult.RoundCleared
    }

    /**
     * Puts back a saved run: [rounds] rounds cleared and whether it was [wasStarted]. The lights are not played again (the player may have
     * left to do something else; to see lights again the run is started over). Returns false if that cannot be (below 0, or the run would
     * have been won, or rounds cleared with no start).
     */
    fun restore(rounds: Int, wasStarted: Boolean): Boolean {
        if (rounds < 0 || rounds >= puzzle.rounds || (rounds > 0 && !wasStarted)) return false
        cleared = rounds
        started = wasStarted
        entered = 0
        return true
    }

    /** A new sequence for a run that is started over: the same buttons, rounds and speed, another seed (a different one for every restart). */
    fun freshPuzzle(): ColorSeqPuzzle =
        ColorSeqPuzzle(puzzle.buttons, puzzle.startLength, puzzle.targetLength, puzzle.stepMs, ((puzzle.seed.toLong() * 1_103_515_245L + 12_345L) and 0x7FFFFFFFL).toInt())
}

/** Makes the runs: only the seed is chosen at random, the level says the buttons, the lengths and how fast the lights go. */
object ColorSeqGenerator {
    fun generate(seed: Int, difficulty: Difficulty): ColorSeqPuzzle = generate(seed, GameLevels.colorSeq(difficulty))

    fun generate(seed: Int, shape: GameLevels.ColorSeqShape): ColorSeqPuzzle =
        ColorSeqPuzzle(shape.buttons, shape.startLength, shape.targetLength, shape.stepMs, Random(seed).nextInt(0, Int.MAX_VALUE))
}
