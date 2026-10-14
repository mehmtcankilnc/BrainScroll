package com.mehmtcan.brainscroll.game.line

/**
 * Which board is the daily one: the size grows through the week (docs/tek-cizgi-plan.md) and the seed comes from the
 * day number (see [com.mehmtcan.brainscroll.time.IstanbulDay]), so everyone gets the same board without a server.
 */
object LineSchedule {
    class Spec(val size: Int, val checkpoints: Int)

    /** 0 is Monday ... 6 is Sunday. Day 0 (1970-01-01) was a Thursday. */
    fun weekday(dayIndex: Long): Int = (dayIndex + 3).mod(7L).toInt()

    fun specFor(dayIndex: Long): Spec = when (weekday(dayIndex)) {
        0, 1, 2 -> Spec(size = 5, checkpoints = 4)
        3, 4 -> Spec(size = 6, checkpoints = 5)
        else -> Spec(size = 7, checkpoints = 6)
    }

    fun puzzleFor(dayIndex: Long): LinePuzzle {
        val spec = specFor(dayIndex)
        return LineGenerator.generate(seed = dayIndex xor SEED_SALT, size = spec.size, checkpoints = spec.checkpoints)
    }

    /**
     * The day that is "#1" in what a player shares. A placeholder until the release day is known: set it to the day number
     * (see [com.mehmtcan.brainscroll.time.IstanbulDay.dayIndex]) of the first day the app offers One Line.
     */
    const val LAUNCH_DAY_INDEX = 20_740L

    /** The number of the board of [dayIndex] in what a player shares; never below 1. */
    fun puzzleNumber(dayIndex: Long): Long = (dayIndex - LAUNCH_DAY_INDEX + 1).coerceAtLeast(1)

    private const val SEED_SALT = 0x4C494E45L
}
