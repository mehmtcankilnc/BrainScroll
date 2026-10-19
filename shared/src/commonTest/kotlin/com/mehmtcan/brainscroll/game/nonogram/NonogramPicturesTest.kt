package com.mehmtcan.brainscroll.game.nonogram

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NonogramPicturesTest {

    private fun rowsOf(size: Int, cells: Set<Int>): List<List<Boolean>> = List(size) { r -> List(size) { c -> r * size + c in cells } }

    private val minimum = mapOf(5 to 12, 8 to 40, 10 to 15)

    @Test
    fun thereAreEnoughPicturesAtEverySize() {
        for ((size, count) in minimum) assertTrue(NonogramPictures.of(size).size >= count, "$size by $size: only ${NonogramPictures.of(size).size}")
    }

    @Test
    fun everyPictureIsSolvableByLogicAndWellFormed() {
        for (size in minimum.keys) for (picture in NonogramPictures.of(size)) {
            assertEquals(size, picture.size)
            assertTrue(NonogramSolver.isSolvable(rowsOf(size, picture.cells)), "${picture.id} ($size) needs guessing")
            assertTrue(NonogramSolver.isSolvable(rowsOf(size, picture.mirroredCells)), "${picture.id} ($size) mirrored needs guessing")
            val share = picture.cells.size * 100 / (size * size)
            assertTrue(share in 34..66, "${picture.id} ($size) is $share% filled")
            val rows = rowsOf(size, picture.cells)
            val emptyLines = rows.count { row -> row.none { it } } + (0 until size).count { c -> rows.none { it[c] } }
            assertTrue(emptyLines <= 1, "${picture.id} ($size) has $emptyLines empty lines")
        }
    }

    @Test
    fun namesAreFilledAndNothingRepeatsAtAnySize() {
        for (size in minimum.keys) {
            val pictures = NonogramPictures.of(size)
            for (p in pictures) assertTrue(p.nameTr.isNotBlank() && p.nameEn.isNotBlank(), p.id)
            assertEquals(pictures.size, pictures.map { it.id }.toSet().size, "ids ($size)")
            assertEquals(pictures.size, pictures.map { it.nameEn }.toSet().size, "English names ($size)")
            assertEquals(pictures.size, pictures.map { it.cells }.toSet().size, "shapes ($size)")
            // No picture is the mirror image of another one.
            val shapes = pictures.map { it.cells }.toSet()
            for (p in pictures) assertTrue(p.mirroredCells == p.cells || p.mirroredCells !in shapes, "${p.id} ($size) has its mirror in the list")
        }
    }

    @Test
    fun aBoardFindsItsPictureWhetherTurnedOrNot() {
        for (size in minimum.keys) {
            val picture = NonogramPictures.of(size).first { it.cells != it.mirroredCells }
            assertEquals(picture.id, assertNotNull(NonogramPuzzle.of(picture, false).picture).id)
            assertEquals(picture.id, assertNotNull(NonogramPuzzle.of(picture, true).picture).id)
            assertTrue(NonogramPuzzle.of(picture, true).encode().endsWith(":1"))
        }
    }

    @Test
    fun everyBoardIsWrittenWithinTheTableLimitAndReadsBack() {
        for (size in minimum.keys) for (picture in NonogramPictures.of(size)) for (mirrored in listOf(false, true)) {
            val puzzle = NonogramPuzzle.of(picture, mirrored)
            val text = puzzle.encode()
            assertTrue(text.length <= 32, "$text is ${text.length} long")
            val back = assertNotNull(NonogramPuzzle.decode(text), text)
            assertEquals(puzzle.solution, back.solution, "${picture.id} ($size)")
            assertEquals(mirrored, back.mirrored)
        }
    }

    @Test
    fun theMediumBoardKeepsTheTextItAlwaysHad() {
        // 8 by 8: the picture as 16 hex digits, row by row from the top left, bit 0 the first cell; saved games rely on it.
        val heart = NonogramPictures.all.first { it.id == "heart" }
        assertEquals("8:00183c7effffff66:0", NonogramPuzzle.of(heart, false).encode())
        assertEquals("8:00183c7effffff66:1", NonogramPuzzle.of(heart, true).encode())
        assertEquals(heart.cells, NonogramPuzzle.decode("8:00183c7effffff66:0")!!.solution)
    }

    @Test
    fun theMediumBoardOfASeedIsTheBoardThatAlwaysCame() {
        // The pictures are chosen by position in the 8 by 8 list, so the list must keep its order and the generator its draw.
        for (seed in 1..30) {
            val old = NonogramGenerator.generate(seed)
            val medium = NonogramGenerator.generate(seed, com.mehmtcan.brainscroll.game.Difficulty.Medium)
            assertEquals(old.encode(), medium.encode(), "seed $seed")
            assertEquals(8, old.size)
        }
    }

    @Test
    fun everyLevelMakesTheBoardOfItsSize() {
        for ((level, size) in listOf(com.mehmtcan.brainscroll.game.Difficulty.Easy to 5, com.mehmtcan.brainscroll.game.Difficulty.Medium to 8, com.mehmtcan.brainscroll.game.Difficulty.Hard to 10)) {
            for (seed in 1..40) {
                val puzzle = NonogramGenerator.generate(seed, level)
                assertEquals(size, puzzle.size, "$level seed $seed")
                assertNotNull(puzzle.picture, "$level seed $seed shows a drawn picture")
            }
        }
    }
}
