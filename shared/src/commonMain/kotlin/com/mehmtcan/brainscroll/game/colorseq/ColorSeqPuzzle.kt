package com.mehmtcan.brainscroll.game.colorseq

import kotlin.random.Random

/**
 * A run of the color sequence: [buttons] coloured buttons, and rounds that play [startLength] lights, then one more, and so on up to
 * [targetLength]. Every round plays the beginning of one long sequence that comes from the [seed] (see [sequence]), so a saved run needs
 * only these numbers. [stepMs] is how long every light takes on the screen. The run is saved as text (see [encode]).
 */
class ColorSeqPuzzle(val buttons: Int, val startLength: Int, val targetLength: Int, val stepMs: Int, val seed: Int) {
    init {
        require(buttons in MIN_BUTTONS..MAX_BUTTONS) { "buttons $buttons" }
        require(startLength in 1..targetLength) { "start $startLength" }
        require(targetLength <= MAX_LENGTH) { "target $targetLength" }
        require(stepMs % 50 == 0 && stepMs / 50 in 2..35) { "step $stepMs" }
        require(seed >= 0) { "seed $seed" }
    }

    /** How many rounds there are: clearing all of them wins the run. */
    val rounds: Int get() = targetLength - startLength + 1

    /** How many lights round [round] (from 0) plays. */
    fun lengthIn(round: Int): Int = startLength + round

    /** The whole sequence: the same button never comes three times in a row, so a run of one light is not missed. */
    val sequence: List<Int> by lazy {
        val random = Random(seed.toLong() * 7_919L + 13L)
        val out = ArrayList<Int>(targetLength)
        while (out.size < targetLength) {
            val next = random.nextInt(buttons)
            if (out.size >= 2 && out[out.size - 1] == next && out[out.size - 2] == next) continue
            out += next
        }
        out
    }

    /** The lights that round [round] (from 0) plays: the first [lengthIn] of the sequence. */
    fun steps(round: Int): List<Int> {
        require(round in 0 until rounds) { "round $round" }
        return sequence.take(lengthIn(round))
    }

    /** The number of buttons, the first and last lengths (one digit each, the last in hex), the step (in 50 ms, base 36) and the seed in base 36. */
    fun encode(): String = "$buttons$startLength${targetLength.toString(16)}${(stepMs / 50).toString(36)}${seed.toString(36)}"

    override fun equals(other: Any?) =
        other is ColorSeqPuzzle && buttons == other.buttons && startLength == other.startLength && targetLength == other.targetLength && stepMs == other.stepMs && seed == other.seed

    override fun hashCode() = (((31 * buttons + startLength) * 31 + targetLength) * 31 + stepMs) * 31 + seed

    companion object {
        const val MIN_BUTTONS = 3
        const val MAX_BUTTONS = 6
        const val MAX_LENGTH = 15

        /** Reads what [encode] wrote. Null if it is not a run (so a saved game of another kind starts a new one). */
        fun decode(text: String): ColorSeqPuzzle? {
            if (text.length < 5) return null
            val buttons = text[0].digitToIntOrNull() ?: return null
            val start = text[1].digitToIntOrNull() ?: return null
            val target = text[2].digitToIntOrNull(16) ?: return null
            val step = (text[3].digitToIntOrNull(36) ?: return null) * 50
            val seed = text.substring(4).toIntOrNull(36) ?: return null
            return runCatching { ColorSeqPuzzle(buttons, start, target, step, seed) }.getOrNull()
        }
    }
}
