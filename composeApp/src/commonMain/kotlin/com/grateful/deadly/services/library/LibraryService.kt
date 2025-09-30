package com.grateful.deadly.services.library

import com.grateful.deadly.domain.models.LibraryShow
import com.grateful.deadly.domain.models.LibraryStats
import kotlinx.coroutines.flow.StateFlow

/**
 * Library service interface (V2 pattern)
 *
 * Provides reactive StateFlow for UI state management and Result-based operations.
 * All business logic is universal - delegates to platform LibraryRepository.
 * This is the service layer in Universal Service + Platform Tool pattern.
 */
interface LibraryService {

    /**
     * Get current library shows as reactive state (V2 pattern)
     * Emits updates automatically when library changes
     */
    fun getCurrentShows(): StateFlow<List<LibraryShow>>

    /**
     * Get current library statistics (V2 pattern)
     * Reactive stats for UI display (count, pinned count, etc.)
     */
    fun getLibraryStats(): StateFlow<LibraryStats>

    /**
     * Add a show to the user's library (V2 pattern)
     * Updates both LibraryShow table and denormalized Show columns
     */
    suspend fun addToLibrary(showId: String): Result<Unit>

    /**
     * Remove a show from the user's library (V2 pattern)
     * CASCADE DELETE removes LibraryShow entry, updates Show columns
     */
    suspend fun removeFromLibrary(showId: String): Result<Unit>

    /**
     * Clear all shows from the user's library (V2 pattern)
     * Bulk operation with transaction safety
     */
    suspend fun clearLibrary(): Result<Unit>

    /**
     * Check if a show is in the user's library (reactive) (V2 pattern)
     * Returns StateFlow for automatic UI updates
     */
    fun isShowInLibrary(showId: String): StateFlow<Boolean>

    /**
     * Pin a show for priority display (V2 pattern)
     * Pinned shows appear first in all library views
     */
    suspend fun pinShow(showId: String): Result<Unit>

    /**
     * Unpin a previously pinned show (V2 pattern)
     */
    suspend fun unpinShow(showId: String): Result<Unit>

    /**
     * Check if a show is pinned (reactive) (V2 pattern)
     * Returns StateFlow for UI indicators
     */
    fun isShowPinned(showId: String): StateFlow<Boolean>

    /**
     * Update library notes for a show (V2 pattern)
     * Personal notes attached to library entries
     */
    suspend fun updateLibraryNotes(showId: String, notes: String?): Result<Unit>

    /**
     * Share a show (V2 pattern - delegates to platform share APIs)
     * Platform-specific sharing implementation
     */
    suspend fun shareShow(showId: String): Result<Unit>

    /**
     * Unpin all shows (V2 pattern)
     * Bulk operation for library management
     */
    suspend fun unpinAllShows(): Result<Unit>
}