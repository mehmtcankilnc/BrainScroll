package com.mehmtcan.brainscroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mehmtcan.brainscroll.cloud.CloudServicesFactory
import com.mehmtcan.brainscroll.cloud.supabaseServices
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.data.rememberDriverProvider
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.AppShell
import com.mehmtcan.brainscroll.ui.daily.DailyViewModel
import com.mehmtcan.brainscroll.ui.haptics.Haptics
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.rememberPlatformHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.rememberReduceMotion
import com.mehmtcan.brainscroll.ui.feed.FeedViewModel
import com.mehmtcan.brainscroll.ui.social.SocialViewModel
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.rememberHighContrast
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
@Preview
fun App(cloudServices: CloudServicesFactory = ::supabaseServices, haptics: Haptics? = null, reduceMotion: Boolean? = null, highContrast: Boolean? = null) {
    // Tests pass their own haptics to see when the phone would buzz.
    CompositionLocalProvider(
        LocalHaptics provides (haptics ?: rememberPlatformHaptics()),
        LocalReduceMotion provides (reduceMotion ?: rememberReduceMotion()),
    ) {
    BrainScrollTheme(highContrast = highContrast ?: rememberHighContrast()) {
        // The puzzles start in the device language (Turkish or English) unless the player chose one before.
        val deviceLanguage = if (Locale.current.language == "tr") Language.TR else Language.EN
        val driverProvider = rememberDriverProvider()
        val feedViewModel = viewModel {
            FeedViewModel(
                repository = GameRepository(BrainScrollDatabase(driverProvider.create())),
                deviceLanguage = deviceLanguage,
                now = { Clock.System.now().toEpochMilliseconds() },
                cloudServices = cloudServices,
            )
        }
        // The daily puzzle shares the database and the account with the feed, so it is built from what the feed owns.
        val dailyViewModel = viewModel {
            DailyViewModel(
                api = feedViewModel.dailyApi,
                repository = feedViewModel.repository,
                signedIn = feedViewModel.signedIn,
                initialLanguage = feedViewModel.repository.savedLanguage() ?: deviceLanguage,
                now = { Clock.System.now().toEpochMilliseconds() },
                onLocalChange = feedViewModel::requestSync,
            )
        }
        // The ranks tab uses the same account and database, and gets the invite codes the feed receives through links.
        val socialViewModel = viewModel {
            SocialViewModel(
                api = feedViewModel.socialApi,
                repository = feedViewModel.repository,
                accountState = feedViewModel.accountState,
                initialLanguage = feedViewModel.repository.savedLanguage() ?: deviceLanguage,
                invites = feedViewModel.inviteCodes,
            )
        }
        AppShell(feedViewModel, dailyViewModel, socialViewModel)
    }
    }
}
