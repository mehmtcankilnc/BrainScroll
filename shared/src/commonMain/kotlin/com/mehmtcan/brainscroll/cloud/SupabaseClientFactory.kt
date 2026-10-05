package com.mehmtcan.brainscroll.cloud

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Where the app finds its Supabase project. The publishable key is meant to be public: it is shipped inside every
 * app and website that uses the project, and what it may do is decided by the Row Level Security policies in
 * `supabase/migrations`, not by keeping the key secret. NEVER put the service-role key or the database password
 * anywhere in this repository.
 */
object SupabaseConfig {
    const val URL = "https://lxrfnaxecvofmgkdtvrn.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_K6wdedPxnM_zY9NIuV0SJQ__Cwf2eIP"

    /**
     * Where Google sends the player back to the app after signing in: `com.mehmtcan.brainscroll://login-callback`.
     * This exact address must be in Supabase (Authentication, URL Configuration, Redirect URLs), in the Android
     * manifest and in the iOS Info.plist.
     */
    const val REDIRECT_SCHEME = "com.mehmtcan.brainscroll"
    const val REDIRECT_HOST = "login-callback"
}

fun createBrainScrollSupabase(): SupabaseClient = createSupabaseClient(
    supabaseUrl = SupabaseConfig.URL,
    supabaseKey = SupabaseConfig.PUBLISHABLE_KEY,
) {
    install(Auth) {
        scheme = SupabaseConfig.REDIRECT_SCHEME
        host = SupabaseConfig.REDIRECT_HOST
        // PKCE: the browser returns a one-time code and the app exchanges it, instead of receiving tokens in the URL.
        flowType = FlowType.PKCE
    }
    install(Postgrest)
}
