package com.grateful.deadly.services.library.platform

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.grateful.deadly.database.Database
import com.grateful.deadly.domain.mappers.ShowMappers
import com.grateful.deadly.domain.models.LibraryDownloadStatus
import com.grateful.deadly.domain.models.LibraryShow
import com.grateful.deadly.domain.models.LibraryStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Android implementation of LibraryRepository using SQLDelight database.
 *
 * This platform tool handles database operations, joins LibraryShow + Show tables,
 * and converts to LibraryShow domain models. Uses Dispatchers.IO for database operations.
 */
actual class LibraryRepository actual constructor(
    private val database: Database
) {
    private val showMappers = ShowMappers()

    /**
     * Get all library shows as reactive flow with JOIN
     * Orders by pin status (pinned first), then by date added (newest first)
     */
    actual fun getLibraryShowsFlow(): Flow<List<LibraryShow>> {
        // Get library metadata flow
        val libraryShowsFlow = database.libraryShowQueries.getAllLibraryShowsFlow()
            .asFlow()
            .mapToList(Dispatchers.IO)

        // Get all shows flow
        val allShowsFlow = database.showQueries.selectAllShows()
            .asFlow()
            .mapToList(Dispatchers.IO)

        // Combine and join in memory (V2 pattern: manual join for reactivity)
        return combine(libraryShowsFlow, allShowsFlow) { libraryEntities, allShows ->
            val showsMap = allShows.associateBy { it.showId }

            libraryEntities.mapNotNull { libraryEntity ->
                showsMap[libraryEntity.showId]?.let { showRow ->
                    val showEntity = mapShowRowToEntity(showRow)
                    val show = showMappers.entityToDomain(showEntity)

                    LibraryShow(
                        show = show,
                        addedToLibraryAt = libraryEntity.addedToLibraryAt,
                        isPinned = libraryEntity.isPinned == 1L,
                        libraryNotes = libraryEntity.libraryNotes,
                        customRating = libraryEntity.customRating?.toFloat(),
                        lastAccessedAt = libraryEntity.lastAccessedAt,
                        tags = libraryEntity.tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() },
                        downloadStatus = LibraryDownloadStatus.NOT_DOWNLOADED // TODO: Add download integration
                    )
                }
            }
        }
    }

    /**
     * Get library statistics as reactive flow
     */
    actual fun getLibraryStatsFlow(): Flow<LibraryStats> {
        val totalShowsFlow = database.libraryShowQueries.getLibraryShowCountFlow()
            .asFlow()
            .mapToOne(Dispatchers.IO)

        val pinnedShowsFlow = database.libraryShowQueries.getPinnedShowCountFlow()
            .asFlow()
            .mapToOne(Dispatchers.IO)

        return combine(totalShowsFlow, pinnedShowsFlow) { total, pinned ->
            LibraryStats(
                totalShows = total.toInt(),
                totalPinned = pinned.toInt(),
                totalDownloaded = 0, // TODO: Add download integration
                totalStorageUsed = 0L // TODO: Add download integration
            )
        }
    }

    /**
     * Add show to library with V2 hybrid pattern:
     * 1. Add to LibraryShow table (rich metadata)
     * 2. Update denormalized Show.isInLibrary and Show.libraryAddedAt columns
     */
    actual suspend fun addShowToLibrary(showId: String, timestamp: Long): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to add show to library: ${e.message}", e))
        }
    }

    /**
     * Remove show from library with V2 hybrid pattern:
     * 1. Remove from LibraryShow table (CASCADE DELETE)
     * 2. Update denormalized Show.isInLibrary column
     */
    actual suspend fun removeShowFromLibrary(showId: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to remove show from library: ${e.message}", e))
        }
    }

    /**
     * Check if show is in library (reactive)
     */
    actual fun isShowInLibraryFlow(showId: String): Flow<Boolean> {
        return database.libraryShowQueries.isShowInLibraryFlow(showId)
            .asFlow()
            .mapToOne(Dispatchers.IO)
    }

    /**
     * Pin/unpin show
     */
    actual suspend fun updatePinStatus(showId: String, isPinned: Boolean): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                database.libraryShowQueries.updatePinStatus(
                    isPinned = if (isPinned) 1L else 0L,
                    showId = showId
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update pin status: ${e.message}", e))
        }
    }

    /**
     * Check if show is pinned (reactive)
     */
    actual fun isShowPinnedFlow(showId: String): Flow<Boolean> {
        return database.libraryShowQueries.isShowPinnedFlow(showId)
            .asFlow()
            .mapToOne(Dispatchers.IO)
    }

    /**
     * Update library notes
     */
    actual suspend fun updateLibraryNotes(showId: String, notes: String?): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                database.libraryShowQueries.updateLibraryNotes(
                    libraryNotes = notes,
                    showId = showId
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update library notes: ${e.message}", e))
        }
    }

    /**
     * Clear entire library with V2 hybrid pattern:
     * 1. Clear LibraryShow table
     * 2. Update all Show.isInLibrary columns
     */
    actual suspend fun clearLibrary(): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to clear library: ${e.message}", e))
        }
    }

    /**
     * Unpin all shows
     */
    actual suspend fun unpinAllShows(): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                database.libraryShowQueries.unpinAllShows()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to unpin all shows: ${e.message}", e))
        }
    }

    // === Helper Functions ===

    /**
     * Convert SQLDelight Show row to ShowEntity.
     * Handles platform-specific type conversions (Long ↔ Int, Boolean mapping).
     */
    private fun mapShowRowToEntity(row: com.grateful.deadly.database.Show): com.grateful.deadly.services.data.models.ShowEntity {
        return com.grateful.deadly.services.data.models.ShowEntity(
            showId = row.showId,
            date = row.date,
            year = row.year.toInt(),
            month = row.month.toInt(),
            yearMonth = row.yearMonth,
            band = row.band,
            url = row.url,
            venueName = row.venueName,
            city = row.city,
            state = row.state,
            country = row.country,
            locationRaw = row.locationRaw,
            setlistStatus = row.setlistStatus,
            setlistRaw = row.setlistRaw,
            songList = row.songList,
            lineupStatus = row.lineupStatus,
            lineupRaw = row.lineupRaw,
            memberList = row.memberList,
            showSequence = row.showSequence.toInt(),
            recordingsRaw = row.recordingsRaw,
            recordingCount = row.recordingCount.toInt(),
            bestRecordingId = row.bestRecordingId,
            averageRating = row.averageRating,
            totalReviews = row.totalReviews.toInt(),
            isInLibrary = row.isInLibrary == 1L,
            libraryAddedAt = row.libraryAddedAt,
            createdAt = row.createdAt,
            updatedAt = row.updatedAt
        )
    }
}
