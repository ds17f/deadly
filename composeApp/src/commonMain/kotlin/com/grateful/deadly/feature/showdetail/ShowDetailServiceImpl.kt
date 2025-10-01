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
import kotlinx.coroutines.Job

/**
 * ShowDetailService implementation following V2's database-first loading patterns.
 *
 * This service coordinates between local database (ShowRepository) and Archive.org API (ArchiveService)
 * using V2's proven approach:
 * 1. Database data loads immediately (no loading spinners for navigation)
 * 2. Archive.org tracks load asynchronously in background
 * 3. Progressive enhancement: DB → UI → API → enhanced UI
 * 4. Background prefetching of adjacent shows (V2 pattern)
 *
 * Follows Universal Service + Platform Tool pattern with reactive StateFlow programming.
 */
class ShowDetailServiceImpl(
    private val showRepository: ShowRepository,
    private val archiveService: ArchiveService
) : ShowDetailService {

    companion object {
        private const val TAG = "ShowDetailService"

        // V2's Default format priority for smart selection with fallback
        // Note: FLAC excluded due to ExoPlayer compatibility issues
        private val DEFAULT_FORMAT_PRIORITY = listOf(
            "VBR MP3",      // Best balance for streaming
            "MP3",          // Universal fallback
            "Ogg Vorbis"    // Good quality, efficient
        )
    }

    // Service scope for background operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Prefetch job tracking (V2 pattern) - tracks active background loads
    private val prefetchJobs = mutableMapOf<String, Job>()

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

        // Cancel any stale prefetches from previous navigation
        cancelAllPrefetches()

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

            // Clear tracks immediately to prevent showing stale data during navigation
            _currentTracks.value = emptyList()
            Logger.d(TAG, "Cleared tracks for new show load")

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
     *
     * Uses efficient DB queries (V2 pattern):
     * - Crosses year boundaries (queries entire database by date)
     * - Filters out shows without recordings (bestRecordingId NOT NULL)
     * - Only fetches 2 shows from DB instead of loading all shows into memory
     */
    override suspend fun getAdjacentShows(): AdjacentShows {
        val currentShow = _currentShow.value
        if (currentShow == null) {
            Logger.d(TAG, "No current show for adjacent search")
            return AdjacentShows(null, null)
        }

        try {
            // Use efficient DB-level navigation queries (V2 pattern)
            val previousShow = showRepository.getPreviousShowByDate(currentShow.date)
            val nextShow = showRepository.getNextShowByDate(currentShow.date)

            Logger.d(TAG, "Adjacent shows: previous=${previousShow?.displayTitle}, next=${nextShow?.displayTitle}")
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
                    onSuccess = { allTracks ->
                        Logger.d(TAG, "Loaded ${allTracks.size} tracks from Archive.org")

                        // Race condition guard: Only update if this recording is still current
                        if (_currentRecording.value?.identifier != recordingId) {
                            Logger.d(TAG, "Discarding tracks for $recordingId (no longer current recording, keeping loading state)")
                            // Don't clear loading flag - the NEW show is still loading
                            return@fold
                        }

                        // V2's Smart format selection with fallback
                        val selectedFormat = selectBestAvailableFormat(allTracks)

                        if (selectedFormat == null) {
                            Logger.w(TAG, "No compatible format found in loaded tracks")
                            _error.value = "No compatible audio format found"
                            _isTracksLoading.value = false
                            return@fold
                        }

                        // Filter to selected format only (V2 pattern)
                        val filteredTracks = filterTracksToFormat(allTracks, selectedFormat)
                        Logger.d(TAG, "Using ${filteredTracks.size} tracks in format: $selectedFormat")

                        _currentTracks.value = filteredTracks
                        _isTracksLoading.value = false

                        // Start background prefetch for adjacent shows (V2 pattern)
                        startAdjacentPrefetch()
                    },
                    onFailure = { error ->
                        Logger.e(TAG, "Failed to load tracks from Archive.org: ${error.message}", error)

                        // Only clear loading and set error if this is still the current recording
                        if (_currentRecording.value?.identifier == recordingId) {
                            _error.value = "Failed to load tracks: ${error.message}"
                            _isTracksLoading.value = false
                        } else {
                            Logger.d(TAG, "Ignoring error for stale recording $recordingId")
                        }
                    }
                )

            } catch (e: Exception) {
                Logger.e(TAG, "Error in background track loading", e)

                // Only clear loading and set error if this is still the current recording
                if (_currentRecording.value?.identifier == recordingId) {
                    _error.value = "Failed to load tracks: ${e.message}"
                    _isTracksLoading.value = false
                } else {
                    Logger.d(TAG, "Ignoring exception for stale recording $recordingId")
                }
            }
        }
    }

    /**
     * V2's Smart format selection with fallback logic
     *
     * Tries formats in priority order until tracks are found.
     */
    private fun selectBestAvailableFormat(
        allTracks: List<Track>,
        formatPriorities: List<String> = DEFAULT_FORMAT_PRIORITY
    ): String? {
        Logger.d(TAG, "Selecting best format from ${allTracks.size} tracks")
        Logger.d(TAG, "Available formats: ${allTracks.map { it.format }.distinct()}")

        // Try each format in priority order
        for (preferredFormat in formatPriorities) {
            val tracksInFormat = allTracks.filter {
                it.format.equals(preferredFormat, ignoreCase = true)
            }

            if (tracksInFormat.isNotEmpty()) {
                Logger.d(TAG, "Selected format '$preferredFormat' (${tracksInFormat.size} tracks)")
                return preferredFormat
            }

            Logger.d(TAG, "Format '$preferredFormat' not available, trying next...")
        }

        // If no format from priority list found, return null
        Logger.w(TAG, "No tracks found in any preferred format")
        return null
    }

    /**
     * V2's Filter tracks to selected format only
     * Used after format selection to get tracks for UI display
     */
    private fun filterTracksToFormat(tracks: List<Track>, selectedFormat: String): List<Track> {
        return tracks.filter { track ->
            track.format.equals(selectedFormat, ignoreCase = true)
        }.sortedBy { it.trackNumber }
    }

    /**
     * Start background prefetch for adjacent shows (V2 pattern).
     * Prefetches 2 next + 2 previous shows to ArchiveService disk cache.
     */
    private fun startAdjacentPrefetch() {
        val current = _currentShow.value
        if (current == null) {
            Logger.d(TAG, "No current show for adjacent prefetch")
            return
        }

        Logger.d(TAG, "Starting adjacent prefetch for: ${current.displayTitle}")

        serviceScope.launch {
            try {
                // Prefetch next 2 shows
                var currentNextDate = current.date
                repeat(2) { index ->
                    val nextShow = showRepository.getNextShowByDate(currentNextDate)
                    if (nextShow != null) {
                        currentNextDate = nextShow.date
                        val recordingId = nextShow.bestRecordingId

                        if (recordingId != null && !prefetchJobs.containsKey(recordingId)) {
                            startPrefetchInternal(nextShow, recordingId, "next+${index + 1}")
                        } else {
                            Logger.d(TAG, "Skipping next+${index + 1} prefetch: recordingId=$recordingId, alreadyPrefetching=${prefetchJobs.containsKey(recordingId ?: "")}")
                        }
                    }
                }

                // Prefetch previous 2 shows
                var currentPrevDate = current.date
                repeat(2) { index ->
                    val previousShow = showRepository.getPreviousShowByDate(currentPrevDate)
                    if (previousShow != null) {
                        currentPrevDate = previousShow.date
                        val recordingId = previousShow.bestRecordingId

                        if (recordingId != null && !prefetchJobs.containsKey(recordingId)) {
                            startPrefetchInternal(previousShow, recordingId, "previous+${index + 1}")
                        } else {
                            Logger.d(TAG, "Skipping previous+${index + 1} prefetch: recordingId=$recordingId, alreadyPrefetching=${prefetchJobs.containsKey(recordingId ?: "")}")
                        }
                    }
                }

            } catch (e: Exception) {
                Logger.e(TAG, "Error in startAdjacentPrefetch", e)
            }
        }
    }

    /**
     * Internal prefetch method - fetches tracks and writes to ArchiveService disk cache.
     */
    private fun startPrefetchInternal(show: Show, recordingId: String, priority: String) {
        // Don't prefetch if already in progress
        if (prefetchJobs[recordingId]?.isActive == true) {
            Logger.d(TAG, "Prefetch already active for: $recordingId")
            return
        }

        Logger.d(TAG, "Starting $priority prefetch for ${show.displayTitle} (recording: $recordingId)")

        val job = serviceScope.launch {
            try {
                // Call ArchiveService which handles cache check and write
                val result = archiveService.getRecordingTracks(recordingId)

                if (result.isSuccess) {
                    val tracks = result.getOrNull() ?: emptyList()
                    Logger.d(TAG, "Prefetch completed for ${show.displayTitle}: ${tracks.size} tracks cached to disk")
                } else {
                    Logger.w(TAG, "Prefetch failed for ${show.displayTitle}: ${result.exceptionOrNull()}")
                }

            } catch (e: Exception) {
                Logger.e(TAG, "Prefetch error for ${show.displayTitle}", e)
            } finally {
                // Remove from active prefetches when complete
                prefetchJobs.remove(recordingId)
            }
        }

        prefetchJobs[recordingId] = job
    }

    /**
     * Cancel all active prefetch jobs.
     */
    private fun cancelAllPrefetches() {
        Logger.d(TAG, "Canceling ${prefetchJobs.size} active prefetch jobs")
        prefetchJobs.values.forEach { it.cancel() }
        prefetchJobs.clear()
    }
}