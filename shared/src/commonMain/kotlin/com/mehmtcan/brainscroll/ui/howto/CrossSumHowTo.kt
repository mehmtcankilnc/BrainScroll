package com.mehmtcan.brainscroll.ui.howto

import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_crosssum_1
import brainscroll.shared.generated.resources.howto_card_crosssum_2
import brainscroll.shared.generated.resources.howto_card_crosssum_3
import brainscroll.shared.generated.resources.howto_crosssum_last
import brainscroll.shared.generated.resources.howto_crosssum_start
import brainscroll.shared.generated.resources.howto_crosssum_tech
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_title_spoiler
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.world

/**
 * A 3 by 3 board of white cells with its frame: the answer is 9 7 1 / 8 9 6 / 1 4 2, so the rows add up to 17, 23 and 7 and the columns to
 * 18, 20 and 9. The dark cells with the sums and the white cells look as they do on the page.
 */
private fun crossBoard(script: DemoSceneBuilder.() -> Unit) = demoScene(4, 4, focus = GameKind.CrossSum.world) {
    clueCell(0, 0)
    clueCell(0, 1, down = 18)
    clueCell(0, 2, down = 20)
    clueCell(0, 3, down = 9)
    clueCell(1, 0, across = 17)
    clueCell(2, 0, across = 23)
    clueCell(3, 0, across = 7)
    script()
}

/** Goal: the row has a sum of 17 and 9, 7 and 1 make it. */
internal val CrossGoalScene = crossBoard {
    ring(0.06f, 0.46f, SceneRole.Evidence, 1 to 0)
    tint(0.08f, 0.94f, SceneRole.Focus, (1..3).map { cell(1, it) })
    place(0.3f, 1, 1, "9")
    place(0.42f, 1, 2, "7")
    place(0.54f, 1, 3, "1")
}

/** Keys: tap a cell, then a key puts the digit in it. */
internal val CrossKeysScene = crossBoard {
    tap(0.2f, 1, 1)
    ring(0.22f, 0.5f, SceneRole.Target, 1 to 1)
    place(0.34f, 1, 1, "9")
    tap(0.6f, 1, 2)
    ring(0.62f, 0.94f, SceneRole.Target, 1 to 2)
    place(0.74f, 1, 2, "7")
}

/** The rule: a digit twice in a block turns the block red. */
internal val CrossRepeatScene = crossBoard {
    place(0.2f, 1, 1, "9")
    place(0.34f, 1, 2, "9")
    tint(0.4f, 0.94f, SceneRole.Bad, (1..3).map { cell(1, it) })
}

/** Technique: three cells that add up to 7 can only hold 1, 2 and 4. */
internal val CrossComboScene = crossBoard {
    ring(0.06f, 0.5f, SceneRole.Evidence, 3 to 0)
    tint(0.08f, 0.94f, SceneRole.Focus, (1..3).map { cell(3, it) })
    ring(0.4f, 0.94f, SceneRole.Target, 3 to 1, 3 to 2, 3 to 3)
    place(0.6f, 3, 1, "1")
    place(0.7f, 3, 2, "4")
    place(0.8f, 3, 3, "2")
}

/** Technique: the last empty cell of a block takes what is left of the sum. */
internal val CrossLastScene = crossBoard {
    set(1, 1, SceneFill.Given, "9")
    set(1, 2, SceneFill.Given, "7")
    ring(0.06f, 0.46f, SceneRole.Evidence, 1 to 0, 1 to 1, 1 to 2)
    ring(0.46f, 0.94f, SceneRole.Target, 1 to 3)
    place(0.62f, 1, 3, "1")
}

/** First step: the block with the fewest ways, the smallest sum, is where to begin. */
internal val CrossStartScene = crossBoard {
    ring(0.06f, 0.5f, SceneRole.Evidence, 3 to 0)
    ring(0.4f, 0.94f, SceneRole.Target, 3 to 1, 3 to 2, 3 to 3)
}

/** Every picture of this file, with a name for the screenshots of the tests. */
internal val CrossSumScenes: List<Pair<String, DemoScene>> by lazy {
    listOf(
        "cross_goal" to CrossGoalScene, "cross_keys" to CrossKeysScene, "cross_repeat" to CrossRepeatScene,
        "cross_combo" to CrossComboScene, "cross_last" to CrossLastScene, "cross_start" to CrossStartScene,
    )
}

/**
 * The steps of the cross sums card: the goal, the keys, the rule about a digit twice, the sums with one way, the last cell of a block, and
 * the first step.
 */
internal val CrossSumHowTo = HowToCardContent(
    listOf(
        sceneStep(Res.string.howto_card_crosssum_1, CrossGoalScene),
        sceneStep(Res.string.howto_card_crosssum_2, CrossKeysScene),
        sceneStep(Res.string.howto_card_crosssum_3, CrossRepeatScene),
        sceneStep(Res.string.howto_crosssum_tech, CrossComboScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_crosssum_last, CrossLastScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_crosssum_start, CrossStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)
