package com.mehmtcan.brainscroll.game.cubefold

import kotlin.random.Random

/** One square of the net: where it is on the grid and which of the six symbols is printed on it. */
data class NetCell(val row: Int, val column: Int, val symbol: Int)

/** Why an option is what it is: the right view, a view with two opposite faces, or a view whose faces are in the wrong turning order. */
enum class CubeWhy { Right, Opposite, Mirror }

/**
 * One question: a net of six squares (every square has a different symbol, 0 to 5) and some views of a corner of the cube it folds to.
 * A view is three symbols in the order a viewer sees them: the top face, the face at the left, the face at the right. The turning order
 * top, left, right is counter-clockwise; the same corner turned on its axis gives the same cube (so a view may be turned, and it is
 * the same view, see [canonical]).
 *
 * [options] are the views, [correct] the index of the one that is a corner of the cube, [whys] what each option is. [partner] says,
 * for every symbol, the symbol on the opposite face of the cube.
 */
data class CubeQuestion(
    val net: List<NetCell>,
    val options: List<List<Int>>,
    val correct: Int,
    val whys: List<CubeWhy>,
    val partner: List<Int>,
) {
    /** The cells of the net (indices into [net]) of the symbols of option [option] that make it wrong: the two opposite faces, or all three. */
    fun evidence(option: Int): List<Int> {
        val view = options[option]
        val symbols = when (whys[option]) {
            CubeWhy.Right -> emptyList()
            CubeWhy.Mirror -> view
            CubeWhy.Opposite -> view.filter { partner[it] in view }
        }
        return symbols.map { s -> net.indexOfFirst { it.symbol == s } }
    }
}

/**
 * A run of cube folding: [rounds] questions, each a net and [options] views, the same questions for the same [seed]. The run is saved as
 * text (see [encode]); the questions are not saved, they come from the seed.
 */
class CubeFoldPuzzle(val options: Int, val rounds: Int, val seed: Int) {
    init {
        require(options in 3..4) { "options $options" }
        require(rounds in 1..MAX_ROUNDS) { "rounds $rounds" }
        require(seed >= 0) { "seed $seed" }
    }

    // The questions are made once (a page asks for its question again and again); making one twice gives the same one, so a race here is harmless.
    private val made = arrayOfNulls<CubeQuestion>(rounds)

    /** The question of round [round] (from 0). */
    fun question(round: Int): CubeQuestion {
        require(round in 0 until rounds) { "round $round" }
        made[round]?.let { return it }
        return CubeFolding.question(Random(seed.toLong() * 7_919L + round * 104_729L + 17L), options).also { made[round] = it }
    }

    /** The number of options, the number of rounds (hex) and the seed in base 36. */
    fun encode(): String = "$options${rounds.toString(16)}${seed.toString(36)}"

    override fun equals(other: Any?) = other is CubeFoldPuzzle && options == other.options && rounds == other.rounds && seed == other.seed

    override fun hashCode() = (31 * options + rounds) * 31 + seed

    companion object {
        const val MAX_ROUNDS = 15

        /** Reads what [encode] wrote. Null if it is not a run (so a saved game of another kind starts a new one). */
        fun decode(text: String): CubeFoldPuzzle? {
            if (text.length < 3) return null
            val options = text[0].digitToIntOrNull() ?: return null
            val rounds = text[1].digitToIntOrNull(16) ?: return null
            val seed = text.substring(2).toIntOrNull(36) ?: return null
            return runCatching { CubeFoldPuzzle(options, rounds, seed) }.getOrNull()
        }
    }
}

/**
 * The geometry of folding a net into a cube, and the making of a question. A die is rolled over the squares of the net: the face that
 * lies on a square is the face that square becomes. The die starts with its faces numbered top 0, bottom 1, north 2, south 3, east 4,
 * west 5; a net folds to a cube when the six squares give six different faces.
 */
object CubeFolding {
    // Face numbers: 0 top, 1 bottom, 2 north, 3 south, 4 east, 5 west. Opposite faces: 0-1, 2-3, 4-5.
    private fun opposite(face: Int) = face xor 1

    /**
     * For every square of [cells] (row, column) the face of the cube it becomes, or null if the net does not fold to a cube. The first
     * square is the one under the die at the start. The net must be connected.
     */
    fun fold(cells: List<Pair<Int, Int>>): IntArray? {
        val faceOf = IntArray(cells.size) { -1 }
        val index = cells.withIndex().associate { it.value to it.index }
        // The state of the die: which face is at top, bottom, north, south, east, west.
        val start = intArrayOf(0, 1, 2, 3, 4, 5)
        val seen = HashSet<Int>()
        val order = ArrayDeque<Pair<Int, IntArray>>()
        order.add(0 to start)
        faceOf[0] = start[1]
        seen += 0
        while (order.isNotEmpty()) {
            val (at, die) = order.removeFirst()
            val (row, column) = cells[at]
            for ((dr, dc) in listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)) {
                val next = index[row + dr to column + dc] ?: continue
                if (next in seen) continue
                val rolled = roll(die, dr, dc)
                faceOf[next] = rolled[1]
                seen += next
                order.add(next to rolled)
            }
        }
        if (seen.size != cells.size) return null
        return if (faceOf.toSet().size == 6) faceOf else null
    }

    /** The die after rolling one square towards (dr, dc): east is +column, south is +row. */
    private fun roll(d: IntArray, dr: Int, dc: Int): IntArray {
        val (top, bottom, north, south, east, west) = d.toList()
        return when {
            dc == 1 -> intArrayOf(west, east, north, south, top, bottom)
            dc == -1 -> intArrayOf(east, west, north, south, bottom, top)
            dr == 1 -> intArrayOf(north, south, bottom, top, east, west)
            else -> intArrayOf(south, north, top, bottom, east, west)
        }
    }

    private operator fun <T> List<T>.component6(): T = this[5]

    /** A random net: six connected squares, no 2 by 2 block, that folds to a cube, with a different symbol on each. */
    fun randomNet(random: Random): List<NetCell> {
        while (true) {
            val cells = ArrayList<Pair<Int, Int>>()
            cells += 0 to 0
            while (cells.size < 6) {
                val (r, c) = cells[random.nextInt(cells.size)]
                val (dr, dc) = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)[random.nextInt(4)]
                val next = r + dr to c + dc
                if (next !in cells) cells += next
            }
            if (hasBlock(cells)) continue
            val minRow = cells.minOf { it.first }
            val minColumn = cells.minOf { it.second }
            val shifted = cells.map { it.first - minRow to it.second - minColumn }
            // The first square must be the one the die starts on; any is fine, the fold only needs it to be in the net.
            if (fold(shifted) == null) continue
            if (shifted.maxOf { it.first } >= 5 || shifted.maxOf { it.second } >= 5) continue
            val symbols = (0 until 6).shuffled(random)
            return shifted.mapIndexed { i, (r, c) -> NetCell(r, c, symbols[i]) }
        }
    }

    private fun hasBlock(cells: List<Pair<Int, Int>>) = cells.any { (r, c) ->
        (r to c + 1) in cells && (r + 1 to c) in cells && (r + 1 to c + 1) in cells
    }

    /**
     * The eight corners of the cube folded from [net], as views (top, left, right) in the order a viewer sees them with the printed side
     * of the net outside the cube. The partner of every symbol (the symbol on the opposite face) comes with them.
     */
    fun corners(net: List<NetCell>): Pair<List<List<Int>>, List<Int>> {
        val faceOf = fold(net.map { it.row to it.column })!!
        val symbolOfFace = IntArray(6)
        net.forEachIndexed { i, cell -> symbolOfFace[faceOf[i]] = cell.symbol }
        val partner = IntArray(6)
        for (cell in net) partner[cell.symbol] = symbolOfFace[opposite(faceOf[net.indexOfFirst { it.symbol == cell.symbol }])]
        val views = ArrayList<List<Int>>()
        for (x in 4..5) for (y in 2..3) for (z in 0..1) {
            // The three faces of a corner turn counter-clockwise, seen from outside, in the order x, y, z when the signs of the three
            // directions multiply to a negative number (the printed side of the net is outside the cube), else in the order x, z, y.
            val sign = (if (x == 4) 1 else -1) * (if (y == 2) 1 else -1) * (if (z == 0) 1 else -1)
            val order = if (sign < 0) listOf(x, y, z) else listOf(x, z, y)
            views += order.map { symbolOfFace[it] }
        }
        return views to partner.toList()
    }

    /** The view turned so that it starts with its smallest symbol: two views are the same cube corner exactly when they have the same canonical form. */
    fun canonical(view: List<Int>): List<Int> {
        val k = view.indexOf(view.min())
        return view.drop(k) + view.take(k)
    }

    /** One question with [options] views: one real corner, a view of the same corner turned the wrong way, one with opposite faces, and so on. */
    fun question(random: Random, options: Int): CubeQuestion {
        val net = randomNet(random)
        val (corners, partner) = corners(net)
        val real = corners.map(::canonical).toSet()
        val answer = corners[random.nextInt(corners.size)]
        val used = HashSet<List<Int>>()
        used += canonical(answer)
        val wrong = ArrayList<Pair<List<Int>, CubeWhy>>()

        fun mirrorOf(): List<Int>? = corners.shuffled(random).map { it.reversed() }.firstOrNull { canonical(it) !in used }
        fun oppositeView(): List<Int>? {
            val all = ArrayList<List<Int>>()
            for (a in 0 until 6) for (b in 0 until 6) for (c in 0 until 6) {
                if (a == b || b == c || a == c) continue
                val view = listOf(a, b, c)
                if (canonical(view) in real || canonical(view) in used) continue
                if (view.none { partner[it] in view }) continue
                all += view
            }
            return all.randomOrNull(random)
        }
        // Always one of each kind first, then a random mix.
        mirrorOf()?.let { wrong += it to CubeWhy.Mirror; used += canonical(it) }
        oppositeView()?.let { wrong += it to CubeWhy.Opposite; used += canonical(it) }
        while (wrong.size < options - 1) {
            val mirror = random.nextBoolean()
            val view = (if (mirror) mirrorOf() else oppositeView()) ?: (mirrorOf() ?: oppositeView()) ?: break
            val kind = if (view.none { partner[it] in view }) CubeWhy.Mirror else CubeWhy.Opposite
            wrong += view to kind
            used += canonical(view)
        }
        // Every view is turned a random number of times, so the answer is not always written the same way and its place is random.
        val all = (listOf(answer to CubeWhy.Right) + wrong.take(options - 1)).map { (view, why) ->
            val k = random.nextInt(3)
            (view.drop(k) + view.take(k)) to why
        }.shuffled(random)
        return CubeQuestion(net, all.map { it.first }, all.indexOfFirst { it.second == CubeWhy.Right }, all.map { it.second }, partner)
    }
}
