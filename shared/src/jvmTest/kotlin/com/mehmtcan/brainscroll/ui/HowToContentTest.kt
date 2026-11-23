package com.mehmtcan.brainscroll.ui

import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.ui.howto.cardOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The content of every how to card: how many steps, the last one gives the first move away, and every text exists in both languages. */
class HowToContentTest {

    @Test
    fun everyGameHasAtLeastFiveStepsAndTheLastOneIsTheOnlySpoiler() {
        for (kind in GameKind.entries) {
            val steps = cardOf(kind).steps
            assertTrue(steps.size in 5..7, "$kind has ${steps.size} steps")
            assertTrue(steps.last().spoiler, "$kind: the last step is the first move")
            assertEquals(1, steps.count { it.spoiler }, "$kind: only the last step is a spoiler")
            assertTrue(steps.last().title != null, "$kind: the last step has its label")
        }
    }

    private fun names(path: String): Map<String, String> {
        val text = File(path).readText()
        return Regex("""<string name="(howto_[a-z0-9_]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL).findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
    }

    @Test
    fun everyHowToTextIsThereInBothLanguagesAndNotEmpty() {
        val en = names("src/commonMain/composeResources/values/strings.xml")
        val tr = names("src/commonMain/composeResources/values-tr/strings.xml")
        assertEquals(en.keys, tr.keys, "the same texts in both languages")
        for ((name, text) in en + tr) {
            assertTrue(text.isNotBlank(), "$name is not empty")
            assertTrue('\'' !in text, "$name has no apostrophe")
        }
        for (kind in GameKind.entries) {
            val key = "howto_${kind.name.lowercase()}_start"
            assertTrue(key in en || kind == GameKind.Sudoku || kind == GameKind.Mines, "$key exists")
        }
    }
}
