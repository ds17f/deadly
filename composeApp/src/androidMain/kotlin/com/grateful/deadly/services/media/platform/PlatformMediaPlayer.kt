package com.grateful.deadly.services.media.platform

import android.content.Context
import android.util.Log
import com.grateful.deadly.services.media.MediaControllerRepository
import com.grateful.deadly.services.media.EnrichedTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android implementation of PlatformMediaPlayer using MediaSessionService.
 *
 * Delegates to MediaControllerRepository which connects to DeadlyMediaSessionService.
 * Enables Android Auto, Wear notifications, and rich media controls automatically.
 */
actual class PlatformMediaPlayer(
    private val context: Context
) {

    companion object {
        private const val TAG = "PlatformMediaPlayer"
    }

    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateUpdateJob: Job? = null

    // MediaController repository for MediaSession integration
    private val mediaControllerRepository = MediaControllerRepository(context)

    private val _playbackState = MutableStateFlow(PlatformPlaybackState())
    actual val playbackState: Flow<PlatformPlaybackState> = _playbackState.asStateFlow()

    // Expose MediaController's current track index for MediaService sync
    private val _currentTrackIndex = MutableStateFlow(-1)
    actual val currentTrackIndex: Flow<Int> = _currentTrackIndex.asStateFlow()

    // Current track metadata for MediaSession
    private var currentTrack: com.grateful.deadly.services.archive.Track? = null
    private var currentRecordingId: String? = null

    // Enriched track metadata for extraction (V2 pattern)
    private var currentEnrichedTracks: List<EnrichedTrack> = emptyList()
    private var currentEnrichedTrackIndex: Int = -1

    init {
        setupMediaControllerStateSync()
    }

    /**
     * Set track metadata for MediaSession rich notifications and Auto/Wear integration.
     */
    actual suspend fun setTrackMetadata(track: com.grateful.deadly.services.archive.Track, recordingId: String) {
        Log.d(TAG, "🎵 [METADATA] Setting track: ${track.title} from $recordingId")
        currentTrack = track
        currentRecordingId = recordingId
    }

    /**
     * Load and play an audio URL using MediaSession.
     * Enables notifications, Android Auto, and Wear support automatically.
     */
    actual suspend fun loadAndPlay(url: String): Result<Unit> {
        return try {
            Log.d(TAG, "🎵 [MEDIA] Loading and playing URL: $url")

            // Use MediaControllerRepository with rich metadata if available
            val track = currentTrack
            val recordingId = currentRecordingId

            if (track != null && recordingId != null) {
                // Use MediaSession with rich metadata for notifications/Auto/Wear
                mediaControllerRepository.loadAndPlay(url, track, recordingId)
            } else {
                // Fallback: could create a dummy track, but this shouldn't happen
                Log.w(TAG, "No track metadata available - MediaSession features may be limited")
                Result.failure(Exception("Track metadata required for MediaSession integration"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load and play URL: $url", e)
            Result.failure(e)
        }
    }

    /**
     * Load and play a playlist of enriched tracks with V2 metadata.
     * Android implementation creates MediaItems with rich MediaMetadata extras.
     */
    actual suspend fun loadAndPlayPlaylist(enrichedTracks: List<EnrichedTrack>, startIndex: Int): Result<Unit> {
        return try {
            Log.d(TAG, "🎵 [PLAYLIST] Loading enriched playlist: ${enrichedTracks.size} tracks, starting at $startIndex")

            // Store enriched tracks for metadata extraction
            currentEnrichedTracks = enrichedTracks
            currentEnrichedTrackIndex = startIndex

            // Extract basic tracks for MediaController (it will handle MediaMetadata creation)
            val tracks = enrichedTracks.map { it.track }
            val recordingId = enrichedTracks.firstOrNull()?.recordingId ?: ""

            // Use V2's playlist queuing approach via MediaController
            // TODO: Update MediaControllerRepository to accept EnrichedTrack directly
            mediaControllerRepository.loadAndPlayPlaylist(tracks, recordingId, startIndex)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load and play enriched playlist", e)
            Result.failure(e)
        }
    }

    actual suspend fun pause(): Result<Unit> {
        return try {
            Log.d(TAG, "🎵 [MEDIA] Pausing playback")
            mediaControllerRepository.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause", e)
            Result.failure(e)
        }
    }

    actual suspend fun resume(): Result<Unit> {
        return try {
            Log.d(TAG, "🎵 [MEDIA] Resuming playback")
            mediaControllerRepository.resume()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume", e)
            Result.failure(e)
        }
    }

    actual suspend fun seekTo(positionMs: Long): Result<Unit> {
        return try {
            Log.d(TAG, "🎵 [MEDIA] Seeking to position: ${positionMs}ms")
            mediaControllerRepository.seekTo(positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seek to position: ${positionMs}ms", e)
            Result.failure(e)
        }
    }

    actual suspend fun stop(): Result<Unit> {
        return try {
            Log.d(TAG, "🎵 [MEDIA] Stopping playback")
            val result = mediaControllerRepository.stop()

            // Clear local state
            updatePlaybackState {
                copy(
                    isPlaying = false,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    isLoading = false,
                    isBuffering = false,
                    error = null
                )
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop", e)
            Result.failure(e)
        }
    }

    /**
     * Skip to next track in playlist (Android MediaController navigation)
     */
    actual suspend fun nextTrack(): Result<Unit> {
        return try {
            Log.d(TAG, "🎵 [NAVIGATION] Next track")
            mediaControllerRepository.nextTrack()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to go to next track", e)
            Result.failure(e)
        }
    }

    /**
     * Skip to previous track in playlist (Android MediaController navigation)
     */
    actual suspend fun previousTrack(): Result<Unit> {
        return try {
            Log.d(TAG, "🎵 [NAVIGATION] Previous track")
            mediaControllerRepository.previousTrack()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to go to previous track", e)
            Result.failure(e)
        }
    }

    actual fun release() {
        Log.d(TAG, "🎵 [MEDIA] Releasing MediaController resources")
        stateUpdateJob?.cancel()
        playerScope.launch {
            mediaControllerRepository.release()
        }
    }

    /**
     * Extract showId from currently playing item.
     * Uses stored EnrichedTrack metadata and synced MediaController track index.
     */
    actual fun extractShowIdFromCurrentItem(): String? {
        val mediaControllerIndex = _currentTrackIndex.value
        return if (mediaControllerIndex >= 0 && mediaControllerIndex < currentEnrichedTracks.size) {
            val enrichedTrack = currentEnrichedTracks[mediaControllerIndex]
            Log.d(TAG, "🎵 [EXTRACT] ShowId: ${enrichedTrack.showId} (index: $mediaControllerIndex)")
            enrichedTrack.showId
        } else {
            Log.w(TAG, "🎵 [EXTRACT] No showId available - invalid index: $mediaControllerIndex/${currentEnrichedTracks.size}")
            null
        }
    }

    /**
     * Extract recordingId from currently playing item.
     * Uses stored EnrichedTrack metadata and synced MediaController track index.
     */
    actual fun extractRecordingIdFromCurrentItem(): String? {
        val mediaControllerIndex = _currentTrackIndex.value
        return if (mediaControllerIndex >= 0 && mediaControllerIndex < currentEnrichedTracks.size) {
            val enrichedTrack = currentEnrichedTracks[mediaControllerIndex]
            Log.d(TAG, "🎵 [EXTRACT] RecordingId: ${enrichedTrack.recordingId} (index: $mediaControllerIndex)")
            enrichedTrack.recordingId
        } else {
            Log.w(TAG, "🎵 [EXTRACT] No recordingId available - invalid index: $mediaControllerIndex/${currentEnrichedTracks.size}")
            null
        }
    }

    /**
     * Extract complete enriched track metadata from currently playing item.
     * Uses stored EnrichedTrack metadata and synced MediaController track index.
     */
    actual fun extractCurrentEnrichedTrack(): EnrichedTrack? {
        val mediaControllerIndex = _currentTrackIndex.value
        return if (mediaControllerIndex >= 0 && mediaControllerIndex < currentEnrichedTracks.size) {
            val enrichedTrack = currentEnrichedTracks[mediaControllerIndex]
            Log.d(TAG, "🎵 [EXTRACT] EnrichedTrack: ${enrichedTrack.shortId} (index: $mediaControllerIndex)")
            enrichedTrack
        } else {
            Log.w(TAG, "🎵 [EXTRACT] No EnrichedTrack available - invalid index: $mediaControllerIndex/${currentEnrichedTracks.size}")
            null
        }
    }

    /**
     * Sync MediaController state to PlatformPlaybackState.
     * Bridges MediaController state to the universal service interface.
     */
    private fun setupMediaControllerStateSync() {
        stateUpdateJob = playerScope.launch {
            // Collect MediaController state and map to PlatformPlaybackState
            mediaControllerRepository.isPlaying.collect { isPlaying ->
                updatePlaybackState { copy(isPlaying = isPlaying) }
            }
        }

        // Collect other state flows for comprehensive state sync
        playerScope.launch {
            mediaControllerRepository.currentPosition.collect { position ->
                updatePlaybackState { copy(currentPositionMs = position) }
            }
        }

        playerScope.launch {
            mediaControllerRepository.duration.collect { duration ->
                updatePlaybackState { copy(durationMs = duration) }
            }
        }

        playerScope.launch {
            mediaControllerRepository.connectionState.collect { connectionState ->
                val connectionLoading = connectionState == MediaControllerRepository.ConnectionState.Connecting
                updatePlaybackState {
                    copy(
                        error = if (connectionState == MediaControllerRepository.ConnectionState.Failed) {
                            "Failed to connect to MediaSession"
                        } else null
                    )
                }
            }
        }

        playerScope.launch {
            mediaControllerRepository.isLoading.collect { isLoading ->
                updatePlaybackState { copy(isLoading = isLoading) }
            }
        }

        playerScope.launch {
            mediaControllerRepository.currentMediaItemIndex.collect { index ->
                _currentTrackIndex.value = index
                Log.d(TAG, "🎵 [SYNC] Track index synced to: $index")
            }
        }
    }

    private fun updatePlaybackState(update: PlatformPlaybackState.() -> PlatformPlaybackState) {
        _playbackState.value = _playbackState.value.update()
    }
}