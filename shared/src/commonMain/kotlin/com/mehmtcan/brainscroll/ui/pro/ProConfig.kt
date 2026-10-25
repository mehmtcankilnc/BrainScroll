package com.mehmtcan.brainscroll.ui.pro

/**
 * RevenueCat settings. THE KEYS GO HERE: the public SDK keys from the RevenueCat dashboard (Project settings, API keys),
 * one for Android (starts with `goog_`) and one for iOS (starts with `appl_`). They are public SDK keys, not secrets,
 * but they are left empty in the repository. While a key is empty the store is off: the app stays free and shows no
 * purchase screen.
 *
 * Never put the RevenueCat secret key (`sk_`) or webhook secret here.
 */
object ProConfig {
    const val ANDROID_API_KEY = ""
    const val IOS_API_KEY = "appl_UbNNlWBRAmyxVavKNPxhKkjWFnh"

    /** The right ("entitlement") and the product, as named in RevenueCat and in both stores (docs/pro-plan.md section 4). */
    const val ENTITLEMENT = "pro"
    const val PRODUCT_ID = "brainscroll_pro"

    /** The privacy pages of the site, in Turkish and in English. There is no separate terms page yet (the store's own terms apply). */
    const val PRIVACY_URL_TR = "https://playbrainscroll.com/privacy.html"
    const val PRIVACY_URL_EN = "https://playbrainscroll.com/en/privacy.html"
}
