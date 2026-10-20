package com.mehmtcan.brainscroll.game.topicgroups

import kotlin.random.Random

enum class TopicGroupsStatus { Playing, Won }

/** What a tap on a word did. */
enum class TopicGroupsResult {
    /** Nothing happened: a word that is already in a found group, a fifth word while four wait, or the game is over. */
    Ignored,

    /** The word was picked. */
    Selected,

    /** The word was put back. */
    Deselected,

    /** The fourth word completed a group: it is found. */
    GroupSolved,

    /** The fourth word completed the group that was left: the board is solved. */
    Solved,

    /** The four picked words are not a group: a mistake. */
    Wrong,

    /** The four picked words were tried before: no new mistake. */
    Repeated,
}

/**
 * The picture of the board for the UI. Words are numbered as in [TopicGroupsPuzzle]: the group of word `w` is `w / 4`.
 */
data class TopicGroupsState(
    /** The sixteen words, by number. */
    val words: List<String>,
    /** The name of each group, by group number. */
    val titles: List<String>,
    /** The groups that are found, in the order they were found: the first lies on the top row. */
    val solved: List<Int>,
    /** The words that are not in a found group, in the order they lie on the board. */
    val board: List<Int>,
    /** The words that are picked, in the order they were picked. */
    val selected: List<Int>,
    val mistakes: Int,
    /** Goes up with every four words that were rejected (also a repeat, which is no mistake), so the screen can react to each. */
    val rejections: Int,
    /** The four words of the last rejection. */
    val lastRejected: List<Int>,
    val status: TopicGroupsStatus,
) {
    val foundCount: Int get() = solved.size
    val canClear: Boolean get() = selected.isNotEmpty()
    val canShuffle: Boolean get() = board.size > 1 && status == TopicGroupsStatus.Playing

    /** The four words of group [group]. */
    fun wordsOf(group: Int): List<Int> = (group * TOPIC_WORDS_PER_GROUP until (group + 1) * TOPIC_WORDS_PER_GROUP).toList()
}

/**
 * The rules of the word groups game, free of UI and time.
 *
 * Sixteen words hide four groups of four that share a topic. The player picks words; with the fourth, the pick is checked: if the
 * four are a group, it is found and its topic shown; if not, it is a mistake (the same four a second time are no new mistake) and
 * the pick stays so one word can be swapped. When three groups are found the fourth is too. There is no losing; the mistakes are
 * only counted.
 */
class TopicGroupsGame(private val puzzle: TopicGroupsPuzzle) {
    private val solved = mutableListOf<Int>()
    private var board = puzzle.initialOrder.toMutableList()
    private val selected = mutableListOf<Int>()
    private var mistakes = 0
    private var rejections = 0
    private var lastRejected: List<Int> = emptyList()
    private val tried = mutableSetOf<Set<Int>>()

    val isFinished: Boolean get() = solved.size == TOPIC_GROUP_COUNT

    val state: TopicGroupsState
        get() = TopicGroupsState(
            words = puzzle.words,
            titles = puzzle.groups.map { it.title },
            solved = solved.toList(),
            board = board.toList(),
            selected = selected.toList(),
            mistakes = mistakes,
            rejections = rejections,
            lastRejected = lastRejected,
            status = if (isFinished) TopicGroupsStatus.Won else TopicGroupsStatus.Playing,
        )

    /** Picks or puts back [word]; the fourth pick is checked. */
    fun toggle(word: Int): TopicGroupsResult {
        if (isFinished || word !in puzzle.words.indices || word !in board) return TopicGroupsResult.Ignored
        if (word in selected) {
            selected -= word
            return TopicGroupsResult.Deselected
        }
        if (selected.size == TOPIC_WORDS_PER_GROUP) return TopicGroupsResult.Ignored
        selected += word
        if (selected.size < TOPIC_WORDS_PER_GROUP) return TopicGroupsResult.Selected
        return check()
    }

    private fun check(): TopicGroupsResult {
        val picked = selected.toList()
        val group = picked.first() / TOPIC_WORDS_PER_GROUP
        if (picked.any { it / TOPIC_WORDS_PER_GROUP != group }) {
            lastRejected = picked
            rejections++
            if (tried.add(picked.toSet())) {
                mistakes++
                return TopicGroupsResult.Wrong
            }
            return TopicGroupsResult.Repeated
        }
        take(group)
        selected.clear()
        if (solved.size == TOPIC_GROUP_COUNT - 1) {
            // Only one group can be left, so it is found as well.
            take((0 until TOPIC_GROUP_COUNT).first { it !in solved })
            return TopicGroupsResult.Solved
        }
        return TopicGroupsResult.GroupSolved
    }

    private fun take(group: Int) {
        solved += group
        board.removeAll { it / TOPIC_WORDS_PER_GROUP == group }
    }

    /** Puts every picked word back. Returns false if none was picked. */
    fun clearSelection(): Boolean {
        if (isFinished || selected.isEmpty()) return false
        selected.clear()
        return true
    }

    /** Mixes up the words that are not found yet, as [seed] says. The picked words stay picked. Returns false if there is nothing to mix. */
    fun shuffle(seed: Int): Boolean {
        if (isFinished || board.size < 2) return false
        board = board.shuffled(Random(seed)).toMutableList()
        return true
    }
}
