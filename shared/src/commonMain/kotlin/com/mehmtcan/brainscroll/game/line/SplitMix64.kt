package com.mehmtcan.brainscroll.game.line

/**
 * A small random number generator with a fixed algorithm (SplitMix64), so the same seed gives the same numbers on every
 * platform and in every later version of the app. The daily board of the one-line game must be the same for everyone,
 * which is why it does not lean on `kotlin.random.Random`.
 */
class SplitMix64(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += GAMMA
        var z = state
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        return z xor (z ushr 31)
    }

    /** A number from 0 until [bound]. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "the bound is positive" }
        return ((nextLong() ushr 1) % bound).toInt()
    }

    fun nextBoolean(): Boolean = nextLong() < 0

    private companion object {
        val GAMMA = 0x9E3779B97F4A7C15UL.toLong()
        val MIX_1 = 0xBF58476D1CE4E5B9UL.toLong()
        val MIX_2 = 0x94D049BB133111EBUL.toLong()
    }
}
