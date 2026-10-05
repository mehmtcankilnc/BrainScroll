package com.mehmtcan.brainscroll

import androidx.compose.runtime.Composable
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
import com.mehmtcan.brainscroll.ui.feed.FeedViewModel
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
@Preview
fun App(cloudServices: CloudServicesFactory = ::supabaseServices) {
    BrainScrollTheme {
        // The puzzles start in the device language (Turkish or English) unless the player chose one before.
        val deviceLanguage = if (Locale.current.language == "tr") Language.TR else Language.EN
        val driverProvider = rememberDriverProvider()
        val viewModel = viewModel {
            FeedViewModel(
                repository = GameRepository(BrainScrollDatabase(driverProvider.create())),
                deviceLanguage = deviceLanguage,
                now = { Clock.System.now().toEpochMilliseconds() },
                cloudServices = cloudServices,
            )
        }
        AppShell(viewModel)
    }
}
