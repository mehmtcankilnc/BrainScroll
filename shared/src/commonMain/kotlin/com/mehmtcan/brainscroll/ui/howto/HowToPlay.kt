package com.mehmtcan.brainscroll.ui.howto

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.howto_button
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import org.jetbrains.compose.resources.stringResource

/** The round "?" next to the name of the game. It opens [HowToPlaySheet]; the touch area is 40 dp wide, so the bar has room. */
@Composable
fun HowToIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val description = stringResource(Res.string.howto_button)
    Box(
        modifier = modifier
            .testTag("howToButton")
            .size(40.dp)
            .semantics { contentDescription = description }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { QuestionMark(colors.textSecondary) }
}

/** A small circle with a question mark, drawn with text so it follows the text color. */
@Composable
private fun QuestionMark(color: Color) {
    Box(
        modifier = Modifier.size(18.dp).border(1.5.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText("?", style = BrainScrollTheme.typography.caption.copy(color = color))
    }
}

/** The three steps of every game. A new game adds its card here. */
private fun cardOf(kind: GameKind): HowToCardContent = when (kind) {
    GameKind.Word -> WordHowTo
    GameKind.Memory -> MemoryHowTo
    GameKind.Tubes -> TubesHowTo
    GameKind.Mines -> MinesHowTo
    GameKind.Sudoku -> SudokuHowTo
    GameKind.Lights -> LightsHowTo
    GameKind.Slide -> SlideHowTo
    GameKind.Target -> TargetHowTo
    GameKind.Pipes -> PipesHowTo
    GameKind.Hanoi -> HanoiHowTo
    GameKind.Queens -> QueensHowTo
    GameKind.Tents -> TentsHowTo
    GameKind.Jigsaw -> JigsawHowTo
    GameKind.BlockOut -> BlockOutHowTo
    GameKind.Arrows -> ArrowsHowTo
    GameKind.Nonogram -> NonogramHowTo
    GameKind.AxisSum -> AxisSumHowTo
    GameKind.BallSlide -> BallSlideHowTo
    GameKind.ColorFlow -> ColorFlowHowTo
    GameKind.Line -> LineHowTo
}

/** How to play one game: the card of three steps over the page. OK, the dark area or the system back button closes it. */
@Composable
fun HowToPlaySheet(kind: GameKind, onClose: () -> Unit, modifier: Modifier = Modifier, wordLanguage: Language? = null) {
    CompositionLocalProvider(LocalHowToWordLanguage provides (wordLanguage ?: LocalHowToWordLanguage.current)) {
        HowToPlayCard(cardOf(kind), onClose, modifier)
    }
}
