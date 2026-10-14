package com.mehmtcan.brainscroll.game.hanoi

/**
 * Finds the fewest moves of the tower of Hanoi from any position: a breadth-first search over the positions (3 to the power of
 * the number of disks, 81 for four disks), which is as exact as it gets and quick for the sizes in use.
 */
object HanoiSolver {

    /** The smallest disk on [tower], which is the one that can move, or -1 if the tower is empty. */
    fun topDisk(pegs: IntArray, tower: Int): Int {
        for (disk in pegs.indices) if (pegs[disk] == tower) return disk
        return -1
    }

    /** True if [disk] (the top of its tower) may be put on [to]: that tower is empty or its top disk is bigger. */
    fun canGo(pegs: IntArray, disk: Int, to: Int): Boolean {
        if (pegs[disk] == to) return false
        val top = topDisk(pegs, to)
        return top < 0 || top > disk
    }

    /** The legal moves from a position, as the pairs of towers. */
    fun legalMoves(pegs: IntArray): List<HanoiMove> {
        val result = ArrayList<HanoiMove>(6)
        for (from in 0 until HanoiGame.TOWERS) {
            val disk = topDisk(pegs, from)
            if (disk < 0) continue
            for (to in 0 until HanoiGame.TOWERS) if (canGo(pegs, disk, to)) result += HanoiMove(from, to)
        }
        return result
    }

    /** A shortest list of moves that puts every disk on tower [goal]; empty if they already are. Never null (every position can be solved). */
    fun shortest(pegs: IntArray, goal: Int): List<HanoiMove> {
        val target = pegs.size.let { n -> encode(IntArray(n) { goal }) }
        val startKey = encode(pegs)
        if (startKey == target) return emptyList()
        val parent = HashMap<Int, Int>()
        val how = HashMap<Int, HanoiMove>()
        parent[startKey] = startKey
        val queue = ArrayDeque<IntArray>()
        queue.addLast(pegs.copyOf())
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val key = encode(current)
            for (move in legalMoves(current)) {
                val next = current.copyOf()
                next[topDisk(current, move.from)] = move.to
                val nextKey = encode(next)
                if (nextKey in parent) continue
                parent[nextKey] = key
                how[nextKey] = move
                if (nextKey == target) {
                    val path = ArrayList<HanoiMove>()
                    var at = nextKey
                    while (at != startKey) {
                        path += how.getValue(at)
                        at = parent.getValue(at)
                    }
                    return path.reversed()
                }
                queue.addLast(next)
            }
        }
        error("every position of the tower of Hanoi can be solved")
    }

    /** The fewest moves from [pegs] to all disks on [goal]. */
    fun distance(pegs: IntArray, goal: Int): Int = shortest(pegs, goal).size

    private fun encode(pegs: IntArray): Int {
        var key = 0
        for (peg in pegs) key = key * HanoiGame.TOWERS + peg
        return key
    }
}
