package com.grateful.deadly.data.library

import com.grateful.deadly.database.Database
import kotlinx.coroutines.flow.Flow

/**
 * Platform-specific library database operations (Universal Service + Platform Tool pattern)
 *
 * This is the platform tool - handles only low-level database operations.
 * Business logic (Flow combinations, joins, mapping) belongs in LibraryService.
 * Only platform difference should be Dispatchers.IO vs Dispatchers.Default.
 */
expect class LibraryDao(database: Database) {

    // === Raw Database Flow Operations ===

    /**
     * Get library show entities flow (raw database)
     * Returns raw database entities without business logic
     */
    fun getAllLibraryShowsFlow(): Flow<List<com.grateful.deadly.database.LibraryShow>>

    /**
     * Get all shows flow (raw database)
     * Used for in-memory joins in universal service
     */
    fun getAllShowsFlow(): Flow<List<com.grateful.deadly.database.Show>>

    /**
     * Get library show count flow (raw database)
     */
    fun getLibraryShowCountFlow(): Flow<Long>

    /**
     * Get pinned show count flow (raw database)
     */
    fun getPinnedShowCountFlow(): Flow<Long>

    /**
     * Check if show is in library (raw database)
     */
    fun isShowInLibraryFlow(showId: String): Flow<Boolean>

    /**
     * Check if show is pinned (raw database)
     */
    fun isShowPinnedFlow(showId: String): Flow<Boolean>

    // === Database Mutation Operations ===

    /**
     * Add show to library (V2 hybrid pattern transaction)
     * Updates both LibraryShow table and denormalized Show columns
     */
    suspend fun addToLibrary(showId: String, timestamp: Long)

    /**
     * Remove show from library (V2 hybrid pattern transaction)
     * Removes from LibraryShow table and updates denormalized Show columns
     */
    suspend fun removeFromLibrary(showId: String)

    /**
     * Update pin status for a show
     */
    suspend fun updatePinStatus(showId: String, isPinned: Boolean)

    /**
     * Update library notes for a show
     */
    suspend fun updateLibraryNotes(showId: String, notes: String?)

    /**
     * Clear entire library (V2 hybrid pattern transaction)
     * Clears LibraryShow table and updates all denormalized Show columns
     */
    suspend fun clearLibrary()

    /**
     * Unpin all shows
     */
    suspend fun unpinAllShows()
}