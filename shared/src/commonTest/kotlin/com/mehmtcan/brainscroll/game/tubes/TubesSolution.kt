package com.mehmtcan.brainscroll.game.tubes

/** A list of pours (from, to) that solves [tubes], found by depth first search. Tests use it to play a board to the end. */
fun solutionOf(tubes: List<List<Int>>, capacity: Int = TubesGame.DEFAULT_CAPACITY): List<Pair<Int, Int>> {
    val seen = HashSet<List<List<Int>>>()
    fun search(current: List<List<Int>>): List<Pair<Int, Int>>? {
        if (TubesGame.isSolved(current, capacity)) return emptyList()
        if (!seen.add(current)) return null
        for (from in current.indices) for (to in current.indices) {
            val next = TubesGame.poured(current, from, to, capacity) ?: continue
            search(next)?.let { return listOf(from to to) + it }
        }
        return null
    }
    return checkNotNull(search(tubes)) { "no solution" }
}
