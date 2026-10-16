package com.mehmtcan.brainscroll.game.line

/** Fixed boards for the tests: the same board every run, made by the generator the feed uses. */
object LineFixtures {
    /** A 5x5 board with one line. */
    val board: LinePuzzle = LineGenerator.generate(seed = 20_740L xor 0x4C494E45L, size = 5, checkpoints = 4)

    /** A 6x6 board with one line. */
    val bigger: LinePuzzle = LineGenerator.generate(seed = 20_741L xor 0x4C494E45L, size = 6, checkpoints = 5)
}
