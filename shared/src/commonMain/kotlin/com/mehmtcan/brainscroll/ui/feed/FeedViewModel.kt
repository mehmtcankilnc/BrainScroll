package com.mehmtcan.brainscroll.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.game.wordle.EndlessFeed
import com.mehmtcan.brainscroll.game.wordle.FeedSnapshot
import com.mehmtcan.brainscroll.game.wordle.FinishedRound
import com.mehmtcan.brainscroll.game.wordle.Language
import com.mehmtcan.brainscroll.game.wordle.loadWordList
import com.mehmtcan.brainscroll.stats.Stats
import com.mehmtcan.brainscroll.stats.Streaks
import com.mehmtcan.brainscroll.stats.computeStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Ready(
        val feed: FeedSnapshot,
        /** Ids of the finished puzzles the player marked with the heart. */
        val favoriteIds: Set<String>,
    ) : FeedUiState
}

/** What the profile tab shows. */
data class ProfileState(
    val stats: Stats,
    val favorites: List<FinishedRound>,
)

/**
 * Owns the [EndlessFeed] so it survives screen rotation and recomposition (a ViewModel lives longer
 * than the composables). The screen sends actions and observes [state]; every action re-publishes a snapshot.
 *
 * It also connects the feed to the database: at start it restores unfinished puzzles, the answer streak
 * and the chosen language, and from then on the feed saves its progress through the [repository].
 */
class FeedViewModel(
    private val repository: GameRepository,
    deviceLanguage: Language,
    private val now: () -> Long,
) : ViewModel() {
    private var feed: EndlessFeed? = null
    private var favoriteIds: Set<String> = emptySet()

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state

    private val _profile = MutableStateFlow(ProfileState(computeStats(emptyList()), emptyList()))
    val profile: StateFlow<ProfileState> = _profile

    /** The page the feed was on, so switching tabs and coming back does not jump to the first puzzle. */
    var page: Int = 0
        private set

    init {
        viewModelScope.launch {
            val lists = Language.entries.associateWith { loadWordList(it) }
            val restored = repository.unfinishedRounds()
            feed = EndlessFeed(
                wordLists = lists,
                startLanguage = repository.savedLanguage() ?: deviceLanguage,
                store = repository,
                restored = restored,
                startStreak = Streaks.current(repository.history()),
                now = now,
            ).also { it.ensureSize(maxOf(AHEAD, restored.size + 1)) }
            favoriteIds = repository.favoriteResults().map { it.id }.toSet()
            publish()
            refreshProfile()
        }
    }

    fun type(page: Int, letter: Char) = act { it.type(page, letter) }

    fun backspace(page: Int) = act { it.backspace(page) }

    fun submit(page: Int) {
        act { it.submit(page) }
        refreshProfile()
    }

    /** Called when the pager settles on [page]: keeps the current and the next puzzle ready. */
    fun onPageSettled(page: Int) {
        this.page = page
        act { it.ensureSize(page + AHEAD) }
    }

    /** Asks to leave [page] forward. Returns false when that is not allowed (the screen blocks the swipe). */
    fun leave(page: Int): Boolean {
        val f = feed ?: return false
        val allowed = f.leave(page)
        publish()
        return allowed
    }

    fun setLanguage(language: Language) {
        act { if (it.setLanguage(language)) repository.saveLanguage(language) }
    }

    /** Hearts or un-hearts a finished puzzle. Its result is saved the moment it ends, so the id always exists. */
    fun toggleFavorite(resultId: String) {
        val nowFavorite = resultId !in favoriteIds
        repository.setFavorite(resultId, nowFavorite, now())
        favoriteIds = if (nowFavorite) favoriteIds + resultId else favoriteIds - resultId
        publish()
        refreshProfile()
    }

    fun refreshProfile() {
        _profile.value = ProfileState(
            stats = computeStats(repository.history()),
            favorites = repository.favoriteResults(),
        )
    }

    private fun act(action: (EndlessFeed) -> Unit) {
        val f = feed ?: return
        action(f)
        publish()
    }

    private fun publish() {
        _state.value = FeedUiState.Ready(feed!!.snapshot(), favoriteIds)
    }

    private companion object {
        /** Current puzzle plus the next one (docs/design.md section 8). */
        const val AHEAD = 2
    }
}
