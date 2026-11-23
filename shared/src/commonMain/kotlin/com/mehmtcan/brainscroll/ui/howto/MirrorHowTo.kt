package com.mehmtcan.brainscroll.ui.howto

import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_card_mirror_1
import brainscroll.shared.generated.resources.howto_card_mirror_2
import brainscroll.shared.generated.resources.howto_mirror_axes
import brainscroll.shared.generated.resources.howto_mirror_start
import brainscroll.shared.generated.resources.howto_mirror_tech
import brainscroll.shared.generated.resources.howto_title_solve
import brainscroll.shared.generated.resources.howto_title_spoiler
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.theme.world

/** A 4 by 4 board with the axis down the middle. The left half is ready: the painted cells are (0,0) (1,1) (2,0) (2,1). */
private fun verticalBoard(script: DemoSceneBuilder.() -> Unit) = demoScene(4, 4, focus = GameKind.Mirror.world) {
    val painted = setOf(0 to 0, 1 to 1, 2 to 0, 2 to 1)
    for (r in 0 until 4) for (c in 0 until 4) {
        if (c < 2) set(r, c, SceneFill.Given, mark = if (r to c in painted) SceneMark.Fill else SceneMark.None) else set(r, c, SceneFill.Empty)
    }
    for (r in 0 until 4) edge(0f, r, 1, 1)
    script()
}

/** Goal: the ready half is mirrored into the empty half, cell by cell. */
internal val MirrorGoalScene = verticalBoard {
    ring(0.06f, 0.4f, SceneRole.Evidence, 0 to 0, 1 to 1, 2 to 0, 2 to 1)
    for ((k, at) in listOf(0 to 3, 1 to 2, 2 to 3, 2 to 2).withIndex()) mark(0.42f + k * 0.12f, at.first, at.second, SceneMark.Fill)
}

/** Painting: a tap paints a cell of the empty half, a second tap on the same cell empties it. */
internal val MirrorPaintScene = verticalBoard {
    tap(0.18f, 0, 3)
    mark(0.24f, 0, 3, SceneMark.Fill)
    tap(0.44f, 0, 3)
    mark(0.5f, 0, 3, SceneMark.None)
    tap(0.7f, 1, 2)
    mark(0.76f, 1, 2, SceneMark.Fill)
}

/** Technique: a cell is the same as the cell the same distance from the line on the other side. */
internal val MirrorAcrossScene = verticalBoard {
    tint(0.06f, 0.94f, SceneRole.Focus, rowCells(2))
    ring(0.1f, 0.5f, SceneRole.Evidence, 2 to 1)
    ring(0.5f, 0.94f, SceneRole.Target, 2 to 2)
    mark(0.7f, 2, 2, SceneMark.Fill)
}

/** Other axes: the same idea on a horizontal axis; on a diagonal the cell across is the one with row and column swapped. */
internal val MirrorAxesScene = demoScene(4, 4, focus = GameKind.Mirror.world) {
    val painted = setOf(0 to 0, 0 to 3, 1 to 1)
    for (r in 0 until 4) for (c in 0 until 4) {
        if (r < 2) set(r, c, SceneFill.Given, mark = if (r to c in painted) SceneMark.Fill else SceneMark.None) else set(r, c, SceneFill.Empty)
    }
    for (c in 0 until 4) edge(0f, 1, c, 2)
    ring(0.06f, 0.5f, SceneRole.Evidence, 0 to 0)
    ring(0.2f, 0.5f, SceneRole.Target, 3 to 0)
    mark(0.42f, 3, 0, SceneMark.Fill)
    ring(0.5f, 0.94f, SceneRole.Evidence, 1 to 1)
    ring(0.6f, 0.94f, SceneRole.Target, 2 to 1)
    mark(0.84f, 2, 1, SceneMark.Fill)
}

/** First step: the painted cells next to the line, one after the other. */
internal val MirrorStartScene = verticalBoard {
    ring(0.06f, 0.94f, SceneRole.Evidence, 1 to 1, 2 to 1)
    ring(0.3f, 0.94f, SceneRole.Target, 1 to 2, 2 to 2)
    mark(0.5f, 1, 2, SceneMark.Fill)
    mark(0.7f, 2, 2, SceneMark.Fill)
}

/**
 * The steps of the mirror card: the goal (the line and the ready half), how to paint, the cell across the line, the other axes, and the first
 * step. The pictures are scenes: the board of the game is a plain grid, a ready cell has the lighter tile, a painted cell a square of the
 * colour of the game, and the axis is a thick line between the cells, the same as on the page.
 */
internal val MirrorHowTo = HowToCardContent(
    listOf(
        sceneStep(Res.string.howto_card_mirror_1, MirrorGoalScene),
        sceneStep(Res.string.howto_card_mirror_2, MirrorPaintScene),
        sceneStep(Res.string.howto_mirror_tech, MirrorAcrossScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_mirror_axes, MirrorAxesScene, title = Res.string.howto_title_solve),
        sceneStep(Res.string.howto_mirror_start, MirrorStartScene, title = Res.string.howto_title_spoiler, spoiler = true),
    ),
)

/** Every picture of this file, with a name for the screenshots of the tests. */
internal val MirrorScenes: List<Pair<String, DemoScene>> by lazy {
    listOf(
        "mirror_goal" to MirrorGoalScene, "mirror_paint" to MirrorPaintScene, "mirror_across" to MirrorAcrossScene,
        "mirror_axes" to MirrorAxesScene, "mirror_start" to MirrorStartScene,
    )
}
