package com.mehmtcan.brainscroll.game

import com.mehmtcan.brainscroll.game.hanoi.HanoiGenerator
import com.mehmtcan.brainscroll.game.hanoi.HanoiSolver
import com.mehmtcan.brainscroll.game.memory.CardSymbol
import com.mehmtcan.brainscroll.game.memory.MemoryGame
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The levels of the tower of Hanoi and of memory match. */
class LevelsOfHanoiAndMemoryTest {

    private val seeds = (1..200).toList()

    @Test
    fun hanoiHasThreeFiveAndFourDisksAtEasyHardAndMedium() {
        assertEquals(3, HanoiGenerator.disksFor(Difficulty.Easy))
        assertEquals(4, HanoiGenerator.disksFor(Difficulty.Medium))
        assertEquals(5, HanoiGenerator.disksFor(Difficulty.Hard))
        for (level in Difficulty.entries) {
            for (seed in seeds) {
                val puzzle = HanoiGenerator.generate(seed, level)
                assertEquals(HanoiGenerator.disksFor(level), puzzle.start.size, "$level seed $seed")
                assertFalse(puzzle.start.all { it == puzzle.goal }, "$level seed $seed starts solved")
                assertTrue(puzzle.par >= HanoiGenerator.minParFor(level), "$level seed $seed par ${puzzle.par}")
                assertEquals(HanoiSolver.distance(puzzle.start, puzzle.goal), puzzle.par, "$level seed $seed")
            }
        }
    }

    @Test
    fun hanoiMediumIsTheBoardThatAlwaysCameForASeed() {
        for (seed in seeds) {
            val old = HanoiGenerator.generate(seed)
            val medium = HanoiGenerator.generate(seed, Difficulty.Medium)
            assertContentEquals(old.start, medium.start, "seed $seed")
            assertEquals(old.goal, medium.goal)
            assertEquals(old.par, medium.par)
        }
    }

    @Test
    fun hanoiGetsHarderWithTheLevel() {
        val average = Difficulty.entries.associateWith { level -> seeds.map { HanoiGenerator.generate(it, level).par }.average() }
        assertTrue(average.getValue(Difficulty.Easy) < average.getValue(Difficulty.Medium), "$average")
        assertTrue(average.getValue(Difficulty.Medium) < average.getValue(Difficulty.Hard), "$average")
    }

    @Test
    fun hanoiBoardsAtALevelStayDifferentFromEachOther() {
        for (level in Difficulty.entries) {
            val distinct = seeds.map { HanoiGenerator.generate(it, level) }.map { it.start.toList() to it.goal }.toSet()
            assertTrue(distinct.size > 20, "$level: ${distinct.size} different boards")
        }
    }

    @Test
    fun memoryHasFourSixAndEightPairs() {
        assertEquals(4, MemoryGame.pairsFor(Difficulty.Easy))
        assertEquals(6, MemoryGame.pairsFor(Difficulty.Medium))
        assertEquals(8, MemoryGame.pairsFor(Difficulty.Hard))
        for (level in Difficulty.entries) {
            val layout = MemoryGame.newLayout(Random(5), level)
            assertEquals(2 * MemoryGame.pairsFor(level), layout.size)
            assertTrue(layout.groupingBy { it }.eachCount().values.all { it == 2 }, "$level: two of each symbol")
            assertEquals(0, layout.size % 4, "$level: whole rows of four")
        }
    }

    @Test
    fun memoryHardUsesEverySymbolThereIs() {
        assertEquals(CardSymbol.entries.toSet(), MemoryGame.newLayout(Random(1), Difficulty.Hard).toSet())
    }

    @Test
    fun memoryMediumIsTheBoardThatAlwaysCameForARandom() {
        for (seed in seeds) {
            assertEquals(MemoryGame.newLayout(Random(seed)), MemoryGame.newLayout(Random(seed), Difficulty.Medium), "seed $seed")
        }
    }
}
