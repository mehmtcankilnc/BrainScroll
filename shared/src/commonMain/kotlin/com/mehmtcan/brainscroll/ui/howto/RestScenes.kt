package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.ui.graphics.Color
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.theme.world

// The pictures of the last games: the word games, memory, the rest. A way of playing and the first move for each (see GridScenes.kt).

private val Blue = Color(0xFF64B5F6)
private val Gold = Color(0xFFFFD54F)
private val Leaf = Color(0xFF81C784)
private val Rose = Color(0xFFE57373)
private val Violet = Color(0xFFBA68C8)

private fun DemoSceneBuilder.hold(r: Int, c: Int, mark: SceneMark) = set(r, c, SceneFill.Empty, mark = mark)

// ---------------------------------------------------------------------------------------------------------------------------------
// Word: letters stand for any word so that the picture is right in every language. The hidden word is A B C D E; the first try was A C B D F.
private fun wordScene(script: DemoSceneBuilder.() -> Unit) = demoScene(2, 5, focus = GameKind.Word.world, script = script)

/** Technique: green letters stay, yellow letters go to other boxes, a grey letter is not used again. */
internal val WordFixScene = wordScene {
    row(0, "ACBDF")
    set(0, 4, SceneFill.Given, "F", ink = Color(0xFF777777))
    tint(0.04f, 0.94f, SceneRole.Good, listOf(cell(0, 0), cell(0, 3)))
    tint(0.04f, 0.94f, SceneRole.Present, listOf(cell(0, 1), cell(0, 2)))
    ring(0.1f, 0.4f, SceneRole.Evidence, 0 to 1, 0 to 2)
    for ((k, letter) in listOf("A", "B", "C", "D", "E").withIndex()) place(0.45f + k * 0.06f, 1, k, letter, fill = SceneFill.Given)
    tint(0.78f, 0.94f, SceneRole.Good, rowCells(1))
}

/** First step: a first word with common letters shows at once which letters are in the word. */
internal val WordStartScene = wordScene {
    for ((k, letter) in listOf("A", "E", "R", "N", "L").withIndex()) place(0.08f + k * 0.07f, 0, k, letter, fill = SceneFill.Given)
    tint(0.55f, 0.94f, SceneRole.Good, listOf(cell(0, 0)))
    tint(0.55f, 0.94f, SceneRole.Present, listOf(cell(0, 1), cell(0, 3)))
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Memory: twelve cards face down; the letters stand for the shapes.
private fun memoryScene(script: DemoSceneBuilder.() -> Unit) = demoScene(3, 4, focus = GameKind.Memory.world) {
    for (r in 0 until 3) row(r, "hhhh")
    script()
}

/** Technique: remember where the cards that did not match are; when you see the same shape again, open its pair at once. */
internal val MemoryRememberScene = memoryScene {
    open(0.1f, 0, 0, "A")
    open(0.2f, 1, 2, "B")
    ring(0.24f, 0.5f, SceneRole.Evidence, 0 to 0, 1 to 2)
    close(0.5f, 0, 0)
    close(0.5f, 1, 2)
    open(0.6f, 2, 3, "A")
    ring(0.64f, 0.94f, SceneRole.Target, 0 to 0)
    open(0.76f, 0, 0, "A")
    tint(0.8f, 0.94f, SceneRole.Good, listOf(cell(0, 0), cell(2, 3)))
}

/** First step: in the first rounds open new cards: a shape not seen yet is worth more than a lucky match. */
internal val MemoryStartScene = memoryScene {
    open(0.08f, 0, 0, "A")
    open(0.16f, 0, 1, "B")
    ring(0.2f, 0.4f, SceneRole.Evidence, 0 to 0, 0 to 1)
    close(0.42f, 0, 0)
    close(0.42f, 0, 1)
    open(0.5f, 0, 2, "C")
    open(0.58f, 0, 3, "D")
    ring(0.62f, 0.9f, SceneRole.Evidence, 0 to 2, 0 to 3)
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Flash Grid: squares light up for a moment (a lit square has a mark), then the player taps them.
private fun flashScene(script: DemoSceneBuilder.() -> Unit) = demoScene(3, 3, focus = GameKind.FlashGrid.world, script = script)

/** Technique: remember the squares as one shape, not one by one. */
internal val FlashShapeScene = flashScene {
    for ((r, c) in listOf(0 to 0, 0 to 1, 1 to 1)) {
        mark(0.06f, r, c, SceneMark.Fill)
        mark(0.34f, r, c, SceneMark.None)
    }
    ring(0.08f, 0.34f, SceneRole.Evidence, 0 to 0, 0 to 1, 1 to 1)
    for ((k, at) in listOf(0 to 0, 0 to 1, 1 to 1).withIndex()) {
        tap(0.46f + k * 0.12f, at.first, at.second)
        mark(0.5f + k * 0.12f, at.first, at.second, SceneMark.Fill)
    }
}

/** First step: the first round lights one square; keep your eyes in the middle of the grid. */
internal val FlashStartScene = flashScene {
    mark(0.08f, 1, 1, SceneMark.Fill)
    mark(0.3f, 1, 1, SceneMark.None)
    ring(0.1f, 0.3f, SceneRole.Target, 1 to 1)
    tap(0.5f, 1, 1)
    mark(0.55f, 1, 1, SceneMark.Fill)
    tint(0.6f, 0.94f, SceneRole.Good, listOf(cell(1, 1)))
}

// ---------------------------------------------------------------------------------------------------------------------------------
// Topic groups: sixteen words in a 4 by 4 board, the words of the demo of the game (the language of the card).
private fun topicScene(language: Language, script: DemoSceneBuilder.(groupCells: (Int) -> List<Int>) -> Unit): DemoScene {
    val groups = groupsOf(language)
    return demoScene(4, 4, focus = GameKind.TopicGroups.world) {
        uniformText()
        TopicLayout.forEachIndexed { i, (group, word) -> set(i / 4, i % 4, SceneFill.Given, groups[group].words[word]) }
        script { g -> TopicLayout.indices.filter { TopicLayout[it].first == g } }
    }
}

/** Technique: a word that fits more than one group waits; find the sure groups first and the four words left are the last group. */
internal fun TopicLastScene(language: Language) = topicScene(language) { cells ->
    tint(0.06f, 0.94f, SceneRole.Good, cells(2))
    tint(0.26f, 0.94f, SceneRole.Focus, cells(1))
    tint(0.46f, 0.94f, SceneRole.Present, cells(3))
    ringCells(0.66f, 0.94f, SceneRole.Target, cells(0))
}

/** First step: start with the narrowest topic (the planets) and pick its four words. */
internal fun TopicStartScene(language: Language) = topicScene(language) { cells ->
    ringCells(0.06f, 0.5f, SceneRole.Target, cells(2))
    for ((k, cell) in cells(2).withIndex()) tap(0.12f + k * 0.09f, cell / 4, cell % 4)
    tint(0.58f, 0.94f, SceneRole.Good, cells(2))
}


/** Every picture of this file that does not depend on the language, with a name for the screenshots of the tests. */
internal val RestScenes: List<Pair<String, DemoScene>> by lazy {
    listOf(
        "word_fix" to WordFixScene, "word_start" to WordStartScene,
        "memory_remember" to MemoryRememberScene, "memory_start" to MemoryStartScene,
        "flash_shape" to FlashShapeScene, "flash_start" to FlashStartScene,
        "topic_last_tr" to TopicLastScene(Language.TR), "topic_start_en" to TopicStartScene(Language.EN),
    )
}
