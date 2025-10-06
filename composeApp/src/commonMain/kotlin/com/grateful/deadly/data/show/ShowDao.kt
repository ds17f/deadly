package com.grateful.deadly.data.show

import com.grateful.deadly.database.Database
import com.grateful.deadly.services.data.models.RecordingEntity
import com.grateful.deadly.services.data.models.ShowEntity
import kotlinx.coroutines.flow.Flow

/**
 * Platform-specific show database operations (Universal Service + Platform Tool pattern)
 *
 * This is the platform tool - handles only low-level database operations.
 * Business logic (queries, filtering, entity-to-domain mapping) belongs in ShowService.
 * Only platform difference should be Dispatchers.IO vs Dispatchers.Default.
 */
expect class ShowDao(database: Database) {

    // === Raw Database Operations ===

    /**
     * Insert a show entity (raw database operation)
     */
    suspend fun insertShow(show: ShowEntity)

    /**
     * Insert multiple shows in a transaction (raw database operation)
     */
    suspend fun insertShows(shows: List<ShowEntity>)

    /**
     * Get raw show entity by ID (raw database operation)
     */
    suspend fun getShowEntityById(showId: String): com.grateful.deadly.database.Show?

    /**
     * Get raw show entities by list of IDs (raw database operation)
     */
    suspend fun getShowEntitiesByIds(showIds: List<String>): List<com.grateful.deadly.database.Show>

    /**
     * Get all raw show entities (raw database operation)
     */
    suspend fun getAllShowEntities(): List<com.grateful.deadly.database.Show>

    /**
     * Get show count (raw database operation)
     */
    suspend fun getShowCount(): Long

    /**
     * Delete all shows (raw database operation)
     */
    suspend fun deleteAllShows()

    /**
     * Update show library status (raw database operation)
     */
    suspend fun updateShowLibraryStatus(showId: String, isInLibrary: Boolean, addedAt: Long?)

    // === Raw Recording Database Operations ===

    /**
     * Insert a recording entity (raw database operation)
     */
    suspend fun insertRecording(recording: RecordingEntity)

    /**
     * Insert multiple recordings (raw database operation)
     */
    suspend fun insertRecordings(recordings: List<RecordingEntity>)

    /**
     * Get raw recording entity by ID (raw database operation)
     */
    suspend fun getRecordingEntityById(identifier: String): com.grateful.deadly.database.Recording?

    /**
     * Get raw recording entities for show (raw database operation)
     */
    suspend fun getRecordingEntitiesForShow(showId: String): List<com.grateful.deadly.database.Recording>

    /**
     * Get recording count (raw database operation)
     */
    suspend fun getRecordingCount(): Long

    /**
     * Delete all recordings (raw database operation)
     */
    suspend fun deleteAllRecordings()

    /**
     * Delete recordings for show (raw database operation)
     */
    suspend fun deleteRecordingsForShow(showId: String)

    // === Raw Recent Shows Database Operations ===

    /**
     * Record a show play with recording ID (raw database operation)
     * The recordingId tracks which specific recording was played for correct navigation
     */
    suspend fun recordShowPlay(showId: String, playTimestamp: Long, recordingId: String? = null)

    /**
     * Get recent show entities with their last played recording IDs (raw database operation)
     * Returns Show entities paired with their last played recording ID from recent_shows
     */
    suspend fun getRecentShowEntities(limit: Int): List<Pair<com.grateful.deadly.database.Show, String?>>

    /**
     * Get recent show entities flow with their last played recording IDs (raw database operation)
     * Returns Show entities paired with their last played recording ID from recent_shows
     */
    fun getRecentShowEntitiesFlow(limit: Int): Flow<List<Pair<com.grateful.deadly.database.Show, String?>>>

    /**
     * Remove recent show (raw database operation)
     */
    suspend fun removeRecentShow(showId: String)

    /**
     * Clear all recent shows (raw database operation)
     */
    suspend fun clearAllRecentShows()

    /**
     * Clean up old recent shows (raw database operation)
     */
    suspend fun cleanupOldRecentShows(keepCount: Int)

    // === User Recording Preferences (raw database operation) ===

    /**
     * Get user's preferred recording for a show (raw database operation)
     * Returns the recordingId the user has set as their default for this show, or null if no preference exists
     */
    suspend fun getUserRecordingPreference(showId: String): String?

    /**
     * Set user's preferred recording for a show (raw database operation)
     * Saves the user's "Set as Default" recording choice
     */
    suspend fun setUserRecordingPreference(showId: String, recordingId: String, timestamp: Long)

    /**
     * Clear user's preferred recording for a show (raw database operation)
     * Removes the user's saved preference (used by "Reset to Recommended")
     */
    suspend fun clearUserRecordingPreference(showId: String)

    // === Raw Query Operations (SQLDelight direct) ===

    /**
     * Execute flexible show query with filters (raw database operation)
     * Returns raw database entities for business logic processing
     */
    suspend fun executeShowQuery(
        yearFilter: Pair<Int, Int>? = null,
        monthFilter: Int? = null,
        venueFilter: String? = null,
        cityFilter: String? = null,
        stateFilter: String? = null,
        hasSetlist: Boolean? = null,
        hasLineup: Boolean? = null,
        hasRecordings: Boolean? = null,
        isInLibrary: Boolean? = null,
        minRating: Double? = null,
        minReviews: Int? = null,
        songFilter: String? = null,
        memberFilter: String? = null,
        orderBy: String = "date",
        ascending: Boolean = true,
        limit: Int? = null
    ): List<com.grateful.deadly.database.Show>

    /**
     * Get show count with filters (raw database operation)
     */
    suspend fun getShowCountWithFilters(
        yearFilter: Pair<Int, Int>? = null,
        monthFilter: Int? = null,
        venueFilter: String? = null,
        cityFilter: String? = null,
        stateFilter: String? = null,
        hasSetlist: Boolean? = null,
        hasLineup: Boolean? = null,
        hasRecordings: Boolean? = null,
        isInLibrary: Boolean? = null,
        minRating: Double? = null,
        minReviews: Int? = null
    ): Long

    /**
     * Get show count by year (raw database operation)
     */
    suspend fun getShowCountByYear(): List<Pair<Int, Int>>

    /**
     * Get show count by venue (raw database operation)
     */
    suspend fun getShowCountByVenue(): List<Pair<String, Int>>

    /**
     * Get shows for a specific month and day across all years (raw database operation).
     * Uses INNER JOIN with Recording table to only return shows with recordings.
     */
    suspend fun getShowsForDate(month: Int, day: Int): List<com.grateful.deadly.database.Show>
}