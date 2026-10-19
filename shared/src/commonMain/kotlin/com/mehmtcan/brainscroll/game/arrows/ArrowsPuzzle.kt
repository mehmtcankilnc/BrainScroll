package com.mehmtcan.brainscroll.game.arrows

/**
 * One arrow of the board: a path of neighbouring cells from the tail to the head ([cells], at least one), and the way the head
 * points ([dir], see [ArrowDir]). With two cells or more the head points the way the last step went, so [dir] is not free there.
 * The arrow leaves by sliding along its own path and on, straight out of the board the way the head points.
 */
class ArrowPath(val cells: List<Int>, val dir: Int) {
    val head: Int get() = cells.last()
    val length: Int get() = cells.size

    override fun equals(other: Any?) = other is ArrowPath && other.cells == cells && other.dir == dir
    override fun hashCode() = cells.hashCode() * 31 + dir
}

/**
 * A board of arrow escape: the arrows with their paths. It is the whole puzzle, and a board is valid only if the arrows can all
 * leave one after the other (see [ArrowsRules.clearOrder]).
 *
 * [encode] writes it as a short text: `s` and then bits written 6 to a character: the size, the number of
 * arrows, and for each arrow its tail cell, its length, and its steps (one direction for a one cell arrow). It stays under the 32
 * characters the cloud table allows. An older text (`6:1k2f4m9a:o3b8cu1x`, one arrow to a cell) is still read.
 */
class ArrowsPuzzle(val size: Int, val arrows: List<ArrowPath>) {
    /** The arrow on each cell, or -1. */
    val owner: IntArray = IntArray(size * size) { -1 }

    init {
        require(size in MIN_SIZE..MAX_SIZE) { "a small board" }
        require(arrows.isNotEmpty() && arrows.size <= MAX_ARROWS) { "a few arrows" }
        arrows.forEachIndexed { index, arrow ->
            require(arrow.length in 1..MAX_LENGTH) { "a short arrow" }
            require(arrow.dir in 0..3) { "a known direction" }
            arrow.cells.forEachIndexed { i, cell ->
                require(cell in 0 until size * size) { "on the board" }
                require(owner[cell] == -1) { "arrows do not share a cell" }
                owner[cell] = index
                if (i > 0) require(stepDir(size, arrow.cells[i - 1], cell) >= 0) { "neighbouring cells" }
            }
            if (arrow.length > 1) require(arrow.dir == stepDir(size, arrow.cells[arrow.length - 2], arrow.head)) { "the head points the way it came" }
        }
        require(ArrowsRules.clearOrder(this, BooleanArray(arrows.size) { true }).second.isEmpty()) { "every arrow can leave" }
    }

    val arrowCount: Int get() = arrows.size

    fun encode(): String {
        val bits = ArrayList<Int>()
        fun put(value: Int, count: Int) {
            for (i in count - 1 downTo 0) bits += (value shr i) and 1
        }
        put(size - MIN_SIZE, 2)
        put(arrows.size, 4)
        for (arrow in arrows) {
            put(arrow.cells.first(), 6)
            put(arrow.length - 1, 3)
            if (arrow.length == 1) put(arrow.dir, 2) else for (i in 1 until arrow.length) put(stepDir(size, arrow.cells[i - 1], arrow.cells[i]), 2)
        }
        while (bits.size % 6 != 0) bits += 0
        return "s" + (0 until bits.size / 6).map { i -> DIGITS[(0 until 6).fold(0) { acc, j -> acc * 2 + bits[i * 6 + j] }] }.joinToString("")
    }

    companion object {
        const val DEFAULT_SIZE = 6
        const val MIN_SIZE = 3
        const val MAX_SIZE = 6
        const val MAX_ARROWS = 15
        const val MAX_LENGTH = 8

        private const val DIGITS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_"

        /** The direction of the step from [from] to the neighbouring cell [to], or -1 if they are not neighbours. */
        fun stepDir(size: Int, from: Int, to: Int): Int {
            val dr = to / size - from / size
            val dc = to % size - from % size
            return (0 until 4).firstOrNull { ArrowDir.dRow[it] == dr && ArrowDir.dColumn[it] == dc } ?: -1
        }

        /** Null if [text] is not a board that [encode] wrote (or not a board that can be cleared). */
        fun decode(text: String): ArrowsPuzzle? = try {
            if (text.startsWith("s")) decodeBits(text) else decodeLegacy(text)
        } catch (e: IllegalArgumentException) {
            null
        }

        private fun decodeBits(text: String): ArrowsPuzzle? {
            val bits = ArrayList<Int>()
            for (ch in text.substring(1)) {
                val v = DIGITS.indexOf(ch)
                if (v < 0) return null
                for (j in 5 downTo 0) bits += (v shr j) and 1
            }
            var pos = 0
            fun take(count: Int): Int? {
                if (pos + count > bits.size) return null
                var v = 0
                repeat(count) { v = v * 2 + bits[pos++] }
                return v
            }
            val size = (take(2) ?: return null) + MIN_SIZE
            val count = take(4) ?: return null
            val arrows = ArrayList<ArrowPath>()
            repeat(count) {
                val tail = take(6) ?: return null
                val length = (take(3) ?: return null) + 1
                if (length == 1) {
                    arrows += ArrowPath(listOf(tail), take(2) ?: return null)
                } else {
                    val cells = arrayListOf(tail)
                    var dir = 0
                    repeat(length - 1) {
                        dir = take(2) ?: return null
                        val row = cells.last() / size + ArrowDir.dRow[dir]
                        val column = cells.last() % size + ArrowDir.dColumn[dir]
                        if (row !in 0 until size || column !in 0 until size) return null
                        cells += row * size + column
                    }
                    arrows += ArrowPath(cells, dir)
                }
            }
            return ArrowsPuzzle(size, arrows)
        }

        private fun unpack(text: String, count: Int): List<Int>? {
            var value = text.toLongOrNull(36) ?: return null
            if (value < 0) return null
            val out = IntArray(count)
            for (i in count - 1 downTo 0) {
                out[i] = (value % 5).toInt() - 1
                value /= 5
            }
            return if (value == 0L) out.toList() else null
        }

        /** The first kind of board: a direction per cell (or -1), every arrow one cell long. */
        private fun decodeLegacy(text: String): ArrowsPuzzle? {
            val parts = text.split(':')
            if (parts.size != 3) return null
            val size = parts[0].toIntOrNull() ?: return null
            if (size !in MIN_SIZE..MAX_SIZE) return null
            val total = size * size
            val half = (total + 1) / 2
            val dirs = (unpack(parts[1], half) ?: return null) + (unpack(parts[2], total - half) ?: return null)
            return ArrowsPuzzle(size, dirs.indices.filter { dirs[it] >= 0 }.map { ArrowPath(listOf(it), dirs[it]) })
        }
    }
}
