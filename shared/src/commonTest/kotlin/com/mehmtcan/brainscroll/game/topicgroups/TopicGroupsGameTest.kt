package com.mehmtcan.brainscroll.game.topicgroups

import com.mehmtcan.brainscroll.game.wordle.Language
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TopicGroupsGameTest {

    private val puzzle = TopicGroupsPuzzle(Language.EN, listOf(0, 1, 2, 3), 7)
    private fun game() = TopicGroupsGame(puzzle)

    /** The four words of group [g]. */
    private fun group(g: Int) = (g * 4 until g * 4 + 4).toList()

    private fun TopicGroupsGame.pick(words: List<Int>): TopicGroupsResult = words.map { toggle(it) }.last()

    @Test
    fun aNewBoardHasAllSixteenWordsAndNothingPicked() {
        val s = game().state
        assertEquals(16, s.board.size)
        assertEquals(puzzle.initialOrder, s.board)
        assertTrue(s.selected.isEmpty() && s.solved.isEmpty())
        assertEquals(0, s.mistakes)
        assertEquals(TopicGroupsStatus.Playing, s.status)
        assertFalse(s.canClear)
        assertTrue(s.canShuffle)
    }

    @Test
    fun tappingPicksAWordAndTappingItAgainPutsItBack() {
        val g = game()
        assertEquals(TopicGroupsResult.Selected, g.toggle(3))
        assertEquals(listOf(3), g.state.selected)
        assertEquals(TopicGroupsResult.Deselected, g.toggle(3))
        assertTrue(g.state.selected.isEmpty())
    }

    @Test
    fun theFourthWordOfAGroupFindsItWithoutAMistake() {
        val g = game()
        assertEquals(TopicGroupsResult.GroupSolved, g.pick(group(2)))
        val s = g.state
        assertEquals(listOf(2), s.solved)
        assertEquals(12, s.board.size)
        assertTrue(s.board.none { it / 4 == 2 })
        assertTrue(s.selected.isEmpty())
        assertEquals(0, s.mistakes)
    }

    @Test
    fun fourWordsThatAreNotAGroupAreAMistakeAndStayPicked() {
        val g = game()
        val wrong = listOf(0, 1, 2, 4)
        assertEquals(TopicGroupsResult.Wrong, g.pick(wrong))
        val s = g.state
        assertEquals(1, s.mistakes)
        assertEquals(1, s.rejections)
        assertEquals(wrong, s.selected)
        assertEquals(wrong, s.lastRejected)
        assertTrue(s.solved.isEmpty() && s.board.size == 16)
    }

    @Test
    fun theSameWrongFourASecondTimeIsNoNewMistake() {
        val g = game()
        g.pick(listOf(0, 1, 2, 4))
        g.toggle(4)
        assertEquals(TopicGroupsResult.Repeated, g.toggle(4))
        assertEquals(1, g.state.mistakes)
        assertEquals(2, g.state.rejections)
        // The same four in another order are the same four.
        g.clearSelection()
        assertEquals(TopicGroupsResult.Repeated, g.pick(listOf(4, 2, 1, 0)))
        assertEquals(1, g.state.mistakes)
    }

    @Test
    fun swappingOneWordOfAWrongPickLooksAtTheNewFour() {
        val g = game()
        g.pick(listOf(0, 1, 2, 4))
        g.toggle(4)
        assertEquals(TopicGroupsResult.GroupSolved, g.toggle(3))
        assertEquals(listOf(0), g.state.solved)
        assertEquals(1, g.state.mistakes)
    }

    @Test
    fun aFifthWordWhileFourAreWaitingIsIgnored() {
        val g = game()
        g.pick(listOf(0, 1, 2, 4))
        assertEquals(TopicGroupsResult.Ignored, g.toggle(8))
        assertEquals(listOf(0, 1, 2, 4), g.state.selected)
    }

    @Test
    fun aWordInAFoundGroupCannotBeTapped() {
        val g = game()
        g.pick(group(0))
        assertEquals(TopicGroupsResult.Ignored, g.toggle(0))
        assertEquals(TopicGroupsResult.Ignored, g.toggle(99))
        assertEquals(TopicGroupsResult.Ignored, g.toggle(-1))
    }

    @Test
    fun findingThreeGroupsFindsTheFourthToo() {
        val g = game()
        assertEquals(TopicGroupsResult.GroupSolved, g.pick(group(1)))
        assertEquals(TopicGroupsResult.GroupSolved, g.pick(group(3)))
        assertEquals(TopicGroupsResult.Solved, g.pick(group(0)))
        val s = g.state
        assertEquals(listOf(1, 3, 0, 2), s.solved)
        assertTrue(s.board.isEmpty())
        assertEquals(TopicGroupsStatus.Won, s.status)
        assertTrue(g.isFinished)
        assertEquals(0, s.mistakes)
        assertEquals(TopicGroupsResult.Ignored, g.toggle(8))
        assertFalse(g.clearSelection())
        assertFalse(g.shuffle(3))
    }

    @Test
    fun clearPutsEveryPickedWordBack() {
        val g = game()
        g.toggle(1)
        g.toggle(5)
        assertTrue(g.state.canClear)
        assertTrue(g.clearSelection())
        assertTrue(g.state.selected.isEmpty())
        assertFalse(g.clearSelection())
    }

    @Test
    fun shuffleMixesOnlyTheWordsThatAreLeftAndKeepsThePick() {
        val g = game()
        g.pick(group(0))
        val before = g.state.board
        g.toggle(5)
        assertTrue(g.shuffle(123))
        val s = g.state
        assertEquals(before.sorted(), s.board.sorted())
        assertNotEquals(before, s.board, "the order did not change")
        assertEquals(listOf(5), s.selected)
        assertEquals(listOf(0), s.solved)
    }

    @Test
    fun theSameSeedMixesTheSameWay() {
        val a = game().also { it.shuffle(5) }.state.board
        val b = game().also { it.shuffle(5) }.state.board
        assertEquals(a, b)
    }

    @Test
    fun stateTellsTheWordsOfAGroup() {
        assertEquals(listOf(8, 9, 10, 11), game().state.wordsOf(2))
    }
}
