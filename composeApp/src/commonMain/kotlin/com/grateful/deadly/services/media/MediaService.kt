package com.grateful.deadly.services.media

import com.grateful.deadly.services.media.platform.PlatformMediaPlayer
import com.grateful.deadly.services.media.platform.PlatformPlaybackState
import com.grateful.deadly.services.archive.Track
import com.grateful.deadly.core.logging.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Universal MediaService implementing Archive.org track playback business logic.
 *
 * This is the Universal Service in the Universal Service + Platform Tool pattern.
 * It contains ALL media playback domain knowledge while remaining platform-agnostic
 * by delegating to PlatformMediaPlayer.
 *
 * Responsibilities:
 * - Archive.org URL handling and track context
 * - Playback queue management and track navigation
 * - Show-aware playback state and progress tracking
 * - Smart format selection and streaming optimization
 * - Playback history and resume functionality
 *
 * Platform tools handle:
 * - Generic audio URL loading and playback (PlatformMediaPlayer)
 * - Platform-optimized media players (ExoPlayer vs AVPlayer)
 * - Audio focus and session management
 * - Low-level playback state events
 */
class MediaService(
    private val platformMediaPlayer: PlatformMediaPlayer
) {

    companion object {
        private const val TAG = "MediaService"

        // Archive.org streaming optimization
        private const val ARCHIVE_STREAM_BASE = "https://archive.org/download"

        // Business logic constants
        private const val SEEK_FORWARD_SECONDS = 15
        private const val SEEK_BACKWARD_SECONDS = 15
        private const val AUTO_NEXT_THRESHOLD_SECONDS = 5 // Auto-advance when near end
    }

    // Service scope for track sync
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        // Sync track index changes from platform (Android MediaController navigation)
        serviceScope.launch {
            platformMediaPlayer.currentTrackIndex.collect { newIndex ->
                if (newIndex >= 0 && newIndex < currentPlaylist.size && newIndex != currentTrackIndex) {
                    Logger.d(TAG, "🎵 [SYNC] Track index from platform: $currentTrackIndex -> $newIndex")
                    currentTrackIndex = newIndex
                    currentTrack = currentPlaylist[newIndex]
                }
            }
        }

        // Restore saved playback state on iOS (Android uses MediaSessionService)
        serviceScope.launch {
            restorePlaybackState()
        }
    }

    /**
     * Restore saved playback state from platform storage (iOS only).
     * Android doesn't need this - MediaSessionService persists state automatically.
     */
    private suspend fun restorePlaybackState() {
        try {
            // Use helper to get saved state (iOS returns state, Android returns null)
            val savedState = getSavedPlaybackState(platformMediaPlayer)

            if (savedState != null) {
                Logger.d(TAG, "🎵 [RESTORE] Restoring playback state - track ${savedState.trackIndex}/${savedState.enrichedTracks.size}")

                // Restore playlist and metadata
                currentPlaylist = savedState.enrichedTracks.map { it.track }
                currentTrackIndex = savedState.trackIndex
                currentTrack = currentPlaylist.getOrNull(currentTrackIndex)
                internalCurrentRecordingId = savedState.recordingId
                _currentShowId.value = savedState.showId
                _currentRecordingId.value = savedState.recordingId

                // Get show metadata from first enriched track
                val firstTrack = savedState.enrichedTracks.firstOrNull()
                currentShowDate = firstTrack?.showDate
                currentVenue = firstTrack?.venue
                currentLocation = firstTrack?.location

                // Restore playback using enriched playlist
                platformMediaPlayer.loadAndPlayPlaylist(savedState.enrichedTracks, savedState.trackIndex)

                // Seek to saved position
                if (savedState.positionMs > 0) {
                    platformMediaPlayer.seekTo(savedState.positionMs)
                }

                // Pause immediately - let user choose to resume
                platformMediaPlayer.pause()

                Logger.d(TAG, "🎵 [RESTORE] Playback state restored successfully")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to restore playback state", e)
        }
    }

    /**
     * Save current playback state (iOS only - for lifecycle events).
     */
    fun savePlaybackState() {
        saveCurrentPlaybackState(platformMediaPlayer)
    }

    // Current playback context - Archive.org business knowledge
    private var currentTrack: Track? = null
    private var currentPlaylist: List<Track> = emptyList()
    private var currentTrackIndex: Int = -1
    private var internalCurrentRecordingId: String? = null

    // Show metadata for UI display
    private var currentShowDate: String? = null
    private var currentVenue: String? = null
    private var currentLocation: String? = null

    // StateFlows for RecentShowsService observation (V2 pattern)
    private val _currentShowId = MutableStateFlow<String?>(null)
    val currentShowId: StateFlow<String?> = _currentShowId.asStateFlow()

    private val _currentRecordingId = MutableStateFlow<String?>(null)
    val currentRecordingId: StateFlow<String?> = _currentRecordingId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val playbackStatus: StateFlow<PlaybackStatus> = platformMediaPlayer.playbackState.map { platformState ->
        _isPlaying.value = platformState.isPlaying
        PlaybackStatus(
            currentPosition = platformState.currentPositionMs,
            duration = platformState.durationMs,
            progress = if (platformState.durationMs > 0) platformState.currentPositionMs.toFloat() / platformState.durationMs.toFloat() else 0f,
            isPlaying = platformState.isPlaying,
            isLoading = platformState.isLoading,
            isBuffering = platformState.isBuffering
        )
    }.stateIn(serviceScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, PlaybackStatus())

    /**
     * Show-aware playback state combining platform state with Archive.org context.
     *
     * Maps platform playback state to domain-aware state with track information.
     */
    val playbackState: Flow<MediaPlaybackState> = platformMediaPlayer.playbackState.map { platformState ->
        MediaPlaybackState(
            currentTrack = currentTrack,
            currentRecordingId = internalCurrentRecordingId,
            showDate = currentShowDate,
            venue = currentVenue,
            location = currentLocation,
            isPlaying = platformState.isPlaying,
            currentPositionMs = platformState.currentPositionMs,
            durationMs = platformState.durationMs,
            isLoading = platformState.isLoading,
            isBuffering = platformState.isBuffering,
            error = platformState.error,
            hasNext = hasNextTrack(),
            hasPrevious = hasPreviousTrack(),
            playlistPosition = if (currentTrackIndex >= 0) currentTrackIndex + 1 else 0,
            playlistSize = currentPlaylist.size
        )
    }

    /**
     * Load and play a specific track from a show (V2 pattern).
     *
     * ALWAYS loads the entire show playlist and starts at the specified track.
     * This enables proper next/previous navigation and matches V2 behavior.
     */
    suspend fun playTrack(
        track: Track,
        recordingId: String,
        allTracks: List<Track>,
        showId: String,           // Show ID for RecentShowsService tracking
        format: String,           // User-selected format (SBD/AUD/etc)
        showDate: String? = null,
        venue: String? = null,
        location: String? = null
    ): Result<Unit> {
        return try {
            // Store show metadata for UI display
            currentShowDate = showDate
            currentVenue = venue
            currentLocation = location

            // Find the index of the clicked track in the full track list
            val startIndex = allTracks.indexOfFirst { it.name == track.name }
            if (startIndex == -1) {
                return Result.failure(Exception("Track ${track.name} not found in show tracks"))
            }

            // Use enriched playlist approach for proper navigation (V2 pattern)
            playPlaylist(allTracks, recordingId, showId, format, startIndex)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to play track ${track.name}", e))
        }
    }

    /**
     * Load and play an Archive.org show playlist.
     *
     * Sets up playlist context and starts with the specified track.
     * Enables track navigation and auto-advance functionality.
     */
    suspend fun playPlaylist(
        tracks: List<Track>,
        recordingId: String,
        showId: String,
        format: String,
        startIndex: Int = 0
    ): Result<Unit> {
        return try {
            if (tracks.isEmpty()) {
                return Result.failure(Exception("Cannot play empty playlist"))
            }

            if (startIndex < 0 || startIndex >= tracks.size) {
                return Result.failure(Exception("Invalid start index: $startIndex"))
            }

            // Set playlist context
            currentPlaylist = tracks
            currentTrackIndex = startIndex
            currentTrack = tracks[startIndex]
            internalCurrentRecordingId = recordingId

            // Update StateFlow for RecentShowsService observation
            _currentShowId.value = showId
            _currentRecordingId.value = recordingId

            // Create enriched tracks with all V2 metadata
            val enrichedTracks = tracks.mapIndexed { index, track ->
                EnrichedTrack.create(
                    track = track,
                    trackIndex = index,
                    showId = showId,
                    recordingId = recordingId,
                    format = format,
                    showDate = currentShowDate,
                    venue = currentVenue,
                    location = currentLocation
                )
            }

            // Log enrichment verification
            Logger.d(TAG, "🎵 [ENRICHMENT] Created ${enrichedTracks.size} enriched tracks")
            enrichedTracks.firstOrNull()?.let { firstTrack ->
                Logger.d(TAG, "🎵 [ENRICHMENT] Sample track - showId: ${firstTrack.showId}, format: ${firstTrack.format}")
                Logger.d(TAG, "🎵 [ENRICHMENT] DisplayAlbum: ${firstTrack.displayAlbum}")
                Logger.d(TAG, "🎵 [ENRICHMENT] MediaId: ${firstTrack.mediaId}")
            }

            // Use enriched playlist approach for metadata-rich platform integration
            platformMediaPlayer.loadAndPlayPlaylist(enrichedTracks, startIndex)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to play playlist", e))
        }
    }

    /**
     * Navigate to next track in playlist.
     *
     * Uses platform-native playlist navigation when available (Android MediaController),
     * falls back to manual navigation (iOS).
     */
    suspend fun nextTrack(): Result<Unit> {
        return try {
            if (!hasNextTrack()) {
                return Result.failure(Exception("No next track available"))
            }

            // Use platform implementation directly (both platforms now support this method)
            platformMediaPlayer.nextTrack()
        } catch (e: Exception) {
            Result.failure(Exception("Failed to advance to next track", e))
        }
    }

    /**
     * Navigate to previous track in playlist.
     *
     * Uses platform-native playlist navigation when available (Android MediaController),
     * falls back to manual navigation (iOS).
     */
    suspend fun previousTrack(): Result<Unit> {
        return try {
            if (!hasPreviousTrack()) {
                return Result.failure(Exception("No previous track available"))
            }

            // Use platform implementation directly (both platforms now support this method)
            platformMediaPlayer.previousTrack()
        } catch (e: Exception) {
            Result.failure(Exception("Failed to go to previous track", e))
        }
    }

    /**
     * Smart seek forward with Archive.org context.
     *
     * Implements smart seek behavior considering track boundaries.
     */
    suspend fun seekForward(): Result<Unit> {
        return try {
            // For Phase 3, simple seek forward implementation
            // In Phase 6, add smart logic for near-end-of-track behavior
            val currentPosition = 0L // Stub for Phase 3
            val targetPosition = currentPosition + (SEEK_FORWARD_SECONDS * 1000)
            platformMediaPlayer.seekTo(targetPosition)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to seek forward", e))
        }
    }

    /**
     * Smart seek backward with Archive.org context.
     *
     * Implements smart seek behavior considering track boundaries.
     */
    suspend fun seekBackward(): Result<Unit> {
        return try {
            // For Phase 3, simple seek backward implementation
            // In Phase 6, add smart logic for track restart vs previous track
            val currentPosition = 0L // Stub for Phase 3
            val targetPosition = maxOf(0L, currentPosition - (SEEK_BACKWARD_SECONDS * 1000))
            platformMediaPlayer.seekTo(targetPosition)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to seek backward", e))
        }
    }

    /**
     * Pause playback.
     * Maintains all Archive.org context for resume.
     */
    suspend fun pause(): Result<Unit> = platformMediaPlayer.pause()

    /**
     * Resume playback.
     * Continues with current Archive.org track and context.
     */
    suspend fun resume(): Result<Unit> = platformMediaPlayer.resume()

    /**
     * Seek to specific position in current track.
     *
     * @param positionMs Target position in milliseconds
     */
    suspend fun seekTo(positionMs: Long): Result<Unit> = platformMediaPlayer.seekTo(positionMs)

    /**
     * Stop playback and clear context.
     * Releases platform player resources and clears Archive.org context.
     */
    suspend fun stop(): Result<Unit> {
        currentTrack = null
        currentPlaylist = emptyList()
        currentTrackIndex = -1
        return platformMediaPlayer.stop()
    }

    /**
     * Release all resources.
     * Should be called when MediaService is no longer needed.
     */
    fun release() {
        currentTrack = null
        currentPlaylist = emptyList()
        currentTrackIndex = -1
        platformMediaPlayer.release()
    }

    // Private helper methods for Archive.org business logic

    private fun buildArchiveStreamUrl(track: Track): String {
        // Archive.org streaming URL format: https://archive.org/download/{identifier}/{filename}
        return "$ARCHIVE_STREAM_BASE/${internalCurrentRecordingId}/${track.name}"
    }

    private fun hasNextTrack(): Boolean {
        return currentTrackIndex >= 0 && currentTrackIndex < currentPlaylist.size - 1
    }

    private fun hasPreviousTrack(): Boolean {
        return currentTrackIndex > 0
    }
}

/**
 * Domain-aware playback state combining platform state with Archive.org context.
 *
 * This represents the high-level playback state that UI components consume.
 * It includes both platform-agnostic playback state and Archive.org business context.
 */
data class MediaPlaybackState(
    // Archive.org track context
    val currentTrack: Track?,
    val currentRecordingId: String?,

    // Show metadata for UI display (V2 pattern)
    val showDate: String?,           // e.g., "1977-05-08"
    val venue: String?,              // e.g., "Barton Hall"
    val location: String?,           // e.g., "Cornell University, Ithaca, NY"

    // Platform playback state (mapped from PlatformPlaybackState)
    val isPlaying: Boolean,
    val currentPositionMs: Long,
    val durationMs: Long,
    val isLoading: Boolean,
    val isBuffering: Boolean,
    val error: String?,

    // Archive.org playlist context
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    val playlistPosition: Int,
    val playlistSize: Int
) {
    /**
     * Current playback position as percentage (0.0 to 1.0).
     */
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()) else 0f

    /**
     * Formatted current position (MM:SS).
     */
    val formattedPosition: String
        get() = formatDuration(currentPositionMs)

    /**
     * Formatted track duration (MM:SS).
     */
    val formattedDuration: String
        get() = formatDuration(durationMs)

    /**
     * Formatted remaining time (-MM:SS).
     */
    val formattedRemaining: String
        get() = if (durationMs > currentPositionMs) "-${formatDuration(durationMs - currentPositionMs)}" else "0:00"

    /**
     * V2-style display subtitle for MiniPlayer: "showDate - venue" or "showDate - location"
     */
    val displaySubtitle: String
        get() = buildString {
            if (!showDate.isNullOrBlank()) {
                append(showDate)
            }
            when {
                !venue.isNullOrBlank() && venue != "Unknown Venue" -> {
                    if (showDate?.isNotBlank() == true) append(" - ")
                    append(venue)
                }
                !location.isNullOrBlank() -> {
                    if (showDate?.isNotBlank() == true) append(" - ")
                    append(location)
                }
            }
        }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes}:${seconds.toString().padStart(2, '0')}"
    }
}

/**
 * Playback status for RecentShowsService observation (V2 pattern).
 *
 * Simplified version of MediaPlaybackState focused on play detection.
 */
data class PlaybackStatus(
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val progress: Float = 0f,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val isBuffering: Boolean = false
)