package com.mehmtcan.brainscroll

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mehmtcan.brainscroll.ui.i18n.AppLanguage
import com.mehmtcan.brainscroll.ui.i18n.applyAppLocale
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mehmtcan.brainscroll.cloud.CloudServicesFactory
import com.mehmtcan.brainscroll.cloud.supabaseServices
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.data.rememberDriverProvider
import com.mehmtcan.brainscroll.db.BrainScrollDatabase
import com.mehmtcan.brainscroll.game.GameKind
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.ui.AppShell
import com.mehmtcan.brainscroll.time.IstanbulDay
import com.mehmtcan.brainscroll.ui.daily.DailyGameViewModel
import com.mehmtcan.brainscroll.ui.daily.DailyViewModel
import com.mehmtcan.brainscroll.ui.haptics.Haptics
import com.mehmtcan.brainscroll.ui.haptics.LocalHaptics
import com.mehmtcan.brainscroll.ui.haptics.rememberPlatformHaptics
import com.mehmtcan.brainscroll.ui.motion.LocalReduceMotion
import com.mehmtcan.brainscroll.ui.motion.rememberReduceMotion
import com.mehmtcan.brainscroll.telemetry.rememberPlatformInfo
import com.mehmtcan.brainscroll.ui.feed.FeedViewModel
import com.mehmtcan.brainscroll.ui.social.SocialViewModel
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.rememberHighContrast
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
@Preview
fun App(cloudServices: CloudServicesFactory = ::supabaseServices, haptics: Haptics? = null,
    reduceMotion: Boolean? = null,
    highContrast: Boolean? = null,
    /** The games in the feed, in order. Tests can ask for just one. */
    /** Which game is the daily puzzle on a day (by day number). The app always plays the crossword; tests fix another game. */
    dailyGame: (Long) -> GameKind = { GameKind.Crossword },
    games: List<GameKind> = listOf(GameKind.Word, GameKind.Memory, GameKind.Tubes, GameKind.Mines, GameKind.Sudoku, GameKind.Lights, GameKind.Slide, GameKind.Target, GameKind.Pipes, GameKind.Hanoi, GameKind.Queens, GameKind.Tents, GameKind.Jigsaw, GameKind.BlockOut, GameKind.Line),
) {
    // Tests pass their own haptics to see when the phone would buzz.
    CompositionLocalProvider(
        LocalHaptics provides (haptics ?: rememberPlatformHaptics()),
        LocalReduceMotion provides (reduceMotion ?: rememberReduceMotion()),
    ) {
    BrainScrollTheme(highContrast = highContrast ?: rememberHighContrast()) {
        // The puzzles start in the device language (Turkish or English) unless the player chose one before.
        val deviceLanguage = AppLanguage.forDevice(Locale.current.language).wordLanguage
        val driverProvider = rememberDriverProvider()
        val platformInfo = rememberPlatformInfo()
        val feedViewModel = viewModel {
            FeedViewModel(
                repository = GameRepository(BrainScrollDatabase(driverProvider.create())),
                deviceLanguage = deviceLanguage,
                now = { Clock.System.now().toEpochMilliseconds() },
                cloudServices = cloudServices,
                platformInfo = platformInfo,
                games = games,
            )
        }
        // The daily puzzle shares the database and the account with the feed, so it is built from what the feed owns.
        val dailyViewModel = viewModel {
            DailyViewModel(
                api = feedViewModel.dailyApi,
                repository = feedViewModel.repository,
                signedIn = feedViewModel.signedIn,
                initialLanguage = feedViewModel.resolvedAppLanguage,
                now = { Clock.System.now().toEpochMilliseconds() },
                onLocalChange = feedViewModel::requestSync,
                telemetry = feedViewModel.telemetry,
            )
        }
        // The daily puzzle of the days when it is not the word puzzle: played on the device, sent to the server after.
        val dailyGameViewModel = viewModel {
            DailyGameViewModel(
                repository = feedViewModel.repository,
                api = feedViewModel.dailyApi,
                signedIn = feedViewModel.signedIn,
                now = { Clock.System.now().toEpochMilliseconds() },
                gameOf = dailyGame,
                language = { feedViewModel.resolvedAppLanguage },
                onLocalChange = feedViewModel::requestSync,
            )
        }
        // The ranks tab uses the same account and database, and gets the invite codes the feed receives through links.
        val socialViewModel = viewModel {
            SocialViewModel(
                api = feedViewModel.socialApi,
                repository = feedViewModel.repository,
                accountState = feedViewModel.accountState,
                initialLanguage = feedViewModel.resolvedAppLanguage,
                invites = feedViewModel.inviteCodes,
                telemetry = feedViewModel.telemetry,
                todayGame = { dailyGame(IstanbulDay.dayIndex(Clock.System.now().toEpochMilliseconds())) },
            )
        }
        // The texts follow the app language: set before anything is composed, and the screens are built again on a change.
        val appLanguage by feedViewModel.appLanguage.collectAsStateWithLifecycle()
        remember(appLanguage) { applyAppLocale(appLanguage) }
        LaunchedEffect(appLanguage) {
            // The daily puzzle is in the app language; the speed tables start on it too.
            dailyViewModel.selectLanguage(feedViewModel.resolvedAppLanguage)
        }
        AppShell(feedViewModel, dailyViewModel, dailyGameViewModel, socialViewModel, appLanguage)
    }
    }
}
