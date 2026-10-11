package com.mehmtcan.brainscroll.game.target

enum class TargetStatus { Playing, Won }

/** What a move did. */
enum class TargetResult {
    /** Nothing happened: not two different numbers on the table, a result that is not allowed, or the game is over. Not counted. */
    Ignored,

    /** Two numbers were replaced by the result. */
    Combined,

    /** The result is the target. */
    Solved,
}

/** A number on the table. [id] is what moves refer to: the starting numbers are 0, 1, 2, 3 and each result gets the next one. */
data class Token(val id: Int, val value: Int)

/** One finished operation, for the list under the numbers. */
data class Step(val a: Int, val op: Op, val b: Int, val result: Int)

/** The picture of a game for the UI. */
data class TargetState(
    val target: Int,
    /** The fewest operations that reach the target. */
    val par: Int,
    val tokens: List<Token>,
    /** The operations done since the start or the last restart, oldest first (an undone one is gone). */
    val steps: List<Step>,
    /** Operations done. Undo and restart do not give them back. */
    val moves: Int,
    val status: TargetStatus,
    val canUndo: Boolean,
    val canRestart: Boolean,
)

/**
 * The rules of the target number game, free of UI and time.
 *
 * The table starts with the [numbers]. A move takes two numbers and an operation and puts the result in their place;
 * a result is only allowed if it is a whole number above zero. Reaching [target] with any number on the table wins.
 * Not every number has to be used. There is no losing: undo takes the last operation back, restart goes back to the
 * start, and neither gives the moves back.
 */
class TargetGame(val numbers: List<Int>, val target: Int, val par: Int) {
    private class Snapshot(val tokens: List<Token>, val steps: List<Step>, val nextId: Int)

    private var tokens = numbers.mapIndexed { index, value -> Token(index, value) }
    private var steps = emptyList<Step>()
    private var nextId = numbers.size
    private val history = ArrayDeque<Snapshot>()
    private var moves = 0
    private var won = false

    init {
        require(numbers.size >= 2) { "a game needs at least two numbers" }
        require(numbers.all { it > 0 } && target > 0) { "numbers and target are above zero" }
        require(target !in numbers) { "the target is not on the table at the start" }
    }

    val isFinished: Boolean get() = won

    val state: TargetState
        get() = TargetState(
            target = target,
            par = par,
            tokens = tokens,
            steps = steps,
            moves = moves,
            status = if (won) TargetStatus.Won else TargetStatus.Playing,
            canUndo = !won && history.isNotEmpty(),
            canRestart = !won && history.isNotEmpty(),
        )

    /** What [firstId] [op] [secondId] would give, or null if it is not a move. Changes nothing. */
    fun preview(firstId: Int, op: Op, secondId: Int): Int? {
        if (won || firstId == secondId) return null
        val a = tokens.firstOrNull { it.id == firstId } ?: return null
        val b = tokens.firstOrNull { it.id == secondId } ?: return null
        return op.apply(a.value, b.value)
    }

    /** Replaces the numbers [firstId] and [secondId] by the result of [firstId] [op] [secondId]. */
    fun combine(firstId: Int, op: Op, secondId: Int): TargetResult {
        val result = preview(firstId, op, secondId) ?: return TargetResult.Ignored
        val a = tokens.first { it.id == firstId }
        val b = tokens.first { it.id == secondId }
        history.addLast(Snapshot(tokens, steps, nextId))
        tokens = tokens.filter { it.id != firstId && it.id != secondId } + Token(nextId++, result)
        steps = steps + Step(a.value, op, b.value, result)
        moves++
        if (result == target) {
            won = true
            return TargetResult.Solved
        }
        return TargetResult.Combined
    }

    /** Takes back the last operation. The move stays counted. Returns false if there is nothing to take back. */
    fun undo(): Boolean {
        if (won || history.isEmpty()) return false
        val before = history.removeLast()
        tokens = before.tokens
        steps = before.steps
        nextId = before.nextId
        return true
    }

    /** Goes back to the start. The moves stay counted. Returns false if nothing had been done. */
    fun restart(): Boolean {
        if (won || history.isEmpty()) return false
        val first = history.first()
        history.clear()
        tokens = first.tokens
        steps = first.steps
        nextId = first.nextId
        return true
    }
}
