package com.mehmtcan.brainscroll.game.cubefold

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/** Playing: questions are still to be answered. Won: every question was answered. Lost: a wrong cube was picked. */
enum class CubeStatus { Playing, Won, Lost }

/** What a pick did. */
enum class CubeResult {
    /** Nothing changed (a run that is over, an option that is not there). */
    Ignored,

    /** The right cube: the next question comes. */
    Correct,

    /** The right cube of the last question: the run is won. */
    Perfect,

    /** A wrong cube: the run is over. */
    Wrong,
}

/** The question that was just answered and what was picked: the screen shows the right cube and why the other one is wrong for a moment. */
data class CubeReveal(val question: CubeQuestion, val picked: Int, val correct: Boolean)

/** The picture of the run for the UI. */
data class CubeState(
    val puzzle: CubeFoldPuzzle,
    /** The questions answered right so far (the number of the question being asked, from 0). */
    val cleared: Int,
    /** The question being asked (once the run is over, the one it ended on). */
    val question: CubeQuestion,
    /** The last pick, or null before the first one (and after a saved run was put back). */
    val last: CubeReveal?,
    val status: CubeStatus,
)

/**
 * The rules of cube folding, free of UI and time. The player is shown a net and some cubes seen from a corner, and picks the cube that
 * the net folds to. A right pick brings the next question; one wrong pick ends the run, and its result is the number of questions
 * answered. Answering the last question right wins.
 */
class CubeFoldGame(val puzzle: CubeFoldPuzzle) {
    private var cleared = 0
    private var last: CubeReveal? = null
    private var status = CubeStatus.Playing

    val isFinished: Boolean get() = status != CubeStatus.Playing

    val state: CubeState
        get() = CubeState(puzzle, cleared, puzzle.question(minOf(cleared, puzzle.rounds - 1)), last, status)

    /** Picks the cube at [index] of the question. */
    fun pick(index: Int): CubeResult {
        val question = puzzle.question(minOf(cleared, puzzle.rounds - 1))
        if (isFinished || index !in question.options.indices) return CubeResult.Ignored
        val correct = index == question.correct
        last = CubeReveal(question, index, correct)
        if (!correct) {
            status = CubeStatus.Lost
            return CubeResult.Wrong
        }
        cleared++
        if (cleared >= puzzle.rounds) {
            status = CubeStatus.Won
            return CubeResult.Perfect
        }
        return CubeResult.Correct
    }

    /** Puts back a saved run: [rounds] questions answered. Returns false if that cannot be (below 0, or the run would have been won). */
    fun restore(rounds: Int): Boolean {
        if (rounds < 0 || rounds >= puzzle.rounds) return false
        cleared = rounds
        last = null
        return true
    }
}

/** Makes the runs: only the seed is chosen at random, the level says how many cubes there are to choose from and how many questions. */
object CubeFoldGenerator {
    fun generate(seed: Int, difficulty: Difficulty): CubeFoldPuzzle = generate(seed, GameLevels.cubeFold(difficulty))

    fun generate(seed: Int, shape: GameLevels.CubeShape): CubeFoldPuzzle = CubeFoldPuzzle(shape.options, shape.rounds, Random(seed).nextInt(0, Int.MAX_VALUE))
}
