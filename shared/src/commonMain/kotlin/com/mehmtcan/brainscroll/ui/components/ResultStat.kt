package com.mehmtcan.brainscroll.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mehmtcan.brainscroll.ui.theme.Spacing
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme

/** The data of one [ResultStat]. */
class ResultStatData(val value: String, val label: String, val color: Color)

/**
 * The numbers a result card is about, big and side by side. [summary] is the one sentence a screen reader says for the whole row
 * (the same words the card used to show as plain text), so the numbers are not read as loose digits.
 */
@Composable
fun ResultStats(summary: String, stats: List<ResultStatData>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = summary },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        // A lone number needs no word under it: "00:12" says it is a time, and the card is shorter for it.
        stats.forEach { ResultStat(it.value, if (stats.size == 1) null else it.label, it.color) }
    }
}

/** One number of a result card: big and colored, with its small label under it ("12" / "moves"). */
@Composable
fun ResultStat(value: String, label: String?, color: Color, modifier: Modifier = Modifier) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(value, style = type.display.copy(color = color, fontSize = 40.sp, fontWeight = FontWeight.Bold))
        if (label != null) BasicText(label, style = type.caption.copy(color = colors.textSecondary))
    }
}
