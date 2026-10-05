package com.mehmtcan.brainscroll.sync

import com.mehmtcan.brainscroll.data.FavoriteRecord
import com.mehmtcan.brainscroll.game.wordle.FinishedRound

/**
 * An in-memory stand-in for Supabase that behaves like the real tables: results are append-only and ignore
 * duplicates, favorites keep the newest write. Each user has their own data. Failures can be injected.
 */
class FakeCloud : CloudApi {
    /** The account of the "session". Tests change it to simulate signing in as someone else. */
    var currentUser: String = "user-a"

    val results = mutableMapOf<String, MutableMap<String, FinishedRound>>()
    val favorites = mutableMapOf<String, MutableMap<String, FavoriteRecord>>()

    /** The next calls fail with a [CloudException.Transient] (offline) while this is above zero. */
    var failTransientCalls = 0

    /** Uploading a result with one of these ids is refused for good. */
    val rejectedResultIds = mutableSetOf<String>()

    var uploadCalls = 0
        private set

    fun resultsOf(user: String): Map<String, FinishedRound> = results[user].orEmpty()
    fun favoritesOf(user: String): Map<String, FavoriteRecord> = favorites[user].orEmpty()

    private fun checkOnline() {
        if (failTransientCalls > 0) {
            failTransientCalls--
            throw CloudException.Transient("offline")
        }
    }

    override suspend fun uploadResults(results: List<FinishedRound>) {
        checkOnline()
        uploadCalls++
        if (results.any { it.id in rejectedResultIds }) throw CloudException.Rejected("refused")
        val mine = this.results.getOrPut(currentUser) { mutableMapOf() }
        results.forEach { mine.putIfAbsent(it.id, it) } // ON CONFLICT DO NOTHING
    }

    override suspend fun uploadFavorites(favorites: List<FavoriteRecord>) {
        checkOnline()
        uploadCalls++
        val mine = this.favorites.getOrPut(currentUser) { mutableMapOf() }
        favorites.forEach { incoming ->
            val stored = mine[incoming.resultId]
            if (stored == null || incoming.updatedAt >= stored.updatedAt) mine[incoming.resultId] = incoming
        }
    }

    override suspend fun downloadResults(): List<FinishedRound> {
        checkOnline()
        return resultsOf(currentUser).values.toList()
    }

    override suspend fun downloadFavorites(): List<FavoriteRecord> {
        checkOnline()
        return favoritesOf(currentUser).values.toList()
    }
}
