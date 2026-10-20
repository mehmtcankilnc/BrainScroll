package com.mehmtcan.brainscroll.ui.sound

import kotlin.math.floor

/** What the games play and when: the choices of docs/ses-efektleri-plan.md as plain functions, so tests can check them. */

const val NOTES = 10
const val RATTLES = 8

/** The note of every cell the ball passes: always the same one, never higher or lower. */
val BALLSLIDE_CELL_SOUND = Sfx.BallSlideCell
const val BALLSLIDE_CELL_VOLUME = 0.40f

/** Cell [cellNo] of stroke [strokeNo]: a new note every third stroke. */
fun nonogramFillSound(strokeNo: Int, cellNo: Int): Sfx = Sfx.nonogramFill((strokeNo / 3 + cellNo).coerceIn(0, NOTES - 1))

/** The [count]-th rattle of the game (from 0). */
fun nonogramLineSound(count: Int): Sfx = Sfx.nonogramLine(count % RATTLES)

const val BALLSLIDE_WHOOSH_VOLUME = 0.40f
const val BALLSLIDE_CELL_THRESHOLD = 0.35f
const val BALLSLIDE_CHORD_VOLUME = 0.55f
const val NONOGRAM_FILL_VOLUME = 0.30f
const val NONOGRAM_FILL_GAP_MILLIS = 80L
const val NONOGRAM_LINE_VOLUME = 0.30f
const val NONOGRAM_LINE_MIN_GAP_MILLIS = 120L
const val NONOGRAM_CHORD_VOLUME = 0.55f

const val TUBES_POUR_VOLUME = 0.35f
const val TUBES_POUR_DELAY_MILLIS = 300L
const val MINES_BOOM_VOLUME = 0.6f
const val HANOI_THUNK_VOLUME = 0.5f
const val UI_TAP_VOLUME = 0.30f
const val UI_GOOD_VOLUME = 0.38f
const val UI_WARNING_VOLUME = 0.70f
const val UI_WIN_CHORD_VOLUME = 0.55f
