package com.mehmtcan.brainscroll.cloud

import com.mehmtcan.brainscroll.account.AccountMergeApi
import com.mehmtcan.brainscroll.account.AccountService
import com.mehmtcan.brainscroll.account.SupabaseAccountMergeApi
import com.mehmtcan.brainscroll.account.SupabaseAccountService
import com.mehmtcan.brainscroll.account.platformAppleSignIn
import com.mehmtcan.brainscroll.daily.DailyApi
import com.mehmtcan.brainscroll.daily.SupabaseDailyApi
import com.mehmtcan.brainscroll.social.SocialApi
import com.mehmtcan.brainscroll.social.SupabaseSocialApi
import com.mehmtcan.brainscroll.sync.CloudApi
import com.mehmtcan.brainscroll.telemetry.SupabaseTelemetryApi
import com.mehmtcan.brainscroll.telemetry.TelemetryApi
import kotlinx.coroutines.CoroutineScope

/** The things that talk to the network, bundled so tests (and previews) can swap them for fakes. */
class CloudServices(
    val account: AccountService,
    val cloud: CloudApi,
    val daily: DailyApi,
    val social: SocialApi,
    val telemetry: TelemetryApi,
    val merge: AccountMergeApi,
)

/** Builds the services. The scope is the one the account service uses for its background work. */
typealias CloudServicesFactory = (CoroutineScope) -> CloudServices

/** The real thing: Supabase for the account and for the data. */
fun supabaseServices(scope: CoroutineScope): CloudServices {
    val client = createBrainScrollSupabase()
    return CloudServices(
        account = SupabaseAccountService(client, platformAppleSignIn(), scope),
        cloud = SupabaseCloudApi(client),
        daily = SupabaseDailyApi(client),
        social = SupabaseSocialApi(client),
        telemetry = SupabaseTelemetryApi(client),
        merge = SupabaseAccountMergeApi(client),
    )
}
