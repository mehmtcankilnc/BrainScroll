package com.mehmtcan.brainscroll.game.guide

import com.mehmtcan.brainscroll.game.Difficulty
import com.mehmtcan.brainscroll.game.fences.FenceGame
import com.mehmtcan.brainscroll.game.fences.FenceGenerator
import com.mehmtcan.brainscroll.game.fences.FenceSolver
import com.mehmtcan.brainscroll.game.greaterless.GlGame
import com.mehmtcan.brainscroll.game.greaterless.GlGenerator
import com.mehmtcan.brainscroll.game.greaterless.GlRules
import com.mehmtcan.brainscroll.game.greaterless.GlSolver
import com.mehmtcan.brainscroll.game.lamps.LampGame
import com.mehmtcan.brainscroll.game.lamps.LampGenerator
import com.mehmtcan.brainscroll.game.lamps.LampSolver
import com.mehmtcan.brainscroll.game.lonenumber.LoneGame
import com.mehmtcan.brainscroll.game.lonenumber.LoneGenerator
import com.mehmtcan.brainscroll.game.lonenumber.LoneSolver
import com.mehmtcan.brainscroll.game.nonogram.NonogramGenerator
import com.mehmtcan.brainscroll.game.nonogram.NonogramMark
import com.mehmtcan.brainscroll.game.nonogram.NonogramRound
import com.mehmtcan.brainscroll.game.queens.QueensGenerator
import com.mehmtcan.brainscroll.game.queens.QueensRound
import com.mehmtcan.brainscroll.game.tents.TentsGenerator
import com.mehmtcan.brainscroll.game.tents.TentsRules
import com.mehmtcan.brainscroll.game.tents.TentsSolver
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.lights.LightsGame
import com.mehmtcan.brainscroll.game.lights.LightsStatus
import com.mehmtcan.brainscroll.game.slide.SlideGame
import com.mehmtcan.brainscroll.game.slide.SlideStatus
import com.mehmtcan.brainscroll.game.target.Op
import com.mehmtcan.brainscroll.game.target.TargetGame
import com.mehmtcan.brainscroll.game.target.TargetGenerator
import com.mehmtcan.brainscroll.game.target.TargetStatus
import com.mehmtcan.brainscroll.game.tubes.TubesGame
import com.mehmtcan.brainscroll.game.tubes.TubesStatus
import com.mehmtcan.brainscroll.game.mines.MinesGame
import com.mehmtcan.brainscroll.game.mines.MinesResult
import com.mehmtcan.brainscroll.game.mines.MinesSolver
import com.mehmtcan.brainscroll.game.mines.MinesStatus
import com.mehmtcan.brainscroll.game.sudoku.SudokuGame
import com.mehmtcan.brainscroll.game.sudoku.SudokuGenerator
import com.mehmtcan.brainscroll.game.sudoku.SudokuLayout
import com.mehmtcan.brainscroll.game.sudoku.SudokuStatus
import com.mehmtcan.brainscroll.game.zerosones.BinGame
import com.mehmtcan.brainscroll.game.zerosones.BinGenerator
import com.mehmtcan.brainscroll.game.zerosones.BinSolver
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The reasons the guide gives: for every step of a game played by the guide alone, the cells it rings as the reason are really what the
 * sentence says they are.
 */
class GuideReasonsTest {
    private val seeds = 0 until 8

    @Test
    fun zerosAndOnesSayWhichPairOrLineForcesTheDigit() {
        var withReason = 0
        var steps = 0
        for (difficulty in Difficulty.entries) for (seed in seeds) {
            val puzzle = BinGenerator.generate(seed, difficulty)
            val answer = BinSolver.uniqueSolution(puzzle)!!
            val game = BinGame(puzzle)
            val n = puzzle.size
            while (!game.isFinished) {
                val state = game.state
                val step = assertNotNull(BinGuide.step(puzzle, state))
                val cell = step.cells.single()
                val digit = step.numbers[0]
                val other = step.numbers[1]
                assertEquals(answer[cell], digit)
                assertEquals(1 - digit, other)
                steps++
                if (step.rule != 0) withReason++
                when (step.rule) {
                    BinSolver.PAIR -> {
                        assertEquals(2, step.evidence.size)
                        assertTrue(step.evidence.all { state.cells[it] == other }, "both ringed cells hold $other")
                    }
                    BinSolver.SPLIT -> {
                        assertEquals(2, step.evidence.size)
                        assertTrue(step.evidence.all { state.cells[it] == other })
                        assertEquals(2, kotlin.math.abs(step.evidence[0] - step.evidence[1]).let { if (it >= n) it / n else it }, "the cell is between them")
                    }
                    BinSolver.HALF -> {
                        assertEquals(n / 2, step.evidence.size)
                        assertTrue(step.evidence.all { state.cells[it] == other })
                    }
                    else -> assertTrue(step.evidence.isEmpty())
                }
                game.tap(cell)
                if (digit == 1) game.tap(cell)
            }
        }
        assertTrue(withReason * 2 > steps, "most steps come with a reason: $withReason of $steps")
    }

    @Test
    fun lampsSayWhatMakesTheLampTheOnlyWay() {
        var withReason = 0
        var steps = 0
        for (difficulty in Difficulty.entries) for (seed in seeds) {
            val puzzle = LampGenerator.generate(seed, difficulty)
            val answer = LampSolver.uniqueSolution(puzzle)!!
            val game = LampGame(puzzle)
            while (!game.isFinished) {
                val state = game.state
                val step = assertNotNull(LampGuide.step(puzzle, state))
                val cell = step.cells.single()
                assertTrue(answer[cell])
                steps++
                if (step.rule != 0) withReason++
                when (step.rule) {
                    LampSolver.ONLY_SELF -> assertEquals(listOf(cell), step.evidence)
                    LampSolver.ONLY_OTHER -> {
                        val dark = step.evidence.single()
                        assertTrue(dark in puzzle.seenFrom(cell), "the ringed cell is one the lamp lights")
                    }
                    LampSolver.WALL_NEEDS -> {
                        val wall = step.evidence.single()
                        assertEquals(puzzle.numberAt(wall), step.numbers[0])
                        assertTrue(cell in puzzle.neighbours(wall), "the lamp is next to the ringed wall")
                    }
                    else -> Unit
                }
                game.tap(cell)
            }
        }
        assertTrue(withReason * 2 > steps, "most steps come with a reason: $withReason of $steps")
    }

    @Test
    fun greaterLessSaysWhyOnlyThatNumberFits() {
        var withReason = 0
        var steps = 0
        for (difficulty in Difficulty.entries) for (seed in seeds) {
            val puzzle = GlGenerator.generate(seed, difficulty)
            val answer = GlSolver.uniqueSolution(puzzle)!!
            val game = GlGame(puzzle)
            val n = puzzle.size
            while (!game.isFinished) {
                val state = game.state
                val step = assertNotNull(GlGuide.step(puzzle, state))
                val cell = step.cells.single()
                val value = step.numbers[0]
                assertEquals(answer[cell], value)
                steps++
                if (step.rule != 0) withReason++
                when (step.rule) {
                    1 -> {
                        assertEquals(n - 1, step.evidence.size)
                        assertEquals((1..n).filter { it != value }.toSet(), step.evidence.map { state.cells[it] }.toSet())
                    }
                    2 -> assertTrue(step.evidence.all { e ->
                        GlRules.links(puzzle).any { (a, b, _) -> (a == cell && b == e) || (b == cell && a == e) }
                    }, "every ringed cell is next to the cell with a sign between")
                    3 -> assertTrue(step.evidence.all { state.cells[it] == value }, "every ringed cell holds the number")
                    else -> assertTrue(step.evidence.isEmpty())
                }
                game.set(cell, value)
            }
        }
        assertTrue(withReason * 2 > steps, "most steps come with a reason: $withReason of $steps")
    }

    @Test
    fun fencesSayWhichNumberOrDotForcesTheLine() {
        var withReason = 0
        var steps = 0
        for (difficulty in Difficulty.entries) for (seed in 0 until 5) {
            val puzzle = FenceGenerator.generate(seed, difficulty)
            val solution = FenceSolver.solveByLogic(puzzle, true)!!
            val g = puzzle.geometry
            val game = FenceGame(puzzle)
            while (!game.isFinished) {
                val state = game.state
                val step = assertNotNull(FenceGuide.step(puzzle, state))
                val edge = step.cells.single()
                assertEquals(FenceSolver.LINE, solution[edge])
                steps++
                if (step.rule != 0) withReason++
                when (step.rule) {
                    1 -> {
                        val cell = step.evidence.single() - g.edges
                        assertTrue(edge in g.cellEdges[cell], "the ringed square has the edge as a side")
                        assertEquals(puzzle.clues[cell], step.numbers[0])
                    }
                    2 -> {
                        val line = step.evidence.single()
                        assertEquals(FenceSolver.LINE, state.marks[line])
                        assertTrue(g.edgeDots[line].any { it in g.edgeDots[edge] }, "the ringed line ends where the new one starts")
                    }
                    else -> assertTrue(step.evidence.isEmpty())
                }
                game.tap(edge)
            }
        }
        assertTrue(withReason * 2 > steps, "most steps come with a reason: $withReason of $steps")
    }

    @Test
    fun loneNumberRingsTheSameNumbersInTheLine() {
        for (difficulty in Difficulty.entries) for (seed in seeds) {
            val puzzle = LoneGenerator.generate(seed, difficulty)
            val answer = LoneSolver.uniqueSolution(puzzle)!!
            val game = LoneGame(puzzle)
            val n = puzzle.size
            while (!game.isFinished) {
                val step = assertNotNull(LoneGuide.step(puzzle, game.state))
                val cell = step.cells.single()
                assertTrue(cell in answer)
                assertEquals(puzzle.numbers[cell], step.numbers[0])
                for (e in step.evidence) {
                    assertEquals(puzzle.numbers[cell], puzzle.numbers[e])
                    assertTrue(e / n == cell / n || e % n == cell % n, "in the same row or column")
                }
                assertEquals(if (step.evidence.isEmpty()) 0 else 1, step.rule)
                game.tap(cell)
            }
        }
    }

    @Test
    fun minesRingTheNumbersThatProveACell() {
        var withReason = 0
        var steps = 0
        for (seed in 1..20) {
            val game = MinesGame(seed = seed)
            var guard = 0
            while (game.state.status == MinesStatus.Playing && guard++ < 120) {
                val state = game.state
                val step = assertNotNull(MinesGuide.step(state))
                val cell = step.cells.single()
                if (step.reason != GuideReason.MinesStart) {
                    steps++
                    if (step.rule != 0) withReason++
                }
                val around = MinesGame.neighbors(cell, state.rows, state.cols)
                when (step.rule) {
                    MinesSolver.SAFE_COMPLETE -> {
                        val number = step.evidence.first()
                        assertTrue(number in around, "the ringed number touches the cell")
                        assertEquals(step.numbers[0], state.numbers[number])
                        assertEquals(step.numbers[0], step.evidence.size - 1, "as many ringed mines as the number says")
                    }
                    MinesSolver.MINE_FILLS -> {
                        val number = step.evidence.single()
                        assertTrue(number in around)
                        assertEquals(step.numbers[0], state.numbers[number])
                    }
                    else -> Unit
                }
                when (step.reason) {
                    GuideReason.MinesStart, GuideReason.MinesSafe -> assertTrue(game.open(cell) != MinesResult.Exploded)
                    GuideReason.MinesMine -> game.toggleFlag(cell)
                    else -> error("${step.reason}")
                }
            }
        }
        assertTrue(withReason * 3 > steps, "a good share of the steps come with a reason: $withReason of $steps")
    }

    @Test
    fun sudokuRingsTheCellsThatKeepTheOtherDigitsOut() {
        for (seed in 1..10) {
            val game = SudokuGame(seed)
            val solution = SudokuGenerator.generate(seed).solution.toList()
            val layout = SudokuLayout.of(game.state.regions)
            var guard = 0
            while (game.state.status == SudokuStatus.Playing && guard++ < 100) {
                val values = game.state.values
                val step = assertNotNull(SudokuGuide.step(game.state, solution))
                val cell = step.cells.single()
                val digit = step.numbers[0]
                when (step.reason) {
                    GuideReason.SudokuOnlyDigit -> {
                        assertEquals(8, step.evidence.size)
                        assertEquals((1..9).filter { it != digit }.toSet(), step.evidence.map { values[it] }.toSet(), "every other digit once")
                        assertTrue(step.evidence.all { it in layout.peers[cell] })
                    }
                    GuideReason.SudokuOnlyPlace -> {
                        assertTrue(step.rule in 1..3)
                        assertTrue(step.evidence.isNotEmpty())
                        assertTrue(step.evidence.all { values[it] == digit }, "the ringed cells hold the digit")
                    }
                    else -> error("${step.reason}")
                }
                game.place(cell, digit)
            }
        }
    }

    @Test
    fun nonogramNamesTheLineAndTheBlockThatFillACell() {
        var withReason = 0
        var steps = 0
        for (seed in 1..12) {
            val puzzle = NonogramGenerator.generate(seed)
            val round = NonogramRound(0, "n$seed", puzzle, Language.EN, { 0L })
            val n = puzzle.size
            var guard = 0
            while (!round.isFinished && guard++ < n * n) {
                val state = round.snapshot().state
                val step = assertNotNull(NonogramGuide.step(state))
                if (step.reason != GuideReason.NonogramFill) break
                val cell = step.cells.single()
                steps++
                if (step.rule != 0) {
                    withReason++
                    val inRow = step.rule <= 2
                    assertTrue(step.evidence.all { if (inRow) it / n == cell / n else it % n == cell % n }, "the ringed cells are the line of the rule")
                    val clues = if (inRow) state.rowClues[cell / n] else state.columnClues[cell % n]
                    assertTrue(step.numbers[2] in clues, "the block is one of the numbers of that line")
                }
                round.paint(cell, cell, NonogramMark.FILLED)
            }
        }
        assertTrue(steps > 0 && withReason > 0, "some cells are filled by their own line: $withReason of $steps")
    }

    @Test
    fun tentsSayWhichTreeOrLineForcesATent() {
        var withReason = 0
        for (seed in 1..15) {
            val puzzle = TentsGenerator.generate(seed)
            val size = puzzle.size
            val rows = (0 until size).map { r -> (0 until size).count { puzzle.tents.contains(r * size + it) } }
            val columns = (0 until size).map { c -> (0 until size).count { puzzle.tents.contains(it * size + c) } }
            val known = HashSet<Int>()
            while (true) {
                val why = HashMap<Int, TentsSolver.Why>()
                val more = TentsSolver.deduce(size, puzzle.trees, rows, columns, known, why)
                if (more.isEmpty()) break
                for (cell in more) {
                    val reason = assertNotNull(why[cell], "every tent that is found has a reason")
                    withReason++
                    when (reason.rule) {
                        TentsSolver.TREE_ONLY -> {
                            val tree = reason.evidence.single()
                            assertTrue(tree in puzzle.trees && cell in TentsRules.straightNeighbors(size, tree), "the tree is next to the tent")
                        }
                        TentsSolver.LINE_ROW -> assertTrue(reason.evidence.all { it / size == cell / size } && cell in reason.evidence)
                        TentsSolver.LINE_COLUMN -> assertTrue(reason.evidence.all { it % size == cell % size } && cell in reason.evidence)
                        else -> error("rule ${reason.rule}")
                    }
                }
                known += more
            }
        }
        assertTrue(withReason > 0)
    }

    @Test
    fun queensRingTheQueensThatAttackTheRestOfALine() {
        for (seed in 1..15) {
            val puzzle = QueensGenerator.generate(seed)
            val round = QueensRound(0, "q$seed", puzzle.size, puzzle.locked, Language.EN, { 0L })
            val n = puzzle.size
            var guard = 0
            while (!round.isFinished && guard++ < n * 2) {
                val state = round.snapshot().state
                val step = assertNotNull(QueensGuide.step(state))
                if (step.reason != GuideReason.QueensPlace) break
                val cell = step.cells.single()
                val placed = state.queens.indices.filter { state.queens[it] }
                if (step.rule != 0) {
                    val line = (0 until n).map { if (step.rule == 1) cell / n * n + it else it * n + cell % n }.filter { it != cell }
                    fun attacks(a: Int, b: Int) = a / n == b / n || a % n == b % n || kotlin.math.abs(a / n - b / n) == kotlin.math.abs(a % n - b % n)
                    assertTrue(step.evidence.all { it in placed }, "the ringed cells are queens on the board")
                    assertTrue(line.all { o -> step.evidence.any { attacks(it, o) } }, "together they attack every other cell of the line")
                    assertTrue(placed.none { attacks(it, cell) }, "nobody attacks the cell itself")
                }
                round.toggle(cell)
            }
        }
    }

    @Test
    fun lightsRingTheLightsATapPutsOut() {
        var withReason = 0
        for (seed in 1..20) {
            val game = LightsGame(seed)
            var taps = 0
            while (game.state.status == LightsStatus.Playing && taps++ < 30) {
                val state = game.state
                val step = assertNotNull(LightsGuide.step(state))
                val cell = step.cells.single()
                if (step.rule == 1) {
                    withReason++
                    assertTrue(step.evidence.all { state.lit[it] }, "the ringed cells are lit")
                    assertEquals(step.numbers[0], step.evidence.size, "the lights that go out are the lit cells it touches")
                    assertTrue(step.numbers[0] > step.numbers[1], "more go out than come on")
                }
                game.tap(cell)
            }
        }
        assertTrue(withReason > 0)
    }

    @Test
    fun slideSaysWhetherATileMovesHomeOrOutOfTheWay() {
        for (seed in 1..4) {
            val game = SlideGame(seed)
            var steps = 0
            while (game.state.status == SlideStatus.Playing && steps++ < 120) {
                val state = game.state
                val step = assertNotNull(SlideGuide.step(state))
                val cell = step.cells.single()
                assertEquals(state.tiles[cell], step.numbers[0])
                assertTrue(step.rule == 1 || step.rule == 2)
                game.tap(cell)
            }
        }
    }

    @Test
    fun tubesSayWhyThePourIsGood() {
        var reasons = HashSet<Int>()
        for (seed in 1..8) {
            val game = TubesGame(TubesGame.newLayout(Random(seed)))
            var pours = 0
            while (game.state.status == TubesStatus.Playing && pours++ < 80) {
                val state = game.state
                val step = assertNotNull(TubesGuide.step(state))
                val (from, to) = step.cells
                reasons += step.rule
                when (step.rule) {
                    2 -> assertEquals(state.tubes[from].last(), state.tubes[to].last())
                    3 -> assertTrue(state.tubes[to].isEmpty())
                    1 -> assertTrue(state.tubes[to].isEmpty() || state.tubes[to].last() == state.tubes[from].last())
                }
                game.pour(from, to)
            }
        }
        assertTrue(reasons.any { it != 0 }, "some pours have a reason")
    }

    @Test
    fun targetSaysHowManyOperationsAreLeft() {
        for (seed in 1..20) {
            val puzzle = TargetGenerator.generate(seed)
            val game = TargetGame(puzzle.numbers, puzzle.target, puzzle.par)
            var moves = 0
            while (game.state.status == TargetStatus.Playing && moves++ < 10) {
                val step = assertNotNull(TargetGuide.step(game.state))
                val left = puzzle.par - game.state.moves
                assertEquals(left, step.numbers[3], "operations left, this one included")
                assertEquals(left == 1, step.rule == 1)
                if (step.rule == 1) assertEquals(puzzle.target, step.numbers[2])
                game.combine(step.cells[0], Op.fromSymbol(step.text.single())!!, step.cells[1])
            }
        }
    }
}
