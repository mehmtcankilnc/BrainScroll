package com.mehmtcan.brainscroll.ui.daily

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.daily_intro
import brainscroll.shared.generated.resources.daily_loading
import brainscroll.shared.generated.resources.daily_next
import brainscroll.shared.generated.resources.daily_offline
import brainscroll.shared.generated.resources.daily_retry
import brainscroll.shared.generated.resources.daily_start
import brainscroll.shared.generated.resources.daily_time
import brainscroll.shared.generated.resources.daily_title
import brainscroll.shared.generated.resources.daily_unavailable
import brainscroll.shared.generated.resources.key_enter
import brainscroll.shared.generated.resources.notice_daily_offline
import brainscroll.shared.generated.resources.notice_daily_unavailable
import brainscroll.shared.generated.resources.notice_not_in_list
import brainscroll.shared.generated.resources.notice_too_short
import brainscroll.shared.generated.resources.result_lost
import brainscroll.shared.generated.resources.result_won
import com.mehmtcan.brainscroll.daily.DailyState
import com.mehmtcan.brainscroll.daily.DailyStatus
import com.mehmtcan.brainscroll.daily.formatCountdown
import com.mehmtcan.brainscroll.daily.formatDuration
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.time.IstanbulDay
import com.mehmtcan.brainscroll.ui.components.AppButton
import com.mehmtcan.brainscroll.ui.components.Chip
import com.mehmtcan.brainscroll.ui.components.HeartButton
import com.mehmtcan.brainscroll.ui.components.NoticePill
import com.mehmtcan.brainscroll.ui.theme.BrainScrollTheme
import com.mehmtcan.brainscroll.ui.theme.Radius
import com.mehmtcan.brainscroll.ui.theme.Spacing
import com.mehmtcan.brainscroll.ui.wordle.WordleGrid
import com.mehmtcan.brainscroll.ui.wordle.WordleKeyboard
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val NOTICE_MILLIS = 1500L
private val BottomAreaHeight = 152.dp // same as the endless feed: the grid does not jump between the two

@OptIn(ExperimentalTime::class)
private fun deviceNow(): Long = Clock.System.now().toEpochMilliseconds()

/** The daily tab: one puzzle a day, the same for everyone. The server holds the answer and the clock. */
@Composable
fun DailyScreen(viewModel: DailyViewModel, modifier: Modifier = Modifier) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    // Opening the tab checks for a new day or an attempt finished on another phone.
    LaunchedEffect(viewModel) { viewModel.onShown() }

    var notice by remember { mutableStateOf<Pair<StringResource, Int>?>(null) }
    var noticeId by remember { mutableLongStateOf(0) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            noticeId++
            notice = (when (event) {
                DailyEvent.TooShort -> Res.string.notice_too_short
                DailyEvent.NotInList -> Res.string.notice_not_in_list
                DailyEvent.Offline -> Res.string.notice_daily_offline
                DailyEvent.Unavailable -> Res.string.notice_daily_unavailable
            }) to noticeId.toInt()
        }
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(NOTICE_MILLIS)
            notice = null
        }
    }

    // A physical keyboard (desktop) works too.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) {
                    return@onPreviewKeyEvent false
                }
                when (event.key) {
                    Key.Enter, Key.NumPadEnter -> { viewModel.submit(); true }
                    Key.Backspace -> { viewModel.backspace(); true }
                    else -> {
                        val ch = event.utf16CodePoint.toChar()
                        if (ch.isLetter()) { viewModel.type(ch); true } else false
                    }
                }
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DailyHeader(ui, onLanguage = viewModel::selectLanguage)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (ui.phase) {
                    DailyPhase.Loading -> Message(stringResource(Res.string.daily_loading))
                    DailyPhase.Offline -> MessageWithRetry(stringResource(Res.string.daily_offline), viewModel::refresh)
                    DailyPhase.Unavailable -> MessageWithRetry(stringResource(Res.string.daily_unavailable), viewModel::refresh)
                    DailyPhase.Intro -> Intro(ui, onStart = viewModel::start)
                    DailyPhase.Playing, DailyPhase.Finished -> Puzzle(ui, viewModel)
                }
            }
        }
        notice?.let { (text, _) ->
            NoticePill(stringResource(text), Modifier.align(Alignment.TopCenter).padding(top = 48.dp))
        }
    }
}

@Composable
private fun DailyHeader(ui: DailyUiState, onLanguage: (Language) -> Unit) {
    val colors = BrainScrollTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Chip(text = ui.dayStreak.current.toString(), textColor = colors.streakDay) { FlameIcon(colors.streakDay) }
        Spacer(Modifier.weight(1f))
        // Two puzzles a day, one per language. Both can be played.
        Language.entries.forEach { language ->
            val selected = language == ui.language
            Chip(
                text = language.name,
                textColor = if (selected) colors.textPrimary else colors.textSecondary,
                raised = selected,
                onClick = { onLanguage(language) },
            )
        }
    }
}

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.Center) {
        BasicText(text, style = BrainScrollTheme.typography.body.copy(color = BrainScrollTheme.colors.textSecondary, textAlign = TextAlign.Center))
    }
}

@Composable
private fun MessageWithRetry(text: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(text, style = BrainScrollTheme.typography.body.copy(color = BrainScrollTheme.colors.textSecondary, textAlign = TextAlign.Center))
        AppButton(stringResource(Res.string.daily_retry), onRetry)
    }
}

/** Before starting: the clock is NOT running yet. It starts on the server when Start is pressed. */
@Composable
private fun Intro(ui: DailyUiState, onStart: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            BasicText(stringResource(Res.string.daily_title), style = type.title.copy(color = colors.textPrimary))
            ui.state?.let { BasicText(IstanbulDay.format(it.dayIndex), style = type.caption.copy(color = colors.textSecondary)) }
            BasicText(stringResource(Res.string.daily_intro), style = type.body.copy(color = colors.textSecondary))
            Spacer(Modifier.height(Spacing.sm))
            AppButton(stringResource(Res.string.daily_start), onStart, primary = true)
        }
    }
}

@Composable
private fun Puzzle(ui: DailyUiState, viewModel: DailyViewModel) {
    val round = ui.round ?: return
    val state = ui.state ?: return
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.sm))
        BasicText(stringResource(Res.string.daily_title), style = type.heading.copy(color = colors.textPrimary))
        if (state.status == DailyStatus.Playing) {
            ElapsedTime(state, ui.receivedAtMs)
        } else {
            Spacer(Modifier.height(Spacing.lg))
        }
        Spacer(Modifier.height(Spacing.sm))

        WordleGrid(round, modifier = Modifier.weight(1f).fillMaxWidth())

        Spacer(Modifier.height(Spacing.md))
        Box(modifier = Modifier.fillMaxWidth().height(BottomAreaHeight), contentAlignment = Alignment.Center) {
            if (state.isFinished) {
                DailyResult(state, ui.isFavorite, viewModel::toggleFavorite)
            } else {
                WordleKeyboard(
                    language = round.language,
                    letterStates = round.letterStates,
                    enterLabel = stringResource(Res.string.key_enter),
                    onLetter = viewModel::type,
                    onBackspace = viewModel::backspace,
                    onEnter = viewModel::submit,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** The running time. The server says when it started and what its clock reads now; the phone only counts the rest. */
@Composable
private fun ElapsedTime(state: DailyState, receivedAtMs: Long) {
    var phoneNow by remember { mutableLongStateOf(receivedAtMs) }
    LaunchedEffect(state, receivedAtMs) {
        while (true) {
            phoneNow = deviceNow()
            delay(250)
        }
    }
    val started = state.startedAtMs ?: return
    val elapsed = (state.serverNowMs - started) + (phoneNow - receivedAtMs)
    BasicText(
        text = stringResource(Res.string.daily_time, formatDuration(elapsed)),
        style = BrainScrollTheme.typography.label.copy(color = BrainScrollTheme.colors.textSecondary),
    )
}

@Composable
private fun DailyResult(state: DailyState, isFavorite: Boolean, onToggleFavorite: () -> Unit) {
    val colors = BrainScrollTheme.colors
    val type = BrainScrollTheme.typography
    val won = state.status == DailyStatus.Won

    Box(modifier = Modifier.fillMaxWidth().background(colors.bgSurface, Radius.card).padding(Spacing.lg)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            BasicText(
                text = if (won) stringResource(Res.string.result_won, state.guesses.size)
                else stringResource(Res.string.result_lost, state.answer.orEmpty()),
                style = type.title.copy(color = if (won) colors.correctFill else colors.textPrimary, textAlign = TextAlign.Center),
            )
            state.durationMs?.let {
                BasicText(stringResource(Res.string.daily_time, formatDuration(it)), style = type.body.copy(color = colors.textPrimary))
            }
            NextPuzzleCountdown(state)
        }
        HeartButton(filled = isFavorite, onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd))
    }
}

/** Counts down to midnight in Istanbul, from the server's clock at the time it answered. */
@Composable
private fun NextPuzzleCountdown(state: DailyState) {
    val receivedAt = remember(state) { deviceNow() }
    var phoneNow by remember(state) { mutableLongStateOf(receivedAt) }
    LaunchedEffect(state) {
        while (true) {
            phoneNow = deviceNow()
            delay(1000)
        }
    }
    val serverNow = state.serverNowMs + (phoneNow - receivedAt)
    BasicText(
        text = stringResource(Res.string.daily_next, formatCountdown(IstanbulDay.millisUntilNextDay(serverNow))),
        style = BrainScrollTheme.typography.caption.copy(color = BrainScrollTheme.colors.textSecondary),
    )
}

/** Small flame for the day streak, drawn as a path so it follows the token color. */
@Composable
private fun FlameIcon(color: Color) {
    Canvas(Modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val flame = Path().apply {
            moveTo(0.50f * w, 0f)
            cubicTo(0.90f * w, 0.30f * h, 0.95f * w, 0.55f * h, 0.85f * w, 0.75f * h)
            cubicTo(0.78f * w, 0.92f * h, 0.62f * w, h, 0.50f * w, h)
            cubicTo(0.30f * w, h, 0.12f * w, 0.88f * h, 0.15f * w, 0.62f * h)
            cubicTo(0.17f * w, 0.45f * h, 0.30f * w, 0.40f * h, 0.38f * w, 0.25f * h)
            cubicTo(0.42f * w, 0.15f * h, 0.45f * w, 0.08f * h, 0.50f * w, 0f)
            close()
        }
        drawPath(flame, color)
    }
}
