package com.mehmtcan.brainscroll.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mehmtcan.brainscroll.ui.home.BrainMascot
import com.mehmtcan.brainscroll.ui.home.MascotFace
import com.mehmtcan.brainscroll.ui.home.faceForDifficulty
import com.mehmtcan.brainscroll.ui.home.lerp
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.test.Test

/** Looks at the faces of the mascot: `shared/build/screenshots/920_mascot_faces.png` and `921_mascot_morph.png`. */
@OptIn(ExperimentalTestApi::class)
class MascotFacesUiTest {
    private data class Named(val name: String, val face: MascotFace, val tint: Float = 0f)

    @Test
    fun theFacesAndTheMorph() = runSkikoComposeUiTest(size = Size(1080f, 1500f), density = Density(2f)) {
        val faces = listOf(
            Named("Happy", MascotFace.Happy), Named("Joy", MascotFace.Joy), Named("Surprised", MascotFace.Surprised),
            Named("Sad", MascotFace.Sad), Named("Worried", MascotFace.Worried), Named("Sleepy", MascotFace.Sleepy),
            Named("Idea", MascotFace.Idea), Named("Medium (slider)", faceForDifficulty(1f), 1f), Named("Hard (slider)", faceForDifficulty(2f), 2f),
        )
        setContent {
            BrainScrollTheme {
                Column(Modifier.fillMaxSize().background(com.mehmtcan.brainscroll.ui.theme.DarkColors.bgPage).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    faces.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { f ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    BrainMascot(face = f.face, tint = f.tint, interactive = false, modifier = Modifier.size(160.dp))
                                    BasicText(f.name, style = BrainScrollTheme.typography.label.copy(color = BrainScrollTheme.colors.textPrimary))
                                }
                            }
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(300)
        snap("920_mascot_faces")
    }

    @Test
    fun theMorphFromSadToJoy() = runSkikoComposeUiTest(size = Size(1080f, 330f), density = Density(2f)) {
        val steps = (0..5).map { i ->
            val f = i / 5f
            if (f < 0.5f) lerp(MascotFace.Sad, MascotFace.Surprised, f * 2f) else lerp(MascotFace.Surprised, MascotFace.Joy, (f - 0.5f) * 2f)
        }
        setContent {
            BrainScrollTheme {
                Row(Modifier.fillMaxSize().background(com.mehmtcan.brainscroll.ui.theme.DarkColors.bgPage).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    steps.forEach { BrainMascot(face = it, interactive = false, modifier = Modifier.size(84.dp)) }
                }
            }
        }
        mainClock.advanceTimeBy(300)
        snap("921_mascot_morph")
    }
}
