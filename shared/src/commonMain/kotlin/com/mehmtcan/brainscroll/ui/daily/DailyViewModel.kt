package com.mehmtcan.brainscroll.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmtcan.brainscroll.daily.DailyApi
import com.mehmtcan.brainscroll.daily.DailyException
import com.mehmtcan.brainscroll.daily.DailyState
import com.mehmtcan.brainscroll.daily.DailyStatus
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.GuessRow
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.Mode
import com.mehmtcan.brainscroll.game.wordle.Outcome
import com.mehmtcan.brainscroll.game.wordle.RoundSnapshot
import com.mehmtcan.brainscroll.game.wordle.WordList
import com.mehmtcan.brainscroll.game.wordle.WordleStatus
import com.mehmtcan.brainscroll.game.wordle.keyStatesOf
import com.mehmtcan.brainscroll.game.wordle.loadWordList
import com.mehmtcan.brainscroll.stats.DayStreak
import com.mehmtcan.brainscroll.telemetry.NoTelemetry
import com.mehmtcan.brainscroll.telemetry.Telemetry
import com.mehmtcan.brainscroll.telemetry.TelemetryEvent
import com.mehmtcan.brainscroll.stats.DayStreaks
import com.mehmtcan.brainscroll.time.IstanbulDay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the daily tab shows as a whole. */
enum class DailyPhase {
    Loading,

    /** No connection (or no account yet): the daily puzzle needs one. Offers a retry. */
    Offline,

    /** The server refused or failed. Offers a retry. */
    Unavailable,

    /** Today's puzzle for this language was not started. Shows the Start button. The clock is not running. */
    Intro,
    Playing,
    Finished,
}

data class DailyUiState(
    val language: Language,
    val phase: DailyPhase,
    /** What the grid and keyboard draw. Only set while [phase] is Playing or Finished. */
    val round: RoundSnapshot?,
    val state: DailyState?,
    val dayStreak: DayStreak,
    /** Whether the finished attempt is a favorite (the heart on the result card). */
    val isFavorite: Boolean,
    /** The phone's clock when [state] arrived. With the server clock in [state] it gives a ticking timer. */
    val receivedAtMs: Long,
)

/** One-off messages. */
enum class DailyEvent { TooShort, NotInList, Offline, Unavailable }

/**
 * The daily puzzle screen's logic. The answer, the coloring and the clock are on the server ([DailyApi]); this
 * only keeps what is being typed, sends guesses, and records the result on the device when the server says the
 * attempt is over.
 *
 * Guesses are checked against the word list on the phone first, so a typo costs nothing and no request is made.
 */
class DailyViewModel(
    private val api: DailyApi,
    private val repository: GameRepository,
    /** True while there is an account session. Without one the server cannot know who is playing. */
    private val signedIn: StateFlow<Boolean>,
    initialLanguage: Language,
    private val now: () -> Long,
    private val loadWords: suspend (Language) -> WordList = ::loadWordList,
    /** Called after something changed that should be backed up (a favorite), so a sync can be scheduled. */
    private val onLocalChange: () -> Unit = {},
    private val telemetry: Telemetry = NoTelemetry,
) : ViewModel() {

    private var language = initialLanguage
    private var phase = DailyPhase.Loading
    private val states = mutableMapOf<Language, DailyState>()
    private var receivedAtMs = now()
    private var input = ""
    private var pending: String? = null
    private var errorTick = 0
    private var words: Map<Language, WordList> = emptyMap()
    private val recorded = mutableSetOf<String>()
    private var job: Job? = null

    private val _ui = MutableStateFlow(snapshot())
    val ui: StateFlow<DailyUiState> = _ui

    private val _events = MutableSharedFlow<DailyEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<DailyEvent> = _events

    init {
        viewModelScope.launch {
            words = Language.entries.associateWith { loadWords(it) }
        }
        viewModelScope.launch {
            // Load as soon as there is an account, and again if the account changes (another account, other results).
            signedIn.collect { ready -> if (ready) refresh() else offline() }
        }
    }

    // --- Screen actions ---

    /** Asks the server for today's state without starting anything. Also the "try again" button. */
    fun refresh() {
        launchExclusive {
            phase = DailyPhase.Loading
            publish()
            load { api.state(language) }
        }
    }

    /**
     * The tab was opened. Checks quietly for a new day, or an attempt finished on another phone, without a loading
     * flicker. If the screen was showing an error it tries again properly.
     */
    fun onShown() {
        when (phase) {
            DailyPhase.Offline, DailyPhase.Unavailable -> refresh()
            DailyPhase.Loading -> Unit
            else -> if (pending == null) launchExclusive { load(silent = true) { api.state(language) } }
        }
    }

    fun selectLanguage(newLanguage: Language) {
        if (newLanguage == language) return
        language = newLanguage
        input = ""
        pending = null
        val known = states[newLanguage]
        if (known != null) {
            // Show what we already know at once, then check with the server in the background.
            phase = phaseOf(known)
            publish()
        }
        refresh()
    }

    /** The Start button: starts the clock on the server. */
    fun start() {
        launchExclusive {
            phase = DailyPhase.Loading
            publish()
            load { api.start(language) }
            // Counted once per press of Start that worked; asking again for a started puzzle is not a new start.
            if (phase == DailyPhase.Playing) telemetry.event(TelemetryEvent.DailyStarted, mapOf("language" to language.name))
        }
    }

    fun type(letter: Char) {
        if (phase != DailyPhase.Playing || pending != null || input.length >= WORD_LENGTH) return
        val upper = language.uppercase(letter.toString())
        if (upper.length == 1 && upper[0] in language.alphabet) {
            input += upper
            publish()
        }
    }

    fun backspace() {
        if (phase != DailyPhase.Playing || pending != null || input.isEmpty()) return
        input = input.dropLast(1)
        publish()
    }

    fun submit() {
        val state = states[language] ?: return
        if (phase != DailyPhase.Playing || pending != null) return

        val reject = when {
            input.length != WORD_LENGTH -> DailyEvent.TooShort
            // Checked here so that a typo never reaches the server and never costs a guess.
            words[language]?.let { input !in it } == true -> DailyEvent.NotInList
            else -> null
        }
        if (reject != null) {
            errorTick++
            publish()
            _events.tryEmit(reject)
            return
        }

        val word = input
        pending = word // the row pulses while the server answers
        publish()
        launchExclusive {
            try {
                val next = api.guess(language, word)
                input = ""
                pending = null
                apply(next)
            } catch (e: CancellationException) {
                throw e
            } catch (e: DailyException) {
                pending = null
                publish()
                when (e) {
                    is DailyException.Offline -> _events.tryEmit(DailyEvent.Offline)
                    // Out of step with the server (finished on another phone, say): ask what the state really is.
                    is DailyException.AlreadyFinished, is DailyException.NotStarted -> load { api.state(language) }
                    else -> _events.tryEmit(DailyEvent.Unavailable)
                }
            }
        }
    }

    /** The heart on the result card. Daily results can be favorites like endless ones. */
    fun toggleFavorite() {
        val id = states[language]?.resultId ?: return
        repository.setFavorite(id, !repository.isFavorite(id), now())
        publish()
        onLocalChange()
    }

    // --- Internals ---

    /** Runs a request that decides the phase of the whole screen, and turns failures into Offline or Unavailable. */
    private suspend fun load(silent: Boolean = false, request: suspend () -> DailyState) {
        try {
            apply(request())
        } catch (e: CancellationException) {
            throw e
        } catch (e: DailyException) {
            if (silent) return // a quiet check that failed must not take a playable screen away
            phase = if (e is DailyException.Offline) DailyPhase.Offline else DailyPhase.Unavailable
            publish()
        }
    }

    private fun offline() {
        phase = DailyPhase.Offline
        publish()
    }

    private fun apply(state: DailyState) {
        states[state.language] = state
        if (state.language == language) phase = phaseOf(state)
        receivedAtMs = now()
        if (state.isFinished) record(state)
        publish()
    }

    private fun phaseOf(state: DailyState) = when (state.status) {
        DailyStatus.NotStarted -> DailyPhase.Intro
        DailyStatus.Playing -> DailyPhase.Playing
        DailyStatus.Won, DailyStatus.Lost -> DailyPhase.Finished
    }

    /** Saves the finished attempt on the device. The server already saved it; this makes the streak instant. */
    private fun record(state: DailyState) {
        val id = state.resultId ?: return
        val answer = state.answer ?: return
        if (!recorded.add(id)) return
        repository.finish(
            FinishedRound(
                id = id,
                mode = Mode.DAILY,
                language = state.language,
                answer = answer,
                guesses = state.guesses.map { it.word },
                outcome = if (state.status == DailyStatus.Won) Outcome.WON else Outcome.LOST,
                wasSkipped = false,
                finishedAt = state.finishedAtMs ?: state.serverNowMs,
                durationMs = state.durationMs,
                puzzleDay = state.dayIndex,
            ),
        )
    }

    /** Only one request that changes the screen runs at a time; a new one replaces the old. */
    private fun launchExclusive(block: suspend () -> Unit) {
        job?.cancel()
        job = viewModelScope.launch { block() }
    }

    private fun publish() {
        _ui.value = snapshot()
    }

    private fun snapshot(): DailyUiState {
        val state = states[language]
        val showRound = state != null && (phase == DailyPhase.Playing || phase == DailyPhase.Finished)
        val today = IstanbulDay.dayIndex(state?.serverNowMs ?: now())
        val dayStreak = DayStreaks.compute(
            finishedDays = repository.history().filter { it.mode == Mode.DAILY }.map { it.dayIndex },
            today = today,
        )
        return DailyUiState(
            language = language,
            phase = phase,
            round = if (showRound) roundOf(state!!) else null,
            state = state,
            dayStreak = dayStreak,
            isFavorite = state?.resultId?.let { repository.isFavorite(it) } ?: false,
            receivedAtMs = receivedAtMs,
        )
    }

    private fun roundOf(state: DailyState): RoundSnapshot {
        val rows = state.guesses.map { GuessRow(it.word, it.results) }
        return RoundSnapshot(
            id = "daily-${state.dayIndex}-${state.language.name}",
            index = 0,
            language = state.language,
            wordLength = WORD_LENGTH,
            maxAttempts = state.maxAttempts,
            rows = rows,
            input = input,
            status = when (state.status) {
                DailyStatus.Won -> WordleStatus.Won
                DailyStatus.Lost -> WordleStatus.Lost
                else -> WordleStatus.Playing
            },
            answer = state.answer,
            error = null,
            errorTick = errorTick,
            letterStates = keyStatesOf(rows),
            skipped = false,
            pending = pending,
        )
    }

    private companion object {
        const val WORD_LENGTH = 5
    }
}
