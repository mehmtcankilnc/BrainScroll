package com.mehmtcan.brainscroll.game.codebreaker

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.GameLevels
import kotlin.random.Random

/** Makes the boards: a code at random for the level (the length, the colours, whether a colour may come twice). It only depends on the seed. */
object CodeGenerator {
    fun generate(seed: Int, difficulty: Difficulty): CodePuzzle = generate(seed, GameLevels.codeBreaker(difficulty))

    fun generate(seed: Int, shape: GameLevels.CodeShape): CodePuzzle {
        val random = Random(seed)
        val secret = if (shape.repeats) {
            List(shape.length) { random.nextInt(shape.colors) }
        } else {
            (0 until shape.colors).shuffled(random).take(shape.length)
        }
        return CodePuzzle(shape.colors, shape.repeats, secret)
    }
}

/**
 * Helps the player and the guide: the codes that still fit everything the guesses told, in order. A code fits when it would have
 * scored the same as the secret on every guess so far.
 */
object CodeSolver {
    fun candidates(puzzle: CodePuzzle, guesses: List<CodeGuess>): List<List<Int>> =
        CodePuzzle.allCodes(puzzle.colors, puzzle.length, puzzle.repeats).filter { code ->
            guesses.all { CodePuzzle.feedback(code, it.colors) == it.feedback }
        }

    /** The first code that fits what is known: a good next guess. Null only for a state that cannot be reached. */
    fun nextGuess(puzzle: CodePuzzle, guesses: List<CodeGuess>): List<Int>? = candidates(puzzle, guesses).firstOrNull()
}
