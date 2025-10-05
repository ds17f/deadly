package com.grateful.deadly.services.show

import com.grateful.deadly.data.show.ShowDao
import com.grateful.deadly.domain.mappers.ShowMappers
import com.grateful.deadly.domain.models.Recording
import com.grateful.deadly.domain.models.RecordingSourceType
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.domain.models.ShowFilters
import com.grateful.deadly.domain.models.ShowOrderBy
import com.grateful.deadly.services.data.models.RecordingEntity
import com.grateful.deadly.services.data.models.ShowEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * Universal ShowService implementation (Universal Service + Platform Tool pattern)
 *
 * Contains all business logic for show operations in commonMain.
 * Delegates raw database operations to platform ShowDao tools.
 * Handles entity-to-domain mapping, filtering logic, and business rules.
 */
class ShowService(
    private val showDao: ShowDao
) {
    private val showMappers = ShowMappers()

    // === Core Operations ===

    /**
     * Insert a show entity into the database.
     * Uses INSERT OR REPLACE to handle duplicates.
     */
    suspend fun insertShow(show: ShowEntity) {
        showDao.insertShow(show)
    }

    /**
     * Insert multiple shows in a transaction for performance.
     */
    suspend fun insertShows(shows: List<ShowEntity>) {
        showDao.insertShows(shows)
    }

    /**
     * Get a show by its ID, returning domain model.
     * Universal business logic: entity-to-domain conversion.
     */
    suspend fun getShowById(showId: String): Show? {
        val showRow = showDao.getShowEntityById(showId)
        return showRow?.let { row ->
            val entity = mapShowRowToEntity(row)
            showMappers.entityToDomain(entity)
        }
    }

    /**
     * Get shows by list of IDs (for FTS4 search results).
     * Maintains the order of the input IDs list for relevance ranking.
     * Universal business logic: preserves order, handles mapping.
     */
    suspend fun getShowsByIds(showIds: List<String>): List<Show> {
        if (showIds.isEmpty()) return emptyList()

        // Preserve the order of showIds for FTS4 relevance ranking
        val shows = showIds.mapNotNull { showId ->
            getShowById(showId)
        }
        return shows
    }

    /**
     * Get shows using flexible filters. This is the main query method.
     * All other query methods are convenience wrappers around this.
     * Universal business logic: filter conversion, entity-to-domain mapping.
     */
    suspend fun getShows(filters: ShowFilters = ShowFilters.all()): List<Show> {
        // Convert ShowFilters to raw database parameters (universal business logic)
        val yearFilter = if (filters.startYear != null && filters.endYear != null) {
            Pair(filters.startYear, filters.endYear)
        } else if (filters.year != null) {
            Pair(filters.year, filters.year)
        } else null

        val showRows = showDao.executeShowQuery(
            yearFilter = yearFilter,
            monthFilter = null, // TODO: Add month support
            venueFilter = filters.venueName,
            cityFilter = filters.city,
            stateFilter = filters.state,
            hasSetlist = filters.hasSetlist,
            hasLineup = filters.hasLineup,
            hasRecordings = filters.hasRecordings,
            isInLibrary = filters.isInLibrary,
            minRating = filters.minRating,
            minReviews = filters.minReviews,
            songFilter = filters.songName,
            memberFilter = filters.memberName,
            orderBy = when (filters.orderBy) {
                com.grateful.deadly.domain.models.ShowOrderBy.DATE -> "date"
                com.grateful.deadly.domain.models.ShowOrderBy.RATING -> "averageRating"
                com.grateful.deadly.domain.models.ShowOrderBy.LIBRARY -> "libraryAddedAt"
            },
            ascending = true, // TODO: Add ascending parameter to ShowFilters
            limit = filters.limit
        )

        // Universal business logic: entity-to-domain conversion
        return showRows.map { row ->
            val entity = mapShowRowToEntity(row)
            showMappers.entityToDomain(entity)
        }
    }

    /**
     * Get the total count of shows in the database.
     */
    suspend fun getShowCount(): Long {
        return showDao.getShowCount()
    }

    /**
     * Delete all shows from the database.
     */
    suspend fun deleteAllShows() {
        showDao.deleteAllShows()
    }

    /**
     * Delete the database file (for data import service).
     */
    suspend fun deleteDatabaseFile(): Boolean {
        // TODO: Implement database file deletion if needed
        // For now, just delete all data
        deleteAllShows()
        deleteAllRecordings()
        return true
    }

    /**
     * Add/remove show from user's library.
     */
    suspend fun updateShowLibraryStatus(showId: String, isInLibrary: Boolean, addedAt: Long?) {
        showDao.updateShowLibraryStatus(showId, isInLibrary, addedAt)
    }

    // === Convenience Methods (wrappers around getShows) ===

    /**
     * Get all shows (no filters).
     */
    suspend fun getAllShows(): List<Show> {
        return getShows(ShowFilters.all())
    }

    /**
     * Get shows for a specific year.
     */
    suspend fun getShowsByYear(year: Int): List<Show> {
        return getShows(ShowFilters.forYear(year))
    }

    /**
     * Get shows within a year range.
     */
    suspend fun getShowsByYearRange(startYear: Int, endYear: Int): List<Show> {
        return getShows(ShowFilters.forYearRange(startYear, endYear))
    }

    /**
     * Get shows for a specific year-month (e.g., "1977-05").
     */
    suspend fun getShowsByYearMonth(yearMonth: String): List<Show> {
        return getShows(ShowFilters.forYearMonth(yearMonth))
    }

    /**
     * Get shows within a date range.
     */
    suspend fun getShowsInDateRange(startDate: String, endDate: String): List<Show> {
        return getShows(ShowFilters.forDateRange(startDate, endDate))
    }

    /**
     * Get shows for a specific month and day across all years (for "Today in History").
     * Returns shows sorted by year ascending (oldest first).
     * Uses SQL query with INNER JOIN to only return shows that have recordings.
     */
    suspend fun getShowsForDate(month: Int, day: Int): List<Show> {
        // Delegate to DAO which uses SQL query with Recording INNER JOIN
        val showRows = showDao.getShowsForDate(month, day)
        return showRows.map { row ->
            val entity = mapShowRowToEntity(row)
            showMappers.entityToDomain(entity)
        }
    }

    /**
     * Search shows by venue name (partial match).
     */
    suspend fun getShowsByVenue(venueName: String): List<Show> {
        return getShows(ShowFilters.forVenue(venueName))
    }

    /**
     * Search shows by city (partial match).
     */
    suspend fun getShowsByCity(city: String): List<Show> {
        return getShows(ShowFilters.forCity(city))
    }

    /**
     * Get shows by exact state match.
     */
    suspend fun getShowsByState(state: String): List<Show> {
        return getShows(ShowFilters.forState(state))
    }

    /**
     * Get shows that have complete setlist data.
     */
    suspend fun getShowsWithSetlists(): List<Show> {
        return getShows(ShowFilters.withSetlists())
    }

    /**
     * Search shows by song name (partial match in setlist).
     */
    suspend fun getShowsBySong(songName: String): List<Show> {
        return getShows(ShowFilters.withSong(songName))
    }

    /**
     * Get shows that have complete lineup data.
     */
    suspend fun getShowsWithLineups(): List<Show> {
        return getShows(ShowFilters.withLineups())
    }

    /**
     * Search shows by band member name (partial match in lineup).
     */
    suspend fun getShowsByMember(memberName: String): List<Show> {
        return getShows(ShowFilters.withMember(memberName))
    }

    /**
     * Get shows that have recordings available.
     */
    suspend fun getShowsWithRecordings(): List<Show> {
        return getShows(ShowFilters.withRecordings())
    }

    /**
     * Get top-rated shows with minimum review threshold.
     */
    suspend fun getTopRatedShows(minReviews: Int, limit: Int): List<Show> {
        return getShows(
            ShowFilters(
                minReviews = minReviews,
                orderBy = com.grateful.deadly.domain.models.ShowOrderBy.RATING,
                limit = limit
            )
        )
    }

    /**
     * Get shows in user's library.
     */
    suspend fun getLibraryShows(): List<Show> {
        return getShows(ShowFilters.inLibrary())
    }

    /**
     * Full-text search across all show fields.
     * Note: This delegates to FTS search service for optimal performance.
     */
    suspend fun searchShows(query: String): List<Show> {
        // TODO: Delegate to SearchService for FTS4 search
        // For now, return empty list until SearchService is implemented
        return emptyList()
    }

    // === Navigation Queries (V2 Pattern) ===

    /**
     * Get the next show chronologically after the given date.
     * Only returns shows with recordings (bestRecordingId NOT NULL).
     * Used for efficient show browsing that crosses year boundaries.
     * Universal business logic: date comparison and filtering.
     */
    suspend fun getNextShowByDate(currentDate: String): Show? {
        val allShows = getAllShowEntities()
        return allShows
            .filter { it.date > currentDate && it.bestRecordingId != null }
            .minByOrNull { it.date }
            ?.let { row ->
                val entity = mapShowRowToEntity(row)
                showMappers.entityToDomain(entity)
            }
    }

    /**
     * Get the previous show chronologically before the given date.
     * Only returns shows with recordings (bestRecordingId NOT NULL).
     * Used for efficient show browsing that crosses year boundaries.
     * Universal business logic: date comparison and filtering.
     */
    suspend fun getPreviousShowByDate(currentDate: String): Show? {
        val allShows = getAllShowEntities()
        return allShows
            .filter { it.date < currentDate && it.bestRecordingId != null }
            .maxByOrNull { it.date }
            ?.let { row ->
                val entity = mapShowRowToEntity(row)
                showMappers.entityToDomain(entity)
            }
    }

    // === Recording Operations (V2 Single-Repository Pattern) ===

    /**
     * Insert a recording entity into the database.
     * Uses INSERT OR REPLACE to handle duplicates.
     */
    suspend fun insertRecording(recording: RecordingEntity) {
        showDao.insertRecording(recording)
    }

    /**
     * Insert multiple recordings in a transaction for performance.
     */
    suspend fun insertRecordings(recordings: List<RecordingEntity>) {
        showDao.insertRecordings(recordings)
    }

    /**
     * Get a recording by its identifier.
     * Universal business logic: entity-to-domain conversion.
     */
    suspend fun getRecordingById(identifier: String): Recording? {
        val recordingRow = showDao.getRecordingEntityById(identifier)
        return recordingRow?.let { row ->
            showMappers.recordingRowToDomain(row)
        }
    }

    /**
     * Get all recordings for a specific show, ordered by rating (best first).
     * Universal business logic: sorting and entity-to-domain conversion.
     */
    suspend fun getRecordingsForShow(showId: String): List<Recording> {
        val recordingRows = showDao.getRecordingEntitiesForShow(showId)
        return recordingRows.map { row ->
            showMappers.recordingRowToDomain(row)
        }.sortedByDescending { it.rating }
    }

    /**
     * Get the best (highest rated) recording for a show.
     * Universal business logic: rating comparison.
     */
    suspend fun getBestRecordingForShow(showId: String): Recording? {
        val recordings = getRecordingsForShow(showId)
        return recordings.maxByOrNull { it.rating }
    }

    /**
     * Get recordings by source type (SBD, AUD, etc).
     * Universal business logic: filtering and entity-to-domain conversion.
     */
    suspend fun getRecordingsBySourceType(sourceType: String): List<Recording> {
        // TODO: Add source type filtering to DAO
        // For now, get all and filter in memory
        val allRecordings = getAllRecordingEntities()
        return allRecordings.filter { it.source?.contains(sourceType, true) == true }
            .map { row ->
                showMappers.recordingRowToDomain(row)
            }
    }

    /**
     * Get top-rated recordings with minimum review threshold.
     * Universal business logic: filtering and sorting.
     */
    suspend fun getTopRatedRecordings(minRating: Double, minReviews: Int, limit: Int): List<Recording> {
        val allRecordings = getAllRecordingEntities()
        return allRecordings
            .filter { it.rating >= minRating && it.reviewCount >= minReviews }
            .sortedByDescending { it.rating }
            .take(limit)
            .map { row ->
                showMappers.recordingRowToDomain(row)
            }
    }

    /**
     * Get the total count of recordings in the database.
     */
    suspend fun getRecordingCount(): Long {
        return showDao.getRecordingCount()
    }

    /**
     * Get count of recordings for a specific show.
     * Universal business logic: counting.
     */
    suspend fun getRecordingCountForShow(showId: String): Long {
        val recordings = showDao.getRecordingEntitiesForShow(showId)
        return recordings.size.toLong()
    }

    /**
     * Delete all recordings from the database.
     */
    suspend fun deleteAllRecordings() {
        showDao.deleteAllRecordings()
    }

    /**
     * Delete all recordings for a specific show.
     */
    suspend fun deleteRecordingsForShow(showId: String) {
        showDao.deleteRecordingsForShow(showId)
    }

    // === Recent Shows Operations (V2 Pattern) ===

    /**
     * Record a show play with UPSERT logic for smart deduplication.
     * Updates lastPlayedTimestamp and increments totalPlayCount.
     */
    suspend fun recordShowPlay(showId: String, playTimestamp: Long = Clock.System.now().toEpochMilliseconds()) {
        showDao.recordShowPlay(showId, playTimestamp)
    }

    /**
     * Get recent shows ordered by last played timestamp.
     * Universal business logic: entity-to-domain conversion.
     */
    suspend fun getRecentShows(limit: Int = 8): List<Show> {
        val recentShowRows = showDao.getRecentShowEntities(limit)
        return recentShowRows.map { row ->
            val entity = mapShowRowToEntity(row)
            showMappers.entityToDomain(entity)
        }
    }

    /**
     * Get reactive flow of recent shows for UI observation (V2 pattern).
     * Universal business logic: Flow mapping.
     */
    fun getRecentShowsFlow(limit: Int = 8): Flow<List<Show>> {
        return showDao.getRecentShowEntitiesFlow(limit).map { showRows ->
            showRows.map { row ->
                val entity = mapShowRowToEntity(row)
                showMappers.entityToDomain(entity)
            }
        }
    }

    /**
     * Check if a show is in the recent list.
     * Universal business logic: list membership check.
     */
    suspend fun isShowInRecent(showId: String): Boolean {
        val recentShows = getRecentShows(50) // Check larger list
        return recentShows.any { it.id == showId }
    }

    /**
     * Remove a show from recent list (privacy control).
     */
    suspend fun removeRecentShow(showId: String) {
        showDao.removeRecentShow(showId)
    }

    /**
     * Clear all recent shows (privacy control).
     */
    suspend fun clearAllRecentShows() {
        showDao.clearAllRecentShows()
    }

    /**
     * Get recent shows statistics for analytics.
     * Universal business logic: statistics calculation.
     */
    suspend fun getRecentShowsStats(): RecentShowsStats {
        val recentShows = showDao.getRecentShowEntities(1000) // Get large sample
        val playTimestamps: List<Long> = emptyList() // TODO: Extract actual play timestamps from recent shows

        return RecentShowsStats(
            totalShows = recentShows.size,
            avgPlayCount = 1.0, // TODO: Calculate from actual data
            maxPlayCount = 1, // TODO: Calculate from actual data
            oldestPlayTimestamp = playTimestamps.minOrNull(),
            newestPlayTimestamp = playTimestamps.maxOrNull()
        )
    }

    /**
     * Clean up old recent shows entries (maintenance - keep only N most recent).
     */
    suspend fun cleanupOldRecentShows(keepCount: Int = 50) {
        showDao.cleanupOldRecentShows(keepCount)
    }

    // === Statistics ===

    /**
     * Get show count grouped by year.
     * Universal business logic: Map conversion.
     */
    suspend fun getShowCountByYear(): Map<Int, Int> {
        val counts = showDao.getShowCountByYear()
        return counts.toMap()
    }

    /**
     * Get show count grouped by venue.
     * Universal business logic: Map conversion.
     */
    suspend fun getShowCountByVenue(): Map<String, Int> {
        val counts = showDao.getShowCountByVenue()
        return counts.toMap()
    }

    /**
     * Get rating statistics for all shows.
     * Universal business logic: statistics calculation.
     */
    suspend fun getAverageRatingStats(): RatingStats {
        val allShows = getAllShowEntities()
        val ratedShows = allShows.filter { it.averageRating != null }

        return RatingStats(
            totalShows = allShows.size,
            ratedShows = ratedShows.size,
            averageRating = if (ratedShows.isNotEmpty()) {
                ratedShows.mapNotNull { it.averageRating }.average()
            } else null,
            minRating = ratedShows.mapNotNull { it.averageRating }.minOrNull(),
            maxRating = ratedShows.mapNotNull { it.averageRating }.maxOrNull()
        )
    }

    // === Helper Methods (Universal Business Logic) ===

    /**
     * Get all raw show entities (for internal business logic).
     */
    private suspend fun getAllShowEntities(): List<com.grateful.deadly.database.Show> {
        return showDao.getAllShowEntities()
    }

    /**
     * Get all raw recording entities (for internal business logic).
     */
    private suspend fun getAllRecordingEntities(): List<com.grateful.deadly.database.Recording> {
        // TODO: Add method to ShowDao for getting all recordings
        return emptyList()
    }

    /**
     * Convert SQLDelight Show row to ShowEntity (Universal Service business logic).
     * Handles platform-specific type conversions (Long ↔ Int, Boolean mapping).
     */
    private fun mapShowRowToEntity(row: com.grateful.deadly.database.Show): ShowEntity {
        return ShowEntity(
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

    /**
     * Convert SQLDelight Recording row to RecordingEntity (Universal Service business logic).
     */
    private fun mapRecordingRowToEntity(row: com.grateful.deadly.database.Recording): RecordingEntity {
        return RecordingEntity(
            identifier = row.identifier,
            showId = row.showId,
            sourceType = row.sourceType,
            taper = row.taper,
            source = row.source,
            lineage = row.lineage,
            sourceTypeString = row.sourceTypeString,
            rating = row.rating,
            rawRating = row.rawRating,
            reviewCount = row.reviewCount.toInt(),
            confidence = row.confidence,
            highRatings = row.highRatings.toInt(),
            lowRatings = row.lowRatings.toInt(),
            collectionTimestamp = row.collectionTimestamp
        )
    }
}

/**
 * Rating statistics data class.
 */
data class RatingStats(
    val totalShows: Int,
    val ratedShows: Int,
    val averageRating: Double?,
    val minRating: Double?,
    val maxRating: Double?
)

/**
 * Recent shows statistics data class (V2 pattern).
 */
data class RecentShowsStats(
    val totalShows: Int,
    val avgPlayCount: Double,
    val maxPlayCount: Int,
    val oldestPlayTimestamp: Long?,
    val newestPlayTimestamp: Long?
)