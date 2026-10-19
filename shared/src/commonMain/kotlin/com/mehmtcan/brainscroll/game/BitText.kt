package com.mehmtcan.brainscroll.game

/**
 * Short texts for saved boards: a list of bits written six to a character (digits, letters, `-` and `_`), so a board of the
 * new games fits the 32 characters the cloud table keeps. The text does not tell how many bits
 * there were; the reader knows how many it wants and ignores the padding.
 */
object BitText {
    private const val DIGITS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_"

    /** Collects bits to write. */
    class Writer {
        private val bits = ArrayList<Int>()

        /** Writes the lowest [count] bits of [value], the highest first. */
        fun put(value: Int, count: Int) {
            for (i in count - 1 downTo 0) bits += (value shr i) and 1
        }

        fun text(): String {
            val padded = ArrayList(bits)
            while (padded.size % 6 != 0) padded += 0
            return (0 until padded.size / 6).map { i -> DIGITS[(0 until 6).fold(0) { acc, j -> acc * 2 + padded[i * 6 + j] }] }.joinToString("")
        }
    }

    /** Reads bits back. [take] gives null when the text has run out. */
    class Reader private constructor(private val bits: List<Int>) {
        private var position = 0

        fun take(count: Int): Int? {
            if (position + count > bits.size) return null
            var value = 0
            repeat(count) { value = value * 2 + bits[position++] }
            return value
        }

        companion object {
            /** Null if [text] has a character that is not one of the digits. */
            fun of(text: String): Reader? {
                val bits = ArrayList<Int>()
                for (ch in text) {
                    val v = DIGITS.indexOf(ch)
                    if (v < 0) return null
                    for (j in 5 downTo 0) bits += (v shr j) and 1
                }
                return Reader(bits)
            }
        }
    }
}
