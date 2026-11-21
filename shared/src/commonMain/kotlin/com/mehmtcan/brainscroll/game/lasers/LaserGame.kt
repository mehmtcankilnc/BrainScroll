package com.mehmtcan.brainscroll.game.lasers

enum class LaserStatus { Playing, Won }

/** What a tap did. */
enum class LaserResult {
    /** Nothing changed (a cell with no mirror, a finished board). */
    Ignored,

    /** A mirror turned, and some target is still dark. */
    Turned,

    /** A mirror turned and the beam passes through every target. */
    Solved,
}

/** The picture of the board for the UI. Cells are numbered row by row from the top left. */
data class LaserState(
    val size: Int,
    /** The row the laser comes in on. */
    val row: Int,
    /** The mirror of each cell as it stands now (see [LaserPuzzle]). */
    val mirrors: List<Int>,
    val targets: Set<Int>,
    val beam: Beam,
    /** How many targets the beam passes through. */
    val litCount: Int,
    /** Turns so far. A restart does not give them back. */
    val turns: Int,
    val status: LaserStatus,
    /** False while every mirror stands as the board was given. */
    val canRestart: Boolean,
) {
    fun isLit(cell: Int): Boolean = cell in beam.lit
}

/**
 * The rules of the laser mirrors, free of UI and time. A tap turns a mirror; the board is won when the beam passes through every
 * target. There is no losing; a restart puts every mirror back as it was given, and the turns stay counted.
 */
class LaserGame(private val puzzle: LaserPuzzle) {
    private var mirrors: List<Int> = puzzle.mirrors
    private var turns = 0
    private var beam = LaserRules.trace(puzzle.size, puzzle.row, mirrors)

    val isFinished: Boolean get() = puzzle.targets.all { it in beam.lit }

    val state: LaserState
        get() = LaserState(
            size = puzzle.size,
            row = puzzle.row,
            mirrors = mirrors,
            targets = puzzle.targets,
            beam = beam,
            litCount = puzzle.targets.count { it in beam.lit },
            turns = turns,
            status = if (isFinished) LaserStatus.Won else LaserStatus.Playing,
            canRestart = !isFinished && mirrors != puzzle.mirrors,
        )

    private fun set(next: List<Int>) {
        mirrors = next
        beam = LaserRules.trace(puzzle.size, puzzle.row, mirrors)
    }

    /** Turns the mirror on [cell]. */
    fun turn(cell: Int): LaserResult {
        if (isFinished || cell !in mirrors.indices || mirrors[cell] == 0) return LaserResult.Ignored
        set(mirrors.toMutableList().also { it[cell] = LaserRules.turned(it[cell]) })
        turns++
        return if (isFinished) LaserResult.Solved else LaserResult.Turned
    }

    /** Puts every mirror back as the board was given. Returns false if they already stand so (or the board is won). */
    fun restart(): Boolean {
        if (isFinished || mirrors == puzzle.mirrors) return false
        set(puzzle.mirrors)
        return true
    }

    /**
     * Puts back what [state] shows, for a saved game. Returns false if [savedMirrors] is not this board with some mirrors turned
     * (the same cells hold mirrors): nothing is changed then.
     */
    fun restore(savedMirrors: List<Int>, savedTurns: Int): Boolean {
        if (savedTurns < 0 || savedMirrors.size != puzzle.mirrors.size) return false
        for (cell in savedMirrors.indices) if ((savedMirrors[cell] == 0) != (puzzle.mirrors[cell] == 0) || savedMirrors[cell] !in 0..2) return false
        set(savedMirrors)
        turns = savedTurns
        return true
    }
}
