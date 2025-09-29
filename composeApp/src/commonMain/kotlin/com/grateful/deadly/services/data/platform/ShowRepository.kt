package com.grateful.deadly.services.data.platform

import com.grateful.deadly.domain.models.Recording
import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.domain.models.ShowFilters
import com.grateful.deadly.services.data.models.RecordingEntity
import com.grateful.deadly.services.data.models.ShowEntity

/**
 * Platform-specific database operations for show data.
 *
 * This is the platform tool in the Universal Service + Platform Tool pattern.
 * It handles low-level database operations and entity↔domain conversion.
 * Returns rich domain models for business logic use.
 */
expect class ShowRepository(database: com.grateful.deadly.database.Database) {

    // === Core Operations ===

    /**
     * Insert a show entity into the database.
     * Uses INSERT OR REPLACE to handle duplicates.
     */
    suspend fun insertShow(show: ShowEntity)

    /**
     * Insert multiple shows in a transaction for performance.
     */
    suspend fun insertShows(shows: List<ShowEntity>)

    /**
     * Get a show by its ID, returning domain model.
     */
    suspend fun getShowById(showId: String): Show?

    /**
     * Get shows by list of IDs (for FTS4 search results).
     * Maintains the order of the input IDs list for relevance ranking.
     */
    suspend fun getShowsByIds(showIds: List<String>): List<Show>

    /**
     * Get shows using flexible filters. This is the main query method.
     * All other query methods are convenience wrappers around this.
     */
    suspend fun getShows(filters: ShowFilters = ShowFilters.all()): List<Show>

    /**
     * Get the total count of shows in the database.
     */
    suspend fun getShowCount(): Long

    /**
     * Delete all shows from the database.
     */
    suspend fun deleteAllShows()

    /**
     * Delete the entire database file and recreate it with current schema.
     * This is useful when schema mismatches occur.
     */
    suspend fun deleteDatabaseFile(): Boolean

    /**
     * Add/remove show from user's library.
     */
    suspend fun updateShowLibraryStatus(showId: String, isInLibrary: Boolean, addedAt: Long?)

    // === Convenience Methods (wrappers around getShows) ===

    /**
     * Get all shows (no filters).
     */
    suspend fun getAllShows(): List<Show>

    /**
     * Get shows for a specific year.
     */
    suspend fun getShowsByYear(year: Int): List<Show>

    /**
     * Get shows within a year range.
     */
    suspend fun getShowsByYearRange(startYear: Int, endYear: Int): List<Show>

    /**
     * Get shows for a specific year-month (e.g., "1977-05").
     */
    suspend fun getShowsByYearMonth(yearMonth: String): List<Show>

    /**
     * Get shows within a date range.
     */
    suspend fun getShowsInDateRange(startDate: String, endDate: String): List<Show>

    /**
     * Get shows for a specific month and day across all years (for "Today in History").
     * Returns shows sorted by year descending (most recent first).
     */
    suspend fun getShowsForDate(month: Int, day: Int): List<Show>

    /**
     * Search shows by venue name (partial match).
     */
    suspend fun getShowsByVenue(venueName: String): List<Show>

    /**
     * Search shows by city (partial match).
     */
    suspend fun getShowsByCity(city: String): List<Show>

    /**
     * Get shows by exact state match.
     */
    suspend fun getShowsByState(state: String): List<Show>

    /**
     * Get shows that have complete setlist data.
     */
    suspend fun getShowsWithSetlists(): List<Show>

    /**
     * Search shows by song name (partial match in setlist).
     */
    suspend fun getShowsBySong(songName: String): List<Show>

    /**
     * Get shows that have complete lineup data.
     */
    suspend fun getShowsWithLineups(): List<Show>

    /**
     * Search shows by band member name (partial match in lineup).
     */
    suspend fun getShowsByMember(memberName: String): List<Show>

    /**
     * Get shows that have recordings available.
     */
    suspend fun getShowsWithRecordings(): List<Show>

    /**
     * Get top-rated shows with minimum review threshold.
     */
    suspend fun getTopRatedShows(minReviews: Int, limit: Int): List<Show>

    /**
     * Get shows in user's library.
     */
    suspend fun getLibraryShows(): List<Show>

    /**
     * Full-text search across all show fields.
     */
    suspend fun searchShows(query: String): List<Show>

    // === Recording Operations (V2 Single-Repository Pattern) ===

    /**
     * Insert a recording entity into the database.
     * Uses INSERT OR REPLACE to handle duplicates.
     */
    suspend fun insertRecording(recording: RecordingEntity)

    /**
     * Insert multiple recordings in a transaction for performance.
     */
    suspend fun insertRecordings(recordings: List<RecordingEntity>)

    /**
     * Get a recording by its identifier.
     */
    suspend fun getRecordingById(identifier: String): Recording?

    /**
     * Get all recordings for a specific show, ordered by rating (best first).
     */
    suspend fun getRecordingsForShow(showId: String): List<Recording>

    /**
     * Get the best (highest rated) recording for a show.
     */
    suspend fun getBestRecordingForShow(showId: String): Recording?

    /**
     * Get recordings by source type (SBD, AUD, etc).
     */
    suspend fun getRecordingsBySourceType(sourceType: String): List<Recording>

    /**
     * Get top-rated recordings with minimum review threshold.
     */
    suspend fun getTopRatedRecordings(minRating: Double, minReviews: Int, limit: Int): List<Recording>

    /**
     * Get the total count of recordings in the database.
     */
    suspend fun getRecordingCount(): Long

    /**
     * Get count of recordings for a specific show.
     */
    suspend fun getRecordingCountForShow(showId: String): Long

    /**
     * Delete all recordings from the database.
     */
    suspend fun deleteAllRecordings()

    /**
     * Delete all recordings for a specific show.
     */
    suspend fun deleteRecordingsForShow(showId: String)

    // === Statistics ===

    /**
     * Get show count grouped by year.
     */
    suspend fun getShowCountByYear(): Map<Int, Int>

    /**
     * Get show count grouped by venue.
     */
    suspend fun getShowCountByVenue(): Map<String, Int>

    /**
     * Get rating statistics for all shows.
     */
    suspend fun getAverageRatingStats(): RatingStats
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