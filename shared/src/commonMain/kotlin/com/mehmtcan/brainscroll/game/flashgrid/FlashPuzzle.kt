package com.mehmtcan.brainscroll.game.flashgrid

import kotlin.random.Random

/**
 * A run of the flash grid: a square of [size] by [size] tiles, and rounds that light [startTiles] tiles, then one more, and so on up to
 * [maxTiles]. The tiles of every round come from the [seed] (see [pattern]), so a saved run needs only these numbers. The run is saved as
 * text (see [encode]).
 */
class FlashPuzzle(val size: Int, val startTiles: Int, val maxTiles: Int, val seed: Int) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "size $size" }
        require(startTiles in 1..maxTiles) { "start $startTiles" }
        require(maxTiles < size * size) { "max $maxTiles" }
        require(maxTiles <= MAX_TILES) { "max $maxTiles" }
        require(seed >= 0) { "seed $seed" }
    }

    /** How many rounds there are: clearing all of them wins the run. */
    val rounds: Int get() = maxTiles - startTiles + 1

    /** How many tiles round [round] (from 0) lights. */
    fun tilesIn(round: Int): Int = startTiles + round

    /** The tiles (numbered row * size + column) that round [round] (from 0) lights, in order of the cells. */
    fun pattern(round: Int): List<Int> {
        require(round in 0 until rounds) { "round $round" }
        return (0 until size * size).shuffled(Random(seed.toLong() * 7_919L + round)).take(tilesIn(round)).sorted()
    }

    /** The side, the first and last numbers of tiles (one digit each, the last in hex) and the seed in base 36. */
    fun encode(): String = "$size$startTiles${maxTiles.toString(16)}${seed.toString(36)}"

    override fun equals(other: Any?) = other is FlashPuzzle && size == other.size && startTiles == other.startTiles && maxTiles == other.maxTiles && seed == other.seed

    override fun hashCode() = ((31 * size + startTiles) * 31 + maxTiles) * 31 + seed

    companion object {
        const val MIN_SIZE = 3
        const val MAX_SIZE = 6
        const val MAX_TILES = 15

        /** Reads what [encode] wrote. Null if it is not a run (so a saved game of another kind starts a new one). */
        fun decode(text: String): FlashPuzzle? {
            if (text.length < 4) return null
            val size = text[0].digitToIntOrNull() ?: return null
            val start = text[1].digitToIntOrNull() ?: return null
            val max = text[2].digitToIntOrNull(16) ?: return null
            val seed = text.substring(3).toIntOrNull(36) ?: return null
            return runCatching { FlashPuzzle(size, start, max, seed) }.getOrNull()
        }
    }
}
