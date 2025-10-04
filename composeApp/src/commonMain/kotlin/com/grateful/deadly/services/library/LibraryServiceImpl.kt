package com.grateful.deadly.services.library

import com.grateful.deadly.core.logging.Logger
import com.grateful.deadly.data.library.LibraryDao
import com.grateful.deadly.domain.mappers.ShowMappers
import com.grateful.deadly.domain.models.LibraryDownloadStatus
import com.grateful.deadly.domain.models.LibraryShow
import com.grateful.deadly.domain.models.LibraryStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock

/**
 * Universal LibraryService implementation (Universal Service + Platform Tool pattern)
 *
 * Provides business logic and reactive StateFlows for library operations.
 * All business logic is in commonMain - delegates to platform LibraryDao tool.
 * Handles Flow combinations, database joins, and entity-to-domain mapping.
 * Uses Result-based error handling and comprehensive logging.
 */
class LibraryServiceImpl(
    private val libraryDao: LibraryDao,
    private val coroutineScope: CoroutineScope
) : LibraryService {

    companion object {
        private const val TAG = "LibraryService"
        private const val STATEFLOW_TIMEOUT = 5000L // 5 seconds
    }

    private val showMappers = ShowMappers()

    // === Reactive StateFlows (Universal Service + Platform Tool pattern) ===

    /**
     * Real reactive StateFlow backed by database with universal business logic
     * Combines library + show data, handles joins and mapping in universal code
     */
    private val _currentShows: StateFlow<List<LibraryShow>> = combine(
        libraryDao.getAllLibraryShowsFlow(),
        libraryDao.getAllShowsFlow()
    ) { libraryEntities, allShows ->
        // Universal business logic: in-memory join and mapping
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
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(STATEFLOW_TIMEOUT),
        initialValue = emptyList()
    )

    /**
     * Real reactive StateFlow for library statistics with universal business logic
     * Combines counts and calculates stats in universal code
     */
    private val _libraryStats: StateFlow<LibraryStats> = combine(
        libraryDao.getLibraryShowCountFlow(),
        libraryDao.getPinnedShowCountFlow()
    ) { total, pinned ->
        // Universal business logic: stats calculation
        LibraryStats(
            totalShows = total.toInt(),
            totalPinned = pinned.toInt(),
            totalDownloaded = 0, // TODO: Add download integration
            totalStorageUsed = 0L // TODO: Add download integration
        )
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(STATEFLOW_TIMEOUT),
        initialValue = LibraryStats(totalShows = 0, totalPinned = 0)
    )

    // === Public Interface ===

    override fun getCurrentShows(): StateFlow<List<LibraryShow>> {
        Logger.d(TAG, "getCurrentShows() - returning StateFlow with ${_currentShows.value.size} shows")
        return _currentShows
    }

    override fun getLibraryStats(): StateFlow<LibraryStats> {
        Logger.d(TAG, "getLibraryStats() - returning StateFlow with stats: ${_libraryStats.value}")
        return _libraryStats
    }

    override suspend fun addToLibrary(showId: String): Result<Unit> {
        Logger.d(TAG, "addToLibrary() - Adding show '$showId' to library")
        val timestamp = Clock.System.now().toEpochMilliseconds()

        return try {
            libraryDao.addToLibrary(showId, timestamp)
            Logger.i(TAG, "addToLibrary() - Successfully added show '$showId' to library")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "addToLibrary() - Failed to add show '$showId' to library: ${e.message}")
            Result.failure(Exception("Failed to add show to library: ${e.message}", e))
        }
    }

    override suspend fun removeFromLibrary(showId: String): Result<Unit> {
        Logger.d(TAG, "removeFromLibrary() - Removing show '$showId' from library")

        return try {
            libraryDao.removeFromLibrary(showId)
            Logger.i(TAG, "removeFromLibrary() - Successfully removed show '$showId' from library")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "removeFromLibrary() - Failed to remove show '$showId' from library: ${e.message}")
            Result.failure(Exception("Failed to remove show from library: ${e.message}", e))
        }
    }

    override suspend fun clearLibrary(): Result<Unit> {
        Logger.d(TAG, "clearLibrary() - Clearing entire library")

        return try {
            libraryDao.clearLibrary()
            Logger.i(TAG, "clearLibrary() - Successfully cleared entire library")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "clearLibrary() - Failed to clear library: ${e.message}")
            Result.failure(Exception("Failed to clear library: ${e.message}", e))
        }
    }

    override fun isShowInLibrary(showId: String): StateFlow<Boolean> {
        Logger.d(TAG, "isShowInLibrary() - Creating reactive StateFlow for show '$showId'")

        return libraryDao.isShowInLibraryFlow(showId)
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(STATEFLOW_TIMEOUT),
                initialValue = false
            )
    }

    override suspend fun pinShow(showId: String): Result<Unit> {
        Logger.d(TAG, "pinShow() - Pinning show '$showId'")

        return try {
            libraryDao.updatePinStatus(showId, isPinned = true)
            Logger.i(TAG, "pinShow() - Successfully pinned show '$showId'")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "pinShow() - Failed to pin show '$showId': ${e.message}")
            Result.failure(Exception("Failed to pin show: ${e.message}", e))
        }
    }

    override suspend fun unpinShow(showId: String): Result<Unit> {
        Logger.d(TAG, "unpinShow() - Unpinning show '$showId'")

        return try {
            libraryDao.updatePinStatus(showId, isPinned = false)
            Logger.i(TAG, "unpinShow() - Successfully unpinned show '$showId'")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "unpinShow() - Failed to unpin show '$showId': ${e.message}")
            Result.failure(Exception("Failed to unpin show: ${e.message}", e))
        }
    }

    override fun isShowPinned(showId: String): StateFlow<Boolean> {
        Logger.d(TAG, "isShowPinned() - Creating reactive StateFlow for show '$showId'")

        return libraryDao.isShowPinnedFlow(showId)
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(STATEFLOW_TIMEOUT),
                initialValue = false
            )
    }

    override suspend fun updateLibraryNotes(showId: String, notes: String?): Result<Unit> {
        Logger.d(TAG, "updateLibraryNotes() - Updating notes for show '$showId'")

        return try {
            libraryDao.updateLibraryNotes(showId, notes)
            Logger.i(TAG, "updateLibraryNotes() - Successfully updated notes for show '$showId'")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "updateLibraryNotes() - Failed to update notes for show '$showId': ${e.message}")
            Result.failure(Exception("Failed to update library notes: ${e.message}", e))
        }
    }

    override suspend fun shareShow(showId: String): Result<Unit> {
        Logger.d(TAG, "shareShow() - Sharing show '$showId' (placeholder implementation)")

        // TODO: Implement platform-specific sharing
        // This would delegate to a platform-specific sharing service
        // For now, return success as placeholder
        Logger.i(TAG, "shareShow() - Share show '$showId' (not yet implemented)")
        return Result.success(Unit)
    }

    override suspend fun unpinAllShows(): Result<Unit> {
        Logger.d(TAG, "unpinAllShows() - Unpinning all shows in library")

        return try {
            libraryDao.unpinAllShows()
            Logger.i(TAG, "unpinAllShows() - Successfully unpinned all shows")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e(TAG, "unpinAllShows() - Failed to unpin all shows: ${e.message}")
            Result.failure(Exception("Failed to unpin all shows: ${e.message}", e))
        }
    }

    /**
     * Populate library with test data (Development only)
     */
    override suspend fun populateTestData(): Result<Unit> {
        return try {
            Logger.d(TAG, "populateTestData() - Adding test shows to library for development")

            // Sample show IDs (these should exist in your show database)
            val testShows = listOf(
                "1970-05-02-harpur-college", // Famous Workingman's Dead release show
                "1972-05-08-bickershaw-festival", // Europe '72 tour
                "1977-05-08-barton-hall", // Cornell '77 - legendary show
                "1989-07-07-jfk-stadium", // 1980s era
                "1995-07-09-soldier-field" // Final tour
            )

            val currentTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

            testShows.forEach { showId ->
                Logger.d(TAG, "Adding test show to library: $showId")
                // Add to library (will skip if already exists due to IGNORE conflict resolution)
                libraryDao.addToLibrary(showId, currentTime)
            }

            // Pin a couple of the test shows
            Logger.d(TAG, "Pinning select test shows")
            libraryDao.updatePinStatus("1977-05-08-barton-hall", true) // Cornell '77 is always pinned
            libraryDao.updatePinStatus("1972-05-08-bickershaw-festival", true) // Europe '72 classic

            Logger.d(TAG, "Test data population completed successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Logger.e(TAG, "Error populating test data", e)
            Result.failure(e)
        }
    }

    // === Universal Business Logic Helper Functions ===

    /**
     * Convert SQLDelight Show row to ShowEntity (Universal Service business logic)
     * Handles platform-specific type conversions (Long ↔ Int, Boolean mapping).
     * This logic was moved from platform implementations to universal service.
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