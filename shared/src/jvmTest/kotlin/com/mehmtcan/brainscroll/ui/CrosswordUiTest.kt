package com.mehmtcan.brainscroll.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.crossword.CrosswordClue
import com.mehmtcan.brainscroll.game.crossword.CrosswordLayout
import com.mehmtcan.brainscroll.game.crossword.CrosswordTestSolver
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.crossword.CrosswordClueTexts
import com.mehmtcan.brainscroll.ui.crossword.CrosswordPicked
import com.mehmtcan.brainscroll.ui.crossword.CrosswordSquareInfo
import com.mehmtcan.brainscroll.ui.crossword.CrosswordView
import com.mehmtcan.brainscroll.ui.haptics.HapticKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The crossword page in the feed, driven through the real screen. The board is random, so the tests read it the way a screen reader
 * does and find the answers the way a player does: from the clues, with the words of the bank. The board starts as a far view; a
 * square is brought into view by a double tap on the board, or by pressing it the way a screen reader does.
 */
@OptIn(ExperimentalTestApi::class)
class CrosswordUiTest {

    private val games = listOf(GameKind.Word, GameKind.Crossword)
    private val cells = CrosswordLayout.CELLS
    private val size = CrosswordLayout.SIZE

    private fun ComposeUiTest.openCrossword(haptics: RecordingHaptics? = null, reduceMotion: Boolean? = null) {
        start(haptics = haptics, reduceMotion = reduceMotion, games = games)
        swipeFeedUp()
        mainClock.advanceTimeBy(1_500)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) { onAllNodesWithTag("crosswordSquare").fetchSemanticsNodes().size >= cells }
    }

    /** The squares of the page, as the screen reader's info: "#" a clue, "." empty, a letter, "!" and a letter when it is wrong. */
    private fun ComposeUiTest.squares(): List<String> = onAllNodesWithTag("crosswordSquare").fetchSemanticsNodes().take(cells).map { it.config[CrosswordSquareInfo] }

    private fun ComposeUiTest.isClue() = squares().map { it == "#" }

    /** The clues the player sees, in the order of the squares. */
    private fun ComposeUiTest.clues(): List<CrosswordClue> {
        val isClue = BooleanArray(cells) { isClue()[it] }
        val texts = onAllNodesWithTag("crosswordSquare").fetchSemanticsNodes().take(cells).map { it.config.getOrNull(CrosswordClueTexts) }
        val slots = CrosswordLayout.slots(isClue)!!
        val result = ArrayList<CrosswordClue>()
        // The clues of a square are written in the order across first, then the straight arrow before the one that turns.
        for ((cell, held) in slots.groupBy { it.clueCell }) {
            val ordered = held.sortedWith(compareByDescending<com.mehmtcan.brainscroll.game.crossword.CrosswordSlot> { it.across }.thenBy { it.bent })
            ordered.forEachIndexed { k, slot -> result += CrosswordClue(cell, slot.across, texts[cell]!![k].drop(2), slot.cells, slot.bent) }
        }
        return result
    }

    /** The letters of the board and its language, worked out from the clues. */
    private fun ComposeUiTest.answer(): Pair<Language, CharArray> {
        for (language in Language.entries) CrosswordTestSolver.solve(language, isClue(), clues())?.let { return language to it }
        error("the clues of the page are not in the bank of any language")
    }

    private fun find(node: SemanticsNode): List<Int>? =
        node.config.getOrNull(CrosswordPicked) ?: node.children.firstNotNullOfOrNull { find(it) }

    private fun ComposeUiTest.board() = onAllNodesWithTag("crosswordBoard").fetchSemanticsNodes().first()

    /** The squares of the answer that is picked, in order, with the square of the cursor last. */
    private fun ComposeUiTest.picked(): List<Int> = find(board())!!

    private fun ComposeUiTest.view(): String = board().config[CrosswordView]

    /** A square is pressed the way a screen reader does it: the board zooms in on it and picks it. */
    private fun ComposeUiTest.tapSquare(cell: Int) {
        onAllNodesWithTag("crosswordSquare")[cell].performSemanticsAction(SemanticsActions.OnClick)
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
    }

    /** Double-taps the board on the spot where a square is shown in the far view. */
    private fun ComposeUiTest.doubleTapOn(cell: Int) {
        val square = onAllNodesWithTag("crosswordSquare").fetchSemanticsNodes()[cell].boundsInRoot
        val board = board().boundsInRoot
        val at = Offset(square.center.x - board.left, square.center.y - board.top)
        onAllNodesWithTag("crosswordBoard")[0].performTouchInput { doubleClick(at) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
    }

    /**
     * The index of the key of a letter on the keyboard of the page that is on screen: of the clickable things with that letter (the
     * squares of the board have it too, and the word puzzle above has a keyboard) the lowest one that is on the screen.
     */
    private fun ComposeUiTest.keyIndex(letter: Char): Int {
        val screen = onRoot().fetchSemanticsNode().boundsInRoot
        val nodes = onAllNodes(hasText(letter.toString()) and hasClickAction()).fetchSemanticsNodes()
        return nodes.indices.filter { nodes[it].boundsInRoot.let { b -> b.top >= 0f && b.bottom <= screen.bottom && b.width > 0f } }.maxBy { nodes[it].boundsInRoot.top }
    }

    private fun ComposeUiTest.key(letter: Char) {
        onAllNodes(hasText(letter.toString()) and hasClickAction())[keyIndex(letter)].performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.backspaceKey() {
        val screen = onRoot().fetchSemanticsNode().boundsInRoot
        val at = onAllNodesWithTag("keyBackspace").fetchSemanticsNodes().indexOfFirst { it.boundsInRoot.top >= 0f && it.boundsInRoot.bottom <= screen.bottom && it.boundsInRoot.width > 0f }
        onAllNodesWithTag("keyBackspace")[at].performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.description(cell: Int) =
        onAllNodesWithTag("crosswordSquare")[cell].fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()

    private fun ComposeUiTest.won() = hasAnyText("Bulmaca çözüldü!", "Crossword solved!")

    @Test
    fun theBoardIsSixteenBySixteenAndStartsAsAFarViewWithAHintToZoom() = runComposeUiTest {
        openCrossword()
        snap("300_crossword_start")
        assertEquals(cells, squares().size)
        assertEquals("overview", view())
        assertTrue(isClue()[0], "the first square is a clue")
        assertTrue(isClue().count { it } in 55..100)
        assertTrue(squares().filter { it != "#" }.all { it == "." })
        assertTrue(onAllNodesWithTag("crosswordZoomHint").fetchSemanticsNodes().isNotEmpty(), "the hint to double-tap is shown")
        assertTrue(onAllNodesWithTag("crosswordBanner").fetchSemanticsNodes().isEmpty(), "no clue is shown before zooming in")
        assertTrue(hasAnyText("Hata: 0", "Mistakes: 0"))
        answer() // the clues lead to one answer
        // The whole board is on the screen in the far view.
        val root = onRoot().fetchSemanticsNode().boundsInRoot
        val boardBounds = board().boundsInRoot
        for (node in onAllNodesWithTag("crosswordSquare").fetchSemanticsNodes()) {
            val b = node.boundsInRoot
            assertTrue(b.left >= boardBounds.left - 1f && b.right <= boardBounds.right + 1f && b.top >= boardBounds.top - 1f && b.bottom <= boardBounds.bottom + 1f, "a square is outside the board: $b against $boardBounds")
        }
        assertTrue(boardBounds.left >= root.left && boardBounds.right <= root.right)
    }

    @Test
    fun aDoubleTapZoomsInOnTheSpotAndAnotherOneZoomsOut() = runComposeUiTest {
        openCrossword()
        val letter = squares().indexOfFirst { it == "." }
        doubleTapOn(letter)
        snap("301_crossword_zoomed")
        assertEquals("zoomed", view())
        assertEquals(letter, picked().last(), "the square that was tapped is the cursor")
        assertTrue(onAllNodesWithTag("crosswordBanner").fetchSemanticsNodes().isNotEmpty(), "its clue is under the board")
        assertTrue(onAllNodesWithTag("crosswordZoomHint").fetchSemanticsNodes().isEmpty())
        // At the fixed size most of the board is out of view: only a few squares are whole inside it.
        val inside = board().boundsInRoot
        val whole = onAllNodesWithTag("crosswordSquare").fetchSemanticsNodes().count { b -> b.boundsInRoot.let { it.width >= 50f && it.left >= inside.left - 1f && it.right <= inside.right + 1f && it.top >= inside.top - 1f && it.bottom <= inside.bottom + 1f } }
        assertTrue(whole in 6..60, "$whole squares are whole in view")
        // A double tap again, anywhere on the board, goes back to the far view.
        onAllNodesWithTag("crosswordBoard")[0].performTouchInput { doubleClick(center) }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertEquals("overview", view())
    }

    @Test
    fun theCornerButtonZoomsOutToo() = runComposeUiTest {
        openCrossword()
        tapSquare(squares().indexOfFirst { it == "." })
        assertEquals("zoomed", view())
        onAllNodesWithTag("crosswordZoomOut")[0].performClick()
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertEquals("overview", view())
    }

    @Test
    fun theCluesAreBigEnoughToReadWhenZoomedIn() = runComposeUiTest {
        openCrossword()
        val first = squares().indexOfFirst { it == "." }
        val overview = onAllNodesWithTag("crosswordSquare").fetchSemanticsNodes()[first].boundsInRoot.width
        tapSquare(first)
        val zoomed = onAllNodesWithTag("crosswordSquare").fetchSemanticsNodes()[picked().last()].boundsInRoot.width
        assertTrue(zoomed > overview * 2f, "a square is $overview wide far away and $zoomed zoomed in")
        assertTrue(zoomed >= 50f, "a square is $zoomed wide zoomed in")
    }

    @Test
    fun aRightLetterFillsTheSquareAndTheCursorMovesOn() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openCrossword(haptics)
        val (_, letters) = answer()
        tapSquare(squares().indexOfFirst { it == "." })
        val cursor = picked().last()
        haptics.kinds.clear()
        key(letters[cursor])
        assertEquals(letters[cursor].toString(), squares()[cursor])
        assertTrue(picked().last() != cursor, "the cursor is on the next square")
        assertTrue(HapticKind.Medium in haptics.kinds)
        assertTrue(hasAnyText("Hata: 0", "Mistakes: 0"))
    }

    @Test
    fun aWrongLetterShowsInRedCountsAMistakeAndBackspaceTakesItBack() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openCrossword(haptics)
        val (language, letters) = answer()
        tapSquare(squares().indexOfFirst { it == "." })
        val cursor = picked().last()
        val wrong = language.alphabet.first { it != letters[cursor] && it != 'Q' && it != 'W' && it != 'X' }
        haptics.kinds.clear()
        key(wrong)
        assertEquals("!$wrong", squares()[cursor])
        assertEquals(cursor, picked().last(), "the cursor stays on the wrong letter")
        assertTrue(hasAnyText("Hata: 1", "Mistakes: 1"))
        assertEquals(HapticKind.Warning, haptics.kinds.last())
        snap("302_crossword_wrong")
        backspaceKey()
        assertEquals(".", squares()[cursor])
    }

    @Test
    fun tappingASquareThatIsInTwoAnswersTwiceSwapsAcrossAndDown() = runComposeUiTest {
        openCrossword()
        val clues = clues()
        val crossing = (0 until cells).first { cell -> clues.count { cell in it.cells } == 2 }
        tapSquare(crossing)
        val first = picked()
        assertEquals(crossing, first.last())
        tapSquare(crossing)
        val second = picked()
        assertEquals(crossing, second.last())
        assertTrue(first.toSet() != second.toSet(), "the other answer is picked now")
        val both = clues.filter { crossing in it.cells }.map { it.cells.toSet() }
        assertTrue(first.toSet() in both && second.toSet() in both)
    }

    @Test
    fun theNextKeyMovesToAnotherAnswer() = runComposeUiTest {
        openCrossword()
        tapSquare(squares().indexOfFirst { it == "." })
        val before = picked().toSet()
        val label = if (onAllNodes(hasText("Sonraki") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()) "Sonraki" else "Next"
        val screen = onRoot().fetchSemanticsNode().boundsInRoot
        val at = onAllNodes(hasText(label) and hasClickAction()).fetchSemanticsNodes().indexOfFirst { it.boundsInRoot.top >= 0f && it.boundsInRoot.bottom <= screen.bottom }
        onAllNodes(hasText(label) and hasClickAction())[at].performClick()
        waitForIdle()
        mainClock.advanceTimeBy(1_000)
        assertTrue(picked().toSet() != before)
    }

    @Test
    fun theViewFollowsTheCursorAlongAnAnswer() = runComposeUiTest {
        openCrossword()
        val (_, letters) = answer()
        val clue = clues().first { it.cells.size >= 5 }
        tapSquare(clue.cells.first())
        val board = board().boundsInRoot
        for (cell in clue.cells) {
            key(letters[cell])
            mainClock.advanceTimeBy(1_000)
            waitForIdle()
            val b = onAllNodesWithTag("crosswordSquare").fetchSemanticsNodes()[picked().last()].boundsInRoot
            assertTrue(b.left >= board.left - 1f && b.right <= board.right + 1f && b.top >= board.top - 1f && b.bottom <= board.bottom + 1f, "the cursor is out of view: $b against $board")
        }
    }

    @Test
    fun aScreenReaderHearsTheCluesAndWhatIsInEachSquare() = runComposeUiTest {
        openCrossword()
        val clue = clues().first()
        val clueText = description(clue.cell)
        assertTrue(clueText.contains(clue.text), clueText)
        val letterCell = squares().indexOf(".")
        val text = description(letterCell)
        assertTrue(text.contains("${letterCell / size + 1}") && text.contains("${letterCell % size + 1}"), text)
        assertTrue(text.contains("boş") || text.contains("empty"), text)
    }

    @Test
    fun typingTheWholeBoardRightWinsShowsTheTimeAndZoomsBackOut() = runComposeUiTest {
        openCrossword()
        val (_, letters) = answer()
        for (cell in letters.indices) {
            if (letters[cell] == ' ') continue
            tapSquare(cell)
            key(letters[cell])
        }
        mainClock.advanceTimeBy(2_500)
        waitForIdle()
        snap("303_crossword_solved")
        assertTrue(won())
        assertTrue(onAllNodesWithTag("favoriteHeart").fetchSemanticsNodes().isNotEmpty())
        assertEquals("overview", view(), "the solved board shows itself whole")
    }

    @Test
    fun theWinBuzzesOnlyWhenTheLastLetterHasSettled() = runComposeUiTest {
        val haptics = RecordingHaptics()
        openCrossword(haptics)
        val (_, letters) = answer()
        val filled = letters.indices.filter { letters[it] != ' ' }
        for (cell in filled.dropLast(1)) {
            tapSquare(cell)
            key(letters[cell])
        }
        tapSquare(filled.last())
        haptics.kinds.clear()
        mainClock.autoAdvance = false
        onAllNodes(hasText(letters[filled.last()].toString()) and hasClickAction())[keyIndex(letters[filled.last()])].performClick()
        mainClock.advanceTimeBy(100)
        assertTrue(HapticKind.Success !in haptics.kinds, "no success buzz while the last letter is still popping in: ${haptics.kinds}")
        mainClock.advanceTimeBy(1_200)
        assertTrue(HapticKind.Success in haptics.kinds, "the success buzz comes once it has settled: ${haptics.kinds}")
    }
}
