package com.mehmtcan.brainscroll.game

import com.mehmtcan.brainscroll.game.blockout.BlockOutGenerator
import com.mehmtcan.brainscroll.game.sudoku.JigsawGenerator
import com.mehmtcan.brainscroll.game.sudoku.SudokuGenerator
import com.mehmtcan.brainscroll.game.tents.TentsGenerator
import com.mehmtcan.brainscroll.game.tents.TentsSolver
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.FeedStore
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.StoredRound
import com.mehmtcan.brainscroll.game.wordle.WordList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The levels of sudoku, jigsaw sudoku, tents and block out. */
class LevelsOfSolverGamesTest {

    private val seeds = (1..40).toList()
    private val games = listOf(GameKind.Sudoku, GameKind.Jigsaw, GameKind.Tents, GameKind.BlockOut, GameKind.Nonogram)

    private class FakeStore : FeedStore {
        val progress = linkedMapOf<String, StoredRound>()
        override fun saveProgress(round: StoredRound) { progress[round.id] = round }
        override fun deleteProgress(id: String) { progress.remove(id) }
        override fun finish(result: FinishedRound) { progress.remove(result.id) }
    }

    private val words = listOf("crane", "slate", "ghost")
    private val lists = mapOf(
        Language.EN to WordList(Language.EN, 5, valid = words, answers = words),
        Language.TR to WordList(Language.TR, 5, valid = listOf("kitap"), answers = listOf("kitap")),
    )

    private fun feed(game: GameKind, level: Difficulty, store: FeedStore = FakeStore(), restored: List<StoredRound> = emptyList()) =
        EndlessFeed(lists, Language.EN, random = Random(11), store = store, restored = restored, games = listOf(game), difficulties = mapOf(game to level))

    @Test
    fun everyLevelIsSavedWithinTheTableLimitsComesBackAndHasAGuideStep() {
        for (game in games) for (level in Difficulty.entries) {
            val store = FakeStore()
            val f = feed(game, level, store)
            f.ensureSize(12)
            for (i in 0 until 12) f.markGuided(i)
            for (stored in store.progress.values) {
                assertEquals(level, stored.difficulty, "$game $level")
                assertTrue(stored.answer.length in 1..32, "$game $level answer '${stored.answer}' is ${stored.answer.length} long")
            }
            val back = feed(game, Difficulty.Medium, restored = store.progress.values.sortedBy { it.position })
            assertEquals(f.snapshot().pages.take(12).map { it.id }, back.snapshot().pages.take(12).map { it.id }, "$game $level restores")
            for (i in 0 until 3) {
                val task = assertNotNull(f.guideTask(i), "$game $level has a guide")
                assertNotNull(task(), "$game $level page $i: the guide has a step")
            }
        }
    }

    @Test
    fun sudokuLevelsHaveMoreOrFewerGivensAndStayFinishableBySingles() {
        for (level in Difficulty.entries) {
            val target = GameLevels.sudokuClues(level)
            for (seed in seeds) {
                val puzzle = SudokuGenerator.generate(seed, target)
                val givens = puzzle.givens.count { it != 0 }
                assertTrue(givens in target..target + 4, "$level seed $seed has $givens givens for $target")
                assertContentEquals(puzzle.solution, assertNotNull(SudokuGenerator.solveBySingles(puzzle.givens), "$level seed $seed needs more than singles"))
            }
        }
        assertEquals(listOf(44, 36, 24), Difficulty.entries.map { GameLevels.sudokuClues(it) })
    }

    @Test
    fun jigsawLevelsHaveMoreOrFewerGivensAndStayFinishableBySingles() {
        for (level in Difficulty.entries) {
            val target = GameLevels.jigsawClues(level)
            for (seed in seeds.take(15)) {
                val puzzle = JigsawGenerator.generate(seed, target)
                val givens = puzzle.givens.count { it != 0 }
                assertTrue(givens in target..target + 5, "$level seed $seed has $givens givens for $target")
                assertContentEquals(puzzle.solution, assertNotNull(SudokuGenerator.solveBySingles(puzzle.givens, puzzle.layout), "$level seed $seed needs more than singles"))
            }
        }
    }

    @Test
    fun theMediumSudokusAreTheBoardsThatAlwaysCame() {
        for (seed in seeds.take(15)) {
            assertContentEquals(SudokuGenerator.generate(seed).givens, SudokuGenerator.generate(seed, GameLevels.sudokuClues(Difficulty.Medium)).givens)
            assertContentEquals(JigsawGenerator.generate(seed).givens, JigsawGenerator.generate(seed, GameLevels.jigsawClues(Difficulty.Medium)).givens)
        }
    }

    @Test
    fun tentsBoardsHaveOneSolutionAndTheRightSizeAndKindOfReasoning() {
        for (level in Difficulty.entries) {
            val shape = GameLevels.tents(level)
            for (seed in seeds) {
                val puzzle = TentsGenerator.generate(seed, level)
                assertEquals(shape.size, puzzle.size)
                assertEquals(shape.tents, puzzle.trees.size)
                assertEquals(1, TentsSolver.solutions(puzzle.size, puzzle.trees, puzzle.rowTargets, puzzle.columnTargets).size, "$level seed $seed")
                val byLogic = TentsSolver.solvesByLogic(puzzle.size, puzzle.trees, puzzle.rowTargets, puzzle.columnTargets, puzzle.tents)
                when (shape.byLogic) {
                    true -> assertTrue(byLogic, "$level seed $seed should be plain reasoning")
                    false -> assertTrue(!byLogic, "$level seed $seed should need more than plain reasoning")
                    null -> Unit
                }
                assertTrue(puzzle.encode().length <= 32, puzzle.encode())
            }
        }
    }

    @Test
    fun theMediumTentsBoardsAreTheBoardsThatAlwaysCame() {
        for (seed in seeds) {
            assertEquals(TentsGenerator.generate(seed).encode(), TentsGenerator.generate(seed, Difficulty.Medium).encode())
        }
    }

    @Test
    fun blockOutBoardsNeedAtLeastTheMovesOfTheirLevel() {
        val means = Difficulty.entries.associateWith { level ->
            val shape = GameLevels.blockOut(level)
            (1..8).map { seed ->
                val board = BlockOutGenerator.generate(seed, shape.size, shape.minPar)
                assertTrue(board.par >= shape.minPar, "$level seed $seed par ${board.par}")
                assertTrue(board.puzzle.encode().length <= 32)
                board.par
            }.average()
        }
        assertTrue(means.getValue(Difficulty.Easy) < means.getValue(Difficulty.Medium), "$means")
        assertTrue(means.getValue(Difficulty.Medium) < means.getValue(Difficulty.Hard), "$means")
        for (seed in 1..8) {
            assertEquals(BlockOutGenerator.generate(seed).puzzle.encode(), BlockOutGenerator.generate(seed, GameLevels.blockOut(Difficulty.Medium).size, GameLevels.blockOut(Difficulty.Medium).minPar).puzzle.encode())
        }
    }
}
