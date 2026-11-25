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

    /**
     * The hint packs (consumables), smallest first: store product id and how many hints it gives. The ids and amounts are also in
     * `supabase/functions/revenuecat-webhook/handler.ts`, which is what really adds the hints: keep the two in step.
     */
    val HINT_PACKS: List<Pair<String, Int>> = listOf("brainscroll_hints_20" to 20, "brainscroll_hints_75" to 75)

    /** The privacy pages of the site, in Turkish and in English. */
    const val PRIVACY_URL_TR = "https://playbrainscroll.com/privacy.html"
    const val PRIVACY_URL_EN = "https://playbrainscroll.com/en/privacy.html"

    /** The terms of use pages of the site (live since 25 November 2026), in Turkish and in English. */
    const val TERMS_URL_TR = "https://playbrainscroll.com/terms.html"
    const val TERMS_URL_EN = "https://playbrainscroll.com/en/terms.html"
}
