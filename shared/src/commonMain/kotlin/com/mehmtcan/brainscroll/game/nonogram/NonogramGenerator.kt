package com.mehmtcan.brainscroll.game.nonogram

import com.mehmtcan.brainscroll.game.Difficulty
import kotlin.random.Random

/**
 * Picks the board of a mini nonogram: one of the drawn pictures (see tools/nonogram), turned left to right or not, which
 * changes the numbers. The pictures are all solvable by logic alone (a test checks every one). The board depends only on the seed,
 * but it is saved as the board itself, so a later change of the list does not change a saved game.
 */
object NonogramGenerator {
    /** The size of the board of a level: easy 5 by 5, medium 8 by 8 (as it always was), hard 10 by 10. */
    fun sizeFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.Easy -> 5
        Difficulty.Medium -> NonogramPuzzle.DEFAULT_SIZE
        Difficulty.Hard -> 10
    }

    /** A board at [difficulty]. Medium is the board [generate] always made for a seed. */
    fun generate(seed: Int, difficulty: Difficulty): NonogramPuzzle = generate(seed, sizeFor(difficulty))

    fun generate(seed: Int, size: Int = NonogramPuzzle.DEFAULT_SIZE): NonogramPuzzle {
        val random = Random(seed)
        val pictures = NonogramPictures.of(size)
        require(pictures.isNotEmpty()) { "pictures were drawn for this size" }
        return NonogramPuzzle.of(pictures[random.nextInt(pictures.size)], random.nextBoolean())
    }
}
