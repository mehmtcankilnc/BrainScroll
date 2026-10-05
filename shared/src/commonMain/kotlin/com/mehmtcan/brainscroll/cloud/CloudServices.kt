package com.mehmtcan.brainscroll.cloud

import com.mehmtcan.brainscroll.account.AccountService
import com.mehmtcan.brainscroll.account.SupabaseAccountService
import com.mehmtcan.brainscroll.account.platformAppleSignIn
import com.mehmtcan.brainscroll.sync.CloudApi
import kotlinx.coroutines.CoroutineScope

/** The two things that talk to the network, bundled so tests (and previews) can swap both for fakes. */
class CloudServices(val account: AccountService, val cloud: CloudApi)

/** Builds the services. The scope is the one the account service uses for its background work. */
typealias CloudServicesFactory = (CoroutineScope) -> CloudServices

/** The real thing: Supabase for the account and for the data. */
fun supabaseServices(scope: CoroutineScope): CloudServices {
    val client = createBrainScrollSupabase()
    return CloudServices(
        account = SupabaseAccountService(client, platformAppleSignIn(), scope),
        cloud = SupabaseCloudApi(client),
    )
}
