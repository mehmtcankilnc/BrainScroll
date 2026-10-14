package com.mehmtcan.brainscroll.game.hanoi

enum class HanoiStatus { Playing, Won }

/** What a move did. */
enum class HanoiResult {
    /** Nothing happened: not allowed (empty tower, a bigger disk on a smaller one, the same tower), or the game is over. Not counted. */
    Ignored,

    /** The top disk of a tower went onto another tower. */
    Moved,

    /** The move put the last disk on the goal tower. */
    Solved,
}

/** One move: the top disk of tower [from] goes onto tower [to]. */
data class HanoiMove(val from: Int, val to: Int)

/**
 * The picture of the towers for the UI. A tower is a list of disks from the bottom up; a disk is a number from 0 (the
 * smallest) to the number of disks less one.
 */
data class HanoiState(
    val towers: List<List<Int>>,
    /** For every disk (0 is the smallest), the tower it is on. */
    val pegs: List<Int>,
    /** The tower all the disks have to be on. */
    val goal: Int,
    /** Moves so far. A restart does not give them back. */
    val moves: Int,
    /** The fewest moves that solve the puzzle from the start. */
    val par: Int,
    val status: HanoiStatus,
    /** False while the disks are as they were at the start. */
    val canRestart: Boolean,
)

/**
 * The rules of the tower of Hanoi, free of UI and time (docs/yeni-oyunlar-plan.md).
 *
 * Three towers and a few disks. The top disk of a tower goes onto another tower that is empty or whose top disk is
 * bigger. The game is won when every disk is on the [goal] tower. There is no losing. A restart puts the disks back as
 * they were and keeps the moves.
 *
 * [start] says, for every disk from the smallest, which tower it begins on. Any such list is a legal position, because the
 * disks of a tower are always in order of size.
 */
class HanoiGame(start: List<Int>, val goal: Int, val par: Int) {
    private val first = start.toIntArray()
    private val pegs = start.toIntArray()
    private var moves = 0

    init {
        require(start.size in 2..MAX_DISKS) { "a few disks" }
        require(start.all { it in 0 until TOWERS }) { "a disk is on one of the towers" }
        require(goal in 0 until TOWERS) { "the goal is one of the towers" }
    }

    val disks: Int get() = pegs.size

    val isFinished: Boolean get() = pegs.all { it == goal }

    val state: HanoiState
        get() = HanoiState(
            towers = towers(pegs),
            pegs = pegs.toList(),
            goal = goal,
            moves = moves,
            par = par,
            status = if (isFinished) HanoiStatus.Won else HanoiStatus.Playing,
            canRestart = !isFinished && !pegs.contentEquals(first),
        )

    /** Moves the top disk of tower [from] onto tower [to]. */
    fun move(from: Int, to: Int): HanoiResult {
        if (isFinished || from !in 0 until TOWERS || to !in 0 until TOWERS) return HanoiResult.Ignored
        val disk = HanoiSolver.topDisk(pegs, from)
        if (disk < 0 || !HanoiSolver.canGo(pegs, disk, to)) return HanoiResult.Ignored
        pegs[disk] = to
        moves++
        return if (isFinished) HanoiResult.Solved else HanoiResult.Moved
    }

    /** Puts the disks back as they were at the start. The moves stay counted. Returns false if nothing had changed. */
    fun restart(): Boolean {
        if (!state.canRestart) return false
        first.copyInto(pegs)
        return true
    }

    companion object {
        const val TOWERS = 3
        const val MAX_DISKS = 8

        /** The towers as lists of disks from the bottom up. */
        fun towers(pegs: IntArray): List<List<Int>> = List(TOWERS) { tower -> pegs.indices.filter { pegs[it] == tower }.reversed() }
    }
}
