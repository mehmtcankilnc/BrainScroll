package com.mehmtcan.brainscroll.ui.i18n

import platform.Foundation.NSUserDefaults

actual fun applyAppLocale(language: AppLanguage?) {
    val defaults = NSUserDefaults.standardUserDefaults
    if (language == null) defaults.removeObjectForKey("AppleLanguages") else defaults.setObject(listOf(language.tag), forKey = "AppleLanguages")
}
