package com.grateful.deadly.feature.showdetail

import com.grateful.deadly.domain.models.Show
import com.grateful.deadly.domain.models.Recording
import com.grateful.deadly.services.archive.Track
import com.grateful.deadly.services.data.platform.ShowRepository
import com.grateful.deadly.services.archive.ArchiveService
import com.grateful.deadly.core.logging.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ShowDetailService implementation following V2's database-first loading patterns.
 *
 * This service coordinates between local database (ShowRepository) and Archive.org API (ArchiveService)
 * using V2's proven approach:
 * 1. Database data loads immediately (no loading spinners for navigation)
 * 2. Archive.org tracks load asynchronously in background
 * 3. Progressive enhancement: DB → UI → API → enhanced UI
 *
 * Follows Universal Service + Platform Tool pattern with reactive StateFlow programming.
 */
class ShowDetailServiceImpl(
    private val showRepository: ShowRepository,
    private val archiveService: ArchiveService
) : ShowDetailService {

    companion object {
        private const val TAG = "ShowDetailService"
    }

    // Service scope for background operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Reactive state flows (V2 pattern)
    private val _currentShow = MutableStateFlow<Show?>(null)
    override val currentShow: StateFlow<Show?> = _currentShow.asStateFlow()

    private val _currentRecording = MutableStateFlow<Recording?>(null)
    override val currentRecording: StateFlow<Recording?> = _currentRecording.asStateFlow()

    private val _currentTracks = MutableStateFlow<List<Track>>(emptyList())
    override val currentTracks: StateFlow<List<Track>> = _currentTracks.asStateFlow()

    private val _isTracksLoading = MutableStateFlow(false)
    override val isTracksLoading: StateFlow<Boolean> = _isTracksLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Load show with optional recording selection using V2's database-first approach.
     *
     * V2's dual routing pattern:
     * - showId only: Load show with best recording auto-selection
     * - showId + recordingId: Load specific recording
     *
     * Database data loads immediately, Archive.org tracks load asynchronously.
     */
    override suspend fun loadShow(showId: String, recordingId: String?) {
        Logger.d(TAG, "loadShow(showId=$showId, recordingId=$recordingId)")
        clearError()

        try {
            // Phase 1: Load show data from database immediately (V2 pattern)
            Logger.d(TAG, "Loading show from database: $showId")
            val show = showRepository.getShowById(showId)

            if (show == null) {
                Logger.w(TAG, "Show not found in database: $showId")
                _error.value = "Show not found: $showId"
                return
            }

            // Update show state immediately (no loading spinner)
            _currentShow.value = show
            Logger.d(TAG, "Show loaded from database: ${show.displayTitle}")

            // Phase 2: Determine which recording to load
            val targetRecordingId = when {
                recordingId != null -> {
                    Logger.d(TAG, "Using provided recordingId: $recordingId")
                    recordingId
                }
                show.bestRecordingId?.isNotBlank() == true -> {
                    Logger.d(TAG, "Using show's best recording: ${show.bestRecordingId}")
                    show.bestRecordingId!!
                }
                else -> {
                    Logger.d(TAG, "No best recording found, loading first available")
                    // Fallback: get first available recording for this show
                    val recordings = showRepository.getRecordingsForShow(showId)
                    recordings.firstOrNull()?.identifier
                }
            }

            if (targetRecordingId == null) {
                Logger.w(TAG, "No recordings available for show: $showId")
                _error.value = "No recordings available for this show"
                return
            }

            // Phase 3: Load recording from database
            Logger.d(TAG, "Loading recording from database: $targetRecordingId")
            val recording = showRepository.getRecordingById(targetRecordingId)

            if (recording == null) {
                Logger.w(TAG, "Recording not found in database: $targetRecordingId")
                _error.value = "Recording not found: $targetRecordingId"
                return
            }

            _currentRecording.value = recording
            Logger.d(TAG, "Recording loaded from database: ${recording.displayTitle}")

            // Phase 4: Load tracks from Archive.org asynchronously (V2 pattern)
            loadTracksInBackground(targetRecordingId)

        } catch (e: Exception) {
            Logger.e(TAG, "Error loading show: $showId", e)
            _error.value = "Failed to load show: ${e.message}"
        }
    }

    /**
     * Select a different recording for the current show.
     * Triggers track reload from Archive.org for the new recording.
     */
    override suspend fun selectRecording(recordingId: String) {
        Logger.d(TAG, "selectRecording($recordingId)")
        clearError()

        try {
            // Load recording from database
            val recording = showRepository.getRecordingById(recordingId)

            if (recording == null) {
                Logger.w(TAG, "Recording not found: $recordingId")
                _error.value = "Recording not found: $recordingId"
                return
            }

            // Update recording state
            _currentRecording.value = recording
            Logger.d(TAG, "Recording selected: ${recording.displayTitle}")

            // Clear current tracks and load new ones
            _currentTracks.value = emptyList()
            loadTracksInBackground(recordingId)

        } catch (e: Exception) {
            Logger.e(TAG, "Error selecting recording: $recordingId", e)
            _error.value = "Failed to select recording: ${e.message}"
        }
    }

    /**
     * Refresh current show data.
     * Clears Archive.org cache and reloads tracks.
     */
    override suspend fun refreshCurrentShow() {
        Logger.d(TAG, "refreshCurrentShow()")

        val currentRecording = _currentRecording.value
        if (currentRecording == null) {
            Logger.w(TAG, "No current recording to refresh")
            return
        }

        try {
            // Clear Archive.org cache for this recording
            archiveService.clearCache(currentRecording.identifier)
            Logger.d(TAG, "Cleared cache for: ${currentRecording.identifier}")

            // Clear current tracks and reload
            _currentTracks.value = emptyList()
            loadTracksInBackground(currentRecording.identifier)

        } catch (e: Exception) {
            Logger.e(TAG, "Error refreshing show", e)
            _error.value = "Failed to refresh: ${e.message}"
        }
    }

    /**
     * Get adjacent shows for navigation.
     * Returns previous and next shows chronologically.
     */
    override suspend fun getAdjacentShows(): AdjacentShows {
        val currentShow = _currentShow.value
        if (currentShow == null) {
            Logger.d(TAG, "No current show for adjacent search")
            return AdjacentShows(null, null)
        }

        try {
            // Get shows for the same year to find adjacent ones
            val year = currentShow.date.take(4).toIntOrNull()
            if (year == null) {
                Logger.w(TAG, "Invalid date format for adjacent search: ${currentShow.date}")
                return AdjacentShows(null, null)
            }

            val yearShows = showRepository.getShowsByYear(year)
                .sortedBy { it.date } // Sort chronologically

            val currentIndex = yearShows.indexOfFirst { it.id == currentShow.id }
            if (currentIndex == -1) {
                Logger.w(TAG, "Current show not found in year shows")
                return AdjacentShows(null, null)
            }

            val previousShow = if (currentIndex > 0) yearShows[currentIndex - 1] else null
            val nextShow = if (currentIndex < yearShows.size - 1) yearShows[currentIndex + 1] else null

            Logger.d(TAG, "Adjacent shows: previous=${previousShow?.id}, next=${nextShow?.id}")
            return AdjacentShows(previousShow, nextShow)

        } catch (e: Exception) {
            Logger.e(TAG, "Error getting adjacent shows", e)
            return AdjacentShows(null, null)
        }
    }

    /**
     * Clear error state.
     */
    override fun clearError() {
        _error.value = null
    }

    /**
     * Load tracks from Archive.org in background without blocking UI (V2 pattern).
     */
    private fun loadTracksInBackground(recordingId: String) {
        Logger.d(TAG, "loadTracksInBackground($recordingId)")

        serviceScope.launch {
            try {
                _isTracksLoading.value = true

                val tracksResult = archiveService.getRecordingTracks(recordingId)

                tracksResult.fold(
                    onSuccess = { tracks ->
                        Logger.d(TAG, "Loaded ${tracks.size} tracks from Archive.org")
                        _currentTracks.value = tracks
                    },
                    onFailure = { error ->
                        Logger.e(TAG, "Failed to load tracks from Archive.org: ${error.message}", error)
                        _error.value = "Failed to load tracks: ${error.message}"
                    }
                )

            } catch (e: Exception) {
                Logger.e(TAG, "Error in background track loading", e)
                _error.value = "Failed to load tracks: ${e.message}"
            } finally {
                _isTracksLoading.value = false
            }
        }
    }
}