package com.mehmtcan.brainscroll.game.lasers

import com.mehmtcan.brainscroll.game.BitText

/**
 * A board of laser mirrors: a square, a laser that comes in from the left edge on one row and goes right, mirrors on some cells and
 * targets on some cells. The player turns the mirrors (each is a `/` or a `\`) so that the beam passes through every target.
 *
 * [mirrors] has one number for each cell, row by row from the top left: 0 for no mirror, [LaserRules.SLASH] or [LaserRules.BACKSLASH]
 * for a mirror as it is when the board is given. [targets] are the cells with a target (a mirror and a target never share a cell).
 *
 * [encode] is the saved text: the size, the row of the laser, then two bits for each cell (the mirror) and one bit for each cell (the
 * target), six bits to a character: 26 characters for the biggest board.
 */
class LaserPuzzle(val size: Int, val row: Int, val mirrors: List<Int>, val targets: Set<Int>) {
    init {
        require(size in MIN_SIZE..MAX_SIZE) { "a small board" }
        require(row in 0 until size) { "the laser is on the board" }
        require(mirrors.size == size * size) { "a number for every cell" }
        require(mirrors.all { it in 0..2 }) { "a mirror is / or \\" }
        require(targets.all { it in 0 until size * size && mirrors[it] == 0 }) { "targets are on empty cells of the board" }
        require(targets.isNotEmpty()) { "something to light" }
    }

    /** The cells that hold a mirror, in order. */
    val mirrorCells: List<Int> = mirrors.indices.filter { mirrors[it] != 0 }

    fun encode(): String {
        val writer = BitText.Writer()
        writer.put(size - MIN_SIZE, 2)
        writer.put(row, 3)
        for (m in mirrors) writer.put(m, 2)
        for (cell in mirrors.indices) writer.put(if (cell in targets) 1 else 0, 1)
        return writer.text()
    }

    override fun equals(other: Any?) = other is LaserPuzzle && other.size == size && other.row == row && other.mirrors == mirrors && other.targets == targets

    override fun hashCode() = 31 * (31 * (31 * size + row) + mirrors.hashCode()) + targets.hashCode()

    companion object {
        const val MIN_SIZE = 4
        const val MAX_SIZE = 7

        /** Null if [text] is not a board that [encode] wrote. */
        fun decode(text: String): LaserPuzzle? {
            val reader = BitText.Reader.of(text) ?: return null
            val size = (reader.take(2) ?: return null) + MIN_SIZE
            val row = reader.take(3) ?: return null
            val mirrors = ArrayList<Int>()
            repeat(size * size) { mirrors += reader.take(2) ?: return null }
            val targets = HashSet<Int>()
            for (cell in 0 until size * size) if ((reader.take(1) ?: return null) == 1) targets += cell
            return try {
                LaserPuzzle(size, row, mirrors, targets)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}

/** A point of the beam, in cells: (0, 0) is the top left corner of the board, the middle of a cell is at .5. */
data class BeamPoint(val x: Float, val y: Float)

/** The way the beam goes: the corners of its line, and the cells it passes through. */
data class Beam(val points: List<BeamPoint>, val lit: Set<Int>)

/** The rules of the mirrors: where the beam goes for a given turn of every mirror. */
object LaserRules {
    const val SLASH = 1
    const val BACKSLASH = 2

    /** The other way a mirror can stand. */
    fun turned(mirror: Int): Int = when (mirror) {
        SLASH -> BACKSLASH
        BACKSLASH -> SLASH
        else -> mirror
    }

    /** The direction (rows, columns) a beam going (dr, dc) takes after a mirror: a `/` sends right to up, a `\` right to down. */
    fun reflect(dr: Int, dc: Int, mirror: Int): Pair<Int, Int> = if (mirror == SLASH) -dc to -dr else dc to dr

    /** The beam of a board with [mirrors] as they stand now. It starts left of the first cell and goes until it leaves the board. */
    fun trace(size: Int, row: Int, mirrors: List<Int>): Beam {
        var r = row
        var c = 0
        var dr = 0
        var dc = 1
        val points = arrayListOf(BeamPoint(-0.45f, row + 0.5f))
        val lit = HashSet<Int>()
        // A beam cannot go round in circles (a mirror turns it the same way back), but a limit keeps a broken board from hanging.
        repeat(size * size * 4) {
            lit += r * size + c
            val mirror = mirrors[r * size + c]
            if (mirror != 0) {
                points += BeamPoint(c + 0.5f, r + 0.5f)
                val next = reflect(dr, dc, mirror)
                dr = next.first
                dc = next.second
            }
            val nr = r + dr
            val nc = c + dc
            if (nr < 0 || nc < 0 || nr >= size || nc >= size) {
                points += BeamPoint(c + 0.5f + dc * 0.5f, r + 0.5f + dr * 0.5f)
                return Beam(points, lit)
            }
            r = nr
            c = nc
        }
        return Beam(points, lit)
    }

    /** True if the beam passes through every target. */
    fun lightsAll(size: Int, row: Int, mirrors: List<Int>, targets: Set<Int>): Boolean = targets.all { it in trace(size, row, mirrors).lit }

    /**
     * The fewest mirrors to turn from [mirrors] so that every target is lit, and which turn of every mirror that is (the mirrors of
     * [LaserPuzzle.mirrorCells] in order, `true` for the other way than they stand now). Null if no turn of the mirrors does it. The
     * search tries every way the mirrors can stand, which is fine for the dozen mirrors of a board.
     */
    fun fewestTurns(puzzleSize: Int, row: Int, mirrors: List<Int>, targets: Set<Int>): List<Boolean>? {
        val cells = mirrors.indices.filter { mirrors[it] != 0 }
        var best: Int? = null
        var bestMask = 0
        val work = mirrors.toMutableList()
        for (mask in 0 until (1 shl cells.size)) {
            for ((i, cell) in cells.withIndex()) work[cell] = if ((mask shr i) and 1 == 1) turned(mirrors[cell]) else mirrors[cell]
            val turns = mask.countOneBits()
            if (best != null && turns >= best) continue
            if (lightsAll(puzzleSize, row, work, targets)) {
                best = turns
                bestMask = mask
            }
        }
        return if (best == null) null else cells.indices.map { (bestMask shr it) and 1 == 1 }
    }
}
