package com.grateful.deadly.services.media

import com.grateful.deadly.services.media.platform.PlatformMediaPlayer
import com.grateful.deadly.services.media.platform.PlatformPlaybackState
import com.grateful.deadly.services.archive.ShowTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
        // Archive.org streaming optimization
        private const val ARCHIVE_STREAM_BASE = "https://archive.org/download"

        // Business logic constants
        private const val SEEK_FORWARD_SECONDS = 15
        private const val SEEK_BACKWARD_SECONDS = 15
        private const val AUTO_NEXT_THRESHOLD_SECONDS = 5 // Auto-advance when near end
    }

    // Current playback context - Archive.org business knowledge
    private var currentTrack: ShowTrack? = null
    private var currentPlaylist: List<ShowTrack> = emptyList()
    private var currentTrackIndex: Int = -1

    /**
     * Show-aware playback state combining platform state with Archive.org context.
     *
     * Maps platform playback state to domain-aware state with track information.
     */
    val playbackState: Flow<MediaPlaybackState> = platformMediaPlayer.playbackState.map { platformState ->
        MediaPlaybackState(
            currentTrack = currentTrack,
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
     * Load and play a single Archive.org track.
     *
     * Handles Archive.org URL construction and track context setup.
     * Clears any existing playlist context for single-track playback.
     */
    suspend fun playTrack(track: ShowTrack): Result<Unit> {
        return try {
            // Set single-track context
            currentTrack = track
            currentPlaylist = listOf(track)
            currentTrackIndex = 0

            // Build Archive.org streaming URL
            val streamUrl = buildArchiveStreamUrl(track)

            // Delegate to platform player
            platformMediaPlayer.loadAndPlay(streamUrl)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to play track ${track.fileName}", e))
        }
    }

    /**
     * Load and play an Archive.org show playlist.
     *
     * Sets up playlist context and starts with the specified track.
     * Enables track navigation and auto-advance functionality.
     */
    suspend fun playPlaylist(tracks: List<ShowTrack>, startIndex: Int = 0): Result<Unit> {
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

            // Build Archive.org streaming URL for starting track
            val streamUrl = buildArchiveStreamUrl(currentTrack!!)

            // Delegate to platform player
            platformMediaPlayer.loadAndPlay(streamUrl)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to play playlist", e))
        }
    }

    /**
     * Navigate to next track in playlist.
     *
     * Handles Archive.org playlist navigation and automatic track advancement.
     */
    suspend fun nextTrack(): Result<Unit> {
        return try {
            if (!hasNextTrack()) {
                return Result.failure(Exception("No next track available"))
            }

            currentTrackIndex++
            currentTrack = currentPlaylist[currentTrackIndex]

            val streamUrl = buildArchiveStreamUrl(currentTrack!!)
            platformMediaPlayer.loadAndPlay(streamUrl)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to advance to next track", e))
        }
    }

    /**
     * Navigate to previous track in playlist.
     *
     * Handles Archive.org playlist navigation and track history.
     */
    suspend fun previousTrack(): Result<Unit> {
        return try {
            if (!hasPreviousTrack()) {
                return Result.failure(Exception("No previous track available"))
            }

            currentTrackIndex--
            currentTrack = currentPlaylist[currentTrackIndex]

            val streamUrl = buildArchiveStreamUrl(currentTrack!!)
            platformMediaPlayer.loadAndPlay(streamUrl)
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

    private fun buildArchiveStreamUrl(track: ShowTrack): String {
        // Archive.org streaming URL format: https://archive.org/download/{identifier}/{filename}
        return "$ARCHIVE_STREAM_BASE/${track.recordingId}/${track.fileName}"
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
    val currentTrack: ShowTrack?,

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

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes}:${seconds.toString().padStart(2, '0')}"
    }
}