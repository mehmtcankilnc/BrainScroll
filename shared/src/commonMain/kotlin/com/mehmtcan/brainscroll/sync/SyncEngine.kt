package com.mehmtcan.brainscroll.sync

import com.mehmtcan.brainscroll.data.FavoriteRecord
import com.mehmtcan.brainscroll.data.GameRepository
import com.mehmtcan.brainscroll.data.KIND_FAVORITE
import com.mehmtcan.brainscroll.data.KIND_RESULT
import com.mehmtcan.brainscroll.data.QueueItem
import com.mehmtcan.brainscroll.game.wordle.FinishedRound

sealed interface SyncResult {
    data class Success(
        /** Queue items that were sent (or dropped because the server refused them). */
        val uploaded: Int,
        val downloadedResults: Int,
        val downloadedFavorites: Int,
    ) : SyncResult

    /** The cloud could not be reached or failed. Nothing is lost: the queue is untouched, try again later. */
    data class Failure(val cause: CloudException) : SyncResult
}

/**
 * Keeps the local database and the cloud in step (docs/decisions.md: offline-first, own outbox).
 *
 * The local database is the source of truth. Everything the player does is saved locally first and queued;
 * [sync] sends the queue and then brings in what the cloud has that this device does not.
 *
 * Why this is safe to repeat and to interrupt at any point:
 * - results are additive and have unique ids, so uploading one twice never duplicates it;
 * - a queue entry is only removed after its upload worked;
 * - favorites use last-writer-wins on `updatedAt`, on this device and in the cloud.
 */
class SyncEngine(
    private val repository: GameRepository,
    private val cloud: CloudApi,
    private val batchSize: Int = 100,
) {
    /** One sync round for the account [userId], which must be the account of the cloud session. */
    suspend fun sync(userId: String): SyncResult = try {
        prepareQueue(userId)
        val uploaded = push()
        val results = cloud.downloadResults()
        repository.mergeRemoteResults(results)
        val favorites = cloud.downloadFavorites()
        repository.mergeRemoteFavorites(favorites)
        SyncResult.Success(uploaded, results.size, favorites.size)
    } catch (e: CloudException) {
        SyncResult.Failure(e)
    }

    /**
     * A different account than last time (first sign-in, or a switch) has none of this device's history:
     * offer all of it. Offering again what the cloud already has is harmless. The queue is saved on disk,
     * so if the upload is interrupted it continues next time without asking again.
     */
    private fun prepareQueue(userId: String) {
        if (repository.syncedUserId() == userId) return
        repository.enqueueEverything()
        repository.setSyncedUserId(userId)
    }

    private suspend fun push(): Int {
        var sent = 0
        while (true) {
            val items = repository.pendingItems(batchSize)
            if (items.isEmpty()) return sent

            val results = repository.resultsByIds(items.filter { it.kind == KIND_RESULT }.map { it.key })
            val favorites = items.filter { it.kind == KIND_FAVORITE }.mapNotNull { repository.favoriteRecord(it.key) }

            try {
                upload(results, favorites)
            } catch (e: CloudException.Rejected) {
                // One bad item must not block the rest of the queue forever: find it by sending items one by one.
                if (items.size > 1) {
                    uploadOneByOne(items)
                    sent += items.size
                    continue
                }
                // A single refused item will never be accepted: it is removed below like every handled item.
            }
            items.forEach(repository::removeFromQueue)
            sent += items.size
        }
    }

    private suspend fun uploadOneByOne(items: List<QueueItem>) {
        for (item in items) {
            val results = if (item.kind == KIND_RESULT) repository.resultsByIds(listOf(item.key)) else emptyList()
            val favorites = if (item.kind == KIND_FAVORITE) listOfNotNull(repository.favoriteRecord(item.key)) else emptyList()
            try {
                upload(results, favorites)
            } catch (e: CloudException.Rejected) {
                // refused for good: drop it below, like every other handled item
            }
            repository.removeFromQueue(item)
        }
    }

    private suspend fun upload(results: List<FinishedRound>, favorites: List<FavoriteRecord>) {
        if (results.isNotEmpty()) cloud.uploadResults(results)
        if (favorites.isNotEmpty()) cloud.uploadFavorites(favorites)
    }
}
