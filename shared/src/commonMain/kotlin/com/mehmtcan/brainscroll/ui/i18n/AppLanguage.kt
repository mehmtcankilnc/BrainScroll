package com.mehmtcan.brainscroll.ui.i18n

import com.mehmtcan.brainscroll.game.wordle.Language

/**
 * The language of the app's texts. Not the same as the language of the word puzzles: a player can use the app in one
 * language and play word puzzles in another. A new language is one more entry here and a `values-xx` strings folder.
 * `null` in the settings means "follow the device".
 */
enum class AppLanguage(val tag: String, val nativeName: String) {
    TR("tr", "Türkçe"),
    EN("en", "English");

    /** The word puzzle language that matches this app language. */
    val wordLanguage: Language get() = when (this) {
        TR -> Language.TR
        EN -> Language.EN
    }

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }

        /** The app language for a device language tag; English for anything the app has no texts for. */
        fun forDevice(tag: String): AppLanguage = fromTag(tag) ?: EN
    }
}

/**
 * Makes the texts of the app follow [language] (or the device again for null). Called before the screens are
 * composed; the screens are composed again after a change.
 */
expect fun applyAppLocale(language: AppLanguage?)
