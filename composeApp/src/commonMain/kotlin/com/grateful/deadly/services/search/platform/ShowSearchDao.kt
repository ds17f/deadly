package com.grateful.deadly.services.search.platform

import com.grateful.deadly.database.Database

/**
 * Platform-specific FTS4 operations for show search.
 *
 * Following V2's exact pattern where SearchService uses ShowSearchDao directly
 * for FTS queries, separate from ShowRepository domain operations.
 *
 * This is the platform tool for FTS4 search operations in the
 * Universal Service + Platform Tool pattern.
 */
expect class ShowSearchDao(database: Database) {

    /**
     * FTS4 search query - returns show IDs with relevance ranking.
     * This is the core search method that SearchService will use.
     *
     * @param query FTS4 query string
     * @return List of show IDs ordered by relevance (best matches first)
     */
    suspend fun searchShows(query: String): List<String>

    /**
     * Insert or update search record for a show.
     * Called during data import to populate FTS index.
     *
     * @param showId Show identifier
     * @param searchText Rich searchable content with enhanced fields
     */
    suspend fun insertOrUpdateShowSearch(showId: String, searchText: String)

    /**
     * Insert multiple search records efficiently in a transaction.
     * Used during bulk data import.
     *
     * @param searchRecords List of (showId, searchText) pairs
     */
    suspend fun insertShowSearchBatch(searchRecords: List<Pair<String, String>>)

    /**
     * Get total count of indexed shows.
     * Used for progress tracking and diagnostics.
     */
    suspend fun getIndexedShowCount(): Int

    /**
     * Check if a show is already indexed.
     * Used to avoid duplicate indexing.
     */
    suspend fun isShowIndexed(showId: String): Boolean

    /**
     * Remove specific show from search index.
     * Used when shows are deleted.
     */
    suspend fun removeShowFromIndex(showId: String)

    /**
     * Clear all FTS4 data (for rebuilding index).
     * Used when resetting search index.
     */
    suspend fun clearAllSearchData()
}