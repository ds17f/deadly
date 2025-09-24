package com.grateful.deadly.services.media.platform

import kotlinx.coroutines.flow.Flow

/**
 * Platform-specific playback state for media operations.
 *
 * Generic playback state with no Archive.org or show-specific knowledge.
 * Universal services will map this to domain-specific models.
 */
data class PlatformPlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBuffering: Boolean = false
)

/**
 * Platform-specific media player for audio playback operations.
 *
 * This is a minimal platform tool in the Universal Service + Platform Tool pattern.
 * It handles ONLY generic media operations with no business logic or Archive.org knowledge.
 *
 * Universal services will handle:
 * - Archive.org URL building for tracks
 * - Track queue management and navigation
 * - Show context and metadata tracking
 * - Smart format selection and fallback
 * - Playback progress tracking and persistence
 *
 * Platform tools handle:
 * - Generic audio URL loading and playback
 * - Platform-optimized media players (ExoPlayer vs AVPlayer)
 * - Playback state management and events
 * - Audio focus and session management
 */
expect class PlatformMediaPlayer {

    /**
     * Current playback state as reactive Flow.
     *
     * Emits state changes for play/pause, position updates, loading states, errors.
     * Universal services subscribe to this for UI updates and business logic.
     */
    val playbackState: Flow<PlatformPlaybackState>

    /**
     * Load and start playing an audio URL.
     *
     * This method has NO knowledge of Archive.org URLs, track formats, or show context.
     * It simply loads and plays any audio URL provided by universal services.
     *
     * @param url Complete audio URL to play
     * @return Result with success or error
     */
    suspend fun loadAndPlay(url: String): Result<Unit>

    /**
     * Pause current playback.
     * Maintains current position for resume.
     */
    suspend fun pause(): Result<Unit>

    /**
     * Resume paused playback.
     * Continues from last position.
     */
    suspend fun resume(): Result<Unit>

    /**
     * Seek to specific position in current track.
     *
     * @param positionMs Target position in milliseconds
     */
    suspend fun seekTo(positionMs: Long): Result<Unit>

    /**
     * Stop playback and release resources.
     * Used when switching tracks or stopping playback completely.
     */
    suspend fun stop(): Result<Unit>

    /**
     * Release all media player resources.
     * Should be called when the player is no longer needed.
     */
    fun release()
}