package com.mehmtcan.brainscroll.ui.howto

import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_oddone_1
import brainscroll.shared.generated.resources.howto_card_oddone_2
import brainscroll.shared.generated.resources.howto_oddone_second
import brainscroll.shared.generated.resources.howto_oddone_start
import brainscroll.shared.generated.resources.howto_oddone_tech
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_title_spoiler
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.world

/** Five numbers in a row, as the page draws them: square tiles with the number, the right one green after a pick. */
private fun numbers(vararg values: Int, script: DemoSceneBuilder.() -> Unit) = demoScene(1, 5, focus = GameKind.OddOne.world) {
    values.forEachIndexed { i, n -> set(0, i, SceneFill.Given, n.toString()) }
    script()
}

/** Goal: four numbers follow a rule (they are all even), the nine does not. */
internal val OddGoalScene = numbers(4, 8, 9, 2, 6) {
    ring(0.06f, 0.5f, SceneRole.Evidence, 0 to 0, 0 to 1, 0 to 3, 0 to 4)
    ring(0.5f, 0.94f, SceneRole.Target, 0 to 2)
}

/** Picking: a tap on the number that breaks the rule turns it green. */
internal val OddPickScene = numbers(4, 8, 9, 2, 6) {
    tap(0.25f, 0, 2)
    tint(0.32f, 0.94f, SceneRole.Good, listOf(cell(0, 2)))
    ring(0.32f, 0.94f, SceneRole.Good, 0 to 2)
}

/** Technique: look at even and odd first, four of them are even and one is odd. */
internal val OddParityScene = numbers(14, 8, 15, 12, 6) {
    tint(0.06f, 0.5f, SceneRole.Focus, listOf(cell(0, 0), cell(0, 1), cell(0, 3), cell(0, 4)))
    ring(0.5f, 0.94f, SceneRole.Target, 0 to 2)
}

/** Technique: all of them are odd, so a third rule is needed: four are multiples of 3. */
internal val OddSecondScene = numbers(15, 21, 17, 27, 33) {
    tint(0.04f, 0.36f, SceneRole.Focus, rowCells(0))
    ring(0.4f, 0.7f, SceneRole.Evidence, 0 to 0, 0 to 1, 0 to 3, 0 to 4)
    ring(0.7f, 0.94f, SceneRole.Target, 0 to 2)
}

/** First step: put the numbers in order of size and start with the smallest and the biggest. */
internal val OddStartScene = numbers(9, 16, 12, 1, 4) {
    ring(0.06f, 0.4f, SceneRole.Evidence, 0 to 3, 0 to 1)
    ring(0.4f, 0.7f, SceneRole.Evidence, 0 to 0, 0 to 4)
    ring(0.7f, 0.94f, SceneRole.Target, 0 to 2)
}

/** Every picture of this file, with a name for the screenshots of the tests. */
internal val OddOneScenes: List<Pair<String, DemoScene>> by lazy {
    listOf(
        "odd_goal" to OddGoalScene, "odd_pick" to OddPickScene, "odd_parity" to OddParityScene,
        "odd_second" to OddSecondScene, "odd_start" to OddStartScene,
    )
}

/**
 * The steps of the odd one out card: the goal, how a pick works, the parity technique, the second rule, and the first step. The pictures are
 * scenes of five square tiles, the same as on the page.
 */
internal val OddOneHowTo = HowToCardContent(
    listOf(
        sceneStep(Res.string.howto_card_oddone_1, OddGoalScene),
        sceneStep(Res.string.howto_card_oddone_2, OddPickScene),
        sceneStep(Res.string.howto_oddone_tech, OddParityScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_oddone_second, OddSecondScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_oddone_start, OddStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)
