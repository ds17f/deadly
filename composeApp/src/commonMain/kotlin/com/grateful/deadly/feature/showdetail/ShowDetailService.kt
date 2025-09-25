package com.grateful.deadly.feature.showdetail

import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.domain.models.Recording
import com.grateful.deadly.services.archive.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * ShowDetailService - Universal service for ShowDetail feature business logic.
 *
 * This service coordinates between local database (ShowRepository) and Archive.org API (ArchiveService)
 * to provide complete show detail functionality following V2's proven patterns.
 *
 * Key Responsibilities:
 * - Database-first show loading (immediate display)
 * - Archive.org track enhancement (progressive loading)
 * - Recording selection and management
 * - Adjacent show navigation support
 * - Background prefetching coordination
 *
 * Follows the Universal Service + Platform Tool pattern:
 * - Contains ALL business logic
 * - Uses platform tools via dependency injection
 * - Remains platform-agnostic
 */
interface ShowDetailService {

    // Reactive state flows (V2 pattern)
    val currentShow: StateFlow<Show?>
    val currentRecording: StateFlow<Recording?>
    val currentTracks: StateFlow<List<Track>>
    val isTracksLoading: StateFlow<Boolean>
    val error: StateFlow<String?>

    /**
     * Load show with optional recording selection.
     *
     * Implements V2's dual routing pattern:
     * - showId only: Load show with best recording auto-selection
     * - showId + recordingId: Load specific recording
     *
     * Database data loads immediately, Archive.org tracks load asynchronously.
     */
    suspend fun loadShow(showId: String, recordingId: String? = null)

    /**
     * Select a different recording for the current show.
     * Triggers track reload from Archive.org for the new recording.
     */
    suspend fun selectRecording(recordingId: String)

    /**
     * Refresh current show data.
     * Clears Archive.org cache and reloads tracks.
     */
    suspend fun refreshCurrentShow()

    /**
     * Get adjacent shows for navigation.
     * Returns previous and next shows chronologically.
     */
    suspend fun getAdjacentShows(): AdjacentShows

    /**
     * Clear error state.
     */
    fun clearError()
}

/**
 * Adjacent shows for navigation support.
 */
data class AdjacentShows(
    val previousShow: Show?,
    val nextShow: Show?
)

/**
 * Complete show detail data combining database and Archive.org information.
 */
data class ShowDetailData(
    val show: Show,
    val recording: Recording?,
    val tracks: List<Track> = emptyList(),
    val tracksLoading: Boolean = false
)