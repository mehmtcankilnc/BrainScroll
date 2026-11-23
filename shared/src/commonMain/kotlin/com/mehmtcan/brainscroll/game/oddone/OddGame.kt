package com.mehmtcan.brainscroll.game.oddone

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/** Playing: questions are still to be answered. Won: every question was answered. Lost: a wrong number was picked. */
enum class OddStatus { Playing, Won, Lost }

/** What a pick did. */
enum class OddResult {
    /** Nothing changed (a run that is over, a number that is not on the board). */
    Ignored,

    /** The right number: the next question comes. */
    Correct,

    /** The right number of the last question: the run is won. */
    Perfect,

    /** A wrong number: the run is over. */
    Wrong,
}

/** The question that was just answered, what was picked and whether it was right: the screen shows the rule for a moment. */
data class OddReveal(val question: OddQuestion, val picked: Int, val correct: Boolean)

/** The picture of the run for the UI. */
data class OddState(
    val puzzle: OddPuzzle,
    /** The questions answered right so far (the number of the question being asked, from 0). */
    val cleared: Int,
    /** The question being asked (once the run is over, the one it ended on). */
    val question: OddQuestion,
    /** The last pick, or null before the first one (and after a saved run was put back). */
    val last: OddReveal?,
    val status: OddStatus,
)

/**
 * The rules of odd one out, free of UI and time. The player is shown five numbers and picks the one that does not follow the rule the
 * other four do. A right pick brings the next question; one wrong pick ends the run, and its result is the number of questions answered.
 * Answering the last question right wins.
 */
class OddGame(val puzzle: OddPuzzle) {
    private var cleared = 0
    private var last: OddReveal? = null
    private var status = OddStatus.Playing

    val isFinished: Boolean get() = status != OddStatus.Playing

    val state: OddState
        get() = OddState(puzzle, cleared, puzzle.question(minOf(cleared, puzzle.rounds - 1)), last, status)

    /** Picks the number at [index] (0 to 4) of the question. */
    fun pick(index: Int): OddResult {
        if (isFinished || index !in 0 until 5) return OddResult.Ignored
        val question = puzzle.question(cleared)
        val correct = index == question.oddIndex
        last = OddReveal(question, index, correct)
        if (!correct) {
            status = OddStatus.Lost
            return OddResult.Wrong
        }
        cleared++
        if (cleared >= puzzle.rounds) {
            status = OddStatus.Won
            return OddResult.Perfect
        }
        return OddResult.Correct
    }

    /** Puts back a saved run: [rounds] questions answered. Returns false if that cannot be (below 0, or the run would have been won). */
    fun restore(rounds: Int): Boolean {
        if (rounds < 0 || rounds >= puzzle.rounds) return false
        cleared = rounds
        last = null
        return true
    }
}

/** Makes the runs: only the seed is chosen at random, the level says the rules and the numbers, and how many questions there are. */
object OddGenerator {
    fun generate(seed: Int, difficulty: Difficulty): OddPuzzle = generate(seed, GameLevels.oddOne(difficulty))

    fun generate(seed: Int, shape: GameLevels.OddShape): OddPuzzle = OddPuzzle(shape.level, shape.rounds, Random(seed).nextInt(0, Int.MAX_VALUE))
}
