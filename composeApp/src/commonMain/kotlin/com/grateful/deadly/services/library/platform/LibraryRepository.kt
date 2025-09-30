package com.grateful.deadly.services.library.platform

import com.grateful.deadly.database.Database
import com.grateful.deadly.domain.models.LibraryShow
import com.grateful.deadly.domain.models.LibraryStats
import kotlinx.coroutines.flow.Flow

/**
 * Platform-specific library database operations (V2 pattern)
 *
 * This is the platform tool in Universal Service + Platform Tool pattern.
 * Handles low-level database operations and joins LibraryShow table with Show table.
 * Returns rich LibraryShow domain models combining show data + library metadata.
 */
expect class LibraryRepository(database: Database) {

    /**
     * Get all library shows as reactive flow (V2 pattern: combines library metadata + show data)
     * Returns shows ordered by pin status (pinned first), then by date added (newest first)
     */
    fun getLibraryShowsFlow(): Flow<List<LibraryShow>>

    /**
     * Get library statistics as reactive flow (V2 pattern)
     */
    fun getLibraryStatsFlow(): Flow<LibraryStats>

    /**
     * Add show to library (V2 pattern)
     * Also updates the denormalized Show.isInLibrary and Show.libraryAddedAt columns
     */
    suspend fun addShowToLibrary(showId: String, timestamp: Long): Result<Unit>

    /**
     * Remove show from library (V2 pattern)
     * CASCADE DELETE removes LibraryShow entry automatically
     * Also updates the denormalized Show.isInLibrary column
     */
    suspend fun removeShowFromLibrary(showId: String): Result<Unit>

    /**
     * Check if show is in library (reactive) (V2 pattern)
     */
    fun isShowInLibraryFlow(showId: String): Flow<Boolean>

    /**
     * Pin/unpin show (V2 pattern)
     */
    suspend fun updatePinStatus(showId: String, isPinned: Boolean): Result<Unit>

    /**
     * Check if show is pinned (reactive) (V2 pattern)
     */
    fun isShowPinnedFlow(showId: String): Flow<Boolean>

    /**
     * Update library notes (V2 pattern)
     */
    suspend fun updateLibraryNotes(showId: String, notes: String?): Result<Unit>

    /**
     * Clear entire library (V2 pattern)
     * Also updates denormalized Show.isInLibrary columns
     */
    suspend fun clearLibrary(): Result<Unit>

    /**
     * Unpin all shows (V2 pattern)
     */
    suspend fun unpinAllShows(): Result<Unit>
}
