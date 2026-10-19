package com.mehmtcan.brainscroll.game

import com.mehmtcan.brainscroll.game.arrows.ArrowsGenerator
import com.mehmtcan.brainscroll.game.arrows.ArrowsRules
import com.mehmtcan.brainscroll.game.pipes.PipesGenerator
import com.mehmtcan.brainscroll.game.queens.QueensGenerator
import com.mehmtcan.brainscroll.game.slide.SlideGame
import com.mehmtcan.brainscroll.game.target.TargetGenerator
import com.mehmtcan.brainscroll.game.tubes.TubesGame
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
import kotlin.test.assertTrue

/** The levels of the games that are made from a few numbers. */
class LevelsOfParametricGamesTest {

    private val seeds = (1..60).toList()
    private val games = listOf(GameKind.Pipes, GameKind.Queens, GameKind.Arrows, GameKind.Slide, GameKind.Lights, GameKind.Target, GameKind.Mines, GameKind.Tubes)

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

    // --- every game, every level: what is saved fits the table and comes back ----------------------------------------

    @Test
    fun everyLevelOfEveryGameIsSavedWithinTheTableLimitsAndComesBack() {
        for (game in games) for (level in Difficulty.entries) {
            val store = FakeStore()
            val f = feed(game, level, store)
            f.ensureSize(25)
            for (i in 0 until 25) f.markGuided(i)
            assertEquals(25, store.progress.size, "$game $level")
            for (stored in store.progress.values) {
                assertEquals(level, stored.difficulty, "$game $level")
                assertTrue(stored.answer.length in 1..32, "$game $level answer '${stored.answer}' is ${stored.answer.length} long")
            }
            val back = feed(game, Difficulty.Medium, restored = store.progress.values.sortedBy { it.position })
            assertEquals(f.snapshot().pages.take(25).map { it.id }, back.snapshot().pages.take(25).map { it.id }, "$game $level restores")
            assertEquals(List(25) { level }, back.snapshot().difficulties.take(25), "$game $level keeps its level")
        }
    }

    // --- Medium is the board that always came ----------------------------------------------------------------------------

    @Test
    fun mediumBoardsAreTheBoardsThatAlwaysCameForASeed() {
        for (seed in seeds) {
            assertContentEquals(PipesGenerator.generate(seed).start, PipesGenerator.generate(seed, GameLevels.pipes(Difficulty.Medium).size, GameLevels.pipes(Difficulty.Medium).minPar).start)
            assertEquals(QueensGenerator.generate(seed).locked, QueensGenerator.generate(seed, GameLevels.queens(Difficulty.Medium).size, GameLevels.queens(Difficulty.Medium).locked).locked)
            assertEquals(ArrowsGenerator.generate(seed).arrows, ArrowsGenerator.generate(seed, Difficulty.Medium).arrows)
            assertEquals(TargetGenerator.generate(seed).let { it.numbers to it.target }, TargetGenerator.generate(seed, Difficulty.Medium).let { it.numbers to it.target })
        }
        assertEquals(GameLevels.SlideShape(3, 4, 80), GameLevels.slide(Difficulty.Medium))
        assertEquals(GameLevels.LightsShape(5, 7), GameLevels.lights(Difficulty.Medium))
        assertEquals(GameLevels.MinesShape(8, 8, 10), GameLevels.mines(Difficulty.Medium))
        assertEquals(GameLevels.TubesShape(5, 2), GameLevels.tubes(Difficulty.Medium))
    }

    // --- pipes -------------------------------------------------------------------------------------------------------

    @Test
    fun pipesGetHarderWithTheLevel() {
        val means = Difficulty.entries.associateWith { level ->
            val shape = GameLevels.pipes(level)
            seeds.take(25).map { seed ->
                val puzzle = PipesGenerator.generate(seed, shape.size, shape.minPar)
                assertEquals(shape.size * shape.size, puzzle.start.size)
                assertTrue(puzzle.par >= shape.minPar, "$level seed $seed par ${puzzle.par}")
                puzzle.par
            }.average()
        }
        assertTrue(means.getValue(Difficulty.Easy) < means.getValue(Difficulty.Medium), "$means")
        assertTrue(means.getValue(Difficulty.Medium) < means.getValue(Difficulty.Hard), "$means")
    }

    // --- queens ------------------------------------------------------------------------------------------------------

    @Test
    fun queensHaveMoreToPlaceAtEachLevel() {
        for (level in Difficulty.entries) {
            val shape = GameLevels.queens(level)
            for (seed in seeds) {
                val puzzle = QueensGenerator.generate(seed, shape.size, shape.locked)
                assertEquals(shape.size, puzzle.size)
                assertEquals(shape.locked, puzzle.locked.size)
            }
        }
        val toPlace = Difficulty.entries.map { GameLevels.queens(it).let { s -> s.size - s.locked } }
        assertTrue(toPlace[0] < toPlace[1] && toPlace[1] < toPlace[2], "$toPlace")
    }

    // --- arrows ------------------------------------------------------------------------------------------------------

    @Test
    fun arrowBoardsAreClearableHaveTheRightCountAndAFairNumberOfFreeArrows() {
        for (level in Difficulty.entries) {
            val shape = GameLevels.arrows(level)
            for (seed in seeds) {
                val puzzle = ArrowsGenerator.generate(seed, level)
                assertEquals(shape.size, puzzle.size)
                assertTrue(puzzle.arrowCount in shape.minArrows..shape.maxArrows, "$level seed $seed")
                val free = ArrowsRules.free(puzzle, BooleanArray(puzzle.arrowCount) { true }).size
                assertTrue(free in shape.minFree..shape.maxFree, "$level seed $seed has $free free arrows")
                assertTrue(puzzle.encode().length <= 32)
            }
        }
    }

    // --- slide -------------------------------------------------------------------------------------------------------

    @Test
    fun slideBoardsAreTheRightSizeAndNotTooEasy() {
        val minimum = mapOf(Difficulty.Easy to 8, Difficulty.Medium to 14, Difficulty.Hard to 24)
        for (level in Difficulty.entries) {
            val shape = GameLevels.slide(level)
            for (seed in seeds) {
                val board = SlideGame.generate(seed, shape.cols, shape.rows, shape.scramble)
                assertEquals(shape.cols * shape.rows, board.size)
                assertTrue(!SlideGame.isSolved(board), "$level seed $seed")
                assertTrue(SlideGame.distance(board, shape.cols) >= minimum.getValue(level) - 4, "$level seed $seed is too easy")
            }
        }
        assertEquals(9, GameLevels.slide(Difficulty.Easy).cols * GameLevels.slide(Difficulty.Easy).rows)
        assertEquals(16, GameLevels.slide(Difficulty.Hard).cols * GameLevels.slide(Difficulty.Hard).rows)
    }

    @Test
    fun theSlideBoardOfTheOldSizeIsTheBoardThatAlwaysCame() {
        for (seed in seeds) {
            assertContentEquals(SlideGame.generate(seed, 3, 4, 80), SlideGame.generate(seed, GameLevels.slide(Difficulty.Medium).cols, GameLevels.slide(Difficulty.Medium).rows, GameLevels.slide(Difficulty.Medium).scramble))
        }
    }

    // --- target ------------------------------------------------------------------------------------------------------

    @Test
    fun targetEasyIsTwoStepsToASmallTargetAndHardNeedsThreeOrMore() {
        for (seed in seeds) {
            val easy = TargetGenerator.generate(seed, Difficulty.Easy)
            assertEquals(2, easy.par, "easy seed $seed")
            assertTrue(easy.target in 10..40, "easy seed $seed target ${easy.target}")
            val hard = TargetGenerator.generate(seed, Difficulty.Hard)
            assertTrue(hard.par >= 4, "hard seed $seed par ${hard.par}")
            assertEquals(6, hard.numbers.size, "hard seed $seed numbers")
            assertEquals(1, hard.numbers.count { it in 10..20 }, "hard seed $seed one big number")
            assertTrue(hard.target in 100..300, "hard seed $seed target ${hard.target}")
            assertEquals(hard.par, TargetGenerator.minSteps(hard.numbers, hard.target))
        }
    }

    // --- lights, mines -----------------------------------------------------------------------------------------------

    @Test
    fun lightsAndMinesGrowWithTheLevel() {
        val lights = Difficulty.entries.map { GameLevels.lights(it) }
        assertTrue(lights[0].size * lights[0].size < lights[1].size * lights[1].size)
        assertTrue(lights[1].taps < lights[2].taps)
        val mines = Difficulty.entries.map { GameLevels.mines(it) }
        assertTrue(mines.map { it.mines.toFloat() / (it.rows * it.cols) }.zipWithNext().all { (a, b) -> a < b }, "the density rises: $mines")
    }

    // --- tubes -------------------------------------------------------------------------------------------------------

    @Test
    fun tubeBoardsHaveTheRightColorsAndCanBeSolved() {
        for (level in Difficulty.entries) {
            val shape = GameLevels.tubes(level)
            repeat(15) { n ->
                val layout = TubesGame.newLayout(Random(n), shape.colors, empty = shape.empty)
                assertEquals(shape.colors + shape.empty, layout.size)
                assertEquals(shape.colors, layout.flatten().toSet().size)
                assertTrue(TubesGame.canSolve(layout, TubesGame.DEFAULT_CAPACITY), "$level board $n")
            }
        }
    }
}
