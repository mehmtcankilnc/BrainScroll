package com.mehmtcan.brainscroll

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.app_name
import brainscroll.shared.generated.resources.glyph_test_label
import brainscroll.shared.generated.resources.legend_absent
import brainscroll.shared.generated.resources.legend_correct
import brainscroll.shared.generated.resources.legend_pending
import brainscroll.shared.generated.resources.tagline
import com.mehmtcan.brainscroll.ui.components.LetterTile
import com.mehmtcan.brainscroll.ui.components.TileState
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Spacing
import org.jetbrains.compose.resources.stringResource

@Composable
@Preview
fun App() {
    BrainScrollTheme {
        val colors = BrainScrollTheme.colors
        val type = BrainScrollTheme.typography

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bgPage)
                .safeContentPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText(
                text = stringResource(Res.string.app_name),
                style = type.display.copy(color = colors.textPrimary),
            )
            Spacer(Modifier.height(Spacing.sm))
            BasicText(
                text = stringResource(Res.string.tagline),
                style = type.body.copy(color = colors.textSecondary),
            )

            Spacer(Modifier.height(Spacing.xxl))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                LabeledTile("B", TileState.Correct, stringResource(Res.string.legend_correct))
                LabeledTile("R", TileState.Pending, stringResource(Res.string.legend_pending))
                LabeledTile("A", TileState.Absent, stringResource(Res.string.legend_absent))
            }

            Spacer(Modifier.height(Spacing.xxl))

            // Font check: every Turkish letter, including the dotted/dotless I pair.
            BasicText(
                text = stringResource(Res.string.glyph_test_label),
                style = type.caption.copy(color = colors.textSecondary),
            )
            Spacer(Modifier.height(Spacing.xs))
            BasicText(
                text = "ĞÜŞİÖÇ ığüşıöç",
                style = type.heading.copy(color = colors.textPrimary),
            )

            Spacer(Modifier.height(Spacing.xl))

            val platformName = remember { getPlatform().name }
            BasicText(
                text = platformName,
                style = type.caption.copy(color = colors.textDisabled),
            )
        }
    }
}

@Composable
private fun LabeledTile(letter: String, state: TileState, label: String) {
    val colors = BrainScrollTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LetterTile(letter, state)
        Spacer(Modifier.height(Spacing.xs))
        BasicText(
            text = label,
            style = BrainScrollTheme.typography.caption.copy(color = colors.textSecondary),
        )
    }
}
