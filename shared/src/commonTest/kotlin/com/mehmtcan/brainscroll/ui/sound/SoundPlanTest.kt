package com.mehmtcan.brainscroll.ui.sound

import kotlin.test.Test
import kotlin.test.assertEquals

class SoundPlanTest {
    @Test
    fun ballSlidePlaysOneNoteForEveryCell() {
        assertEquals(Sfx("ballslide_cell"), BALLSLIDE_CELL_SOUND)
    }

    @Test
    fun nonogramFillMovesUpEveryThirdStroke() {
        assertEquals(Sfx("nonogram_fill_00"), nonogramFillSound(2, 0))
        assertEquals(Sfx("nonogram_fill_01"), nonogramFillSound(3, 0))
        assertEquals(Sfx("nonogram_fill_09"), nonogramFillSound(30, 4))
    }

    @Test
    fun nonogramRattlesGoRoundEightFiles() {
        assertEquals(Sfx("nonogram_line_07"), nonogramLineSound(7))
        assertEquals(Sfx("nonogram_line_00"), nonogramLineSound(8))
    }

    @Test
    fun everyFileOfThePackIsListed() {
        assertEquals(10 + 19, Sfx.All.size)
        assertEquals(10 + 19, Sfx.All.toSet().size)
    }
}
