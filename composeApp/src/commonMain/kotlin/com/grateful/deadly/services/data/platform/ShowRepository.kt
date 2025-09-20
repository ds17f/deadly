package com.grateful.deadly.services.data.platform

import com.grateful.deadly.services.data.models.ShowEntity

/**
 * Platform-specific database operations for show data.
 *
 * This is the platform tool in the Universal Service + Platform Tool pattern.
 * It handles only the low-level, platform-specific database operations.
 * The universal DataImportService handles JSON parsing, mapping, and workflow.
 */
expect class ShowRepository(database: com.grateful.deadly.database.Database) {
    /**
     * Insert a show entity into the database.
     * Uses INSERT OR REPLACE to handle duplicates.
     */
    suspend fun insertShow(show: ShowEntity)

    /**
     * Get a show by its ID.
     */
    suspend fun getShowById(showId: String): ShowEntity?

    /**
     * Get the total count of shows in the database.
     */
    suspend fun getShowCount(): Long

    /**
     * Delete all shows from the database.
     */
    suspend fun deleteAllShows()

    /**
     * Insert multiple shows in a transaction for performance.
     */
    suspend fun insertShows(shows: List<ShowEntity>)
}