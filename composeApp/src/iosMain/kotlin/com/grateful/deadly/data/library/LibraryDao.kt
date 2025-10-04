package com.grateful.deadly.data.library

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.grateful.deadly.database.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * iOS implementation of LibraryDao (Platform Tool)
 *
 * Handles only raw database operations using Dispatchers.Default.
 * All business logic (Flow combinations, joins, mapping) is in LibraryService.
 */
actual class LibraryDao actual constructor(
    private val database: Database
) {

    // === Raw Database Flow Operations ===

    actual fun getAllLibraryShowsFlow(): Flow<List<com.grateful.deadly.database.LibraryShow>> {
        return database.libraryShowQueries.getAllLibraryShowsFlow()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    actual fun getAllShowsFlow(): Flow<List<com.grateful.deadly.database.Show>> {
        return database.showQueries.selectAllShows()
            .asFlow()
            .mapToList(Dispatchers.Default)
    }

    actual fun getLibraryShowCountFlow(): Flow<Long> {
        return database.libraryShowQueries.getLibraryShowCountFlow()
            .asFlow()
            .mapToOne(Dispatchers.Default)
    }

    actual fun getPinnedShowCountFlow(): Flow<Long> {
        return database.libraryShowQueries.getPinnedShowCountFlow()
            .asFlow()
            .mapToOne(Dispatchers.Default)
    }

    actual fun isShowInLibraryFlow(showId: String): Flow<Boolean> {
        return database.libraryShowQueries.isShowInLibraryFlow(showId)
            .asFlow()
            .mapToOne(Dispatchers.Default)
    }

    actual fun isShowPinnedFlow(showId: String): Flow<Boolean> {
        return database.libraryShowQueries.isShowPinnedFlow(showId)
            .asFlow()
            .mapToOne(Dispatchers.Default)
    }

    // === Database Mutation Operations ===

    actual suspend fun addToLibrary(showId: String, timestamp: Long) {
        withContext(Dispatchers.Default) {
            database.transaction {
                // Add to LibraryShow table
                database.libraryShowQueries.addToLibrary(
                    showId = showId,
                    addedToLibraryAt = timestamp
                )

                // Update denormalized Show columns (V2 hybrid pattern)
                database.showQueries.updateShowLibraryStatus(
                    isInLibrary = 1L,
                    libraryAddedAt = timestamp,
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                    showId = showId
                )
            }
        }
    }

    actual suspend fun removeFromLibrary(showId: String) {
        withContext(Dispatchers.Default) {
            database.transaction {
                // Remove from LibraryShow table
                database.libraryShowQueries.removeFromLibrary(showId)

                // Update denormalized Show columns (V2 hybrid pattern)
                database.showQueries.updateShowLibraryStatus(
                    isInLibrary = 0L,
                    libraryAddedAt = null,
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                    showId = showId
                )
            }
        }
    }

    actual suspend fun updatePinStatus(showId: String, isPinned: Boolean) {
        withContext(Dispatchers.Default) {
            database.libraryShowQueries.updatePinStatus(
                isPinned = if (isPinned) 1L else 0L,
                showId = showId
            )
        }
    }

    actual suspend fun updateLibraryNotes(showId: String, notes: String?) {
        withContext(Dispatchers.Default) {
            database.libraryShowQueries.updateLibraryNotes(
                libraryNotes = notes,
                showId = showId
            )
        }
    }

    actual suspend fun clearLibrary() {
        withContext(Dispatchers.Default) {
            database.transaction {
                // Get all library show IDs before clearing
                val libraryShowIds = database.libraryShowQueries.getAllLibraryShowsFlow()
                    .executeAsList()
                    .map { it.showId }

                // Clear LibraryShow table
                database.libraryShowQueries.clearLibrary()

                // Update denormalized Show columns for all affected shows
                libraryShowIds.forEach { showId ->
                    database.showQueries.updateShowLibraryStatus(
                        isInLibrary = 0L,
                        libraryAddedAt = null,
                        updatedAt = Clock.System.now().toEpochMilliseconds(),
                        showId = showId
                    )
                }
            }
        }
    }

    actual suspend fun unpinAllShows() {
        withContext(Dispatchers.Default) {
            database.libraryShowQueries.unpinAllShows()
        }
    }
}