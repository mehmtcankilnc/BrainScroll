package com.mehmtcan.brainscroll.ui.i18n

import java.util.Locale

// The device language, taken before the app changes it for the first time.
private val deviceLocale: Locale = Locale.getDefault()

actual fun applyAppLocale(language: AppLanguage?) {
    Locale.setDefault(language?.let { Locale.forLanguageTag(it.tag) } ?: deviceLocale)
}
